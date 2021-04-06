package nightcrawler.concurrent

import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals

class HashExecutorCompletionServiceTest {
    @Test
    fun submitCallable() {
        val service = HashExecutorCompletionService<Int>(Executors.newSingleThreadExecutor())
        val future = service.submit { 1 }
        assertEquals(1, future.get())
    }

    @Test
    fun submitRunnable() {
        val service = HashExecutorCompletionService<Int>(Executors.newSingleThreadExecutor())
        val future = service.submit({ }, 1)
        assertEquals(1, future.get())
    }
}
