package dagupan

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

class PrototypeTest {
    private val taskMap: MutableMap<String, Task> = mutableMapOf()

    private fun countIt(maxLimit: Int): Unit {
        var i = 0
        while (i < maxLimit)
            i++
    }

    @BeforeEach
    fun setup() {
        Task("task0", emptySet()) { countIt(1000000000) }.also { taskMap[it.name] = it }
        val task1 = Task("task1", emptySet()) { countIt(1000000000) }.also { taskMap[it.name] = it }
        val task2 = Task("task2", emptySet()) { countIt(1000000000) }.also { taskMap[it.name] = it }
        val task3 = Task("task3", setOf(task1)) { countIt(1000) }.also { taskMap[it.name] = it }
        val task4 = Task("task4", emptySet()) { countIt(1000000000) }.also { taskMap[it.name] = it }
        Task("task5", emptySet()) { countIt(1000) }.also { taskMap[it.name] = it }
        Task("task6", setOf(task3, task1)) { countIt(1000) }.also { taskMap[it.name] = it }
        val task7 = Task("task7", setOf(task4, task1)) { countIt(1000) }.also { taskMap[it.name] = it }
        Task("task8", setOf(task2, task7)) { countIt(1000) }.also { taskMap[it.name] = it }
        Task("task9", emptySet()) { countIt(1000) }.also { taskMap[it.name] = it }
    }

//    @Test
//    fun testReverseDependencyGraph() {
//
//        val result = TaskExecutorV2().reverseDependencyGraph(taskMap.values.toSet())
//        assertThat(result[taskMap["task0"]], `is`(emptySet()))
//        assertThat(
//                result[taskMap["task1"]],
//                `is`(setOf(
//                        taskMap["task3"],
//                        taskMap["task6"],
//                        taskMap["task7"]
//                )))
//    }


    @Test
    fun case0() {
        val resultMap = runBlocking {
            val taskExecutorV2 = TaskExecutorV2(this)
            taskExecutorV2.submit(taskMap)
        }
//        assertThat(resultMap["task3"]?.startTime?.compareTo(resultMap["task1"]?.endTime), greaterThanOrEqualTo<Int>(0))
//        assertThat(resultMap["task6"]?.startTime?.compareTo(resultMap["task3"]?.endTime), greaterThanOrEqualTo<Int>(0))
//        assertThat(resultMap["task6"]?.startTime?.compareTo(resultMap["task1"]?.endTime), greaterThanOrEqualTo<Int>(0))
//        assertThat(resultMap["task7"]?.startTime?.compareTo(resultMap["task4"]?.endTime), greaterThanOrEqualTo<Int>(0))
//        assertThat(resultMap["task7"]?.startTime?.compareTo(resultMap["task1"]?.endTime), greaterThanOrEqualTo<Int>(0))
//        assertThat(resultMap["task8"]?.startTime?.compareTo(resultMap["task7"]?.endTime), greaterThanOrEqualTo<Int>(0))
//        assertThat(resultMap["task8"]?.startTime?.compareTo(resultMap["task2"]?.endTime), greaterThanOrEqualTo<Int>(0))
//        assertThat(resultMap.values.all { it.success }, `is`(true))
    }

    @Test
    fun case1() {
        val newTask1 = Task("task1", emptySet()) { throw RuntimeException() }.also { taskMap[it.name] = it }
        val newTask3 = Task("task3", setOf(newTask1)) { countIt(1000) }.also { taskMap[it.name] = it }
        Task("task6", setOf(newTask3, newTask1)) { countIt(1000) }.also { taskMap[it.name] = it }
        val newTask7 = Task("task7", setOf(taskMap["task4"]!!, newTask1)) { countIt(1000) }.also { taskMap[it.name] = it }
        Task("task8", setOf(taskMap["task2"]!!, newTask7)) { countIt(1000) }.also { taskMap[it.name] = it }
        val resultMap = runBlocking {
            val taskExecutorV2 = TaskExecutorV2(this)
            taskExecutorV2.submit(taskMap)
        }
//        assertThat(resultMap["task0"]?.success, `is`(true))
//        assertThat(resultMap["task1"]?.success, `is`(false))
//        assertThat(resultMap["task2"]?.success, `is`(true))
//        assertThat(resultMap["task3"], nullValue())
//        assertThat(resultMap["task4"]?.success, `is`(true))
//        assertThat(resultMap["task5"]?.success, `is`(true))
//        assertThat(resultMap["task6"], nullValue())
//        assertThat(resultMap["task7"], nullValue())
//        assertThat(resultMap["task8"], nullValue())
//        assertThat(resultMap["task9"]?.success, `is`(true))
    }


    @Test
    fun anotherTest() {
        runBlocking {
            launch {
                taskMap["task0"]!!.doWork()
            }
        }
    }


    @FlowPreview
    @Test
    fun firstTest() {
        val task = taskMap["task3"]!!
//        val flow1 = if (task.dependsOn.isEmpty()) flowOf(task.doWork) else  task.dependsOn.map { it.doWork }.asFlow(){it->flowOf(task.doWork)}
        val mainFlow = flow {
            // flow builder
            task.dependsOn.forEach {
                emit(it.theWork) // emit next value
            }
            emit(task.theWork)
        }

//        val flowWork: Flow<() -> Unit> = flowOf(task.theWork)
        val dependencies = task.dependsOn.map { flowOf(it.theWork) }

//        val foldedFlow = taskMap.values
//                .fold(listOf<Flow<() -> Unit>>()) { acc, someTask -> acc + flowOfTask(someTask) }
//                .reduce { flow1, flow2 -> flow1.flatMapConcat { flow2 } }

        runBlocking { taskMap.values.map { it.theWork }.asFlow() }
    }

//    private fun flowOfTask(task: Task): List<Flow<() -> Unit>> = task.dependsOn.map { flowOfTask(it) }.toList().flatten() + flowOf(task.theWork)

}

