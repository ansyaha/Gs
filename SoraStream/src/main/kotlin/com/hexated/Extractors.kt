package com.hexated

import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.GMPlayer
import com.lagradost.cloudstream3.extractors.StreamSB
import com.lagradost.cloudstream3.extractors.Voe
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder.getCaptchaToken
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*
import java.math.BigInteger
import java.security.MessageDigest

open class Playm4u : ExtractorApi() {
    override val name = "Playm4u"
    override val mainUrl = "https://play9str.playm4u.xyz"
    override val requiresReferer = true
    private val password = "plhq@@@22"

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url, referer = referer).document
        val script = document.selectFirst("script:containsData(idfile =)")?.data() ?: return
        val passScript = document.selectFirst("script:containsData(domain_ref =)")?.data() ?: return

        val pass = passScript.substringAfter("CryptoJS.MD5('").substringBefore("')")
        val amount = passScript.substringAfter(".toString()), ").substringBefore("));").toInt()

        val idFile = "idfile".findIn(script)
        val idUser = "idUser".findIn(script)
        val domainApi = "DOMAIN_API".findIn(script)
        val nameKeyV3 = "NameKeyV3".findIn(script)
        val dataEnc = caesarShift(
            mahoa(
                "Win32|$idUser|$idFile|$referer",
                md5(pass)
            ), amount
        ).toHex()

        val captchaKey =
            document.select("script[src*=https://www.google.com/recaptcha/api.js?render=]")
                .attr("src").substringAfter("render=")
        val token = getCaptchaToken(
            url,
            captchaKey,
            referer = referer
        )

        val source = app.post(
            domainApi, data = mapOf(
                "namekey" to nameKeyV3,
                "token" to "$token",
                "referrer" to "$referer",
                "data" to "$dataEnc|${md5(dataEnc + password)}",
            ), referer = "$mainUrl/"
        ).parsedSafe<Source>()

        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                source?.data ?: return,
                "$mainUrl/",
                Qualities.P1080.value,
                INFER_TYPE
            )
        )

        subtitleCallback.invoke(
            SubtitleFile(
                source.sub?.substringBefore("|")?.toLanguage() ?: return,
                source.sub.substringAfter("|"),
            )
        )

    }

    private fun caesarShift(str: String, amount: Int): String {
        var output = ""
        val adjustedAmount = if (amount < 0) amount + 26 else amount
        for (element in str) {
            var c = element
            if (c.isLetter()) {
                val code = c.code
                c = when (code) {
                    in 65..90 -> ((code - 65 + adjustedAmount) % 26 + 65).toChar()
                    in 97..122 -> ((code - 97 + adjustedAmount) % 26 + 97).toChar()
                    else -> c
                }
            }
            output += c
        }
        return output
    }

    private fun mahoa(input: String, key: String): String {
        val a = CryptoJS.encrypt(key, input)
        return a.replace("U2FsdGVkX1", "")
            .replace("/", "|a")
            .replace("+", "|b")
            .replace("=", "|c")
            .replace("|", "-z")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    private fun String.toHex(): String {
        return this.toByteArray().joinToString("") { "%02x".format(it) }
    }

    private fun String.findIn(data: String): String {
        return "$this\\s*=\\s*[\"'](\\S+)[\"'];".toRegex().find(data)?.groupValues?.get(1) ?: ""
    }

    private fun String.toLanguage() : String {
        return if(this == "EN") "English" else this
    }

    data class Source(
        @JsonProperty("data") val data: String? = null,
        @JsonProperty("sub") val sub: String? = null,
    )

}

class TravelR : GMPlayer() {
    override val name = "TravelR"
    override val mainUrl = "https://travel-russia.xyz"
}

class Mwish : Filesim() {
    override val name = "Mwish"
    override var mainUrl = "https://mwish.pro"
}

class Animefever : Filesim() {
    override val name = "Animefever"
    override var mainUrl = "https://animefever.fun"
}

class Multimovies : Filesim() {
    override val name = "Multimovies"
    override var mainUrl = "https://multimovies.cloud"
}

class MultimoviesSB : StreamSB() {
    override var name = "Multimovies"
    override var mainUrl = "https://multimovies.website"
}

class Yipsu : Voe() {
    override val name = "Yipsu"
    override var mainUrl = "https://yip.su"
}