package net.edmison.HdrHistogram.cli;

import java.nio.ByteBuffer;

import org.HdrHistogram.Histogram;

public class LongHistogramCompare {
    Histogram allHistogram = new Histogram(3);

    Histogram merger1 = null;
    Histogram merger2 = new Histogram(3);
    long valueCount = 0;
    int mergeCount = 0;

    private int batchSize = 0;

    public LongHistogramCompare() {
        this(10000);
    }

    public LongHistogramCompare(int batchSize) {
        this.batchSize = batchSize;
    }

    public void accrue(long value) {
        allHistogram.recordValue(value);
        merger2.recordValue(value);
        valueCount++;
        if (valueCount % batchSize == 0) {
            reduce();
        }
    }

    private void reduce() {
        if (merger1 == null) {
            merger1 = merger2;
            merger2 = new Histogram(3);
        } else {
            ByteBuffer bufferOut = ByteBuffer.allocate(merger2.getNeededByteBufferCapacity());
            merger2.encodeIntoByteBuffer(bufferOut);
            bufferOut.flip();
            Histogram interimHistogram = Histogram.decodeFromByteBuffer(bufferOut, 0);
            merger1.add(interimHistogram);
            mergeCount++;
            merger2 = new Histogram(3);
        }
    }

    public boolean compare() {
        reduce();
        return allHistogram.equals(merger1);
    }

}