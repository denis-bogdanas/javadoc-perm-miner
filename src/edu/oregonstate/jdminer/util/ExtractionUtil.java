package edu.oregonstate.jdminer.util;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.extractMethod.ExtractMethodHandler;
import com.intellij.refactoring.extractMethod.ExtractMethodProcessor;
import com.intellij.refactoring.extractMethod.PrepareFailedException;
import com.intellij.refactoring.util.RefactoringUtil;
import com.siyeh.ipp.psiutils.PsiSelectionSearcher;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Nicholas Nelson <nelsonni@oregonstate.edu> Created on on 7/28/16.
 */
public class ExtractionUtil {
    /**
     * Examines all PsiElements within the current selection, returning a list of PsiLocalVariable elements that
     * represent all local variables declared or referenced from within that selection.
     *
     * @param project       Project
     * @param editor        editor to get text selection
     * @return local variable elements referenced or declared in the selection
     */
    @NotNull
    public static List<PsiLocalVariable> getLocalVariables(@NotNull Project project, @NotNull Editor editor) {
        // TODO: restrict to local variables that are used outside of the selection context
        List<PsiLocalVariable> localVariables =
                PsiSelectionSearcher.searchElementsInSelection(editor, project, PsiLocalVariable.class, true).stream()
                        .filter(PsiElement::isPhysical)
                        .collect(Collectors.toList());

        List<PsiLocalVariable> referencedVariables =
                PsiSelectionSearcher.searchElementsInSelection(editor, project, PsiExpression.class, true).stream()
                        .flatMap(expression -> RefactoringUtil.collectReferencedVariables(expression).stream()
                                .filter(PsiElement::isPhysical)
                                .filter(variable -> variable instanceof PsiLocalVariable)
                                .map(variable -> (PsiLocalVariable) variable))
                        .distinct()
                        .collect(Collectors.toList());

        return Stream.concat(localVariables.stream(), referencedVariables.stream()).distinct()
                .collect(Collectors.toList());
    }

    /**
     * Examines all PsiElements within the current selection, returning a list of PsiExpression elements that
     * represent all local references to variables from within that selection. Variables can be declared outside
     * of the scope of the selection.
     *
     * @param project       Project
     * @param editor        editor to get text selection
     * @return reference expression elements declared in the selection
     */
    @NotNull
    public static List<PsiExpression> getLocalExpressions(@NotNull Project project, @NotNull Editor editor) {
        TargetElementUtil util = TargetElementUtil.getInstance();
        return PsiSelectionSearcher.searchElementsInSelection(editor, project, PsiExpression.class, true).stream()
                .filter(expression -> expression.getContainingFile() != null)
                .filter(expression -> expression instanceof PsiReferenceExpression)
                .filter(expression -> {
                    PsiReferenceExpression refExpression = (PsiReferenceExpression) expression;
                    return (refExpression.resolve() != null);
                })
                .filter(expression -> {
                    PsiReferenceExpression refExpression = (PsiReferenceExpression) expression;
                    PsiElement resolve = refExpression.resolve();
                    assert resolve != null;
                    PsiElement element = util.findTargetElement(editor, util.getReferenceSearchFlags(),
                            resolve.getTextOffset());
                    assert element != null;
                    return (element instanceof PsiVariable);
                })
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Prepares an ExtractMethodProcessor that can smartly extract all selected elements into a new method that can
     * be guarded cleanly with Android Run-time Permissions.
     *
     * @param project       Project
     * @param editor        editor to get text selection
     * @param psiFile       PsiFile
     * @param newMethodName String containing the desired name of the new method inserted into the psiFile
     * @return ExtractMethodProcessor that will extract selection to a new method; see ExtractMethodProcessor.run()
     * @throws PrepareFailedException thrown if no ExtractMethodProcessor can be instantiated
     */
    @NotNull
    public static ExtractMethodProcessor getExtractMethodProcessor(Project project, Editor editor,
                                                                   PsiFile psiFile, String newMethodName)
            throws PrepareFailedException {
        final PsiElement[] elements = ExtractMethodHandler.getElements(project, editor, psiFile);
        final ExtractMethodProcessor processor =
                new ExtractMethodProcessor(project, editor, elements, null, "Extract Method", newMethodName, null);
        processor.prepare();
        processor.testPrepare();
        return processor;
    }

    /**
     * Update the selection to contain the complete line(s) of the current selection.
     * If the selection starts on the same line as the method header, the selection
     * will begin at the beginning of the first PsiElement in the PsiMethod Body
     *
     * @param psiFile        the file that is open in the editor
     * @param document       the document containing the current selection
     * @param selectionModel the current selection
     */
    public static void expandLineSelection(PsiFile psiFile, Document document, SelectionModel selectionModel) {
        Pair<Integer, Integer> selection = getExpandedLineOffsets(psiFile, document, selectionModel);

        selectionModel.setSelection(selection.getFirst(), selection.getSecond());
    }

    public static Pair<Integer, Integer> getExpandedLineOffsets(PsiFile psiFile, Document document,
                                                             SelectionModel selectionModel) {
        int selectionStart = selectionModel.getSelectionStart();
        PsiMethod psiMethod = PluginUtil.getContainingMethod(psiFile, selectionStart);
        PsiElement psiElement = PsiUtil.getElementAtOffset(psiFile, selectionStart);

        final int startLineNumber = document.getLineNumber(selectionStart);
        final int endLineNumber = document.getLineNumber(selectionModel.getSelectionEnd());
        final int lineStartOffset = document.getLineStartOffset(startLineNumber);
        final int lineEndOffset = document.getLineEndOffset(endLineNumber);

        if(selectionStartSameLineAsMethodHeader(document, psiMethod, psiElement)){
            return new Pair<>(getSameLineOffset(psiMethod), lineEndOffset);
        }else {
            return new Pair<>(lineStartOffset, lineEndOffset);
        }
    }

    /**
     * @param document          the document containing the current selection
     * @param psiMethod         the PsiMethod that the selection is in
     * @param psiElement        the element at the start of the current selection
     * @return                  true if the selectionStart is on the same line as the header of the method it is in
     *                          false otherwise
     */
    private static boolean selectionStartSameLineAsMethodHeader(Document document, PsiMethod psiMethod,
                                                                PsiElement psiElement){
        int headerOffset = psiMethod.getTextOffset();
        int elementOffset = psiElement.getTextOffset();
        int headerLine = document.getLineNumber(headerOffset);
        int elementLine = document.getLineNumber(elementOffset);
        return (headerLine == elementLine);
    }

    /**
     *
     * @param psiMethod     the PsiMethod that the selection is in
     * @return              the offset of the beginning of the first psiElement
     */
    private static int getSameLineOffset(PsiMethod psiMethod){
        return psiMethod.getBody().getFirstBodyElement().getTextOffset();
    }

    /**
     * Update the selection to remove a prefix string from the current selection.
     *
     * @param prefix         the prefix string to remove from the current selection
     * @param selectionModel the current selection
     */
    public static void contractSelectionOnPrefix(String prefix, SelectionModel selectionModel) {
        if (selectionModel.hasSelection()) {
            //noinspection ConstantConditions
            if (selectionModel.getSelectedText().startsWith(prefix)) {
                int start = selectionModel.getSelectionStart() + prefix.length();
                int end = selectionModel.getSelectionEnd();
                selectionModel.setSelection(start, end);
            }
        }
    }

    /**
     * Update the selection bounds to shift n line(s) down or up from the starting line and m line(s) down or up from
     * the ending line of the current selection. For x line(s) at the top of the current selection, (n + x) becomes
     * the shift for the starting line, where x is the number of lines beginning with the prefix string.
     *
     * @param document       the document containing the current selection
     * @param selectionModel teh current selection
     * @param prefix         the prefix string to match for removing a line from the current selection
     * @param startLineShift the n number of lines to shift; shift down on positive, shift up on negative
     * @param endLineShift   the m number of lines to shift; shift down on positive, shift up on negative
     */
    // want to make startLineShift and endLineShift start at 0
    //public static void shiftLineSelectionOnPrefix(Document document, SelectionModel selectionModel, String prefix, int startLineShift, int endLineShift) {
    public static void shiftLineSelectionOnPrefix(Document document, SelectionModel selectionModel, String prefix) {
        if (selectionModel.hasSelection()) {
            int prefixLineShift = 0;
            @SuppressWarnings("ConstantConditions")
            Scanner scanner = new Scanner(selectionModel.getSelectedText());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (!line.trim().startsWith(prefix)) {
                    break;
                }
                prefixLineShift++;
            }
            /*
            System.out.println("---ShiftLineSelectionOnPrefix: ");
            System.out.print(startLineShift);
            System.out.print(endLineShift);
            System.out.println("\n------------------\n");
            */
            //shiftLineSelection(document, selectionModel, startLineShift + prefixLineShift, endLineShift);
            shiftLineSelection(document, selectionModel, prefixLineShift);
        }
    }

    /**
     * Update the selection bounds to shift n line(s) down or up from the starting line and m line(s) down or up from
     * the ending line of the current selection.
     *
     * @param document       the document containing the current selection
     * @param selectionModel the current selection
     * @param startLineShift the n number of lines to shift; shift down on positive, shift up on negative
     * @param endLineShift   the m number of lines to shift; shift down on positive, shift up on negative
     */
    //public static void shiftLineSelection(Document document, SelectionModel selectionModel, int startLineShift, int endLineShift) {
     public static void shiftLineSelection(Document document, SelectionModel selectionModel, int numberLineShift) {
        /*
        System.out.println("---ShiftLineSelection: ");
        System.out.print(startLineShift);
        System.out.print(endLineShift);
        System.out.println("\n------------------\n");
        System.out.println("\n-------OVER-------\n");
        */
        final int startLineNumber = document.getLineNumber(selectionModel.getSelectionStart());
        final int endLineNumber = document.getLineNumber(selectionModel.getSelectionEnd());

        //System.out.print(startLineNumber);
        //System.out.print(endLineNumber);
        //final int lineStartOffset = document.getLineStartOffset(startLineNumber + startLineShift);
        //final int lineEndOffset = document.getLineEndOffset(endLineNumber + endLineShift);

        final int lineStartOffset = document.getLineStartOffset(startLineNumber + numberLineShift);
        final int lineEndOffset = document.getLineEndOffset(endLineNumber);
        selectionModel.setSelection(lineStartOffset, lineEndOffset);
    }
}
