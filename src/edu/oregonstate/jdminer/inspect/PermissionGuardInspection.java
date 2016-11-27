package edu.oregonstate.jdminer.inspect;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.GlobalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptionsProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.javadoc.PsiDocMethodOrFieldRef;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PermissionGuardInspection extends GlobalInspectionTool {

    private static final Logger LOG = Logger.getInstance(PermissionGuardInspection.class);

    @Override
    public boolean isGraphNeeded() {
        return true;
    }

    @Override
    public void runInspection(@NotNull AnalysisScope scope, @NotNull InspectionManager manager,
                              @NotNull GlobalInspectionContext globalContext,
                              @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        Module module = null;
        try {
            module = IntellijUtil.getModule(scope, globalContext.getProject());
        } catch (DroidPermException e) {
            LOG.error(e);
        }
        if (module == null) {
            LOG.warn("Analysis scope does not contain a module.");
            return;
        }
        LOG.info("inspecting module " + module.getName());
        PsiSearchHelper searchHelper = PsiSearchHelper.SERVICE.getInstance(globalContext.getProject());
        GlobalSearchScope libScope = ProjectScope.getLibrariesScope(globalContext.getProject());
        PsiElement[] commentsWithLocation =
                searchHelper.findCommentsContainingIdentifier("ACCESS_COARSE_LOCATION", libScope);
        System.out.println("\nUsages in comments for ACCESS_COARSE_LOCATION:");
        System.out.println("==============================================");
        Arrays.stream(commentsWithLocation).forEach(comment -> {
            PsiElement parent = comment;
            while (parent instanceof PsiDocToken || parent instanceof PsiDocTag || parent instanceof PsiDocComment
                    || parent instanceof PsiDocMethodOrFieldRef) {
                parent = parent.getParent();
            }
            PsiClass parentClass = PsiTreeUtil.getParentOfType(parent, PsiClass.class);
            String prefix = (parentClass != null ? parentClass.getName() : null) + ": "
                    + parent.getClass().getSimpleName() + ": ";
            if (parent instanceof PsiNameIdentifierOwner) {
                PsiNameIdentifierOwner nameOwner = (PsiNameIdentifierOwner) parent;
                String outText = prefix
                        + (nameOwner.getNameIdentifier() != null ? nameOwner.getNameIdentifier().getText()
                                                                 : nameOwner.getText());
                System.out.println(outText);
            } else {
                System.out.println(prefix + parent.getText());
            }
        });
        System.out.println();
    }

    private TextRange lineToTextRange(int droidPermLine, PsiMethod psiMethod) {
        PsiDocumentManager docManager = PsiDocumentManager.getInstance(psiMethod.getProject());
        Document document = docManager.getDocument(psiMethod.getContainingFile());
        assert document != null;
        int intellijLine = droidPermLine - 1; // in DroidPerm lines start from 1, in Intellij - from 0.

        return new TextRange(document.getLineStartOffset(intellijLine), document.getLineEndOffset(intellijLine));
    }
}
