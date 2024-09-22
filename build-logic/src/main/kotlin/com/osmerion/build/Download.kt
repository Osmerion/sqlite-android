package com.osmerion.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.HexFormat

@DisableCachingByDefault
abstract class Download : DefaultTask() {

    @get:Input
    abstract val src: Property<String>

    @get:OutputFile
    abstract val dest: RegularFileProperty

    @get:Input
    abstract val checksum: Property<String>

    @get:Input
    abstract val hashingAlgorithm: Property<String>

    init {
        outputs.upToDateWhen {
            dest.get().asFile.exists() && calculateChecksum() == checksum.get()
        }
    }

    @TaskAction
    protected fun download() {
        val httpClient = HttpClient.newHttpClient()

        val request = HttpRequest.newBuilder()
            .uri(URI.create(src.get()))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(dest.get().asFile.toPath()))
        if (response.statusCode() != 200) {
            throw IllegalStateException("Failed to download file: ${response.statusCode()}")
        }

        val actualChecksum = calculateChecksum()
        if (actualChecksum != checksum.get()) {
            throw IllegalStateException("Checksum mismatch: $actualChecksum")
        }
    }

    private fun calculateChecksum(): String {
        val digest = MessageDigest.getInstance(hashingAlgorithm.get())
        DigestInputStream(dest.get().asFile.inputStream(), digest).use {
            it.readAllBytes()

            it.readAllBytes()
        }

        val checksum = digest.digest()
        return HexFormat.of().formatHex(checksum)
    }

}