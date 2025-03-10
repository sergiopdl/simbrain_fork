package org.simbrain.util.table

import org.simbrain.util.sampleWithoutReplacement
import smile.math.matrix.Matrix

/**
 * Mutable table whose columns have arbitrary types.
 */
class BasicDataWrapper(
    data: MutableList<MutableList<Any?>>,
    override var columns: MutableList<Column> = inferColumns(data)
) : SimbrainDataModel() {

    override val isMutable = true

    var data: MutableList<MutableList<Any?>> = data
        set(value) {
            field = value
            columns = inferColumns(value)
        }

    /**
     * Insert column to left, unless the index is -1 (no selection) in which case it is added as the right-most column.
     */
    fun insertColumn(
        colIndex: Int,
        name: String = "New Column",
        type: Column.DataType = Column.DataType.DoubleType
    ) {
        val newColIndex = if (colIndex == -1) columnCount else colIndex
        if (colIndex in -1 until columnCount) {
            columns.add(newColIndex, Column(name, type))
            data.forEach { row -> row.add(newColIndex, null) }
            fireTableStructureChanged()
        }
    }

    override fun deleteColumn(colIndex: Int, fireEvent: Boolean) {
        if (validateColumnIndex(colIndex)) {
            data.forEach { row -> row.removeAt(colIndex) }
            if (fireEvent) {
                fireTableStructureChanged()
            }
        }
    }

    /**
     * Insert row above, unless the index is -1 (no selection) in which case it is added as the bottom.
     */
    override fun insertRow(rowIndex: Int) {
        val newRowIndex = if (rowIndex == -1) rowCount else rowIndex
        if (rowIndex in -1 until rowCount) {
            data.add(newRowIndex, MutableList(columnCount) { null })
            fireTableStructureChanged()
        }
    }

    override fun deleteRow(rowIndex: Int, fireEvent: Boolean) {
        // Allowing removal of all rows causes weird behavior, so we just aren't allowing it
        //  TODO: Empty tables should be possible.
        if (rowCount == 1) {
            return
        }
        if (validateRowIndex(rowIndex)) {
            data.removeAt(rowIndex)
            if (fireEvent) {
                fireTableStructureChanged()
            }
        }
    }

    override fun getRowCount(): Int {
        return data.size
    }

    override fun getColumnCount(): Int {
        return data[0].size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        if (validateRowIndex(rowIndex) && validateColumnIndex(columnIndex)) {
            return data[rowIndex][columnIndex]
        }
        return null
    }

    override fun setValueAt(value: Any?, rowIndex: Int, colIndex: Int) {
        if (validateRowIndex(rowIndex) && validateColumnIndex(colIndex)) {
            withValidatedValue(value, colIndex) {
                data[rowIndex][colIndex] = it
                fireTableDataChanged()
            }
        }
    }

    /**
     * Tries to parse a value into the type associated with a column, and executes a block if the parse is successful.
     */
    fun withValidatedValue(value: Any?, colIndex: Int, block: (Any) -> Unit) {
        try {
            when (columns[colIndex].type) {
                Column.DataType.DoubleType -> block(tryParsingDouble(value))
                Column.DataType.IntType -> block(tryParsingInt(value))
                Column.DataType.StringType -> if (value is String) block(value)
            }
        } catch (e: NumberFormatException) {
            println("There was a problem parsing ${value} in a column of type ${columns[colIndex].type}")
        }
    }

    /**
     * Parse the provided value into a double if possible, else throw an exception
     */
    private fun tryParsingDouble(value: Any?): Double {
        if (value is Double) {
            return value
        }
        if (value is String) {
            return value.toDouble()
        }
        if (value is Int) {
            return value.toDouble()
        }
        throw NumberFormatException("Tried to parse a value that was not double into double")
    }

    /**
     * Parse the provided value into an integer if possible, else throw an exception.
     */
    fun tryParsingInt(value: Any?): Int {
        if (value is Int) {
            return value
        }
        if (value is String) {
            return value.toInt()
        }
        if (value is Double) {
            return value.toInt()
        }
        throw NumberFormatException("Tried to parse a value that was not int into int")
    }

    override fun randomizeColumn(col: Int) {
        if (validateColumnIndex(col)) {
            // String case
            if (columns[col].type == Column.DataType.StringType) {
                randomizeStringColum(col)
            }
            // Numeric case
            (0 until rowCount).forEach {
                setValueAt(columns[col].getRandom(), it, col)
            }
            fireTableDataChanged()
        }
    }

    fun randomizeStringColum(col: Int) {
        if (validateColumnIndex(col) && columns[col].type == Column.DataType.StringType ) {
            val options = getStringColumn(col).toSet().toList()
            (0 until rowCount).forEach {
                setValueAt(options.sampleWithoutReplacement().first(), it, col)
            }
            fireTableDataChanged()
        }
    }
}

/**
 * Infer a column from a 2d array of data.
 */
private fun inferColumns(data: MutableList<MutableList<Any?>>) =
    data[0].mapIndexed { i, value ->
        createColumn("Column ${i + 1}", value)
    }.toMutableList()

fun createFrom2DArray(data: Array<out Array<out Any?>>): BasicDataWrapper {
    return BasicDataWrapper(data.map { it.toMutableList() }.toMutableList())
}

fun createFromDoubleArray(data: Array<DoubleArray>): BasicDataWrapper {
    return BasicDataWrapper(data.map { it.toMutableList() as MutableList<Any?> }.toMutableList())
}

fun createFromMatrix(data: Matrix): BasicDataWrapper {
    return BasicDataWrapper(
        data.toArray().map { it.toMutableList() as MutableList<Any?> }.toMutableList(),
        (1..data.ncols()).map { createColumn("$it", 0.0) }.toMutableList()
    )
}

fun createFromFloatArray(data: Array<FloatArray>): BasicDataWrapper {
    return BasicDataWrapper(data.map { it.toMutableList() as MutableList<Any?> }.toMutableList())
}

fun createFromColumn(data: DoubleArray): BasicDataWrapper {
    return BasicDataWrapper(data.map { mutableListOf(it as Any?) }.toMutableList())
}

fun createFromColumn(data: FloatArray): BasicDataWrapper {
    return BasicDataWrapper(data.map { mutableListOf(it as Any?) }.toMutableList())
}

fun createFromColumn(data: IntArray): BasicDataWrapper {
    return BasicDataWrapper(data.map { mutableListOf(it as Any?) }.toMutableList())
}

fun createFromColumn(data: Array<String>): BasicDataWrapper {
    return BasicDataWrapper(data.map { mutableListOf(it as Any?) }.toMutableList())
}

