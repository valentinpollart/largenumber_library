import java.math.BigInteger
import kotlin.random.Random
import kotlin.time.measureTime

@kotlin.time.ExperimentalTime
object Tester {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Generating random values\n")

        fun buildLargeNumberRandomList(): MutableList<LargeNumber> {
            return buildList<LargeNumber> {
                for (i in 0..99) {
                    add(LargeNumber(buildList<Int> {
                        for (j in 0..31) {
                            add(Random.nextBits(31))
                        }
                    }.toMutableList(),1))
                }
            }.toMutableList()
        }

        fun buildZeroLargeNumberList(): MutableList<LargeNumber> {
            return buildList<LargeNumber> {
                for (i in 0..99) {
                    add(LargeNumber(buildList<Int> {
                        for (j in 0..31) {
                            add(0)
                        }
                    }.toMutableList(),1))
                }
            }.toMutableList()
        }

        val aArray = buildLargeNumberRandomList()
        val bArray = buildLargeNumberRandomList()
        val cArray = buildZeroLargeNumberList()

        println("Testing addition ...")
        var meantime: Double = 0.0
        for (i in 0..99) {
            measureTime {
                val a = aArray[i]
                val b = bArray[i]
                cArray[i] = a + b
            }.also {
                meantime += it.inMilliseconds
            }
        }
        meantime /= 100
        println("Meantime for addition : $meantime ms")

        println("Checking results ...")
        for (i in 0..99) {
            if (BigInteger(aArray[i].toString()) + BigInteger(bArray[i].toString()) != BigInteger(cArray[i].toString())) {
                println("False : $i")
                println(aArray[i].toString())
                println(bArray[i].toString())
                println(cArray[i].toString())
                println(BigInteger(aArray[i].toString()) + BigInteger(bArray[i].toString()))
            }
        }

        println("\nTesting subtraction")
        meantime = 0.0
        for (i in 0..99) {
            measureTime {
                val a = aArray[i]
                val b = bArray[i]
                cArray[i] = a - b
            }.also {
                meantime += it.inMilliseconds
            }
        }
        meantime /= 100
        println("Meantime for subtraction : $meantime ms")

        println("Checking results ...")
        for (i in 0..99) {
            if (BigInteger(aArray[i].toString()) - BigInteger(bArray[i].toString()) != BigInteger(cArray[i].toString())) {
                println("False : $i")
                println(aArray[i].toString())
                println(bArray[i].toString())
                println(cArray[i].toString())
                println(BigInteger(aArray[i].toString()) - BigInteger(bArray[i].toString()))
                break
            }
        }


        println("\nTesting multiplication")
        meantime = 0.0
        for (i in 0..99) {
            measureTime {
                val a = aArray[i]
                val b = bArray[i]
                println(aArray[i].toString())
                println(bArray[i].toString())
                println(BigInteger(aArray[i].toString()) * BigInteger(bArray[i].toString()))
                cArray[i] = a * b
                println(cArray[i].toString())
            }.also {
                meantime += it.inMilliseconds
            }
        }
        meantime /= 100
        println("Meantime for multiplication : $meantime ms")
        println("Checking results ...")
        for (i in 0..99) {
            if (BigInteger(aArray[i].toString()) * BigInteger(bArray[i].toString()) != BigInteger(cArray[i].toString())) {
                println("False : $i")
                println(aArray[i].toString())
                println(bArray[i].toString())
                println(cArray[i].toString())
                println(BigInteger(aArray[i].toString()) * BigInteger(bArray[i].toString()))
                break
            }
        }
    }
}