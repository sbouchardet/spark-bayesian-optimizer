package dk.gp.mtgpc

import breeze.optimize.DiffFunction
import breeze.linalg.DenseVector
import breeze.linalg.DenseMatrix
import dk.gp.cov.CovFunc
import dk.gp.gpc.GpcModel
import breeze.linalg._
import dk.gp.gpc.util.GpcApproxDiffFunction

case class MtGpcDiffFunction(initialModel: MtgpcModel) extends DiffFunction[DenseVector[Double]] {

  private val gpDiffFunctions = createGpDiffFunctions()

  def calculate(params: DenseVector[Double]): (Double, DenseVector[Double]) = {
    val loglikWithD = gpDiffFunctions.par.map(gpDiffFunc => gpDiffFunc.calculate(params)).toList

    val totalLoglik = loglikWithD.map(_._1).sum
    val totalGrad = sum(loglikWithD.map(_._2))

    (totalLoglik, totalGrad)
  }

  private def createGpDiffFunctions(): Seq[GpcApproxDiffFunction] = {

    val taskIds = initialModel.x(::, 0).toArray.distinct

    val gpDiffFunctions = taskIds.map { cId =>
      val idx = initialModel.x(::, 0).findAll { x => x == cId }
      val taskX = initialModel.x(idx, ::).toDenseMatrix
      val taskY = initialModel.y(idx).toDenseVector

      val model = GpcModel(taskX, taskY, initialModel.covFunc, initialModel.covFuncParams, initialModel.gpMean)
      val gpDiffFunction = GpcApproxDiffFunction(model)
      gpDiffFunction
    }
    gpDiffFunctions
  }
}