package com.github.tjake.rbm.minst;

import com.github.tjake.rbm.DataItem;

/**
 * Container class that represents a Minst image and it's label
 */
public class MinstItem implements DataItem
{
    public static final int NUMBER_OF_LABELS = 10;
    public final String label;
    public final byte[] data;

    public MinstItem(String label, byte[] data)
    {
        this.label = label;
        this.data = data;
    }

    @Override
    public byte[] getData()
    {
        return data;
    }

    @Override
    public String getLabel()
    {
        return label;
    }
}
