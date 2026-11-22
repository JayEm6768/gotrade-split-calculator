package com.splitcalc.app

import com.splitcalc.app.data.SplitRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositoryCodecTest {

    @Test
    fun `combos round trip`() {
        val combos = listOf(
            SavedCombo("Core", listOf(SavedRow("VOO", "45"), SavedRow("SMH", "55"))),
            SavedCombo("Solo", listOf(SavedRow("QQQ", "100"))),
        )
        assertEquals(combos, SplitRepository.decodeCombos(SplitRepository.encodeCombos(combos)))
    }

    @Test
    fun `malformed combo strings are skipped without throwing`() {
        listOf("", "|||", "x=", "=VOO:50", "garbage", "a=b=c|=|;;;").forEach {
            val decoded = SplitRepository.decodeCombos(it)
            assertTrue(decoded.all { combo -> combo.name.isNotBlank() && combo.rows.isNotEmpty() }, it)
        }
        assertTrue(SplitRepository.decodeCombos("|||").isEmpty())
        assertTrue(SplitRepository.decodeCombos("x=").isEmpty())
        assertTrue(SplitRepository.decodeCombos("=VOO:50").isEmpty())
        assertTrue(SplitRepository.decodeCombos("garbage").isEmpty())
    }
}
