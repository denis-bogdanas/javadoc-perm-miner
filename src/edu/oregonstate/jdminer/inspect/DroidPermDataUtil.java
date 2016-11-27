package edu.oregonstate.jdminer.inspect;

import com.intellij.codeInspection.reference.RefMethod;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import edu.oregonstate.jdminer.inspect.jaxb.JaxbCallback;

import java.util.List;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 4/29/2016.
 */
class DroidPermDataUtil {

    /**
     * @return The callback corresponding to this method or null if no corresponding callback was found.
     */
    static JaxbCallback getCallbackFor(RefMethod method, List<JaxbCallback> data) {
        if (data == null) return null;
        return data.stream().filter(callback -> sameMethod(method, callback)).findAny().orElseGet(() -> null);
    }

    private static boolean sameMethod(RefMethod method, JaxbCallback callback) {
        //todo support anonymous classes, for them getQualifiedName() is null
        assert method.getOwnerClass().getElement() != null;
        return callback.getDeclaringClass().equals(method.getOwnerClass().getElement().getQualifiedName())
                && getSigNoReturnType(callback).equals(getSubSignature(method));
    }

    private static String getSigNoReturnType(JaxbCallback callback) {
        int spaceIndex = callback.getSignature().indexOf(' ');
        return callback.getSignature().substring(spaceIndex + 1);
    }

    private static String getSubSignature(RefMethod refMethod) {
        if (!(refMethod.getElement() instanceof PsiMethod)) {
            return null;
        }
        PsiMethod method = (PsiMethod) refMethod.getElement();
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        boolean first = true;
        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (!first) {
                sb.append(",");
                first = false;
            }
            assert parameter.getTypeElement() != null;
            sb.append(parameter.getTypeElement().getType().getCanonicalText());
        }
        sb.append(")");
        return sb.toString();
    }
}
