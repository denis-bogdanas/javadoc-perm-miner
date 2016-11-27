package edu.oregonstate.jdminer.inspect;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import edu.oregonstate.jdminer.inspect.jaxb.JaxbStmt;
import edu.oregonstate.jdminer.util.PluginUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 5/2/2016.
 */
class PermissionGuardQuickFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    private JaxbStmt jaxbStmt;
    private String permission;

    PermissionGuardQuickFix(@Nullable PsiElement element, @NotNull JaxbStmt jaxbStmt) {
        super(element);
        this.jaxbStmt = jaxbStmt;
        this.permission = jaxbStmt.getUncheckedPermissions().iterator().next();
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file,
                       @Nullable("is null when called from inspection") Editor editor,
                       @NotNull PsiElement startElement,
                       @NotNull PsiElement endElement) {

        PluginUtil.openFileInEditor(project, file);
        OpenFileDescriptor fileDesc =
                new OpenFileDescriptor(project, file.getVirtualFile(), startElement.getTextOffset());
        Editor openEditor = FileEditorManager.getInstance(project).openTextEditor(fileDesc, true);
        assert openEditor != null;

        int startOffset = startElement.getTextOffset();
        int endOffset = endElement.getTextOffset() + endElement.getTextLength();
        openEditor.getSelectionModel().setSelection(startOffset, endOffset);

        //DataContext dataContext = DataManager.getInstance().getDataContext(openEditor.getComponent());
    }

    @NotNull
    @Override
    public String getText() {
        return "Insert guards for " + permission;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Insert permission guard getFamilyName()";
    }
}
