package dev.alenajam.monsterdialer.packs.data

import java.io.PrintWriter
import java.io.StringWriter

/** A shareable, content-free report describing why one import attempt failed. */
data class CharacterPackImportDiagnostic(
    val fileName: String,
    val summary: String,
    val report: String
) {
    companion object {
        fun from(fileName: String, error: Throwable): CharacterPackImportDiagnostic {
            val summary = error.message ?: "The selected file could not be imported."
            val causes = generateSequence(error) { it.cause }
                .mapIndexed { index, cause ->
                    if (index == 0) cause::class.java.name else "Caused by ${cause::class.java.name}"
                }
                .joinToString("\n")
            val stackTrace = StringWriter().also { writer ->
                error.printStackTrace(PrintWriter(writer))
            }.toString().trim()
            val report = buildString {
                appendLine("MonsterDialer character pack import diagnostic")
                appendLine("Result: Failed")
                appendLine("File: $fileName")
                appendLine("Supported formatVersion: ${CharacterPackValidator.SupportedFormatVersion}")
                appendLine("Error: $summary")
                appendLine("Exception chain:")
                appendLine(causes)
                appendLine()
                appendLine("Stack trace:")
                append(stackTrace)
            }
            return CharacterPackImportDiagnostic(fileName, summary, report)
        }
    }
}
