package org.edx.mobile.model;

import org.edx.mobile.model.api.TranscriptModel;
import org.edx.mobile.model.db.DownloadEntry;
import org.edx.mobile.model.download.NativeDownloadModel;

/**
 * Any audio model should implement this interface.
 * Database model should also implement this interface.
 * @author zohaib
 *
 *
 */

public interface AudioModel {

    String getUsername();

    String getTitle();

    String getBlockId();

    long getSize();

    long getDuration();

    String getFilePath();

    String getOggUrl();

    String getMp3Url();

    int getWatchedStateOrdinal();

    int getDownloadedStateOrdinal();

    long getDmId();

    String getEnrollmentId();

    String getChapterName();

    String getSectionName();

    int getLastPlayedOffset();

    String getLmsUrl();

    boolean isCourseActive();

    long getDownloadedOn();

    TranscriptModel getTranscripts();
    //TODO: write all required method of the video model

    /**
     * Sets download information from the given download object.
     * @param download
     */
    void setDownloadInfo(NativeDownloadModel download);

    /**
     * Sets download information from the given video object.
     * @param audioByUrl
     */
    void setDownloadInfo(DownloadEntry audioByUrl);

    /**
     * Sets downloading information from the given download object.
     * @param download
     */
    void setDownloadingInfo(NativeDownloadModel download);
    
}
