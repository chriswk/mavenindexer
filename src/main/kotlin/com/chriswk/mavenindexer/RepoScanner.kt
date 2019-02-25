package com.chriswk.mavenindexer

import com.chriswk.maven.MavenMetadata
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.logging.log4j.kotlin.Logging
import org.jsoup.Jsoup


class RepoScanner(val rootUrl: String, val kafkaUrl: String) : Logging {
    val client = OkHttpClient.Builder().build()
    val objectMapper = XmlMapper().registerModule(KotlinModule())
    var seenUrls = HashSet<String>()
    fun start() {
        HttpUrl.parse(rootUrl)?.let { url ->
            findMetaData(url)
        }
    }

    fun findMetaData(rootUrl: HttpUrl, child: String? = null) {
        val url = if (child != null && child != "..") {
            rootUrl.newBuilder().addPathSegment(child.dropLast(1)).build()
        } else {
            rootUrl
        }
        if (seenUrls.add(url.toString())) {
            println(url)
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { res ->
                println(res.code())
                if (res.isSuccessful) {
                    res.body()?.let { b ->
                        val body = b.string()
                        val links = Jsoup.parse(body).select("a[href]")
                        val subfolders = links.filter {
                            val href = it.attr("href")
                            href.endsWith("/") &&
                                    !href.contains("..")
                        }
                                .take(10)
                        val mavenMetaData = links.filter { it.attr("href") == "maven-metadata.xml" }.firstOrNull()
                        mavenMetaData?.let { meta ->
                            parseMavenMetaData(url, meta.attr("href"))
                        }
                        subfolders.forEach { findMetaData(url, it.attr("href")) }

                    }
                } else {
                    logger.warn("Something went wrong when getting $url")
                }

            }
        } else {
            println("Had already seen $url")
        }

    }

    fun parseMavenMetaData(context: HttpUrl, path: String) {
        val metaDataUrl = context.newBuilder().addPathSegment(path).build()
        val req = Request.Builder().url(metaDataUrl).get().build()
        client.newCall(req).execute().use { res ->
            if (res.isSuccessful) {
                res.body()?.let { b ->
                    val body = b.string()
                    logger.info(body)
                    val metadata: MavenMetadata = objectMapper.readValue(body)
                    logger.info(metadata)
                }
            }
        }
    }
}
