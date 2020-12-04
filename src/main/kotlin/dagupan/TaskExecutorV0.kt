package dagupan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

@Deprecated("Previous implementation for reference - to be removed")
class TaskExecutorV0(private val scope : CoroutineScope= CoroutineScope(SupervisorJob())) {
    private val resultMap = mutableMapOf<Task, Deferred<TaskReport>>()

    fun execute(taskSet: Set<Task>, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        runBlocking(context = dispatcher) {
             taskSet.map(::fromTaskAsync).map { kickOff(it) }
            }
    }

    private fun fromTaskAsync(task: Task): Deferred<TaskReport> {
        return resultMap[task]?.let { it } ?: prepareAsync(task)
    }

    private fun prepareAsync(task: Task): Deferred<TaskReport> {
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

    private suspend fun kickOff(deferred: Deferred<TaskReport>) {
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

    private fun reverseLookup(deferred: Deferred<TaskReport>) =
            resultMap.filterValues { it == deferred }.keys.first()
}