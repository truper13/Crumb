package com.squareup.picasso;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build.VERSION;
import android.os.Looper;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings.System;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import com.crumby.impl.crumby.UnsupportedUrlFragment;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.codec.CharEncoding;

final class Utils {
    static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    static final int DEFAULT_READ_TIMEOUT = 20000;
    private static final int KEY_PADDING = 50;
    static final StringBuilder MAIN_THREAD_KEY_BUILDER;
    private static final int MAX_DISK_CACHE_SIZE = 52428800;
    private static final int MIN_DISK_CACHE_SIZE = 5242880;
    static final String OWNER_DISPATCHER = "Dispatcher";
    static final String OWNER_HUNTER = "Hunter";
    static final String OWNER_MAIN = "Main";
    private static final String PICASSO_CACHE = "picasso-cache";
    static final String THREAD_IDLE_NAME = "Picasso-Idle";
    static final String THREAD_PREFIX = "Picasso-";
    static final String VERB_BATCHED = "batched";
    static final String VERB_CANCELED = "canceled";
    static final String VERB_CHANGED = "changed";
    static final String VERB_COMPLETED = "completed";
    static final String VERB_CREATED = "created";
    static final String VERB_DECODED = "decoded";
    static final String VERB_DELIVERED = "delivered";
    static final String VERB_ENQUEUED = "enqueued";
    static final String VERB_ERRORED = "errored";
    static final String VERB_EXECUTING = "executing";
    static final String VERB_IGNORED = "ignored";
    static final String VERB_JOINED = "joined";
    static final String VERB_REMOVED = "removed";
    static final String VERB_REPLAYING = "replaying";
    static final String VERB_RETRYING = "retrying";
    static final String VERB_TRANSFORMED = "transformed";
    private static final String WEBP_FILE_HEADER_RIFF = "RIFF";
    private static final int WEBP_FILE_HEADER_SIZE = 12;
    private static final String WEBP_FILE_HEADER_WEBP = "WEBP";

    @TargetApi(11)
    private static class ActivityManagerHoneycomb {
        private ActivityManagerHoneycomb() {
        }

        static int getLargeMemoryClass(ActivityManager activityManager) {
            return activityManager.getLargeMemoryClass();
        }
    }

    @TargetApi(12)
    private static class BitmapHoneycombMR1 {
        private BitmapHoneycombMR1() {
        }

        static int getByteCount(Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    }

    private static class OkHttpLoaderCreator {
        private OkHttpLoaderCreator() {
        }

        static Downloader create(Context context) {
            return new OkHttpDownloader(context);
        }
    }

    private static class PicassoThread extends Thread {
        public PicassoThread(Runnable r) {
            super(r);
        }

        public void run() {
            Process.setThreadPriority(10);
            super.run();
        }
    }

    static class PicassoThreadFactory implements ThreadFactory {
        PicassoThreadFactory() {
        }

        public Thread newThread(Runnable r) {
            return new PicassoThread(r);
        }
    }

    static {
        MAIN_THREAD_KEY_BUILDER = new StringBuilder();
    }

    private Utils() {
    }

    static int getBitmapBytes(Bitmap bitmap) {
        int result;
        if (VERSION.SDK_INT >= WEBP_FILE_HEADER_SIZE) {
            result = BitmapHoneycombMR1.getByteCount(bitmap);
        } else {
            result = bitmap.getRowBytes() * bitmap.getHeight();
        }
        if (result >= 0) {
            return result;
        }
        throw new IllegalStateException("Negative size: " + bitmap);
    }

    static void checkNotMain() {
        if (isMain()) {
            throw new IllegalStateException("Method call should not happen from the main thread.");
        }
    }

    static void checkMain() {
        if (!isMain()) {
            throw new IllegalStateException("Method call should happen from the main thread.");
        }
    }

    static boolean isMain() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    static String getLogIdsForHunter(BitmapHunter hunter) {
        return getLogIdsForHunter(hunter, UnsupportedUrlFragment.DISPLAY_NAME);
    }

    static String getLogIdsForHunter(BitmapHunter hunter, String prefix) {
        StringBuilder builder = new StringBuilder(prefix);
        Action action = hunter.getAction();
        if (action != null) {
            builder.append(action.request.logId());
        }
        List<Action> actions = hunter.getActions();
        if (actions != null) {
            int count = actions.size();
            for (int i = 0; i < count; i++) {
                if (i > 0 || action != null) {
                    builder.append(", ");
                }
                builder.append(((Action) actions.get(i)).request.logId());
            }
        }
        return builder.toString();
    }

    static void log(String owner, String verb, String logId) {
        log(owner, verb, logId, UnsupportedUrlFragment.DISPLAY_NAME);
    }

    static void log(String owner, String verb, String logId, String extras) {
        Log.d("Picasso", String.format("%1$-11s %2$-12s %3$s %4$s", new Object[]{owner, verb, logId, extras}));
    }

    static String createKey(Request data) {
        String result = createKey(data, MAIN_THREAD_KEY_BUILDER);
        MAIN_THREAD_KEY_BUILDER.setLength(0);
        return result;
    }

    static String createKey(Request data, StringBuilder builder) {
        if (data.uri != null) {
            String path = data.uri.toString();
            builder.ensureCapacity(path.length() + KEY_PADDING);
            builder.append(path);
        } else {
            builder.ensureCapacity(KEY_PADDING);
            builder.append(data.resourceId);
        }
        builder.append('\n');
        if (data.rotationDegrees != 0.0f) {
            builder.append("rotation:").append(data.rotationDegrees);
            if (data.hasRotationPivot) {
                builder.append('@').append(data.rotationPivotX).append('x').append(data.rotationPivotY);
            }
            builder.append('\n');
        }
        if (data.targetWidth != 0) {
            builder.append("resize:").append(data.targetWidth).append('x').append(data.targetHeight);
            builder.append('\n');
        }
        if (data.centerCrop) {
            builder.append("centerCrop\n");
        } else if (data.centerInside) {
            builder.append("centerInside\n");
        }
        if (data.transformations != null) {
            int count = data.transformations.size();
            for (int i = 0; i < count; i++) {
                builder.append(((Transformation) data.transformations.get(i)).key());
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    static void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }

    static boolean parseResponseSourceHeader(String header) {
        boolean z = true;
        if (header == null) {
            return false;
        }
        String[] parts = header.split(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, 2);
        if ("CACHE".equals(parts[0])) {
            return true;
        }
        if (parts.length == 1) {
            return false;
        }
        try {
            if (!("CONDITIONAL_CACHE".equals(parts[0]) && Integer.parseInt(parts[1]) == 304)) {
                z = false;
            }
            return z;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static Downloader createDefaultDownloader(Context context) {
        boolean okUrlFactory = false;
        try {
            Class.forName("com.squareup.okhttp.OkUrlFactory");
            okUrlFactory = true;
        } catch (ClassNotFoundException e) {
        }
        boolean okHttpClient = false;
        try {
            Class.forName("com.squareup.okhttp.OkHttpClient");
            okHttpClient = true;
        } catch (ClassNotFoundException e2) {
        }
        if (okHttpClient == okUrlFactory) {
            return okHttpClient ? OkHttpLoaderCreator.create(context) : new UrlConnectionDownloader(context);
        } else {
            throw new RuntimeException("Picasso detected an unsupported OkHttp on the classpath.\nTo use OkHttp with this version of Picasso, you'll need:\n1. com.squareup.okhttp:okhttp:1.6.0 (or newer)\n2. com.squareup.okhttp:okhttp-urlconnection:1.6.0 (or newer)\nNote that OkHttp 2.0.0+ is supported!");
        }
    }

    static File createDefaultCacheDir(Context context) {
        File cache = new File(context.getApplicationContext().getCacheDir(), PICASSO_CACHE);
        if (!cache.exists()) {
            cache.mkdirs();
        }
        return cache;
    }

    static long calculateDiskCacheSize(File dir) {
        long size = 5242880;
        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            size = (((long) statFs.getBlockCount()) * ((long) statFs.getBlockSize())) / 50;
        } catch (IllegalArgumentException e) {
        }
        return Math.max(Math.min(size, 52428800), 5242880);
    }

    static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) getService(context, "activity");
        boolean largeHeap = (context.getApplicationInfo().flags & AccessibilityEventCompat.TYPE_TOUCH_INTERACTION_START) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && VERSION.SDK_INT >= 11) {
            memoryClass = ActivityManagerHoneycomb.getLargeMemoryClass(am);
        }
        return (AccessibilityEventCompat.TYPE_TOUCH_INTERACTION_START * memoryClass) / 7;
    }

    static boolean isAirplaneModeOn(Context context) {
        if (System.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0) {
            return true;
        }
        return false;
    }

    static <T> T getService(Context context, String service) {
        return context.getSystemService(service);
    }

    static boolean hasPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == 0;
    }

    static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD];
        while (true) {
            int n = input.read(buffer);
            if (-1 == n) {
                return byteArrayOutputStream.toByteArray();
            }
            byteArrayOutputStream.write(buffer, 0, n);
        }
    }

    static boolean isWebPFile(InputStream stream) throws IOException {
        byte[] fileHeaderBytes = new byte[WEBP_FILE_HEADER_SIZE];
        if (stream.read(fileHeaderBytes, 0, WEBP_FILE_HEADER_SIZE) != WEBP_FILE_HEADER_SIZE) {
            return false;
        }
        if (WEBP_FILE_HEADER_RIFF.equals(new String(fileHeaderBytes, 0, 4, CharEncoding.US_ASCII)) && WEBP_FILE_HEADER_WEBP.equals(new String(fileHeaderBytes, 8, 4, CharEncoding.US_ASCII))) {
            return true;
        }
        return false;
    }

    static int getResourceId(Resources resources, Request data) throws FileNotFoundException {
        if (data.resourceId != 0 || data.uri == null) {
            return data.resourceId;
        }
        String pkg = data.uri.getAuthority();
        if (pkg == null) {
            throw new FileNotFoundException("No package provided: " + data.uri);
        }
        List<String> segments = data.uri.getPathSegments();
        if (segments == null || segments.isEmpty()) {
            throw new FileNotFoundException("No path segments: " + data.uri);
        } else if (segments.size() == 1) {
            try {
                return Integer.parseInt((String) segments.get(0));
            } catch (NumberFormatException e) {
                throw new FileNotFoundException("Last path segment is not a resource ID: " + data.uri);
            }
        } else if (segments.size() == 2) {
            return resources.getIdentifier((String) segments.get(1), (String) segments.get(0), pkg);
        } else {
            throw new FileNotFoundException("More than two path segments: " + data.uri);
        }
    }

    static Resources getResources(Context context, Request data) throws FileNotFoundException {
        if (data.resourceId != 0 || data.uri == null) {
            return context.getResources();
        }
        String pkg = data.uri.getAuthority();
        if (pkg == null) {
            throw new FileNotFoundException("No package provided: " + data.uri);
        }
        try {
            return context.getPackageManager().getResourcesForApplication(pkg);
        } catch (NameNotFoundException e) {
            throw new FileNotFoundException("Unable to obtain resources for package: " + data.uri);
        }
    }
}
