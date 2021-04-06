package nightcrawler.concurrent.exception

class HashExecutorServiceInterruptedException(val throwables: List<Throwable>) : InterruptedException()
