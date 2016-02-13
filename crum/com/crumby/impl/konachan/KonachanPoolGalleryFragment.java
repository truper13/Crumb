package com.crumby.impl.konachan;

import com.crumby.impl.BooruPoolGalleryProducer;
import com.crumby.lib.fragment.GalleryGridFragment;
import com.crumby.lib.fragment.GalleryViewerFragment;
import com.crumby.lib.fragment.producer.GalleryProducer;

public class KonachanPoolGalleryFragment extends GalleryGridFragment {
    public static final String BASE_URL = "http://konachan.com/pool/show/";
    public static final boolean BREADCRUMB_ALT_NAME = true;
    public static final int BREADCRUMB_ICON = 2130837613;
    public static final String BREADCRUMB_NAME = "pools";
    public static final Class BREADCRUMB_PARENT_CLASS;
    public static final String REGEX_BASE;
    public static final String REGEX_URL;
    public static final boolean SUGGESTABLE = true;

    static {
        REGEX_BASE = KonachanPoolFragment.REGEX_BASE + "/show/";
        REGEX_URL = REGEX_BASE + "([0-9]+)*";
        BREADCRUMB_PARENT_CLASS = KonachanPoolFragment.class;
    }

    protected GalleryProducer createProducer() {
        return new BooruPoolGalleryProducer(BASE_URL, KonachanFragment.class, REGEX_URL, KonachanFragment.BASE_URL, false);
    }

    public static String matchIdFromUrl(String url) {
        return GalleryViewerFragment.matchIdFromUrl(REGEX_URL, url);
    }
}
