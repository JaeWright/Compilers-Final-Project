package backend

abstract class Expr {
    abstract fun eval(runtime:Runtime):Data
}

class NoneExpr(): Expr() {
    override fun eval(runtime:Runtime) = None
}

class AssignmentExpr(val variable: String, val value: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val data = value.eval(runtime)
        runtime.symbolTable.put(variable, data)
        //println("Debug: Assigned value $data to variable $variable")
        return data
    }
}

class Block(val exprList: List<Expr>): Expr() {
    override fun eval(runtime:Runtime): Data {
        var result:Data = None
        exprList.forEach {
            result = it.eval(runtime)
        }
        return result
    }
}



class PrintExpr(val value: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val data = value.eval(runtime)
        println(data.toString())
        return data
    }
}

class StringExpr(val value: String) : Expr() {
    override fun eval(runtime: Runtime): Data {
        //println("Debug: ran StringExpr with string: $String")
        return StringData(value)
    }
}

class IdentifierExpr(val identifier: String) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val value = runtime.symbolTable[identifier]
        //runtime.printSymbolTable()
        //val symbolTableLength = runtime.symbolTable.size
        //println("Debug: Identifier '$identifier' has value: $value with tableSize $symbolTableLength")
        return value ?: throw RuntimeException("Identifier '$identifier' is not defined")
    }
}

class ConcatenationExpr(val left: Expr, val right: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val leftData = left.eval(runtime)
        val rightData = right.eval(runtime)
        if (leftData is StringData && rightData is StringData) {
            return StringData(leftData.value + rightData.value)
        } else {
            throw RuntimeException("Both operands of ++ operator must be strings")
        }
    }
}


class StringLiteral(val lexeme:String): Expr() {
    override fun eval(runtime:Runtime): Data
    = StringData(lexeme)
}

class IntLiteral(val value:String): Expr() {
    override fun eval(runtime:Runtime): Data{
        val intValue = value.toInt()
        return IntData(intValue)
    }
    
}


class MultiplyExpr(val left: Expr, val right: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        //println("Evaluating MultiplyExpr")

        val leftData = left.eval(runtime)
        //println("Left operand evaluated to: $leftData of type ${leftData.javaClass.simpleName}")

        val rightData = right.eval(runtime)
        //println("Right operand evaluated to: $rightData of type ${rightData.javaClass.simpleName}")

        return when {
            leftData is StringData && rightData is IntData -> {
                val result = leftData.value.repeat(rightData.value)
                //println("Multiplying string '${leftData.value}' by ${rightData.value} times, result: $result")
                StringData(result)
            }
            leftData is IntData && rightData is IntData -> {
                val result = leftData.value * rightData.value
                //println("Multiplying integer ${leftData.value} by ${rightData.value}, result: $result")
                IntData(result)
            }
            else -> {
                //println("Error: MismatchedInput in MultiplyExpr: leftData is ${leftData.javaClass.name}, rightData is ${rightData.javaClass.name}")
                throw RuntimeException("MismatchedInput")
            }
        }
    }
}

enum class Operator {
    Add,
    Sub,
    Mul,
    Div
}

class Arithmetics(
    val op:Operator,
    val left:Expr,
    val right:Expr
): Expr() {
    override fun eval(runtime:Runtime): Data {
        val x = left.eval(runtime)
        val y = right.eval(runtime)
        if(x !is IntData || y !is IntData) {
            throw Exception("cannot handle non-integer")
        }
        return IntData(
            when(op) {
                Operator.Add -> x.value + y.value
                Operator.Sub -> x.value - y.value
                Operator.Mul -> x.value * y.value
                Operator.Div -> {
                    if(y.value != 0) {
                        x.value / y.value
                    } else {
                        throw Exception("cannot divide by zero")
                    }
                }
                else -> throw Exception("Unknown operator")
            }
        )
    }
}

class ForLoopExpr(private val variable: String, private val start: Int, private val end: Int, private val body: List<Expr>): Expr() {
    override fun eval(runtime: Runtime): Data {
        for (i in start..end) {
            runtime.symbolTable[variable] = IntData(i)
            // Evaluate the body of the loop
            for (expr in body) {
                expr.eval(runtime)
            }
        }
        // Sum calculation should be outside the loop
        var sum = runtime.symbolTable.values.filterIsInstance<IntData>().sumBy { it.value }
        return IntData(sum)
    }
}


class Declare(
    val name: String,
    val parameters: List<String>,
    val body: Expr,
) : Expr() {
    override fun eval(runtime:Runtime):Data {
        val funcdata = FuncData(name, parameters, body)
        runtime.symbolTable[name] = funcdata
        //println("func made")
        return None
    }
}

class Invoke(
    val funcname: String,
    val arguments: List<Expr>,
) : Expr() {
    override fun eval(runtime:Runtime): Data {
        //runtime.printSymbolTable()
        val f = runtime.symbolTable[funcname]
        if(f == null) {
            throw Exception("|$funcname| does not exist.")
        }
        if(f !is FuncData) {
            throw Exception("$funcname is not a function.")
        }
        if(arguments.size != f.parameters.size) {
            throw Exception("$funcname expects ${f.parameters.size} arguments, but ${arguments.size} given.")
        }
        
        // evaluate each argument to a data
        val argumentData = arguments.map {
            it.eval(runtime)
        }

        // create a subscope and evaluate the body using the subscope
        return f.body.eval(runtime.subscope(
            f.parameters.zip(argumentData).toMap()
        ))
    }
}


class Ifelse(
    val cond:Expr,
    val trueExpr:Expr,
    val falseExpr:Expr
): Expr() {
    override fun eval(runtime:Runtime): Data {
        val cond_data = cond.eval(runtime)
        if(cond_data !is BooleanData) {
            throw Exception("need boolean data in if-else")
        }
        return if(cond_data.value) {
            return trueExpr.eval(runtime)
        } else {
            return falseExpr.eval(runtime)
        }
    }
}

enum class Comparator {
    LT,
    LE,
    GT,
    GE,
    EQ,
    NE,
}


class Compare(
    val comparator: Comparator,
    val left: Expr,
    val right: Expr
): Expr() {
    override fun eval(runtime:Runtime): Data {
        val x = left.eval(runtime)
        val y = right.eval(runtime)
        if(x is IntData && y is IntData) {
            return BooleanData(
                when(comparator) {
                    Comparator.LT -> x.value < y.value
                    Comparator.LE -> x.value <= y.value
                    Comparator.GT -> x.value > y.value
                    Comparator.GE -> x.value >= y.value
                    Comparator.EQ -> x.value == y.value
                    Comparator.NE -> x.value != y.value
                }
            )
        } else {
            throw Exception("Non-integer data in comparison")
        }
    }
}

class While(val cond:Expr, val body:Expr): Expr() {
    override fun eval(runtime:Runtime): Data {
        var flag = cond.eval(runtime) as BooleanData
        var result:Data = None
        var iter:Int = 1_000_000
        while(flag.value) {
            result = body.eval(runtime)
            flag = cond.eval(runtime) as BooleanData
            if(iter == 0) {
                println("MAX_ITER reached")
                println(runtime)
                return None
            }
            iter --
        }
        return result
    }
}

class PlusPlus(val identifier: String) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val value = runtime.symbolTable[identifier]
        return if (value != null) {
            when (value) {
                is IntData -> {
                    val intValue = value.value
                    runtime.symbolTable[identifier] = IntData(intValue + 1)
                    IntData(intValue + 1)
                }
                else -> throw RuntimeException("Identifier '$identifier' does not hold an integer value")
            }
        } else {
            throw RuntimeException("Identifier '$identifier' is not defined")
        }
    }
}


class ArrayExpr(val identifier: String, val values: List<Expr>) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val evaluatedValues = values.map { it.eval(runtime) }
        //println("name: $identifier")
        // Convert evaluated values to appropriate data types
        val arrayData = evaluatedValues.map {
            when (it) {
                is IntData -> it.value
                is StringData -> it.value
                // Handle other data types as needed
                else -> throw RuntimeException("Unsupported data type in array initialization")
            }
        }.toTypedArray()

        // Assign array to symbol table
        runtime.symbolTable.put(identifier, ArrayData(arrayData.map { it as Any? }.toTypedArray()))

        return ArrayData(arrayData.map { it as Any? }.toTypedArray())
    }
}

class ArrayAccess(val identifier: String, val index: Int) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val arrayData = runtime.symbolTable[identifier] as? ArrayData
            ?: throw RuntimeException("Error: Array '$identifier' not found in symbol table")
        
        val array = arrayData.array
        if (index < 0 || index >= array.size) {
            throw RuntimeException("Index out of bounds")
        }

        val element = array[index]
        return when (element) {
            is String -> StringData(element)
            is Int -> IntData(element)
            else -> throw RuntimeException("Unsupported data type in array")
        }
    }
}

class ArrayReassign(val identifier: String, val index: Int, val update: Expr) : Expr() {
    override fun eval(runtime: Runtime): Data {
        val arrayData = runtime.symbolTable[identifier] as? ArrayData
            ?: throw RuntimeException("Error: Array '$identifier' not found in symbol table")
        
        val array = arrayData.array
        if (index < 0 || index >= array.size) {
            throw RuntimeException("Error: Index out of bounds")
        }

        val newValue = update.eval(runtime)
        when (newValue) {
            is IntData -> array[index] = newValue.value
            is StringData -> array[index] = newValue.value
            // Add support for other data types as needed
            else -> throw RuntimeException("Unsupported data type for array reassignment")
        }

        return newValue
    }
}
