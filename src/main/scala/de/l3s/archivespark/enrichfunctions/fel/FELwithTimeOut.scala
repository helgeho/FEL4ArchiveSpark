/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Helge Holzmann
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.l3s.archivespark.enrichfunctions.fel

import com.yahoo.semsearch.fastlinking.FastEntityLinker
import com.yahoo.semsearch.fastlinking.hash.QuasiSuccinctEntityHash
import com.yahoo.semsearch.fastlinking.view.EmptyContext
import de.l3s.archivespark.enrich._
import de.l3s.archivespark.enrich.dataloads.TextLoad
import de.l3s.archivespark.enrich.functions.DataLoad
import it.unimi.dsi.fastutil.io.BinIO

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import java.util.concurrent.CountDownLatch

import scala.util.Try

class FELwithTimeOut private (scoreThreshold: Double, stopwords: Set[String], modelFile: String, timeout: Duration) extends DefaultFieldDependentEnrichFunc[EnrichRoot with TextLoad, String, Seq[FELAnnotation]] with SingleField[Seq[FELAnnotation]] {
  override def dependency = DataLoad(TextLoad.Field)
  override def dependencyField = TextLoad.Field

  override def resultField = "entities"

  private lazy val fel = {
    val hash = BinIO.loadObject(modelFile).asInstanceOf[QuasiSuccinctEntityHash]
    new FastEntityLinker(hash, new EmptyContext())
  }

  override def derive(source: TypedEnrichable[String], derivatives: Derivatives): Unit = {
    val fel = this.fel
    var thread: Thread = null
    val latch = new CountDownLatch(1)
    try {
      val annotations = Await.result(Future {
        thread = Thread.currentThread
        latch.countDown()
        var offset = 0
        source.get.split("\\.").flatMap{ sentence =>
          val results = fel.getResults(sentence, scoreThreshold).asScala.filter(r => !stopwords.contains(r.s.getSpan)).map{result =>
            FELAnnotation(
              result.s.getSpan,
              result.s.getStartOffset + offset,
              result.s.getEndOffset + offset,
              result.text.toString,
              result.score
            )
          }
          offset += sentence.length + 1
          results
        }
      }, timeout)
      derivatives.setNext(MultiValueEnrichable(annotations))
    } catch {
      case e: TimeoutException =>
        latch.await()
        Try { thread.interrupt() }
        throw e
    }
  }
}

object FELwithTimeOut extends FELwithTimeOut(FELConstants.DefaultScoreThreshold, FELConstants.DefaultStopWords, FELConstants.DefaultModelFile, FELConstants.DefaultTimeout) {
  def apply(scoreThreshold: Double = FELConstants.DefaultScoreThreshold, stopwords: Set[String] = FELConstants.DefaultStopWords, modelFile: String = FELConstants.DefaultModelFile, timeout: Duration = FELConstants.DefaultTimeout) = new FELwithTimeOut(scoreThreshold, stopwords, modelFile, timeout)
}
