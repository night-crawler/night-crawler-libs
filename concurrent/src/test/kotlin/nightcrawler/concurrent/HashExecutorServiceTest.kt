package nightcrawler.concurrent

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HashExecutorServiceTest {
    @Test
    fun invokeAll() {
        val returnValues = (0 until 30).toList()
        val executorService = HashExecutorService.singleThreaded(10)
        val tasks = returnValues.map { i ->
            CallableTask(value = i, timeout = 0, hashCode = i % 10)
        }
        val futures = executorService.invokeAll(tasks)
        assertEquals(returnValues, futures.map { it.get() })
    }

    @Test
    fun invokeAllWithTimeout() {
        val returnValues = (0 until 30).toList()
        val executorService = HashExecutorService.singleThreaded(30)
        val tasks = returnValues.map { i ->
            CallableTask(i, i.toLong() * 100, i)
        }
        val futures = executorService.invokeAll(tasks, 2, TimeUnit.SECONDS)
        assertTrue(futures.count { it.isCancelled } <= 10)
    }

    @Test
    fun invokeAny() {
        val executorService = HashExecutorService.singleThreaded(3)
        val result = executorService.invokeAny(GENERIC_TASKS)
        assertEquals(5, result)
    }

    @Test
    fun invokeAnyWithExceptions() {
        val executorService = HashExecutorService.singleThreaded(3)
        assertFailsWith<ExecutionException> {
            executorService.invokeAny(FAILING_TASKS)
        }
    }

    @Test
    fun invokeAnyWithTimeout() {
        val executorService = HashExecutorService.singleThreaded(3)
        val result = executorService.invokeAny(GENERIC_TASKS, 400, TimeUnit.MILLISECONDS)
        assertEquals(5, result)
    }

    @Test
    fun shutdown() {
        val executorService = HashExecutorService.singleThreaded(3)
        assertFalse(executorService.isShutdown)
        assertFalse(executorService.isTerminated)
        executorService.invokeAll(GENERIC_TASKS)
        executorService.shutdown()
        assertTrue(executorService.isShutdown)
    }

    @Test
    fun shutdownNow() {
        val executorService = HashExecutorService.singleThreaded(3)
        assertFalse(executorService.isShutdown)
        assertFalse(executorService.isTerminated)
        executorService.invokeAll(GENERIC_TASKS)
        val list = executorService.shutdownNow()
        assertTrue(executorService.isShutdown)

        assertEquals(0, list.size)
    }

    @Test
    fun awaitTermination() {
        val executorService = HashExecutorService.singleThreaded(3)
        executorService.invokeAll(GENERIC_TASKS)
        val result = executorService.awaitTermination(0, TimeUnit.SECONDS)
        assertFalse(result)
    }

    @Test
    fun submitCallable() {
        val executorService = HashExecutorService.singleThreaded(3)
        val future = executorService.submit(Callable { 1 })
        assertEquals(1, future.get())
    }

    @Test
    fun submitRunnable() {
        val executorService = HashExecutorService.singleThreaded(3)
        val future = executorService.submit { }
        assertEquals(null, future.get())
    }

    @Test
    fun submitRunnableWithResult() {
        val executorService = HashExecutorService.singleThreaded(3)
        val future = executorService.submit({ }, 10)
        assertEquals(10, future.get())
    }

    companion object {
        val GENERIC_TASKS = listOf(
            CallableTask(value = 1, timeout = 100, hashCode = 0, doFail = false),
            CallableTask(value = 2, timeout = 200, hashCode = 0, doFail = false),

            CallableTask(value = 3, timeout = 50, hashCode = 1, doFail = false),
            CallableTask(value = 4, timeout = 200, hashCode = 1, doFail = false),

            CallableTask(value = 5, timeout = 10, hashCode = 2, doFail = false),
            CallableTask(value = 6, timeout = 200, hashCode = 2, doFail = false),
        )

        val FAILING_TASKS = listOf(
            CallableTask(value = 1, timeout = 100, hashCode = 0, doFail = true),
            CallableTask(value = 2, timeout = 200, hashCode = 0, doFail = true),

            CallableTask(value = 3, timeout = 50, hashCode = 1, doFail = true),
            CallableTask(value = 4, timeout = 200, hashCode = 1, doFail = true),

            CallableTask(value = 5, timeout = 10, hashCode = 2, doFail = true),
            CallableTask(value = 6, timeout = 200, hashCode = 2, doFail = true),
        )

        class CallableTask(
            private val value: Number,
            private val timeout: Long,
            private val hashCode: Int,
            private val doFail: Boolean = false
        ) :
            Callable<Int> {
            override fun call(): Int {
                if (timeout > 0) {
                    Thread.sleep(timeout)
                }
                if (doFail) {
                    throw RuntimeException()
                }
                return value.toInt()
            }

            override fun hashCode(): Int {
                return hashCode
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is CallableTask) return false
                if (value != other.value) return false
                return true
            }
        }

    }
}
