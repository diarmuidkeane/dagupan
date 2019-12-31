package dagupan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class TaskExecutor(private val scope : CoroutineScope= CoroutineScope(SupervisorJob())) {
    private val resultMap = mutableMapOf<Task, Deferred<Unit>>()

    fun execute(taskSet: Set<Task>, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        runBlocking(context = dispatcher) {
             taskSet.map(::fromTaskAsync).map { kickOff(it) }
            }
    }

    private fun fromTaskAsync(task: Task): Deferred<Unit> {
        return resultMap[task]?.let { it } ?: prepareAsync(task)
    }

    private fun prepareAsync(task: Task): Deferred<Unit> {
        resultMap[task] = scope.async(start = CoroutineStart.LAZY) {
            task.dependsOn.forEach {
                resultMap[it]?.run {
                    if (!isActive) {
                        join()
                    }
                }
            }
            task.doWork()
        }
        return  resultMap[task]!!
    }

    private suspend fun kickOff(deferred: Deferred<Unit>) {
        val task = reverseLookup(deferred)
        runCatching {
            task.dependsOn.forEach {
                resultMap[it]?.run {
                    if (isCancelled) {
                        deferred.cancel()
                    }
                }
            }
            if(!deferred.isCancelled) deferred.await()
        }.onFailure { task.failure(it) }.onSuccess { task.success() }
    }

    private fun reverseLookup(deferred: Deferred<Unit>) =
            resultMap.filterValues { it == deferred }.keys.first()
}