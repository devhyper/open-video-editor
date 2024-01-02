package io.github.devhyper.openvideoeditor.misc

fun validateInt(string: String): String {
    if (string.toIntOrNull() == null) {
        return "Input must be a valid integer"
    }
    return ""
}

fun validateIntAndNonzero(string: String): String {
    val int = string.toIntOrNull()
    if (int == null) {
        return "Input must be a valid integer"
    } else if (int == 0) {
        return "Input must be nonzero"
    }
    return ""
}

fun validateUInt(string: String): String {
    if (string.toUIntOrNull() == null) {
        return "Input must be a valid unsigned integer"
    }
    return ""
}

fun validateUIntAndNonzero(string: String): String {
    val uint = string.toUIntOrNull()
    if (uint == null) {
        return "Input must be a valid unsigned integer"
    } else if (uint == 0U) {
        return "Input must be nonzero"
    }
    return ""
}

fun validateFloat(string: String): String {
    if (string.toFloatOrNull() == null) {
        return "Input must be a valid float"
    }
    return ""
}

fun validateFloatAndNonzero(string: String): String {
    val float = string.toFloatOrNull()
    if (float == null) {
        return "Input must be a valid float"
    } else if (float == 0F) {
        return "Input must be nonzero"
    }
    return ""
}

fun validateUFloat(string: String): String {
    val float = string.toFloatOrNull()
    if (float == null || float < 0) {
        return "Input must be a valid unsigned float"
    }
    return ""
}

fun validateUFloatAndNonzero(string: String): String {
    val float = string.toFloatOrNull()
    if (float == null || float < 0) {
        return "Input must be a valid unsigned float"
    } else if (float == 0F) {
        return "Input must be nonzero"
    }
    return ""
}
