package com.torch.inappupdateandroid;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.ActivityResult;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

@RequiresApi(api = 21)
public class AppUpdateHelper implements InstallStateUpdatedListener {

    public static final int APP_UPDATE_REQUEST_CODE = 277873283;
    public static final boolean IN_APP_UPDATE_AVAILABLE =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    private final AppUpdateManager appUpdateManager;
    private final Activity activity;
    @AppUpdateType
    private int appUpdateType;

    public AppUpdateHelper(Activity activity,
                           @AppUpdateType int appUpdateType) {
        this.appUpdateManager = AppUpdateManagerFactory.create(activity.getApplicationContext());
        this.activity = activity;
        this.appUpdateType = appUpdateType;
        this.appUpdateManager.registerListener(this);
    }

    public AppUpdateHelper(Activity activity) {
        this(activity, AppUpdateType.FLEXIBLE);
    }

    public void setAppUpdateType(int appUpdateType) {
        this.appUpdateType = appUpdateType;
    }

    public void checkForUpdate() {

        // Checks that the platform will allow the specified type of update.
        if (IN_APP_UPDATE_AVAILABLE) {

            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(info -> {
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(appUpdateType)) {
                    startUpdate(info);
                }
            });

        }
    }

    private void startUpdate(AppUpdateInfo info) {
        try {
            appUpdateManager.startUpdateFlowForResult(info,
                                                      appUpdateType,
                                                      activity,
                                                      APP_UPDATE_REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            // show error message.
        }
    }

    private void flexibleUpdateDownloadCompleted() {
        Snackbar.make(
                        activity.findViewById(android.R.id.content),
                        "An update has just been downloaded.",
                        Snackbar.LENGTH_INDEFINITE
                     )
                .setAction("RESTART", v -> appUpdateManager.completeUpdate())
                .setActionTextColor(Color.WHITE).
                show();
    }

    public void onResume() {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(info -> {
            if (appUpdateType == AppUpdateType.FLEXIBLE &&
                    info.installStatus() == InstallStatus.DOWNLOADED) {
                // If the update is downloaded but not installed, notify the user to complete the
                // update.
                flexibleUpdateDownloadCompleted();
            } else if (appUpdateType == AppUpdateType.IMMEDIATE && info.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // for AppUpdateType.IMMEDIATE only, already executing updater
                startUpdate(info);
            }
        });
    }

    public void onDestroy() {
        appUpdateManager.unregisterListener(this);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_CANCELED) {
                // The user has denied or canceled the update.

            } else if (resultCode == AppCompatActivity.RESULT_OK) {
                // For IMMEDIATE updates, you might not receive this callback
                // because the update should already be finished by the time control is given
                // back to your app.

            } else if (resultCode == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                // Some other error prevented either the user from providing consent or
                // the update from proceeding.
            }
        }
    }

    @Override
    public void onStateUpdate(@NonNull InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADED &&
                appUpdateType == AppUpdateType.FLEXIBLE) {
            flexibleUpdateDownloadCompleted();
        }
    }
}
