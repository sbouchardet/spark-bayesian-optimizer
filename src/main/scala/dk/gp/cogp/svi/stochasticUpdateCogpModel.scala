package dk.gp.cogp.svi

import breeze.linalg.DenseMatrix
import dk.gp.cov.CovFunc
import breeze.linalg.DenseVector
import dk.gp.cov.utils.covDiag
import dk.gp.cogp.lb.LowerBound
import dk.gp.cogp.model.CogpModel
import dk.gp.cogp.model.Task
import dk.bayes.math.gaussian.MultivariateGaussian

object stochasticUpdateCogpModel {

  def apply(lb: LowerBound, tasks: Array[Task], trainCovParams: Boolean = true): LowerBound = {

    //@TODO when learning just the covParameters, at some iteration, loglik accuracy suddenly goes down, numerical stability issues? 
    //@TODO Given just gU and hypG are learned, learning first hypG then gU doesn't not converge (loglik is decreasing), why is that?
    val newU = (0 until lb.model.g.size).map { j => stochasticUpdateU(j, lb) }.toArray
    lb.model = withNewGu(newU, lb.model)

    val (newW, newWDelta) = stochasticUpdateW(lb)
    val (newBeta, newBetaDelta) = stochasticUpdateBeta(lb)

    if (trainCovParams) {
      val newHypCovG: Array[(DenseVector[Double], DenseVector[Double])] = (0 until lb.model.g.size).map { j => stochasticUpdateHypCovG(j, lb) }.toArray
      lb.model = withNewCovParamsG(newHypCovG, lb.model).copy(w = newW, wDelta = newWDelta, beta = newBeta, betaDelta = newBetaDelta)
      lb.clearCache()
    } else lb.model = lb.model.copy(w = newW, wDelta = newWDelta, beta = newBeta, betaDelta = newBetaDelta)

    val newV = (0 until lb.model.h.size).map { i => stochasticUpdateV(i, lb) }.toArray
    lb.model = withNewHu(newV, lb.model)

    if (trainCovParams) {
      val newHypCovH: Array[(DenseVector[Double], DenseVector[Double])] = (0 until lb.model.h.size).map { i => stochasticUpdateHypCovH(i, lb) }.toArray
      lb.model = withNewCovParamsH(newHypCovH, lb.model)
      lb.clearCache()
    }

    lb
  }

  private def withNewGu(newGu: Array[MultivariateGaussian], model: CogpModel): CogpModel = {
    val newG = (0 until model.g.size).map { j =>
      model.g(j).copy(u = newGu(j))
    }.toArray
    val newModel = model.copy(g = newG)

    newModel
  }

  private def withNewCovParamsG(newHypCovG: Array[(DenseVector[Double], DenseVector[Double])], model: CogpModel): CogpModel = {
    val newG = (0 until model.g.size).map { j =>
      model.g(j).copy(covFuncParams = newHypCovG(j)._1, covFuncParamsDelta = newHypCovG(j)._2)
    }.toArray
    val newModel = model.copy(g = newG)

    newModel
  }

  private def withNewHu(newHu: Array[MultivariateGaussian], model: CogpModel): CogpModel = {
    val newH = (0 until model.h.size).map { i =>
      model.h(i).copy(u = newHu(i))
    }.toArray
    val newModel = model.copy(h = newH)

    newModel
  }

  private def withNewCovParamsH(newHypCovH: Array[(DenseVector[Double], DenseVector[Double])], model: CogpModel): CogpModel = {
    val newH = (0 until model.h.size).map { i =>
      model.h(i).copy(covFuncParams = newHypCovH(i)._1, covFuncParamsDelta = newHypCovH(i)._2)
    }.toArray
    val newModel = model.copy(h = newH)

    newModel
  }
}