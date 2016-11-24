package org.oregonstate.dcminer;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    private static final File CONFIG_FILE = new File("config.properties");
    private static final Path PERMISSION_FILE = Paths.get("PermissionList.txt");

    private static String corpusDir;
    static File resultFile;
    static List<String> permissions, methodPermissions, uriPermissions, mixedPermissions, storagePermissions;

    private static void loadConfig() {
        Properties prop = new Properties();
        try (InputStream stream = new FileInputStream(CONFIG_FILE)) {
            prop.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        corpusDir = prop.getProperty("corpus.dir");
        resultFile = new File(prop.getProperty("result.file"));
    }

    private static void loadPermissions() {
        permissions = new ArrayList<>();
        methodPermissions = new ArrayList<>();
        uriPermissions = new ArrayList<>();
        mixedPermissions = new ArrayList<>();
        storagePermissions = new ArrayList<>();
        List<String> currentList = null;

        for (String line : (Iterable<String>) () -> {
            try {
                return Files.lines(PERMISSION_FILE).filter(s -> !s.isEmpty()).iterator();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }) {
            switch (line) {
                case "#Method:":
                    currentList = methodPermissions;
                    break;
                case "#URI:":
                    currentList = uriPermissions;
                    break;
                case "#Mixed:":
                    currentList = mixedPermissions;
                    break;
                case "#Storage:":
                    currentList = storagePermissions;
                    break;
                default:
                    assert currentList != null;
                    permissions.add(line);
                    currentList.add(line);
            }
        }
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        loadConfig();
        loadPermissions();
        File[] files = new File(corpusDir).listFiles();
        if (files == null) {
            throw new RuntimeException("Error: " + corpusDir + " is not a directory");
        }

        try {
            FileUtils.writeStringToFile(resultFile, App.header(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (File file : files) {
            if (file.isDirectory()) {
                new App(file).analyze();
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        Date date = new Date(elapsedTime);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = formatter.format(date);
        System.out.println("Total time: " + time);
    }
}