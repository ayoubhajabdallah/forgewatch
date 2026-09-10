package main

import "testing"

func TestGeneratorDeterministicAndBounded(t *testing.T) {
	for _, bounds := range [][2]float64{{20, 30}, {-20, -10}, {100, 100}, {1e-9, 2e-9}, {-1e308, -9e307}} {
		a := newReadingGenerator(bounds[0], bounds[1], 42)
		b := newReadingGenerator(bounds[0], bounds[1], 42)
		for range 1000 {
			value := a.next()
			if value != b.next() {
				t.Fatal("same seed did not reproduce the reading")
			}
			if !finite(value) || value < bounds[0] || value > bounds[1] {
				t.Fatalf("reading %g outside %v", value, bounds)
			}
		}
	}
}

func TestGeneratorVariesGradually(t *testing.T) {
	g := newReadingGenerator(20, 30, 7)
	previous := g.next()
	totalChange := 0.0
	for range 1000 {
		value := g.next()
		delta := value - previous
		if delta < 0 {
			delta = -delta
		}
		totalChange += delta
		previous = value
	}
	// Check a reproducible sequence's overall behavior, not individual random samples.
	if totalChange == 0 || totalChange/1000 > 1 {
		t.Fatalf("expected gradual nonconstant variation, mean change=%g", totalChange/1000)
	}
}
