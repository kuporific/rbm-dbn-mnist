package com.github.tjake.rbm.music;

import com.github.tjake.rbm.DataItem;

import java.util.Objects;

/**
 *
 */
public class MusicItem implements DataItem
{
    private final byte[] data;
    private final String label;

    public MusicItem(byte[] data, String label)
    {
        this.data = Objects.requireNonNull(data);
        this.label = label;
    }

    public byte[] getData()
    {
        return data;
    }

    public String getLabel()
    {
        return label;
    }
}
