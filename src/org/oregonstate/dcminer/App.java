package org.oregonstate.dcminer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class App {
    private static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
    private static final String GRADLE_FILE_NAME = "build.gradle";

    private File rootDir;
    private int numAndroid;
    int intentService;
    int asyncTask;
    int asyncTaskLoader;
    int thread;
    int handler;
    int subHandler;
    int nr_checkSelfPermission;
    int REF_runOnUiThread;
    int REF_handlerPost;
    int nrActivities;
    int nrFragments;
    private Integer sloc;
    private int declaredDangerousPerm;
    Set<String> checkedPerm = new TreeSet<>();//to keep them sorted
    int nrRtPermMethod, nrRtPermUri, nrRtPermMixed, nrRtPermStorage;

    Set<String> asyncTasks;
    Set<String> asyncTaskLoaders;
    Set<String> intents;
    List<File> manifestFiles;
    private List<File> gradleFiles;
    private Integer minSdkVersion;
    private Integer maxSdkVersion;


    App(File rootDir) {
        System.out.println("App: " + rootDir.getName());

        this.rootDir = rootDir;
        this.asyncTasks = new HashSet<>();
        this.asyncTaskLoaders = new HashSet<>();
        this.intents = new HashSet<>();
        asyncTasks.add("AsyncTask");
        asyncTaskLoaders.add("AsyncTaskLoader");
        intents.add("IntentService");
    }

    void analyze() {
        manifestFiles = (List<File>) FileUtils
                .listFiles(rootDir, new NameFileFilter(MANIFEST_FILE_NAME), TrueFileFilter.INSTANCE);
        numAndroid = manifestFiles.size();
        gradleFiles = (List<File>) FileUtils
                .listFiles(rootDir, new NameFileFilter(GRADLE_FILE_NAME), TrueFileFilter.INSTANCE);

        mineAllSdkVersions();
        mineDangerousPermissions();

        final String[] javaExt = new String[]{"java"};
        int temp = 0;
        int num = asyncTaskLoaders.size() + asyncTasks.size() + intents.size();
        while (num != temp) {
            temp = num;
            for (File manifest : manifestFiles) {
                try {
                    List<File> sourceFiles =
                            (List<File>) FileUtils.listFiles(manifest.getParentFile(), javaExt, true);
                    for (File sourceFile : sourceFiles) {
                        String sourceContent = FileUtils.readFileToString(sourceFile);
                        startPreVisitor(sourceContent);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            num = asyncTaskLoaders.size() + asyncTasks.size() + intents.size();
        }

        for (File manifest : manifestFiles) {
            try {
                List<File> sourceFiles = (List<File>) FileUtils.listFiles(manifest.getParentFile(), javaExt, true);
                for (File sourceFile : sourceFiles) {
                    startAsyncVisitor(FileUtils.readFileToString(sourceFile));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileUtils.writeStringToFile(Main.resultFile, toString(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mineDangerousPermissions() {
        for (File manifest : manifestFiles) {
            try {
                String content = FileUtils.readFileToString(manifest);
                for (String perm : Main.permissions) {
                    if (content.contains(perm)) {
                        declaredDangerousPerm++;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void mineAllSdkVersions() {
        List<File> apiVersionFiles = new ArrayList<>();
        apiVersionFiles.addAll(manifestFiles);
        apiVersionFiles.addAll(gradleFiles);

        for (File apiVersionFile : apiVersionFiles) {
            try {
                mineSdkVersion(apiVersionFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final Pattern MANIFEST_SDK_VERSION_PATTERN = Pattern.compile("android:targetSdkVersion=\"(\\d+)\"");
    private static final Pattern GRADLE_SDK_VERSION_PATTERN = Pattern.compile("targetSdkVersion (\\d+)");

    private void mineSdkVersion(File apiVersionFile) throws IOException {
        Pattern pattern = apiVersionFile.getName().equals(MANIFEST_FILE_NAME)
                          ? MANIFEST_SDK_VERSION_PATTERN : GRADLE_SDK_VERSION_PATTERN;
        Matcher matcher = pattern.matcher(FileUtils.readFileToString(apiVersionFile));
        if (matcher.find()) {
            int apiVersion = Integer.parseInt(matcher.group(1));
            if (minSdkVersion == null) {
                minSdkVersion = apiVersion;
                maxSdkVersion = apiVersion;
            } else {
                if (apiVersion < minSdkVersion) {
                    minSdkVersion = apiVersion;
                }
                if (apiVersion > maxSdkVersion) {
                    maxSdkVersion = apiVersion;
                }
            }
        }
    }

    private void startPreVisitor(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {
            public boolean visit(TypeDeclaration node) {
                Type superclassType = node.getSuperclassType();
                if (superclassType != null) {
                    String superclassName = superclassType.toString();
                    String className = node.getName().toString();

                    for (String asyncTask : asyncTasks.toArray(new String[asyncTasks.size()])) {
                        if (superclassName.contains(asyncTask + "<")) {
                            asyncTasks.add(className);
                        }
                    }
                    for (String asyncTaskLoader : asyncTaskLoaders.toArray(new String[asyncTaskLoaders.size()])) {
                        if (superclassName.contains(asyncTaskLoader + "<")) {
                            asyncTaskLoaders.add(className);
                        }
                    }
                    for (String intentService : intents.toArray(new String[intents.size()])) {
                        if (superclassName.equalsIgnoreCase(intentService)) {
                            intents.add(className);
                        }
                    }
                }
                return true;
            }
        });
    }

    private void startAsyncVisitor(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(source.toCharArray());
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new AsyncASTVisitor(this));
    }

    static String header() {
        return "App, SLOC, API level, Activity, Fragment, Apps, AsyncTask, AsyncTaskLoader, IntentService, Thread, "
                + "runOnUiThread(), handler.post(), Handler, Sub-handler, "
                + "checkSelfPermission(), Meth ChPerm, URI ChPerm, Mixed ChPerm, Storage ChPerm,"
                + "Decl Perm, Checked Perm\n";
    }

    public String toString() {
        return rootDir.getName().replace('+', '/') + ","
                + sloc + "," + getApiInterval() + ","
                + nrActivities + "," + nrFragments + "," + numAndroid + "," + asyncTask + "," + asyncTaskLoader + ","
                + intentService + "," + thread + ", "
                + REF_runOnUiThread + ", " + REF_handlerPost + ", "
                + handler + "," + subHandler + ","
                + nr_checkSelfPermission + ","
                + nrRtPermMethod + "," + nrRtPermUri + "," + nrRtPermMixed + "," + nrRtPermStorage + ","
                + declaredDangerousPerm + "," + prettyPrintList(checkedPerm) + "\n";
    }

    private CharSequence prettyPrintList(Set<String> checkedPerm) {
        StringBuilder sb = new StringBuilder();
        checkedPerm.forEach(s -> sb.append(s).append(" "));
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb;
    }

    @SuppressWarnings("NumberEquality")
    private String getApiInterval() {
        return minSdkVersion != null
               ? (maxSdkVersion != minSdkVersion ? minSdkVersion + "--" + maxSdkVersion : minSdkVersion + "")
               : "";
    }
}
