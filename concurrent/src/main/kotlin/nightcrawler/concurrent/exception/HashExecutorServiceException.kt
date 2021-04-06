package nightcrawler.concurrent.exception

class HashExecutorServiceException(val throwables: List<Throwable>) : RuntimeException()

