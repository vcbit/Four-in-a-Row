package com.vitorpamplona.amethyst.ui.actions

import android.content.ContentResolver
import android.net.Uri
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.util.*

object ImageUploader {
    fun uploadImage(
        uri: Uri,
        contentResolver: ContentResolver,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val contentType = contentResolver.getType(uri)

        val client = OkHttpClient.Builder().build()

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "${UUID.randomUUID()}",
                object : RequestBody() {
                    override fun contentType(): MediaType? =
                        contentType?.toMediaType()

                    override fun writeTo(sink: BufferedSink) {
                        val imageInputStream = contentResolver.openInputStream(uri)
                        checkNotNull(imageInputStream) {
   