package net.edmison.HdrHistogram.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class HDRHistogramCLITest {
    Random random = new Random();

    @Test
    public void consistentLongValueTest() {
        HdrHistogramCLI cli = new HdrHistogramCLI();
        cli.seed = (long) 0xDEADBEEF;
        cli.baseOption = new EventProbability(50d, 50d);
        cli.eventsOption = new EventProbability[] { new EventProbability(0.01, 15d, 3d),
                new EventProbability(0.001, 400d, 60d),
                new EventProbability(0.001, 600d, 60d),
                new EventProbability(0.0001, 5000d, 100d),
                new EventProbability(0.00001, 500000d, 100d)
        };
        cli.count = 5_000_000;

        cli.validate();

        LongHistogramCompare comparator = new LongHistogramCompare();
        cli.processLong(comparator::accrue);
        assertTrue(comparator.compare());
    }

    @Test
    public void randomLongValueTest() {
        // run 10 tests with a randomized seed, and random values)
        for (int i = 0; i < 10; i++) {
            try {
                HdrHistogramCLI cli = new HdrHistogramCLI();
                cli.seed = random.nextLong();
                cli.baseOption = new EventProbability(50d, 5d);
                cli.eventsOption = new EventProbability[] { new EventProbability(0.01, 15d, 3d),
                        new EventProbability(0.001, 400d, 60d),
                        new EventProbability(0.001, 600d, 60d),
                        new EventProbability(0.0001, 5000d, 100d),
                        new EventProbability(0.00001, 500000d, 100d)
                };
                cli.count = 5_000_000;

                cli.validate();

                LongHistogramCompare comparator = new LongHistogramCompare();
                cli.processLong(comparator::accrue);
                assertTrue(comparator.compare());
            } catch (Throwable cause) {
                fail(cause);
            }
        }
    }

    public void consistentDoubleValueTest() {
        HdrHistogramCLI cli = new HdrHistogramCLI();
        cli.seed = (long) 0xDEADBEEF;
        cli.baseOption = new EventProbability(50d, 50d);
        cli.eventsOption = new EventProbability[] { new EventProbability(0.01, 15d, 3d),
                new EventProbability(0.001, 400d, 60d),
                new EventProbability(0.001, 600d, 60d),
                new EventProbability(0.0001, 5000d, 100d),
                new EventProbability(0.00001, 500000d, 100d)
        };
        cli.count = 5_000_000;

        cli.validate();

        DoubleHistogramCompare comparator = new DoubleHistogramCompare();
        cli.processDouble(comparator::accrue);
        assertTrue(comparator.compare());
    }

    @Test
    public void randomDoubleValueTest() {

        // run 10 tests with a randomized seed, and random values)
        for (int i = 0; i < 10; i++) {
            try {
                HdrHistogramCLI cli = new HdrHistogramCLI();
                cli.seed = random.nextLong();
                cli.baseOption = new EventProbability(50d, 5d);
                cli.eventsOption = new EventProbability[] { new EventProbability(0.01, 15d, 3d),
                        new EventProbability(0.001, 400d, 60d),
                        new EventProbability(0.001, 600d, 60d),
                        new EventProbability(0.0001, 5000d, 100d),
                        new EventProbability(0.00001, 500000d, 100d)
                };
                cli.count = 5_000_000;

                cli.validate();

                DoubleHistogramCompare comparator = new DoubleHistogramCompare();
                cli.processDouble(comparator::accrue);
                assertTrue(comparator.compare());
            } catch (Throwable cause) {
                fail(cause);
            }
        }
    }

    @Test
    public void noArguments() {
        String[] args = {};
        int exitCode = HdrHistogramCLI.execute(args);
        assertNotEquals(0, exitCode);
    }

    @Test
    public void missingArguments() {
        String[] args = {};
        int exitCode = HdrHistogramCLI.execute(args);
        assertNotEquals(0, exitCode);
    }

    @Test
    public void invalidBaseProbabilityArg() {
        String[] args;
        int exitCode;
        try {
            args = new String[] { "-b", "50", "-e", "0.01:200:20", "500", "datagen" };
            exitCode = HdrHistogramCLI.execute(args);
            assertNotEquals(0, exitCode);
        } catch (Exception cause) {
            fail(cause);
        }

        try {
            args = new String[] { "-b", "50:aa", "-e", "0.01:200:20", "500", "datagen" };
            exitCode = HdrHistogramCLI.execute(args);
            assertNotEquals(0, exitCode);
        } catch (Exception cause) {
            fail(cause);
        }

        try {
            args = new String[] { "-b", "string", "-e", "0.01:200:20", "500", "datagen" };
            exitCode = HdrHistogramCLI.execute(args);
            assertNotEquals(0, exitCode);
        } catch (Exception cause) {
            fail(cause);
        }

        try {
            args = new String[] { "-e", "0.01:200:20", "500", "datagen" };
            exitCode = HdrHistogramCLI.execute(args);
            assertNotEquals(0, exitCode);
        } catch (Exception cause) {
            fail(cause);
        }
    }

    @Test
    public void invalidEventProbabilityArg() {
        String[] args;
        int exitCode;
        args = new String[] { "-b", "50:5", "-e", "0.01:200", "500", "data" };
        exitCode = HdrHistogramCLI.execute(args);
        assertNotEquals(0, exitCode);

        args = new String[] { "-b", "50:5", "-e", "string", "500", "data" };
        exitCode = HdrHistogramCLI.execute(args);
        assertNotEquals(0, exitCode);

        args = new String[] { "-b", "50:5", "-e", "101:200:4", "500", "data" };
        exitCode = HdrHistogramCLI.execute(args);
        assertNotEquals(0, exitCode);
    }

    @Test
    public void validLongArguments() {
        String[] args = { "-b", "50:5", "-e", "0.01:200:50", "-e", "0.001:400:60", "-e", "0.001:600:60", "-e",
                "0.0001:5000:100", "500", "data" };
        int exitCode = HdrHistogramCLI.execute(args);
        assertEquals(0, exitCode);
    }

}
