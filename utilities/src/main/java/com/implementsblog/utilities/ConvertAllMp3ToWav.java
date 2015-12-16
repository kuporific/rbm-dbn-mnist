package com.implementsblog.utilities;

import javazoom.jl.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public class ConvertAllMp3ToWav
{
    private static final Logger log
            = LoggerFactory.getLogger(ConvertAllMp3ToWav.class);

    public static void main(String[] args)
    {
        Converter converter = new Converter();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                Paths.get(args[0]),
                file -> file.getFileName().toString().endsWith(".mp3")))
        {
            for (Path file : stream)
            {
                final String mp3FileName = file.toString();
                final Path wavFile = Paths.get(
                        mp3FileName.substring(0, mp3FileName.indexOf('.'))
                                + ".wav");

                if (!Files.exists(wavFile))
                {
                    try
                    {
                        log.info("Start converstion of {} ...", mp3FileName);

                        final String wavFileName = wavFile.toString();
                        converter.convert(mp3FileName, wavFileName);

                        log.info(
                                "Converted {} to {}",
                                mp3FileName,
                                wavFileName);
                    }
                    catch (Exception e)
                    {
                        log.warn("Failed to convert " + mp3FileName, e);
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
