package com.github.tjake.rbm;

import java.util.List;

/**
 *
 */
public interface DataSetReader
{
    DataItem getRandomTrainingItem();
    DataItem getRandomTestItem();
    List<String> getLabels();
    int getRows();
    int getCols();
}
