package org.edx.mobile.view.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.core.IEdxEnvironment;

import subtitleFile.Caption;

public class TranscriptAdapter extends BaseListAdapter<Caption> {
    @ColorInt
    private final int SELECTED_TRANSCRIPT_COLOR = ContextCompat.getColor(getContext(), R.color.custom_dark_gray);
    @ColorInt
    private final int UNSELECTED_TRANSCRIPT_COLOR = ContextCompat.getColor(getContext(), R.color.philu_light_grey);

    @Inject
    public TranscriptAdapter(Context context, IEdxEnvironment environment) {
        super(context, R.layout.row_transcript_item, environment);
    }

    @Override
    public void render(BaseViewHolder tag, Caption model) {
        final ViewHolder viewHolder = (ViewHolder) tag;
        String captionText = model.content;
        if (captionText.endsWith("<br />")) {
            captionText = captionText.substring(0, captionText.length() - 6);
        }
        if (Build.VERSION.SDK_INT >= 24) {
            captionText = Html.fromHtml(captionText, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            captionText = Html.fromHtml(captionText).toString();
        }
        viewHolder.transcriptTv.setText(captionText);
        final int position = getPosition(model);
        if (isSelected(position)) {
            viewHolder.transcriptTv.setTextColor(SELECTED_TRANSCRIPT_COLOR);
            viewHolder.transcriptTv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            viewHolder.transcriptTv.setTextColor(UNSELECTED_TRANSCRIPT_COLOR);
            viewHolder.transcriptTv.setTypeface(Typeface.DEFAULT);
        }
    }

    @Override
    public BaseViewHolder getTag(View convertView) {
        return new ViewHolder(convertView);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    private static class ViewHolder extends BaseViewHolder {
        final TextView transcriptTv;

        public ViewHolder(View convertView) {
            transcriptTv = (TextView) convertView.findViewById(R.id.transcript_item);
        }
    }
}
