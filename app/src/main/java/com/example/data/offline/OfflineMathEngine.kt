package com.example.data.offline

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object OfflineMathEngine {

    fun trySolveStepByStep(
        rawInput: String,
        topicHint: String = "General",
        language: String = "English"
    ): String? {
        val cleaned = rawInput.trim()
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("²", "^2")
            .replace("³", "^3")
        if (cleaned.isBlank()) return null

        // 1. Check Statistics (comma-separated numbers or stats keywords)
        extractNumberListIfStats(cleaned, topicHint)?.let { nums ->
            return solveStatistics(nums, language)
        }

        // 2. Check Quadratic Equation (contains = and x^2)
        val normalizedEq = normalizeImplicitMultiplication(cleaned.replace(",", ""))
        if (normalizedEq.contains("=") && normalizedEq.contains("x^2", ignoreCase = true)) {
            solveQuadratic(normalizedEq, language)?.let { return it }
        }

        // 3. Check Linear Equation in x (contains = and x, no higher power)
        if (normalizedEq.contains("=") && normalizedEq.contains("x", ignoreCase = true) && !normalizedEq.contains("^")) {
            solveLinear(normalizedEq, language)?.let { return it }
        }

        // 4. Extract math expression from natural-language math prompt if needed (e.g. "Calculate 4,875 + 2,396 - 1,548 step by step")
        val exprCandidate = extractExpressionCandidate(cleaned)
        if (exprCandidate != null) {
            evaluateExpressionWithSteps(exprCandidate, language)?.let { return it }
        }

        return null
    }

    private fun extractNumberListIfStats(input: String, topicHint: String): List<Double>? {
        val hasStatsKeyword = topicHint.contains("Statistics", ignoreCase = true) ||
            input.contains("mean", ignoreCase = true) ||
            input.contains("median", ignoreCase = true) ||
            input.contains("mode", ignoreCase = true) ||
            input.contains("standard deviation", ignoreCase = true) ||
            input.contains("variance", ignoreCase = true)

        val commaMatches = Regex("""-?\d+(?:\.\d+)?""").findAll(
            // Look after ":" if present (e.g. "Find mean of: 12, 15, 12, 18, 23")
            input.substringAfter(":", input)
        ).mapNotNull { it.value.toDoubleOrNull() }.toList()

        val hasCommaSeparatedNumbers = Regex("""-?\d+(?:\.\d+)?\s*,\s*-?\d+(?:\.\d+)?""").containsMatchIn(input)
        // Avoid confusing formatted thousands like 4,875 with list unless >= 3 items or stats keyword
        val commaCount = input.count { it == ',' }
        if ((hasStatsKeyword && commaMatches.size >= 2) || (hasCommaSeparatedNumbers && commaCount >= 2 && !input.contains("+") && !input.contains("=") && !input.contains("x", ignoreCase = true))) {
            return commaMatches
        }
        return null
    }

    private fun solveStatistics(nums: List<Double>, language: String): String {
        val n = nums.size
        val sum = nums.sum()
        val mean = sum / n
        val sorted = nums.sorted()
        val median = if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        val freq = nums.groupingBy { it }.eachCount()
        val maxFreq = freq.values.maxOrNull() ?: 1
        val modes = freq.filterValues { it == maxFreq }.keys.toList()
        val modeText = if (maxFreq == 1) "No mode (all values appear once)" else modes.joinToString(", ") { fmt(it) }
        val range = sorted.last() - sorted.first()
        val sqDiffs = nums.map { (it - mean).pow(2) }
        val variance = sqDiffs.sum() / n
        val stdDev = sqrt(variance)

        val isHindi = language == "Hindi"
        return buildString {
            appendLine(if (isHindi) "### ऑफलाइन सांख्यिकी समाधान (On-Device Math Engine)" else "### Offline Statistics Solution (On-Device Math Engine)")
            appendLine()
            appendLine("**1. Data Set ($n values):** `${nums.joinToString(", ") { fmt(it) }}`")
            appendLine("**Sorted Order:** `${sorted.joinToString(", ") { fmt(it) }}`")
            appendLine()
            appendLine("**2. Mean (औसत):**")
            appendLine("- Formula: `Mean = Σx / n`")
            appendLine("- Sum `Σx = ${nums.joinToString(" + ") { fmt(it) }} = ${fmt(sum)}`")
            appendLine("- `Mean = ${fmt(sum)} / $n = ${fmt(mean)}`")
            appendLine()
            appendLine("**3. Median (माध्यिका):**")
            appendLine("- Middle value of sorted data = `${fmt(median)}`")
            appendLine()
            appendLine("**4. Mode & Range:**")
            appendLine("- **Mode (बहुलक):** `$modeText` (frequency: $maxFreq)")
            appendLine("- **Range (परिसर):** `${fmt(sorted.last())} - ${fmt(sorted.first())} = ${fmt(range)}`")
            appendLine()
            appendLine("**5. Variance & Standard Deviation (प्रसरण और मानक विचलन):**")
            appendLine("- Formula: `σ² = Σ(x - μ)² / n`, `σ = √σ²`")
            appendLine("- `Σ(x - μ)² = ${fmt(sqDiffs.sum())}`")
            appendLine("- **Population Variance (σ²):** `${fmt(sqDiffs.sum())} / $n = ${fmt(variance)}`")
            appendLine("- **Standard Deviation (σ):** `√${fmt(variance)} = ${fmt(stdDev)}`")
        }
    }

    private fun solveLinear(eq: String, language: String): String? {
        val parts = eq.split("=")
        if (parts.size != 2) return null
        val lhs = parseLinearSide(parts[0]) ?: return null
        val rhs = parseLinearSide(parts[1]) ?: return null

        val a = lhs.first - rhs.first // coefficient of x on left
        val c = rhs.second - lhs.second // constant moved to right
        val isHindi = language == "Hindi"

        if (abs(a) < 1e-12) {
            return if (abs(c) < 1e-12) {
                "### Offline Algebra Solution\n\nBoth sides simplify identically (`0 = 0`), so **infinitely many solutions** exist for `x`."
            } else {
                "### Offline Algebra Solution\n\nThe `x` terms cancel (`0 = ${fmt(c)}`), so there is **no solution**."
            }
        }

        val x = c / a
        return buildString {
            appendLine(if (isHindi) "### ऑफलाइन रैखिक समीकरण हल (On-Device Math Engine)" else "### Offline Linear Equation Solution (On-Device Math Engine)")
            appendLine()
            appendLine("**1. Given Equation:** `${eq.replace("*", "")}`")
            appendLine("- Left side simplified: `${fmt(lhs.first)}x + (${fmt(lhs.second)})`")
            appendLine("- Right side simplified: `${fmt(rhs.first)}x + (${fmt(rhs.second)})`")
            appendLine()
            appendLine("**2. Group `x` terms on Left & Constants on Right:**")
            appendLine("- `(${fmt(lhs.first)} - ${fmt(rhs.first)})x = ${fmt(rhs.second)} - (${fmt(lhs.second)})`")
            appendLine("- `${fmt(a)}x = ${fmt(c)}`")
            appendLine()
            appendLine("**3. Divide both sides by `${fmt(a)}`:**")
            appendLine("- `x = ${fmt(c)} / ${fmt(a)}`")
            appendLine("- **Final Answer:** `x = ${fmt(x)}`")
            appendLine()
            appendLine("**4. Verification Check:**")
            val lhsCheck = lhs.first * x + lhs.second
            val rhsCheck = rhs.first * x + rhs.second
            appendLine("- LHS = `${fmt(lhsCheck)}`, RHS = `${fmt(rhsCheck)}` ✔")
        }
    }

    private fun solveQuadratic(eq: String, language: String): String? {
        val parts = eq.split("=")
        if (parts.size != 2) return null
        val lhs = parseQuadraticSide(parts[0]) ?: return null
        val rhs = parseQuadraticSide(parts[1]) ?: return null

        val a = lhs.a - rhs.a
        val b = lhs.b - rhs.b
        val c = lhs.c - rhs.c
        if (abs(a) < 1e-12) return solveLinear(eq, language)

        val d = b * b - 4.0 * a * c
        val isHindi = language == "Hindi"

        return buildString {
            appendLine(if (isHindi) "### ऑफलाइन द्विघात समीकरण हल (On-Device Math Engine)" else "### Offline Quadratic Equation Solution (On-Device Math Engine)")
            appendLine()
            appendLine("**1. Standard Form (`ax² + bx + c = 0`):**")
            appendLine("- `${fmt(a)}x² + (${fmt(b)})x + (${fmt(c)}) = 0`")
            appendLine("- `a = ${fmt(a)}`, `b = ${fmt(b)}`, `c = ${fmt(c)}`")
            appendLine()
            appendLine("**2. Quadratic Formula:**")
            appendLine("- `x = (-b ± √(b² - 4ac)) / 2a`")
            appendLine()
            appendLine("**3. Calculate Discriminant (`Δ = b² - 4ac`):**")
            appendLine("- `Δ = (${fmt(b)})² - 4(${fmt(a)})(${fmt(c)})`")
            appendLine("- `Δ = ${fmt(b * b)} - (${fmt(4 * a * c)}) = ${fmt(d)}`")
            appendLine()
            appendLine("**4. Find Roots:**")
            when {
                d > 1e-12 -> {
                    val sqrtD = sqrt(d)
                    val x1 = (-b + sqrtD) / (2 * a)
                    val x2 = (-b - sqrtD) / (2 * a)
                    appendLine("- Since `Δ > 0`, there are **two distinct real roots**:")
                    appendLine("- `√Δ = ${fmt(sqrtD)}`")
                    appendLine("- `x₁ = (${fmt(-b)} + ${fmt(sqrtD)}) / ${fmt(2 * a)} = ${fmt(x1)}`")
                    appendLine("- `x₂ = (${fmt(-b)} - ${fmt(sqrtD)}) / ${fmt(2 * a)} = ${fmt(x2)}`")
                    appendLine()
                    appendLine("**Final Answer:** `x = ${fmt(x1)}` or `x = ${fmt(x2)}`")
                }
                abs(d) <= 1e-12 -> {
                    val x = -b / (2 * a)
                    appendLine("- Since `Δ = 0`, there is **one repeated real root**:")
                    appendLine("- `x = ${fmt(-b)} / ${fmt(2 * a)} = ${fmt(x)}`")
                    appendLine()
                    appendLine("**Final Answer:** `x = ${fmt(x)}`")
                }
                else -> {
                    val realPart = -b / (2 * a)
                    val imagPart = sqrt(-d) / abs(2 * a)
                    appendLine("- Since `Δ < 0`, there are **two complex conjugate roots**:")
                    appendLine("- `x = ${fmt(realPart)} ± ${fmt(imagPart)}i`")
                }
            }
        }
    }

    private data class QuadCoeff(val a: Double, val b: Double, val c: Double)

    private fun parseQuadraticSide(expr: String): QuadCoeff? {
        val compact = expr.replace(" ", "").lowercase()
            .replace("-", "+-")
            .trimStart('+')
        if (compact.isBlank()) return QuadCoeff(0.0, 0.0, 0.0)
        val tokens = compact.split("+").filter { it.isNotBlank() }
        var a = 0.0
        var b = 0.0
        var c = 0.0
        for (tok in tokens) {
            when {
                tok.endsWith("x^2") -> {
                    val coeffStr = tok.removeSuffix("x^2").removeSuffix("*")
                    val coeff = when (coeffStr) {
                        "", "+" -> 1.0
                        "-" -> -1.0
                        else -> coeffStr.toDoubleOrNull() ?: return null
                    }
                    a += coeff
                }
                tok.endsWith("x") -> {
                    val coeffStr = tok.removeSuffix("x").removeSuffix("*")
                    val coeff = when (coeffStr) {
                        "", "+" -> 1.0
                        "-" -> -1.0
                        else -> coeffStr.toDoubleOrNull() ?: return null
                    }
                    b += coeff
                }
                tok.contains("x") -> return null
                else -> {
                    c += tok.toDoubleOrNull() ?: return null
                }
            }
        }
        return QuadCoeff(a, b, c)
    }

    private fun parseLinearSide(expr: String): Pair<Double, Double>? {
        val q = parseQuadraticSide(expr) ?: return null
        if (abs(q.a) > 1e-12) return null
        return q.b to q.c
    }

    private fun normalizeImplicitMultiplication(s: String): String {
        return s.replace(Regex("""(\d)(x)""", RegexOption.IGNORE_CASE), "$1*$2")
    }

    private fun extractExpressionCandidate(input: String): String? {
        // Remove thousands commas like 4,875 -> 4875
        val noCommas = input.replace(Regex("""(\d),(\d{3})"""), "$1$2")
        // Check if entire string is mostly a math expression
        val allowed = Regex("""^[0-9+\-*/^().\s]+|(?:sqrt|sin|cos|tan|log|ln|abs)\([0-9+\-*/^().\s]+\)$""", RegexOption.IGNORE_CASE)
        if (allowed.matches(noCommas) && noCommas.any { it.isDigit() }) {
            return noCommas
        }
        // Otherwise look for a contiguous arithmetic sub-expression with at least one operator
        val sub = Regex("""(?:(?:sqrt|sin|cos|tan|log|ln|abs)\s*\([^)]+\)|[0-9]+(?:\.[0-9]+)?|\([^)]+\))(?:\s*[+\-*/^]\s*(?:(?:sqrt|sin|cos|tan|log|ln|abs)\s*\([^)]+\)|[0-9]+(?:\.[0-9]+)?|\([^)]+\)))+""", RegexOption.IGNORE_CASE)
        return sub.find(noCommas)?.value?.trim()
    }

    private fun evaluateExpressionWithSteps(expr: String, language: String): String? {
        val steps = mutableListOf<String>()
        val result = runCatching { ExpressionParser(expr, steps).parse() }.getOrNull() ?: return null
        if (result.isNaN() || result.isInfinite()) return null
        val isHindi = language == "Hindi"

        return buildString {
            appendLine(if (isHindi) "### ऑफलाइन गणित समाधान (On-Device Math Engine)" else "### Offline Math Solution (On-Device Math Engine)")
            appendLine()
            appendLine("**1. Expression:** `$expr`")
            appendLine()
            appendLine("**2. Step-by-Step Calculation (BODMAS / PEMDAS):**")
            if (steps.isEmpty()) {
                appendLine("- Direct value = `${fmt(result)}`")
            } else {
                steps.forEachIndexed { i, step ->
                    appendLine("- Step ${i + 1}: `$step`")
                }
            }
            appendLine()
            appendLine("**3. Final Answer:** `${fmt(result)}`")
        }
    }

    private class ExpressionParser(
        private val str: String,
        private val steps: MutableList<String>
    ) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected char: ${ch.toChar()}")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> {
                        val y = parseTerm()
                        val res = x + y
                        steps.add("${fmt(x)} + ${fmt(y)} = ${fmt(res)}")
                        x = res
                    }
                    eat('-'.code) -> {
                        val y = parseTerm()
                        val res = x - y
                        steps.add("${fmt(x)} - ${fmt(y)} = ${fmt(res)}")
                        x = res
                    }
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> {
                        val y = parseFactor()
                        val res = x * y
                        steps.add("${fmt(x)} × ${fmt(y)} = ${fmt(res)}")
                        x = res
                    }
                    eat('/'.code) -> {
                        val y = parseFactor()
                        if (abs(y) < 1e-12) throw ArithmeticException("Division by zero")
                        val res = x / y
                        steps.add("${fmt(x)} ÷ ${fmt(y)} = ${fmt(res)}")
                        x = res
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = str.substring(startPos, pos).lowercase()
                x = parseFactor()
                val rawVal = x
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    "abs" -> abs(x)
                    else -> throw IllegalArgumentException("Unknown function: $func")
                }
                steps.add("$func(${fmt(rawVal)}) = ${fmt(x)}")
            } else {
                throw IllegalArgumentException("Unexpected char: ${ch.toChar()}")
            }

            if (eat('^'.code)) {
                val exp = parseFactor()
                val res = x.pow(exp)
                steps.add("${fmt(x)}^${fmt(exp)} = ${fmt(res)}")
                x = res
            }
            return x
        }
    }

    private fun fmt(v: Double): String {
        val rounded = v.roundToLong()
        return if (abs(v - rounded) < 1e-9) {
            rounded.toString()
        } else {
            String.format(java.util.Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        }
    }
}
