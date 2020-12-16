import java.io.IOException
import java.net.ServerSocket


object Tester {
    @JvmStatic
    fun main(args: Array<String>) {
        val two = LargeNumber("11111111111111")
        val one = LargeNumber("2")
        println("Hello World !")
        val three = one * two
        println(three.toString())
    }
}