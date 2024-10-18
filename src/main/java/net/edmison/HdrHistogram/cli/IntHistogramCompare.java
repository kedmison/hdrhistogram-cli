package net.edmison.HdrHistogram.cli;

import java.nio.ByteBuffer;

import org.HdrHistogram.IntCountsHistogram;

public class IntHistogramCompare {
    IntCountsHistogram allHistogram = new IntCountsHistogram(3);

    IntCountsHistogram merger1 = null;
    IntCountsHistogram merger2 = new IntCountsHistogram(3);
    long valueCount = 0;
    int mergeCount = 0;

    private int batchSize = 0;

    public IntHistogramCompare() {
        this(10000);
    }

    public IntHistogramCompare(int batchSize) {
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
            merger2 = new IntCountsHistogram(3);
        } else {
            ByteBuffer bufferOut = ByteBuffer.allocate(merger2.getNeededByteBufferCapacity());
            merger2.encodeIntoByteBuffer(bufferOut);
            bufferOut.flip();
            IntCountsHistogram interimHistogram = IntCountsHistogram.decodeFromByteBuffer(bufferOut, 0);
            merger1.add(interimHistogram);
            mergeCount++;
            merger2 = new IntCountsHistogram(3);
        }
    }

    public boolean compare() {
        reduce();
        return allHistogram.equals(merger1);
    }

}