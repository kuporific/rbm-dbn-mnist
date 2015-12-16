package com.implementsblog.utilities;

import net.coobird.thumbnailator.Thumbnails;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 *
 */
public class ScaleAndPartitionSpectrogram
{
    private static final Logger log
            = LoggerFactory.getLogger(ScaleAndPartitionSpectrogram.class);

    private static final int DOWN_FROM_TOP = 100;
    private static final int UP_FROM_BOTTOM = 80;
    private static final double SCALE = 0.75;
    private static final int PARTITION_WIDTH = 500;
    private static final String IMG_FILE_TYPE = "png";
    private static final String PARTITION = "partition";

    public static void main(String[] args)
    {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(args[0]),
                file -> {
                    final String fileName = file.getFileName().toString();
                    return fileName.endsWith(".png")
                            && !fileName.contains(PARTITION);
                }))
        {
            for (Path file : stream)
            {
                final BufferedImage image = ImageIO.read(file.toFile());
                int type = image.getType();

                final String fileName = file.getFileName().toString();

                final String fileNameWithoutExt
                        = fileName.substring(0, fileName.lastIndexOf("."));

                partitionHorizontal(
                        resize(
                                // Strip the top and bottom from the image.
                                // It does have much sound data.
                                image.getSubimage(
                                        0,
                                        DOWN_FROM_TOP,
                                        image.getWidth(),
                                        image.getHeight() - DOWN_FROM_TOP - UP_FROM_BOTTOM),
                                SCALE),
                        PARTITION_WIDTH,
                        fileNameWithoutExt)
                        .forEach(triplet ->
                        {
                            final String name
                                    // Original file name without extension
                                    = triplet.getValue2()
                                    // with partition number
                                    + "_" + PARTITION + triplet.getValue1()
                                    // new file type.
                                    + "." + IMG_FILE_TYPE;
                            try
                            {
                                ImageIO.write(
                                        triplet.getValue0(),
                                        IMG_FILE_TYPE,
                                        Paths.get(args[0], name).toFile());
                            }
                            catch (IOException e)
                            {
                                log.warn(
                                        "Failed to write scaled and "
                                                + "partitioned image, "
                                                + name,
                                        e);
                            }
                        });
            }
        }
        catch (IOException e)
        {
            log.error("Failed to resize images.", e);
        }
    }

    private static BufferedImage resize(BufferedImage image, double scale)
            throws IOException
    {
        return Thumbnails.of(image)
                .height((int) (image.getHeight() * scale))
                .asBufferedImage();
    }

    /**
     * Partitions the given image into equal sized images of the desired width.
     *
     * @param image the image to partition.
     * @param partitionWidth the width of each partition (any excess at the end
     *     of the image is dropped).
     * @param fileName the name of the file from which the image originated
     *     without the file extension type.
     *
     * @return A triplet of
     *     <ol>
     *         <li>Partitioned Image</li>
     *         <li>Partition Number</li>
     *         <li>Original File Name</li>
     *     </ol>
     */
    private static Stream<Triplet<BufferedImage, Integer, String>>
    partitionHorizontal(
            BufferedImage image,
            int partitionWidth,
            String fileName)
    {
        final Stream.Builder<Triplet<BufferedImage, Integer, String>> builder
                = Stream.builder();

        for (int w = 0; w < image.getWidth() - partitionWidth; w += partitionWidth)
        {
            builder.accept(
                    Triplet.with(
                            image.getSubimage(
                                    w,
                                    0,
                                    partitionWidth,
                                    image.getHeight()),
                            w / partitionWidth,
                            fileName
                    ));
        }

        return builder.build();
    }
}
