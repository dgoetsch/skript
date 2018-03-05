package dev.yn.playground.vertx.task

import dev.yn.playground.task.result.AsyncResult
import dev.yn.playground.task.result.CompletableResult
import dev.yn.playground.task.result.Result
import io.vertx.core.Future
import io.vertx.core.Vertx

/**
 * interface for providing vertx instance to VertxTasks
 */
interface VertxProvider {
    fun provideVertx(): AsyncResult<Vertx>
}

interface VertxTaskContext {
    fun getVertx(): Vertx
}

class VertxResult<T>(val future: Future<T>): AsyncResult<T> {
    override fun setHandler(handler: (Result<T>) -> Unit) {
        future.setHandler {
            when {
                it.succeeded() -> handler(Result.Success(it.result()))
                else -> handler(Result.Failure(it.cause()))
            }
        }
    }

    override fun <U> map(f: (T) -> U): AsyncResult<U> {
        return VertxResult(future.map(f))
    }

    override fun <U> flatMap(f: (T) -> AsyncResult<U>): AsyncResult<U> {
        val result = CompletableResult<U>()
        future.setHandler {
            if(it.succeeded()) {
                f(it.result()).setHandler(result.completionHandler())
            } else {
                result.fail(it.cause())
            }
        }
        return result
    }

    override fun recover(f: (Throwable) -> AsyncResult<T>): AsyncResult<T> {
        val result = CompletableResult<T>()
        future.setHandler {
            if(it.succeeded()) {
                result.succeed(it.result())
            } else {
                f(it.cause()).setHandler(result.completionHandler())
            }
        }
        return result
    }

    override fun isComplete(): Boolean {
        return future.isComplete
    }

    override fun isSuccess(): Boolean {
        return future.succeeded()
    }

    override fun isFailure(): Boolean {
        return future.failed()
    }

    override fun result(): T? {
        return future.result()
    }

    override fun error(): Throwable? {
        return future.cause()
    }

}