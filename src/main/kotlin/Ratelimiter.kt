import kotlinx.coroutines.*
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

data class RateLimiterConfig(val rate: Int, val unit: ChronoUnit, val concurrency: Int)

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

