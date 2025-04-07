package org.example.variabletypestatusplugin

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.StatusBarWidget.TextPresentation
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import com.intellij.util.ui.UIUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import java.awt.event.MouseEvent

class VariableTypeStatus : StatusBarWidgetFactory {
    override fun getId() = "VariableTypeStatus"
    override fun getDisplayName() = "Python Variable Type"
    override fun isAvailable(project: Project) = true

    override fun createWidget(project: Project): StatusBarWidget {
        return VariableTypeStatusWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {}

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class VariableTypeStatusWidget(private val project: Project) : StatusBarWidget, TextPresentation {

    private var statusBar: StatusBar? = null
    private var currentText: String = "Type: unknown"

    init {
        //Listen to cursor position change
        val editorEventMulticaster = EditorFactory.getInstance().eventMulticaster
        editorEventMulticaster.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val editor = event.editor
                updateText(editor)
            }
        }, this)
    }

    private fun updateText(editor: Editor) {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)

        if (psiFile == null) {
            currentText = "No PSI"
            statusBar?.updateWidget(ID())
            return
        }

        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)

        if (element == null) {
            currentText = "No element"
            statusBar?.updateWidget(ID())
            return
        }

        // Szukamy zmiennej lub referencji
        val variable = PsiTreeUtil.getParentOfType(
            element,
            PyReferenceExpression::class.java,
            PyTargetExpression::class.java
        )

        if (variable == null) {
            currentText = "Not a variable"
            statusBar?.updateWidget(ID())
            return
        }

        val context = TypeEvalContext.codeAnalysis(project, psiFile)
        val type = context.getType(variable)

        currentText = if (type != null) {
            "Type: ${type.name}"
        } else {
            "Type: unknown"
        }

        // Update the widget in status bar
        UIUtil.invokeLaterIfNeeded {
            statusBar?.updateWidget(ID())
        }
    }

    override fun ID(): String = "VariableTypeStatus"

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {}

    override fun getText(): String = currentText

    override fun getAlignment(): Float = 0.5f

    override fun getTooltipText(): String = "Type of Python variable under caret"

    override fun getClickConsumer(): Consumer<MouseEvent>? = null

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
}
