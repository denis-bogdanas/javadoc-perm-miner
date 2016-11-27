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
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionGuardInspection extends GlobalInspectionTool {

    private static final Logger LOG = Logger.getInstance(PermissionGuardInspection.class);

    @Override
    public boolean isGraphNeeded() {
        return false;
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
        GlobalSearchScope libScope = ProjectScope.getLibrariesScope(globalContext.getProject());
        final String perm = "ACCESS_COARSE_LOCATION";
        //todo only Java files
        PsiFile[] filesWithLoc = CacheManager.SERVICE.getInstance(globalContext.getProject())
                .getFilesWithWord(perm, UsageSearchContext.IN_COMMENTS, libScope, true);
        processFilesWithLocation(filesWithLoc, perm);
    }

    private void processFilesWithLocation(PsiFile[] filesWithLoc, String perm) {
        System.out.println("\nClasses containing " + perm + " in comments: " + filesWithLoc.length);
        int totalOccurrences = Arrays.stream(filesWithLoc)
                .mapToInt(elem -> occurrencesInType(elem, perm, PsiComment.class)).sum();
        int javadocOccurrences = Arrays.stream(filesWithLoc)
                .mapToInt(elem -> occurrencesInType(elem, perm, PsiDocComment.class)).sum();
        System.out.println(
                "Total occurrences in \n\tall comments: " + totalOccurrences + "\n\tjavadoc: " + javadocOccurrences);
        System.out.println("==============================================");
        //noinspection unchecked
        Arrays.stream(filesWithLoc).flatMap(file -> PsiTreeUtil.getChildrenOfAnyType(file, PsiClass.class).stream())
                .forEach(psiClass -> {
                    System.out.println(psiClass.getQualifiedName()
                            + ", com:" + occurrencesInType(psiClass, perm, PsiComment.class)
                            + ", javadoc:" + occurrencesInType(psiClass, perm, PsiDocComment.class));
                    Collection<PsiDocCommentOwner> childCommentOwners =
                            PsiTreeUtil.findChildrenOfType(psiClass, PsiDocCommentOwner.class);
                    List<PsiDocCommentOwner> commentOwners = new ArrayList<>(childCommentOwners.size() + 1);
                    commentOwners.add(psiClass);
                    commentOwners.addAll(childCommentOwners);

                    List<PsiDocCommentOwner> locCommentOwners = commentOwners.stream().filter(comOwner ->
                            comOwner.getDocComment() != null
                                    && comOwner.getDocComment().getText().contains(perm)
                    ).collect(Collectors.toList());
                    for (PsiDocCommentOwner elem : locCommentOwners) {
                        //noinspection ConstantConditions
                        System.out.println("\t" + elem.getNode().getElementType() + ": " + elem.getName()
                                + ": " + occurrences(elem.getDocComment().getText(), perm));
                    }
                });
        System.out.println();
    }

    private static int occurrences(String str, String substr) {
        int occurrences = 0;
        int index = str.indexOf(substr);
        while (index != -1) {
            occurrences++;
            index = str.indexOf(substr, index + 1);
        }
        return occurrences;
    }

    private static int occurrencesInType(PsiElement elem, String str, Class<? extends PsiElement> psiClass) {
        //noinspection unchecked
        return PsiTreeUtil.findChildrenOfAnyType(elem, psiClass).stream()
                .mapToInt(comm -> occurrences(comm.getText(), str)).sum();
    }

    private TextRange lineToTextRange(int droidPermLine, PsiMethod psiMethod) {
        PsiDocumentManager docManager = PsiDocumentManager.getInstance(psiMethod.getProject());
        Document document = docManager.getDocument(psiMethod.getContainingFile());
        assert document != null;
        int intellijLine = droidPermLine - 1; // in DroidPerm lines start from 1, in Intellij - from 0.

        return new TextRange(document.getLineStartOffset(intellijLine), document.getLineEndOffset(intellijLine));
    }
}
