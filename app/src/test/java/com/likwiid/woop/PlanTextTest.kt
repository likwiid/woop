// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop

import com.likwiid.woop.data.PlanText
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanTextTest {
    @Test
    fun lowersTheCapitalOfAnOrdinaryWord() {
        assertEquals("put it in the drawer", PlanText.clause("Put it in the drawer"))
    }

    @Test
    fun keepsTextThatIsAlreadyLowercase() {
        assertEquals("open my book instead", PlanText.clause("open my book instead"))
    }

    @Test
    fun keepsThePronounI() {
        assertEquals("I see my phone", PlanText.clause("I see my phone"))
    }

    @Test
    fun keepsContractionsOfI() {
        assertEquals("I'm tired after work", PlanText.clause("I'm tired after work"))
        assertEquals("I'll want to skip", PlanText.clause("I'll want to skip"))
        assertEquals("I’ve had a long day", PlanText.clause("I’ve had a long day"))
    }

    @Test
    fun lowersWordsThatOnlyStartWithI() {
        assertEquals("it rains", PlanText.clause("It rains"))
        assertEquals("ice cream calls", PlanText.clause("Ice cream calls"))
    }

    @Test
    fun keepsAcronyms() {
        assertEquals("NASA posts a launch", PlanText.clause("NASA posts a launch"))
        assertEquals("TV is on", PlanText.clause("TV is on"))
        assertEquals("NASA's feed is open", PlanText.clause("NASA's feed is open"))
    }

    @Test
    fun keepsNamesWithAnInnerCapital() {
        assertEquals("McDonald's is on the way", PlanText.clause("McDonald's is on the way"))
    }

    @Test
    fun lowersASingleLetterArticle() {
        assertEquals("a friend texts", PlanText.clause("A friend texts"))
    }

    @Test
    fun leavesTextNotStartingWithALetterAlone() {
        assertEquals("3 emails arrive", PlanText.clause("3 emails arrive"))
        assertEquals("\"Later\" sounds good", PlanText.clause("\"Later\" sounds good"))
    }

    @Test
    fun trimsSpacesAndClosingPunctuation() {
        assertEquals("go for a walk", PlanText.clause("  Go for a walk. "))
        assertEquals("I feel stuck", PlanText.clause("I feel stuck,"))
    }

    @Test
    fun blankStaysBlank() {
        assertEquals("", PlanText.clause("   "))
    }

    @Test
    fun composesTheFullSentence() {
        assertEquals(
            "If I see my phone on the desk, then I will put it in the drawer.",
            PlanText.sentence("I see my phone on the desk.", "Put it in the drawer."),
        )
        assertEquals("If it gets late, then I will read one page.", PlanText.sentence("It gets late", "Read one page"))
    }

    @Test
    fun splitsIntoTwoLinesForTheWidget() {
        assertEquals("If my phone buzzes,", PlanText.ifPart("My phone buzzes,"))
        assertEquals("then I will leave it face down.", PlanText.thenPart("Leave it face down"))
    }
}
