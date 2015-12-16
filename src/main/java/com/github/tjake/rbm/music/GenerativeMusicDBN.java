package com.github.tjake.rbm.music;

import com.github.tjake.rbm.DataSetReader;
import com.github.tjake.rbm.Layer;
import com.github.tjake.rbm.LayerFactory;
import com.github.tjake.rbm.SimpleRBM;
import com.github.tjake.rbm.StackedRBM;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public class GenerativeMusicDBN
{
    private final StackedRBM dbn;
    private final DataSetReader dataSetReader;

    public GenerativeMusicDBN(Path state, DataSetReader dataSetReader)
            throws IOException
    {
        this.dataSetReader = dataSetReader;

        this.dbn = new StackedRBM();
        dbn.load(
                new DataInputStream(
                    new BufferedInputStream(
                            new FileInputStream(state.toFile()))),
                new LayerFactory());
    }

    public void drawImages(Path saveTo) throws IOException
    {
        for (String label : dataSetReader.getLabels())
        {
            final Path toSave = saveTo.resolve(label + "_generated.png");

            if (!Files.exists(toSave))
            {
                Files.createFile(toSave);
            }

            ImageIO.write(getImage(label), "png", toSave.toFile());
        }
    }

    private BufferedImage getImage(String label)
    {
        SimpleRBM r = dbn.getInnerRBMs().get(dbn.getInnerRBMs().size() - 1);

        Layer input = new Layer(r.biasVisible.size());

        //setup input

        input.set(
                input.size() - dataSetReader.getLabels().size()
                        + dataSetReader.getLabels().indexOf(label),
                // TODO What value to set it to? 100_000 seems weirdly big...
                10_000.0f);

        input = r.activateHidden(input);
        input = r.activateVisible(input);

        for (int i = dbn.getInnerRBMs().size() - 2; i >= 0; i--)
        {
            SimpleRBM prevRbm = dbn.getInnerRBMs().get(i);

            if (input.size() > prevRbm.biasHidden.size())
            {
                float[] newInput = new float[prevRbm.biasHidden.size()];
                System.arraycopy(
                        input.get(), 0,
                        newInput, 0,
                        newInput.length);
                input = new Layer(newInput);
            }

            input = prevRbm.activateVisible(input);
        }

        return drawImage(input);
    }

    private BufferedImage drawImage(Layer input)
    {
        BufferedImage image = new BufferedImage(
                dataSetReader.getCols(),
                dataSetReader.getRows(),
                BufferedImage.TYPE_BYTE_GRAY);

        byte draw[] = new byte[input.size()];
        for (int i = 0; i < input.size(); i++)
        {
            draw[i] = (byte) Math.round(input.get(i) * 255f);
        }

        WritableRaster r = image.getRaster();
        r.setDataElements(
                0,
                0,
                dataSetReader.getCols(),
                dataSetReader.getRows(),
                draw);

        return image;
    }
}
