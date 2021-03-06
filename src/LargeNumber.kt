@ExperimentalStdlibApi
class LargeNumber {
    // Big endian representation of the large number
    private var BASE = 1000000000
    private var slices: MutableList<Int> = emptyList<Int>().toMutableList()
    private var sign: Int
    private var digitsPerSlice: Int = Int.MAX_VALUE.toString().length - 1

    companion object {
        val ZERO = LargeNumber("0")
        val ONE = LargeNumber("1")
        val TWO = LargeNumber("2")
    }

    // This constructor expects a string representation of a large number in 10 radix and split it into 9 digits slices
    constructor(largeNumber: String) {
        var parsedNumber = largeNumber
        // Parse large number sign if provided
        sign = 1
        if(parsedNumber[0] == '-') {
            sign = -1
            parsedNumber = parsedNumber.drop(1)
        } else if (parsedNumber[0] == '+') {
            parsedNumber = parsedNumber.drop(1)
        }

        // Remove padding 0s
        parsedNumber = parsedNumber.dropWhile { it == '0'}
        if (parsedNumber.isEmpty()) {
            sign = 0
            slices.add(0,0)
            return
        }

        // Compute bigEndianSize : this slice might be smaller than others slices (which are 9 digits long); then parse it
        var bigEndianSize = parsedNumber.length % digitsPerSlice
        if (bigEndianSize == 0) {
            bigEndianSize = digitsPerSlice
        }
        slices.add(parsedNumber.substring(0,bigEndianSize).toInt())
        parsedNumber = parsedNumber.drop(bigEndianSize)

        // Parse regular slices from the string
        while (parsedNumber.isNotEmpty()) {
            slices.add(parsedNumber.substring(0,digitsPerSlice).toInt())
            parsedNumber = parsedNumber.drop(digitsPerSlice)
        }
    }

    constructor(slices: MutableList<Int>, sign: Int){
        this.slices = slices
        this.sign = sign
    }

    // Implements comparison operator for LargeNumber object
    operator fun compareTo(largeNumber: LargeNumber): Int {
        if (sign != largeNumber.sign) {
            return (sign - largeNumber.sign) / 2
        }
        return compareAbs(slices,largeNumber.slices) * sign
    }


    // Implements addition operator for LargeNumber object
    operator fun plus(largeNumber: LargeNumber): LargeNumber {
        // Fast return for null term
        if (sign == 0) {
            return largeNumber
        }
        if (largeNumber.sign == 0) {
            return this
        }
        // Addition if both large numbers have the same sign
        if (sign == largeNumber.sign) {
            return LargeNumber(add(slices, largeNumber.slices),sign)
        }

        // Subtraction if signs are different
        return this - largeNumber
    }

    // Implements subtraction for LargeNumber object
    operator fun minus(largeNumber: LargeNumber): LargeNumber {
        // Subtraction is always between absolute values (greater minus lower) and sign is computed aside
        val comp = compareAbs(slices, largeNumber.slices)

        // Fast return if absolute values are equal
        if (comp == 0) {
            return LargeNumber("0")
        }

        return LargeNumber(if (comp >0) subtract(slices,largeNumber.slices) else subtract(largeNumber.slices,slices),this.compareTo(largeNumber))
    }

    // Implements multiplication for LargeNumber object
    operator fun times(largeNumber: LargeNumber): LargeNumber {
        val one = LargeNumber("1")
        // Fast return if one factor is zero
        if (sign == 0 || largeNumber.sign == 0) {
            return LargeNumber("0")
        }

        // Fast return if one factor is one
        if (this == one) {
            return largeNumber
        }
        if (largeNumber == one) {
            return this
        }

        return LargeNumber(multiply(slices,largeNumber.slices),sign*largeNumber.sign)
    }

    // Implements right shift for LargeNumber object
    private infix fun shr(n: Int): LargeNumber {
        val result: MutableList<Int> = zeros(this.slices.size)
        if(this.sign == 0) {
            return ZERO
        }
        for (i in 0 until result.size - 1) {
            // Classic shift for each slice
            result.add(i,this.slices[i] shr n)
            // Remainder propagation on next slice
            result[i + 1] += this.slices[i] - result[i]
        }
        // Last shift (no remainder)
        result[result.size - 1] = this.slices[result.size - 1] shr n
        return LargeNumber(result, this.sign)
    }


    fun remShr(n: Int): LargeNumber {
        val result: MutableList<Int> = zeros(this.slices.size)
        var shift = n
        for (i in result.size - 1 downTo 0) {
            result[i] = slices[i]
            result[i] = (result[i] shr shift) shl shift
            shift -= 32
            if (shift <= 0) {
                break
            }
        }
        return this - LargeNumber(result, sign)
    }

    fun montgomeryTimes(largeNumber: LargeNumber, n: LargeNumber, v: LargeNumber): LargeNumber {
        val k = n.modK() + 1
        val s = this * largeNumber
        val t = (s * v).remShr(k)
        val m = s + t * n
        val u = m shr k
        return if (u >= n) u - n else u
    }

    fun squareAndMultiply(exponent: LargeNumber, n: LargeNumber, r: LargeNumber, rModInv: LargeNumber, v: LargeNumber): LargeNumber {
        // Turn into Montgomery form
        val mgyForm = this.montgomeryTimes(r, n, v)

        // 1 in Montgomery form
        var p = r - n


        for (i in exponent.slices.size - 1 downTo 0) {
            val slice = Integer.toBinaryString(exponent.slices[i])
            for (j in slice.length -1 downTo 0) {
                p = p.montgomeryTimes(p, n ,v)
                if(slice[j] == '1') {
                    p = p.montgomeryTimes(mgyForm, n ,v)
                }
            }
        }

        return p.montgomeryTimes(rModInv, n, v)
    }

    // Implements addition between List<Int> with carry propagation
    private fun add(xSlices: MutableList<Int>, ySlices: MutableList<Int>): MutableList<Int> {
        // Determines result size
        var resultSize = xSlices.size
        if (xSlices.size < ySlices.size) {
            resultSize = ySlices.size
        }
        var xCursor = xSlices.size
        var yCursor = ySlices.size
        var resultSlices: MutableList<Int> = MutableList(resultSize){0}
        var result: Long
        var carry = 0

        // Adding common parts
        while (xCursor > 0 && yCursor > 0) {
            result = (xSlices[--xCursor].toLong() and 0xffffffffL) + (ySlices[--yCursor].toLong() and 0xffffffffL) + carry
            carry = (result/BASE).toInt()
            result %= BASE
            resultSlices[--resultSize] = result.toInt()
        }

        // Adding remaining part from greatest number
        if(yCursor != 0) {
            while (yCursor > 0) {
                resultSlices[--resultSize] = ySlices[--yCursor] + carry
                carry = 0
            }
        } else if (xCursor != 0) {
            while (xCursor > 0){
                resultSlices[--resultSize] = xSlices[--xCursor] + carry
                carry = 0
            }
        }

        // Adding final carry
        if (carry != 0) {
            resultSlices = (mutableListOf(carry) + resultSlices).toMutableList()
        }
        return resultSlices
    }

    // Implements subtraction between List<Int> with carry propagation
    private fun subtract(xSlices: MutableList<Int>, ySlices: MutableList<Int>): MutableList<Int> {
        // xSlices always has a greater absolute value

        // Determines result size
        var resultSize = xSlices.size
        if (xSlices.size < ySlices.size) {
            resultSize = ySlices.size
        }
        var xCursor = xSlices.size
        var yCursor = ySlices.size
        val resultSlices: MutableList<Int> = MutableList(resultSize){0}
        var result: Long
        var carry: Long = 0

        // Subtracting common parts
        while (xCursor > 0 && yCursor > 0) {
            result = (xSlices[--xCursor].toLong() and 0xffffffffL) - (ySlices[--yCursor].toLong() and 0xffffffffL) - carry
            carry = 0
            if (result < 0) {
                while (result < 0) {
                    result += BASE
                    carry++
                }
            }
            resultSlices[--resultSize] = result.toInt()
        }

        // Adding remaining part from greatest absolute value
        while (xCursor > 0) {
            resultSlices[xCursor--] = xSlices[xCursor--] - carry.toInt()
            carry = 0
        }

        return resultSlices
    }

    // Implements multiplication between List<Int>
    private fun multiply(xSlices: MutableList<Int>, ySlices: MutableList<Int>): MutableList<Int>{
        var highStep: MutableList<Int>
        var times: Long
        // Determines which number has a greater absolute value
        val resultSize = xSlices.size + ySlices.size
        var result = zeros(resultSize)

        for (i in (xSlices.size - 1) downTo 0) {
            for (j in (ySlices.size - 1) downTo 0) {
                highStep = zeros(resultSize)
                times = (xSlices[i].toLong() and 0xffffffffL) * (ySlices[j].toLong() and 0xffffffffL)
                highStep[i + j + 1] = (times%BASE).toInt()
                times /= BASE
                highStep[i + j] = (times).toInt()
                result = add(result, highStep)
            }
        }
        while (result[0] == 0) {
            result.removeAt(0)
        }
        return result
    }

    // Determines which List<Int> has a greater absolute value
    private fun compareAbs(xSlices: MutableList<Int>, ySlices: MutableList<Int>): Int {
        if (xSlices.size != ySlices.size) {
            return (xSlices.size - ySlices.size)
        }
        for (i in 0 until xSlices.size) {
            if (xSlices[i] != ySlices[i]) {
                return (xSlices[i] - ySlices[i])
            }
        }
        return 0
    }

    // Transforms LargeNumber object into String for testing and console returns
    override fun toString(): String{
        var string = ""
        var currentSlice: Long
        val largeNumberToDisplay = this
        var remainder: Long = 0

        // Fetch value equivalent to 9 digits from each Int in the List and propagates the carry
        for (i in this.slices.size - 1 downTo  1) {
            currentSlice = (slices[i].toLong() and 0xffffffffL) + remainder
            largeNumberToDisplay.slices[i] = currentSlice.toInt()
            remainder = currentSlice/BASE
            string = (currentSlice%BASE).toString().padStart(9,'0').plus(string)
        }

        // Fetch value from the bigEndianInt from the List
        string = (((largeNumberToDisplay.slices[0].toLong() and 0xffffffffL) + remainder).toString()).plus(string).trimStart('0')
        if (this.sign < 0) {
            string = '-'.plus(string)
        }
        return string
    }

    // Create a List<Int> of zeros with desired size
    private fun zeros(size: Int): MutableList<Int>{
        return buildList {
            for (i in 0 until size) {
                add(0)
            }
        }.toMutableList()
    }

    private fun modK(): Int {
        var mostSignificantSlice = this.slices[0]
        var k = 0
        while (mostSignificantSlice != 0) {
            k++
            mostSignificantSlice /= 2
        }
        return 32 * (this.slices.size - 1) + k
    }

    public fun changeDomain(r: LargeNumber, n: LargeNumber, v: LargeNumber): LargeNumber {
        return this.montgomeryTimes(r, n, v)
    }
}
