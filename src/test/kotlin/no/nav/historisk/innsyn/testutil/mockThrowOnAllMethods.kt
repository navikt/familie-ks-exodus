package no.nav.historisk.innsyn.testutil

import org.mockito.Mockito.mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import java.lang.IllegalArgumentException

private class ThrowOnAllMethodsAnswer(private val className: String) : Answer<Any> {
    override fun answer(invocation: InvocationOnMock?): Any {
        throw IllegalArgumentException("mock: $className")
    }
}

fun<T> mockThrowOnAllMethods(clazz: Class<T>): T {
    return mock(clazz, ThrowOnAllMethodsAnswer(clazz.canonicalName))
}

inline fun<reified T> mockThrowOnAllMethods(): T {
    return mockThrowOnAllMethods(T::class.java)
}