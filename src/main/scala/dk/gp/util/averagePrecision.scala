package dk.gp.util

import com.typesafe.scalalogging.slf4j.LazyLogging
import breeze.linalg.DenseMatrix
import breeze.linalg.DenseVector

object averagePrecision extends LazyLogging {

  def apply(predicted: Array[Double], actual: Array[Double], k: Int): Double = {
    val labSet = actual.toSet

    if (labSet.nonEmpty) {
      val n = math.min(predicted.length, k)
      var i = 0
      var cnt = 0d
      var score = 0d
      while (i < n) {
        if (labSet.contains(predicted(i)) && !predicted.take(i).contains(predicted(i))) {
          cnt += 1
          score = score + cnt / (i + 1)
        }
        i += 1
      }

      score / math.min(labSet.size, k)
    } else {
      logger.info("Empty ground truth set, check input data")
      0.0
    }
  }

  def apply(predictedMat: DenseMatrix[Double], actual: DenseVector[Double], k: Int): DenseVector[Double] = {
    val apkVec = DenseVector.tabulate(predictedMat.rows) { r =>

      val predicted = predictedMat(r, ::).t.toArray
      apply(predicted, Array(actual(r)), k)
    }

    apkVec
  }
}