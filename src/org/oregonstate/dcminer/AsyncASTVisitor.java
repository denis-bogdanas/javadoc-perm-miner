package org.oregonstate.dcminer;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.function.Predicate;

class AsyncASTVisitor extends ASTVisitor {
    private final Set<String> asyncTasks;
    private final Set<String> asyncTaskLoaders;
    private final Set<String> intents;
    private final App app;

    AsyncASTVisitor(App app) {
        this.asyncTaskLoaders = app.asyncTaskLoaders;
        this.asyncTasks = app.asyncTasks;
        this.intents = app.intents;
        this.app = app;
    }

    public boolean visit(ClassInstanceCreation node) {
        Type type = node.getType();
        if (type.isSimpleType()) {
            SimpleType simpleType = (SimpleType) type;
            String newInstanceClass = simpleType.getName().toString();
            asyncTasks.stream().filter(newInstanceClass::equalsIgnoreCase).forEach(asyncTask -> app.asyncTask++);
            asyncTaskLoaders.stream().filter(newInstanceClass::equalsIgnoreCase)
                    .forEach(asyncTaskLoader -> app.asyncTaskLoader++);
            if (newInstanceClass.equalsIgnoreCase("Thread")) {
                app.thread++;
            }
            mineHandler(newInstanceClass);
            mineIntentService(node, newInstanceClass);
        }
        return true;
    }

    private void mineHandler(String name) {
        if (name.equals("Handler")) {
            app.handler++;

            //a more general pattern - we'll consider everything ending with Handler being a sub-handler.
        } else if (name.endsWith("Handler")) {
            app.subHandler++;
        }
    }

    private void mineIntentService(ClassInstanceCreation node, String name) {
        if (name.equalsIgnoreCase("Intent")) {
            int paramSize = node.arguments().size();
            if (paramSize == 2 || paramSize == 4) {
                Object param;
                if (paramSize == 2) {
                    param = node.arguments().get(1);
                } else { // ==4
                    param = node.arguments().get(3);
                }

                if (param instanceof TypeLiteral) {
                    String temp = ((TypeLiteral) param).getType().toString();
                    intents.stream().filter(temp::equalsIgnoreCase).forEach(intentService -> app.intentService++);
                }
            }

            if (paramSize == 1 || paramSize == 2) {
                Object param;
                if (paramSize == 1) {
                    param = node.arguments().get(0);
                } else { // == 2
                    param = node.arguments().get(1);
                }

                if (param instanceof StringLiteral) {
                    String temp = ((StringLiteral) param).getLiteralValue();

                    boolean isContain = false;
                    for (File manifest : app.manifestFiles) {
                        try {
                            String xmlFile = FileUtils.readFileToString(manifest);
                            if (xmlFile.contains(temp)) {
                                isContain = true;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (isContain) {
                        app.intentService++;
                    }

                }
            }
        }
    }

    /**
     * Mine instances of Activity and Fragment. Contains some inaccuracies.
     */
    public boolean visit(TypeDeclaration node) {
        Type superclassType = node.getSuperclassType();
        if (superclassType != null) {
            String superclassName = superclassType.toString();
            if (superclassName.endsWith("Activity") || superclassName.contains("Activity<")) {
                app.nrActivities++;
            } else if (superclassName.endsWith("Fragment") || superclassName.contains("Fragment<")) {
                app.nrFragments++;
            }
        }
        return true;
    }

    public boolean visit(MethodInvocation node) {
        mineRunOnUiThread(node);
        mineCheckSelfPermission(node);
        return true;
    }

    private void mineCheckSelfPermission(MethodInvocation node) {
        if (node.getName().getIdentifier().equals("checkSelfPermission")) {
            app.nr_checkSelfPermission++;
        }
    }

    /**
     * Mine code patterns amenable to the refactoring Thread -> AsyncTask.
     *
     * @param methodInvocation - the invocation of activity.REF_runOnUiThread() or handler.post().
     */
    private void mineRunOnUiThread(MethodInvocation methodInvocation) {
        String methodName = methodInvocation.getName().getIdentifier();
        if (methodName.equals("runOnUiThread")) {
            if (getParentWith(methodInvocation, this::isNewThread) != null) {
                app.REF_runOnUiThread++;
            }
        } else if (methodName.equals("post") && methodInvocation.getExpression() instanceof SimpleName) {
            SimpleName qualifier = (SimpleName) methodInvocation.getExpression();
            if (qualifier.getIdentifier().toLowerCase().contains("handler")) {
                if (getParentWith(methodInvocation, this::isNewThread) != null) {
                    app.REF_handlerPost++;
                }
            }
        }
    }

    /**
     * Match either new Thread() {...} or new Thread(new Runnable(){...})
     */
    private boolean isNewThread(ASTNode node) {
        if (node instanceof AnonymousClassDeclaration && node.getParent() instanceof ClassInstanceCreation) {
            ClassInstanceCreation anonNewInstance = (ClassInstanceCreation) node.getParent();
            if (anonNewInstance.getType() instanceof SimpleType) {
                String anonType = ((SimpleType) anonNewInstance.getType()).getName().toString();
                if (anonType.equals("Runnable")) {
                    ASTNode runnableParent = anonNewInstance.getParent();
                    if (runnableParent instanceof ClassInstanceCreation) {
                        ClassInstanceCreation newThreadCandidate = (ClassInstanceCreation) runnableParent;
                        if (newThreadCandidate.getType() instanceof SimpleType &&
                                ((SimpleType) anonNewInstance.getType()).getName().toString().equals("Thread")) {
                            return true;
                        }
                    }
                } else if (anonType.equals("Thread")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ASTNode getParentWith(ASTNode node, Predicate<ASTNode> predicate) {
        while (node != null) {
            if (predicate.test(node)) {
                return node;
            } else {
                node = node.getParent();
            }
        }
        return null;
    }


    @Override
    public boolean visit(FieldAccess node) {
        analyzeField(node.getExpression(), node.getName().getIdentifier());
        return false;
    }

    @Override
    public boolean visit(QualifiedName name) {
        analyzeField(name.getQualifier(), name.getName().getIdentifier());
        return false;
    }

    /*@Override
    public boolean visit(SimpleName name) {
        analyzeField(name.getIdentifier());
        return false;
    }*/

    private void analyzeField(Expression qualifier, String fieldName) {
        if (!((qualifier instanceof Name && ((Name) qualifier).getFullyQualifiedName().endsWith("permission"))
                || (qualifier instanceof FieldAccess
                && ((FieldAccess) qualifier).getName().getIdentifier().equals("permission")))) {
            return;
        }
        if (Main.permissions.contains(fieldName)) {
            app.checkedPerm.add(fieldName);
        }
        if (Main.methodPermissions.contains(fieldName)) {
            app.nrRtPermMethod++;
        }
        if (Main.uriPermissions.contains(fieldName)) {
            app.nrRtPermUri++;
        }
        if (Main.mixedPermissions.contains(fieldName)) {
            app.nrRtPermMixed++;
        }
        if (Main.storagePermissions.contains(fieldName)) {
            app.nrRtPermStorage++;
        }
    }
}
