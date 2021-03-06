package com.github.tjake.rbm.music;

import com.github.tjake.rbm.BinaryLayer;
import com.github.tjake.rbm.DataItem;
import com.github.tjake.rbm.DataSetReader;
import com.github.tjake.rbm.Layer;
import com.github.tjake.rbm.LayerFactory;
import com.github.tjake.rbm.SimpleRBM;
import com.github.tjake.rbm.StackedRBM;
import com.github.tjake.rbm.StackedRBMTrainer;
import com.github.tjake.rbm.Tuple;
import com.github.tjake.rbm.minst.MinstItem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class BinaryMusicDBN
{
    private final DataSetReader dr;
    private final StackedRBM rbm;
    private final StackedRBMTrainer trainer;
    private final LayerFactory layerFactory = new LayerFactory();

    public BinaryMusicDBN(DataSetReader dr)
    {
        this.dr = dr;
        rbm = new StackedRBM();
        trainer = new StackedRBMTrainer(rbm, 0.5f, 0.001f, 0.2f, 0.2f);
    }

    private void learn(int iterations, boolean addLabels, int stopAt)
    {
        final int learnSize = 30;
        // Get random input
        final List<Layer> inputBatch = new ArrayList<>(learnSize);
        final List<Layer> labelBatch = addLabels
                ? new ArrayList<>(learnSize)
                : Collections.<Layer>emptyList();

        for (int p = 0; p < iterations; p++)
        {
            inputBatch.clear();
            labelBatch.clear();

            for (int j = 0; j < learnSize; j++)
            {
                DataItem trainItem = dr.getRandomTrainingItem();
                inputBatch.add(
                        new BinaryLayer(
                                layerFactory.create(trainItem.getData())));

                if (addLabels)
                {
                    float[] labelInput = new float[dr.getLabels().size()];
                    labelInput[dr.getLabels().indexOf(trainItem.getLabel())] = 1.0f;
                    labelBatch.add(layerFactory.create(labelInput));
                }
            }

            double error = trainer.learn(inputBatch, labelBatch, stopAt);

            if (p % 100 == 0)
            {
                System.out.println(
                        "Iteration " + p
                                + ", Error = " + error
                                + ", Energy = " + rbm.freeEnergy());
            }
        }
    }

    private Iterator<Tuple> evaluate(DataItem test)
    {
        Layer input = new BinaryLayer(layerFactory.create(test.getData()));

        int stackNum = rbm.getInnerRBMs().size();

        for (int i = 0; i < stackNum; i++)
        {
            SimpleRBM iRBM = rbm.getInnerRBMs().get(i);

            if (iRBM.biasVisible.size() > input.size())
            {
                Layer newInput = new Layer(iRBM.biasVisible.size());

                System.arraycopy(
                        input.get(), 0,
                        newInput.get(), 0,
                        input.size());

                for (int j = input.size(); j < newInput.size(); j++)
                {
                    newInput.set(j, 0.1f);
                }

                input = newInput;
            }

            if (i == (stackNum - 1))
            {
                return iRBM.iterator(input);
            }

            input = iRBM.activateHidden(input);
        }

        throw new IllegalStateException(
                "Should have returned before reaching here...");
    }

    public void start(File saveto)
    {
        boolean prevStateLoaded = false;

        if (saveto.exists())
        {
            try
            {
                rbm.load(
                        new DataInputStream(
                                new BufferedInputStream(
                                        new FileInputStream(saveto))),
                        layerFactory);
                prevStateLoaded = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        System.out.println("prevStateLoaded: " + prevStateLoaded);

        if (!prevStateLoaded)
        {
            rbm.setLayerFactory(layerFactory)
                    .addLayer(dr.getRows() * dr.getCols(), false)
                    .addLayer(500, false)
                    .addLayer(500, false)
                    .addLayer(2000, false)
                    .withCustomInput(500 + dr.getLabels().size())
                    .build();
        }

        final int numIterations = 400;
        System.out.println("Training level 1 " + LocalTime.now());
        learn(numIterations, false, 1);
        System.out.println("Training level 2 " + LocalTime.now());
        learn(numIterations, false, 2);
        System.out.println("Training level 3 " + LocalTime.now());
        learn(numIterations, true, 3);

        try
        {
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(saveto)));
            rbm.save(out);

            out.flush();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        double numCorrect = 0;
        double numWrong = 0;
        double numAlmost = 0.0;

        final int toTest = 1000;
        for (int count = 0; count < toTest; count++)
        {
            DataItem testCase = dr.getRandomTestItem();

            Iterator<Tuple> it = evaluate(testCase);

            float[] labeld = new float[dr.getLabels().size()];

            for (int i = 0; i < 2; i++)
            {
                Tuple t = it.next();

                for (int j = (t.visible.size() - dr.getLabels().size()), k = 0;
                     j < t.visible.size() && k < dr.getLabels().size();
                     j++, k++)
                {
                    labeld[k] += t.visible.get(j);
                }
            }

            float max1 = 0.0f;
            int p1 = -1;
            int p2 = -1;

            for (int i = 0; i < labeld.length; i++)
            {
                labeld[i] /= 2;
                if (labeld[i] > max1)
                {
                    max1 = labeld[i];
                    p2 = p1;
                    p1 = i;
                }
            }

            if (p1 == dr.getLabels().indexOf(testCase.getLabel()))
            {
                numCorrect++;
            }
            else if (p2 == dr.getLabels().indexOf(testCase.getLabel()))
            {
                numAlmost++;
            }
            else
            {
                numWrong++;
            }
        }

        System.out.println("Error Rate = "
                + ((numWrong / (numAlmost + numCorrect + numWrong)) * 100));
    }
}