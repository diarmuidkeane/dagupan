package dagupan

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.test.Ignore

class TaskExecutorTest {

    private val taskMap: MutableMap<String,Task> = mutableMapOf()
    private val testList = Collections.synchronizedList(ArrayList<String>())

    @BeforeEach
    fun setup(){
        Task("task0",emptySet()) { testList.add("task0") ; println( Thread.currentThread().name)} .also { taskMap["task0"] = it  }
        val task1 = Task("task1",emptySet()) { testList.add("task1") ; println( Thread.currentThread().name)} .also { taskMap["task1"] = it  }
        val task2 = Task("task2",emptySet()) { testList.add("task2") ; println( Thread.currentThread().name)} .also { taskMap["task2"] = it  }
        val task3 = Task("task3",setOf(task1)) { testList.add("task3") ; println( Thread.currentThread().name)} .also { taskMap["task3"] = it   }
        val task4 = Task("task4", emptySet()) { testList.add("task4") ; println( Thread.currentThread().name)} .also { taskMap["task4"] = it   }
        Task("task5",emptySet()) { testList.add("task5") ; println( Thread.currentThread().name)} .also { taskMap["task5"] = it   }
        Task("task6", setOf(task3, task1)) { testList.add("task6") ; println( Thread.currentThread().name)} .also { taskMap["task6"] = it  }
        val task7 = Task("task7", setOf(task4, task1)) { testList.add("task7") ; println( Thread.currentThread().name)} .also { taskMap["task7"] = it  }
        Task("task8", setOf(task2, task7)) { testList.add("task8") ; println( Thread.currentThread().name)} .also { taskMap["task8"] = it  }
        Task("task9", emptySet()) { testList.add("task9") ; println( Thread.currentThread().name)} .also { taskMap["task9"] = it   }
    }

    @AfterEach
    fun tearDown(){
        testList.clear()
        taskMap.clear()
    }

    @Test
    fun `execute when run with default dispatcher then task ordering is as expected`() {
        TaskExecutor().execute(taskMap.values.toSet())
        assertThat(testList, hasSize(10))
        assertThat(testList, containsInAnyOrder("task0", "task1", "task2", "task3", "task4", "task5", "task6", "task7", "task8", "task9"))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task3")))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task3"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")))
    }

    @Test
    fun `execute when independent workload fails then the other workloads are not disturbed`() {
        taskMap["task0"] = Task("task0",emptySet()) { throw RuntimeException() }
        TaskExecutor().execute(taskMap.values.toSet())
        assertThat(testList, hasSize(9))
        assertThat(testList, containsInAnyOrder("task1", "task2", "task3", "task4", "task5", "task6", "task7", "task8", "task9"))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task3")))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task3"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")))
    }

    @Test
    fun `execute when taskExecutor cancelled only unfinished or hanging jobs are incomplete`() {
        runCatching {
            runBlocking {
                supervisorScope {
                    val taskExecutor = TaskExecutor(this)
                    taskMap["task0"] = Task("task0", emptySet()) {
                        launch {
                            while (true) {
                                delay(1000)
                            }
                        }
                    }
                    taskExecutor.execute(taskMap.values.toSet())
                    while(testList.size <9)
                    {
                        delay(10)
                    }
                    this.coroutineContext.cancel()
                }
            }
        }
        assertThat(testList, hasSize(9))
        assertThat(testList, containsInAnyOrder("task1", "task2", "task3", "task4", "task5", "task6", "task7", "task8", "task9"))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task3")))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task3"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")))
    }

    @Ignore
    @Test
    fun `execute when a workload dependency fails then the dependent workload also fails but the other workloads are not disturbed`() {
        val newTask1 = Task("task1",emptySet()) { println(Thread.currentThread().name);throw RuntimeException() }.also { taskMap["task1"] = it }
        val newTask3 = Task("task3", setOf(newTask1)) { testList.add("task3"); println(Thread.currentThread().name) }.also { taskMap["task3"] = it }
        Task("task6",setOf(newTask3, newTask1)) { testList.add("task6"); println(Thread.currentThread().name) }.also { taskMap["task6"] = it }
        TaskExecutor().execute(taskMap.values.toSet())
        assertThat(testList, hasSize(7))
        assertThat(testList, containsInAnyOrder("task0", "task2", "task4", "task5", "task7", "task8", "task9"))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")));
    }

    @Ignore
    @Test
    fun `execute when a workload dependency fails then the dependent workload also fails but it's own dependencies and the other workloads are not disturbed`() {
        val newTask3 =  Task("task3", setOf(taskMap["task1"]!! )) { println( Thread.currentThread().name) ;throw RuntimeException()}.also {  taskMap["task3"] = it}
        Task("task6", setOf(newTask3, taskMap["task1"]!! )) { testList.add("task6"); println( Thread.currentThread().name) } .also { taskMap["task6"] = it  }
        TaskExecutor().execute(taskMap.values.toSet())
        assertThat(testList, hasSize(8))
        assertThat(testList, containsInAnyOrder("task0","task1","task2", "task4", "task5", "task7", "task8", "task9"))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")));
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")));
    }

    @Test
    fun `execute when run with concurrent threaded dispatcher then task ordering is as expected`() {
        TaskExecutor().execute(taskMap.values.toSet(), Executors.newFixedThreadPool(10).asCoroutineDispatcher())
        assertThat(testList, hasSize(10))
        assertThat(testList, containsInAnyOrder("task0", "task1", "task2", "task3", "task4", "task5", "task6", "task7", "task8", "task9"))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task3")))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task3"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")))
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")))
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")))
    }
}