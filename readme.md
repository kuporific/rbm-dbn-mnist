Differences from source repo
============================

This fork added additional classes to analyze spectrograms of classical piano works. The audio files were taken from the [Classical Piano Midi Page](http://www.piano-midi.de/) and processed by the utility classes in the `utility` package. The `com.github.tjake.rbm.music` package contains classes based on their `com.github.tjake.rbm.minst` counterparts, but they should also be generic enough that they can also analyze the MNIST data set with little modification.

Other changes include making some of the classes in `com.github.tjake.rbm` a little more generic, as well as general formatting changes.

#### Audio File Processing Steps

1. MP3 files were downloaded from the [Classical Piano Midi Page](http://www.piano-midi.de/)
1. MP3 audio file were converted to WAV via the `ConvertAllMp3ToWav` class
1. WAV files were imported into [Virtual ANS](http://www.warmplace.ru/soft/ans/) and exported as PNG spectrograms (for audio, Virtual ANS only reads the WAV file type)
1. Spectrograms were cropped, scaled down, and partitioned by the `ScaleAndPartitionSpectrogram` class
1. those new PNG files were added to the `resources/test` and `resources/train` directories

rbm-dbn-mnist
==========

Learn more about this project from this blog post: 

http://tjake.github.com/blog/2013/02/18/resurgence-in-artificial-intelligence/

This project provides a implementation for a Restricted Boltzmann Machine and a Deep Belief Network

It uses the [MNIST handwritten dataset](http://yann.lecun.com/exdb/mnist/) to illistrate an example RBM and DBN.

Usage
=====

From source build the project with maven:

1. mvn deploy

This will build a single jar and download the mnist dataset.

2. java -jar target/rbm-dbn-mnist-0.0.1.jar 

Runs the app. shows the usage screen

````
Usage: [rbm minst-labels.gz minst-images.gz]
	   [dbn minst-images.gz minst-labels.gz dbn.bin]
	   [gen dbn.bin]
````

3. java -jar target/rbm-dbn-mnist-0.0.1.jar rbm target/minst/train-labels-idx1-ubyte.gz target/minst/train-images-idx3-ubyte.gz

Trains a single RBM with 100 hidden nodes.  Each of the hidden nodes weights are rendered alongside the test digit in blue.

![RBM Demo](http://tjake.github.com/images/MinstRBM.png)


4. java -jar target/rbm-dbn-mnist-0.0.1.jar dbn target/minst/train-labels-idx1-ubyte.gz target/minst/train-images-idx3-ubyte.gz /tmp/dbn.bin

Trains a Deep Belief Network made up of three RBMs.  It learns to match pictures of digits with their corresponding label. It takes about 10m to train but once it's done it has ~95% accuracy rate.  The trained DBN is saved to a file.

5. java -jar target/rbm-dbn-mnist-0.0.1.jar gen /tmp/dbn.bin

Takes the trained DBN from step 4. and reverses the flow, generating a visual image of a digit from a digit label.

License
=======

Copyright 2013 T Jake Luciani <jake@apache.org>

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the 'Software'), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. 

