/**
 * Parser combinator class for code definitions, with the included
 * definition intended for use in syntax highlighting.
 * Inspired by CW3 of CFL.
 * @author Benjamin Groom
 */

package backend.parsing

private typealias Col = Collection<Any>
data class Seq<A, B>(val x: A, val y: B)

abstract class Parser<I: Col, T> {
    abstract fun parse(tokens: I): Set<Pair<T, I>>
    fun parseAll(tokens: I): Set<T> =
        parse(tokens).map { (t, _) -> t }.toSet()
}

class AltParser<I: Col, T>(
    private val p: Parser<I, T>,
    private val q: Parser<I, T>
): Parser<I, T>()
{
    override fun parse(tokens: I): Set<Pair<T, I>> =
        p.parse(tokens) + q.parse(tokens)
}

class SeqParser<I: Col, T, S>(
    private val p: Parser<I, T>,
    private val q: Parser<I, S>
): Parser<I, Seq<T, S>>()
{
    override fun parse(tokens: I): Set<Pair<Seq<T, S>, I>> =
        p.parse(tokens).map { (hd1, tl1) ->
            q.parse(tl1).map { (hd2, tl2) ->
                Pair(Seq(hd1, hd2), tl2)
            }
        }.flatten().toSet()
}

class MapParser<I: Col, T, S>(
    private val p: Parser<I, T>,
    private val f: (T) -> S): Parser<I, S>()
{
    override fun parse(tokens: I): Set<Pair<S, I>> =
        p.parse(tokens).map { (hd, tl) ->
            Pair(f(hd), tl)
        }.toSet()
}

// Parser combinator infix convenience functions.
infix fun <I: Col, T> Parser<I, T>.or(q: Parser<I, T>) = AltParser(this, q)
infix fun <I: Col, T, S> Parser<I, T>.then(q: Parser<I, S>) = SeqParser(this, q)
infix fun <I: Col, T, S> Parser<I, T>.map(f: (T) -> S) = MapParser(this, f)

/**
 * The expression interface is used for defining the type of tokens
 * found using the parser as a common supertype.
 */
interface Exp

data class Keyword(val word: String): Exp
data class Operator(val op: String): Exp
data class StringLiteral(val contents: String): Exp
data class Identifier(val id: String): Exp
data class Type(val id: String): Exp

interface TextParser {
    fun parse(text: String)
}
