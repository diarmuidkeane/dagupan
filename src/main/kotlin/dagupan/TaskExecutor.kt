package dagupan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope

class TaskExecutor {

    private val resultMap = mutableMapOf<Task, Deferred<Unit>>()

    fun execute(taskSet: Set<Task>, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        runBlocking(dispatcher) {
            supervisorScope {
                 fun prepareAsync(task: Task): Deferred<Unit> {
                    resultMap[task] = async(start = CoroutineStart.LAZY) {
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

                fun fromTaskAsync(task: Task): Deferred<Unit> {
                    return resultMap[task]?.let { it } ?: prepareAsync(task)
                }

                taskSet.map { fromTaskAsync(it) }
                        .forEach { kickOff(it) }
            }
        }
    }

    private suspend fun kickOff(deferred: Deferred<Unit>) {
        runCatching {
            reverseLookup(deferred).dependsOn.forEach {
                resultMap[it]?.run {
                    if (isCancelled) {
                        deferred.cancel()
                    }
                }
            }
            if(!deferred.isCancelled) deferred.await()
        }.onFailure { /**/ }.onSuccess { /**/ }
    }

    private fun reverseLookup(deferred: Deferred<Unit>) =
            resultMap.filterValues { it == deferred }.keys.first()
}