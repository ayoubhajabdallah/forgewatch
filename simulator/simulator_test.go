package main

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)

func TestMeasurementRequestAndStatuses(t *testing.T) {
	for _, status := range []int{200, 201, 204, 400, 404, 429, 500, 503} {
		t.Run(http.StatusText(status), func(t *testing.T) {
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.Method != http.MethodPost || r.URL.Path != "/api/sensors/7/measurements" || r.Header.Get("Content-Type") != "application/json" {
					t.Errorf("unexpected request: %s %s %s", r.Method, r.URL.Path, r.Header.Get("Content-Type"))
				}
				var payload map[string]float64
				if err := json.NewDecoder(r.Body).Decode(&payload); err != nil || len(payload) != 1 || payload["value"] != 23.5 {
					t.Errorf("unexpected JSON: %v, %v", payload, err)
				}
				w.WriteHeader(status)
				if status >= 400 {
					_, _ = io.WriteString(w, "sensor unavailable")
				}
			}))
			defer server.Close()
			got, err := sendMeasurement(context.Background(), newHTTPClient(), server.URL+"/api/sensors/7/measurements", 23.5)
			if got != status || (err != nil) != (status >= 300) {
				t.Fatalf("status=%d error=%v", got, err)
			}
			if status >= 400 && !strings.Contains(err.Error(), "sensor unavailable") {
				t.Fatalf("missing backend error: %v", err)
			}
		})
	}
}

func TestRedirectNotFollowedAndErrorBodyBounded(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/redirect" {
			t.Error("measurement redirect was followed")
		}
		w.Header().Set("Location", "/redirect")
		w.WriteHeader(http.StatusFound)
		_, _ = io.WriteString(w, strings.Repeat("x", 10000))
	}))
	defer server.Close()
	status, err := sendMeasurement(context.Background(), newHTTPClient(), server.URL, 25)
	if status != http.StatusFound || err == nil || len(err.Error()) > 4200 {
		t.Fatalf("status=%d error length=%v", status, err)
	}
}

func TestBackendUnavailable(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(http.ResponseWriter, *http.Request) {}))
	endpoint := server.URL
	server.Close()
	status, err := sendMeasurement(context.Background(), newHTTPClient(), endpoint, 25)
	if status != 0 || err == nil {
		t.Fatalf("unavailable backend: status=%d error=%v", status, err)
	}
}

func TestRequestCancellationAndTimeout(t *testing.T) {
	for _, cancelEarly := range []bool{true, false} {
		t.Run(map[bool]string{true: "cancellation", false: "timeout"}[cancelEarly], func(t *testing.T) {
			started := make(chan struct{})
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				// Consume the body so the server can observe the client disconnect.
				_, _ = io.Copy(io.Discard, r.Body)
				close(started)
				<-r.Context().Done()
			}))
			defer server.Close()
			ctx, cancel := context.WithCancel(context.Background())
			defer cancel()
			client := newHTTPClient()
			if !cancelEarly {
				client.Timeout = 100 * time.Millisecond
			}
			done := make(chan error, 1)
			go func() {
				_, err := sendMeasurement(ctx, client, server.URL, 25)
				done <- err
			}()
			select {
			case <-started:
			case <-time.After(2 * time.Second):
				t.Fatal("request did not start")
			}
			if cancelEarly {
				cancel()
			}
			select {
			case err := <-done:
				if err == nil {
					t.Fatal("expected interrupted request")
				}
			case <-time.After(2 * time.Second):
				t.Fatal("request failed to stop")
			}
		})
	}
}

func TestOneShotSendsExactlyOnce(t *testing.T) {
	for _, status := range []int{201, 503} {
		calls := 0
		server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			calls++
			w.WriteHeader(status)
		}))
		cfg := config{backendURL: server.URL, sensorID: 7, mode: "one-shot"}
		var logs bytes.Buffer
		err := run(context.Background(), cfg, newHTTPClient(), log.New(&logs, "", 0), func() float64 { return 25 })
		server.Close()
		if calls != 1 || (err != nil) != (status == 503) {
			t.Fatalf("calls=%d error=%v", calls, err)
		}
		if !strings.Contains(logs.String(), "sensor_id=7 value=25 http_status=") {
			t.Fatalf("missing measurement context: %s", logs.String())
		}
	}
}

func TestContinuousRecoversAndStops(t *testing.T) {
	var calls atomic.Int32
	requests := make(chan struct{}, 10)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if calls.Add(1) == 1 {
			w.WriteHeader(http.StatusServiceUnavailable)
		} else {
			w.WriteHeader(http.StatusCreated)
		}
		requests <- struct{}{}
	}))
	defer server.Close()
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	var logs bytes.Buffer
	done := make(chan error, 1)
	cfg := config{backendURL: server.URL, sensorID: 7, interval: time.Millisecond, mode: "continuous"}
	go func() {
		done <- run(ctx, cfg, newHTTPClient(), log.New(&logs, "", 0), func() float64 { return 25 })
	}()
	for range 4 {
		select {
		case <-requests:
		case <-time.After(2 * time.Second):
			t.Fatal("continuous mode did not recover")
		}
	}
	cancel()
	select {
	case err := <-done:
		if err != nil {
			t.Fatal(err)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("continuous mode did not stop")
	}
	if !strings.Contains(logs.String(), "http_status=503") || strings.Count(logs.String(), "http_status=201 accepted") < 2 || !strings.Contains(logs.String(), "shutdown: stopped") {
		t.Fatalf("unexpected logs: %s", logs.String())
	}
}

func TestCancelledRunDoesNotSend(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	cfg := config{backendURL: "http://localhost:8080", sensorID: 7, mode: "continuous"}
	err := run(ctx, cfg, newHTTPClient(), log.New(io.Discard, "", 0), func() float64 {
		t.Fatal("generated after cancellation")
		return 0
	})
	if err != nil {
		t.Fatal(err)
	}
}
