/*
 * Decompiled with CFR 0.152.
 */
package com.qcl.launcher.launcher.launch;

import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import com.qcl.launcher.manifest.AppManifest;
import java.util.Vector;

public class TouchInjector {
    public static Vector<String> rebaseArguments(GameLaunchSetting gameLaunchSetting, Vector<String> args) {
        if (!gameLaunchSetting.touchInjector) {
            return args;
        }
        Vector<String> newArgs = new Vector<String>();
        if (args.contains("Forge") || args.contains("cpw.mods.fml.common.launcher.FMLTweaker") || args.contains("fmlclient") || args.contains("forgeclient")) {
            if (args.contains("cpw.mods.bootstraplauncher.BootstrapLauncher")) {
                String version = "unknown";
                boolean hit = false;
                for (String arg : args) {
                    if (hit) {
                        if (arg.startsWith("--")) {
                            hit = false;
                        } else {
                            if (arg.startsWith("1.17")) {
                                version = "1.17";
                                break;
                            }
                            if (arg.startsWith("1.18")) {
                                version = "1.18";
                                break;
                            }
                            if (!arg.startsWith("1.19")) break;
                            version = "1.19";
                            break;
                        }
                    }
                    if (!"--assetIndex".equals(arg)) continue;
                    hit = true;
                }
                hit = false;
                for (int i = 0; i < args.size(); ++i) {
                    if (hit) {
                        newArgs.add(args.get(i) + ":" + AppManifest.PLUGIN_DIR + "/touch/TouchInjector-forge.jar");
                        hit = false;
                        continue;
                    }
                    if (args.get(i).startsWith("-Xms")) {
                        newArgs.add("-Dtouchinjector.version=" + version);
                        newArgs.add(args.get(i));
                        continue;
                    }
                    if (args.get(i).equals("-cp")) {
                        hit = true;
                        newArgs.add(args.get(i));
                        continue;
                    }
                    newArgs.add(args.get(i));
                }
            } else {
                for (int i = 0; i < args.size(); ++i) {
                    if (args.get(i).startsWith("-Xms")) {
                        newArgs.add("-javaagent:" + AppManifest.PLUGIN_DIR + "/touch/TouchInjector.jar=forge");
                    }
                    newArgs.add(args.get(i));
                }
            }
            return newArgs;
        }
        if (args.contains("optifine.OptiFineTweaker") || args.contains("com.mumfrey.liteloader.launch.LiteLoaderTweaker")) {
            for (int i = 0; i < args.size(); ++i) {
                if (args.get(i).startsWith("-Xms")) {
                    newArgs.add("-javaagent:" + AppManifest.PLUGIN_DIR + "/touch/TouchInjector.jar=optifine");
                }
                newArgs.add(args.get(i));
            }
            return newArgs;
        }
        if (args.contains("net.fabricmc.loader.impl.launch.knot.KnotClient")) {
            boolean hit = false;
            for (int i = 0; i < args.size(); ++i) {
                if (hit) {
                    newArgs.add(args.get(i) + ":" + AppManifest.PLUGIN_DIR + "/touch/TouchInjector.jar");
                    hit = false;
                    continue;
                }
                if (args.get(i).equals("net.fabricmc.loader.impl.launch.knot.KnotClient")) {
                    newArgs.add("com.tungsten.touchinjector.launch.FabricKnotClient");
                    continue;
                }
                if (args.get(i).equals("-cp")) {
                    hit = true;
                    newArgs.add(args.get(i));
                    continue;
                }
                newArgs.add(args.get(i));
            }
            return newArgs;
        }
        if (args.contains("org.quiltmc.loader.impl.launch.knot.KnotClient")) {
            boolean hit = false;
            for (int i = 0; i < args.size(); ++i) {
                if (hit) {
                    newArgs.add(args.get(i) + ":" + AppManifest.PLUGIN_DIR + "/touch/TouchInjector.jar");
                    hit = false;
                    continue;
                }
                if (args.get(i).equals("org.quiltmc.loader.impl.launch.knot.KnotClient")) {
                    newArgs.add("com.tungsten.touchinjector.launch.QuiltKnotClient");
                    continue;
                }
                if (args.get(i).equals("-cp")) {
                    hit = true;
                    newArgs.add(args.get(i));
                    continue;
                }
                newArgs.add(args.get(i));
            }
            return newArgs;
        }
        for (int i = 0; i < args.size(); ++i) {
            if (args.get(i).startsWith("-Xms")) {
                newArgs.add("-javaagent:" + AppManifest.PLUGIN_DIR + "/touch/TouchInjector.jar=vanilla");
            }
            newArgs.add(args.get(i));
        }
        return newArgs;
    }
}

