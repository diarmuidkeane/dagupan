package dagupan

import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

class TaskAssemblerTest {

    private interface Workload
    private class WorkloadClass1:Workload{
    }

    private class WorkloadClass2:Workload{
    }

    private class WorkloadClass3:Workload{
        private val anotherField:String="anotherField"
        private val dependentWorkload1:WorkloadClass1 = WorkloadClass1()
        private val dependentWorkload2:WorkloadClass2 = WorkloadClass2()

        fun someMethod()=WorkloadClass2()
    }

    @Test
    fun `assemble when tasks are assembled from workloads their dependencies are set as expected`(){
        val whatever:List<KType> = TaskAssembler().resolveDependencies<WorkloadClass3,Workload>()
        assertThat(whatever,hasItems(WorkloadClass2::class.starProjectedType, WorkloadClass1::class.starProjectedType))
    }
}