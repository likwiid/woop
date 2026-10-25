// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.data

/**
 * Joins a plan's two fields into the rehearsed "If …, then I will …." sentence.
 * The fields are typed (or spoken) as standalone lines, so they usually start with a capital;
 * mid-sentence that capital has to go. Kept free of Android types so it can be unit tested.
 */
object PlanText {
    fun sentence(ifCue: String, thenAction: String): String = "${ifPart(ifCue)} ${thenPart(thenAction)}"

    /** First half on its own line, e.g. the widget's cue row. */
    fun ifPart(ifCue: String): String = "If ${clause(ifCue)},"

    fun thenPart(thenAction: String): String = "then I will ${clause(thenAction)}."

    /**
     * A field as it reads inside the sentence: trimmed, closing punctuation dropped, and the first
     * letter lowered unless the first word is "I" (I'm, I'll…) or looks like an acronym or a name
     * with an inner capital (NASA, TV, McDonald's).
     */
    fun clause(field: String): String {
        val text = field.trim().trimEnd('.', ',').trimEnd()
        val first = text.firstOrNull() ?: return text
        if (!first.isUpperCase()) return text
        val word = text.takeWhile { it.isLetter() }
        if (word == "I" || word.drop(1).any { it.isUpperCase() }) return text
        return first.lowercaseChar() + text.substring(1)
    }
}
