package dagupan

import kotlinx.coroutines.asCoroutineDispatcher
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class TaskExecutorTest {

    private val taskMap: MutableMap<String,Task> = mutableMapOf()
    private val testList = Collections.synchronizedList(ArrayList<String>())

    @BeforeEach
    fun setup(){
        val task0 = Task(emptySet()) { testList.add("task0") } .also { taskMap["task0"] = it  }
        val task1 = Task(emptySet()) { testList.add("task1") } .also { taskMap["task1"] = it  }
        val task2 = Task(emptySet()) { testList.add("task2") } .also { taskMap["task2"] = it  }
        val task3 = Task(setOf(task1)) { testList.add("task3") } .also { taskMap["task3"] = it   }
        val task4 = Task(emptySet()) { testList.add("task4") } .also { taskMap["task4"] = it   }
        val task5 = Task(emptySet()) { testList.add("task5") } .also { taskMap["task5"] = it   }
        val task6 = Task(setOf(task3, task1)) { testList.add("task6") } .also { taskMap["task6"] = it  }
        val task7 = Task(setOf(task4, task1)) { testList.add("task7") } .also { taskMap["task7"] = it  }
        val task8 = Task(setOf(task2, task7)) { testList.add("task8") } .also { taskMap["task8"] = it  }
        val task9 = Task(emptySet()) { testList.add("task9") } .also { taskMap["task9"] = it   }

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
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task3")));
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task3"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")));
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")));
    }

    @Test
    fun `execute when run with concurrent threaded dispatcher then task ordering is as expected`() {
        TaskExecutor().execute(taskMap.values.toSet(),Executors.newFixedThreadPool(10).asCoroutineDispatcher())
        assertThat(testList, hasSize(10))
        assertThat(testList, containsInAnyOrder("task0","task1","task2","task3","task4","task5","task6","task7","task8","task9"))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task3")));
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task3"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")));
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")));
    }
}