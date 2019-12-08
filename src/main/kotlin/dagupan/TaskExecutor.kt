package dagupan

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class TaskExecutor {

    fun execute(taskSet: Set<Task>, dispatcher: CoroutineDispatcher = Dispatchers.Default) {
        runBlocking (dispatcher){
            taskSet.map { it.prepare(this)}.forEach {  it.deferred?.await() }
        }
    }
}