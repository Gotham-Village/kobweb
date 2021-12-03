package com.varabyte.kobweb.cli.export

import com.varabyte.kobweb.cli.common.Anims
import com.varabyte.kobweb.cli.common.KobwebGradle
import com.varabyte.kobweb.cli.common.consumeProcessOutput
import com.varabyte.kobweb.cli.common.findKobwebProject
import com.varabyte.kobweb.cli.common.assertKobwebProject
import com.varabyte.kobweb.cli.common.handleConsoleOutput
import com.varabyte.kobweb.cli.common.newline
import com.varabyte.konsole.foundation.anim.konsoleAnimOf
import com.varabyte.konsole.foundation.input.Keys
import com.varabyte.konsole.foundation.input.onKeyPressed
import com.varabyte.konsole.foundation.konsoleApp
import com.varabyte.konsole.foundation.konsoleVarOf
import com.varabyte.konsole.foundation.text.textLine
import com.varabyte.konsole.foundation.text.yellow
import kotlinx.coroutines.delay

private enum class ExportState {
    EXPORTING,
    FINISHING,
    FINISHED,
    CANCELLING,
    CANCELLED,
}

@Suppress("BlockingMethodInNonBlockingContext")
fun handleExport(isInteractive: Boolean) {
    if (isInteractive) konsoleApp {
        findKobwebProject() ?: return@konsoleApp

        newline() // Put space between user prompt and eventual first line of Gradle output

        var exportState by konsoleVarOf(ExportState.EXPORTING)

        var cancelReason by konsoleVarOf("")
        val ellipsis = konsoleAnimOf(Anims.ELLIPSIS)
        konsole {
            textLine() // Add space between this block and Gradle text which will appear above
            when (exportState) {
                ExportState.EXPORTING -> textLine("Exporting$ellipsis")
                ExportState.FINISHING -> textLine("Cleaning up$ellipsis")
                ExportState.FINISHED -> textLine("Export finished successfully")
                ExportState.CANCELLING -> yellow { textLine("Cancelling export: $cancelReason$ellipsis") }
                ExportState.CANCELLED -> yellow { textLine("Export cancelled: $cancelReason") }
            }
        }.run {
            val exportProcess = KobwebGradle.export()
            exportProcess.consumeProcessOutput(::handleConsoleOutput)

            onKeyPressed {
                if (exportState == ExportState.EXPORTING && key == Keys.Q) {
                    cancelReason = "User requested cancellation"
                    exportProcess.destroy()
                    exportState = ExportState.CANCELLING
                }
            }

            while (exportProcess.isAlive) {
                delay(300)
            }

            if (exportProcess.exitValue() != 0) {
                if (exportState != ExportState.CANCELLING) {
                    cancelReason =
                        "Server failed to build. Please check Gradle output and fix the errors before continuing."
                    exportState = ExportState.CANCELLING
                }
            }

            if (exportState == ExportState.EXPORTING) {
                exportState = ExportState.FINISHING
            }
            check(exportState in listOf(ExportState.FINISHING, ExportState.CANCELLING))

            val stopProcess = KobwebGradle.stopServer()
            stopProcess.consumeProcessOutput(::handleConsoleOutput)
            stopProcess.waitFor()

            exportState = if (exportState == ExportState.FINISHING) ExportState.FINISHED else ExportState.CANCELLED
        }
    } else {
        assert(!isInteractive)
        assertKobwebProject()
        KobwebGradle.export().also { it.consumeProcessOutput(); it.waitFor() }
        KobwebGradle.stopServer().also { it.consumeProcessOutput(); it.waitFor() }
    }
}