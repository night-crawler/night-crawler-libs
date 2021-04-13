package nightcrawler.concurrent

import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue

class HashExecutorCompletionService<T : Any?>(
    private val executor: Executor,
    private val completionQueue: BlockingQueue<Future<T>> = LinkedBlockingQueue()
) : ExecutorCompletionService<T>(executor, completionQueue) {

    override fun submit(task: Callable<T>): Future<T> {
        val futureTask = HashCodeFutureTask(task, completionQueue)
        executor.execute(futureTask)
        return futureTask
    }

    override fun submit(task: Runnable, result: T): Future<T> {
        val futureTask = HashCodeFutureTask(task, result, completionQueue)
        executor.execute(futureTask)
        return futureTask
    }

    private class HashCodeFutureTask<T> : FutureTask<T> {
        private var completionQueue: BlockingQueue<Future<T>>
        private var hashCodeValue: Int
        private var callable: Callable<T>

        constructor(callable: Callable<T>, completionQueue: BlockingQueue<Future<T>>) : super(callable) {
            this.hashCodeValue = callable.hashCode()
            this.callable = callable
            this.completionQueue = completionQueue
        }

        constructor(runnable: Runnable, result: T, completionQueue: BlockingQueue<Future<T>>) : super(
            runnable,
            result
        ) {
            this.hashCodeValue = runnable.hashCode()
            this.callable = Executors.callable(runnable, result)
            this.completionQueue = completionQueue
        }

        override fun done() {
            completionQueue.add(this)
        }

        override fun hashCode(): Int = hashCodeValue
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HashCodeFutureTask<*>) return false

            if (callable != other.callable) return false

            return true
        }
    }
}
