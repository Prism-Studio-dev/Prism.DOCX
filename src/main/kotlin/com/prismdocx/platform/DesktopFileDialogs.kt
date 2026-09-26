package com.prismdocx.platform

import com.prismdocx.editor.SaveTarget
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

/** Нативные диалоги вызываются из обработчиков UI, а не из фоновых файловых операций. */
object DesktopFileDialogs {
    fun chooseDocument(): File? = JFileChooser().run {
        dialogTitle = "Открыть DOCX"
        fileFilter = FileNameExtensionFilter("Документы Word (.docx)", "docx")
        isAcceptAllFileFilterUsed = false
        if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) selectedFile else null
    }

    fun chooseSaveTarget(source: File): SaveTarget? {
        val chooser = JFileChooser().apply {
            dialogTitle = "Сохранить копию"
            selectedFile = File(source.parentFile, source.nameWithoutExtension + "_metadata.docx")
            fileFilter = FileNameExtensionFilter("Документы Word (.docx)", "docx")
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
        val selected = chooser.selectedFile
        val target = if (selected.extension.equals("docx", true)) selected else File(selected.parentFile, selected.name + ".docx")
        if (target.canonicalFile == source.canonicalFile) {
            JOptionPane.showMessageDialog(null, "Укажите другое имя для копии.", "Исходный документ", JOptionPane.INFORMATION_MESSAGE)
            return null
        }
        val overwrite = target.exists()
        if (overwrite && JOptionPane.showConfirmDialog(null, "Заменить файл ${target.name}?", "Файл уже существует",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return null
        return SaveTarget(target, overwrite)
    }
}
