package net.edmison.HdrHistogram.cli;

import java.nio.ByteBuffer;

import org.HdrHistogram.DoubleHistogram;

public class DoubleHistogramCompare {
    DoubleHistogram allHistogram = new DoubleHistogram(3);

    DoubleHistogram merger1 = null;
    DoubleHistogram merger2 = new DoubleHistogram(3);
    long valueCount = 0;
    int mergeCount = 0;

    private int batchSize = 0;

    public DoubleHistogramCompare() {
        this(10000);
    }

    public DoubleHistogramCompare(int batchSize) {
        this.batchSize = batchSize;
    }

    public void accrue(double value) {
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
            merger2 = new DoubleHistogram(3);
        } else {
            ByteBuffer bufferOut = ByteBuffer.allocate(merger2.getNeededByteBufferCapacity());
            merger2.encodeIntoByteBuffer(bufferOut);
            bufferOut.flip();
            DoubleHistogram interimHistogram = DoubleHistogram.decodeFromByteBuffer(bufferOut, 0);
            merger1.add(interimHistogram);
            mergeCount++;
            merger2 = new DoubleHistogram(3);
        }
    }

    public boolean compare() {
        reduce();
        System.out.println("Min and max values are " + allHistogram.getMinValue()
                + " and " + allHistogram.getMaxValue());
        return allHistogram.equals(merger1);
    }

}