package de.griefed.serverpackcreator.api.utilities.common

@Suppress("UNCHECKED_CAST", "unused")
fun interface Affirm<T> {
    fun test(test: T): Boolean
    fun and(other: Affirm<in T>): Affirm<T> = Affirm { test: T -> test(test) && other.test(test) }
    fun negate(): Affirm<T> = Affirm { test: T -> !test(test) }
    fun or(other: Affirm<in T>): Affirm<T> = Affirm { test: T -> test(test) || other.test(test) }

    companion object {
        fun <T> isEqual(targetRef: Any): Affirm<T> = Affirm { `object`: T -> targetRef == `object` }
        fun <T> not(target: Affirm<in T>): Affirm<T> = target.negate() as Affirm<T>
    }
}