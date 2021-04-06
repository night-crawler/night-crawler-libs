package nightcrawler.collections


import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConcurrentTimeBucketQueueTest {
    @Test
    fun test() {
        val t1 = Instant.ofEpochMilli(0)
        val queue = ConcurrentTimeBucketQueue<String>(100)
        queue.add(t1.plusMillis(0), "a")
        queue.add(t1.plusMillis(50), "a")
        queue.add(t1.plusMillis(99), "a")
        assertEquals(listOf(t1), queue.buckets)
        assertEquals(3, queue.size)

        queue.add(t1.plusMillis(100), "a")
        queue.add(t1.plusMillis(150), "a")
        queue.add(t1.plusMillis(199), "a")
        assertEquals(listOf(t1, t1.plusMillis(100)), queue.buckets)
        assertEquals(6, queue.size)

        queue.add(t1.plusMillis(200), "a")
        queue.add(t1.plusMillis(250), "a")
        queue.add(t1.plusMillis(299), "a")
        assertEquals(listOf(t1, t1.plusMillis(100), t1.plusMillis(200)), queue.buckets)
        assertEquals(9, queue.size)

        val elements = queue.pop(t1.plusMillis(150))
        assertEquals(6, elements.size)
        assertEquals(listOf(t1.plusMillis(200)), queue.buckets)
        assertEquals(3, queue.size)

        val lastElements = queue.pop(t1.plusMillis(1000000))
        assertEquals(3, lastElements.size)
        assertEquals(listOf(), queue.buckets)
        assertEquals(0, queue.size)
        assertEquals(0, queue.numBuckets)
    }
}