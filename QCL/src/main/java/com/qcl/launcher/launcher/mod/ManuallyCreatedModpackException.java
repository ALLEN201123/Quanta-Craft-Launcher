package com.qcl.launcher.launcher.mod;

import java.nio.file.Path;

public class ManuallyCreatedModpackException extends Exception {
    private final Path path;

    public ManuallyCreatedModpackException(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}