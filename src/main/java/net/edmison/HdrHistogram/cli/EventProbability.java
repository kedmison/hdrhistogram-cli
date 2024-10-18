package net.edmison.HdrHistogram.cli;

import java.util.Objects;
import java.util.Random;

import picocli.CommandLine.ITypeConverter;

public record EventProbability(Double probability, Boolean flatten, Double mean, Double deviation) {
    public EventProbability {
        Objects.requireNonNull(probability);
        Objects.requireNonNull(flatten);
        Objects.requireNonNull(mean);
        Objects.requireNonNull(deviation);
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("probability must be between 0.0 and 1.0");
        }
        if (mean <= 0) {
            throw new IllegalArgumentException("mean must be greater than 0");
        }
        if (deviation <= 0) {
            throw new IllegalArgumentException("deviation must be greater than 0");
        }
    }

    public EventProbability(Double mean, Double deviation) {
        this(1.0, true, mean, deviation);
    }

    public EventProbability(Double probability, Double mean, Double deviation) {
        this(probability, false, mean, deviation);
    }

    /**
     * Calculates a gaussian value based on the probability, mean and deviation
     * 
     * @param random the random number generator to use
     * @return a double value based on the probability, mean and deviation
     */
    public double nextDouble(Random random) {
        double value = 0;
        /*
         * The Random.nextGaussian() method returns "Gaussian ("normally")
         * distributed double value with mean 0.0 and standard deviation 1.0 from this
         * random number generator's sequence".
         * 
         * In practice, this means that it occasionally returns values that are multiple
         * standard deviations away from the mean 0.0,
         * so nextGaussian() may return values that are less than -1.0.
         * 
         * Since this method simulates the probability of occurence of a
         * latency-inducing event, and generates a value for simulated latency impact
         * based on the mean and deviation values, we need to ensure that a negative
         * value for gaussian * deviation does not exceed the mean value
         * and does not create a latency impact that is less than 0.
         */
        if (random.nextDouble() <= probability) {
            double gaussian = flatten ? Math.abs(random.nextGaussian()) : random.nextGaussian();
            value = (mean + (gaussian * deviation));
        }
        return value;
    }

    public long nextLong(Random random) {
        double value = 0;
        do {
            value = Math.max(0.0, nextDouble(random));
        } while (value > (double) Long.MAX_VALUE);
        return (long) value;
    }

    public int nextInt(Random random) {
        double value = 0;
        do {
            value = Math.max(0.0, nextDouble(random));
        } while (value > (double) Integer.MAX_VALUE);
        return (int) value;
    }
}

class BaseEventProbabilityConverter implements ITypeConverter<EventProbability> {
    public EventProbability convert(String value) throws Exception {
        String[] parts = value.split(":");
        if (parts.length == 2) {
            return new EventProbability(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } else {
            throw new Exception("Invalid event probability format");
        }
    }
}

class EventProbabilityConverter implements ITypeConverter<EventProbability> {
    public EventProbability convert(String value) throws Exception {
        String[] parts = value.split(":");
        if (parts.length == 3) {
            return new EventProbability(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        } else {
            throw new Exception("Invalid event probability format");
        }
    }
}
