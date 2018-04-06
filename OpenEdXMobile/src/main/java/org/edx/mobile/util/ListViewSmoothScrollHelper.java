package org.edx.mobile.util;

import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

/**
 * Created by Zohaib on 4/5/2018.
 */

public class ListViewSmoothScrollHelper {

    public static void smoothScrollToPosition(final AbsListView view, final int position) {
        try {
            View child = getChildAtPosition(view, position);
            // There's no need to scroll if child is already at top or view is already scrolled to its end
            if (child != null && ((child.getTop() == 0) || ((child.getTop() > 0) && !view.canScrollVertically(1)))) {
                return;
            }

            view.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(final AbsListView view, final int scrollState) {
                    if (scrollState == SCROLL_STATE_IDLE) {
                        view.setOnScrollListener(null);

                        // Fix for scrolling bug
                        view.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                view.setSelection(position);
                            }
                        }, 200);
                    }
                }

                @Override
                public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
                                     final int totalItemCount) {
                }
            });

            // Perform scrolling to position
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    view.smoothScrollToPositionFromTop(position, 0);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static View getChildAtPosition(final AdapterView view, final int position) {
        try {
            final int index = position - view.getFirstVisiblePosition();
            if ((index >= 0) && (index < view.getChildCount())) {
                return view.getChildAt(index);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
