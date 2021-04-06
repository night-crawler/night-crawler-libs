package nightcrawler.collections

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock

class ConcurrentBucketQueue<K, V>(
    private val delegate: IBucketQueue<K, V> = BucketQueue()
) : IBucketQueue<K, V> {
    private val lock = ReentrantReadWriteLock()

    override val size: Int
        get() = lock.read() { delegate.size }

    override val numBuckets: Int
        get() = lock.read { delegate.numBuckets }

    override val buckets: List<K>
        get() = lock.read { delegate.buckets }

    override fun add(key: K, element: V): Collection<V> =
        lock.writeLock().withLock {
            delegate.add(key, element)
        }

    override fun addAll(key: K, elements: Collection<V>): Collection<V> =
        lock.writeLock().withLock {
            delegate.addAll(key, elements)
        }

    override fun pop(inclusiveKey: K): Collection<V> =
        lock.writeLock().withLock {
            delegate.pop(inclusiveKey)
        }
}