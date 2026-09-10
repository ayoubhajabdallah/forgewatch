package main

import (
	"errors"
	"flag"
	"io"
	"math"
	"strings"
	"testing"
	"time"
)

func noEnv(string) string { return "" }

func TestConfigEnvironmentAndFlagPrecedence(t *testing.T) {
	env := map[string]string{
		"FORGEWATCH_BACKEND_URL": "http://localhost:9090/forge/",
		"FORGEWATCH_SENSOR_ID":   "7", "FORGEWATCH_INTERVAL": "2s",
		"FORGEWATCH_MIN_VALUE": "-10", "FORGEWATCH_MAX_VALUE": "10", "FORGEWATCH_MODE": "one-shot",
	}
	cfg, err := parseConfig([]string{"-sensor-id", "8", "-interval", "500ms"}, func(key string) string { return env[key] }, io.Discard)
	if err != nil {
		t.Fatal(err)
	}
	if cfg.sensorID != 8 || cfg.interval != 500*time.Millisecond || cfg.minValue != -10 || cfg.maxValue != 10 || cfg.mode != "one-shot" {
		t.Fatalf("unexpected config: %+v", cfg)
	}
	if got := cfg.endpoint(); got != "http://localhost:9090/forge/api/sensors/8/measurements" {
		t.Fatalf("endpoint: %s", got)
	}
	// Flags also override malformed environment defaults before validation.
	env["FORGEWATCH_SENSOR_ID"] = "invalid"
	if _, err := parseConfig([]string{"-sensor-id", "9"}, func(key string) string { return env[key] }, io.Discard); err != nil {
		t.Fatal(err)
	}
}

func TestConfigDefaultsAndFixedValue(t *testing.T) {
	cfg, err := parseConfig([]string{"-sensor-id", "1"}, noEnv, io.Discard)
	if err != nil {
		t.Fatal(err)
	}
	if cfg.backendURL != "http://localhost:8080" || cfg.interval != 5*time.Second || cfg.mode != "continuous" || cfg.minValue != 20 || cfg.maxValue != 30 {
		t.Fatalf("unexpected defaults: %+v", cfg)
	}
	if _, err := parseConfig([]string{"-sensor-id", "1", "-min", "100", "-max", "100"}, noEnv, io.Discard); err != nil {
		t.Fatalf("fixed readings should be valid: %v", err)
	}
}

func TestInvalidConfig(t *testing.T) {
	for _, args := range [][]string{
		{}, {"-sensor-id", "0"}, {"-sensor-id", "-1"}, {"-sensor-id", "1.5"},
		{"-sensor-id", "9223372036854775808"}, {"-interval", "0s"}, {"-interval", "-1s"},
		{"-interval", "abc"}, {"-min", "NaN"}, {"-max", "+Inf"}, {"-min", "abc"},
		{"-min", "31"}, {"-min", "-1e308", "-max", "1e308"},
		{"-backend-url", "localhost:8080"}, {"-backend-url", "ftp://localhost"},
		{"-backend-url", "http://"}, {"-backend-url", "http://localhost:99999"},
		{"-backend-url", "http://user:password@localhost"},
		{"-backend-url", "http://localhost?x=1"}, {"-backend-url", "http://localhost#fragment"},
		{"-mode", "invalid"}, {"extra"}, {"-unknown"},
	} {
		t.Run(strings.Join(args, " "), func(t *testing.T) {
			input := append([]string{"-sensor-id", "1"}, args...)
			if len(args) == 0 {
				input = nil // Missing required sensor.
			}
			if _, err := parseConfig(input, noEnv, io.Discard); err == nil {
				t.Fatalf("accepted invalid arguments: %v", input)
			}
		})
	}
	_, err := parseConfig([]string{"-h"}, noEnv, io.Discard)
	if !errors.Is(err, flag.ErrHelp) {
		t.Fatalf("help should not be a configuration failure: %v", err)
	}
	if finite(math.NaN()) || finite(math.Inf(-1)) {
		t.Fatal("non-finite values accepted")
	}
}
