package edu.oregonstate.jdminer.inspect;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

class InspectionPsiUtil {
    static PsiClass createPsiClass(String qualifiedName, Project project) {
        final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        final GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        return psiFacade.findClass(qualifiedName, searchScope);
    }

    static boolean isAbstractClass(PsiClass aClass) {
        return aClass != null && aClass.getModifierList() != null &&
                aClass.getModifierList().hasModifierProperty("abstract");
    }

    static boolean isStaticMethod(PsiMethod method) {
        return hasMethodModifier(method, "static");
    }

    static boolean isPublicMethod(PsiMethod method) {
        return hasMethodModifier(method, "public");
    }

    private static boolean hasMethodModifier(PsiMethod method, String aStatic) {
        return method != null &&
                method.getModifierList().hasModifierProperty(aStatic);
    }

}
