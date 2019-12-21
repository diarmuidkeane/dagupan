package dagupan

import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Service
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType

@ExtendWith(SpringExtension::class)
@SpringBootTest
class SpringTaskAssemblerTest {
    companion object {
        private interface Workload

        @Service
        private class WorkloadClass1 : Workload {
        }

        @Service
        private class WorkloadClass2 : Workload {
        }

        @Service
        private class WorkloadClass3 : Workload {
            @Value("anotherField")
            private val anotherField: String = "anotherField"

            @Autowired
            private lateinit var dependentWorkload1: WorkloadClass1
            @Autowired
            private lateinit var dependentWorkload2: WorkloadClass2

            fun someMethod() = WorkloadClass2()
        }

        @SpringBootConfiguration
        open class Application{
            fun main(args: Array<String>) {
                SpringApplication(Application::class.java).run(*args)
            }

        }
    }

    @Test
    fun `assemble when tasks are assembled from workloads their dependencies are set as expected`(){
        val whatever:List<KType> = TaskAssembler().resolveDependencies<WorkloadClass3,Workload>()
        assertThat(whatever,hasItems(WorkloadClass2::class.starProjectedType, WorkloadClass1::class.starProjectedType))
    }
}