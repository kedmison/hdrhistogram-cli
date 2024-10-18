package net.edmison.HdrHistogram.cli;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.HdrHistogram.DoubleHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.IntCountsHistogram;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

/**
 * This class is a CLI utility for generating data simulating event latencies.
 * It is based on the idea that a latency distribution has a certain base
 * minimum latency,
 * and that additional latency may accrue from multiple distinct causes,
 * each with their own probability of occurrence and amount of impact.
 * <p>
 * Gaussian values from java.util.Random are used to generate the incremental
 * latency amount for
 * each individual contributing cause.
 * This specification can be something like:
 * <code>-b 50:15 -e 0.009:200:50 -e 0.002:400:60 -e 0.0008:600:120 -e 0.00005:2000:100 -e 0.00002:1000:200 </code>
 * where:
 * <ul>
 * <li>-b 50:15 specifies a base latency of 50, with a stddev of 15
 * <li>-e 0.009:200:50 specifies that 0.9% of the time, an event happens with
 * median incremental latency 200, and stddev of 50
 * <li>-e 0.002:400:60 specifies that 0.2% of the time, an event happens with
 * median incremental latency 400, and stddev of 60
 * <li>-e 0.0008:600:120 0.08% of the time, an event occurs with median
 * incremental latency 600, and stddev of 120
 * <li>-e 0.00002:1000:200 0.002% of the time an event occurs with median
 * incremental latency 1000, and stddev of 200
 * </ul>
 * This is somewhat rough but is useful for generating data that can then be
 * graphed in a HDR Histogram.
 * 
 * This CLI can generate a set of values according to this latency distribution
 * for use in other ways,
 * can create a HDR histogram and output its table of data for interpretation,
 * or it can generate a URL that allows
 * immediate visualization of the data.
 * It can also do testing of merged HDR histograms. HDR histograms are
 * commutative: that is,
 * HDRHistogram(a) + HDRHistogram(b) = HDRHistogram(a,b)
 * This test function can demonstrate that this property exists, for a specified
 * set of latency events.
 * 
 */

@Command(name = "HdrHistogramCLI", description = "Performs HDR Histogram operations")
public class HdrHistogramCLI {
    enum HistType {
        INT, LONG, DOUBLE
    };

    @Option(names = { "-s", "--seed" }, paramLabel = "SEED", scope = ScopeType.INHERIT, 
            description = "seed value for generating random values")
    Long seed;
    @Option(names = { "-b", "--base" }, paramLabel = "BASE", scope = ScopeType.INHERIT, 
            description = "The basic event probability, with a latency mean and a standard deviation.  e.g. <latencyMean:stdDev>", 
            converter = BaseEventProbabilityConverter.class)
    EventProbability baseOption;
    @Option(names = { "-e", "--event" }, paramLabel = "EVENT", scope = ScopeType.INHERIT, 
            description = "The probability of an event occurring, with a probability, a latency mean, and a standard deviation: e.g. <probability:latencyMean:stdDev>", 
            converter = EventProbabilityConverter.class, required = true, arity = "1..*")
    EventProbability[] eventsOption;
    @Option(names = { "-t", "--type" }, paramLabel = "TYPE", scope = ScopeType.INHERIT, 
            description = "The type of values (and histogram) to generate.  valid values are int, long, or double", 
            defaultValue = "LONG")
    HistType histType;
    @Parameters(index = "0", scope = ScopeType.INHERIT, 
            description = "The number of values to generate")
    Integer count;

    @Spec
    static CommandSpec spec;

    public static void main(String[] args) {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    protected static int execute(String[] args) {
        int exitCode = new CommandLine(new HdrHistogramCLI())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        return exitCode;
    }

    void validate() {
        if (count < 1) {
            throw new ParameterException(HdrHistogramCLI.spec.commandLine(),
                    "The count value has to be greater than or equal to 1");
        }
    }

    void processInt(IntConsumer intConsumer) {
        ProbabilitySupplier supplier = getProbabilitySupplier();
        IntStream.generate(supplier).limit(count).forEachOrdered(intConsumer);
    }

    void processLong(LongConsumer longConsumer) {
        ProbabilitySupplier supplier = getProbabilitySupplier();
        LongStream.generate(supplier).limit(count).forEachOrdered(longConsumer);
    }

    void processDouble(DoubleConsumer doubleConsumer) {
        ProbabilitySupplier supplier = getProbabilitySupplier();
        DoubleStream.generate(supplier).limit(count).forEachOrdered(doubleConsumer);
    }

    private ProbabilitySupplier getProbabilitySupplier() {
        List<EventProbability> eventProbabilities = new ArrayList<>();
        eventProbabilities.add(baseOption);
        eventProbabilities.addAll(Arrays.asList(eventsOption));

        ProbabilitySupplier supplier = (seed == null
                ? new ProbabilitySupplier(eventProbabilities)
                : new ProbabilitySupplier(eventProbabilities, seed));
        return supplier;
    }

    @Command(name = "test", description = "test merged histograms produces same result as using a single histogram")
    void testMerge() {
        validate();

        boolean compareResult;
        switch (histType) {
            case INT:
                IntHistogramCompare intComparator = new IntHistogramCompare();
                processLong(intComparator::accrue);
                compareResult = intComparator.compare();
                break;
            case LONG:
                LongHistogramCompare longComparator = new LongHistogramCompare();
                processLong(longComparator::accrue);
                compareResult = longComparator.compare();
                break;
            case DOUBLE:
                DoubleHistogramCompare doubleComparator = new DoubleHistogramCompare();
                processDouble(doubleComparator::accrue);
                compareResult = doubleComparator.compare();
                break;
            default:
                throw new RuntimeException("Unknown histogram type: " + histType);
        }
        if (compareResult) {
            System.out.println("Histograms are equal");
        } else {
            System.out.println("Histograms are not equal");
        }

    }

    @Command(name = "data", description = "Generate numbers simulating latency")
    void dataGen() {
        validate();
        switch (histType) {
            case INT:
                processInt(System.out::println);
                break;
            case LONG:
                processLong(System.out::println);
                break;
            case DOUBLE:
                processDouble(System.out::println);
                break;
            default:
                throw new RuntimeException("Unknown histogram type: " + histType);
        }
    }

    @Command(name = "hist", description = "Generate a HDR Histogram")
    void generate() {
        validate();

        switch (histType) {
            case INT:
                generateInt();
                break;
            case LONG:
                generateLong();
                break;
            case DOUBLE:
                generateDouble();
                break;
            default:
                throw new RuntimeException("Unknown histogram type: " + histType);
        }
    }

    private void generateInt() {
        IntCountsHistogram histogram = new IntCountsHistogram(3);
        processInt(histogram::recordValue);
        histogram.outputPercentileDistribution(System.out, 1.0);
    }

    private void generateLong() {
        Histogram histogram = new Histogram(3);
        processLong(histogram::recordValue);
        histogram.outputPercentileDistribution(System.out, 1.0);
    }

    private void generateDouble() {
        DoubleHistogram histogram = new DoubleHistogram(4);
        processDouble(histogram::recordValue);
        histogram.outputPercentileDistribution(System.out, 1.0);
    }

    @Command(name = "url", description = "Generate a link to view a HDR histogram")
    void generateLink() {
        validate();

        switch (histType) {
            case INT:
                generateIntUrl();
                break;
            case LONG:
                generateLongUrl();
                break;
            case DOUBLE:
                System.out.println("Type Double is not supported by the HdrHistogramWidget");
                break;
            default:
                throw new RuntimeException("Unknown histogram type: " + histType);
        }
    }

    private void generateIntUrl() {
        IntCountsHistogram histogram = new IntCountsHistogram(3);
        processInt(histogram::recordValue);
        ByteBuffer bbuf = ByteBuffer.allocate(histogram.getNeededByteBufferCapacity());
        histogram.encodeIntoCompressedByteBuffer(bbuf);
        bbuf.flip();
        ByteBuffer base64 = java.util.Base64.getEncoder().encode(bbuf);

        // from Twitter, it's possible to tweet Long-based histograms
        // from https://x.com/giltene/status/1329576490967592961?s=21
        // example:
        // https://hdrhistogram.github.io/HdrHistogramWidget/
        // ?unitText=nanoseconds
        // &data.Series_A=<base64-encoded-compressed-histogram>
        // &data.Series_B=<base64-encoded-compressed-histogram>
        String urlEncodedHist = URLEncoder.encode(new String(base64.array()), StandardCharsets.UTF_8);
        System.out.println("https://hdrhistogram.github.io/HdrHistogramWidget/?unitText=nanoseconds" +
                "&data.Series_A=" + urlEncodedHist);
    }

    private void generateLongUrl() {
        Histogram histogram = new Histogram(3);
        processInt(histogram::recordValue);
        ByteBuffer bbuf = ByteBuffer.allocate(histogram.getNeededByteBufferCapacity());
        histogram.encodeIntoCompressedByteBuffer(bbuf);
        bbuf.flip();
        ByteBuffer base64 = java.util.Base64.getEncoder().encode(bbuf);

        // from Twitter, it's possible to tweet Long-based histograms
        // from https://x.com/giltene/status/1329576490967592961?s=21
        // example:
        // https://hdrhistogram.github.io/HdrHistogramWidget/
        // ?unitText=nanoseconds
        // &data.Series_A=<base64-encoded-compressed-histogram>
        // &data.Series_B=<base64-encoded-compressed-histogram>
        String urlEncodedHist = URLEncoder.encode(new String(base64.array()), StandardCharsets.UTF_8);
        System.out.println("https://hdrhistogram.github.io/HdrHistogramWidget/?unitText=nanoseconds" +
                "&data.Series_A=" + urlEncodedHist);
    }

}
