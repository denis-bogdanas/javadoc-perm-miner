package org.oregonstate.jdminer;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

class Miner {
    public static void analyze(File androidSrcDir) throws IOException {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);

        Map options = JavaCore.getOptions();
        parser.setCompilerOptions(options);
        parser.setEnvironment(new String[0], new String[]{Main.androidSrcDir.toString()}, new String[]{"UTF-8"}, false);

        final String[] javaExt = new String[]{"java"};
        //todo use strems
        List<File> sourceFiles = (List<File>) FileUtils.listFiles(androidSrcDir, javaExt, true);
        for (File sourceFile : sourceFiles) {
            parser.setUnitName(sourceFile.getName());
            parser.setSource(FileUtils.readFileToString(sourceFile).toCharArray());
            final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            cu.accept(new MinerASTVisitor(sourceFile));
        }
    }

    /**
     * Inspired from http://stackoverflow.com/a/1724821/4182868
     */
    private static String getMethodFullName(IMethodBinding method) {
        StringBuilder name = new StringBuilder();
        name.append(method.getDeclaringClass().getQualifiedName()).append(": ");
        name.append(method.getReturnType().getQualifiedName());
        name.append(".");
        name.append(method.getName());
        name.append("(");

        String comma = "";
        for (ITypeBinding parameterType : method.getParameterTypes()) {
            name.append(comma);
            name.append(parameterType.getQualifiedName());
            comma = ", ";
        }
        name.append(")");

        return name.toString();
    }

    private static class MinerASTVisitor extends ASTVisitor {
        private final File sourceFile;
        private TypeDeclaration currentClass;

        public MinerASTVisitor(File sourceFile) {
            this.sourceFile = sourceFile;
        }

        @Override
        public boolean visit(TypeDeclaration type) {
            currentClass = type;
            return true;
        }

        public boolean visit(MethodDeclaration methodDec) {
            IMethodBinding method = methodDec.resolveBinding();
            String text =
                    method != null ? getMethodFullName(method)
                                   : "Binding not resolved for: " + sourceFile.getName() + ": " + methodDec.getName();
            System.out.println(text);
            return false;
        }
    }
}
