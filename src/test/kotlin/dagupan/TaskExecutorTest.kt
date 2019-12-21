package dagupan

import kotlinx.coroutines.asCoroutineDispatcher
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class TaskExecutorTest {

    @Test
    fun `execute when run with different dispatchers then task ordering is as expected`() {
        val testList = Collections.synchronizedList(ArrayList<String>())
        val task0 = Task(emptySet()) { testList.add("task0") }
        val task1 = Task(emptySet()) { testList.add("task1") }
        val task2 = Task(emptySet()) { testList.add("task2") }
        val task3 = Task(setOf(task1)) { testList.add("task3") }
        val task4 = Task(emptySet()) { testList.add("task4") }
        val task5 = Task(emptySet()) { testList.add("task5") }
        val task6 = Task(setOf(task3, task1)) { testList.add("task6") }
        val task7 = Task(setOf(task4, task1)) { testList.add("task7") }
        val task8 = Task(setOf(task2, task7)) { testList.add("task8") }
        val task9 = Task(emptySet()) { testList.add("task9") }
        val taskSet: Set<Task> = listOf(task0, task1, task2, task3, task4, task5, task6, task7, task8, task9).shuffled().toSet()

        TaskExecutor().execute(taskSet)
        assertThat(testList, hasSize(10))
        assertThat(testList, containsInAnyOrder("task0","task1","task2","task3","task4","task5","task6","task7","task8","task9"))
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task3")));
        assertThat(testList.indexOf("task6"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task3"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task4")));
        assertThat(testList.indexOf("task7"), greaterThan(testList.indexOf("task1")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task7")));
        assertThat(testList.indexOf("task8"), greaterThan(testList.indexOf("task2")));

        testList.clear()
        TaskExecutor().execute(taskSet,Executors.newFixedThreadPool(10).asCoroutineDispatcher())
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