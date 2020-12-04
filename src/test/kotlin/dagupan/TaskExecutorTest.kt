package dagupan

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.iterableWithSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

class TaskExecutorTest {

    private val taskMap: MutableMap<String, Task> = mutableMapOf()

    private fun randomCountWorkload(maxLimit: Int = Random.nextInt(10000, Int.MAX_VALUE)): Unit {
        var i = 0
        while (i < maxLimit)
            i++
    }

    @BeforeEach
    fun setup() {
        Task("task0", emptySet()) { randomCountWorkload() }.also { taskMap[it.name] = it }
        val task1 = Task("task1", emptySet()) { randomCountWorkload() }.also { taskMap[it.name] = it }
        val task2 = Task("task2", emptySet()) { randomCountWorkload() }.also { taskMap[it.name] = it }
        val task3 = Task("task3", setOf(task1)) { randomCountWorkload() }.also { taskMap[it.name] = it }
        val task4 = Task("task4", emptySet()) { randomCountWorkload() }.also { taskMap[it.name] = it }
        Task("task5", emptySet()) { randomCountWorkload() }.also { taskMap[it.name] = it }
        Task("task6", setOf(task3, task1)) { randomCountWorkload() }.also { taskMap[it.name] = it }
        val task7 = Task("task7", setOf(task4, task1)) { randomCountWorkload() }.also { taskMap[it.name] = it }
        Task("task8", setOf(task2, task7)) { randomCountWorkload() }.also { taskMap[it.name] = it }
        Task("task9", emptySet()) { randomCountWorkload() }.also { taskMap[it.name] = it }
    }

    @AfterEach
    fun tearDown() {
        taskMap.clear()
    }

    @Test
    fun `execute when run with default dispatcher then task ordering is as expected`() {

        val resultMap = runBlocking {
            val taskExecutorV2 = TaskExecutor(this)
            taskExecutorV2.submit(taskMap)
        }
        assertThat(resultMap.keys, iterableWithSize(10))
        val resultTask0 = resultMap["task0"]
        val resultTask1 = resultMap["task1"]
        val resultTask2 = resultMap["task2"]
        val resultTask3 = resultMap["task3"]
        val resultTask4 = resultMap["task4"]
        val resultTask5 = resultMap["task5"]
        val resultTask6 = resultMap["task6"]
        val resultTask7 = resultMap["task7"]
        val resultTask8 = resultMap["task8"]
        val resultTask9 = resultMap["task9"]

        assertTrue(resultTask0 is TaskReport.Successful)
        assertTrue(resultTask1 is TaskReport.Successful)
        assertTrue(resultTask2 is TaskReport.Successful)
        assertTrue(resultTask3 is TaskReport.Successful)
        assertTrue(resultTask4 is TaskReport.Successful)
        assertTrue(resultTask5 is TaskReport.Successful)
        assertTrue(resultTask6 is TaskReport.Successful)
        assertTrue(resultTask7 is TaskReport.Successful)
        assertTrue(resultTask8 is TaskReport.Successful)
        assertTrue(resultTask9 is TaskReport.Successful)

        assertTrue(resultTask3.startTime >= resultTask1.endTime)
        assertTrue(resultTask6.startTime >= resultTask3.endTime)
        assertTrue(resultTask6.startTime >= resultTask1.endTime)
        assertTrue(resultTask7.startTime >= resultTask4.endTime)
        assertTrue(resultTask7.startTime >= resultTask1.endTime)
        assertTrue(resultTask8.startTime >= resultTask7.endTime)
        assertTrue(resultTask8.startTime >= resultTask2.endTime)
    }

    @Test
    fun `execute when independent workload fails then the other workloads are not disturbed`() {
        Task("task0", emptySet()) { throw RuntimeException() }.also { taskMap[it.name] = it }

        val resultMap = runBlocking {
            val taskExecutorV2 = TaskExecutor(this)
            taskExecutorV2.submit(taskMap)
        }

        assertThat(resultMap.keys, iterableWithSize(10))
        val resultTask0 = resultMap["task0"]
        val resultTask1 = resultMap["task1"]
        val resultTask2 = resultMap["task2"]
        val resultTask3 = resultMap["task3"]
        val resultTask4 = resultMap["task4"]
        val resultTask5 = resultMap["task5"]
        val resultTask6 = resultMap["task6"]
        val resultTask7 = resultMap["task7"]
        val resultTask8 = resultMap["task8"]
        val resultTask9 = resultMap["task9"]

        assertTrue(resultTask0 is TaskReport.Failed)
        assertTrue(resultTask1 is TaskReport.Successful)
        assertTrue(resultTask2 is TaskReport.Successful)
        assertTrue(resultTask3 is TaskReport.Successful)
        assertTrue(resultTask4 is TaskReport.Successful)
        assertTrue(resultTask5 is TaskReport.Successful)
        assertTrue(resultTask6 is TaskReport.Successful)
        assertTrue(resultTask7 is TaskReport.Successful)
        assertTrue(resultTask8 is TaskReport.Successful)
        assertTrue(resultTask9 is TaskReport.Successful)

        assertTrue(resultTask3.startTime >= resultTask1.endTime)
        assertTrue(resultTask6.startTime >= resultTask3.endTime)
        assertTrue(resultTask6.startTime >= resultTask1.endTime)
        assertTrue(resultTask7.startTime >= resultTask4.endTime)
        assertTrue(resultTask7.startTime >= resultTask1.endTime)
        assertTrue(resultTask8.startTime >= resultTask7.endTime)
        assertTrue(resultTask8.startTime >= resultTask2.endTime)
    }

    @Test
    fun `execute when a workload dependency fails then the dependent workload also fails but it's own dependencies and the other workloads are not disturbed`() {
        val newTask3 = Task("task3", setOf(taskMap["task1"]!!)) { randomCountWorkload(1000); throw RuntimeException() }.also { taskMap[it.name] = it }
        Task("task6", setOf(newTask3, taskMap["task1"]!!)) { randomCountWorkload(1000) }.also { taskMap[it.name] = it }

        val resultMap = runBlocking {
            val taskExecutorV2 = TaskExecutor(this)
            taskExecutorV2.submit(taskMap)
        }

        assertThat(resultMap.keys, iterableWithSize(10))
        val resultTask0 = resultMap["task0"]
        val resultTask1 = resultMap["task1"]
        val resultTask2 = resultMap["task2"]
        val resultTask3 = resultMap["task3"]
        val resultTask4 = resultMap["task4"]
        val resultTask5 = resultMap["task5"]
        val resultTask6 = resultMap["task6"]
        val resultTask7 = resultMap["task7"]
        val resultTask8 = resultMap["task8"]
        val resultTask9 = resultMap["task9"]

        assertTrue(resultTask0 is TaskReport.Successful)
        assertTrue(resultTask1 is TaskReport.Successful)
        assertTrue(resultTask2 is TaskReport.Successful)
        assertTrue(resultTask3 is TaskReport.Failed)
        assertTrue(resultTask4 is TaskReport.Successful)
        assertTrue(resultTask5 is TaskReport.Successful)
        assertTrue(resultTask6 is TaskReport.Cancelled)
        assertTrue(resultTask7 is TaskReport.Successful)
        assertTrue(resultTask8 is TaskReport.Successful)
        assertTrue(resultTask9 is TaskReport.Successful)

        assertTrue(resultTask3.startTime >= resultTask1.endTime)
        assertTrue(resultTask7.startTime >= resultTask4.endTime)
        assertTrue(resultTask7.startTime >= resultTask1.endTime)
        assertTrue(resultTask8.startTime >= resultTask7.endTime)
        assertTrue(resultTask8.startTime >= resultTask2.endTime)
    }

    @Test
    fun `execute when a workload dependency fails then all transitively dependent workloads fail but the other workloads are not disturbed`() {
        val newTask1 = Task("task1", emptySet()) { randomCountWorkload(); throw RuntimeException() }.also { taskMap[it.name] = it }
        val newTask3 = Task("task3", setOf(newTask1)) { randomCountWorkload() }.also { taskMap[it.name] = it }
        Task("task6", setOf(newTask3, newTask1)) { randomCountWorkload() }.also { taskMap[it.name] = it }
        val newTask7 = Task("task7", setOf(taskMap["task4"]!!, newTask1)) { randomCountWorkload() }.also { taskMap[it.name] = it }
        Task("task8", setOf(taskMap["task2"]!!, newTask7)) { randomCountWorkload() }.also { taskMap[it.name] = it }

        val resultMap = runBlocking {
            val taskExecutorV2 = TaskExecutor(this)
            taskExecutorV2.submit(taskMap)
        }

        assertThat(resultMap.keys, iterableWithSize(10))
        val resultTask0 = resultMap["task0"]
        val resultTask1 = resultMap["task1"]
        val resultTask2 = resultMap["task2"]
        val resultTask3 = resultMap["task3"]
        val resultTask4 = resultMap["task4"]
        val resultTask5 = resultMap["task5"]
        val resultTask6 = resultMap["task6"]
        val resultTask7 = resultMap["task7"]
        val resultTask8 = resultMap["task8"]
        val resultTask9 = resultMap["task9"]

        assertTrue(resultTask0 is TaskReport.Successful)
        assertTrue(resultTask1 is TaskReport.Failed)
        assertTrue(resultTask2 is TaskReport.Successful)
        assertTrue(resultTask3 is TaskReport.Cancelled)
        assertTrue(resultTask4 is TaskReport.Successful)
        assertTrue(resultTask5 is TaskReport.Successful)
        assertTrue(resultTask6 is TaskReport.Cancelled)
        assertTrue(resultTask7 is TaskReport.Cancelled)
        assertTrue(resultTask8 is TaskReport.Cancelled)
        assertTrue(resultTask9 is TaskReport.Successful)

    }

    @Test
    fun `execute when taskExecutor cancelled remaining incomplete jobs are terminated`() {
        taskMap.clear()
        Task("task0", emptySet()) { }.also { taskMap[it.name] = it }
        Task("task1", emptySet()) { delay(100) }.also { taskMap[it.name] = it }
        Task("task2", emptySet()) { delay(100) }.also { taskMap[it.name] = it }


        val resultMap = runBlocking {
            val taskExecutorV2 = TaskExecutor(this)
            val resultMap = taskExecutorV2.submit(taskMap)
            delay(10)
            taskExecutorV2.cancel()
            resultMap
        }

        assertThat(resultMap.keys, iterableWithSize(3))
        val resultTask0 = resultMap["task0"]
        val resultTask1 = resultMap["task1"]
        val resultTask2 = resultMap["task2"]

        assertTrue(resultTask0 is TaskReport.Successful)
        assertTrue(resultTask1 is TaskReport.Failed)
        assertTrue(resultTask2 is TaskReport.Failed)
    }

    @Ignore
    @Test
    fun `execute when run with concurrent threaded dispatcher then task ordering is as expected`() {
        //TODO
    }
}