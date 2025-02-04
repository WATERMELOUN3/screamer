package powerdancer.screamer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.receiveAsFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import powerdancer.dsp.filter.AbstractTerminalFilter
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.AudioFormat

class TcpAudioSender(private val host: String, private val port: Int = 6789): AbstractTerminalFilter() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(TcpAudioSender::class.java)
    }

    private val output: AtomicReference<Socket?> = AtomicReference(null)
    private var encodedSampleRate: Byte = 0
    private var bitSize: Byte = 0
    var channels: Byte = 0

    private var connectionJob: Job? = null

    @OptIn(ObsoleteCoroutinesApi::class)
    override suspend fun onInit() {
        connectionJob = CoroutineScope(Dispatchers.Default).launch {
            ticker(1000).receiveAsFlow().collect {
                if (output.get() == null) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(InetAddress.getByName(host), port), 200)
                        socket.soTimeout = 1000
                        output.set(socket)
                        logger.info("connected")
                    } catch (e:Exception) {
//                        logger.error(e.message, e)
                    }
                }
            }
        }
    }

    override suspend fun onFormatChange(format: AudioFormat) {
        if (logger.isDebugEnabled) logger.debug("formatChanged {}", format)
        bitSize = format.sampleSizeInBits.toByte()
        channels = format.channels.toByte()
        encodedSampleRate = ScreamUtils.encodeSampleRate(format.sampleRate.toInt())
    }

    override suspend fun onPcmData(data: ByteBuffer) {
        val payloadSize = data.remaining() + 5
        if (logger.isDebugEnabled) logger.debug("payloadSize {}", payloadSize)
        output.get()?.getOutputStream()?.let {
            runCatching {
                if (logger.isDebugEnabled) logger.debug("sending to {}", host)
                it.write(
                    byteArrayOf(
                        (payloadSize ushr 8).toByte(),
                        payloadSize.toByte(),
                        encodedSampleRate,
                        bitSize,
                        channels,
                        0,
                        0
                    )
                )

                if (logger.isDebugEnabled) logger.debug("sent header to {}", host)

                it.write(data.array(), data.position(), data.remaining())
                if (logger.isDebugEnabled) logger.debug("sent payload to {}", host)
            }.getOrElse {
                output.set(null)
                logger.error(it.message, it)
            }
        }
    }


    override suspend fun onClose() {
        kotlin.runCatching {
            connectionJob?.cancel()

            output.get()?.close()
        }
    }

}