package calculator

class MyBigInt(private var value: String) {
    companion object {
        val ZERO = MyBigInt("0")
        val ONE = MyBigInt("1")
    }

    private fun mulByDigit(digit: Int): MyBigInt {
        if(digit == 10) return MyBigInt("${this.value}0")
        var carry = 0
        var res = ""
        for(pointer in this.value.lastIndex downTo 0) {
            val thisDigit = this.value[pointer].digitToInt()
            val curr = thisDigit * digit + carry
            carry = curr / 10
            res += (curr % 10).digitToChar()
        }
        if(carry != 0) res += carry.digitToChar()

        return MyBigInt(res.reversed())
    }

    fun getVal(): String{
        return this.value
    }

    fun mod2(): MyBigInt {
        return if(this.value.last().digitToInt() % 2 == 1) ONE else ZERO
    }

    operator fun unaryMinus(): MyBigInt {
        if(this.value == "0") return MyBigInt("0")
        return if(this.value[0]=='-') MyBigInt(this.value.substring(1))
        else MyBigInt('-' + this.value)
    }
    operator fun compareTo(other: MyBigInt): Int {
        if(this.value[0] != '-' && other.value[0]=='-') return 1
        if(this.value[0] == '-' && other.value[0]!='-') return -1
        if(this.value.length > other.value.length) return 1
        if(this.value.length < other.value.length) return -1
        for(i in this.value.indices) {
            if(this.value[i] == other.value[i]) continue
            return this.value[i].digitToInt() - other.value[i].digitToInt()
        }
        return 0
    }
    operator fun plus(other: MyBigInt): MyBigInt {
        var pointer1 = this.value.length - 1
        var pointer2 = other.value.length - 1
        val sign1 = if (this.value[0] == '-') -1 else 1
        val sign2 = if (other.value[0] == '-') -1 else 1
        if(sign1 == -1 && sign2 == -1) {
            return -(-this + -other)
        }
        if(sign1 == -1 && -this >= other) {
            return -(-this + -other)
        }
        if(sign2 == -1 && this < -other) {
            return -(-other + -this)
        }
        var carry = 0
        val res = mutableListOf<Char>()
        while(pointer1 >= 0 || pointer2 >= 0) {
            val thisDigit: Int
            val otherDigit: Int
            if(pointer1 >= 0 && this.value[pointer1]=='-'){
                pointer1--
                continue
            }
            else if(pointer2 >= 0 && other.value[pointer2]=='-') {
                pointer2--
                continue
            }
            else if(pointer1 >= 0 && pointer2 >= 0) {
                thisDigit = this.value[pointer1--].digitToInt()
                otherDigit = other.value[pointer2--].digitToInt() * sign2
            }
            else if(pointer1 >= 0){
                thisDigit = this.value[pointer1--].digitToInt()
                otherDigit = 0
            }
            else{
                thisDigit = 0
                otherDigit = other.value[pointer2--].digitToInt() * sign2
            }
            var curr = thisDigit + otherDigit + carry
            if(curr < 0){
                curr += 10
                carry = -1
            }
            else{
                carry = curr / 10
            }
            res += (curr % 10).digitToChar()
        }
        if(carry > 0) res += '1'
        while(res.size!=1 && res.last() == '0') res.removeLast()
        if(carry < 0){
            res += '-'
        }
        return MyBigInt(res.reversed().joinToString(""))
    }

    operator fun minus(other: MyBigInt): MyBigInt {
        return this + -other
    }

    operator fun times(other: MyBigInt): MyBigInt {
        if(this < ZERO && other < ZERO) return -this * -other
        if(this < ZERO) return -(-this * other)
        if(other < ZERO) return -(this * -other)
        var res = ZERO
        var copy = MyBigInt(other.value)
        for(pointer in this.value.lastIndex downTo 0) {
            res += copy.mulByDigit(this.value[pointer].digitToInt())
            if(copy != ZERO)copy = copy.mulByDigit(10)
        }
        return res
    }

    operator fun div(other: MyBigInt): MyBigInt {
        if(this < ZERO && other < ZERO) return -this / -other
        if(this < ZERO) return -(-this / other)
        if(other < ZERO) return -(this / -other)
        var res = ZERO
        var curr = ZERO
        for(pointer in this.value.indices) {
            if(curr != ZERO) curr = curr.mulByDigit(10)
            curr += MyBigInt(this.value[pointer].toString())
            var toAdd = ZERO
            while(curr >= other) {
                curr -= other
                toAdd += ONE
            }
            if(res != ZERO) res = res.mulByDigit(10)
            res += toAdd
        }
        return res
    }

}

val precedence = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2, '^' to 3, '(' to 0, ')' to 0)
val variables = mutableMapOf<String, MyBigInt>()

fun binPow(a: MyBigInt, pow: MyBigInt): MyBigInt {
    if(pow == MyBigInt.ZERO) return MyBigInt.ONE
    var res = binPow(a, pow/(MyBigInt.ONE + MyBigInt.ONE))
    res *= res
    if(pow.mod2() == MyBigInt.ONE) res *= a
    return res
}

fun printHelp() {
    println("This program evaluates arithmetic expressions, supports variables")
}

fun shrink(expr: String): String {
    return if (expr.count { it == '-' } % 2 == 1) "-" else "+"
}

fun compress(expr: String): String {
    return "+$expr".replace(" ", "").replace("[-+ ]+".toRegex()) { shrink(it.value) }
}

fun checkValid(expr: String): Boolean {
    return "([-+*/^]\\(*\\w+\\)*)+".toRegex().matches(expr)
}

fun getPostfix(expr: String): MutableList<String> {
    val postfix = mutableListOf<String>()
    val stack = mutableListOf<Char>()
    var lastIndex = 0

    for(i in expr.indices) {
        if(precedence.containsKey(expr[i])) {
            val operand = expr.substring(lastIndex, i)
            lastIndex = i + 1
            if(operand.isNotBlank()) postfix.add(operand)
            if(expr[i]=='('){
                stack.add('(')
                continue
            }
            if(expr[i]==')'){
                while(stack.last() != '(') {
                    postfix.add(stack.removeLast().toString())
                }
                stack.removeLast()
                continue
            }
            while(stack.isNotEmpty() && precedence[stack.last()]!! >= precedence[expr[i]]!!) {
                postfix.add(stack.removeLast().toString())
            }
            stack.add(expr[i])
        }
        if(expr[i] == '|'){
            val operand = expr.substring(lastIndex, i)
            if(operand.isNotBlank()) postfix.add(operand)
        }
    }
    while(stack.isNotEmpty()) postfix.add(stack.removeLast().toString())
    return postfix
}

fun evalPostFix(postfix: MutableList<String>): String {
    val stack = mutableListOf<MyBigInt>()
    for(expr in postfix) {
        if(precedence.containsKey(expr[0])) {
            val right = stack.removeLast()!!
            val left = stack.removeLast()!!
            val result = when(expr[0]){
                '+' -> left + right
                '-' -> left - right
                '/' -> {
                    if(right == MyBigInt.ZERO) return "Division by zero"
                    left / right
                }
                '*' -> left * right
                '^' -> binPow(left, right)
                else -> MyBigInt.ZERO
            }
            stack.add(result)
        }
        else if("(0|-?[1-9]\\d*)".toRegex().matches(expr)){
            stack.add(MyBigInt(expr))
        }
        else {
            if(!"[a-zA-Z]+".toRegex().matches(expr)) return "Invalid identifier"
            if(!variables.containsKey(expr)) return "Unknown variable"
            stack.add(variables[expr]!!)
        }
    }
    return stack.last().getVal()
}

fun getExprValue(expr: String): String {
    val compressed = compress(expr)
    val handleUnaryOps = compressed.replace("[/*(][-+]\\w+".toRegex())
    { "${it.value[0]}(0${it.value.substring(1)})" }
    if(!checkValid(handleUnaryOps)) return "Invalid expression"
    if(!checkBracketsBalanced(handleUnaryOps)) return "Invalid expression"
    val postfix = getPostfix("0$handleUnaryOps|")
    return evalPostFix(postfix)
}

fun checkBracketsBalanced(expr: String): Boolean {
    var curr = 0
    for(ch in expr) {
        when(ch){ '(' -> curr++; ')' -> curr-- }
        if(curr < 0) return false
    }
    return curr == 0
}

fun main() {

    while(true){
        val op = readln()
        when{
            op.isBlank() -> continue
            op == "/exit" -> { println("Bye!"); break }
            op == "/help" -> printHelp()
            op[0] == '/' -> println("Unknown command")
            '=' in op -> {
                val (variableNameTrailing, expr) = op.split('=', limit = 2)
                val variableName = variableNameTrailing.replace(" ", "")
                if(!"[a-zA-Z]+\\s*".toRegex().matches(variableName)) {
                    println("Invalid identifier")
                    continue
                }
                val res = getExprValue(expr)
                if(!"(0|-?[1-9]\\d*)".toRegex().matches(res)) println("Invalid assignment")
                else{
                    variables[variableName] = MyBigInt(res)
                }
            }
            else -> {
                println(getExprValue(op))
            }
        }
    }
}
