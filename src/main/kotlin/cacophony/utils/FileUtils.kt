package cacophony.utils

import java.nio.file.Path
import java.nio.file.Paths

fun withExtension(file: Path, extension: String) =
    Paths.get(
        file.toString().let { name ->
            name.substring(0, name.lastIndexOf('.')) + extension
        },
    )
