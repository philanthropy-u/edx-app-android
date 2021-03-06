package org.edx.mobile.whatsnew;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.edx.mobile.R;
import org.edx.mobile.base.BaseFragment;
import org.edx.mobile.databinding.FragmentWhatsNewItemBinding;
import org.edx.mobile.util.UiUtil;

public class WhatsNewItemFragment extends BaseFragment {
    public static final String ARG_MODEL = "ARG_MODEL";

    private FragmentWhatsNewItemBinding binding;

    public static WhatsNewItemFragment newInstance(@NonNull WhatsNewItemModel model) {
        final WhatsNewItemFragment fragment = new WhatsNewItemFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_MODEL, model);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_whats_new_item, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();
        final WhatsNewItemModel model = args.getParcelable(ARG_MODEL);

        binding.title.setText(escapePlatformName(model.getTitle()));
        binding.messageTitle.setText(escapePlatformName(model.getMessageTitle()));
        binding.message.setText(escapePlatformName(model.getMessage()));

        if (!model.getImage().isEmpty()) {
            loadImage(model.getImage());
        } else {
            binding.image.setVisibility(View.INVISIBLE);
        }
    }

    private void loadImage(String imageName) {
        @DrawableRes
        final int imageRes = UiUtil.getDrawable(getContext(), imageName);
        Glide.with(binding.image.getContext())
                .load(imageRes)
                .fitCenter()
                .dontAnimate()
                .into(binding.image);
    }

    private String escapePlatformName(@NonNull String input) {
        return input.replaceAll("platform_name", getString(R.string.platform_name));
    }
}
