package com.qcl.launcher.launcher.game;

import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.launcher.game.CompatibilityRule;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.platform.OperatingSystem;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public final class Arguments {
    public static final List<Argument> DEFAULT_GAME_ARGUMENTS;
    public static final List<Argument> DEFAULT_JVM_ARGUMENTS;

    @SerializedName("game")
    private final List<Argument> game;

    @SerializedName("jvm")
    private final List<Argument> jvm;

    public Arguments() {
        this(null, null);
    }

    public Arguments(List<Argument> list, List<Argument> list2) {
        this.game = list;
        this.jvm = list2;
    }

    public List<Argument> getGame() {
        List<Argument> list = this.game;
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableList(list);
    }

    public Arguments withGame(List<Argument> list) {
        return new Arguments(list, this.jvm);
    }

    public List<Argument> getJvm() {
        List<Argument> list = this.jvm;
        if (list == null) {
            return null;
        }
        return Collections.unmodifiableList(list);
    }

    public Arguments withJvm(List<Argument> list) {
        return new Arguments(this.game, list);
    }

    public Arguments addGameArguments(String... strArr) {
        return addGameArguments(Arrays.asList(strArr));
    }

    public Arguments addGameArguments(List<String> list) {
        return new Arguments(Lang.merge(getGame(), (List) list.stream().map(Arguments$$ExternalSyntheticLambda2.INSTANCE).collect(Collectors.toList())), getJvm());
    }

    public Arguments addJVMArguments(String... strArr) {
        return addJVMArguments(Arrays.asList(strArr));
    }

    public Arguments addJVMArguments(List<String> list) {
        return new Arguments(getGame(), Lang.merge(getJvm(), (List) list.stream().map(Arguments$$ExternalSyntheticLambda2.INSTANCE).collect(Collectors.toList())));
    }

    public static Arguments merge(Arguments arguments, Arguments arguments2) {
        if (arguments == null) {
            return arguments2;
        }
        if (arguments2 == null) {
            return arguments;
        }
        List<Argument> list = arguments.game;
        List merge = (list == null && arguments2.game == null) ? null : Lang.merge(list, arguments2.game);
        List<Argument> list2 = arguments.jvm;
        return new Arguments(merge, (list2 == null && arguments2.jvm == null) ? null : Lang.merge(list2, arguments2.jvm));
    }

    public static List<String> parseStringArguments(List<String> list, final Map<String, String> map) {
        return (List) list.stream().filter(new Predicate() { // from class: com.qcl.launcher.launcher.game.Arguments$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean nonNull;
                nonNull = Objects.nonNull((String) obj);
                return nonNull;
            }
        }).flatMap(new Function() { // from class: com.qcl.launcher.launcher.game.Arguments$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Stream stream;
                stream = new StringArgument((String) obj).toString(map, Collections.emptyMap()).stream();
                return stream;
            }
        }).collect(Collectors.toList());
    }

    public static List<String> parseArguments(List<Argument> list, Map<String, String> map) {
        return parseArguments(list, map, Collections.emptyMap());
    }

    public static List<String> parseArguments(List<Argument> list, final Map<String, String> map, final Map<String, Boolean> map2) {
        return (List) list.stream().filter(new Predicate() { // from class: com.qcl.launcher.launcher.game.Arguments$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean nonNull;
                nonNull = Objects.nonNull((Argument) obj);
                return nonNull;
            }
        }).flatMap(new Function() { // from class: com.qcl.launcher.launcher.game.Arguments$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Stream stream;
                stream = ((Argument) obj).toString(map, map2).stream();
                return stream;
            }
        }).collect(Collectors.toList());
    }

    static {
        LinkedList linkedList = new LinkedList();
        linkedList.add(new RuledArgument(Collections.singletonList(new CompatibilityRule(CompatibilityRule.Action.ALLOW, new OSRestriction(OperatingSystem.WINDOWS))), Collections.singletonList("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump")));
        linkedList.add(new RuledArgument(Collections.singletonList(new CompatibilityRule(CompatibilityRule.Action.ALLOW, new OSRestriction(OperatingSystem.WINDOWS, "^10\\."))), Arrays.asList("-Dos.name=Windows 10", "-Dos.version=10.0")));
        linkedList.add(new StringArgument("-Djava.library.path=${natives_directory}"));
        linkedList.add(new StringArgument("-Dminecraft.launcher.brand=${launcher_name}"));
        linkedList.add(new StringArgument("-Dminecraft.launcher.version=${launcher_version}"));
        linkedList.add(new StringArgument("-cp"));
        linkedList.add(new StringArgument("${classpath}"));
        DEFAULT_JVM_ARGUMENTS = Collections.unmodifiableList(linkedList);
        LinkedList linkedList2 = new LinkedList();
        linkedList2.add(new RuledArgument(Collections.singletonList(new CompatibilityRule(CompatibilityRule.Action.ALLOW, null, Collections.singletonMap("has_custom_resolution", true))), Arrays.asList("--width", "${resolution_width}", "--height", "${resolution_height}")));
        DEFAULT_GAME_ARGUMENTS = Collections.unmodifiableList(linkedList2);
    }
}
