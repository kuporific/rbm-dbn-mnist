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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MusicDBN
{
    private final DataSetReader dataSetReader;
    private final StackedRBM rbm;
    private final StackedRBMTrainer trainer;
    private final LayerFactory layerFactory = new LayerFactory();

    public MusicDBN(DataSetReader dataSetReader)
    {
        this.dataSetReader = dataSetReader;
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
                DataItem trainItem = dataSetReader.getRandomTrainingItem();
                // TODO Why binary layer?
                inputBatch.add(new BinaryLayer(new Layer(trainItem.getData())));

                if (addLabels)
                {
                    float[] labelInput = new float[dataSetReader.getLabels().size()];
                    labelInput[dataSetReader.getLabels().indexOf(trainItem.getLabel())] = 1.0f;
                    labelBatch.add(new Layer(labelInput));
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
        Layer input = new BinaryLayer(new Layer(test.getData()));

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

    public void start(Path saveto)
    {
        boolean prevStateLoaded = false;

        if (Files.exists(saveto))
        {
            try
            {
                rbm.load(
                        new DataInputStream(
                                new BufferedInputStream(
                                        new FileInputStream(saveto.toFile()))),
                        layerFactory);
                prevStateLoaded = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        System.out.println("prevStateLoaded: " + prevStateLoaded);

        int numIterations = 500;

        rbm.setLayerFactory(layerFactory)
                // second parameter 'false' means the layer is not Gaussian.
                .addLayer(dataSetReader.getRows() * dataSetReader.getCols(), false)
                .addLayer(2000, false)
                .addLayer(500, false)
                .addLayer(200, false)
                .withCustomInput(500 + dataSetReader.getLabels().size())
                .build();

        // Second parameter 'false'/'true' is 'include labels', which only
        // applies to the third level.
        System.out.println("Training level 1");
        learn(numIterations, false, 1);
        System.out.println("Training level 2");
        learn(numIterations, false, 2);
        System.out.println("Training level 3");
        learn(numIterations, true, 3);

        try
        {
            DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(saveto.toFile())));
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
            DataItem testCase = dataSetReader.getRandomTestItem();

            Iterator<Tuple> it = evaluate(testCase);

            float[] labeled = new float[10];

            for (int i = 0; i < 2; i++)
            {
                Tuple t = it.next();

                for (int j = (t.visible.size() - 10), k = 0;
                     j < t.visible.size() && k < 10;
                     j++, k++)
                {
                    labeled[k] += t.visible.get(j);
                }
            }

            float max1 = 0.0f;
            int p1 = -1;
            int p2 = -1;

            for (int i = 0; i < labeled.length; i++)
            {
                labeled[i] /= 2;
                if (labeled[i] > max1)
                {
                    max1 = labeled[i];
                    p2 = p1;
                    p1 = i;
                }
            }

            if (p1 == dataSetReader.getLabels().indexOf(testCase.getLabel()))
            {
                numCorrect++;
            }
            else if (p2 == dataSetReader.getLabels().indexOf(testCase.getLabel()))
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
