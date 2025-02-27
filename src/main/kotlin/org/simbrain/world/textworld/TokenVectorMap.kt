package org.simbrain.world.textworld

import org.simbrain.util.projection.DataPoint
import org.simbrain.util.projection.NTree
import smile.math.matrix.Matrix

/**
 * Associates string tokens with vector representations and vice-versa.
 */
class TokenVectorMap(
    tokens: List<String>,
    /**
     * Matrix whose rows correspond to vector representations of corresponding tokens.
     */
    var tokenVectorMatrix: Matrix
) {

    /**
     * Assume indices of the token list correspond to rows of the cocMatrix
     */
    var tokensMap: Map<String, Int> = tokens.mapIndexed{i, t -> t to i}.toMap()

    val size = tokensMap.size

    /**
     * N-Tree (optimized to find vectors near a given vector) associating vectors with tokens.
     */
    private val treeMap = NTree(size).apply {
        tokensMap.forEach { (token, i) ->
            add(DataPoint(tokenVectorMatrix.row(i), token))
        }
    }

    init {
        if (tokens.size != tokenVectorMatrix.nrows()) {
            throw IllegalArgumentException("token list must be same length as token vector matrix has rows")
        }
    }

    /**
     * Return the vector associated with given string or a 0 vector if none found
     */
    fun get(token: String): DoubleArray {
        val tokenIndex = tokensMap[token]
        if (tokenIndex != null) {
            return tokenVectorMatrix.row(tokenIndex)
        } else {
            // Zero array if no matching token is found
            return DoubleArray(size)
        }
    }

    /**
     * Finds the closest vector in terms of Euclidean distance, then returns the
     * String associated with it.
     */
    fun getClosestWord(key: DoubleArray): String {
        // TODO: Add a default minimum distance and if above that, return null or zero vector
        return treeMap.getClosestPoint(DataPoint(key)).label
    }
}