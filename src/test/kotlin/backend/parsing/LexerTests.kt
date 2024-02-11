package backend.parsing

import kotlin.test.*
import backend.parsing.der
import org.junit.jupiter.api.assertAll

internal class LexerTests {

    // Extension methods.
    @Test
    fun `Then extension corresponds with Sequence`() {
        val r1 = Rexp.CHAR('a')
        val r2 = Rexp.CHAR('b')
        assertEquals(Rexp.SEQ(r1, r2), r1 then r2)
    }

    @Test
    fun `Or extension corresponds with Alternative`() {
        val r1 = Rexp.CHAR('a')
        val r2 = Rexp.CHAR('b')
        assertEquals(Rexp.ALT(r1, r2), r1 or r2)
    }

    @Test
    fun `Rec extension corresponds with Record`() {
        val name = "record"
        val r = Rexp.CHAR('a')
        assertEquals(Rexp.RECD(name, r), name rec r)
    }

    // Derivatives.
    @Test
    fun `Basic regex derivatives match`() {
        assertEquals(Rexp.ZERO, der('a', Rexp.ZERO))
        assertEquals(Rexp.ZERO, der('a', Rexp.ONE))
        assertEquals(Rexp.ZERO, der('a', Rexp.CHAR('b')))
        assertEquals(Rexp.ONE, der('a', Rexp.CHAR('a')))
    }

    @Test
    fun `Alternative derivative with simple regex`() {
        val r1 = Rexp.CHAR('a')
        val r2 = Rexp.CHAR('b')
        assertEquals(Rexp.ONE or Rexp.ZERO, der('a', r1 or r2))
    }

    @Test
    fun `Sequence derivative with non-nullable r1`() {
        val r1 = Rexp.CHAR('a')
        val r2 = Rexp.CHAR('b')
        assertEquals(Rexp.ONE then Rexp.CHAR('b'), der('a', r1 then r2))
    }

    @Test
    fun `Sequence derivative with nullable r1`() {
        val r1 = Rexp.ONE
        val r2 = Rexp.CHAR('b')
        assertEquals((Rexp.ZERO then Rexp.CHAR('b')) or Rexp.ZERO, der('a', r1 then r2))
    }

    @Test
    fun `Star derivative is as expected`() {
        assertEquals(Rexp.ONE then Rexp.CHAR('a').star(), der('a', Rexp.CHAR('a').star()))
    }

    @Test
    fun `CFUN recognises valid character`() {
        assertEquals(Rexp.ONE, der('a', Rexp.CFUN { c -> c == 'a' }))
    }

    @Test
    fun `CFUN does not match on an invalid character`() {
        assertEquals(Rexp.ZERO, der('a', Rexp.CFUN { c -> c == 'b' }))
    }

    @Test
    fun `Range matches on an included character`() {
        assertEquals(Rexp.ONE, der('a', Rexp.RANGE(setOf('b', 'a'))))
    }

    @Test
    fun `Range does not match on an excluded character`() {
        assertEquals(Rexp.ZERO, der('a', Rexp.RANGE(setOf('b', 'c'))))
    }

    @Test
    fun `Record yields derivative of the contained r`() {
        val innerR: Rexp = Rexp.STAR(Rexp.CHAR('a') then Rexp.CHAR('b'))
        assertEquals(der('a', innerR), der('a', "record" rec innerR))
    }

    // Flatten tests.
    @Test
    fun `Flatten left, right, alphabet and record all produce a flattened inner val`() {
        val innerVal = Chr('c')
        val left = Left(innerVal)
        val right = Right(innerVal)
        val alphabet = Alphabet(innerVal)
        val record = Record("name", innerVal)
        assertEquals("c", flatten(left))
        assertEquals("c", flatten(right))
        assertEquals("c", flatten(alphabet))
        assertEquals("c", flatten(record))
    }

    @Test
    fun `Flatten sequence produces a concatenation of v1 and v2`() {
        val v1 = Chr('a')
        val v2 = Chr('b')
        assertEquals("ab", flatten(Sequ(v1, v2)))
    }

    @Test
    fun `Flatten stars produces a concatenation of all values in the star`() {
        val vs = listOf(Chr('a'), Chr('b'), Chr('c'))
        assertEquals("abc", flatten(Stars(vs)))
    }

    // Env tests.

    @Test
    fun `Env yields an empty list for Empty, Chr and Alphabet`() {
        assertEquals(listOf(), env(Empty))
        assertEquals(listOf(), env(Chr('a')))
        assertEquals(listOf(), env(Alphabet(Chr('a'))))
    }

    @Test
    fun `Env yields the flattened v concatenated with the env of v`() {
        assertEquals(
            listOf(Pair("record", "bc")),
            env(Record("record", Sequ(Chr('b'), Chr('c'))))
        )
    }

}