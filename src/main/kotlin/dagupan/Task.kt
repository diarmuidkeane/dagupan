package dagupan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

data class Task(private val dependsOn: Set<Task>, private val doThis: () -> Unit) {
    var deferred: Deferred<Unit>?=null

    fun prepare(coroutineScope: CoroutineScope) : Task {
        deferred = coroutineScope.async(start = CoroutineStart.LAZY) {
            dependsOn.forEach {
                val deferred = it.deferred
                if (deferred!=null && !deferred.isActive) {
                    deferred.join()
                }
            }
            doThis()
        }
        return this
    }
}