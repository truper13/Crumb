package com.squareup.picasso;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.crumby.impl.crumby.UnsupportedUrlFragment;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

class Dispatcher {
    static final int AIRPLANE_MODE_CHANGE = 10;
    private static final int AIRPLANE_MODE_OFF = 0;
    private static final int AIRPLANE_MODE_ON = 1;
    private static final int BATCH_DELAY = 200;
    private static final String DISPATCHER_THREAD_NAME = "Dispatcher";
    static final int HUNTER_BATCH_COMPLETE = 8;
    static final int HUNTER_COMPLETE = 4;
    static final int HUNTER_DECODE_FAILED = 6;
    static final int HUNTER_DELAY_NEXT_BATCH = 7;
    static final int HUNTER_RETRY = 5;
    static final int NETWORK_STATE_CHANGE = 9;
    static final int REQUEST_CANCEL = 2;
    static final int REQUEST_GCED = 3;
    static final int REQUEST_SUBMIT = 1;
    private static final int RETRY_DELAY = 500;
    boolean airplaneMode;
    final List<BitmapHunter> batch;
    final Cache cache;
    final Context context;
    final DispatcherThread dispatcherThread;
    final Downloader downloader;
    final Map<Object, Action> failedActions;
    final Handler handler;
    final Map<String, BitmapHunter> hunterMap;
    final Handler mainThreadHandler;
    final NetworkBroadcastReceiver receiver;
    final boolean scansNetworkChanges;
    final ExecutorService service;
    final Stats stats;

    private static class DispatcherHandler extends Handler {
        private final Dispatcher dispatcher;

        /* renamed from: com.squareup.picasso.Dispatcher.DispatcherHandler.1 */
        class C06081 implements Runnable {
            final /* synthetic */ Message val$msg;

            C06081(Message message) {
                this.val$msg = message;
            }

            public void run() {
                throw new AssertionError("Unknown handler message received: " + this.val$msg.what);
            }
        }

        public DispatcherHandler(Looper looper, Dispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case Dispatcher.REQUEST_SUBMIT /*1*/:
                    this.dispatcher.performSubmit(msg.obj);
                case Dispatcher.REQUEST_CANCEL /*2*/:
                    this.dispatcher.performCancel((Action) msg.obj);
                case Dispatcher.HUNTER_COMPLETE /*4*/:
                    this.dispatcher.performComplete(msg.obj);
                case Dispatcher.HUNTER_RETRY /*5*/:
                    this.dispatcher.performRetry((BitmapHunter) msg.obj);
                case Dispatcher.HUNTER_DECODE_FAILED /*6*/:
                    this.dispatcher.performError((BitmapHunter) msg.obj, false);
                case Dispatcher.HUNTER_DELAY_NEXT_BATCH /*7*/:
                    this.dispatcher.performBatchComplete();
                case Dispatcher.NETWORK_STATE_CHANGE /*9*/:
                    this.dispatcher.performNetworkStateChange(msg.obj);
                case Dispatcher.AIRPLANE_MODE_CHANGE /*10*/:
                    Dispatcher dispatcher = this.dispatcher;
                    if (msg.arg1 != Dispatcher.REQUEST_SUBMIT) {
                        z = false;
                    }
                    dispatcher.performAirplaneModeChange(z);
                default:
                    Picasso.HANDLER.post(new C06081(msg));
            }
        }
    }

    static class DispatcherThread extends HandlerThread {
        DispatcherThread() {
            super("Picasso-Dispatcher", Dispatcher.AIRPLANE_MODE_CHANGE);
        }
    }

    static class NetworkBroadcastReceiver extends BroadcastReceiver {
        static final String EXTRA_AIRPLANE_STATE = "state";
        private final Dispatcher dispatcher;

        NetworkBroadcastReceiver(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        void register() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.AIRPLANE_MODE");
            if (this.dispatcher.scansNetworkChanges) {
                filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            }
            this.dispatcher.context.registerReceiver(this, filter);
        }

        void unregister() {
            this.dispatcher.context.unregisterReceiver(this);
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                    if (intent.hasExtra(EXTRA_AIRPLANE_STATE)) {
                        this.dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra(EXTRA_AIRPLANE_STATE, false));
                    }
                } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                    this.dispatcher.dispatchNetworkStateChange(((ConnectivityManager) Utils.getService(context, "connectivity")).getActiveNetworkInfo());
                }
            }
        }
    }

    Dispatcher(Context context, ExecutorService service, Handler mainThreadHandler, Downloader downloader, Cache cache, Stats stats) {
        this.dispatcherThread = new DispatcherThread();
        this.dispatcherThread.start();
        this.context = context;
        this.service = service;
        this.hunterMap = new LinkedHashMap();
        this.failedActions = new WeakHashMap();
        this.handler = new DispatcherHandler(this.dispatcherThread.getLooper(), this);
        this.downloader = downloader;
        this.mainThreadHandler = mainThreadHandler;
        this.cache = cache;
        this.stats = stats;
        this.batch = new ArrayList(HUNTER_COMPLETE);
        this.airplaneMode = Utils.isAirplaneModeOn(this.context);
        this.scansNetworkChanges = Utils.hasPermission(context, "android.permission.ACCESS_NETWORK_STATE");
        this.receiver = new NetworkBroadcastReceiver(this);
        this.receiver.register();
    }

    void shutdown() {
        this.service.shutdown();
        this.dispatcherThread.quit();
        this.receiver.unregister();
    }

    void dispatchSubmit(Action action) {
        this.handler.sendMessage(this.handler.obtainMessage(REQUEST_SUBMIT, action));
    }

    void dispatchCancel(Action action) {
        this.handler.sendMessage(this.handler.obtainMessage(REQUEST_CANCEL, action));
    }

    void dispatchComplete(BitmapHunter hunter) {
        this.handler.sendMessage(this.handler.obtainMessage(HUNTER_COMPLETE, hunter));
    }

    void dispatchRetry(BitmapHunter hunter) {
        this.handler.sendMessageDelayed(this.handler.obtainMessage(HUNTER_RETRY, hunter), 500);
    }

    void dispatchFailed(BitmapHunter hunter) {
        this.handler.sendMessage(this.handler.obtainMessage(HUNTER_DECODE_FAILED, hunter));
    }

    void dispatchNetworkStateChange(NetworkInfo info) {
        this.handler.sendMessage(this.handler.obtainMessage(NETWORK_STATE_CHANGE, info));
    }

    void dispatchAirplaneModeChange(boolean airplaneMode) {
        int i;
        Handler handler = this.handler;
        Handler handler2 = this.handler;
        if (airplaneMode) {
            i = REQUEST_SUBMIT;
        } else {
            i = AIRPLANE_MODE_OFF;
        }
        handler.sendMessage(handler2.obtainMessage(AIRPLANE_MODE_CHANGE, i, AIRPLANE_MODE_OFF));
    }

    void performSubmit(Action action) {
        BitmapHunter hunter = (BitmapHunter) this.hunterMap.get(action.getKey());
        if (hunter != null) {
            hunter.attach(action);
        } else if (!this.service.isShutdown()) {
            hunter = BitmapHunter.forRequest(this.context, action.getPicasso(), this, this.cache, this.stats, action, this.downloader);
            hunter.future = this.service.submit(hunter);
            this.hunterMap.put(action.getKey(), hunter);
            this.failedActions.remove(action.getTarget());
            if (action.getPicasso().loggingEnabled) {
                Utils.log(DISPATCHER_THREAD_NAME, "enqueued", action.request.logId());
            }
        } else if (action.getPicasso().loggingEnabled) {
            Utils.log(DISPATCHER_THREAD_NAME, "ignored", action.request.logId(), "because shut down");
        }
    }

    void performCancel(Action action) {
        String key = action.getKey();
        BitmapHunter hunter = (BitmapHunter) this.hunterMap.get(key);
        if (hunter != null) {
            hunter.detach(action);
            if (hunter.cancel()) {
                this.hunterMap.remove(key);
                if (action.getPicasso().loggingEnabled) {
                    Utils.log(DISPATCHER_THREAD_NAME, "canceled", action.getRequest().logId());
                }
            }
        }
        Action remove = (Action) this.failedActions.remove(action.getTarget());
        if (remove != null && remove.getPicasso().loggingEnabled) {
            Utils.log(DISPATCHER_THREAD_NAME, "canceled", remove.getRequest().logId(), "from replaying");
        }
    }

    void performRetry(BitmapHunter hunter) {
        if (!hunter.isCancelled()) {
            if (this.service.isShutdown()) {
                performError(hunter, false);
                return;
            }
            NetworkInfo networkInfo = null;
            if (this.scansNetworkChanges) {
                networkInfo = ((ConnectivityManager) Utils.getService(this.context, "connectivity")).getActiveNetworkInfo();
            }
            boolean hasConnectivity;
            if (networkInfo == null || !networkInfo.isConnected()) {
                hasConnectivity = false;
            } else {
                hasConnectivity = true;
            }
            boolean shouldRetryHunter = hunter.shouldRetry(this.airplaneMode, networkInfo);
            boolean supportsReplay = hunter.supportsReplay();
            if (!shouldRetryHunter) {
                boolean willReplay;
                if (this.scansNetworkChanges && supportsReplay) {
                    willReplay = true;
                } else {
                    willReplay = false;
                }
                performError(hunter, willReplay);
                if (willReplay) {
                    markForReplay(hunter);
                }
            } else if (!this.scansNetworkChanges || hasConnectivity) {
                if (hunter.getPicasso().loggingEnabled) {
                    Utils.log(DISPATCHER_THREAD_NAME, "retrying", Utils.getLogIdsForHunter(hunter));
                }
                hunter.future = this.service.submit(hunter);
            } else {
                performError(hunter, supportsReplay);
                if (supportsReplay) {
                    markForReplay(hunter);
                }
            }
        }
    }

    void performComplete(BitmapHunter hunter) {
        if (!hunter.shouldSkipMemoryCache()) {
            this.cache.set(hunter.getKey(), hunter.getResult());
        }
        this.hunterMap.remove(hunter.getKey());
        batch(hunter);
        if (hunter.getPicasso().loggingEnabled) {
            Utils.log(DISPATCHER_THREAD_NAME, "batched", Utils.getLogIdsForHunter(hunter), "for completion");
        }
    }

    void performBatchComplete() {
        List<BitmapHunter> copy = new ArrayList(this.batch);
        this.batch.clear();
        this.mainThreadHandler.sendMessage(this.mainThreadHandler.obtainMessage(HUNTER_BATCH_COMPLETE, copy));
        logBatch(copy);
    }

    void performError(BitmapHunter hunter, boolean willReplay) {
        if (hunter.getPicasso().loggingEnabled) {
            Utils.log(DISPATCHER_THREAD_NAME, "batched", Utils.getLogIdsForHunter(hunter), "for error" + (willReplay ? " (will replay)" : UnsupportedUrlFragment.DISPLAY_NAME));
        }
        this.hunterMap.remove(hunter.getKey());
        batch(hunter);
    }

    void performAirplaneModeChange(boolean airplaneMode) {
        this.airplaneMode = airplaneMode;
    }

    void performNetworkStateChange(NetworkInfo info) {
        if (this.service instanceof PicassoExecutorService) {
            ((PicassoExecutorService) this.service).adjustThreadCount(info);
        }
        if (info != null && info.isConnected()) {
            flushFailedActions();
        }
    }

    private void flushFailedActions() {
        if (!this.failedActions.isEmpty()) {
            Iterator<Action> iterator = this.failedActions.values().iterator();
            while (iterator.hasNext()) {
                Action action = (Action) iterator.next();
                iterator.remove();
                if (action.getPicasso().loggingEnabled) {
                    Utils.log(DISPATCHER_THREAD_NAME, "replaying", action.getRequest().logId());
                }
                performSubmit(action);
            }
        }
    }

    private void markForReplay(BitmapHunter hunter) {
        Action action = hunter.getAction();
        if (action != null) {
            markForReplay(action);
        }
        List<Action> joined = hunter.getActions();
        if (joined != null) {
            int n = joined.size();
            for (int i = AIRPLANE_MODE_OFF; i < n; i += REQUEST_SUBMIT) {
                markForReplay((Action) joined.get(i));
            }
        }
    }

    private void markForReplay(Action action) {
        Object target = action.getTarget();
        if (target != null) {
            action.willReplay = true;
            this.failedActions.put(target, action);
        }
    }

    private void batch(BitmapHunter hunter) {
        if (!hunter.isCancelled()) {
            this.batch.add(hunter);
            if (!this.handler.hasMessages(HUNTER_DELAY_NEXT_BATCH)) {
                this.handler.sendEmptyMessageDelayed(HUNTER_DELAY_NEXT_BATCH, 200);
            }
        }
    }

    private void logBatch(List<BitmapHunter> copy) {
        if (copy != null && !copy.isEmpty() && ((BitmapHunter) copy.get(AIRPLANE_MODE_OFF)).getPicasso().loggingEnabled) {
            StringBuilder builder = new StringBuilder();
            for (BitmapHunter bitmapHunter : copy) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(Utils.getLogIdsForHunter(bitmapHunter));
            }
            Utils.log(DISPATCHER_THREAD_NAME, "delivered", builder.toString());
        }
    }
}
