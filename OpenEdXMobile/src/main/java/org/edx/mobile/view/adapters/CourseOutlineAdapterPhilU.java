package org.edx.mobile.view.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.vipulasri.timelineview.LineType;
import com.github.vipulasri.timelineview.TimelineView;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.internal.Animation;
import com.joanzapata.iconify.widget.IconImageView;

import org.edx.mobile.R;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.course.BlockPath;
import org.edx.mobile.model.course.BlockType;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.model.course.DiscussionBlockModel;
import org.edx.mobile.model.course.HasDownloadEntry;
import org.edx.mobile.model.course.HtmlBlockModel;
import org.edx.mobile.model.course.IBlock;
import org.edx.mobile.model.course.VideoBlockModel;
import org.edx.mobile.model.db.DownloadEntry;
import org.edx.mobile.module.db.DataCallback;
import org.edx.mobile.module.db.IDatabase;
import org.edx.mobile.module.storage.IStorage;
import org.edx.mobile.util.Config;
import org.edx.mobile.util.DateUtil;
import org.edx.mobile.util.MemoryUtil;
import org.edx.mobile.util.ResourceUtil;
import org.edx.mobile.util.TimeZoneUtils;
import org.edx.mobile.view.custom.IconImageViewXml;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by arslan on 12/14/17.
 */


/**
 * Used for pinned behavior.
 */
public class CourseOutlineAdapterPhilU extends BaseAdapter {

    private final Logger logger = new Logger(getClass().getName());

    public interface DownloadListener {
        void download(List<? extends HasDownloadEntry> models);

        void download(DownloadEntry videoData);

        void viewDownloadsStatus();
    }

    private Context context;
    private CourseComponent rootComponent;
    private LayoutInflater mInflater;
    private List<SectionRow> mData;

    private IDatabase dbStore;
    private IStorage storage;
    private DownloadListener mDownloadListener;
    private Config config;
    private boolean isVideoMode;
    public String lastAccessedId = "";
    private boolean hasLastAccessedShown = false;
    private int lastAccessedPosition = -1;

    public CourseOutlineAdapterPhilU(Context context, Config config, IDatabase dbStore, IStorage storage,
                                     DownloadListener listener, boolean isVideoMode, String lastAccessComponentId) {
        this.context = context;
        this.config = config;
        this.dbStore = dbStore;
        this.storage = storage;
        this.mDownloadListener = listener;
        this.isVideoMode = isVideoMode;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mData = new ArrayList();
        lastAccessedId = lastAccessComponentId;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public SectionRow getItem(int position) {
        if (position < 0 || position >= mData.size())
            return null;
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == SectionRow.ITEM;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {

        int type = getItemViewType(position);

        // FIXME: Re-enable row recycling in favor of better DB communication [MA-1640]
        //if (convertView == null) {
        switch (type) {
            case SectionRow.ITEM: {
                convertView = mInflater.inflate(R.layout.subsection_row_layout, parent, false);
                // apply a tag to this list row
                ViewHolder2 tag = getTag2(convertView);
                convertView.setTag(tag);
                break;
            }
            case SectionRow.SECTION: {
                convertView = mInflater.inflate(R.layout.section_row, parent, false);
                break;
            }
            default: {
                throw new IllegalArgumentException(String.valueOf(type));
            }
        }
        //}

        switch (type) {
            case SectionRow.ITEM: {
                return getRowView2(position, convertView);
            }
            case SectionRow.SECTION: {
                return getHeaderView2(position, convertView);
            }
            default: {
                throw new IllegalArgumentException(String.valueOf(type));
            }
        }
    }

    /**
     * component can be null.
     *
     * @IComponent component should be ICourse
     */
    public void setData(CourseComponent component) {
        if (component != null && !component.isContainer())
            return;//
        this.rootComponent = component;
        mData.clear();
        if (rootComponent != null) {
            List<IBlock> children = rootComponent.getChildren();
            for (IBlock block : children) {
                CourseComponent comp = (CourseComponent) block;
                if (isVideoMode && comp.getVideos().size() == 0)
                    continue;
                if (comp.isContainer()) {
                    SectionRow header = new SectionRow(SectionRow.SECTION, comp);
                    mData.add(header);
                    for (IBlock childBlock : comp.getChildren()) {
                        CourseComponent child = (CourseComponent) childBlock;
                        if (isVideoMode && child.getVideos().size() == 0)
                            continue;
                        SectionRow row = new SectionRow(SectionRow.ITEM, false, child);
                        mData.add(row);
                    }
                } else {
                    SectionRow row = new SectionRow(SectionRow.ITEM, true, comp);
                    mData.add(row);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void reloadData() {
        if (this.rootComponent != null)
            setData(this.rootComponent);
    }

    public View getRowView(int position, View convertView) {
        final SectionRow row = this.getItem(position);
        final SectionRow nextRow = this.getItem(position + 1);
        final CourseComponent component = row.component;
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        if (nextRow == null) {
            viewHolder.halfSeparator.setVisibility(View.GONE);
            viewHolder.wholeSeparator.setVisibility(View.VISIBLE);
        } else {
            viewHolder.wholeSeparator.setVisibility(View.GONE);
            boolean isLastChildInBlock = !row.component.getParent().getId().equals(nextRow.component.getParent().getId());
            if (isLastChildInBlock) {
                viewHolder.halfSeparator.setVisibility(View.GONE);
            } else {
                viewHolder.halfSeparator.setVisibility(View.VISIBLE);
            }
        }

        viewHolder.rowType.setVisibility(View.GONE);
        viewHolder.rowSubtitleIcon.setVisibility(View.GONE);
        viewHolder.rowSubtitle.setVisibility(View.GONE);
        viewHolder.rowSubtitleDueDate.setVisibility(View.GONE);
        viewHolder.rowSubtitlePanel.setVisibility(View.GONE);
        viewHolder.numOfVideoAndDownloadArea.setVisibility(View.GONE);

        if (component.isContainer()) {
            getRowViewForContainer(viewHolder, row);
        } else {
            getRowViewForLeaf(viewHolder, row);
        }
        return convertView;
    }

    public View getRowView2(int position, View convertView) {
        Log.d("POSITION" , position+"");
        final SectionRow row = this.getItem(position);
        final CourseComponent c = row.component;
        final ViewHolder2 viewHolder2 = (ViewHolder2) convertView.getTag();
        viewHolder2.subSectionTitleTV.setText(c.getDisplayName());
//        viewHolder2.subSectionDescriptionTV.setText(c.getInternalName());

        if(! c.isContainer())
        {
            //this is a block
            getRowViewForLeaf2(viewHolder2 , row);

        }
        else{
            getRowViewForContainer2(viewHolder2, row, position);
        }

        //Todo check content availability type and set download/downloaded icons accordingly
        return convertView;
    }

    private void getRowViewForContainer2(ViewHolder2 viewHolder2, final SectionRow row, int position) {
        final CourseComponent c = row.component;
        viewHolder2.blockTypeIcon.setVisibility(View.GONE);
        List<IBlock> blocks = c.getChildren();
        viewHolder2.courseAvailabilityStatusIcon.setImageResource(R.drawable.ic_download);
        if(blocks != null && blocks.size() > 0) {
            viewHolder2.subSectionTitleTV.setText(blocks.get(0).getDisplayName());
            if (blocks.size() == 1) {
                viewHolder2.subSectionDescriptionTV.setVisibility(View.GONE);
                viewHolder2.multipleItemsCV.setVisibility(View.GONE);
            } else {
                viewHolder2.subSectionDescriptionTV.setText("+" + (blocks.size() - 1) + " Sub Topics");
                viewHolder2.multipleItemsCV.setVisibility(View.VISIBLE);
            }
        }

        if(lastAccessedPosition !=-1 && position < lastAccessedPosition)
        {
            viewHolder2.subSectionTitleTV.setTextColor(ContextCompat.getColor(context , R.color.dark_blue));
            viewHolder2.timelineViewMarker.setMarkerSize(20);
//            viewHolder2.subSectionDescriptionTV.setTextColor(Color.BLACK);
        }

        if(lastAccessedId!= null && !lastAccessedId.isEmpty() && lastAccessedId.equals(c.getId()))
        {
            viewHolder2.timelineViewMarker.setMarkerSize(35);
//            viewHolder2.subSectionTitleTV.setTextSize(18);
            viewHolder2.subSectionTitleTV.setTextColor(ContextCompat.getColor(context , R.color.dark_blue));
            viewHolder2.subSectionTitleTV.setTypeface(null , Typeface.BOLD);
            hasLastAccessedShown = true;
            lastAccessedPosition = position;
        }
        else if(!hasLastAccessedShown)
        {
            viewHolder2.subSectionTitleTV.setTextColor(ContextCompat.getColor(context , R.color.dark_blue));
            viewHolder2.timelineViewMarker.setMarkerSize(20);
//            viewHolder2.subSectionDescriptionTV.setTextColor(Color.BLACK);
        }
//            else{
//                viewHolder2.subSectionTitleTV.setTextColor(Color.BLACK);
//                viewHolder2.timelineViewMarker.setMarkerSize(35);
//                viewHolder2.subSectionDescriptionTV.setTextColor(Color.BLACK);
//
//            }
//            if(lastAccessedId != null && !lastAccessedId.isEmpty())
//            {
//                viewHolder2.subSectionTitleTV.setTextColor(Color.GREEN);
//            }
//            else
//            {
//                viewHolder2.subSectionTitleTV.setTextColor(Color.BLACK);
//            }
//            viewHolder2.timelineViewMarker.set
//            viewHolder2.timelineViewMarker.setMarkerColor(ContextCompat.getColor(context , R.color.edx_brand_primary_base));
//            viewHolder2.timelineViewMarker.setActivated(true);
        viewHolder2.timelineViewMarker.setMarker(ContextCompat.getDrawable(context, R.drawable.ic_timeline_marker_filled));
        int markerType = getTypeForTimelineMarker(position);
        viewHolder2.timelineViewMarker.initLine(markerType);
//        viewHolder2.courseAvailabilityStatusIcon.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Toast.makeText(context, "Download button clicked"+ c.getDisplayName(), Toast.LENGTH_SHORT).show();
//            }
//        });



//        String courseId = component.getCourseId();
//        BlockPath path = component.getPath();
//        //FIXME - we should add a new column in database - pathinfo.
//        //then do the string match to get the record
//        String chapterId = path.get(1) == null ? "" : path.get(1).getDisplayName();
//        String sequentialId = path.get(2) == null ? "" : path.get(2).getDisplayName();
//
//        holder.rowTitle.setText(component.getDisplayName());
//        holder.numOfVideoAndDownloadArea.setVisibility(View.VISIBLE);
//        if (component.isGraded()) {
//            holder.bulkDownload.setVisibility(View.INVISIBLE);
//            holder.rowSubtitlePanel.setVisibility(View.VISIBLE);
//            holder.rowSubtitleIcon.setVisibility(View.VISIBLE);
//            holder.rowSubtitle.setVisibility(View.VISIBLE);
//            holder.rowSubtitle.setText(component.getFormat());
//            holder.rowSubtitle.setTypeface(holder.rowSubtitle.getTypeface(), Typeface.BOLD);
//            holder.rowSubtitle.setTextColor(ContextCompat.getColor(context,
//                    R.color.edx_brand_gray_dark));
//            if (!TextUtils.isEmpty(component.getDueDate())) {
//                try {
//                    holder.rowSubtitleDueDate.setText(getFormattedDueDate(component.getDueDate()));
//                    holder.rowSubtitleDueDate.setVisibility(View.VISIBLE);
//                } catch (IllegalArgumentException e) {
//                    logger.error(e);
//                }
//            }
//        }
//
//        final int totalDownloadableVideos = component.getDownloadableVideosCount();
//        // support video download for video type excluding the ones only viewable on web
//        if (totalDownloadableVideos == 0) {
//            holder.numOfVideoAndDownloadArea.setVisibility(View.GONE);
//        } else {
//            holder.bulkDownload.setVisibility(View.VISIBLE);
//            holder.noOfVideos.setVisibility(View.VISIBLE);
//            holder.noOfVideos.setText("" + totalDownloadableVideos);
//
//            Integer downloadedCount = dbStore.getDownloadedVideosCountForSection(courseId,
//                    chapterId, sequentialId, null);
//
//            if (downloadedCount == totalDownloadableVideos) {
//                holder.noOfVideos.setVisibility(View.VISIBLE);
//                setRowStateOnDownload(holder, DownloadEntry.DownloadedState.DOWNLOADED, null);
//            } else if (dbStore.getDownloadingVideosCountForSection(courseId, chapterId,
//                    sequentialId, null) + downloadedCount == totalDownloadableVideos) {
//                holder.noOfVideos.setVisibility(View.GONE);
//                setRowStateOnDownload(holder, DownloadEntry.DownloadedState.DOWNLOADING,
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View downloadView) {
//                                mDownloadListener.viewDownloadsStatus();
//                            }
//                        });
//            } else {
//                holder.noOfVideos.setVisibility(View.VISIBLE);
//                setRowStateOnDownload(holder, DownloadEntry.DownloadedState.ONLINE,
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View downloadView) {
//                                mDownloadListener.download(component.getVideos());
//                            }
//                        });
//            }
//        }
    }
    private void getRowViewForLeaf2(ViewHolder2 viewHolder2,
                                    final SectionRow row) {
        final CourseComponent c = row.component;

        viewHolder2.subSectionDescriptionTV.setVisibility(View.GONE);
        viewHolder2.timelineViewMarker.setVisibility(View.GONE);
        viewHolder2.blockTypeIcon.setVisibility(View.VISIBLE);
        viewHolder2.subSectionTitleTV.setTextColor(ContextCompat.getColor(context , R.color.dark_blue));
        //TODO decide which type of block(text,audio,video) it is & set icon accordingly.
        if (row.component instanceof VideoBlockModel) {
            viewHolder2.blockTypeIcon.setIcon(FontAwesomeIcons.fa_film);

            final DownloadEntry videoData = ((VideoBlockModel) row.component).getDownloadEntry(storage);
            if (null != videoData) {
                updateUIForVideo2(viewHolder2, videoData);
                return;
            }
        }
        if (row.component instanceof HtmlBlockModel) {
            viewHolder2.blockTypeIcon.setIcon(FontAwesomeIcons.fa_text_height);
            viewHolder2.courseAvailabilityStatusIcon.setVisibility(View.GONE);
        }
        if (config.isDiscussionsEnabled() && row.component instanceof DiscussionBlockModel) {
            viewHolder2.blockTypeIcon.setIcon(FontAwesomeIcons.fa_comments_o);
            viewHolder2.courseAvailabilityStatusIcon.setVisibility(View.GONE);
            checkAccessStatus2(viewHolder2, c);

            //            checkAccessStatus(viewHolder, unit);
        } else if (!c.isMultiDevice()) {
            // If we reach here & the type is VIDEO, it means the video is webOnly
            viewHolder2.courseAvailabilityStatusIcon.setVisibility(View.GONE);
            viewHolder2.blockTypeIcon.setIcon(FontAwesomeIcons.fa_laptop);
//            viewHolder.bulkDownload.setVisibility(View.INVISIBLE);
//            viewHolder.rowType.setIcon(FontAwesomeIcons.fa_laptop);
//            viewHolder.rowType.setIconColorResource(R.color.edx_brand_gray_accent);
        }
        else {
            viewHolder2.courseAvailabilityStatusIcon.setVisibility(View.INVISIBLE);
            if (c.getType() == BlockType.PROBLEM) {
                viewHolder2.blockTypeIcon.setIcon(FontAwesomeIcons.fa_list);
            }
// else {
//                viewHolder2.blockTypeIcon.setIcon(FontAwesomeIcons.fa_file_o);
//            }
            checkAccessStatus2(viewHolder2, c);
        }


        //        final CourseComponent unit = row.component;
//        viewHolder.rowType.setVisibility(View.VISIBLE);
//        viewHolder.rowSubtitleIcon.setVisibility(View.GONE);
//        viewHolder.rowSubtitleDueDate.setVisibility(View.GONE);
//        viewHolder.rowSubtitle.setVisibility(View.GONE);
//        viewHolder.rowSubtitlePanel.setVisibility(View.GONE);
//        viewHolder.bulkDownload.setVisibility(View.INVISIBLE);
//        viewHolder.rowTitle.setText(unit.getDisplayName());
//
//        if (row.component instanceof VideoBlockModel) {
//            final DownloadEntry videoData = ((VideoBlockModel) row.component).getDownloadEntry(storage);
//            if (null != videoData) {
//                updateUIForVideo(viewHolder, videoData);
//                return;
//            }
//        }
//        if (config.isDiscussionsEnabled() && row.component instanceof DiscussionBlockModel) {
//            viewHolder.rowType.setIcon(FontAwesomeIcons.fa_comments_o);
//            checkAccessStatus(viewHolder, unit);
//        } else if (!unit.isMultiDevice()) {
//            // If we reach here & the type is VIDEO, it means the video is webOnly
//            viewHolder.bulkDownload.setVisibility(View.INVISIBLE);
//            viewHolder.rowType.setIcon(FontAwesomeIcons.fa_laptop);
//            viewHolder.rowType.setIconColorResource(R.color.edx_brand_gray_accent);
//        } else {
//            viewHolder.bulkDownload.setVisibility(View.INVISIBLE);
//            if (unit.getType() == BlockType.PROBLEM) {
//                viewHolder.rowType.setIcon(FontAwesomeIcons.fa_list);
//            } else {
//                viewHolder.rowType.setIcon(FontAwesomeIcons.fa_file_o);
//            }
//            checkAccessStatus(viewHolder, unit);
//        }
    }


    private int getTypeForTimelineMarker(int position)
    {
        final SectionRow row = this.getItem(position);

        int typeToReturn = LineType.NORMAL;

        SectionRow previousRow = this.getItem(position-1);
        SectionRow nextSectionRow = this.getItem(position+1);

        if(previousRow == null)
        {
            return 0;
        }
        if(nextSectionRow == null)
        {
            typeToReturn =  LineType.END;
        }
        else {
            typeToReturn = LineType.NORMAL;

        }

        //Enabling below code if you want only marker and no lines if there single item

//        if(previousRow.type == SectionRow.SECTION && nextSectionRow.type == SectionRow.ITEM)
//        {
//            typeToReturn = LineType.BEGIN;
//        }
//        else if(previousRow.type == SectionRow.SECTION && nextSectionRow.type == SectionRow.SECTION)
//        {
//            typeToReturn = LineType.ONLYONE;
//        }
//        else if (previousRow.type == SectionRow.ITEM && nextSectionRow.type == SectionRow.ITEM)
//        {
//            typeToReturn = LineType.NORMAL;
//        }

        return typeToReturn;
    }


    private void getRowViewForLeaf(ViewHolder viewHolder,
                                   final SectionRow row) {
        final CourseComponent unit = row.component;
        viewHolder.rowType.setVisibility(View.VISIBLE);
        viewHolder.rowSubtitleIcon.setVisibility(View.GONE);
        viewHolder.rowSubtitleDueDate.setVisibility(View.GONE);
        viewHolder.rowSubtitle.setVisibility(View.GONE);
        viewHolder.rowSubtitlePanel.setVisibility(View.GONE);
        viewHolder.bulkDownload.setVisibility(View.INVISIBLE);
        viewHolder.rowTitle.setText(unit.getDisplayName());

        if (row.component instanceof VideoBlockModel) {
            final DownloadEntry videoData = ((VideoBlockModel) row.component).getDownloadEntry(storage);
            if (null != videoData) {
                updateUIForVideo(viewHolder, videoData);
                return;
            }
        }
        if (config.isDiscussionsEnabled() && row.component instanceof DiscussionBlockModel) {
            viewHolder.rowType.setIcon(FontAwesomeIcons.fa_comments_o);
            checkAccessStatus(viewHolder, unit);
        } else if (!unit.isMultiDevice()) {
            // If we reach here & the type is VIDEO, it means the video is webOnly
            viewHolder.bulkDownload.setVisibility(View.INVISIBLE);
            viewHolder.rowType.setIcon(FontAwesomeIcons.fa_laptop);
            viewHolder.rowType.setIconColorResource(R.color.edx_brand_gray_accent);
        } else {
            viewHolder.bulkDownload.setVisibility(View.INVISIBLE);
            if (unit.getType() == BlockType.PROBLEM) {
                viewHolder.rowType.setIcon(FontAwesomeIcons.fa_list);
            } else {
                viewHolder.rowType.setIcon(FontAwesomeIcons.fa_file_o);
            }
            checkAccessStatus(viewHolder, unit);
        }
    }

    private void checkAccessStatus(final ViewHolder viewHolder, final CourseComponent unit) {
        dbStore.isUnitAccessed(new DataCallback<Boolean>(true) {
            @Override
            public void onResult(Boolean accessed) {
                if (accessed) {
                    viewHolder.rowType.setIconColorResource(R.color.edx_brand_gray_accent);
                } else {
                    viewHolder.rowType.setIconColorResource(R.color.edx_brand_primary_base);
                }
            }

            @Override
            public void onFail(Exception ex) {
                logger.error(ex);
            }
        }, unit.getId());
    }
    private void checkAccessStatus2(final ViewHolder2 viewHolder, final CourseComponent unit) {
        dbStore.isUnitAccessed(new DataCallback<Boolean>(true) {
            @Override
            public void onResult(Boolean accessed) {
                if (accessed) {
                    viewHolder.blockTypeIcon.setIconColorResource(R.color.edx_brand_gray_accent);
                } else {
                    viewHolder.blockTypeIcon.setIconColorResource(R.color.edx_brand_primary_base);
                }
            }

            @Override
            public void onFail(Exception ex) {
                logger.error(ex);
            }
        }, unit.getId());
    }

    private void updateUIForVideo(@NonNull final ViewHolder viewHolder, @NonNull final DownloadEntry videoData) {
        viewHolder.rowType.setIcon(FontAwesomeIcons.fa_film);
        viewHolder.numOfVideoAndDownloadArea.setVisibility(View.VISIBLE);
        viewHolder.bulkDownload.setVisibility(View.VISIBLE);
        viewHolder.rowSubtitlePanel.setVisibility(View.VISIBLE);
        if (videoData.getDuration() > 0L) {
            viewHolder.rowSubtitle.setVisibility(View.VISIBLE);
            viewHolder.rowSubtitle.setText(videoData.getDurationReadable());
        }
        if (videoData.getSize() > 0L) {
            viewHolder.rowSubtitleDueDate.setVisibility(View.VISIBLE);
            viewHolder.rowSubtitleDueDate.setText(MemoryUtil.format(context, videoData.getSize()));
            // Set appropriate right margin of subtitle
            final int rightMargin = (int) context.getResources().getDimension(R.dimen.widget_margin_double);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                    viewHolder.rowSubtitle.getLayoutParams();
            params.setMargins(0, 0, rightMargin, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.setMarginEnd(rightMargin);
            }
        }

        dbStore.getWatchedStateForVideoId(videoData.videoId,
                new DataCallback<DownloadEntry.WatchedState>(true) {
                    @Override
                    public void onResult(DownloadEntry.WatchedState result) {
                        if (result != null && result == DownloadEntry.WatchedState.WATCHED) {
                            viewHolder.rowType.setIconColorResource(R.color.edx_brand_gray_accent);
                        } else {
                            viewHolder.rowType.setIconColorResource(R.color.edx_brand_primary_base);
                        }
                    }

                    @Override
                    public void onFail(Exception ex) {
                        logger.error(ex);
                    }
                });

        if (videoData.isVideoForWebOnly()) {
            viewHolder.numOfVideoAndDownloadArea.setVisibility(View.GONE);
        } else {
            viewHolder.numOfVideoAndDownloadArea.setVisibility(View.VISIBLE);
            dbStore.getDownloadedStateForVideoId(videoData.videoId,
                    new DataCallback<DownloadEntry.DownloadedState>(true) {
                        @Override
                        public void onResult(DownloadEntry.DownloadedState state) {
                            if (state == null || state == DownloadEntry.DownloadedState.ONLINE) {
                                // not yet downloaded
                                setRowStateOnDownload(viewHolder, DownloadEntry.DownloadedState.ONLINE,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mDownloadListener.download(videoData);
                                            }
                                        });
                            } else if (state == DownloadEntry.DownloadedState.DOWNLOADING) {
                                // may be download in progress
                                setRowStateOnDownload(viewHolder, DownloadEntry.DownloadedState.DOWNLOADING,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mDownloadListener.viewDownloadsStatus();
                                            }
                                        });
                            } else if (state == DownloadEntry.DownloadedState.DOWNLOADED) {
                                setRowStateOnDownload(viewHolder, DownloadEntry.DownloadedState.DOWNLOADED, null);
                            }
                        }

                        @Override
                        public void onFail(Exception ex) {
                            logger.error(ex);
                            viewHolder.bulkDownload.setVisibility(View.VISIBLE);
                        }
                    });
        }

    }

    private void updateUIForVideo2(@NonNull final ViewHolder2 viewHolder, @NonNull final DownloadEntry videoData) {
//        viewHolder.subSectionDescriptionTV.setVisibility(View.VISIBLE);

//        viewHolder.rowType.setIcon(FontAwesomeIcons.fa_film);
//        viewHolder.numOfVideoAndDownloadArea.setVisibility(View.VISIBLE);
//        viewHolder.bulkDownload.setVisibility(View.VISIBLE);
//        viewHolder.rowSubtitlePanel.setVisibility(View.VISIBLE);
        if (videoData.getDuration() > 0L) {
            viewHolder.subSectionDescriptionTV.setVisibility(View.VISIBLE);
            viewHolder.subSectionDescriptionTV.setText(videoData.getDurationReadable());
        }
        if (videoData.getSize() > 0L) {
            viewHolder.subSectionDescriptionTV.setText(viewHolder.subSectionDescriptionTV.getText().toString()+" | "+ MemoryUtil.format(context, videoData.getSize()));
//            viewHolder.rowSubtitleDueDate.setVisibility(View.VISIBLE);
//            viewHolder.rowSubtitleDueDate.setText(MemoryUtil.format(context, videoData.getSize()));
//            // Set appropriate right margin of subtitle
//            final int rightMargin = (int) context.getResources().getDimension(R.dimen.widget_margin_double);
//            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
//                    viewHolder.rowSubtitle.getLayoutParams();
//            params.setMargins(0, 0, rightMargin, 0);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//                params.setMarginEnd(rightMargin);
//            }
        }

        dbStore.getWatchedStateForVideoId(videoData.videoId,
                new DataCallback<DownloadEntry.WatchedState>(true) {
                    @Override
                    public void onResult(DownloadEntry.WatchedState result) {
                        if (result != null && result == DownloadEntry.WatchedState.WATCHED) {
                            viewHolder.blockTypeIcon.setIconColorResource(R.color.edx_brand_gray_accent);
                        } else {
                            viewHolder.blockTypeIcon.setIconColorResource(R.color.edx_brand_primary_base);
                        }
                    }

                    @Override
                    public void onFail(Exception ex) {
                        logger.error(ex);
                    }
                });

        if (videoData.isVideoForWebOnly()) {
            viewHolder.courseAvailabilityStatusIcon.setVisibility(View.GONE);
//            viewHolder.numOfVideoAndDownloadArea.setVisibility(View.GONE);
        } else {
            viewHolder.courseAvailabilityStatusIcon.setVisibility(View.VISIBLE);
//            viewHolder.numOfVideoAndDownloadArea.setVisibility(View.VISIBLE);
            dbStore.getDownloadedStateForVideoId(videoData.videoId,
                    new DataCallback<DownloadEntry.DownloadedState>(true) {
                        @Override
                        public void onResult(DownloadEntry.DownloadedState state) {
                            if (state == null || state == DownloadEntry.DownloadedState.ONLINE) {
                                // not yet downloaded
//                                setRowStateOnDownload(viewHolder, DownloadEntry.DownloadedState.ONLINE,
//                                        new View.OnClickListener() {
//                                            @Override
//                                            public void onClick(View v) {
//                                                mDownloadListener.download(videoData);
//                                            }
//                                        });
                                setRowStateOnDownload2(viewHolder, DownloadEntry.DownloadedState.ONLINE,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mDownloadListener.download(videoData);
                                            }
                                        });

                            } else if (state == DownloadEntry.DownloadedState.DOWNLOADING) {
                                // may be download in progress
                                setRowStateOnDownload2(viewHolder, DownloadEntry.DownloadedState.DOWNLOADING,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                mDownloadListener.viewDownloadsStatus();
                                            }
                                        });

//                                setRowStateOnDownload(viewHolder, DownloadEntry.DownloadedState.DOWNLOADING,
//                                        new View.OnClickListener() {
//                                            @Override
//                                            public void onClick(View v) {
//                                                mDownloadListener.viewDownloadsStatus();
//                                            }
//                                        });
                            } else if (state == DownloadEntry.DownloadedState.DOWNLOADED) {
                                setRowStateOnDownload2(viewHolder, DownloadEntry.DownloadedState.DOWNLOADED, null);

//                                setRowStateOnDownload(viewHolder, DownloadEntry.DownloadedState.DOWNLOADED, null);
                            }
                        }

                        @Override
                        public void onFail(Exception ex) {
                            logger.error(ex);
                            viewHolder.courseAvailabilityStatusIcon.setVisibility(View.VISIBLE);

                            //                            viewHolder.bulkDownload.setVisibility(View.VISIBLE);
                        }
                    });
        }

    }

    private void getRowViewForContainer(ViewHolder holder,
                                        final SectionRow row) {
        final CourseComponent component = row.component;
        String courseId = component.getCourseId();
        BlockPath path = component.getPath();
        //FIXME - we should add a new column in database - pathinfo.
        //then do the string match to get the record
        String chapterId = path.get(1) == null ? "" : path.get(1).getDisplayName();
        String sequentialId = path.get(2) == null ? "" : path.get(2).getDisplayName();

        holder.rowTitle.setText(component.getDisplayName());
        holder.numOfVideoAndDownloadArea.setVisibility(View.VISIBLE);
        if (component.isGraded()) {
            holder.bulkDownload.setVisibility(View.INVISIBLE);
            holder.rowSubtitlePanel.setVisibility(View.VISIBLE);
            holder.rowSubtitleIcon.setVisibility(View.VISIBLE);
            holder.rowSubtitle.setVisibility(View.VISIBLE);
            holder.rowSubtitle.setText(component.getFormat());
            holder.rowSubtitle.setTypeface(holder.rowSubtitle.getTypeface(), Typeface.BOLD);
            holder.rowSubtitle.setTextColor(ContextCompat.getColor(context,
                    R.color.edx_brand_gray_dark));
            if (!TextUtils.isEmpty(component.getDueDate())) {
                try {
                    holder.rowSubtitleDueDate.setText(getFormattedDueDate(component.getDueDate()));
                    holder.rowSubtitleDueDate.setVisibility(View.VISIBLE);
                } catch (IllegalArgumentException e) {
                    logger.error(e);
                }
            }
        }

        final int totalDownloadableVideos = component.getDownloadableVideosCount();
        // support video download for video type excluding the ones only viewable on web
        if (totalDownloadableVideos == 0) {
            holder.numOfVideoAndDownloadArea.setVisibility(View.GONE);
        } else {
            holder.bulkDownload.setVisibility(View.VISIBLE);
            holder.noOfVideos.setVisibility(View.VISIBLE);
            holder.noOfVideos.setText("" + totalDownloadableVideos);

            Integer downloadedCount = dbStore.getDownloadedVideosCountForSection(courseId,
                    chapterId, sequentialId, null);

            if (downloadedCount == totalDownloadableVideos) {
                holder.noOfVideos.setVisibility(View.VISIBLE);
                setRowStateOnDownload(holder, DownloadEntry.DownloadedState.DOWNLOADED, null);
            } else if (dbStore.getDownloadingVideosCountForSection(courseId, chapterId,
                    sequentialId, null) + downloadedCount == totalDownloadableVideos) {
                holder.noOfVideos.setVisibility(View.GONE);
                setRowStateOnDownload(holder, DownloadEntry.DownloadedState.DOWNLOADING,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View downloadView) {
                                mDownloadListener.viewDownloadsStatus();
                            }
                        });
            } else {
                holder.noOfVideos.setVisibility(View.VISIBLE);
                setRowStateOnDownload(holder, DownloadEntry.DownloadedState.ONLINE,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View downloadView) {
                                mDownloadListener.download(component.getVideos());
                            }
                        });
            }
        }
    }


    private String getFormattedDueDate(final String date) throws IllegalArgumentException {
        final SimpleDateFormat dateFormat;
        final Date dueDate = DateUtil.convertToDate(date);
        if (android.text.format.DateUtils.isToday(dueDate.getTime())) {
            dateFormat = new SimpleDateFormat("HH:mm");
            String formattedDate = ResourceUtil.getFormattedString(context.getResources(), R.string.due_date_today,
                    "due_date", dateFormat.format(dueDate)).toString();
            formattedDate += " " + TimeZoneUtils.getTimeZoneAbbreviation(TimeZone.getDefault());
            return formattedDate;
        } else {
            dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            return ResourceUtil.getFormattedString(context.getResources(), R.string.due_date_past_future,
                    "due_date", dateFormat.format(dueDate)).toString();
        }
    }

    /**
     * Makes various changes to the row based on a video element's download status
     *
     * @param row      ViewHolder of the row view
     * @param state    current state of video download
     * @param listener the listener to attach to the video download button
     */
    private void setRowStateOnDownload(ViewHolder row, DownloadEntry.DownloadedState state
            , View.OnClickListener listener) {
        switch (state) {
            case DOWNLOADING:
                row.bulkDownload.setIcon(FontAwesomeIcons.fa_spinner);
                row.bulkDownload.setIconAnimation(Animation.PULSE);
                row.bulkDownload.setIconColorResource(R.color.edx_brand_primary_base);
                break;
            case DOWNLOADED:
                row.bulkDownload.setImageResource(R.drawable.ic_done);
                row.bulkDownload.setIconAnimation(Animation.NONE);
                row.bulkDownload.setIconColorResource(R.color.edx_brand_gray_accent);
                break;
            case ONLINE:
                row.bulkDownload.setIcon(FontAwesomeIcons.fa_download);
                row.bulkDownload.setIconAnimation(Animation.NONE);
                row.bulkDownload.setIconColorResource(R.color.edx_brand_gray_accent);
                break;
        }
        row.numOfVideoAndDownloadArea.setOnClickListener(listener);
        if (listener == null) {
            row.numOfVideoAndDownloadArea.setClickable(false);
        }
    }

    private void setRowStateOnDownload2(ViewHolder2 row, DownloadEntry.DownloadedState state
            , View.OnClickListener listener) {
        switch (state) {
            case DOWNLOADING:
                row.courseAvailabilityStatusIcon.setIcon(FontAwesomeIcons.fa_spinner);
                row.courseAvailabilityStatusIcon.setIconAnimation(Animation.PULSE);
                row.courseAvailabilityStatusIcon.setIconColorResource(R.color.white);

//                row.bulkDownload.setIcon(FontAwesomeIcons.fa_spinner);
//                row.bulkDownload.setIconAnimation(Animation.PULSE);
//                row.bulkDownload.setIconColorResource(R.color.edx_brand_primary_base);
                break;
            case DOWNLOADED:
                row.courseAvailabilityStatusIcon.setImageResource(R.drawable.ic_done);
                row.courseAvailabilityStatusIcon.setIconAnimation(Animation.NONE);
                row.courseAvailabilityStatusIcon.setIconColorResource(R.color.white);
                row.courseAvailabilityStatusIcon.setTag("downloaded");
                //                row.bulkDownload.setIcon(FontAwesomeIcons.fa_check);
//                row.bulkDownload.setIconAnimation(Animation.NONE);
//                row.bulkDownload.setIconColorResource(R.color.edx_brand_gray_accent);
                break;
            case ONLINE:
                row.courseAvailabilityStatusIcon.setIcon(FontAwesomeIcons.fa_download);
                row.courseAvailabilityStatusIcon.setIconAnimation(Animation.NONE);
                row.courseAvailabilityStatusIcon.setIconColorResource(R.color.white);

//                row.bulkDownload.setIcon(FontAwesomeIcons.fa_download);
//                row.bulkDownload.setIconAnimation(Animation.NONE);
//                row.bulkDownload.setIconColorResource(R.color.edx_brand_gray_accent);
                break;
        }
        row.courseAvailabilityStatusIcon.setOnClickListener(listener);
        if (listener == null) {
            row.courseAvailabilityStatusIcon.setClickable(false);
        }
    }

    public View getHeaderView(int position, View convertView) {
        final SectionRow row = this.getItem(position);
        TextView titleView = (TextView) convertView.findViewById(R.id.row_header);
        View separator = convertView.findViewById(R.id.row_separator);
        titleView.setText(row.component.getDisplayName());
        if (position == 0) {
            separator.setVisibility(View.GONE);
        } else {
            separator.setVisibility(View.VISIBLE);
        }
        return convertView;
    }

    public View getHeaderView2(int position, View convertView) {
        final SectionRow row = this.getItem(position);
        TextView titleView = (TextView) convertView.findViewById(R.id.section_title);
        titleView.setText(row.component.getDisplayName());
        return convertView;
    }

    //    public ViewHolder getTag(View convertView) {
//        ViewHolder holder = new ViewHolder();
//        holder.rowType = (IconImageView) convertView
//                .findViewById(R.id.row_type);
//        holder.rowTitle = (TextView) convertView
//                .findViewById(R.id.row_title);
//        holder.rowSubtitle = (TextView) convertView
//                .findViewById(R.id.row_subtitle);
//        holder.rowSubtitleDueDate = (TextView) convertView
//                .findViewById(R.id.row_subtitle_due_date);
//        holder.rowSubtitleIcon = (IconImageView) convertView
//                .findViewById(R.id.row_subtitle_icon);
//        holder.rowSubtitleIcon.setIconColorResource(R.color.edx_brand_primary_base);
//        holder.noOfVideos = (TextView) convertView
//                .findViewById(R.id.no_of_videos);
//        holder.bulkDownload = (IconImageView) convertView
//                .findViewById(R.id.bulk_download);
//        holder.bulkDownload.setIconColorResource(R.color.edx_brand_gray_accent);
//        holder.numOfVideoAndDownloadArea = (LinearLayout) convertView
//                .findViewById(R.id.bulk_download_layout);
//        holder.rowSubtitlePanel = convertView.findViewById(R.id.row_subtitle_panel);
//        holder.halfSeparator = convertView.findViewById(R.id.row_half_separator);
//        holder.wholeSeparator = convertView.findViewById(R.id.row_whole_separator);
//
//        // Accessibility
//        ViewCompat.setImportantForAccessibility(holder.rowSubtitle, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
//
//        return holder;
//    }
    public ViewHolder2 getTag2(View convertView) {
        ViewHolder2 holder = new ViewHolder2();
        holder.sectionTitleTV = (TextView)convertView.findViewById(R.id.section_title);
        holder.subSectionTitleTV = (TextView) convertView.findViewById(R.id.subsection_title_tv);
        holder.subSectionDescriptionTV = (TextView) convertView.findViewById(R.id.subsection_desription);
        holder.block1TitleTV = (TextView) convertView.findViewById(R.id.subsection_block_1_tv);
        holder.block2TitleTV = (TextView)convertView.findViewById(R.id.subsection_block_2_tv);
        holder.blockContainerLL = (LinearLayout) convertView.findViewById(R.id.subsection_blocks_container);
        holder.timelineViewMarker = (TimelineView) convertView.findViewById(R.id.subsection_timeline_marker);
//                    timelineViewMarker.initLine(viewType);
        holder.courseAvailabilityStatusIcon = (IconImageViewXml) convertView.findViewById(R.id.subsection_status_icon);
        holder.multipleItemsCV = (CardView) convertView.findViewById(R.id.multiple_items_cv);
        holder.blockTypeIcon = (IconImageViewXml) convertView.findViewById(R.id.block_type_icon);
        holder.titlesContainer = (RelativeLayout) convertView.findViewById(R.id.title_container);
        holder.blockContainer = (RelativeLayout) convertView.findViewById(R.id.block_container);
//        holder.rowType = (IconImageView) convertView
//                .findViewById(R.id.row_type);
//        holder.rowTitle = (TextView) convertView
//                .findViewById(R.id.row_title);
//        holder.rowSubtitle = (TextView) convertView
//                .findViewById(R.id.row_subtitle);
//        holder.rowSubtitleDueDate = (TextView) convertView
//                .findViewById(R.id.row_subtitle_due_date);
//        holder.rowSubtitleIcon = (IconImageView) convertView
//                .findViewById(R.id.row_subtitle_icon);
//        holder.rowSubtitleIcon.setIconColorResource(R.color.edx_brand_primary_base);
//        holder.noOfVideos = (TextView) convertView
//                .findViewById(R.id.no_of_videos);
//        holder.bulkDownload = (IconImageView) convertView
//                .findViewById(R.id.bulk_download);
//        holder.bulkDownload.setIconColorResource(R.color.edx_brand_gray_accent);
//        holder.numOfVideoAndDownloadArea = (LinearLayout) convertView
//                .findViewById(R.id.bulk_download_layout);
//        holder.rowSubtitlePanel = convertView.findViewById(R.id.row_subtitle_panel);
//        holder.halfSeparator = convertView.findViewById(R.id.row_half_separator);
//        holder.wholeSeparator = convertView.findViewById(R.id.row_whole_separator);
//
//        // Accessibility
//        ViewCompat.setImportantForAccessibility(holder.rowSubtitle, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);

        return holder;
    }

    public static class ViewHolder {
        IconImageView rowType;
        TextView rowTitle;
        TextView rowSubtitle;
        TextView rowSubtitleDueDate;
        IconImageView rowSubtitleIcon;
        IconImageView bulkDownload;
        TextView noOfVideos;
        LinearLayout numOfVideoAndDownloadArea;
        View rowSubtitlePanel;
        View halfSeparator;
        View wholeSeparator;
    }

    public static class ViewHolder2 {
        TextView subSectionTitleTV, sectionTitleTV, subSectionDescriptionTV, block1TitleTV, block2TitleTV;
        TimelineView timelineViewMarker;
        LinearLayout blockContainerLL;
        IconImageViewXml courseAvailabilityStatusIcon, blockTypeIcon;
        CardView multipleItemsCV;
        RelativeLayout blockContainer, titlesContainer;
//        IconImageView rowType;
//        TextView rowTitle;
//        TextView rowSubtitle;
//        TextView rowSubtitleDueDate;
//        IconImageView rowSubtitleIcon;
//        IconImageView bulkDownload;
//        TextView noOfVideos;
//        LinearLayout numOfVideoAndDownloadArea;
//        View rowSubtitlePanel;
//        View halfSeparator;
//        View wholeSeparator;
    }

    public static class SectionRow {
        public static final int ITEM = 0;
        public static final int SECTION = 1;

        public final int type;
        public final boolean topComponent;
        public final CourseComponent component;

        public SectionRow(int type, CourseComponent component) {
            this(type, false, component);
        }

        public SectionRow(int type, boolean topComponent, CourseComponent component) {
            this.type = type;
            this.topComponent = topComponent;
            this.component = component;
        }
    }

    public int getPositionByItemId(String itemId) {
        int size = getCount();
        for (int i = 0; i < size; i++) {
            if (getItem(i).component.getId().equals(itemId))
                return i;
        }
        return -1;
    }

    public void setLastAccessedId(String lastAccessedId)
    {
        this.lastAccessedId = lastAccessedId;
        lastAccessedPosition = -1;
        hasLastAccessedShown = false;
        notifyDataSetChanged();
    }
}
