package powerdancer.screamer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import powerdancer.dsp.event.Bump
import powerdancer.dsp.event.Event
import powerdancer.dsp.event.FormatChange
import powerdancer.dsp.event.PcmData
import powerdancer.dsp.filter.AbstractFilter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ScreamUnicastAudioReceiver(port: Int) : AbstractFilter() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(ScreamMulticastAudioReceiver::class.java)
    }

    private val screamSocket = DatagramSocket(port).apply {
        soTimeout = 100
    }
    private val buf: ByteBuffer = ByteBuffer.allocate(1157).order(ByteOrder.LITTLE_ENDIAN)
    private var encodedSampleRate: Byte = 0
    private var bitSize: Byte = 0
    var channels: Byte = 0

    override suspend fun onBump(): Flow<Event> = flow {

        buf.clear()

        val packet = DatagramPacket(buf.array(), 1157)
        try {
            screamSocket.receive(packet)
        } catch (e: SocketTimeoutException) {
            return@flow
        }
        buf.limit(packet.length)

        val newEncodedSampleRate = buf.get()
        val newBitSize = buf.get()
        val newChannels = buf.get()
        // channel mapping (2 bytes)
        buf.get()
        buf.get()
        if (
            (encodedSampleRate != newEncodedSampleRate) ||
            (bitSize != newBitSize) ||
            (channels != newChannels)
        ) {
            encodedSampleRate = newEncodedSampleRate
            bitSize = newBitSize
            channels = newChannels
            emit(
                FormatChange(
                    ScreamUtils.audioFormat(
                        ScreamUtils.decodeSampleRate(encodedSampleRate),
                        bitSize.toInt(),
                        channels.toInt()
                    )
                )
            )
        }
        emit(PcmData(buf))
        emit(Bump)
    }
        .flowOn(Dispatchers.IO)
}