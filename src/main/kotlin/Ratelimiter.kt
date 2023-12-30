import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking {
    val requestLimit = 50 // Limiting to 5 requests
    val timeWindowMs = 1000L // Time window of 10 seconds

    // Creating an instance of WindowedRateLimiter
    val rateLimiter = WindowedRateLimiter<Int, String>(requestLimit, timeWindowMs)

    // Creating a flow of integers from 1 to 10,000
    val intFlow = (1..10000).asFlow()

    // Applying rate limited operation on each integer
    intFlow.flatMapConcat { i ->
        rateLimiter.invokeRateLimited(i) { number ->
            // Defining the operation to be rate limited
            "Processed $number at ${System.currentTimeMillis()}"
        }
    }.collect { result ->
        // Collecting and handling the result
        println(result)
    }
}

class WindowedRateLimiter<T, R>(private val requestLimit: Int, private val timeWindowMs: Long) {
    private val mutex = Mutex()
    private var invocations = 0
    private var startTime = System.currentTimeMillis()
    fun invokeRateLimited(t: T, function: (T) -> R): Flow<R> = flow {
        mutex.withLock {
            val currentTime = System.currentTimeMillis()
            if (invocations >= requestLimit && currentTime - startTime < timeWindowMs) {
                delay(timeWindowMs - (currentTime - startTime))
                invocations=0
                startTime = System.currentTimeMillis()
            }

            emit(function(t))
            invocations++
        }
    }
}

class LeakyBucketRateLimiter(private val capacity: Int) {
    private var tokens = AtomicInteger(capacity)
    private var addTokenJob: Job? = null

    fun launchAddTokenTask() {
        if (addTokenJob == null || !addTokenJob!!.isActive) {
            addTokenJob = GlobalScope.launch {
                val interval = 1000L // 1 second
                while (isActive) {
                    addToken()
                    delay(interval)
                }
                delay(10000)
                stopAddingTokens()
            }
        }
    }


    fun stopAddingTokens() {
        addTokenJob?.cancel()
    }

    fun addToken() {
        if(tokens.get() < capacity) {
            println("Added token to the bucket, Total tokens available ${tokens.incrementAndGet()}")
        }
    }

    fun getToken(): Boolean {
        return if (tokens.get() > 0) {
            println("Token give. Tokens available now: ${tokens.decrementAndGet()}")
            true
        } else {
            false
        }
    }
}

