## FEL 4 [ArchiveSpark](https://github.com/helgeho/ArchiveSpark)

This *enrich function* enables the use of Yahoo's [Fast Entity Linker Toolkit](https://github.com/yahoo/FEL) (FEL) with [ArchiveSpark](https://github.com/helgeho/ArchiveSpark).

## Usage

We provide two *enrich functions* of a similar purpose. Both run FEL and enrich the input records with the recognized named entities. However, we found that this process in some cases takes very long (see *Issues* below), which can be problematic when running it on very large collections. For that reason, besides the regular `FEL` we have included a second one, called `FELwithTimeOut`, which runs the annotation process in a separate thread and interrupts it if it is running too long (after 10 seconds by default).

First, you will need to disseminate the model file (by default `english-nov15.hash`) to the nodes of your cluster. This can be done through the `SparkContext` (`sc`), e.g. from an HDFS location:

```scala
sc.addFile("hdfs:///path/to/english-nov15.hash")
```

Next, create an instance of the *enrich function*, e.g., to run it on the text of a webpage:

```scala
val fel = FEL.on(HtmlText)
```

or with paramters:

```scala
val fel = FEL(scoreThreshold = -5, modelFile = "english-nov15.hash").on(HtmlText)
```

`FEL` or `FELwithTimeOut` can be parameterized as follows:

```scala
FEL(scoreThreshold: Double, stopwords: Set[String], modelFile: String)
  
FELwithTimeOut(scoreThreshold: Double, stopwords: Set[String], modelFile: String, timeout: Duration)
```

The default values are defined in [FELConstants](src/main/scala/de/l3s/archivespark/enrichfunctions/fel/FELConstants.scala).

Finally, you can enrich your dataset (ArchiveSpark RDD) with FEL:

```scala
val enriched = rdd.enrich(fel)
```

The enriched annotations are of type [FELAnnotation](src/main/scala/de/l3s/archivespark/enrichfunctions/fel/FELAnnotation.scala).

## Build and install

To build this project [*Maven*](https://maven.apache.org) as well as [*SBT*](http://www.scala-sbt.org) needs to be installed.
As it depends on *FEL*, you will first need to install that locally, before you can build *FEL4ArchiveSpark*:

```scala
git clone https://github.com/yahoo/FEL.git
cd FEL
mvn install
```

Now, the required JAR can be created using SBT:

```scala
git clone https://github.com/helgeho/FEL4ArchiveSpark.git
cd FEL4ArchiveSpark
sbt assembly
```

The compiled JAR file under target/scala-2.11/fel4archivespark-assembly-1.0.0.jar includes all required dependencies (except for ArchiveSpark itself).
To use it (as shown under *Usage*, see above), you will need to add it to your classpath together with ArchiveSpark. Please make sure to add this JAR before all other JARs in order to avoid conflicts.

## Issues

We found that in some cases FEL takes extremely long for no obvious reason and sometimes even appears to have got stuck. One example record where we encountered this issue can be found under [long-running-example.txt](long-running-example.txt) (in ArchiveSpark's JSON output format).

One workaround that solved this issue in most cases in our experiments was to split up texts into smaller chunks, e.g., sentences.
Therefore, this enrich function splits texts on periods ('.') before applying FEL. This should not cause any problems in most cases as entity names rarely contain periods. The position of an entity is added up accordingly, so that the reported positions still relate to the full text.

### License

The MIT License (MIT)

Copyright (c) 2017 Helge Holzmann

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
