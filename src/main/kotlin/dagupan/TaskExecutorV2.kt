package dagupan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

class TaskExecutorV2(private val scope: CoroutineScope) {
    private val jobMap = mutableMapOf<String, Job>()
    private val resultMap: MutableMap<String, TaskReport> = mutableMapOf()

    private fun reverseDependencyGraph(taskSet: Set<Task>): Map<Task, Set<Task>> {
        return taskSet.map { task -> Pair(task, taskSet.filter { task in it.dependsOn }.toSet()) }.toMap()
    }

    suspend fun cancel() {
        jobMap.values.map { it.cancelAndJoin() }
        jobMap.keys.map { if (resultMap[it] == null) resultMap[it] = TaskReport.Cancelled(it) } //mark unmapped jobs as cancelled
    }

    fun submit(taskMap: MutableMap<String, Task>): MutableMap<String, TaskReport> {
        val taskSet = taskMap.values.toSet()
        val reverseMap = reverseDependencyGraph(taskSet)

        fun processTasks(taskSet: Set<Task>): MutableMap<String, TaskReport> {

            fun propagateFailure(task: Task) {
                reverseMap[task]?.forEach {
                    jobMap[it.name]?.cancel()
                    resultMap[it.name] = TaskReport.Cancelled(it.name)
                }
            }

            taskSet.forEach {
                if (it.dependsOn.isNotEmpty())
                    processTasks(it.dependsOn)

                if (!jobMap.contains(it.name))
                    jobMap[it.name] = scope.launch {
                        if (!it.dependsOn.any { dep -> jobMap[dep.name]?.isCancelled!! }) {
                            val result = it.doWork()
                            if (result is TaskReport.Failed) propagateFailure(it)
                            resultMap[it.name] = result
                        } else
                            resultMap[it.name] = TaskReport.Cancelled(it.name)
                    }
            }
            return resultMap
        }
        return processTasks(taskSet)
    }

}