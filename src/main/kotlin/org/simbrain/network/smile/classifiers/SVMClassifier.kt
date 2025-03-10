package org.simbrain.network.smile.classifiers

import org.simbrain.network.smile.ClassificationAlgorithm
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.util.UserParameter
import org.simbrain.util.getOneHot
import smile.classification.Classifier
import smile.classification.SVM
import smile.math.kernel.PolynomialKernel
import smile.math.matrix.Matrix
import smile.validation.metric.Accuracy

/**
 * Wrapper for Smile SVM Classifier.
 *
 */
class SVMClassifier @JvmOverloads constructor(inputSize: Int = 4, outputSize: Int = 4):
    ClassificationAlgorithm(inputSize, outputSize) {

    init {
        // SVM Assumes targets are -1 or 1
        trainingData.labelEncoding = ClassificationDataset.LabelEncoding.Bipolar
    }

    // TODO: Provide separate object for selecting Kernel
    @UserParameter(label = "Polynomial Kernel Degree", order = 20)
    var kernelDegree = 2

    @UserParameter(label = "Soft margin penalty parameter", order = 30)
    var C = 1000.0

    @UserParameter(label = "Tolerance of convergence test", order = 40)
    var tolerance = 1E-3

    override var model: Classifier<DoubleArray>? = null

    override fun copy(): ClassificationAlgorithm {
        return SVMClassifier(inputSize, outputSize).also {
            it.kernelDegree = kernelDegree
            it.C = C
            it.tolerance = tolerance
        }
    }

    override val name = "Support Vector Machine"

    override fun fit(inputs: Array<DoubleArray>, targets: IntArray) {
        model = SVM.fit(inputs, targets, PolynomialKernel(kernelDegree), C, tolerance)
        setAccuracyLabel(Accuracy.of(targets, model?.predict(inputs)))
    }

    override fun predict(input: DoubleArray): Int {
        // Todo: null to -1 seems problematic
        return model?.predict(input) ?: -1
    }

    override fun getOutputVector(result: Int): Matrix {
        return getOneHot(result, outputSize)
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ClassificationAlgorithm.getTypes()
        }
    }

}