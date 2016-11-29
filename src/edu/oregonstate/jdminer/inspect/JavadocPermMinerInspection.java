package edu.oregonstate.jdminer.inspect;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.GlobalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptionsProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.oregonstate.droidperm.jaxb.JaxbUtil;
import org.oregonstate.droidperm.perm.miner.XmlPermDefMiner;
import org.oregonstate.droidperm.perm.miner.jaxb_out.*;
import org.oregonstate.droidperm.util.MyCollectors;
import org.oregonstate.droidperm.util.SortUtil;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JavadocPermMinerInspection extends GlobalInspectionTool {

    private static final Logger LOG = Logger.getInstance(JavadocPermMinerInspection.class);
    private static final File METADATA_XML = new File("d:/DroidPerm/droid-perm/config/perm-def-API-23.xml");
    private static final File XML_OUT = new File("d:/DroidPerm/javadoc-perm-miner/temp/javadoc-xml-out.xml");

    @Override
    public boolean isGraphNeeded() {
        return false;
    }

    @Override
    public void runInspection(@NotNull AnalysisScope scope, @NotNull InspectionManager manager,
                              @NotNull GlobalInspectionContext globalContext,
                              @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        try {
            List<PermissionDef> metadadaPermDefs = XmlPermDefMiner.load(METADATA_XML).getPermissionDefs();
            Multimap<String, PsiDocCommentOwner> permToCommentOwnersMap =
                    buildDocCommentOwners(globalContext.getProject());
            List<PermissionDef> collectedPermDef = buildPermissionDefs(permToCommentOwnersMap);
            List<PermissionDef> newPermDefs = new ArrayList<>(collectedPermDef);
            newPermDefs.removeAll(metadadaPermDefs);

            System.out.println("Total permission defs collected: " + collectedPermDef.size());
            System.out.println("New permission defs, after removing metadata defs: " + newPermDefs.size());

            savePermissionDefs(newPermDefs);
        } catch (JAXBException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private Multimap<String, PsiDocCommentOwner> buildDocCommentOwners(Project project) {
        return JPMData.wordMap.keySet().stream().collect(MyCollectors.toMultimapForCollection(
                ArrayListMultimap::create,
                perm -> perm,
                perm -> buildDocCommentOwners(project, JPMData.wordMap.get(perm))
        ));
    }

    private List<PsiDocCommentOwner> buildDocCommentOwners(Project project, String permWord) {
        GlobalSearchScope libScope = ProjectScope.getLibrariesScope(project);
        PsiFile[] filesWithPerm = CacheManager.SERVICE.getInstance(project)
                .getFilesWithWord(permWord, UsageSearchContext.IN_COMMENTS, libScope, true);

        System.out.println("\nClasses containing " + permWord + " in comments: " + filesWithPerm.length);
        int totalOccurrences = Arrays.stream(filesWithPerm)
                .mapToInt(file -> occurrencesInType(file, permWord, PsiComment.class)).sum();
        int javadocOccurrences = Arrays.stream(filesWithPerm)
                .mapToInt(file -> occurrencesInType(file, permWord, PsiDocComment.class)).sum();
        System.out.println(
                "Total occurrences in \n\tall comments: " + totalOccurrences + "\n\tjavadoc: " + javadocOccurrences);
        System.out.println("==============================================");
        List<PsiDocCommentOwner> result = new ArrayList<>();
        //Because we eventually check for classes, occurrences outside Java will be ignored.
        //noinspection unchecked
        Arrays.stream(filesWithPerm).flatMap(file -> PsiTreeUtil.getChildrenOfAnyType(file, PsiClass.class).stream())
                .forEach(psiClass -> {
                    boolean excluded = JPMUtil.startsWitAny(psiClass.getQualifiedName(), JPMData.classExclusionList);
                    String excludedStr = excluded ? ", excluded" : "";
                    System.out.println(psiClass.getQualifiedName()
                            + ", com:" + occurrencesInType(psiClass, permWord, PsiComment.class)
                            + ", javadoc:" + occurrencesInType(psiClass, permWord, PsiDocComment.class)
                            + excludedStr);
                    if (excluded) {
                        return;
                    }

                    Collection<PsiDocCommentOwner> childCommentOwners =
                            PsiTreeUtil.findChildrenOfType(psiClass, PsiDocCommentOwner.class);
                    List<PsiDocCommentOwner> commentOwners = new ArrayList<>(childCommentOwners.size() + 1);
                    commentOwners.add(psiClass);
                    commentOwners.addAll(childCommentOwners);

                    List<PsiDocCommentOwner> permCommentOwners = commentOwners.stream().filter(comOwner ->
                            comOwner.getDocComment() != null
                                    && comOwner.getDocComment().getText().contains(permWord)
                    ).collect(Collectors.toList());
                    for (PsiDocCommentOwner elem : permCommentOwners) {
                        //noinspection ConstantConditions
                        String docText = elem.getDocComment().getText();
                        boolean hidden = docText.contains("@hide");
                        int occurrences = occurrences(docText, permWord);
                        String hiddenText = hidden ? ", @hide" : "";
                        System.out.println("\t" + elem.getNode().getElementType() + ": " + elem.getName()
                                + ": " + occurrences + hiddenText);
                        if (occurrences > 0 && !hidden) {
                            result.add(elem);
                        }
                    }
                });
        System.out.println();
        return result;
    }

    private List<PermissionDef> buildPermissionDefs(Multimap<String, PsiDocCommentOwner> permToCommentOwnersMap) {
        Multimap<PsiDocCommentOwner, String> commentOwnerToPermMap
                = Multimaps.invertFrom(permToCommentOwnersMap, HashMultimap.create());
        return commentOwnerToPermMap.keySet().stream()
                .map(elem -> buildPermissionDef(elem, commentOwnerToPermMap.get(elem)))
                .sorted(SortUtil.permissionDefComparator)
                .collect(Collectors.toList());
    }

    private PermissionDef buildPermissionDef(PsiDocCommentOwner docCommentOwner, Collection<String> permColl) {
        PsiClass classOrSelf =
                docCommentOwner instanceof PsiClass ? (PsiClass) docCommentOwner : docCommentOwner.getContainingClass();
        assert classOrSelf != null;
        String className = XmlPermDefMiner.processInnerClasses(classOrSelf.getQualifiedName());
        String target;
        PermTargetKind targetKind;
        if (docCommentOwner instanceof PsiClass) {
            target = null;
            targetKind = PermTargetKind.Class;
        } else if (docCommentOwner instanceof PsiField) {
            target = XmlPermDefMiner.processInnerClasses(docCommentOwner.getName());
            targetKind = PermTargetKind.Field;
        } else if (docCommentOwner instanceof PsiMethod) {
            PsiMethod meth = (PsiMethod) docCommentOwner;
            StringBuilder sb = new StringBuilder();
            //noinspection ConstantConditions
            sb.append(meth.getReturnType().getCanonicalText()).append(" ");
            sb.append(meth.getName()).append("(");
            PsiParameter[] parameters = meth.getParameterList().getParameters();
            for (int i = 0; i < parameters.length; i++) {
                sb.append(parameters[i].getType().getCanonicalText());
                if (i < parameters.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            target = XmlPermDefMiner.cleanupSignature(sb.toString());
            targetKind = PermTargetKind.Method;
        } else {
            throw new RuntimeException("Invalid PsiElement type: " + docCommentOwner);
        }
        PermissionDef permDef = new PermissionDef();
        permDef.setClassName(className);
        permDef.setTarget(target);
        permDef.setTargetKind(targetKind);
        permDef.setPermissionRel(PermissionRel.AllOf);

        List<Permission> permissions = permColl.stream().sorted().map(perm -> new Permission(perm, null))
                .collect(Collectors.toList());
        permDef.setPermissions(permissions);
        return permDef;
    }

    private void savePermissionDefs(List<PermissionDef> permissionDefs) throws JAXBException {
        JaxbUtil.save(new PermissionDefList(permissionDefs), PermissionDefList.class, XML_OUT);
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
}
