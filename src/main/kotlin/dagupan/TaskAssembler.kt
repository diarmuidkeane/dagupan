package dagupan

import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType

class TaskAssembler{

    inline fun <reified K , reified B> resolveDependencies(): List<KType> {
        val baseType = B::class.starProjectedType
        return K::class.declaredMembers
                .filter { it is KProperty }
                .map { it.returnType }
                .filter { baseType.isSupertypeOf(it) }
    }
}

