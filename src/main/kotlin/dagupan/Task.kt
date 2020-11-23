package dagupan

import java.time.LocalDateTime

sealed class TaskReport(private val name: String) {
    data class Successful(val name: String, val threadName: String, val startTime: LocalDateTime, val endTime: LocalDateTime) : TaskReport(name)
    data class Cancelled(val name: String) : TaskReport(name)
    data class Failed(val name: String, val threadName: String, val startTime: LocalDateTime) : TaskReport(name)
}

data class Task(val name: String = "Task", val dependsOn: Set<Task> = emptySet(), val success: () -> Unit = {}, val failure: (Throwable) -> Unit = {}, val theWork: suspend () -> Unit) {
    suspend fun doWork(): TaskReport {
        val startTime = LocalDateTime.now()
        val threadName = Thread.currentThread().name
        return try {
            theWork()
            val endTime = LocalDateTime.now()
            TaskReport.Successful(name, threadName, startTime, endTime)
        }catch (ex:Throwable){
            TaskReport.Failed(name,threadName,startTime)
        }
    }
}
