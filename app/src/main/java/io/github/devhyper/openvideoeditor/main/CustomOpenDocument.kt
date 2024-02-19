package io.github.devhyper.openvideoeditor.main

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

class CustomOpenDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        val intent = super.createIntent(context, input)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
        return intent
    }
}