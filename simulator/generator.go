package main

import (
	"math"
	"math/rand"
)

// A bounded random walk models gradual variation around the range midpoint.
// These are illustrative synthetic readings, not real ABP process values.
type readingGenerator struct {
	low, high float64
	position  float64
	random    *rand.Rand
}

func newReadingGenerator(low, high float64, seed int64) *readingGenerator {
	return &readingGenerator{low: low, high: high, position: 0.5, random: rand.New(rand.NewSource(seed))}
}

func (g *readingGenerator) next() float64 {
	g.position += 0.12*(0.5-g.position) + 0.04*g.random.NormFloat64()
	g.position = math.Max(0, math.Min(1, g.position))
	value := g.low + g.position*(g.high-g.low)
	// Clamp floating-point edge cases without rounding away small thresholds.
	return math.Max(g.low, math.Min(g.high, value))
}
