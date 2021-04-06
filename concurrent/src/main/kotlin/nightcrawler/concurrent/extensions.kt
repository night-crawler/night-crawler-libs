package nightcrawler.concurrent

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future


fun <T> List<Future<T>>.cancelAll(startFrom: Int = 0) {
    subList(startFrom, size).forEach {
        it.cancel(true)
    }
}

fun <T> List<Future<T>>.blockWait() {
    filterNot { it.isDone }.forEach {
        try {
            it.get()
        } catch (ignore: CancellationException) {
        } catch (ignore: ExecutionException) {
        }
    }
}

