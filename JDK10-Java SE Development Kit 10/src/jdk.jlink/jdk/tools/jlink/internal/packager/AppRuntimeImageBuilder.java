/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package jdk.tools.jlink.internal.packager;


import jdk.tools.jlink.builder.DefaultImageBuilder;
import jdk.tools.jlink.internal.Jlink;
import jdk.tools.jlink.internal.JlinkTask;
import jdk.tools.jlink.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AppRuntimeImageBuilder is a private API used only by the Java Packager to generate
 * a Java runtime image using jlink. AppRuntimeImageBuilder encapsulates the
 * arguments that jlink requires to generate this image. To create the image call the
 * build() method.
 */
public final class AppRuntimeImageBuilder {
    private Path outputDir = null;
    private Map<String, String> launchers = Collections.emptyMap();
    private List<Path> modulePath = null;
    private Set<String> addModules = null;
    private Set<String> limitModules = null;
    private String excludeFileList = null;
    private Map<String, String> userArguments = null;
    private Boolean stripNativeCommands = null;

    public AppRuntimeImageBuilder() {}

    public void setOutputDir(Path value) {
        outputDir = value;
    }

    public void setLaunchers(Map<String, String> value) {
        launchers = value;
    }

    public void setModulePath(List<Path> value) {
        modulePath = value;
    }

    public void setAddModules(Set<String> value) {
        addModules = value;
    }

    public void setLimitModules(Set<String> value) {
        limitModules = value;
    }

    public void setExcludeFileList(String value) {
        excludeFileList = value;
    }

    public void setStripNativeCommands(boolean value) {
        stripNativeCommands = value;
    }

    public void setUserArguments(Map<String, String> value) {
        userArguments = value;
    }

    public void build() throws IOException {
        // jlink main arguments
        Jlink.JlinkConfiguration jlinkConfig =
            new Jlink.JlinkConfiguration(new File("").toPath(), // Unused
                                         addModules,
                                         ByteOrder.nativeOrder(),
                                         moduleFinder(modulePath,
                                             limitModules, addModules));

        // plugin configuration
        List<Plugin> plugins = new ArrayList<Plugin>();

        if (stripNativeCommands) {
            plugins.add(Jlink.newPlugin(
                        "strip-native-commands",
                        Collections.singletonMap("strip-native-commands", "on"),
                        null));
        }

        if (excludeFileList != null && !excludeFileList.isEmpty()) {
            plugins.add(Jlink.newPlugin(
                        "exclude-files",
                        Collections.singletonMap("exclude-files", excludeFileList),
                        null));
        }

        // add user supplied jlink arguments
        for (Map.Entry<String, String> entry : userArguments.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            plugins.add(Jlink.newPlugin(key,
                                        Collections.singletonMap(key, value),
                                        null));
        }

        // build the image
        Jlink.PluginsConfiguration pluginConfig = new Jlink.PluginsConfiguration(
            plugins, new DefaultImageBuilder(outputDir, launchers), null);
        Jlink jlink = new Jlink();
        jlink.build(jlinkConfig, pluginConfig);
    }

    /*
     * Returns a ModuleFinder that limits observability to the given root
     * modules, their transitive dependences, plus a set of other modules.
     */
    public static ModuleFinder moduleFinder(List<Path> modulepaths,
                                            Set<String> roots,
                                            Set<String> otherModules) {
        return JlinkTask.newModuleFinder(modulepaths, roots, otherModules);
    }
}
