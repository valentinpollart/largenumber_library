class LargeNumber {
    // Big endian representation of the large number
    private var slices: MutableList<Int> = emptyList<Int>().toMutableList()
    private var sign: Int

    private var digitsPerSlice: Int = Int.MAX_VALUE.toString().length - 1

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
            return LargeNumber(add(slices,largeNumber.slices),sign)
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
        return if (comp > 0) {
            LargeNumber(subtract(slices,largeNumber.slices), this.compareTo(largeNumber)*comp)
        } else  {
            LargeNumber(subtract(largeNumber.slices,slices), this.compareTo(largeNumber)*comp)
        }
    }

    // Implements multiplication for LargeNumber object
    operator fun times(largeNumber: LargeNumber): LargeNumber {
        val one = LargeNumber("1")
        // Fast return if one factor is zero
        if ((sign == 0) or (largeNumber.sign == 0)) {
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
            resultSlices[--resultSize] = result.toInt()
            carry = (result shr 32).toInt()
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
        var resultSlices: MutableList<Int> = MutableList(resultSize){0}
        var result: Long
        var carry = 0

        // Subtracting common parts
        while (xCursor > 0 && yCursor > 0) {
            result = (xSlices[--xCursor].toLong() and 0xffffffffL) - (ySlices[--yCursor].toLong() and 0xffffffffL) - carry
            if (result < 0) {
                carry = 1
                result += 0x100000000L
            }
            resultSlices[--resultSize] = result.toInt()
        }

        // Adding remaining part from greatest absolute value
        while (xCursor > 0) {
            resultSlices[xCursor--] = xSlices[xCursor--] - carry
            carry = 0
        }

        return resultSlices
    }

    // Implements multiplication between List<Int>
    private fun multiply(xSlices: MutableList<Int>, ySlices: MutableList<Int>): MutableList<Int>{
        val lower: MutableList<Int>
        val higher: MutableList<Int>
        var lowStep: MutableList<Int>
        var highStep: MutableList<Int>
        var times: Long
        // Determines which number has a greater absolute value
        if (compareAbs(xSlices, ySlices) > 0) {
            lower = ySlices
            higher = xSlices
        } else {
            lower = xSlices
            higher = ySlices
        }

        var result = zeros(lower.size + higher.size)
        var lowCursor = lower.size
        var highCursor = higher.size

        for (i in (lowCursor - 1) downTo 0) {
            lowStep = zeros(lower.size + higher.size)
            for (j in (highCursor - 1) downTo 0) {
                highStep = zeros(lower.size + higher.size)
                times = (lower[i].toLong() and 0xffffffffL) * (higher[j].toLong() and 0xffffffffL)
                highStep[i + j] = times.toInt()
                if ((times shr 32).toInt() != 0) {
                    if (i + j == 0) {
                        highStep.add(0,(times shr 32).toInt())
                    } else {
                        highStep[i + j - 1] = (times shr 32).toInt()
                    }

                }

                lowStep = add(lowStep,highStep)
            }
            result = add(result, lowStep)
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
    public override fun toString(): String{
        var string = ""
        var currentSlice: Long
        var largeNumberToDisplay = this
        var remainder: Long = 0
        var radix = 1000000000

        // Fetch value equivalent to 9 digits from each Int in the List and propagates the carry
        for (i in this.slices.size - 1 downTo  1) {
            currentSlice = (slices[i].toLong() and 0xffffffffL) + remainder
            largeNumberToDisplay.slices[i] = currentSlice.toInt()
            if (currentSlice > radix) {
                remainder = currentSlice/radix
            }
            string = (currentSlice%radix).toString().padStart(9,'0').plus(string)
        }

        // Fetch value from the bigEndianInt from the List
        string = ((largeNumberToDisplay.slices[0] + remainder).toString()).plus(string)
        return string
    }

    // Create a List<Int> of zeros with desired size
    private fun zeros(size: Int): MutableList<Int>{
        return buildList<Int> {
            for (i in 0..size) {
                add(0)
            }
        }.toMutableList()
    }
}