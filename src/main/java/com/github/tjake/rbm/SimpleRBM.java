package com.github.tjake.rbm;

import com.github.tjake.util.Utilities;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class SimpleRBM
{
    // biases and weights
    public Layer biasVisible;
    public Layer biasHidden;
    public Layer[] weights;

    public LayerFactory lfactory;

    Random rand = new Random();

    protected float scale = 0.001f;
    boolean gaussianVisibles = false;

    public SimpleRBM()
    {
    }

    public SimpleRBM(
            int numVisible,
            int numHidden,
            boolean gaussianVisibles,
            LayerFactory lfactory)
    {
        this.lfactory = lfactory;

        this.gaussianVisibles = gaussianVisibles;

        // initialize nodes

        biasVisible = new Layer(numVisible);
        for (int i = 0; i < numVisible; i++)
        {
            biasVisible.set(i, (float) (scale * rand.nextGaussian()));
        }

        biasHidden = lfactory.create(numHidden);
        for (int i = 0; i < numHidden; i++)
        {
            biasHidden.set(i, (float) (scale * rand.nextGaussian()));
        }

        // initialize weights and weight change matrices
        weights = new Layer[numHidden];

        // randomly initialize weights
        for (int i = 0; i < numHidden; i++)
        {
            weights[i] = lfactory.create(numVisible);
            for (int j = 0; j < numVisible; j++)
            {
                weights[i].set(j, (float) (2 * scale * rand.nextGaussian()));
            }
        }
    }

    public void save(DataOutput dataOutput) throws IOException
    {
        dataOutput.write(LayerFactory.MAGIC);

        dataOutput.writeBoolean(gaussianVisibles);
        lfactory.save(biasVisible, dataOutput);
        lfactory.save(biasHidden, dataOutput);

        for (Layer weight : weights)
        {
            lfactory.save(weight, dataOutput);
        }
    }

    public void load(DataInput dataInput, LayerFactory lfactory)
            throws IOException
    {
        this.lfactory = lfactory;

        byte[] magic = new byte[4];
        dataInput.readFully(magic);

        if (!Arrays.equals(LayerFactory.MAGIC, magic))
        {
            throw new IOException("Bad File Format");
        }

        gaussianVisibles = dataInput.readBoolean();

        biasVisible = lfactory.load(dataInput);
        biasHidden = lfactory.load(dataInput);
        weights = new Layer[biasHidden.size()];

        for (int i = 0; i < weights.length; i++)
        {
            weights[i] = lfactory.load(dataInput);
        }
    }

    // Given visible data, return the expected hidden unit values.
    public Layer activateHidden(final Layer visible)
    {
        Layer workingHidden = new Layer(biasHidden.size());

        if (visible.size() != biasVisible.size())
        {
            throw new IllegalArgumentException("Mismatched input "
                    + visible.size()
                    + " != "
                    + biasVisible.size());
        }

        // dot product of weights and visible
        for (int i = 0; i < weights.length; i++)
        {
            for (int k = 0; k < visible.size(); k++)
            {
                workingHidden.add(i, weights[i].get(k) * visible.get(k));
            }
        }

        //Add hidden bias
        for (int i = 0; i < workingHidden.size(); i++)
        {
            float inputBias = 0.0f;

            workingHidden.set(
                    i,
                    Utilities.sigmoid(workingHidden.get(i)
                            + biasHidden.get(i)
                            + inputBias));
        }

        return workingHidden;
    }

    // Given hidden states, return the expected visible unit values.
    public Layer activateVisible(final Layer hidden)
    {
        Layer workingVisible = lfactory.create(biasVisible.size());

        // dot product of weights and hidden
        for (int k = 0; k < weights.length; k++)
        {
            for (int i = 0; i < workingVisible.size(); i++)
            {
                workingVisible.add(i, weights[k].get(i) * hidden.get(k));
            }
        }

        //Add visible bias
        for (int i = 0; i < workingVisible.size(); i++)
        {
            workingVisible.add(i, biasVisible.get(i));

            if (!gaussianVisibles)
            {
                workingVisible.set(i, Utilities.sigmoid(workingVisible.get(i)));
            }
        }

        return workingVisible;
    }

    public Iterator<Tuple> iterator(Layer visible)
    {
        return iterator(visible, new Tuple.Factory(visible));
    }

    public Iterator<Tuple> reverseIterator(Layer visible)
    {
        return reverseIterator(visible, new Tuple.Factory(visible));
    }

    public Iterator<Tuple> iterator(
            final Layer visible,
            final Tuple.Factory tfactory)
    {
        return new Iterator<Tuple>()
        {
            Layer v = visible;
            Layer h = activateHidden(v);

            public boolean hasNext()
            {
                return true;
            }

            public Tuple next()
            {
                Tuple t = tfactory.create(v, h);

                // Next updown
                v = activateVisible(Utilities.bernoulli(h));
                h = activateHidden(v);

                return t;
            }

            public void remove()
            {

            }
        };
    }

    public Iterator<Tuple> reverseIterator(
            final Layer hidden,
            final Tuple.Factory tfactory)
    {
        return new Iterator<Tuple>()
        {
            Layer v = activateVisible(Utilities.bernoulli(hidden));
            Layer h = hidden;


            public boolean hasNext()
            {
                return true;
            }

            public Tuple next()
            {
                Tuple t = tfactory.create(v, h);

                // Next downup
                v = activateVisible(Utilities.bernoulli(h));
                h = activateHidden(v);

                return t;
            }

            public void remove()
            {

            }
        };
    }

    public float freeEnergy()
    {
        float energy = 0.0f;

        for (int j = 0; j < biasHidden.size(); j++)
        {
            for (int i = 0; i < biasVisible.size(); i++)
            {
                energy -= biasVisible.get(i)
                        * biasHidden.get(j)
                        * weights[j].get(i);
            }
        }

        return energy;
    }

}