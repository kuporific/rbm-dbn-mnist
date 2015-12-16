package com.github.tjake.rbm;

import java.util.ArrayList;
import java.util.List;

public class StackedRBMTrainer
{
    private final StackedRBM stackedRBM;
    private final SimpleRBMTrainer inputTrainer;
    final float momentum;
    final Float targetSparsity;
    float learningRate;

    public StackedRBMTrainer(
            StackedRBM stackedRBM,
            float momentum,
            float l2,
            Float targetSparsity,
            float learningRate)
    {
        this.stackedRBM = stackedRBM;
        this.momentum = momentum;
        this.targetSparsity = targetSparsity;
        this.learningRate = learningRate;

        inputTrainer = new SimpleRBMTrainer(
                momentum,
                l2,
                targetSparsity,
                learningRate);
    }

    //Starts at the bottom of the DBN and uses the output of one RBM as the input of
    //the next.  This continues till it hits stopAt.  Then it trains the RBM with the
    //mutated input batch.  It also allows a second batch to be appended to a input batch
    //So you can combine a deep RBM feature with a second input.
    //
    //An example being features of a digit picture combined with the digit label.
    public double learn(
            List<Layer> bottomBatch,
            List<Layer> topBatch,
            int stopAt)
    {
        if (topBatch != null
                && !topBatch.isEmpty()
                && topBatch.size() != bottomBatch.size())
        {
            throw new IllegalArgumentException("TopBatch != BottomBatch");
        }

        if (stopAt < 0 || stopAt > stackedRBM.innerRBMs.size())
        {
            throw new IllegalArgumentException("Invalid stopAt");
        }

        List<Layer> nextInputs = new ArrayList<>(bottomBatch);

        for (int i = 0; i < stopAt; i++)
        {
            //At stopping point do actual learning
            if (i == stopAt - 1)
            {
                return inputTrainer.learn(
                        stackedRBM.innerRBMs.get(i),
                        nextInputs,
                        false);
            }

            //Use the hidden of this layer as the inputs of the next layer
            for (int j = 0; j < nextInputs.size(); j++)
            {
                Layer next = stackedRBM
                        .getLevel(i)
                        .activateHidden(nextInputs.get(j));

                if (topBatch != null && !topBatch.isEmpty() && i == stopAt - 2)
                {
                    float[] nextConcat = new float[next.size() + topBatch.get(j).size()];
                    System.arraycopy(next.get(), 0, nextConcat, 0, next.size());
                    System.arraycopy(
                            topBatch.get(j).get(), 0,
                            nextConcat, next.size(),
                            topBatch.get(j).size());

                    next = new Layer(nextConcat);
                }
                else
                {
                    next = stackedRBM
                        .getLevel(i)
                        .activateHidden(nextInputs.get(j));
                }

                nextInputs.set(j, next);
            }
        }

        throw new IllegalStateException("Didn't find a level top stop at");
    }
}
