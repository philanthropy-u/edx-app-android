package org.edx.mobile.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.edx.mobile.R;
import org.edx.mobile.event.NewRelicEvent;
import org.edx.mobile.logger.Logger;

import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import roboguice.fragment.RoboFragment;

public class BaseFragment extends RoboFragment implements EasyPermissions.PermissionCallbacks {

    private boolean isFirstVisit;
    protected final Logger logger = new Logger(getClass().getSimpleName());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().post(new NewRelicEvent(getClass().getSimpleName()));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isFirstVisit = true;
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstVisit) {
            isFirstVisit = false;
        } else {
            onRevisit();
        }
    }

    /**
     * Called when a Fragment is re-displayed to the user (the user has navigated back to it).
     * Defined to mock the behavior of {@link Activity#onRestart() Activity.onRestart} function.
     */
    protected void onRevisit() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        logger.debug(String.format(Locale.ENGLISH, "Permissions granted: %s", perms.toString()));
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        logger.debug(String.format(Locale.ENGLISH, "Permissions denied: %s", perms.toString()));
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
}
