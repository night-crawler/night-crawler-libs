package nightcrawler.collections

import java.time.Instant
import java.util.function.Function

class ConcurrentTimeBucketQueue<T>(
    resolutionMs: Long,
    private val collectionInitializer: Function<Instant?, MutableCollection<T>> = Function { mutableListOf() },
    private val bucketResolver: Function<Instant, Instant> = Function { instant ->
        Instant.ofEpochMilli(instant.toEpochMilli() / resolutionMs * resolutionMs)
    },
    private val delegate: IBucketQueue<Instant, T> = ConcurrentBucketQueue(
        BucketQueue(
            bucketResolver,
            collectionInitializer
        )
    )
) : IBucketQueue<Instant, T> by delegate
