package com.qcl.launcher.utils.gson.tools;

public @interface JsonSubtype {
    Class<?> clazz();

    String name();
}
