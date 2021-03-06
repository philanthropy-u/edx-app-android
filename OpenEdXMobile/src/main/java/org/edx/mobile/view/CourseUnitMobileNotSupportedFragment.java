package org.edx.mobile.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.edx.mobile.R;
import org.edx.mobile.model.course.BlockType;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.services.EdxCookieManager;
import org.edx.mobile.services.ViewPagerDownloadManager;
import org.edx.mobile.util.BrowserUtil;

/**
 *
 */
public class CourseUnitMobileNotSupportedFragment extends CourseUnitFragment {

    /**
     * Create a new instance of fragment
     */
    public static CourseUnitMobileNotSupportedFragment newInstance(CourseComponent unit) {
        CourseUnitMobileNotSupportedFragment f = new CourseUnitMobileNotSupportedFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putSerializable(Router.EXTRA_COURSE_UNIT, unit);
        f.setArguments(args);

        return f;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_course_unit_grade, container, false);
        v.findViewById(R.id.view_on_web_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BrowserUtil.open(getActivity(), unit.getWebUrl());
                environment.getAnalyticsRegistry().trackOpenInBrowser(unit.getId()
                        , unit.getCourseId(), unit.isMultiDevice(), unit.getBlockId());
                /*
                  Clearing the cookies because browser may create/update cookies that may not work
                  for the application session so by clearing we make sure that we update cookies once
                  user comes back to app.
                 */
                EdxCookieManager.getSharedInstance(getContext()).clearWebWiewCookie();

            }
        });
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (ViewPagerDownloadManager.instance.inInitialPhase(unit))
            ViewPagerDownloadManager.instance.addTask(this);
    }


    @Override
    public void run() {
        ViewPagerDownloadManager.instance.done(this, true);
    }

}
