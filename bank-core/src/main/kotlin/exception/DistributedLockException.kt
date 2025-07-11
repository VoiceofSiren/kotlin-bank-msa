package exception

class LockAcquisitionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)