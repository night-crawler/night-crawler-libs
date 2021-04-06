package nightcrawler.collections

interface IBucketQueue<K, V> {
    val size: Int
    val numBuckets: Int
    val buckets: List<K>
    fun add(key: K, element: V): Collection<V>
    fun addAll(key: K, elements: Collection<V>): Collection<V>
    fun pop(inclusiveKey: K): Collection<V>
}
