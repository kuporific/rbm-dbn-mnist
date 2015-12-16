package com.github.tjake.rbm.minst;

import com.github.tjake.rbm.DataSetReader;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * Reads the Minst image data from
 */
public class MinstDatasetReader implements Enumeration<MinstItem>, DataSetReader
{
    final DataInputStream labelsBuf;
    final DataInputStream imagesBuf;

    Random r = new Random();

    final SortedMap<String, List<MinstItem>> trainingSet = new TreeMap<>();
    final SortedMap<String, List<MinstItem>> testSet = new TreeMap<>();

    int rows = 0;
    int cols = 0;
    int count = 0;
    int current = 0;

    public MinstDatasetReader(File labelsFile, File imagesFile)
    {
        try
        {
            labelsBuf = new DataInputStream(new GZIPInputStream(new FileInputStream(labelsFile)));
            imagesBuf = new DataInputStream(new GZIPInputStream(new FileInputStream(imagesFile)));

            verify();
            createTrainingSet();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

    public void createTrainingSet() {
        boolean done = false;

        while (!done || !hasMoreElements()) {
            MinstItem i = nextElement();

            if (r.nextDouble() > 0.3)
            {
                List<MinstItem> l = testSet.computeIfAbsent(
                        i.label,
                        s -> new ArrayList<>());
                l.add(i);
                testSet.put(i.label, l);
            }
            else
            {
                List<MinstItem> l = trainingSet.computeIfAbsent(
                        i.label,
                        s -> new ArrayList<>());

                l.add(i);

                trainingSet.put(i.label, l);
            }

            if (trainingSet.isEmpty())
                continue;

            boolean isDone = true;
            for (Map.Entry<String, List<MinstItem>> entry : trainingSet.entrySet()) {
                if (entry.getValue().size() < 100) {
                    isDone = false;
                    break;
                }
            }

            done = isDone;
        }
    }

    @Override
    public MinstItem getRandomTestItem()
    {
        List<MinstItem> list = testSet.get(String.valueOf(r.nextInt(MinstItem.NUMBER_OF_LABELS)));
        return list.get(r.nextInt(list.size()));
    }

    @Override
    public MinstItem getRandomTrainingItem()
    {
        List<MinstItem> list = trainingSet.get(String.valueOf(r.nextInt(MinstItem.NUMBER_OF_LABELS)));
        return list.get(r.nextInt(list.size()));
    }

    private void verify() throws IOException
    {
        int magic = labelsBuf.readInt();
        int labelCount = labelsBuf.readInt();

        System.err.println("Labels magic=" + magic + ", count=" + labelCount);

        magic = imagesBuf.readInt();
        int imageCount = imagesBuf.readInt();
        rows = imagesBuf.readInt();
        cols = imagesBuf.readInt();

        System.err.println("Images magic=" + magic + " count=" + imageCount + " rows=" + rows + " cols=" + cols);

        if (labelCount != imageCount)
            throw new IOException("Label Image count mismatch");

        count = imageCount;
    }

    public boolean hasMoreElements()
    {
        return current < count;
    }

    public MinstItem nextElement()
    {
        try
        {
            final byte[] data = new byte[rows * cols];

            for (int i = 0; i < rows * cols; i++)
            {
                data[i] = saturatedCast(imagesBuf.readUnsignedByte());
            }

            return new MinstItem(
                    Integer.toString(labelsBuf.readUnsignedByte()),
                    data);
        }
        catch (IOException e)
        {
            current = count;
            throw new IOError(e);
        }
        finally
        {
            current++;
        }
    }

    @Override
    public List<String> getLabels()
    {
        return new ArrayList<>(trainingSet.keySet());
    }

    @Override
    public int getRows()
    {
        return rows;
    }

    @Override
    public int getCols()
    {
        return cols;
    }

    private byte saturatedCast(int integer)
    {
        if (integer > Byte.MAX_VALUE)
        {
            return Byte.MAX_VALUE;
        }
        else if (integer < Byte.MIN_VALUE)
        {
            return Byte.MIN_VALUE;
        }
        else
        {
            return (byte) integer;
        }
    }
}
