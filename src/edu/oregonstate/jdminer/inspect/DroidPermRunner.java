package edu.oregonstate.jdminer.inspect;

import com.google.common.base.Strings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Denis Bogdanas <bogdanad@oregonstate.edu> Created on 4/29/2016.
 */
public class DroidPermRunner {
    private static final String[] EXTRA_OPTS = new String[]{"--pathalgo", "CONTEXTSENSITIVE", "--notaintwrapper",
            "--cgalgo", "GEOM", "--taint-analysis-enabled", "false"};

    /**
     * @param module - analyzed module (should build to an apk file).
     * @return Temp xml file with results
     */
    static File run(Module module) throws IOException, DroidPermException {
        Path apkFile = getApkPath(module);
        File xmlFile = File.createTempFile("droid-perm", "xml");
        xmlFile.deleteOnExit();

        List<String> command = new ArrayList<>();
        String droidPermHome = getDroidPermHomeFromSettings();
        String droidPermClasspath = droidPermHome + "/droid-perm.jar";
        String androidClasspath = droidPermHome + "/android-23-with-util-io.zip";
        command.addAll(Arrays.asList("java", "-jar", droidPermClasspath, apkFile.toString(), androidClasspath,
                "--xml-out", xmlFile.getAbsolutePath()));
        command.addAll(Arrays.asList(EXTRA_OPTS));

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(new File(droidPermHome))
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = builder.start();
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            process.destroy();
            throw new DroidPermException(e);
        }

        if (exitCode != 0) {
            throw new DroidPermException("DroidPerm terminated with exit code " + exitCode);
        }
        return xmlFile;
    }

    @NotNull
    private static Path getApkPath(Module module) throws IOException, DroidPermException {
        Path moduleDir = IntellijUtil.getModulePath(module);
        List<Path> matches = new ArrayList<>();
        Files.walk(moduleDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("debug.apk"))
                .forEach(matches::add);
        if (matches.isEmpty()) {
            throw new FileNotFoundException("no debug.apk files");
        }
        if (matches.size() > 1) {
            String[] paths = matches.stream().map(m -> m.getFileName().toString()).toArray(String[]::new);
            throw new DroidPermException("multiple debug.apk files found:\n\t" + StringUtil.join(paths, ",\n\t"));
        }
        Path apkFile = matches.get(0);
        if (!apkFile.toFile().exists()) {
            throw new FileNotFoundException(apkFile.toAbsolutePath().toString());
        }
        return apkFile;
    }

    /**
     * The work dir for DroidPerm, with all its config files, droid-perm.jar and android classpath.
     */
    @NotNull
    private static String getDroidPermHomeFromEnv() {
        String droidPermHome = System.getenv().get("DROID_PERM_HOME");
        if (!Strings.isNullOrEmpty(droidPermHome)) {
            return droidPermHome;
        } else {
            throw new RuntimeException(
                    "Environment variable DROID_PERM_HOME was not set.");
        }
    }

    private static String getDroidPermHomeFromSettings() {
        String droidPermHome = null;
        if (Strings.isNullOrEmpty(droidPermHome)) {
            throw new RuntimeException(
                    "DroidPerm home directory was not set (Settings -> Other settings -> DroidPermPlugin)");
        }
        return droidPermHome;
    }
}
