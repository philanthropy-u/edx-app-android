package org.edx.mobile.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.edx.mobile.R;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.course.BlockType;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.module.analytics.Analytics;
import org.edx.mobile.services.LastAccessManager;
import org.edx.mobile.services.MediaDownloadHelper;
import org.edx.mobile.services.ViewPagerDownloadManager;
import org.edx.mobile.view.adapters.CourseUnitPagerAdapter;
import org.edx.mobile.view.common.PageViewStateCallback;
import org.edx.mobile.view.custom.DisableableViewPager;
import org.edx.mobile.view.custom.IconImageViewXml;
import org.edx.mobile.view.custom.IndicatorController;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import roboguice.inject.InjectView;


/**
 *
 */
public class CourseUnitNavigationActivity extends CourseBaseActivity implements
        CourseUnitVideoFragment.HasComponent, MediaDownloadHelper.DownloadManagerCallback {

    protected Logger logger = new Logger(getClass().getSimpleName());
    @Inject
    LastAccessManager lastAccessManager;
    private DisableableViewPager pager;
    private CourseComponent selectedUnit;
    private List<CourseComponent> unitList = new ArrayList<>();
    private CourseUnitPagerAdapter pagerAdapter;
    @InjectView(R.id.goto_next)
    private Button mNextBtn;
    @InjectView(R.id.goto_prev)
    private Button mPreviousBtn;
    @InjectView(R.id.goto_next_container)
    private LinearLayout mBtnNextUnit;
    @InjectView(R.id.goto_prev_container)
    private LinearLayout mBtnPreviousUnit;
    @InjectView(R.id.next_button_icon)
    private IconImageViewXml mNextBtnIcon;
    @InjectView(R.id.previous_button_icon)
    private IconImageViewXml mPreviousBtnIcon;
    @InjectView(R.id.next_unit_title)
    private TextView mNextUnitLbl;
    @InjectView(R.id.prev_unit_title)
    private TextView mPreviousUnitLbl;
    @InjectView(R.id.indicator_container)
    private FrameLayout mIndicatorContainer;

    private IndicatorController indicatorController;

    private PageViewStateCallback currentVisiblePage;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RelativeLayout insertPoint = (RelativeLayout) findViewById(R.id.fragment_container);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.view_course_unit_pager, null);
        insertPoint.addView(v, 0,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pager = (DisableableViewPager) findViewById(R.id.pager);
        pagerAdapter = new CourseUnitPagerAdapter(getSupportFragmentManager(),
                environment, unitList, courseData, this);
        pager.setAdapter(pagerAdapter);


        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // replaced ViewPager.SCROLL_STATE_DRAGGING flag logic with logic to track current page
                // and match it with next page and activate appropriate initialization and destruction callbacks
                // when the page is changed completely.
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    int curIndex = pager.getCurrentItem();
                    PageViewStateCallback nextPage = (PageViewStateCallback) pagerAdapter.instantiateItem(pager, curIndex);
                    if(currentVisiblePage != null && currentVisiblePage != nextPage){
                        currentVisiblePage.onPageDisappear();
                    }
                    if (nextPage != null && currentVisiblePage != nextPage)
                        nextPage.onPageShow();
                    currentVisiblePage = nextPage;
                    tryToUpdateForEndOfSequential();
                }
            }
        });

        findViewById(R.id.course_unit_nav_bar).setVisibility(View.VISIBLE);

        mBtnPreviousUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigatePreviousComponent();
            }
        });
        mBtnNextUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateNextComponent();
            }
        });

        initProgressIndicator();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIForOrientation();
    }

    @Override
    public void navigatePreviousComponent() {
        int index = pager.getCurrentItem();
        if (index > 0) {
            PageViewStateCallback curView = (PageViewStateCallback) pagerAdapter.instantiateItem(pager, index);
            if (curView != null)
                curView.onPageDisappear();
            pager.setCurrentItem(index - 1);
        }
    }

    @Override
    public void navigateNextComponent() {
        int index = pager.getCurrentItem();
        if (index < pagerAdapter.getCount() - 1) {
            PageViewStateCallback curView = (PageViewStateCallback) pagerAdapter.instantiateItem(pager, index);
            if (curView != null)
                curView.onPageDisappear();
            pager.setCurrentItem(index + 1);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUIForOrientation();
        environment.getAnalyticsRegistry().trackCourseComponentViewed(selectedUnit.getId(),
                courseData.getCourse().getId(), selectedUnit.getBlockId());
    }

    public CourseComponent getComponent() {
        return selectedUnit;
    }


    @Override
    protected void onLoadData() {
        selectedUnit = courseManager.getComponentById(courseData.getCourse().getId(), courseComponentId);
        updateDataModel();
    }

    @Override
    protected void onOffline() {
    }

    protected void hideLastAccessedView(View v) {
    }

    protected void showLastAccessedView(View v, String title, View.OnClickListener listener) {
    }


    private void initProgressIndicator() {
        indicatorController = new IndicatorController(R.drawable.unit_indicator_dot_active, R.drawable.unit_indicator_dot_inactive);
        mIndicatorContainer.addView(indicatorController.newInstance(this));
    }

    private void setCurrentUnit(CourseComponent component) {
        this.selectedUnit = component;
        if (this.selectedUnit == null)
            return;

        courseComponentId = selectedUnit.getId();
        environment.getDatabase().updateAccess(null, selectedUnit.getId(), true);

        updateUIForOrientation();

        lastAccessManager.setLastAccessed(selectedUnit.getCourseId(), this.selectedUnit.getId());

        Intent resultData = new Intent();
        resultData.putExtra(Router.EXTRA_COURSE_COMPONENT_ID, courseComponentId);
        setResult(RESULT_OK, resultData);

        environment.getAnalyticsRegistry().trackScreenView(
                Analytics.Screens.UNIT_DETAIL, courseData.getCourse().getId(), selectedUnit.getInternalName());
        environment.getAnalyticsRegistry().trackCourseComponentViewed(selectedUnit.getId(),
                courseData.getCourse().getId(), selectedUnit.getBlockId());
    }

    private void tryToUpdateForEndOfSequential() {
        int curIndex = pager.getCurrentItem();

        if (!selectedUnit.getParent().getId().equalsIgnoreCase(pagerAdapter.getUnit(curIndex).getParent().getId())) {
            indicatorController.setCount(getTotalComponentsCount(pagerAdapter.getUnit(curIndex)));
        }

        setCurrentUnit(pagerAdapter.getUnit(curIndex));

        indicatorController.selectPosition(getCurrentComponentIndex());

        mPreviousBtn.setEnabled(curIndex > 0);
        mPreviousBtnIcon.setEnabled(curIndex > 0);
        mNextBtn.setEnabled(curIndex < pagerAdapter.getCount() - 1);
        mNextBtnIcon.setEnabled(curIndex < pagerAdapter.getCount() - 1);

        findViewById(R.id.course_unit_nav_bar).requestLayout();

        setTitle(selectedUnit.getParent().getDisplayName());
        try{
            getSupportActionBar().setSubtitle(selectedUnit.getDisplayName());
        }catch (NullPointerException ex){
            logger.error(ex);
        }
        String currentSubsectionId = selectedUnit.getParent().getId();
        if (curIndex + 1 <= pagerAdapter.getCount() - 1) {
            String nextUnitSubsectionId = unitList.get(curIndex + 1).getParent().getId();
            if (currentSubsectionId.equalsIgnoreCase(nextUnitSubsectionId)) {
                mNextUnitLbl.setVisibility(View.GONE);
            } else {
                mNextUnitLbl.setText(unitList.get(curIndex + 1).getParent().getDisplayName());
                mNextUnitLbl.setVisibility(View.GONE);
            }
        } else {
            // we have reached the end and next button is disabled
            mNextUnitLbl.setVisibility(View.GONE);
        }

        if (curIndex - 1 >= 0) {
            String prevUnitSubsectionId = unitList.get(curIndex - 1).getParent().getId();
            if (currentSubsectionId.equalsIgnoreCase(prevUnitSubsectionId)) {
                mPreviousUnitLbl.setVisibility(View.GONE);
            } else {
                mPreviousUnitLbl.setText(unitList.get(curIndex - 1).getParent().getDisplayName());
                mPreviousUnitLbl.setVisibility(View.GONE);
            }
        } else {
            // we have reached the start and previous button is disabled
            mPreviousUnitLbl.setVisibility(View.GONE);
        }
    }

    private void updateDataModel() {
        unitList.clear();
        if (selectedUnit == null || selectedUnit.getRoot() == null) {
            logger.warn("selectedUnit is null?");
            return;   //should not happen
        }

        //if we want to navigate through all unit of within the parent node,
        //we should use courseComponent instead.   Requirement maybe changed?
        // unitList.addAll( courseComponent.getChildLeafs() );
        List<CourseComponent> leaves = new ArrayList<>();

        selectedUnit.getRoot().fetchAllLeafComponents(leaves, EnumSet.allOf(BlockType.class));

        unitList.addAll(leaves);
        pagerAdapter.notifyDataSetChanged();

        ViewPagerDownloadManager.instance.setMainComponent(selectedUnit, unitList);

        int index = unitList.indexOf(selectedUnit);
        if (index >= 0) {
            pager.setCurrentItem(index);
            currentVisiblePage = (PageViewStateCallback) pagerAdapter.instantiateItem(pager, index);
            if (currentVisiblePage != null)
                currentVisiblePage.onFirstPageLoad();
            tryToUpdateForEndOfSequential();
        }

        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }

        // Initialize indicator layout for first opened unit
        indicatorController.setCount(getTotalComponentsCount(selectedUnit));
        indicatorController.selectPosition(getCurrentComponentIndex());

    }

    private int getTotalComponentsCount(CourseComponent unit) {
        return unit.getParent().getChildren().size();
    }

    private int getCurrentComponentIndex() {
        return selectedUnit.getParent().getChildren().indexOf(selectedUnit);
    }

    private void updateUIForOrientation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && CourseUnitPagerAdapter.isCourseUnitVideo(selectedUnit)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setActionBarVisible(false);
            findViewById(R.id.course_unit_nav_bar).setVisibility(View.GONE);

        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setActionBarVisible(true);
            findViewById(R.id.course_unit_nav_bar).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDownloadStarted(Long result) {
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show();
        updateListUI();
    }

    @Override
    public void onDownloadFailedToStart() {
        updateListUI();
    }

    @Override
    public void showProgressDialog(int numDownloads) {
        updateListUI();
    }

    @Override
    public void updateListUI() {
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        environment.getRouter().manageAudioServiceRouting(isTaskRoot() , CourseUnitNavigationActivity.this);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        environment.getRouter().stopAudioServiceIfRunning(CourseUnitNavigationActivity.this);
        super.onDestroy();
    }
}
