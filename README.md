# dagupan
A DAG scheduler written using Kotlin Coroutines

### Overview 
A Kotlin Coroutine implementation of a scheduler for an arbitrary set of tasks that are interrelated in a directed acyclic graph . Directed because execution will flow from a given task's dependencies to the task itself . Acyclic because the provided tasks can never have cyclical dependencies . 

### Features
* can schedule and execute any workload in the form `()->Unit` and provide optional handlers for success and/or failure
* easy to declaratively compose the acyclical graph of interdependent tasks using a simple value object 
* task execution can dispatched using the default  Coroutine Dispatcher or can use a thread-pool for concurrent execution 
* a failing task will only cause its downstream dependent tasks to be cancelled . All other workloads will carry on undisturbed 
* tests covering typical scenarios . 100% Code Coverage 

### Usage
A simple vale object `Task` is provided as the means to encode the workloads ,sucess and failure handlers based on the outcome of the workload and the dependencies between workloads . The task has a simple signature.
```
data class Task( val dependsOn: Set<Task> , val success: () -> Unit = {}, val failure: (Throwable) -> Unit ={}, val doWork: () -> Unit)
```
Avoiding cyclical dependencies between tasks comes about naturally from simply declaring the tasks one by one and assigning dependencies as requred from the tasks that have been decalred bfore .
```
        val task0 = Task(emptySet()) { testList.add("task0") }
        val task1 = Task(setOf(task0)) { testList.add("task1") }
        val task2 = Task(emptySet()) { testList.add("task2") }
        val task3 = Task(setOf(task0,task1)) { testList.add("task3") }
```
Tasks built up in this way cannot have cyclical dependencies since it is not possible to refer to a task that has not yet been declared. 

To execute the tasks pass them in a set to the `TaskExecutor` 
```
  val taskSet = setOf(task0,task1,task2,task3)
  TaskExecutor().execute(taskSet)
```
there is an optional second parameter which can be used if concurrent thread pool execution is reqired 
```
  val taskSet = setOf(task0,task1,task2,task3)
  TaskExecutor().execute(taskSet,Executors.newFixedThreadPool(4).asCoroutineDispatcher())
```
Calling thread is blocked until the executor completes execution. Cancellation and workload timeouts is on the Todo list.

### Todo
* workload timeouts
* Non blocking execution submission - Cancellation of the entire taskSet.
* Passing results to and from workloads 
* A nice way to assemble tasks from a grouping of workloads that are all methods coming from a family of Spring Beans . 
* not need to do reverse lookup of deferred back to associated task - create a reverse map instead 

Feel free to add to and/or upvote the feature you would like to see implemented.

### Licence
Code is provided without any licence attached to its use . You are welcome to copy and use as you wish for commercial purposes or otherwise . That said if you do come up with anything that may enhance the code in any way such as a bug fix or a suggestion for improvement or extra functionality please do let me know by whatever means suits be that by pull request or by just emailing diarmuidkeane `at` gmail `dot` com

