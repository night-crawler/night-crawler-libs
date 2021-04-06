package nightcrawler.concurrent

import nightcrawler.concurrent.exception.HashExecutorServiceException
import nightcrawler.concurrent.exception.HashExecutorServiceInterruptedException
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Function


class HashExecutorService(
    pool: Collection<ExecutorService>,
    private val bucketMapper: Function<Any, Int>
) : ExecutorService {
    companion object {
        @JvmStatic
        fun of(poolSize: Int, numThreadsPerPool: Int): HashExecutorService {
            val pool = (0 until poolSize).map {
                Executors.newFixedThreadPool(numThreadsPerPool)
            }
            return HashExecutorService(pool) {
                it.hashCode() % poolSize
            }
        }

        @JvmStatic
        fun singleThreaded(poolSize: Int): HashExecutorService {
            return of(poolSize, numThreadsPerPool = 1)
        }
    }

    private val pool = pool.toTypedArray()

    override fun execute(command: Runnable) {
        pool[command.index].execute(command)
    }

    override fun shutdown() {
        doSafe { shutdown() }
    }

    override fun shutdownNow(): List<Runnable> {
        return doSafe { shutdownNow() }.flatten()
    }

    override fun isShutdown(): Boolean {
        return doSafe { isShutdown }.all { it }
    }

    override fun isTerminated(): Boolean {
        return doSafe { isTerminated }.all { it }
    }

    @Throws(InterruptedException::class)
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return doSafe { awaitTermination(timeout, unit) }.all { it }
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> {
        return pool[task.index].submit(task)
    }

    override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
        return pool[task.index].submit(task, result)
    }

    override fun submit(task: Runnable): Future<*> {
        return pool[task.index].submit(task)
    }

    @Throws(InterruptedException::class)
    override fun <T : Any?> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>> {
        val futures = tasks.map { FutureTask(it) }

        try {
            tasks.forEachIndexed { index, callable ->
                val future = futures[index]
                pool[callable.index].execute(future)
            }
            futures.blockWait()

            return futures
        } catch (ex: Throwable) {
            futures.cancelAll()
            throw ex
        }
    }

    @Throws(InterruptedException::class)
    override fun <T : Any?> invokeAll(
        tasks: Collection<Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): List<Future<T>> {
        val nanos = unit.toNanos(timeout)
        val deadline = System.nanoTime() + nanos
        val futures = tasks.map { FutureTask(it) }
        try {
            tasks.forEachIndexed { index, callable ->
                val future = futures[index]
                if (deadline < System.nanoTime()) {
                    futures.cancelAll()
                    return futures
                }
                pool[callable.index].execute(future)
            }

            futures.forEachIndexed() { index, future ->
                if (!future.isDone) {
                    try {
                        future.get(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)
                    } catch (ignore: CancellationException) {
                    } catch (ignore: ExecutionException) {
                    } catch (ex: TimeoutException) {
                        futures.cancelAll(index)
                        return futures
                    }
                }
            }

            return futures
        } catch (ex: Throwable) {
            futures.cancelAll()
            throw ex
        }
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun <T : Any?> invokeAny(tasks: Collection<Callable<T>>): T {
        return doInvokeAny(tasks, false, 0)
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun <T : Any?> invokeAny(tasks: Collection<Callable<T>>, timeout: Long, unit: TimeUnit): T {
        return doInvokeAny(tasks, true, unit.toNanos(timeout))
    }

    private fun <T> doSafe(block: ExecutorService.() -> T): List<T> {
        val errors = mutableListOf<Throwable>()
        var hasInterruptedException = false
        val results = pool.mapNotNull { executorService ->
            try {
                block.invoke(executorService)
            } catch (ex: Throwable) {
                if (ex is InterruptedException) {
                    hasInterruptedException = true
                }
                errors.add(ex)
                null
            }
        }

        if (errors.isNotEmpty()) {
            if (hasInterruptedException) {
                throw HashExecutorServiceInterruptedException(errors)
            }
            throw HashExecutorServiceException(errors)
        }
        return results
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    private fun <T : Any?> doInvokeAny(
        tasks: Collection<Callable<T>>,
        timed: Boolean,
        timeout: Long
    ): T {
        var nanos = timeout
        var ntasks = tasks.size
        require(ntasks != 0)
        val futures = ArrayList<Future<T>>(ntasks)
        val ecs = HashExecutorCompletionService<T>(this)

        // For efficiency, especially in executors with limited
        // parallelism, check to see if previously submitted tasks are
        // done before submitting more of them. This interleaving
        // plus the exception mechanics account for messiness of main
        // loop.
        try {
            // Record exceptions so that if we fail to obtain any
            // result, we can throw the last exception we got.
            var ee: ExecutionException? = null
            val deadline = if (timed) System.nanoTime() + nanos else 0L
            val it = tasks.iterator()

            // Start one task for sure; the rest incrementally
            futures.add(ecs.submit(it.next()))
            --ntasks
            var active = 1
            while (true) {
                var f = ecs.poll()
                if (f == null) {
                    if (ntasks > 0) {
                        --ntasks
                        futures.add(ecs.submit(it.next()))
                        ++active
                    } else if (active == 0) {
                        break
                    } else if (timed) {
                        f = ecs.poll(nanos, TimeUnit.NANOSECONDS)
                        if (f == null) throw TimeoutException()
                        nanos = deadline - System.nanoTime()
                    } else {
                        f = ecs.take()
                    }
                }
                if (f != null) {
                    --active
                    ee = try {
                        return f.get()
                    } catch (eex: ExecutionException) {
                        eex
                    } catch (rex: RuntimeException) {
                        ExecutionException(rex)
                    }
                }
            }
            if (ee == null) {
                ee = ExecutionException(null)
            }
            throw ee
        } finally {
            futures.cancelAll()
        }
    }

    private val Runnable.index get() = bucketMapper.apply(this)

    private val Callable<*>.index get() = bucketMapper.apply(this)
}
