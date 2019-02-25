package com.chriswk.maven

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

data class GAV(val groupId: String, val artifactId: String, val version: String)

@JacksonXmlRootElement(localName = "metadata")
data class MavenMetadata(@JacksonXmlProperty(isAttribute = true) val modelVersion: String?, val groupId: String, val artifactId: String, val version: String?, val versioning: Versioning?)
data class Versioning(val versions: List<String>, val latest: String?, val release: String?, val lastUpdated: String?)
