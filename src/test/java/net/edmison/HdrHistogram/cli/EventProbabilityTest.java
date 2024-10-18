package net.edmison.HdrHistogram.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import net.edmison.HdrHistogram.cli.EventProbability;

class EventProbabilityTest {

    @Test
    void testValidConstruction() {
        assertDoesNotThrow(() -> new EventProbability(0.5, false, 100d, 10d));
        assertDoesNotThrow(() -> new EventProbability(100d, 10d));
        assertDoesNotThrow(() -> new EventProbability(0.5, 100d, 10d));
    }

    @Test
    void testInvalidProbability() {
        assertThrows(IllegalArgumentException.class, () -> new EventProbability(-0.1, false, 100d, 10d));
        assertThrows(IllegalArgumentException.class, () -> new EventProbability(1.1, false, 100d, 10d));
    }

    @Test
    void testInvalidMeanAndDeviation() {
        assertThrows(IllegalArgumentException.class, () -> new EventProbability(0.5, false, 0d, 10d));
        assertThrows(IllegalArgumentException.class, () -> new EventProbability(0.5, false, 100d, 0d));
    }

    @Test
    void testNextLong() {
        EventProbability ep = new EventProbability(1.0, false, 100d, 10d);
        Random mockRandom = new Random(42); // Use a fixed seed for reproducibility

        long value = ep.nextLong(mockRandom);
        assertTrue(value >= 0);
    }

    @Test
    void testFlattenedDistribution() {
        EventProbability ep = new EventProbability(1.0, true, 100d, 10d);
        Random mockRandom = new Random(42);

        long value = ep.nextLong(mockRandom);
        assertTrue(value >= 100);
    }

    @Test
    void testProbabilityEffect() {
        EventProbability lowProb = new EventProbability(0.1, false, 100d, 10d);
        EventProbability highProb = new EventProbability(0.9, false, 100d, 10d);
        Random mockRandom = new Random(42);

        int lowSum = 0, highSum = 0;
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            lowSum += lowProb.nextLong(mockRandom);
            highSum += highProb.nextLong(mockRandom);
        }
        float lowAvg = lowSum / iterations;
        float highAvg = highSum / iterations;

        assertTrue(highAvg > lowAvg);
    }
}
