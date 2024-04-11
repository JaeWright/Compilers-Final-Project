package backend

abstract class Data

object None:Data() {
    override fun toString() = "None"
}

data class IntData(val value: Int) : Data() {
    override fun toString() = value.toString()
}

data class StringData(val value: String) : Data() {
    override fun toString() = value
}

data class BooleanData(val value: Boolean) : Data() {
    override fun toString() = value.toString()
}

class FuncData(
    val name: String,
    val parameters: List<String>,
    val body: Expr,
) : Data() {
    override fun toString()
    = parameters.joinToString(", ").let {
        "$name($it)"
    }
}

class ArrayData(val array: Array<Any?>) : Data() {
    override fun toString(): String {
        return array.joinToString(", ", "[", "]")
    }
}