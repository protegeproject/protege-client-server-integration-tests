package org.protege.editor.owl.integration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public abstract class ProjectBaseTest extends BaseTest {

    private static final String defaultDataDirectory = "root";
    private static final String defaultSnapshotDirectory = "."; // current location

    public ProjectBaseTest() {
        super();
    }

    protected void removeDataDirectory() {
        File dataDir = new File(getDataDirectory());
        if (dataDir.exists()) {
            try {
                FileUtils.deleteDirectory(dataDir);
            } catch (IOException e) {
                System.err.println("Failed to remove data directory: "
                        + dataDir.getAbsolutePath()
                        + ". Please do it manually");
            }
        }
    }

    protected String getDataDirectory() {
        return defaultDataDirectory;
    }

    protected void removeSnapshotFiles() {
        File[] snapshotFiles = getAllSnapshotFiles();
        for (File snapshotFile : snapshotFiles) {
            if(!snapshotFile.delete()) {
                System.err.println("Failed to remove snapshot file: "
                        + snapshotFile.getAbsolutePath()
                        + ". Please do it manually");
            }
        }
    }

    protected String getSnapshotDirectory() {
        return defaultSnapshotDirectory;
    }

    private File[] getAllSnapshotFiles() {
        return new File(getSnapshotDirectory()).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith("-snapshot");
            }
        });
    }
}
