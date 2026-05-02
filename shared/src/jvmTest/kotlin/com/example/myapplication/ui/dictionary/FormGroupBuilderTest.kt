package com.example.myapplication.ui.dictionary

import com.example.myapplication.data.forms.FormGroup
import com.example.myapplication.data.forms.getFormsConfig
import java.io.File
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormGroupBuilderTest {

    private val dbPath = File("../app/src/main/assets/dictionary.db").absolutePath

    private fun queryForms(lemma: String, locale: String): List<Triple<String, String?, String?>> {
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val stmt = conn.prepareStatement("""
            SELECT f.form, f.group_key, f.pronouns
            FROM dict_form f
            JOIN dict_entry e ON e.id = f.entry_id
            WHERE e.lemma = ? AND e.locale = ?
        """)
        stmt.setString(1, lemma)
        stmt.setString(2, locale)
        val rs = stmt.executeQuery()
        val result = mutableListOf<Triple<String, String?, String?>>()
        while (rs.next()) result.add(Triple(rs.getString(1), rs.getString(2), rs.getString(3)))
        conn.close()
        return result
    }

    // ── Präsens ──────────────────────────────────────────────────────────────

    @Test
    fun machen_present_has_all_six_pronouns() {
        val groups = buildFormGroups(queryForms("machen", "de"), getFormsConfig("de"), "machen")
        val present = groups.find { it.label == "Präsens" }
        assertTrue(present != null, "Le groupe 'Präsens' doit exister")
        val pronouns = present.rows.mapNotNull { it.label }.toSet()
        assertEquals(setOf("ich", "du", "er,sie,es", "wir", "ihr", "sie"), pronouns,
            "Les 6 pronoms du présent doivent être présents sans doublon")
    }

    @Test
    fun machen_present_forms_are_correct() {
        val groups = buildFormGroups(queryForms("machen", "de"), getFormsConfig("de"), "machen")
        val byPronoun = groups.find { it.label == "Präsens" }!!.rows.associate { it.label to it.form }
        assertEquals("mache",  byPronoun["ich"],        "ich mache")
        assertEquals("machst", byPronoun["du"],         "du machst")
        assertEquals("macht",  byPronoun["er,sie,es"],  "er,sie,es macht")
        assertEquals("machen", byPronoun["wir"],        "wir machen")
        assertEquals("macht",  byPronoun["ihr"],        "ihr macht")
        assertEquals("machen", byPronoun["sie"],        "sie machen")
    }

    // ── Präteritum ───────────────────────────────────────────────────────────

    @Test
    fun machen_has_preterite_group() {
        val groups = buildFormGroups(queryForms("machen", "de"), getFormsConfig("de"), "machen")
        val preterite = groups.find { it.label == "Präteritum" }
        assertTrue(preterite != null, "Le groupe 'Präteritum' doit exister")
        assertTrue(preterite.rows.isNotEmpty(), "Le Präteritum ne doit pas être vide")
    }

    // ── Partizip I (dérivé) ───────────────────────────────────────────────────

    @Test
    fun machen_partizip_i_is_derived() {
        val groups = buildFormGroups(queryForms("machen", "de"), getFormsConfig("de"), "machen")
        val partizipI = groups.find { it.label == "Partizip I" }
        assertTrue(partizipI != null, "Le groupe 'Partizip I' doit exister (dérivé)")
        assertEquals(1, partizipI.rows.size, "Partizip I doit avoir une seule forme")
        assertEquals("machend", partizipI.rows[0].form, "Partizip I de 'machen' = 'machend'")
    }

    @Test
    fun sein_partizip_i_is_derived() {
        val groups = buildFormGroups(queryForms("sein", "de"), getFormsConfig("de"), "sein")
        val partizipI = groups.find { it.label == "Partizip I" }
        assertTrue(partizipI != null, "Le groupe 'Partizip I' doit exister pour 'sein'")
        assertEquals("seiend", partizipI.rows[0].form, "Partizip I de 'sein' = 'seiend'")
    }

    // ── Futur I (dérivé) ─────────────────────────────────────────────────────

    @Test
    fun machen_futur_i_is_derived() {
        val groups = buildFormGroups(queryForms("machen", "de"), getFormsConfig("de"), "machen")
        val futur = groups.find { it.label == "Futur I" }
        assertTrue(futur != null, "Le groupe 'Futur I' doit exister (dérivé)")
        val byPronoun = futur.rows.associate { it.label to it.form }
        assertEquals("werde machen",  byPronoun["ich"],        "ich werde machen")
        assertEquals("wirst machen",  byPronoun["du"],         "du wirst machen")
        assertEquals("wird machen",   byPronoun["er,sie,es"],  "er/sie/es wird machen")
        assertEquals("werden machen", byPronoun["wir"],        "wir werden machen")
        assertEquals("werdet machen", byPronoun["ihr"],        "ihr werdet machen")
        assertEquals("werden machen", byPronoun["sie"],        "sie werden machen")
    }

    // ── Perfekt (dérivé) ─────────────────────────────────────────────────────

    @Test
    fun machen_perfekt_is_derived() {
        val groups = buildFormGroups(queryForms("machen", "de"), getFormsConfig("de"), "machen")
        val perfekt = groups.find { it.label == "Perfekt" }
        assertTrue(perfekt != null, "Le groupe 'Perfekt' doit exister (dérivé)")
        val byPronoun = perfekt.rows.associate { it.label to it.form }
        assertEquals("habe gemacht",  byPronoun["ich"],        "ich habe gemacht")
        assertEquals("hast gemacht",  byPronoun["du"],         "du hast gemacht")
        assertEquals("hat gemacht",   byPronoun["er,sie,es"],  "er/sie/es hat gemacht")
        assertEquals("haben gemacht", byPronoun["wir"],        "wir haben gemacht")
        assertEquals("habt gemacht",  byPronoun["ihr"],        "ihr habt gemacht")
        assertEquals("haben gemacht", byPronoun["sie"],        "sie haben gemacht")
    }

    // ── Doublons ─────────────────────────────────────────────────────────────

    @Test
    fun machen_no_duplicate_rows() {
        val groups = buildFormGroups(queryForms("machen", "de"), getFormsConfig("de"), "machen")
        groups.forEach { group ->
            val keys = group.rows.map { Pair(it.label, it.form) }
            assertEquals(keys.size, keys.distinct().size,
                "Doublons détectés dans le groupe '${group.label}'")
        }
    }

    // ── Français — temps composés dérivés ────────────────────────────────────

    private fun lireGroups()  = buildFormGroups(queryForms("lire",  "fr"), getFormsConfig("fr"), "lire")
    private fun allerGroups() = buildFormGroups(queryForms("aller", "fr"), getFormsConfig("fr"), "aller")

    // lire — auxiliaire avoir

    @Test
    fun lire_auxiliary_is_avoir() {
        val auxiliary = lireGroups().find { it.label == "Auxiliaire" }
        assertTrue(auxiliary != null, "Le groupe 'Auxiliaire' doit exister pour 'lire'")
        assertEquals("avoir", auxiliary.rows.firstOrNull()?.form, "Auxiliaire de 'lire' = 'avoir'")
    }

    // aller — auxiliaire être

    @Test
    fun aller_auxiliary_is_etre() {
        val auxiliary = allerGroups().find { it.label == "Auxiliaire" }
        assertTrue(auxiliary != null, "Le groupe 'Auxiliaire' doit exister pour 'aller'")
        assertEquals("être", auxiliary.rows.firstOrNull()?.form, "Auxiliaire de 'aller' = 'être'")
    }

    @Test
    fun aller_passe_compose_uses_etre() {
        val bp = byPronoun(allerGroups(), "Passé composé")
        assertEquals("suis allé",   bp["je"],         "je suis allé")
        assertEquals("es allé",     bp["tu"],         "tu es allé")
        assertEquals("est allé",    bp["il/elle/on"], "il/elle/on est allé")
        assertEquals("sommes allé", bp["nous"],       "nous sommes allé")
        assertEquals("êtes allé",   bp["vous"],       "vous êtes allé")
        assertEquals("sont allé",   bp["ils/elles"],  "ils/elles sont allé")
    }

    private fun byPronoun(groups: List<com.example.myapplication.data.forms.FormGroup>, label: String) =
        groups.find { it.label == label }!!.rows.associate { it.label to it.form }

    @Test
    fun lire_passe_compose_is_derived() {
        val bp = byPronoun(lireGroups(), "Passé composé")
        assertEquals("ai lu",    bp["je"],          "je ai lu")
        assertEquals("as lu",    bp["tu"],           "tu as lu")
        assertEquals("a lu",     bp["il/elle/on"],   "il/elle/on a lu")
        assertEquals("avons lu", bp["nous"],         "nous avons lu")
        assertEquals("avez lu",  bp["vous"],         "vous avez lu")
        assertEquals("ont lu",   bp["ils/elles"],    "ils/elles ont lu")
    }

    @Test
    fun lire_plus_que_parfait_is_derived() {
        val bp = byPronoun(lireGroups(), "Plus-que-parfait")
        assertEquals("avais lu",   bp["je"],         "je avais lu")
        assertEquals("avait lu",   bp["il/elle/on"], "il/elle/on avait lu")
        assertEquals("avions lu",  bp["nous"],       "nous avions lu")
        assertEquals("avaient lu", bp["ils/elles"],  "ils/elles avaient lu")
    }

    @Test
    fun lire_futur_anterieur_is_derived() {
        val bp = byPronoun(lireGroups(), "Futur antérieur")
        assertEquals("aurai lu",  bp["je"],          "je aurai lu")
        assertEquals("aura lu",   bp["il/elle/on"],  "il/elle/on aura lu")
        assertEquals("auront lu", bp["ils/elles"],   "ils/elles auront lu")
    }

    @Test
    fun lire_conditionnel_passe_is_derived() {
        val bp = byPronoun(lireGroups(), "Conditionnel passé")
        assertEquals("aurais lu",   bp["je"],         "je aurais lu")
        assertEquals("aurait lu",   bp["il/elle/on"], "il/elle/on aurait lu")
        assertEquals("aurions lu",  bp["nous"],       "nous aurions lu")
        assertEquals("auraient lu", bp["ils/elles"],  "ils/elles auraient lu")
    }

    @Test
    fun lire_subjonctif_passe_is_derived() {
        val bp = byPronoun(lireGroups(), "Subjonctif passé")
        assertEquals("aie lu",   bp["je"],          "je aie lu")
        assertEquals("ait lu",   bp["il/elle/on"],  "il/elle/on ait lu")
        assertEquals("ayons lu", bp["nous"],        "nous ayons lu")
        assertEquals("aient lu", bp["ils/elles"],   "ils/elles aient lu")
    }
}
