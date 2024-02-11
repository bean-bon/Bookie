package backend.parsing

import backend.parsing.LexingHelpers.Companion.chainSize
import kotlin.math.max

class LexingHelpers private constructor() {
    companion object {
        fun makeAlternativeChainRecord(key: String, altValues: List<String>): Rexp {
            fun aux(vals: List<String>): Rexp = when {
                vals.isEmpty() -> Rexp.ZERO
                else -> vals[0].re or aux(vals.drop(1))
            }
            return key rec aux(altValues)
        }
        fun Rexp.chainSize(): Int = when (this) {
            Rexp.ZERO -> 1
            Rexp.ONE -> 1
            is Rexp.CHAR -> 1
            is Rexp.ALT -> 1 + max(this.r1.chainSize(), this.r2.chainSize())
            is Rexp.CFUN -> 1
            is Rexp.PLUS -> 1 + this.r.chainSize()
            is Rexp.RANGE -> 1
            is Rexp.RECD -> 1 + this.r.chainSize()
            is Rexp.SEQ -> 1 + max(this.r1.chainSize(), this.r2.chainSize())
            is Rexp.STAR -> 1 + this.r.chainSize()
        }
    }
}