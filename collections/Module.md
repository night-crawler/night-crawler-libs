# Module collections

Custom collections

# Package demo

## ConcurrentTimeBucketQueue

```kotlin

fun sample() {
    val t1 = Instant.ofEpochMilli(0)

    // 100ms resolution
    val queue = ConcurrentTimeBucketQueue<String>(100)
    queue.add(t1.plusMillis(0), "a")
    queue.add(t1.plusMillis(50), "a")
    queue.add(t1.plusMillis(99), "a")
    assertEquals(listOf(t1), queue.buckets)
    assertEquals(3, queue.size)

    val elements = queue.pop(t1.plusMillis(150))
    assertEquals(6, elements.size)
    assertEquals(listOf(t1.plusMillis(200)), queue.buckets)
    assertEquals(3, queue.size)
}

```
