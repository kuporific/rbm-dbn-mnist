package com.github.tjake.rbm;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class StackedRBM extends SimpleRBM
{
    private LayerFactory layerFactory;
    private List<Integer> layerSizes;
    private List<Integer> customInputSizes;
    private List<Boolean> gaussianFlag;
    List<SimpleRBM> innerRBMs;

    public StackedRBM()
    {
        layerSizes = new ArrayList<>();
        customInputSizes = new ArrayList<>();
        gaussianFlag = new ArrayList<>();
        innerRBMs = new ArrayList<>();
    }

    public StackedRBM setLayerFactory(LayerFactory layerFactory)
    {
        this.layerFactory = layerFactory;
        return this;
    }

    public StackedRBM addLayer(int numUnits, boolean gaussian)
    {
        if (!innerRBMs.isEmpty())
        {
            throw new RuntimeException(
                    "Can't add new layers after already built");
        }

        layerSizes.add(numUnits);
        gaussianFlag.add(gaussian);
        return this;
    }

    public StackedRBM withCustomInput(int numUnits)
    {
        while (customInputSizes.size() < layerSizes.size())
        {
            customInputSizes.add(null);
        }

        customInputSizes.set(customInputSizes.size() - 1, numUnits);

        return this;
    }

    public StackedRBM build()
    {
        if (!innerRBMs.isEmpty())
        {
            return this; //already built
        }

        if (layerSizes.size() <= 1)
        {
            throw new IllegalArgumentException(
                    "Requires at least two layers to build");
        }

        for (int i = 0; i < layerSizes.size() - 1; i++)
        {
            int inputSize = layerSizes.get(i);

            if (!customInputSizes.isEmpty()
                    && customInputSizes.size() >= i
                    && customInputSizes.get(i + 1) != null)
            {
                inputSize = customInputSizes.get(i + 1);
            }

            innerRBMs.add(
                    new SimpleRBM(
                        inputSize,
                        layerSizes.get(i + 1),
                        gaussianFlag.get(i),
                        layerFactory));

            System.out.println(
                    "Added RBM " + inputSize + " -> " + layerSizes.get(i + 1));
        }

        return this;
    }

    public Layer activateHidden(Layer visible, Layer bias)
    {
        throw new UnsupportedOperationException();
    }

    public Layer activateVisible(Layer hidden, Layer bias)
    {
        throw new UnsupportedOperationException();
    }

    public Iterator<Tuple> iterator(Layer visible)
    {
        Layer input = visible;

        int stackNum = innerRBMs.size();

        for (int i = 0; i < stackNum; i++)
        {
            SimpleRBM iRBM = innerRBMs.get(i);

            if (i == (stackNum - 1))
            {
                return iRBM.iterator(visible, new Tuple.Factory(input));
            }

            visible = iRBM.activateHidden(visible);
        }

        throw new AssertionError("code bug");
    }

    @Override
    public Iterator<Tuple> reverseIterator(Layer visible)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Tuple> iterator(Layer visible, Tuple.Factory tfactory)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Tuple> reverseIterator(
            Layer visible,
            Tuple.Factory tfactory)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(DataOutput dataOutput) throws IOException
    {
        dataOutput.write(LayerFactory.MAGIC);

        dataOutput.writeInt(innerRBMs.size());

        for (SimpleRBM rbm : innerRBMs)
        {
            rbm.save(dataOutput);
        }
    }

    @Override
    public void load(DataInput dataInput, LayerFactory layerFactory)
            throws IOException
    {
        this.layerFactory = layerFactory;

        byte[] magic = new byte[4];
        dataInput.readFully(magic);

        if (!Arrays.equals(LayerFactory.MAGIC, magic))
        {
            throw new IOException("Bad File Format");
        }

        int numInner = dataInput.readInt();

        for (int i = 0; i < numInner; i++)
        {
            System.out.println("Loading rbm " + i);

            SimpleRBM loaded = new SimpleRBM();
            loaded.load(dataInput, layerFactory);
            innerRBMs.add(loaded);
        }
    }

    public List<SimpleRBM> getInnerRBMs()
    {
        return innerRBMs;
    }

    @Override
    public float freeEnergy()
    {
        float energy = 0.0f;

        for (SimpleRBM rbm : innerRBMs)
        {
            energy += rbm.freeEnergy();
        }

        return energy;
    }

    public SimpleRBM getLevel(int i)
    {
        return innerRBMs.get(i);
    }
}
