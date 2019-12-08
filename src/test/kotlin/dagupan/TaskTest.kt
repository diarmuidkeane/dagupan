package dagupan

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

internal class TaskTest {

    @Test
    fun `prepare when invoked defers task to be executed later`() {
        val check = AtomicBoolean(false)
        val task1 = Task(emptySet()) {check.getAndSet(true)}

        runBlocking {
            task1.prepare(this)
            assertThat(check.get(), `is`(false))
            task1.deferred!!.await()
        }
        assertThat(check.get(), `is`(true))
    }

    @Test
    fun `prepare when invoked on task with dependecies defers all tasks to be executed later`() {
        val check1 = AtomicBoolean(false)
        val task1 = Task(emptySet()) {check1.getAndSet(true)}
        val check2 = AtomicBoolean(false)
        val task2 = Task(emptySet()) {check2.getAndSet(true)}
        val check3 = AtomicBoolean(false)
        val task3 = Task(setOf(task1,task2)){check3.getAndSet(true)}

        runBlocking {
            task1.prepare(this)
            assertThat(check1.get(), `is`(false))
            task2.prepare(this)
            assertThat(check2.get(), `is`(false))
            task3.prepare(this)
            assertThat(check3.get(), `is`(false))
            assertThat(check1.get(), `is`(false))
            assertThat(check2.get(), `is`(false))
            assertThat(check3.get(), `is`(false))
            task3.deferred!!.await()
        }
        assertThat(check1.get(), `is`(true))
        assertThat(check2.get(), `is`(true))
        assertThat(check3.get(), `is`(true))
    }

}