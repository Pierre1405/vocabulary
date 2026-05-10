package com.example.myapplication.ui

import androidx.compose.runtime.compositionLocalOf

interface Strings {
    // Navigation
    val back: String

    // Home
    val appTitle: String
    val homeReading: String
    val homeReview: String
    val homeDictionary: String
    val homeConjugation: String

    // Story
    val storiesTitle: String
    val storiesEmpty: String

    // Sentence
    val sentenceEmpty: String
    val sentenceTabAll: String
    val sentenceTabDetail: String
    val sentenceDetailWip: String
    val play: String
    val loop: String
    val playAll: String
    val stop: String

    // Review selection
    val reviewTitle: String
    val reviewSentences: String
    val reviewWords: String
    val reviewConjugation: String
    val reviewTabReview: String
    val reviewTabLowGrade: String
    val reviewTabSchedule: String

    // Review card
    val reviewEmpty: String
    val reviewPrevious: String
    val reviewNext: String
    val reviewSpeakHint: String
    val reviewListen: String
    val reviewStopListen: String

    // Review preview (grade 1)
    val reviewPreviewTitle: String
    val reviewPreviewStart: String

    // Review completion
    val completionTitle: String
    val completionRestart: String
    val completionRestartLow: String
    val completionFinish: String

    // Upcoming reviews
    val reviewUpcomingTitle: String
    fun reviewUpcomingIn(hours: Long): String

    // Dictionary
    val dictionaryTitle: String
    val dictionarySearchHint: String
    val dictionaryClear: String
    val dictionaryTranslations: String
    val dictionaryForms: String
    val dictionaryListen: String
}

object StringsFr : Strings {
    override val back = "Retour"

    override val appTitle         = "Vocabulaire"
    override val homeReading      = "Lecture"
    override val homeReview       = "Révision"
    override val homeDictionary   = "Dictionnaire"
    override val homeConjugation  = "Conjugaisons"

    override val storiesTitle = "Histoires"
    override val storiesEmpty = "Aucune histoire trouvée."

    override val sentenceEmpty     = "Aucune phrase trouvée."
    override val sentenceTabAll    = "All"
    override val sentenceTabDetail = "Detail"
    override val sentenceDetailWip = "Detail — à venir"
    override val play    = "▶"
    override val loop    = "Loop"
    override val playAll = "Lire tout"
    override val stop    = "Stop"

    override val reviewTitle       = "Révision"
    override val reviewSentences   = "Phrases"
    override val reviewWords       = "Mots"
    override val reviewConjugation = "Conjugaisons"
    override val reviewTabReview   = "Réviser"
    override val reviewTabLowGrade = "Non maîtrisé"
    override val reviewTabSchedule = "Planifier"

    override val reviewEmpty       = "Rien à réviser pour aujourd'hui."
    override val reviewPrevious    = "← Précédent"
    override val reviewNext        = "Suivant →"
    override val reviewSpeakHint   = "Prononcez ou écrivez..."
    override val reviewListen      = "Écouter"
    override val reviewStopListen  = "Arrêter"

    override val reviewPreviewTitle = "À revoir — note 1"
    override val reviewPreviewStart = "Commencer"

    override val completionTitle      = "Série terminée !"
    override val completionRestart    = "Recommencer"
    override val completionRestartLow = "Recommencer avec les notes < 3"
    override val completionFinish     = "Fin"

    override val reviewUpcomingTitle = "À venir"
    override fun reviewUpcomingIn(hours: Long): String = when {
        hours < 1L  -> "< 1h"
        hours < 24L -> "dans ${hours}h"
        else        -> "dans ${hours / 24}j"
    }

    override val dictionaryTitle        = "Dictionnaire"
    override val dictionarySearchHint   = "Rechercher un mot…"
    override val dictionaryClear        = "Effacer"
    override val dictionaryTranslations = "Traductions"
    override val dictionaryForms        = "Formes"
    override val dictionaryListen       = "Écouter"
}

object StringsEn : Strings {
    override val back = "Back"

    override val appTitle         = "Vocabulary"
    override val homeReading      = "Reading"
    override val homeReview       = "Review"
    override val homeDictionary   = "Dictionary"
    override val homeConjugation  = "Conjugations"

    override val storiesTitle = "Stories"
    override val storiesEmpty = "No stories found."

    override val sentenceEmpty     = "No sentences found."
    override val sentenceTabAll    = "All"
    override val sentenceTabDetail = "Detail"
    override val sentenceDetailWip = "Detail — coming soon"
    override val play    = "▶"
    override val loop    = "Loop"
    override val playAll = "Play all"
    override val stop    = "Stop"

    override val reviewTitle       = "Review"
    override val reviewSentences   = "Sentences"
    override val reviewWords       = "Words"
    override val reviewConjugation = "Conjugations"
    override val reviewTabReview   = "Review"
    override val reviewTabLowGrade = "Not mastered"
    override val reviewTabSchedule = "Schedule"

    override val reviewEmpty       = "Nothing to review today."
    override val reviewPrevious    = "← Previous"
    override val reviewNext        = "Next →"
    override val reviewSpeakHint   = "Speak or type..."
    override val reviewListen      = "Listen"
    override val reviewStopListen  = "Stop"

    override val reviewPreviewTitle = "To review — grade 1"
    override val reviewPreviewStart = "Start"

    override val completionTitle      = "Session complete!"
    override val completionRestart    = "Restart"
    override val completionRestartLow = "Restart with grades < 3"
    override val completionFinish     = "Done"

    override val reviewUpcomingTitle = "Upcoming"
    override fun reviewUpcomingIn(hours: Long): String = when {
        hours < 1L  -> "< 1h"
        hours < 24L -> "in ${hours}h"
        else        -> "in ${hours / 24}d"
    }

    override val dictionaryTitle        = "Dictionary"
    override val dictionarySearchHint   = "Search a word…"
    override val dictionaryClear        = "Clear"
    override val dictionaryTranslations = "Translations"
    override val dictionaryForms        = "Forms"
    override val dictionaryListen       = "Listen"
}

fun stringsForLocale(locale: String): Strings = when (locale) {
    "en" -> StringsEn
    else -> StringsFr
}

val LocalStrings = compositionLocalOf<Strings> { StringsFr }
