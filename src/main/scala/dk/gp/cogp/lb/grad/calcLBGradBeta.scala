package dk.gp.cogp.lb.grad

import breeze.linalg.DenseMatrix
import breeze.linalg.DenseVector
import breeze.linalg.diag
import breeze.linalg.sum
import breeze.linalg.trace
import breeze.numerics.pow
import dk.gp.cogp.lb.LowerBound
import dk.gp.cov.utils.covDiag
import dk.gp.cogp.lb.wAm
import dk.gp.math.diagProd

object calcLBGradBeta {

  def apply(lb: LowerBound): DenseVector[Double] = {

    val dBeta = lb.model.beta.mapPairs {
      case (i, beta) =>
        dLogN(i, lb) - dTildeQ(i, lb) - dTildeP(i, lb) - dTraceQ(i, lb) - dTraceP(i, lb)
    }

    dBeta
  }

  private def dTraceP(i: Int, lb: LowerBound): Double = {

    val Ai = lb.calcAi(i) //@TODO Ai term is computed many times across different lb derivatives, the same with Aj
    val lambdaI = Ai.t * Ai
    val v = lb.model.h(i).u

    val dTraceP = 0.5 * sum(diagProd(v.v,lambdaI))
    dTraceP
  }

  private def dLogN(i: Int, lb: LowerBound): Double = {

    val beta = lb.model.beta(i)
    val Ai = lb.calcAi(i)
    val y = lb.yi(i)

    val yTerm = y - wAm(i, lb) - Ai * lb.model.h(i).u.m
    val dLogN = (0.5 * y.size) / beta - 0.5 * sum(pow(yTerm, 2))

    dLogN
  }

  private def dTildeP(i: Int, lb: LowerBound): Double =  0.5 * lb.tildeP(i)

  private def dTraceQ(i: Int, lb: LowerBound) = {
    val dTraceQ = (0 until lb.model.g.size).map { j =>

      val Aj = lb.Aj(i, j)
      val lambdaJ = lb.lambdaJ(i, j)
      val gU = lb.model.g(j).u

      pow(lb.model.w(i, j), 2) * sum(diagProd(gU.v,lambdaJ))
    }.sum

    0.5 * dTraceQ
  }

  private def dTildeQ(i: Int, lb: LowerBound): Double = {

    val dTildeQ = (0 until lb.model.g.size).map { j => pow(lb.model.w(i, j), 2) * lb.tildeQ(i,j)}.sum
    0.5 * dTildeQ
  }

}