package me.fru1t.stak.server.models

/**
 * Provides context to a function's return by providing two fields: the yielded [value] and the
 * [status]. Results are most notably used in `when` expression statements (ie. `return when(...)`
 * or `val foo = when(...)`) against the [status] which enables static checking to validate that
 * all possibilities of the [status] are accounted for.
 *
 * Note that one should not document parameters for this data class. [Result] is somewhat ubiquitous
 * within this application and will **not** change without extensive review for alternatives.
 * @param V the value type which can be nullable or even [Nothing].
 * @param S the status type which must be an [Enum].
 */
data class Result<out V, S : Enum<S>>(
    /** The yielded value of the method. */
    val value: V,

    /** Context for why the method returned. */
    val status: S)

/**
 * Provides context to a function's return by providing a single generic [status] enum field.
 * Statuses are most notable used in `when` expression statements which enables static checking that
 * validates all possibilities of [S] are checked for which is useful when making changes to [S].
 * @param S the status type which must eb an [Enum].
 */
data class Status<S : Enum<S>>(
    /** Context for why the method returned. */
    val status : S)
