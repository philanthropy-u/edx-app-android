package org.edx.mobile.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.edx.mobile.base.MainApplication;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.db.DownloadEntry;
import org.edx.mobile.module.db.DataCallback;

import java.util.List;

/**
 * Created by Zohaib Asaad on 2/1/2018.
 */

public class CourseMediaStatusRefreshService extends IntentService {

    Logger logger = new Logger(CourseMediaStatusRefreshService.class);

    public CourseMediaStatusRefreshService() {
        super(CourseMediaStatusRefreshService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final IEdxEnvironment environment = MainApplication.getEnvironment(this);

        environment.getDatabase().getAllMedia(new DataCallback<List<DownloadEntry>>() {

            @Override
            public void onResult(List<DownloadEntry> result) {
                logger.debug("Updating downloaded courses content status");
                for (DownloadEntry downloadEntry : result) {
                    if (downloadEntry.downloaded == DownloadEntry.DownloadedState.DOWNLOADED
                            && !environment.getDownloadManager().isDownloadComplete(downloadEntry.dmId)) {
                        environment.getStorage().removeDownload(downloadEntry);
                        logger.debug(String.format("Downloaded media for %s not found.", downloadEntry.getDownloadUrl()));
                    }
                }
                logger.debug("Finished updating downloaded courses content status");
            }

            @Override
            public void onFail(Exception ex) {
                logger.debug("Failed to update downloaded courses content status");
                logger.debug(ex.getMessage());
            }
        });
    }

}
