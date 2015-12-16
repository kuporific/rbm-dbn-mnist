package com.github.tjake.rbm.music;

import com.github.tjake.rbm.DataSetReader;
import com.github.tjake.rbm.minst.MinstDatasetReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;

/**
 *
 */
public class Demo
{
    /**
     * 0 - state
     * 1 - output dir
     * 2 - labels set in
     * 3 = data set in
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            usage("");
        }

        final Path state = Paths.get(args[0]);

        long start = System.currentTimeMillis();

        System.out.println("Read data : " + LocalTime.now());
        final DataSetReader dataSetReader
                = new MusicDataSetReader(
                Paths.get(Demo.class.getResource("/train").getFile()),
                Paths.get(Demo.class.getResource("/test").getFile()));
//                = new MinstDatasetReader(new File(args[2]), new File(args[3]));

        System.out.println("Took " + ((System.currentTimeMillis() - start)
                / 1000) + " seconds");

        System.out.println("Training tarted at : " + LocalTime.now());

        BinaryMusicDBN dbn = new BinaryMusicDBN(dataSetReader);

        start = System.currentTimeMillis();
        dbn.start(state.toFile());
        System.out.println("Done. Took " + ((double) (System.currentTimeMillis()
                - start) / 1000) + " seconds");

        File load = new File(args[0]);

        if (!load.isFile())
        {
            usage("invalid saved file: " + args[0]);
        }

        GenerativeMusicDBN generative = new GenerativeMusicDBN(
                state,
                dataSetReader);

        generative.drawImages(Paths.get(args[1]));
    }

    private static void usage(String err)
    {
        System.err.println("Usage:\t[dbn path/to/save/state]\n"
                + "\t[gen path/to/saved/state path/to/save/generatedImages]");

        if (err != null && err.length() > 0)
        {
            System.err.println(err);
        }

        System.exit(-1);
    }
}
