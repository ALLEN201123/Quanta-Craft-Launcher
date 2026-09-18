package com.qcl.launcher.launcher.download;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.qcl.launcher.launcher.game.Arguments;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.utils.Lang;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class PatchMerger {

    /* loaded from: classes2.dex */
    public interface ReMergeCallback {
        void onFailed();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$reMergePatch$0(DialogInterface dialogInterface, int i) {
    }

    public static Version reMergePatch(Context context, Version version, Version version2, String str, ReMergeCallback reMergeCallback) {
        Version removePatchById = version.removePatchById(str);
        if (version2 != null) {
            removePatchById = removePatchById.addPatch(version2);
        }
        List<Version> patches = removePatchById.getPatches();
        Iterator<Version> it = removePatchById.getPatches().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Version next = it.next();
            if (next.getId().equals("game")) {
                removePatchById = next.setId(next.getVersion()).setVersion(null).setPriority(null).addPatch(next);
                break;
            }
        }
        if (removePatchById == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.dialog_unknown_error_title));
            builder.setMessage(context.getString(R.string.dialog_unknown_error_msg));
            builder.setPositiveButton(context.getString(R.string.dialog_unknown_error_title), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.download.PatchMerger$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    PatchMerger.lambda$reMergePatch$0(dialogInterface, i);
                }
            });
            reMergeCallback.onFailed();
            builder.create().show();
        } else {
            patches.sort(new Comparator() { // from class: com.qcl.launcher.launcher.download.PatchMerger$$ExternalSyntheticLambda1
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return PatchMerger.lambda$reMergePatch$1((Version) obj, (Version) obj2);
                }
            });
            for (Version version3 : patches) {
                if (!version3.getId().equals("game")) {
                    if (version3.getId().equals("optifine")) {
                        removePatchById = mergeOptifinePatch(removePatchById, version3);
                    } else {
                        removePatchById = mergePatch(removePatchById, version3);
                    }
                }
            }
        }
        return removePatchById;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$reMergePatch$1(Version version, Version version2) {
        if (version.getPriority() > version2.getPriority()) {
            return -1;
        }
        return version.getPriority() > version2.getPriority() ? 1 : 0;
    }

    public static Version mergePatch(Version version, Version version2) {
        Version mainClass = version.addPatch(version2).setMainClass(version2.getMainClass());
        if (version2.getMinecraftArguments().isPresent()) {
            mainClass = mainClass.setMinecraftArguments(version2.getMinecraftArguments().get());
        }
        if (version2.getArguments().isPresent()) {
            if (mainClass.getArguments().isPresent()) {
                mainClass = mainClass.setArguments(Arguments.merge(mainClass.getArguments().get(), version2.getArguments().get()));
            } else {
                mainClass = mainClass.setArguments(version2.getArguments().get());
            }
        }
        List<Library> arrayList = new ArrayList<>(Lang.merge(mainClass.getLibraries(), version2.getLibraries()));
        for (Library library : mainClass.getLibraries()) {
            for (Library library2 : version2.getLibraries()) {
                if (library.equals(library2)) {
                    arrayList.remove(library2);
                }
                if (library.getArtifactId().equals(library2.getArtifactId()) && !library.getVersion().equals(library2.getVersion())) {
                    arrayList.remove(library);
                }
            }
        }
        return mainClass.setLibraries(arrayList);
    }

    public static Version mergeOptifinePatch(Version version, Version version2) {
        boolean z;
        Iterator<Version> it = version.getPatches().iterator();
        while (true) {
            if (!it.hasNext()) {
                z = false;
                break;
            }
            if (it.next().getId().equals("forge")) {
                z = true;
                break;
            }
        }
        Version addPatch = version.addPatch(version2);
        if (version2.getMinecraftArguments().isPresent()) {
            addPatch = addPatch.setMinecraftArguments(version2.getMinecraftArguments().get());
        }
        if (z) {
            if (version2.getArguments().isPresent()) {
                if (addPatch.getArguments().isPresent()) {
                    addPatch = addPatch.setArguments(Arguments.merge(addPatch.getArguments().get(), new Arguments().addGameArguments("--tweakClass", "optifine.OptiFineForgeTweaker")));
                } else {
                    addPatch = addPatch.setArguments(new Arguments().addGameArguments("--tweakClass", "optifine.OptiFineForgeTweaker"));
                }
            }
        } else {
            addPatch = addPatch.setMainClass(version2.getMainClass());
            if (version2.getArguments().isPresent()) {
                if (addPatch.getArguments().isPresent()) {
                    addPatch = addPatch.setArguments(Arguments.merge(addPatch.getArguments().get(), version2.getArguments().get()));
                } else {
                    addPatch = addPatch.setArguments(version2.getArguments().get());
                }
            }
        }
        List<Library> arrayList = new ArrayList<>(Lang.merge(addPatch.getLibraries(), version2.getLibraries()));
        for (Library library : addPatch.getLibraries()) {
            for (Library library2 : version2.getLibraries()) {
                if (library.equals(library2)) {
                    arrayList.remove(library2);
                }
                if (library.getArtifactId().equals(library2.getArtifactId()) && !library.getVersion().equals(library2.getVersion())) {
                    arrayList.remove(library);
                }
            }
        }
        return addPatch.setLibraries(arrayList);
    }
}
