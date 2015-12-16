package com.github.tjake.rbm.music;

import com.github.tjake.rbm.DataSetReader;
import org.javatuples.Quartet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 */
public class MusicDataSetReader implements DataSetReader
{
    private static final Random RANDOM = new Random();

    private final int rows;
    private final int cols;

    private final List<String> labels;
    private final List<MusicItem> trainingItems;
    private final List<MusicItem> testItems;

    public MusicDataSetReader(Path trainingImagesDir, Path testImagesDir)
    {
        final Set<String> labels = new HashSet<>();
        final Quartet<List<MusicItem>, List<String>, Integer, Integer>
                training = parse(trainingImagesDir);
        this.trainingItems = training.getValue0();
        labels.addAll(training.getValue1());
        rows = training.getValue2();
        cols = training.getValue3();

        final Quartet<List<MusicItem>, List<String>, Integer, Integer>
                test = parse(testImagesDir);
        this.testItems = test.getValue0();
        // labels should be the same from training and test data sets...
        labels.addAll(test.getValue1());

        this.labels = Collections.unmodifiableList(new ArrayList<>(labels));
    }

    private static Quartet<List<MusicItem>, List<String>, Integer, Integer>
    parse(Path imagesDir)
    {
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(imagesDir))
        {
            List<MusicItem> musicItems = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int rows = 0;
            int cols = 0;

            for (Path imageFilePath : paths)
            {
                final String fileName = imageFilePath.getFileName().toString();
                final String label = fileName.substring(0, fileName.indexOf('_'));

                final BufferedImage image = ImageIO.read(imageFilePath.toFile());
                final int height = image.getHeight();
                final int width = image.getWidth();
                final byte[] imageData = new byte[height * width];

                final byte[] data = ((DataBufferByte) image
                        .getRaster()
                        .getDataBuffer())
                        .getData();

                for (int i = 0, j = 0; j < data.length; i++, j += 3)
                {
                    imageData[i] = data[j];
                }

                labels.add(label);
                musicItems.add(new MusicItem(imageData, label));
                rows = height;
                cols = width;
            }

            return Quartet.with(
                    musicItems,
                    labels,
                    rows,
                    cols);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public MusicItem getRandomTrainingItem()
    {
        return trainingItems.get(RANDOM.nextInt(trainingItems.size()));
    }

    public MusicItem getRandomTestItem()
    {
        return testItems.get(RANDOM.nextInt(testItems.size()));
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public int getRows()
    {
        return rows;
    }

    public int getCols()
    {
        return cols;
    }
}
