package edu.oregonstate.jdminer.inspect;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefMethod;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiStatement;
import edu.oregonstate.jdminer.inspect.jaxb.JaxbCallback;
import edu.oregonstate.jdminer.inspect.jaxb.JaxbLoadUtil;
import edu.oregonstate.jdminer.inspect.jaxb.JaxbStmt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PermissionGuardInspection extends GlobalInspectionTool {

    private static final Logger LOG = Logger.getInstance(PermissionGuardInspection.class);

    private static final String ALERT_MESSAGE = "Permission guard required for: ";

    private List<JaxbCallback> droidPermData;

    @Override
    public boolean isGraphNeeded() {
        //todo - test if works with false.
        // We don't need references to be resolved,
        // but It's not clear whether value false will produce the same sequence of analyzed elements.
        return true;
    }

    @Override
    public void runInspection(@NotNull AnalysisScope scope, @NotNull InspectionManager manager,
                              @NotNull GlobalInspectionContext globalContext,
                              @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        try {
            //todo support for scope containing multiple modules
            Module module = IntellijUtil.getModule(scope, globalContext.getProject());
            if (module == null) {
                LOG.warn("Analysis scope does not contain a module.");
                return;
            }
            LOG.info("inspecting module " + module.getName());
            runDroidPerm(module);
        } catch (DroidPermException e) {
            LOG.error(e);
        }

        super.runInspection(scope, manager, globalContext, problemDescriptionsProcessor);
    }

    private void runDroidPerm(Module module) {
        try {
            File xmlFile = DroidPermRunner.run(module);
            droidPermData = JaxbLoadUtil.load(xmlFile);
            LOG.info("DroidPerm executed successfully");
        } catch (DroidPermException e) {
            LOG.error(e);
        } catch (JAXBException e) {
            LOG.error("JaxbLoadUtil unable to load XML file", e);
        } catch (Exception e) {
            LOG.error("Exception while running DroidPerm", e);
        }
    }

    @Nullable
    @Override
    public CommonProblemDescriptor[] checkElement(@NotNull RefEntity refEntity, @NotNull AnalysisScope scope,
                                                  @NotNull InspectionManager manager,
                                                  @NotNull GlobalInspectionContext globalContext) {
        //todo log an error if not all JaxbCallback objects produced by a DroidPerm execution were detected
        // by this analysis
        if (refEntity instanceof RefMethod) {
            RefMethod refMethod = (RefMethod) refEntity;
            JaxbCallback callback = DroidPermDataUtil.getCallbackFor(refMethod, droidPermData);

            if (callback != null) {
                PsiModifierListOwner methodElement = refMethod.getElement();
                if (methodElement instanceof PsiMethod) {
                    PsiMethod psiMethod = (PsiMethod) methodElement;
                    return checkCallbackWithEntries(psiMethod, callback, manager);
                } else {
                    LOG.error("RefMethod for " + callback + " not resolved to PsiMethod");
                }
            }
        }
        return null;
    }

    private CommonProblemDescriptor[] checkCallbackWithEntries(PsiMethod psiMethod, JaxbCallback callback,
                                                               InspectionManager manager) {
        List<CommonProblemDescriptor> result = new ArrayList<>();
        //todo now only top-level statements are supported.
        @SuppressWarnings("ConstantConditions")
        PsiStatement[] psiStmts = psiMethod.getBody().getStatements();
        int psiStmtIndex = 0;

        for (JaxbStmt jaxbStmt : callback.getStmts()) {
            if (jaxbStmt.allGuarded()) {
                continue;
            }

            int line = jaxbStmt.getLine();
            TextRange dpLineRange = lineToTextRange(line, psiMethod);
            while (psiStmtIndex < psiStmts.length && !psiStmts[psiStmtIndex].getTextRange().intersects(dpLineRange)) {
                psiStmtIndex++;
            }
            if (psiStmtIndex == psiStmts.length) {
                LOG.error("PSI code not found for: " + jaxbStmt);
            } else {
                PsiStatement psiStatement = psiStmts[psiStmtIndex];
                //todo restrict warning range to the method call specified in JaxbStmt, not the whole stmt.
                result.add(manager.createProblemDescriptor(
                        psiStatement, ALERT_MESSAGE + jaxbStmt.getUncheckedPermissions(), true,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false,
                        new PermissionGuardQuickFix(psiStatement, jaxbStmt)));
            }
        }

        return result.toArray(new CommonProblemDescriptor[0]);
    }

    private TextRange lineToTextRange(int droidPermLine, PsiMethod psiMethod) {
        PsiDocumentManager docManager = PsiDocumentManager.getInstance(psiMethod.getProject());
        Document document = docManager.getDocument(psiMethod.getContainingFile());
        assert document != null;
        int intellijLine = droidPermLine - 1; // in DroidPerm lines start from 1, in Intellij - from 0.

        return new TextRange(document.getLineStartOffset(intellijLine), document.getLineEndOffset(intellijLine));
    }
}
