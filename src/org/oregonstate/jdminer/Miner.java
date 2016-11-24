package org.oregonstate.jdminer;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

class Miner {
    public static void analyze(File androidSrcDir) throws IOException {
        final String[] javaExt = new String[]{"java"};
        List<File> sourceFiles = (List<File>) FileUtils.listFiles(androidSrcDir, javaExt, true);
        for (File sourceFile : sourceFiles) {
            ASTParser parser = ASTParser.newParser(AST.JLS8);
            parser.setResolveBindings(true);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setSource(FileUtils.readFileToString(sourceFile).toCharArray());
            final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
            cu.accept(new MinerASTVisitor());
        }
    }

    /**
     * Inspired from http://stackoverflow.com/a/1724821/4182868
     */
    private static String getMethodFullName(IMethodBinding method) {
        StringBuilder name = new StringBuilder();
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
        public boolean visit(MethodDeclaration methodDec) {
            System.out.println(getMethodFullName(methodDec.resolveBinding()));
            return false;
        }
    }
}
