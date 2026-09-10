package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
)

func newHTTPClient() *http.Client {
	return &http.Client{
		Timeout: 5 * time.Second,
		// A redirect is not a successful measurement and must not turn POST into GET.
		CheckRedirect: func(_ *http.Request, _ []*http.Request) error { return http.ErrUseLastResponse },
	}
}

func sendMeasurement(ctx context.Context, client *http.Client, endpoint string, value float64) (int, error) {
	body, err := json.Marshal(struct {
		Value float64 `json:"value"`
	}{Value: value})
	if err != nil {
		return 0, fmt.Errorf("encode measurement: %w", err)
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(body))
	if err != nil {
		return 0, fmt.Errorf("create request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := client.Do(req)
	if err != nil {
		return 0, fmt.Errorf("send measurement: %w", err)
	}
	defer resp.Body.Close()
	// Bound response consumption; the simulator only needs the HTTP status.
	body, err = io.ReadAll(io.LimitReader(resp.Body, 4096))
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		if err != nil {
			return resp.StatusCode, fmt.Errorf("read error response: %w", err)
		}
		return resp.StatusCode, fmt.Errorf("backend rejected measurement: %s", strings.TrimSpace(string(body)))
	}
	return resp.StatusCode, nil
}

func run(ctx context.Context, cfg config, client *http.Client, logger *log.Logger, next func() float64) error {
	endpoint := cfg.endpoint()
	for {
		if ctx.Err() != nil {
			logger.Print("shutdown: stopped")
			return nil
		}
		value := next()
		logger.Printf("generated sensor_id=%d value=%g source=synthetic", cfg.sensorID, value)
		status, err := sendMeasurement(ctx, client, endpoint, value)
		httpResult := "unavailable"
		if status != 0 {
			httpResult = strconv.Itoa(status)
		}
		if err != nil {
			logger.Printf("measurement sensor_id=%d value=%g http_status=%s error=%q", cfg.sensorID, value, httpResult, err)
		} else {
			logger.Printf("measurement sensor_id=%d value=%g http_status=%s accepted", cfg.sensorID, value, httpResult)
		}
		if ctx.Err() != nil {
			logger.Print("shutdown: stopped; an interrupted request may already have reached the backend")
			return nil
		}
		if cfg.mode == "one-shot" {
			return err
		}
		// Do not retry a failed POST: it might have been persisted before the error.
		// Continuous mode waits, then generates a new reading, with no overlap.
		timer := time.NewTimer(cfg.interval)
		select {
		case <-ctx.Done():
			timer.Stop()
			logger.Print("shutdown: stopped")
			return nil
		case <-timer.C:
		}
	}
}
