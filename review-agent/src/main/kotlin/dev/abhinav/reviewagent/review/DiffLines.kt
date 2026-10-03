package dev.abhinav.reviewagent.review

/**
 * The lines on the new side of a unified diff that GitHub accepts review comments on:
 * every line inside a hunk (added or context), per file. Deleted files have none.
 */
class DiffLines private constructor(private val linesByFile: Map<String, Set<Int>>) {

    val files: Set<String> get() = linesByFile.keys

    fun contains(file: String, line: Int): Boolean = linesByFile[file]?.contains(line) == true

    companion object {
        private val hunkHeader = Regex("""^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@""")

        fun parse(diff: String): DiffLines {
            val result = mutableMapOf<String, MutableSet<Int>>()
            var currentFile: String? = null
            var nextLine = 0

            for (line in diff.lines()) {
                when {
                    line.startsWith("diff --git ") -> currentFile = null
                    line.startsWith("+++ ") -> {
                        val path = line.removePrefix("+++ ")
                        currentFile = if (path == "/dev/null") null else path.removePrefix("b/")
                    }
                    line.startsWith("--- ") -> Unit
                    line.startsWith("@@") -> nextLine = hunkHeader.find(line)?.groupValues?.get(1)?.toInt() ?: 0
                    currentFile == null || nextLine == 0 -> Unit
                    line.startsWith("+") || line.startsWith(" ") -> {
                        result.getOrPut(currentFile) { mutableSetOf() } += nextLine
                        nextLine++
                    }
                    // "-" lines exist only on the old side; "\ No newline" lines are markers.
                    else -> Unit
                }
            }
            return DiffLines(result)
        }
    }
}
