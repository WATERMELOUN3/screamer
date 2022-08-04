package powerdancer.dsp.filter.impl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import powerdancer.dsp.filter.AbstractTerminalFilter
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Mixer
import javax.sound.sampled.SourceDataLine

class AudioPlayer(var samplesInBuffer: Int, mixerName: String? = null, private val configKey: String = "audioPlayer"): AbstractTerminalFilter() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(AudioPlayer::class.java)
    }

    private val mixer: Mixer.Info? = mixerName?.let {
        AudioSystem.getMixerInfo().first {
            it.name == mixerName
        }
    }

    private var output: SourceDataLine? = null
    private var playing = true
    lateinit var format: AudioFormat

    override suspend fun onFormatChange(format: AudioFormat) {
        this.format = format

        if (output != null) {
            onClose()
        }
        output = AudioSystem.getSourceDataLine(format, mixer)
        if (playing) {
            open()
        }
        logger.info(format.toString())
        logger.info("Output: ${mixer?.name ?: "default"}")
    }

    override suspend fun onPcmData(data: ByteBuffer) {
        if (playing) {
            output!!.write(data.array(), data.position() + data.arrayOffset(), data.remaining())
        }
    }

    override suspend fun onClose() {
        runCatching {
            output!!.stop()
            output!!.close()
        }
    }

    override suspend fun onConfigPush(key: String, value: String) {
        if (key == configKey) {
            when(value) {
                "stop" -> {
                    playing = false
                    onClose()
                }
                "play" -> {
                    open()
                }
            }
        } else if (key == "$configKey.buffer") {
            samplesInBuffer = value.toInt()
            open()
        }
    }
    private fun open() {
        runCatching {
            output!!.open(format, format.channels * ((format.sampleSizeInBits + 7) / 8) * samplesInBuffer)
            output!!.start()
            playing = true
        }
    }
}