package dagupan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class TaskExecutor {

    fun execute(taskSet: Set<Task>, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        runBlocking(dispatcher) {
            val resultMap = mutableMapOf<Task, Deferred<Unit>>()

            fun fromTaskAsync(task: Task): Deferred<Unit> {
                val existingResult = resultMap[task]
                return if (existingResult != null) existingResult
                else {
                    resultMap[task] = async(start = CoroutineStart.LAZY) {
                        task.dependsOn.forEach {
                            resultMap[it]?.run { if (!isActive) { join() } }
                        }
                        task.doWork()
                    }
                    resultMap[task]!!
                }
            }

            taskSet.map { fromTaskAsync(it) }
                    .forEach { it.await() }
        }
    }


}