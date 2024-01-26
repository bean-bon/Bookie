package backend.parsing

import java.io.InvalidObjectException

/**
 * Basic Regex implementation intended for recognising code constructs.
 * Inspired by my implementation of CFL coursework 2.
 * @author Benjamin Groom
 */

sealed interface Rexp
data object ZERO: Rexp
data object ONE: Rexp
data class CHAR(val c: Char): Rexp
data class ALT(val r1: Rexp, val r2: Rexp): Rexp
data class SEQ(val r1: Rexp, val r2: Rexp): Rexp
data class STAR(val r: Rexp): Rexp
data class RANGE(val s: Set<Char>): Rexp
data class CFUN(val f: (Char) -> Boolean): Rexp
data class PLUS(val r: Rexp): Rexp
data class RECD(val x: String, val r: Rexp): Rexp

private sealed interface Val
private data object Empty: Val
private data class Chr(val c: Char): Val
private data class Sequ(val v1: Val, val v2: Val): Val
private data class Left(val v: Val): Val
private data class Right(val v: Val): Val
private data class Stars(val vs: List<Val>): Val
private data class Pluses(val vs: List<Val>): Val
private data class Alphabet(val v: Val): Val
private data class Record(val x: String, val v: Val): Val

// Exception for invalid Rexp usage.
class MkepsMismatchException: InvalidObjectException("Invalid Rexp used for Mkeps.")
class InjectionMismatchException: InvalidObjectException("Invalid Rexp used for Inject.")
class ReificationMismatchException: InvalidObjectException("Invalid Val passed to reification function.")
class SimplificationMismatchException: InvalidObjectException("Invalid Rexp passed to simplification function.")
class LexingException: InvalidObjectException("An error was encountered while lexing.")

// Convenience extension conversions.

/** Equivalent of sequence. */
infix fun Rexp.then(r: Rexp) = SEQ(this, r)

/** Equivalent of alternative. */
infix fun Rexp.or(r: Rexp) = ALT(this, r)

/** Equivalent of plus. */
infix fun Rexp.plus(r: Rexp): Rexp = PLUS(r)

/** Equivalent of star. */
fun Rexp.star() = STAR(this)

/**
 * Shorthand for record Rexp.
 */
infix fun String.rec(r: Rexp): Rexp = RECD(this, r)

/**
 * Converts the string into a regular expression.
 */
val String.re: Rexp
    get() = string2Rexp(this)

private fun string2Rexp(s: String): Rexp = when {
    s == "" -> ONE
    s.length == 1 -> CHAR(s[0])
    else -> SEQ(CHAR(s[0]), string2Rexp(s.substring(1)))
}

// Nullable and derivative.
private fun nullable(r: Rexp): Boolean = when (r) {
    ZERO -> false
    ONE -> true
    is CHAR -> false
    is ALT -> nullable(r.r1) || nullable(r.r2)
    is SEQ -> nullable(r.r1) && nullable(r.r2)
    is STAR -> true
    is PLUS -> nullable(r)
    is RANGE -> false
    is CFUN -> false
    is RECD -> nullable(r.r)
}

private fun der(c: Char, r: Rexp): Rexp = when (r) {
    ZERO,
    ONE -> ZERO
    is CHAR -> if (c == r.c) { ONE } else { ZERO }
    is ALT -> der(c, r.r1) or der(c, r.r2)
    is SEQ ->
        if (nullable(r.r1)) { (der(c, r.r1) then r.r2) or der(c, r.r2) }
        else { der(c, r.r1) then r.r2 }
    is STAR,
    is PLUS -> der(c, r) then r.star()
    is CFUN -> if (r.f(c)) { ONE } else { ZERO }
    is RANGE -> if (r.s.contains(c)) { ONE } else { ZERO }
    is RECD -> der(c, r)
}

private fun flatten(v: Val): String = when (v) {
    Empty -> ""
    is Chr -> v.c.toString()
    is Sequ -> flatten(v.v1) + flatten(v.v2)
    is Stars -> v.vs.joinToString { flatten(it) }
    is Pluses -> v.vs.joinToString { flatten(it) }
    is Left,
    is Right,
    is Alphabet,
    is Record -> flatten(v)
}

private fun env(v: Val): List<Pair<String, String>> = when (v) {
    Empty,
    is Chr,
    is Alphabet -> emptyList()
    is Left,
    is Right -> env(v)
    is Sequ -> env(v.v1) + env(v.v2)
    is Stars -> v.vs.flatMap { env(it) }
    is Pluses -> v.vs.flatMap { env(it) }
    is Record -> listOf(Pair(v.x, flatten(v))) + env(v)
}

@Throws(MkepsMismatchException::class)
private fun mkeps(r: Rexp): Val = when (r) {
    ONE -> Empty
    is ALT -> if (nullable(r.r1)) { Left(mkeps(r.r1)) } else { Right(mkeps(r.r2)) }
    is SEQ -> Sequ(mkeps(r.r1), mkeps(r.r2))
    is STAR -> Stars(emptyList())
    is PLUS -> Pluses(emptyList())
    is RECD -> Record(r.x, mkeps(r.r))
    is CFUN -> Alphabet(Empty)
    is RANGE -> Alphabet(Empty)
    else -> throw MkepsMismatchException()
}

@Throws(InjectionMismatchException::class)
private fun inj(r: Rexp, c: Char, match: Val): Val = when (r) {
    is SEQ -> when (match) {
        is Sequ -> TODO()
        is Left ->
            if (match.v is Sequ) { Sequ(inj(r.r1, c, match.v.v1), match.v.v2) }
            else { throw InjectionMismatchException() }
        is Right -> Sequ(mkeps(r.r1), inj(r.r2, c, match))
        else -> throw InjectionMismatchException()
    }
    is ALT -> when (match) {
        is Left -> Left(inj(r.r1, c, match.v))
        is Right -> Right(inj(r.r1, c, match.v))
        else -> throw InjectionMismatchException()
    }
    is CHAR -> Chr(r.c)
    is STAR ->
        if (match is Sequ && match.v2 is Stars) { Stars(listOf(inj(r.r, c, match.v1)) + match.v2.vs) }
        else { throw InjectionMismatchException() }
    is PLUS ->
        if (match is Sequ && match.v2 is Stars) { Pluses(listOf(inj(r.r, c, match.v1)) + match.v2.vs) }
        else { throw InjectionMismatchException() }
    is RANGE ->
        if (match is Empty) { Alphabet(Chr(c)) }
        else { throw InjectionMismatchException() }
    is RECD -> Record(r.x, inj(r.r, c, match))
    else -> throw InjectionMismatchException()
}

// Reification functions.
private fun F_ID(v: Val): Val = v
private fun F_LEFT(f: (Val) -> Val) = { v: Val -> Left(f(v)) }
private fun F_RIGHT(f: (Val) -> Val) = { v: Val -> Right(f(v)) }

@Throws(ReificationMismatchException::class)
private fun F_ALT(f1: (Val) -> Val, f2: (Val) -> Val) = { v: Val -> when (v) {
    is Left -> Left(f1(v))
    is Right -> Right(f2(v))
    else -> throw ReificationMismatchException()
} }

@Throws(ReificationMismatchException::class)
private fun F_SEQ(f1: (Val) -> Val, f2: (Val) -> Val) = { v: Val -> when (v) {
    is Sequ -> Sequ(f1(v.v1), f2(v.v2))
    else -> throw ReificationMismatchException()
} }

private fun F_SEQ_Empty1(f1: (Val) -> Val, f2: (Val) -> Val) =
    { v: Val -> Sequ(f1(Empty), f2(v)) }
private fun F_SEQ_Empty2(f1: (Val) -> Val, f2: (Val) -> Val) =
    { v: Val -> Sequ(f1(v), f2(Empty)) }

// Rexp simplification.
@Throws(ReificationMismatchException::class, SimplificationMismatchException::class)
private fun simp(r: Rexp): Pair<Rexp, (Val) -> Val> = when (r) {
    is ALT -> {
        val (r1s, f1s) = simp(r.r1)
        val (r2s, f2s) = simp(r.r2)
        if (r1s == ZERO) { Pair(r2s, F_RIGHT(f2s)) }
        else if (r2s == ZERO) { Pair(r1s, F_LEFT(f1s)) }
        else if (r1s == r2s) { Pair(r1s, F_LEFT(f1s)) }
        else { Pair(r1s or r2s, F_ALT(f1s, f2s)) }
    }
    is SEQ -> {
        val (r1s, f1s) = simp(r.r1)
        val (r2s, f2s) = simp(r.r2)
        if (r1s == ZERO || r2s == ZERO) { throw SimplificationMismatchException() }
        else if (r1s == ONE) { Pair(r2s, F_SEQ_Empty1(f1s, f2s)) }
        else if (r2s == ONE) { Pair(r1s, F_SEQ_Empty2(f1s, f2s)) }
        else { Pair(r1s then r2s, F_SEQ(f1s, f2s)) }
    }
    else -> Pair(r, ::F_ID)
}

// Lexer.
private fun lexSimp(r: Rexp, s: String): Val = when (s) {
    "" ->
        if (nullable(r)) { mkeps(r) }
        else { throw LexingException() }
    else -> {
        val (rSimp, fSimp) = simp(der(s[0], r))
        inj(r, s[0], fSimp(lexSimp(rSimp, s.drop(1))))
    }
}

internal fun lexingSimp(r: Rexp, s: String) = env(lexSimp(r, s))

// Tokens.
interface Token

/**
 * Interface to tokenise text.
 */
interface Tokeniser {
    fun tokenise(s: String): List<Token>
}