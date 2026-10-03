package dev.abhinav.reviewagent.tools

import java.io.File

/** Resolves a model-supplied path inside [root], or returns null if it would escape it. */
internal fun resolveInRepo(root: File, path: String): File? {
    if (path.isBlank() || File(path).isAbsolute) return null
    val rootCanonical = root.canonicalFile
    val resolved = File(rootCanonical, path).canonicalFile
    // canonicalFile resolves "..", "." and symlinks, so this catches every way out of the repo.
    return resolved.takeIf { it == rootCanonical || it.path.startsWith(rootCanonical.path + File.separator) }
}
