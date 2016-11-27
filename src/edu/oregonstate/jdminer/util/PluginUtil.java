package edu.oregonstate.jdminer.util;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicholas Nelson <nelsonni@oregonstate.edu> Created on on 7/10/16.
 */
public class PluginUtil {

    @Nullable
    public static PsiClass getTopLevelClass(PsiFile psiFile, Integer offset) {
        PsiElement psiElement = PsiUtil.getElementAtOffset(psiFile, offset);
        return PsiUtil.getTopLevelClass(psiElement);
    }



    public static PsiClass getContainingClass(@NotNull PsiFile file, int offset) {
        PsiElement element = PsiUtil.getElementAtOffset(file, offset);
        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }

    @Nullable
    @Contract("null, _ -> null")
    public static PsiMethod getContainingMethod(@NotNull PsiFile file, int offset) {
        PsiElement psiElement = PsiUtil.getElementAtOffset(file, offset);
        return PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
    }

    @NotNull
    public static Integer getMethodLineCount(@NotNull PsiMethod psiMethod, @NotNull Document document) {
        final PsiCodeBlock body = psiMethod.getBody();
        assert body != null;
        PsiElement firstBodyElement = body.getFirstBodyElement();
        assert firstBodyElement != null;
        PsiElement lastBodyElement = body.getLastBodyElement();
        assert lastBodyElement != null;

        int startLineNumber = document.getLineNumber(firstBodyElement.getTextOffset());
        int endLineNumber = document.getLineNumber(lastBodyElement.getTextOffset());
        return endLineNumber - startLineNumber + 1;
    }

    @NotNull
    static String getMethodSignature(@NotNull PsiMethod psiMethod) {
        if (!psiMethod.isPhysical()) System.out.println("Method '" + psiMethod.getName() + "' is not physical");
        MethodSignature signature = psiMethod.getSignature(PsiSubstitutor.EMPTY);
        PsiType[] parameterTypes = signature.getParameterTypes();
        String[] parameterSignatures = Arrays.stream(parameterTypes)
                .map(PsiType::getPresentableText)
                .map(s -> Character.toLowerCase(s.charAt(0)) + s.substring(1))
                .toArray(String[]::new);
        String parameters = StringUtil.join(parameterSignatures, ",");
        return signature.getName() + "(" + parameters + ");";
    }

    @NotNull
    public static String getUniqueMethodName(@NotNull PsiClass psiClass, String candidateName) {
        List<String> methodNames = Arrays.stream(psiClass.getMethods())
                .map(NavigationItem::getName)
                .collect(Collectors.toList());
        int iterator = 1;
        if (methodNames.contains(candidateName)) {
            candidateName = candidateName.concat(Integer.toString(iterator));
        }
        while (methodNames.contains(candidateName)) {
            iterator += 1;
            candidateName = candidateName.substring(0, candidateName.length() - 1) + iterator;
        }
        return candidateName;
    }

    @NotNull
    public static String getUniqueFieldName(@NotNull PsiClass psiClass, String candidateName) {
        @SuppressWarnings("unchecked")
        Collection<PsiField> psiFields = PsiTreeUtil.collectElementsOfType(psiClass, PsiField.class);
        List<String> fieldNames = psiFields.stream()
                .map(NavigationItem::getName)
                .collect(Collectors.toList());
        int iterator = 1;
        if (fieldNames.contains(candidateName)) {
            candidateName = candidateName.concat(Integer.toString(iterator));
        }
        while (fieldNames.contains(candidateName)) {
            iterator += 1;
            candidateName = candidateName.substring(0, candidateName.length() - 1) + iterator;
        }
        return candidateName;
    }

    @Nullable
    public static PsiElement getFieldLocation(PsiClass psiClass) {
        PsiField[] fields = psiClass.getFields();
        if (fields.length > 0) {
            return fields[fields.length-1];
        } else {
            final PsiElement lBrace = psiClass.getLBrace();
            if (lBrace != null) {
                PsiElement result = lBrace.getNextSibling();
                while (result.getNextSibling() instanceof PsiWhiteSpace) {
                    result = result.getNextSibling();
                }
                return result;
            }
        }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    public static int getSelectionLineCount(SelectionModel selectionModel) {
        if (selectionModel.hasSelection()) {
            return selectionModel.getSelectedText().split("[\n|\r]").length;
        }
        return 0;
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean isSelectionBlank(SelectionModel selectionModel) {
        return selectionModel.hasSelection() && selectionModel.getSelectedText().trim().length() == 0;
    }


    /**
     * @param psiMethod
     * @return The first PsiElement in psiMethod
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static PsiElement getMethodStatementLocation(PsiMethod psiMethod) {
        PsiCodeBlock body = psiMethod.getBody();
        if (body != null) {
            final PsiJavaToken lBrace = body.getLBrace();
            if (lBrace != null) {
                PsiElement result = lBrace.getNextSibling();
                PsiSuperExpression superExpression =
                        PsiTreeUtil.findChildOfAnyType(result, false, PsiSuperExpression.class);
                PsiClass containingClass = psiMethod.getContainingClass();
                assert containingClass != null;
                while (result instanceof PsiWhiteSpace ||
                        (superExpression != null && thisOrSuperReference(superExpression, containingClass))) {
                    result = result.getNextSibling();
                    superExpression = PsiTreeUtil.findChildOfAnyType(result, false, PsiSuperExpression.class);
                }

                return result;
            }
        }
        return null;
    }

    // Reference: com.intellij.codeInsight.daemon.impl.analysis.HighlightUtil::thisOrSuperReference()
    private static boolean thisOrSuperReference(@Nullable PsiExpression qualifierExpression, PsiClass aClass) {
        if (qualifierExpression == null) return true;
        PsiJavaCodeReferenceElement qualifier;
        if (qualifierExpression instanceof PsiThisExpression) {
            qualifier = ((PsiThisExpression)qualifierExpression).getQualifier();
        }
        else if (qualifierExpression instanceof PsiSuperExpression) {
            qualifier = ((PsiSuperExpression)qualifierExpression).getQualifier();
        }
        else {
            return false;
        }
        if (qualifier == null) return true;
        PsiElement resolved = qualifier.resolve();
        return resolved instanceof PsiClass && InheritanceUtil.isInheritorOrSelf(aClass, (PsiClass)resolved, true);
    }

    public static void openFileInEditor(Project project, PsiFile psiFile) {
        String path = psiFile.getVirtualFile().getCanonicalPath();
        assert path != null;
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(path);
        assert vf != null;
        fileEditorManager.openFile(vf, true, true);
    }

    public static String trimPermission(String permission){
        String[] permissionTemp = permission.split("\\.");
        return permissionTemp[permissionTemp.length - 1];
    }

    public static boolean preconditions(PsiFile psiFile, Editor editor) {
        if (editor != null && psiFile != null) {
            boolean isJavaFileType = psiFile.getFileType().equals(StdFileTypes.JAVA);
            PsiElement selectedElement = psiFile.findElementAt(editor.getSelectionModel().getSelectionStart());
            PsiMethod containingMethod = PsiTreeUtil.getParentOfType(selectedElement, PsiMethod.class, false);


            //TODO: make hasAndroidSDK() return true for the test fixture. Then make sure methodOnLine works correctly.
            if (isJavaFileType && hasAndroidSDK() && containingMethod != null && methodOnLine(psiFile, editor)) {
                // enable action only in Java file with selection inside of a PsiMethod
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean methodOnLine(PsiFile psiFile, Editor editor) {
        int start;
        if(isSelectionBlank(editor.getSelectionModel())){
            Pair<Integer, Integer> selection = ExtractionUtil.getExpandedLineOffsets(psiFile, editor.getDocument(),
                    editor.getSelectionModel());
            start = selection.getFirst();
        }else{
            start = editor.getSelectionModel().getSelectionStart();
        }

        PsiElement currentElement = PsiUtil.getElementAtOffset(psiFile, start);
        while(!currentElement.textMatches("\n")){
            if(currentElement instanceof PsiMethod ){
                return true;
            }
            currentElement = currentElement.getNextSibling();
        }
        return false;
    }

    private static boolean hasAndroidSDK() {
        Sdk[] allJDKs = ProjectJdkTable.getInstance().getAllJdks();
        for (Sdk sdk : allJDKs) {
            if (sdk.getSdkType().getName().toLowerCase().contains("android")) {
                return true;
            }
        }
        return false; // no Android SDK found
    }
}
