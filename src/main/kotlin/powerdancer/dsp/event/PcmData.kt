package powerdancer.dsp.event

import powerdancer.dsp.ObjectPool
import java.nio.ByteBuffer

data class PcmData (
    val data: ByteBuffer
): Event {
    companion object {
        private val bufferPool = ObjectPool<ByteBuffer> {
            ByteBuffer.allocate(20000)
        }

        suspend fun takeBufferFromPool(minSize: Int): ByteBuffer {
            val b = bufferPool.take()
            return if (b.capacity() < minSize) {
                ByteBuffer.allocate(minSize * 2)
            } else {
                b
            }
        }

        suspend fun putBufferIntoPool(b: ByteBuffer) {
            bufferPool.put(b)
        }
    }

    override suspend fun clone(): Pair<Event, suspend () -> Unit> {
        data.mark()
        val copy = takeBufferFromPool(data.remaining())
            .clear()
            .put(data)
            .flip()
        data.reset()

        return PcmData(copy) to {
            putBufferIntoPool(copy)
        }
    }
}