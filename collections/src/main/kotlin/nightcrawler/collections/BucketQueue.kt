package nightcrawler.collections

import java.util.TreeSet
import java.util.function.Function

class BucketQueue<K, V>(
    private val bucketResolver: Function<K, K> = Function { k -> k },
    private val collectionInitializer: Function<K?, MutableCollection<V>> = Function { mutableListOf() }
) : IBucketQueue<K, V> {

    private val bucketsInternal: MutableMap<K, MutableCollection<V>> = mutableMapOf()
    private val knownBuckets = TreeSet<K>()
    private var sizeInternal: Int = 0

    override fun add(key: K, element: V): Collection<V> {
        return bucketsInternal.compute(bucketResolver.apply(key)) { bucket, old ->
            val oldSize = old?.size ?: 0
            val newCollection = old
                ?.apply { add(element) }
                ?: collectionInitializer.apply(bucket).apply { add(element) }

            knownBuckets.add(bucket)
            sizeInternal += newCollection.size - oldSize

            newCollection
        }!!
    }

    override fun addAll(key: K, elements: Collection<V>): Collection<V> {
        val bucket = bucketResolver.apply(key)

        if (elements.isEmpty()) {
            return elements
        }

        return bucketsInternal.compute(bucket) { _, old ->
            val oldSize = old?.size ?: 0
            val newCollection = old
                ?.apply { addAll(elements) }
                ?: collectionInitializer.apply(bucket).apply { addAll(elements) }

            knownBuckets.add(bucket)
            sizeInternal += newCollection.size - oldSize

            newCollection
        }!!
    }

    override fun pop(inclusiveKey: K): Collection<V> {
        val keys = knownBuckets.headSet(inclusiveKey, true).toList()
        val elements = keys
            .mapNotNull { key ->
                bucketsInternal.remove(key)?.also { bucket ->
                    sizeInternal -= bucket.size
                }
            }
            .flatten()

        knownBuckets.removeAll(keys)

        return collectionInitializer.apply(null).apply {
            addAll(elements)
        }
    }

    override val size: Int get() = sizeInternal
    override val numBuckets: Int get() = bucketsInternal.size
    override val buckets: List<K> get() = knownBuckets.toList()
}
