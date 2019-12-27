package dagupan

data class Task( val dependsOn: Set<Task> , val success: () -> Unit = {}, val failure: (Throwable) -> Unit ={}, val doWork: () -> Unit)