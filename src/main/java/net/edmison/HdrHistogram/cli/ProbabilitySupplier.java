package net.edmison.HdrHistogram.cli;

import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public class ProbabilitySupplier implements IntSupplier, LongSupplier, DoubleSupplier {

    private Random random = new Random();
    private List<EventProbability> eventProbabilities;

    public ProbabilitySupplier(List<EventProbability> eventProbabilities) {
        this.eventProbabilities = eventProbabilities;
    }

    public ProbabilitySupplier(List<EventProbability> eventProbabilities, long seed) {
        this(eventProbabilities);
        random.setSeed(seed);
    }

    @Override
    public int getAsInt() {
        int value = 0;
        do {
            for (EventProbability eventProbability : eventProbabilities) {
                value += eventProbability.nextInt(random);
            }
        } while (value <= 0);
        return value;
    }

    @Override
    public long getAsLong() {
        long value = 0;
        do {
            for (EventProbability eventProbability : eventProbabilities) {
                value += eventProbability.nextLong(random);
            }
        } while (value <= 0);
        return value;
    }

    @Override
    public double getAsDouble() {
        double value = 0.0;
        do {
            for (EventProbability eventProbability : eventProbabilities) {
                value += eventProbability.nextDouble(random);
            }
        } while (value <= 0.0);
        return value;
    }

}
