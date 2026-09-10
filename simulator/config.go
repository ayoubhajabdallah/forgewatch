package main

import (
	"flag"
	"fmt"
	"io"
	"math"
	"net/url"
	"strconv"
	"strings"
	"time"
)

type config struct {
	backendURL string
	sensorID   int64
	interval   time.Duration
	minValue   float64
	maxValue   float64
	mode       string
}

func parseConfig(args []string, getenv func(string) string, output io.Writer) (config, error) {
	env := func(name, fallback string) string {
		if value := getenv(name); value != "" {
			return value
		}
		return fallback
	}
	flags := flag.NewFlagSet("forgewatch-simulator", flag.ContinueOnError)
	flags.SetOutput(output)
	backend := flags.String("backend-url", env("FORGEWATCH_BACKEND_URL", "http://localhost:8080"), "backend base URL")
	sensor := flags.String("sensor-id", env("FORGEWATCH_SENSOR_ID", ""), "existing sensor ID (required)")
	interval := flags.String("interval", env("FORGEWATCH_INTERVAL", "5s"), "delay between completed requests, e.g. 500ms or 5s")
	minimum := flags.String("min", env("FORGEWATCH_MIN_VALUE", "20"), "minimum synthetic reading")
	maximum := flags.String("max", env("FORGEWATCH_MAX_VALUE", "30"), "maximum synthetic reading")
	mode := flags.String("mode", env("FORGEWATCH_MODE", "continuous"), "continuous or one-shot")
	if err := flags.Parse(args); err != nil {
		return config{}, err
	}
	if flags.NArg() != 0 {
		return config{}, fmt.Errorf("unexpected positional arguments: %v", flags.Args())
	}

	base, err := url.Parse(*backend)
	if err != nil || base == nil || (base.Scheme != "http" && base.Scheme != "https") || base.Hostname() == "" {
		return config{}, fmt.Errorf("backend-url must be an absolute http or https URL")
	}
	if base.User != nil || base.RawQuery != "" || base.ForceQuery || base.Fragment != "" {
		return config{}, fmt.Errorf("backend-url must not contain credentials, a query, or a fragment")
	}
	if port := base.Port(); port != "" {
		n, err := strconv.Atoi(port)
		if err != nil || n < 1 || n > 65535 {
			return config{}, fmt.Errorf("backend-url port must be between 1 and 65535")
		}
	}
	id, err := strconv.ParseInt(*sensor, 10, 64)
	if err != nil || id <= 0 {
		return config{}, fmt.Errorf("sensor-id is required and must be a positive integer")
	}
	delay, err := time.ParseDuration(*interval)
	if err != nil || delay <= 0 {
		return config{}, fmt.Errorf("interval must be a positive Go duration, e.g. 500ms or 5s")
	}
	low, err := strconv.ParseFloat(*minimum, 64)
	if err != nil || !finite(low) {
		return config{}, fmt.Errorf("min must be a finite number")
	}
	high, err := strconv.ParseFloat(*maximum, 64)
	if err != nil || !finite(high) {
		return config{}, fmt.Errorf("max must be a finite number")
	}
	if low > high || !finite(high-low) {
		return config{}, fmt.Errorf("min must not exceed max, and their difference must be finite")
	}
	if *mode != "continuous" && *mode != "one-shot" {
		return config{}, fmt.Errorf("mode must be continuous or one-shot")
	}
	return config{
		backendURL: strings.TrimRight(base.String(), "/"),
		sensorID:   id, interval: delay, minValue: low, maxValue: high, mode: *mode,
	}, nil
}

func finite(value float64) bool {
	return !math.IsNaN(value) && !math.IsInf(value, 0)
}

func (c config) endpoint() string {
	base, _ := url.Parse(c.backendURL) // Validated before any requests are sent.
	return base.JoinPath("api", "sensors", strconv.FormatInt(c.sensorID, 10), "measurements").String()
}
