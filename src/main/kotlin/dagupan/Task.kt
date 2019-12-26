package dagupan

data class Task( val dependsOn: Set<Task>, val doWork: () -> Unit)