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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.oregonstate.droidperm.jaxb.JaxbUtil;
import org.oregonstate.droidperm.perm.miner.XmlPermDefMiner;
import org.oregonstate.droidperm.perm.miner.jaxb_out.*;
import org.oregonstate.droidperm.util.MyCollectors;
import org.oregonstate.droidperm.util.SortUtil;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavadocPermMinerInspection extends GlobalInspectionTool {

    private static final Logger LOG = Logger.getInstance(JavadocPermMinerInspection.class);
    private static final File METADATA_XML = new File("d:/DroidPerm/droid-perm/config/perm-def-API-23.xml");
    private static final File XML_OUT = new File("d:/DroidPerm/javadoc-perm-miner/temp/javadoc-xml-out.xml");
    private static final File MANUAL_XML_OUT = new File("d:/DroidPerm/javadoc-perm-miner/temp/manual-xml-out.xml");
    private static final File PARAMETRIC_SENS_OUT =
            new File("d:/DroidPerm/javadoc-perm-miner/temp/parametric-sens-out.xml");

    @Override
    public boolean isGraphNeeded() {
        return false;
    }

    @Override
    public void runInspection(@NotNull AnalysisScope scope, @NotNull InspectionManager manager,
                              @NotNull GlobalInspectionContext globalContext,
                              @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        try {
            List<PermissionDef> metadadaPermDefs =
                    JaxbUtil.load(PermissionDefList.class, METADATA_XML).getPermissionDefs();
            List<PermissionDef> excludedPermDefs =
                    JaxbUtil.load(PermissionDefList.class, getClass().getResource("ExcludedPermDef.xml"))
                            .getPermissionDefs();
            Multimap<String, PsiDocCommentOwner> permToCommentOwnersMap =
                    buildDocCommentOwners(globalContext.getProject());
            List<PermissionDef> collectedPermDef = buildPermissionDefs(permToCommentOwnersMap);
            List<PermissionDef> newPermDefs = new ArrayList<>(collectedPermDef);
            List<PermissionDef> classPermDefsCoveredByCustomDefs = JPMData.getClassPermDefsCoveredByCustomDefs();
            newPermDefs.removeAll(metadadaPermDefs);
            newPermDefs.removeAll(excludedPermDefs);
            newPermDefs.removeAll(classPermDefsCoveredByCustomDefs);

            System.out.println("Total permission defs collected: " + collectedPermDef.size());
            System.out.println("New permission defs, after removing metadata and excluded defs: " + newPermDefs.size());

            List<PermissionDef> customPermDefs =
                    buildCustomPermDefs(JPMData.classCustomPerm, globalContext.getProject(), this::buildPermissionDef);
            customPermDefs.removeAll(newPermDefs); //should not change results
            newPermDefs.addAll(customPermDefs);
            System.out.println("Final perm defs, after adding custom permissions: " + newPermDefs.size());

            savePermissionDefs(newPermDefs, XML_OUT);

            List<PermissionDef> manualPermDefs =
                    buildCustomPermDefs(JPMData.manualPerm, globalContext.getProject(), this::buildPermissionDef);
            System.out.println("Manual perm defs: " + manualPermDefs.size());
            savePermissionDefs(manualPermDefs, MANUAL_XML_OUT);

            List<ParametricSensDef> parametricSensDefs =
                    buildCustomPermDefs(JPMData.parametricPerm, globalContext.getProject(),
                            this::buildParametricSensDef);
            System.out.println("Parametric sens defs: " + parametricSensDefs.size());
            JaxbUtil.save(new PermissionDefList(Collections.emptyList(), Collections.emptyList(), parametricSensDefs),
                    PermissionDefList.class, PARAMETRIC_SENS_OUT);
        } catch (JAXBException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private <T> List<T> buildCustomPermDefs(List<JPMData.CustomPermDef> customPermRawData,
                                            Project project,
                                            BiFunction<PsiDocCommentOwner, Collection<String>, T> permDefBuilder) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope libScope = ProjectScope.getLibrariesScope(project);

        List<T> result = new ArrayList<>();
        for (JPMData.CustomPermDef customPermDef : customPermRawData) {
            String className = customPermDef.className;
            if (customPermDef.permList == null) {
                continue;//class has to be ignored
            }
            PsiClass psiClass = psiFacade.findClass(className, libScope);
            if (psiClass == null) {
                LOG.error("Custom class not found: " + className);
                continue;
            }

            if (customPermDef.memberNames != null) {
                customPermDef.memberNames.stream()
                        .flatMap(memberName -> {
                            PsiDocCommentOwner[] members = memberName.equals("<init>")
                                                           ? psiClass.getConstructors()
                                                           : psiClass.findMethodsByName(memberName, false);
                            //if this is not a method then maybe it's a field
                            if (members.length == 0) {
                                PsiField field = psiClass.findFieldByName(memberName, false);
                                if (field != null) {
                                    members = new PsiDocCommentOwner[]{field};
                                } else {
                                    LOG.error("Custom member not found: " + className + "." + memberName);
                                }
                            }
                            return Arrays.stream(members);
                        })
                        .filter(member -> !isHidden(member, getText(member)))
                        .map((PsiDocCommentOwner member) -> permDefBuilder.apply(member, customPermDef.permList))
                        .forEach(result::add);
            }
            if (customPermDef.includeAllMethods) {
                Arrays.stream(psiClass.getMethods()).filter(psiMeth -> !isHidden(psiMeth, getText(psiMeth)))
                        .map(psiMeth -> permDefBuilder.apply(psiMeth, customPermDef.permList))
                        .forEach(result::add);
            }
            if (customPermDef.includeUriFields) {
                Stream<PsiClass> classes = customPermDef.includeInnerClassesForUri
                                           ? Stream.concat(
                        Stream.of(psiClass),
                        PsiTreeUtil.findChildrenOfType(psiClass, PsiClass.class).stream())
                                           : Stream.of(psiClass);

                classes.flatMap(currentClass -> Stream.of(currentClass.getFields()))
                        .filter(psiField -> !isHidden(psiField, getText(psiField)))
                        .filter(psiField -> psiField.getType().getCanonicalText().equals("android.net.Uri"))
                        .map(psiField -> permDefBuilder.apply(psiField, customPermDef.permList))
                        .forEach(result::add);
            }
        }
        return result;
    }

    private String getText(PsiDocCommentOwner commOwner) {
        return commOwner.getDocComment() != null ? commOwner.getDocComment().getText() : "";
    }

    private Multimap<String, PsiDocCommentOwner> buildDocCommentOwners(Project project) {
        return JPMData.wordMap.keySet().stream().collect(MyCollectors.toMultimapForCollection(
                ArrayListMultimap::create,
                perm -> perm,
                perm -> buildDocCommentOwners(project, perm)
        ));
    }

    private List<PsiDocCommentOwner> buildDocCommentOwners(Project project, String perm) {
        String permWord = JPMData.wordMap.get(perm);
        GlobalSearchScope libScope = ProjectScope.getLibrariesScope(project);
        PsiFile[] filesWithPerm = CacheManager.SERVICE.getInstance(project)
                .getFilesWithWord(permWord, UsageSearchContext.IN_COMMENTS, libScope, true);

        System.out.println("\nClasses containing " + permWord + " in comments: " + filesWithPerm.length);
        int totalOccurrences = Arrays.stream(filesWithPerm)
                .mapToInt(file -> occurrencesInType(file, perm, PsiComment.class)).sum();
        int javadocOccurrences = Arrays.stream(filesWithPerm)
                .mapToInt(file -> occurrencesInType(file, perm, PsiDocComment.class)).sum();
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
                            + ", com:" + occurrencesInType(psiClass, perm, PsiComment.class)
                            + ", javadoc:" + occurrencesInType(psiClass, perm, PsiDocComment.class)
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
                                    && containsPerm(comOwner.getDocComment().getText(), perm)
                    ).collect(Collectors.toList());
                    for (PsiDocCommentOwner elem : permCommentOwners) {
                        //noinspection ConstantConditions
                        String docText = elem.getDocComment().getText();
                        boolean hidden = isHidden(elem, docText);
                        int occurrences = occurrences(docText, perm);
                        String hiddenText = hidden ? ", hidden" : "";
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

    private boolean isHidden(PsiDocCommentOwner elem, String docText) {
        assert elem.getModifierList() != null;
        return docText.contains("@hide") || docText.contains("@removed")
                || !elem.getModifierList().hasModifierProperty(PsiModifier.PUBLIC);
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
        Pair<String, PermTargetKind> targetAndKind = getTargetAndKind(docCommentOwner);
        List<Permission> permissions = permColl.stream().sorted().map(perm -> new Permission(perm, null))
                .collect(Collectors.toList());
        PermissionDef permDef = new PermissionDef(className, targetAndKind.first, targetAndKind.second, permissions);
        permDef.setPermissionRel(PermissionRel.AllOf);
        permDef.setComment(buildComment(docCommentOwner.getDocComment(), permissions));
        permDef.setConditional(true);
        return permDef;
    }

    private ParametricSensDef buildParametricSensDef(PsiDocCommentOwner docCommentOwner, @SuppressWarnings("unused")
            Collection<String> permColl) {
        PsiClass classOrSelf =
                docCommentOwner instanceof PsiClass ? (PsiClass) docCommentOwner : docCommentOwner.getContainingClass();
        assert classOrSelf != null;
        String className = XmlPermDefMiner.processInnerClasses(classOrSelf.getQualifiedName());
        Pair<String, PermTargetKind> targetAndKind = getTargetAndKind(docCommentOwner);
        return new ParametricSensDef(className, targetAndKind.first);
    }

    @NotNull
    private Pair<String, PermTargetKind> getTargetAndKind(PsiMember member) {
        String target;
        PermTargetKind targetKind;
        if (member instanceof PsiClass) {
            target = null;
            targetKind = PermTargetKind.Class;
        } else if (member instanceof PsiField) {
            target = XmlPermDefMiner.processInnerClasses(member.getName());
            targetKind = PermTargetKind.Field;
        } else if (member instanceof PsiMethod) {
            PsiMethod meth = (PsiMethod) member;
            StringBuilder sb = new StringBuilder();
            //noinspection ConstantConditions
            String returnTypeStr = meth.isConstructor() ? "" : meth.getReturnType().getCanonicalText() + " ";
            sb.append(returnTypeStr);
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

            //replacing constructor name with jimple equivalent
            if (meth.isConstructor()) {
                target = "void <init>" + target.substring(target.indexOf('('));
            }

            targetKind = PermTargetKind.Method;
        } else {
            throw new RuntimeException("Invalid PsiElement type: " + member);
        }
        return Pair.create(target, targetKind);
    }

    private String buildComment(PsiDocComment docComment, List<Permission> permissions) {
        if (docComment == null) {
            return null;
        }
        final int contextLen = 150;
        String docText = docComment.getText();
        List<Integer> indexes = buildOccurrenceIndexes(docText, permissions);
        List<TextRange> ranges = buildOccurrenceRanges(indexes, contextLen, docText.length());
        List<TextRange> fileRanges = expandToFileRangesFullLines(docComment, ranges);
        if (fileRanges.isEmpty()) {
            return null;
        }

        //Concatenate text in the ranges
        String fileText = docComment.getContainingFile().getText();
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (TextRange range : fileRanges) {
            sb.append(range.substring(fileText)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Collect indexes of positions where permissions are referred, sorted ascendingly.
     */
    @NotNull
    private List<Integer> buildOccurrenceIndexes(String docText, List<Permission> permissions) {
        List<Integer> indexes = new ArrayList<>();
        permissions.forEach(perm -> {
            String permName = StringUtils.substringAfterLast(perm.getName(), ".");
            int index = docText.indexOf(permName);
            while (index != -1) {
                indexes.add(index);
                index = docText.indexOf(permName, index + 1);
            }
        });
        Collections.sort(indexes);
        return indexes;
    }

    /**
     * Convert the indexes into intervals of text that should be included in the final comment.
     * By default for each index I'll consider [index + contextLen, index - contextLen].
     * If 2 indexes are less than 2*contextLen appart, they will be joined into one interval.
     */
    @NotNull
    private List<TextRange> buildOccurrenceRanges(List<Integer> indexes, int contextLen, int docLength) {
        List<TextRange> ranges = new ArrayList<>();
        TextRange current = null;
        for (int index : indexes) {
            TextRange indexRange = buildTextRange(index, contextLen, docLength);
            if (current == null) {
                current = indexRange;
            } else {
                if (current.intersects(indexRange)) {
                    //combine the 2 ranges
                    current = current.union(indexRange);
                } else {
                    //save the old range and make the new one current
                    ranges.add(current);
                    current = indexRange;
                }
            }
        }
        if (current != null) {
            ranges.add(current);
        }
        return ranges;
    }

    @NotNull
    private TextRange buildTextRange(int index, int contextLen, int docLength) {
        int start = index < contextLen ? 0 : index - contextLen;
        int end = index > docLength - contextLen ? docLength : index + contextLen;
        return new TextRange(start, end);
    }

    /**
     * Convert ranges in comment into ranges in file, expanding them to include full lines.
     */
    private List<TextRange> expandToFileRangesFullLines(PsiDocComment docComment, List<TextRange> ranges) {
        PsiDocumentManager docManager = PsiDocumentManager.getInstance(docComment.getProject());
        Document document = docManager.getDocument(docComment.getContainingFile());
        assert document != null;
        int docStartOffset = docComment.getTextRange().getStartOffset();
        return ranges.stream().map(range -> {
            int newStartOffset
                    = document.getLineStartOffset(document.getLineNumber(docStartOffset + range.getStartOffset()));
            int newEndOffset = document.getLineEndOffset(document.getLineNumber(docStartOffset + range.getEndOffset()));
            return new TextRange(newStartOffset, newEndOffset);
        }).collect(Collectors.toList());
    }

    private void savePermissionDefs(List<PermissionDef> permissionDefs, File file) throws JAXBException {
        JaxbUtil.save(new PermissionDefList(permissionDefs), PermissionDefList.class, file);
    }

    private static int occurrencesInType(PsiElement elem, String perm, Class<? extends PsiElement> psiClass) {
        //noinspection unchecked
        return PsiTreeUtil.findChildrenOfAnyType(elem, psiClass).stream()
                .mapToInt(comm -> occurrences(comm.getText(), perm)).sum();
    }

    private static int occurrences(String str, String perm) {
        return JPMData.regexMap.containsKey(perm) ? occurrencesRegex(str, JPMData.regexMap.get(perm))
                                                  : occurrencesRegular(str, JPMData.wordMap.get(perm));
    }

    private static int occurrencesRegular(String str, String permWord) {
        int occurrences = 0;
        int index = str.indexOf(permWord);
        while (index != -1) {
            occurrences++;
            index = str.indexOf(permWord, index + 1);
        }
        return occurrences;
    }

    private static int occurrencesRegex(String str, String permRegex) {
        Pattern pattern = Pattern.compile(permRegex);
        Matcher matcher = pattern.matcher(str);
        int occurrences = 0;
        while (matcher.find()) {
            occurrences++;
        }
        return occurrences;
    }

    private boolean containsPerm(String text, String perm) {
        if (JPMData.regexMap.containsKey(perm)) {
            Pattern pattern = Pattern.compile(JPMData.regexMap.get(perm));
            Matcher matcher = pattern.matcher(text);
            return matcher.find();
        } else {
            return text.contains(JPMData.wordMap.get(perm));
        }
    }
}
