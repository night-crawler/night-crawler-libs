package nightcrawler.concurrent

import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RunnableFuture

class HashExecutorCompletionService<T : Any?>(
    private val executor: Executor,
    private val completionQueue: BlockingQueue<Future<T>> = LinkedBlockingQueue()
) : ExecutorCompletionService<T>(executor, completionQueue) {

    override fun submit(task: Callable<T>): Future<T> {
        val futureTask = HashCodeFutureTask(task)
        executor.execute(HashCodeQueueingFuture(futureTask, completionQueue))
        return futureTask
    }

    override fun submit(task: Runnable, result: T): Future<T> {
        val futureTask = HashCodeFutureTask(task, result)
        executor.execute(HashCodeQueueingFuture(futureTask, completionQueue))
        return futureTask
    }

    private class HashCodeFutureTask<T> : FutureTask<T> {
        private var hashCodeValue: Int
        private var callable: Callable<T>

        constructor(callable: Callable<T>) : super(callable) {
            this.hashCodeValue = callable.hashCode()
            this.callable = callable
        }

        constructor(runnable: Runnable, result: T) : super(runnable, result) {
            this.hashCodeValue = runnable.hashCode()
            this.callable = Executors.callable(runnable, result)
        }

        override fun hashCode(): Int = hashCodeValue
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HashCodeFutureTask<*>) return false

            if (callable != other.callable) return false

            return true
        }
    }

    private class HashCodeQueueingFuture<V>(
        private val runnableFuture: RunnableFuture<V>,
        private val completionQueue: BlockingQueue<Future<V>>
    ) : FutureTask<Void>(runnableFuture, null) {
        override fun done() {
            completionQueue.add(runnableFuture)
        }

        override fun hashCode(): Int {
            return runnableFuture.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HashCodeQueueingFuture<*>) return false

            if (runnableFuture != other.runnableFuture) return false
            if (completionQueue != other.completionQueue) return false

            return true
        }
    }
}
