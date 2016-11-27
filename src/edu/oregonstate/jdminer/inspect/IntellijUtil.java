package edu.oregonstate.jdminer.inspect;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 5/10/2016.
 */
public class IntellijUtil {

    public static Path getModulePath(Module module) {
        //noinspection ConstantConditions
        return Paths.get(module.getOptionValue(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY));
    }

    private static Path getScopePath(AnalysisScope scope) throws DroidPermException {
        if (!(scope.getScopeType() == AnalysisScope.DIRECTORY)) {
            return null;
        }
        Class<AnalysisScope> scopeClazz = AnalysisScope.class;
        try {
            Field myElementField = scopeClazz.getDeclaredField("myElement");
            myElementField.setAccessible(true);
            PsiElement scopePsiElement = (PsiElement) myElementField.get(scope);

            if (scopePsiElement != null) {
                String canonicalPath = ((PsiDirectory) scopePsiElement).getVirtualFile().getCanonicalPath();
                return canonicalPath != null ? Paths.get(canonicalPath) : null;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new DroidPermException(e);
        }
    }

    /**
     * A ton of hacks. No guarantee will work in all cases/all machines.
     */
    private static boolean scopeContainsModule(AnalysisScope scope, Module module) throws DroidPermException {
        if (scope.containsModule(module)) {
            return true;
        }
        Path modulePath = getModulePath(module);
        Path scopePath = getScopePath(scope);
        return modulePath != null && scopePath != null &&
                modulePath.toAbsolutePath().equals(scopePath.toAbsolutePath());
    }

    public static Module getModule(AnalysisScope scope, Project project) throws DroidPermException {
        ModuleManager man = ModuleManager.getInstance(project);
        for (Module module : man.getModules()) {
            if (scopeContainsModule(scope, module)) {
                return module;
            }
        }
        return null;
    }
}
