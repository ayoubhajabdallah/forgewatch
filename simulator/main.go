package main

import (
	"context"
	"errors"
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() { os.Exit(mainExit()) }

func mainExit() int {
	logger := log.New(os.Stderr, "", log.Ldate|log.Ltime|log.LUTC)
	cfg, err := parseConfig(os.Args[1:], os.Getenv, os.Stderr)
	if errors.Is(err, flag.ErrHelp) {
		return 0
	}
	if err != nil {
		logger.Printf("configuration error: %v", err)
		return 2
	}
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()
	client := newHTTPClient()
	defer client.CloseIdleConnections()
	generator := newReadingGenerator(cfg.minValue, cfg.maxValue, time.Now().UnixNano())
	logger.Printf("starting mode=%s sensor_id=%d interval=%s min=%g max=%g source=synthetic timestamps=UTC",
		cfg.mode, cfg.sensorID, cfg.interval, cfg.minValue, cfg.maxValue)
	if err := run(ctx, cfg, client, logger, generator.next); err != nil {
		return 1 // The request error was already logged with its sensor, value and HTTP status.
	}
	return 0
}
