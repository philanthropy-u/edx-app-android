package org.edx.mobile.view.adapters;


import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.TextUtils;

import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.model.api.EnrolledCoursesResponse;
import org.edx.mobile.model.course.AudioBlockModel;
import org.edx.mobile.model.course.BlockType;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.model.course.DiscussionBlockModel;
import org.edx.mobile.model.course.HtmlBlockModel;
import org.edx.mobile.model.course.VideoBlockModel;
import org.edx.mobile.model.db.DownloadEntry;
import org.edx.mobile.util.Config;
import org.edx.mobile.view.CourseUnitAudioFragment;
import org.edx.mobile.view.CourseUnitDiscussionFragment;
import org.edx.mobile.view.CourseUnitEmptyFragment;
import org.edx.mobile.view.CourseUnitFragment;
import org.edx.mobile.view.CourseUnitMobileNotSupportedFragment;
import org.edx.mobile.view.CourseUnitOnlyOnYoutubeFragment;
import org.edx.mobile.view.CourseUnitVideoFragment;
import org.edx.mobile.view.CourseUnitWebViewFragment;

import java.util.List;

public class CourseUnitPagerAdapter extends FragmentStatePagerAdapter {

    private final IEdxEnvironment environment;
    private List<CourseComponent> unitList;
    private EnrolledCoursesResponse courseData;
    private CourseUnitFragment.HasComponent callback;

    public CourseUnitPagerAdapter(FragmentManager manager,
                                  IEdxEnvironment environment,
                                  List<CourseComponent> unitList,
                                  EnrolledCoursesResponse courseData,
                                  CourseUnitFragment.HasComponent callback) {
        super(manager);
        this.environment = environment;
        this.unitList = unitList;
        this.courseData = courseData;
        this.callback = callback;
    }

    public CourseComponent getUnit(int pos) {
        if (pos >= unitList.size())
            pos = unitList.size() - 1;
        if (pos < 0)
            pos = 0;
        return unitList.get(pos);
    }

    /**
     * @return True if given unit is a video unit (not an only on YouTube unit)
     */
    public static boolean isCourseUnitVideo(CourseComponent unit) {
        return (unit instanceof VideoBlockModel && ((VideoBlockModel) unit).getData().encodedVideos.getPreferredVideoInfo() != null);
    }

    private boolean isYoutubeVideo(CourseComponent unit) {
        return unit instanceof VideoBlockModel && ((VideoBlockModel) unit).getData().encodedVideos.getYoutubeVideoInfo() != null;
    }

    @Override
    public Fragment getItem(int pos) {
        CourseComponent unit = getUnit(pos);
        CourseUnitFragment unitFragment;
        //FIXME - for the video, let's ignore studentViewMultiDevice for now
        if (unit instanceof AudioBlockModel) {
            unitFragment = CourseUnitAudioFragment.newInstance((AudioBlockModel) unit, (pos < unitList.size()), (pos > 0));
        } else if (isYoutubeVideo(unit)&& !isDownloaded(unit) && !TextUtils.isEmpty(environment.getConfig().getYoutubeApiKey())) {
            unitFragment = CourseUnitOnlyOnYoutubeFragment.newInstance(unit, environment.getConfig().getYoutubeApiKey());
        } else if (isCourseUnitVideo(unit)) {
            unitFragment = CourseUnitVideoFragment.newInstance((VideoBlockModel) unit, (pos < unitList.size()), (pos > 0));
        } else if (environment.getConfig().isDiscussionsEnabled() && unit instanceof DiscussionBlockModel) {
            unitFragment = CourseUnitDiscussionFragment.newInstance(unit, courseData);
        } else if (!unit.isMultiDevice()) {
            unitFragment = CourseUnitMobileNotSupportedFragment.newInstance(unit);
        } else if (unit.getType() != BlockType.VIDEO &&
                unit.getType() != BlockType.HTML &&
                unit.getType() != BlockType.OTHERS &&
                unit.getType() != BlockType.DISCUSSION &&
                unit.getType() != BlockType.PROBLEM) {
            unitFragment = CourseUnitEmptyFragment.newInstance(unit);
        } else if (unit instanceof HtmlBlockModel) {
            unitFragment = CourseUnitWebViewFragment.newInstance((HtmlBlockModel) unit);
        }

        //fallback
        else {
            unitFragment = CourseUnitMobileNotSupportedFragment.newInstance(unit);
        }

        unitFragment.setHasComponentCallback(callback);

        return unitFragment;
    }

    private boolean isDownloaded(CourseComponent unit) {
        DownloadEntry downloadEntry = ((VideoBlockModel) unit).getDownloadEntry(environment.getStorage());
        return downloadEntry != null && environment.getDownloadManager().isDownloadComplete(downloadEntry.getDmId());
    }

    @Override
    public int getCount() {
        return unitList.size();
    }
}
