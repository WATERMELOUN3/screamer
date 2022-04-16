package powerdancer.screamer.pdBedroom

import kotlinx.coroutines.*

object Test {
    @OptIn(DelicateCoroutinesApi::class)
    fun run(): Job {
        return GlobalScope.launch {

        }
    }
}

fun main() {
    runBlocking { Test.run().join() }
}