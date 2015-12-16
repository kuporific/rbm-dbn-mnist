package com.github.tjake.rbm;

import java.util.Iterator;
import java.util.List;

public class SimpleRBMTrainer
{
    private final float momentum;
    private final float l2;
    private final Float targetSparsity;
    private final float learningRate;

    private Layer[] gWeights;
    private Layer gVisible;
    private Layer gHidden;

    public SimpleRBMTrainer(
            float momentum,
            float l2,
            Float targetSparsity,
            Float learningRate)
    {
        this.momentum = momentum;
        this.l2 = l2;
        this.targetSparsity = targetSparsity;
        this.learningRate = learningRate;
    }

    public double learn(
            final SimpleRBM rbm,
            List<Layer> inputBatch,
            boolean reverse)
    {
        final int batchSize = inputBatch.size();

        if (gWeights == null
                || gWeights.length != rbm.biasHidden.size()
                || gWeights[0].size() != rbm.biasVisible.size())
        {
            gWeights = new Layer[rbm.biasHidden.size()];
            for (int i = 0; i < gWeights.length; i++)
            {
                gWeights[i] = new Layer(rbm.biasVisible.size());
            }

            gVisible = new Layer(rbm.biasVisible.size());
            gHidden = new Layer(rbm.biasHidden.size());
        }
        else
        {
            for (Layer aGw : gWeights)
            {
                aGw.clear();
            }

            gVisible.clear();
            gHidden.clear();
        }

        // Contrastive Divergance
        for (Layer input : inputBatch)
        {
            try
            {
                Iterator<Tuple> it = reverse
                        ? rbm.reverseIterator(input)
                        : rbm.iterator(input);

                Tuple up = it.next();
                Tuple down = it.next();

                for (int i = 0; i < gWeights.length; i++)
                {
                    final Layer weight = gWeights[i];
                    for (int j = 0; j < gWeights[i].size(); j++)
                    {
                        weight.add(
                                j,
                                (up.hidden.get(i) * up.visible.get(j))
                                - (down.hidden.get(i) * down.visible.get(j)));
                    }
                }

                for (int i = 0; i < gVisible.size(); i++)
                {
                    gVisible.add(i, up.visible.get(i) - down.visible.get(i));
                }

                for (int i = 0; i < gHidden.size(); i++)
                {
                    gHidden.add(
                            i,
                            targetSparsity == null
                                    ? up.hidden.get(i) - down.hidden.get(i)
                                    : targetSparsity - up.hidden.get(i));
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }

        // Average
        for (int i = 0; i < gWeights.length; i++)
        {
            for (int j = 0; j < gWeights[i].size(); j++)
            {
                float x = gWeights[i].get(j) / batchSize * (1 - momentum);
                x = x + momentum * (x - l2 * rbm.weights[i].get(j));

                rbm.weights[i].add(j, learningRate * x);
            }
        }

        double error = 0.0;

        for (int i = 0; i < gVisible.size(); i++)
        {
            float x = gVisible.get(i) / batchSize;

            error += x * x;

            x = x * (1 - momentum);
            x = x + (momentum * (x * rbm.biasVisible.get(i)));

            rbm.biasVisible.add(i, learningRate * x);
        }

        error = Math.sqrt(error / gVisible.size());

        if (targetSparsity != null)
        {
            for (int i = 0; i < gHidden.size(); i++)
            {
                gHidden.set(i, targetSparsity - gHidden.get(i) / batchSize);
            }
        }
        else
        {
            for (int i = 0; i < gHidden.size(); i++)
            {
                float x = gHidden.get(i) / batchSize * (1 - momentum);
                x = x + momentum * x * rbm.biasHidden.get(i);

                rbm.biasHidden.add(i, learningRate * x);
            }
        }

        return error;
    }
}
