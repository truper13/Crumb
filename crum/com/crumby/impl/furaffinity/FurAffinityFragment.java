package com.crumby.impl.furaffinity;

import android.view.ViewGroup;
import com.crumby.C0065R;
import com.crumby.impl.crumby.CrumbyGalleryFragment;
import com.crumby.lib.fragment.GalleryGridFragment;
import com.crumby.lib.fragment.producer.GalleryProducer;
import com.crumby.lib.router.SettingAttributes;
import com.crumby.lib.widget.firstparty.omnibar.OmniformContainer;
import java.util.regex.Pattern;

public class FurAffinityFragment extends GalleryGridFragment {
    public static final int ACCOUNT_LAYOUT = 2130903071;
    public static final String ACCOUNT_TYPE = "furaffinity";
    public static final String BASE_URL = "http://furaffinity.net";
    public static final int BREADCRUMB_ICON = 2130837588;
    public static final String BREADCRUMB_NAME = "furAffinity";
    public static final String DISPLAY_NAME = "furAffinity";
    public static final String REGEX_BASE;
    public static final String REGEX_URL;
    public static final String ROOT_NAME = "furaffinity.net";
    public static final int SEARCH_FORM_ID = 2130903094;
    public static final SettingAttributes SETTING_ATTRIBUTES;
    public static final boolean SUGGESTABLE = true;

    static {
        REGEX_BASE = "(?:http://www.|https://www.|https://|http://|www.)?(" + Pattern.quote(ROOT_NAME) + ")";
        REGEX_URL = REGEX_BASE + CrumbyGalleryFragment.REGEX_URL;
        SETTING_ATTRIBUTES = new SettingAttributes(INCLUDE_IN_HOME_FALSE);
    }

    protected int getHeaderLayout() {
        return C0065R.layout.omniform_container;
    }

    protected void setupHeaderLayout(ViewGroup header) {
        ((OmniformContainer) header).showAsInGrid(getImage().getLinkUrl());
    }

    public String getSearchPrefix() {
        return DISPLAY_NAME;
    }

    public String getSearchArgumentName() {
        return "q";
    }

    protected GalleryProducer createProducer() {
        return new FurAffinityProducer();
    }
}
