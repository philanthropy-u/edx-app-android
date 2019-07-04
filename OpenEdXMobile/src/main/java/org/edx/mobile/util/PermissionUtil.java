package org.edx.mobile.util;

import android.Manifest;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.edx.mobile.R;

import pub.devrel.easypermissions.EasyPermissions;

public class PermissionUtil {

    public static boolean hasStoragePermission(@Nullable Context context) {
        return context != null && EasyPermissions.hasPermissions(context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }

    public static void requestStoragePermission(@NonNull Fragment fragment, int requestCode) {
        EasyPermissions.requestPermissions(
                fragment,
                fragment.getString(R.string.download_permission_rationale),
                requestCode,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        );
    }
}
