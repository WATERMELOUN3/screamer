package powerdancer.screamer

import kotlinx.coroutines.runBlocking
import powerdancer.dsp.Processor
import powerdancer.dsp.filter.impl.AudioPlayer
import powerdancer.screamer.pdBedroom.BedroomReceiver
import powerdancer.screamer.pdBedroom.BedroomTheater
import powerdancer.screamer.pdBedroom.Karaoke
import powerdancer.screamer.pdBedroom.Test

fun main(args:Array<String>) = runBlocking{
    if (args.isEmpty()) {
        Processor.process(
            ScreamMulticastAudioReceiver(),
            AudioPlayer(2048)
        ).join()
    } else {
        when(args[0]) {
            "unicast" -> {
                when(args.size) {
                    1 -> ScreamUnicastAudioReceiver.run().join()
                    2 -> ScreamUnicastAudioReceiver.run(args[1].toInt())
                    else -> ScreamUnicastAudioReceiver.run(args[1].toInt(), args[2])
                }
            }
            "receiver" -> ScreamerReceiver.run().join()
            "bedroomReceiver" -> BedroomReceiver.run().join()
            "test" -> Test.run().join()
            "bedroom" -> BedroomTheater.run().join()
            "karaoke" -> Karaoke.run().join()
            else -> println("Invalid input.")
        }
    }

}
