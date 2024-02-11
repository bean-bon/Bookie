package backend.parsing

import java.io.InvalidObjectException

/**
 * Basic Regex implementation intended for recognising code constructs.
 * Inspired by my implementation of CFL coursework 2.
 * @author Benjamin Groom
 */

sealed interface Rexp {
    data object ZERO : Rexp
    data object ONE : Rexp
    data class CHAR(val c: Char) : Rexp
    data class ALT(val r1: Rexp, val r2: Rexp) : Rexp
    data class SEQ(val r1: Rexp, val r2: Rexp) : Rexp
    data class STAR(val r: Rexp) : Rexp
    data class RANGE(val s: Set<Char>) : Rexp {
        override fun toString(): String = "RANGE(...)"
    }
    data class CFUN(val f: (Char) -> Boolean) : Rexp
    data class PLUS(val r: Rexp) : Rexp
    data class RECD(val x: String, val r: Rexp) : Rexp
}

sealed interface Val
data object Empty: Val
data class Chr(val c: Char): Val
data class Sequ(val v1: Val, val v2: Val): Val
data class Left(val v: Val): Val
data class Right(val v: Val): Val
data class Stars(val vs: List<Val>): Val
data class Pluses(val vs: List<Val>): Val
data class Alphabet(val v: Val): Val
data class Record(val x: String, val v: Val): Val

// Exception for invalid Rexp usage.
private class MkepsMismatchException: InvalidObjectException("Invalid Rexp used for Mkeps.")
private class InjectionMismatchException: InvalidObjectException("Invalid Rexp used for Inject.")
private class ReificationMismatchException(v: Val, func: String): InvalidObjectException("Invalid Val $v passed to reification function $func.")
class SimplificationMismatchException: InvalidObjectException("Invalid Rexp passed to simplification function.")
class LexingException: InvalidObjectException("An error was encountered while lexing.")

// Convenience extension conversions.

/** Equivalent of sequence. */
infix fun Rexp.then(r: Rexp) = Rexp.SEQ(this, r)

/** Equivalent of alternative. */
infix fun Rexp.or(r: Rexp) = Rexp.ALT(this, r)


/** Equivalent of plus. */
fun Rexp.plus(): Rexp = Rexp.PLUS(this)

/** Equivalent of star. */
fun Rexp.star() = Rexp.STAR(this)

/**
 * Shorthand for record Rexp.
 */
infix fun String.rec(r: Rexp): Rexp = Rexp.RECD(this, r)

/**
 * Converts the string into a regular expression.
 */
val String.re: Rexp
    get() = string2Rexp(this)

private fun string2Rexp(s: String): Rexp = when {
    s == "" -> Rexp.ONE
    s.length == 1 -> Rexp.CHAR(s[0])
    else -> Rexp.SEQ(Rexp.CHAR(s[0]), string2Rexp(s.substring(1)))
}

// Nullable and derivative.
private fun nullable(r: Rexp): Boolean = when (r) {
    Rexp.ZERO -> false
    Rexp.ONE -> true
    is Rexp.CHAR -> false
    is Rexp.ALT -> nullable(r.r1) || nullable(r.r2)
    is Rexp.SEQ -> nullable(r.r1) && nullable(r.r2)
    is Rexp.STAR -> true
    is Rexp.PLUS -> nullable(r.r)
    is Rexp.RANGE -> false
    is Rexp.CFUN -> false
    is Rexp.RECD -> nullable(r.r)
}

fun der(c: Char, r: Rexp): Rexp = run {
//    println("der($c, $r)")
    when (r) {
    Rexp.ZERO -> Rexp.ZERO
    Rexp.ONE -> Rexp.ZERO
    is Rexp.CHAR -> if (c == r.c) Rexp.ONE else Rexp.ZERO
    is Rexp.ALT -> der(c, r.r1) or der(c, r.r2)
    is Rexp.SEQ ->
        if (nullable(r.r1)) Rexp.ALT(der(c, r.r1) then r.r2, der(c, r.r2))
        else der(c, r.r1) then r.r2
    is Rexp.STAR -> der(c, r.r) then r.r.star()
    is Rexp.PLUS -> der(c, r.r) then r.r.star()
    is Rexp.CFUN -> if (r.f(c)) { Rexp.ONE } else { Rexp.ZERO }
    is Rexp.RANGE -> if (r.s.contains(c)) { Rexp.ONE } else { Rexp.ZERO }
    is Rexp.RECD -> der(c, r.r)
}}

fun flatten(v: Val): String = when (v) {
    Empty -> ""
    is Chr -> v.c.toString()
    is Left -> flatten(v.v)
    is Right -> flatten(v.v)
    is Sequ -> flatten(v.v1) + flatten(v.v2)
    is Stars -> v.vs.joinToString(separator = "", transform = ::flatten)
    is Pluses -> v.vs.joinToString(separator = "", transform = ::flatten)
    is Alphabet -> flatten(v.v)
    is Record -> flatten(v.v)
}

fun env(v: Val): List<Pair<String, String>> = run {
//    println("env: $v")
    when (v) {
        Empty,
        is Chr,
        is Alphabet -> emptyList()
        is Left -> env(v.v)
        is Right -> env(v.v)
        is Sequ -> env(v.v1) + env(v.v2)
        is Stars -> v.vs.flatMap(::env)
        is Pluses -> v.vs.flatMap(::env)
        is Record -> listOf(Pair(v.x, flatten(v.v))) + env(v.v)
    }
}

@Throws(MkepsMismatchException::class)
fun mkeps(r: Rexp): Val = run {
//    println("mkeps with $r")
    when (r) {
    Rexp.ONE -> Empty
    is Rexp.ALT -> if (nullable(r.r1)) Left(mkeps(r.r1)) else Right(mkeps(r.r2))
    is Rexp.SEQ -> Sequ(mkeps(r.r1), mkeps(r.r2))
    is Rexp.STAR -> Stars(emptyList())
    is Rexp.PLUS -> Pluses(emptyList())
    is Rexp.CFUN -> Alphabet(Empty)
    is Rexp.RANGE -> Alphabet(Empty)
    is Rexp.RECD -> Record(r.x, mkeps(r.r))
    else -> throw MkepsMismatchException()
} }

@Throws(InjectionMismatchException::class)
private fun inj(inRe: Rexp, c: Char, match: Val): Val = run {
//    println("injection: $inRe, $c, $match")
    when (inRe) {
    is Rexp.SEQ -> when (match) {
        is Sequ -> Sequ(inj(inRe.r1, c, match.v1), match.v2)
        is Left ->
            if (match.v is Sequ) { Sequ(inj(inRe.r1, c, match.v.v1), match.v.v2) }
            else { throw InjectionMismatchException() }
        is Right -> Sequ(mkeps(inRe.r1), inj(inRe.r2, c, match.v))
        else -> throw InjectionMismatchException()
    }
    is Rexp.ALT -> when (match) {
        is Left -> Left(inj(inRe.r1, c, match.v))
        is Right -> Right(inj(inRe.r2, c, match.v))
        else -> throw InjectionMismatchException()
    }
    is Rexp.CHAR -> Chr(inRe.c)
    is Rexp.STAR ->
        if (match is Sequ && match.v2 is Stars) {
//            println("stars prod: ${Stars(listOf(inj(inRe.r, c, match.v1)) + match.v2.vs)}")
            Stars(listOf(inj(inRe.r, c, match.v1)) + match.v2.vs) }
        else { throw InjectionMismatchException() }
    is Rexp.PLUS ->
        if (match is Sequ && match.v2 is Stars) { Pluses(listOf(inj(inRe.r, c, match.v1)) + match.v2.vs) }
        else { throw InjectionMismatchException() }
    is Rexp.RANGE ->
        if (match is Empty) { Alphabet(Chr(c)) }
        else { throw InjectionMismatchException() }
    is Rexp.RECD -> Record(inRe.x, inj(inRe.r, c, match))
    else -> throw InjectionMismatchException()
} }

// Reification functions.
private fun F_ID(v: Val): Val = v
private fun F_LEFT(f: (Val) -> Val) = { v: Val -> Left(f(v)) }
private fun F_RIGHT(f: (Val) -> Val) = { v: Val -> Right(f(v)) }

@Throws(ReificationMismatchException::class)
private fun F_ALT(f1: (Val) -> Val, f2: (Val) -> Val) = { v: Val -> when (v) {
    is Left -> Left(f1(v))
    is Right -> Right(f2(v))
    else -> throw ReificationMismatchException(v, "F_ALT")
} }

@Throws(ReificationMismatchException::class)
private fun F_SEQ(f1: (Val) -> Val, f2: (Val) -> Val) = { v: Val -> when (v) {
    is Sequ -> Sequ(f1(v.v1), f2(v.v2))
    else -> throw ReificationMismatchException(v, "F_SEQ")
} }

private fun F_SEQ_Empty1(f1: (Val) -> Val, f2: (Val) -> Val) =
    { v: Val -> Sequ(f1(Empty), f2(v)) }
private fun F_SEQ_Empty2(f1: (Val) -> Val, f2: (Val) -> Val) =
    { v: Val -> Sequ(f1(v), f2(Empty)) }

private fun F_ERROR(v: Val): Val = throw SimplificationMismatchException()

// Rexp simplification.
@Throws(ReificationMismatchException::class, SimplificationMismatchException::class)
fun simp(r: Rexp): Pair<Rexp, (Val) -> Val> = when (r) {
    is Rexp.ALT -> {
        val (r1s, f1s) = simp(r.r1)
        val (r2s, f2s) = simp(r.r2)
        if (r1s == Rexp.ZERO) Pair(r2s, F_RIGHT(f2s))
        else if (r2s == Rexp.ZERO) Pair(r1s, F_LEFT(f1s))
        else if (r1s == r2s) Pair(r1s, F_LEFT(f1s))
        else Pair(r1s or r2s, F_ALT(f1s, f2s))
    }
    is Rexp.SEQ -> {
        val (r1s, f1s) = simp(r.r1)
        val (r2s, f2s) = simp(r.r2)
        if (r1s == Rexp.ZERO) Pair(r2s, ::F_ERROR)
        else if (r2s == Rexp.ZERO) Pair(r1s, ::F_ERROR)
        else if (r1s == Rexp.ONE) {print(""); Pair(r2s, F_SEQ_Empty1(f1s, f2s))}
        else if (r2s == Rexp.ONE) {print(""); Pair(r1s, F_SEQ_Empty2(f1s, f2s))}
        else {print(""); Pair(r1s then r2s, F_SEQ(f1s, f2s))}
    }
    else -> Pair(r, ::F_ID)
}

// Lexer.
private fun lexSimp(r: Rexp, s: String): Val = when (s) {
    "" ->
        if (nullable(r)) mkeps(r)
        else throw LexingException()
    else -> {
        println("simplifying on ${s[0]} and $r")
        val (rSimp, fSimp) = simp(der(s[0], r))
        println("got $rSimp and $fSimp")
        val i = inj(r, s[0], fSimp(lexSimp(rSimp, s.drop(1))))
//        println("inj yielded $i")
        i
    }
}

fun ders(s: String, r: Rexp): Rexp = when (s) {
    "" -> r
    else -> ders(s.drop(1), der(s[0], r))
}

fun matcher(s: String, r: Rexp): Boolean = nullable(ders(s, r))

internal fun lexingSimp(r: Rexp, s: String) = env(lexSimp(r, s))

/**
 * Interface to tokenise text.
 */
interface Tokeniser {
    fun tokenise(s: String): List<Token>
}

object TokenKeys {
    const val KEYWORD = "key"
    const val OP = "op"
    const val STRING = "str"
    const val PARENTHESIS = "par"
    const val WHITESPACE = "wts"
    const val ID = "id"
    const val TYPE = "ty"
    const val NUMBER = "num"
    const val OTHER = "other"
}