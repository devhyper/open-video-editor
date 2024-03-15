package io.github.devhyper.openvideoeditor.main

import android.content.Context
import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

class CustomPickVisualMedia(private val useLegacyFilePicker: () -> Boolean) :
    ActivityResultContracts.PickVisualMedia() {
    private fun getVisualMimeType(input: VisualMediaType): String? {
        return when (input) {
            is ImageOnly -> "image/*"
            is VideoOnly -> "video/*"
            is SingleMimeType -> input.mimeType
            is ImageAndVideo -> null
        }
    }

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        val intent = if (useLegacyFilePicker()) {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = getVisualMimeType(input.mediaType)

                if (type == null) {
                    // ACTION_OPEN_DOCUMENT requires to set this parameter when launching the
                    // intent with multiple mime types
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                }
            }
        } else {
            super.createIntent(context, input)
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        return intent
    }
}