/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.app;

import static android.view.WindowManagerImpl.createWindowContextWindowManager;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UiContext;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacksController;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.ref.Reference;

/**
 * {@link WindowContext} is a context for non-activity windows such as
 * {@link android.view.WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY} windows or system
 * windows. Its resources and configuration are adjusted to the area of the display that will be
 * used when a new window is added via {@link android.view.WindowManager#addView}.
 *
 * @see Context#createWindowContext(int, Bundle)
 * @hide
 */
@UiContext
public class WindowContext extends ContextWrapper {
    private final WindowManager mWindowManager;
    private final IWindowManager mWms;
    private final WindowTokenClient mToken;
    private boolean mListenerRegistered;
    private final ComponentCallbacksController mCallbacksController =
            new ComponentCallbacksController();

    /**
     * Default constructor. Will generate a {@link WindowTokenClient} and attach this context to
     * the token.
     *
     * @param base Base {@link Context} for this new instance.
     * @param type Window type to be used with this context.
     * @hide
     */
    public WindowContext(@NonNull Context base, int type, @Nullable Bundle options) {
        this(base, null /* display */, type, options);
    }

    /**
     * Default constructor. Will generate a {@link WindowTokenClient} and attach this context to
     * the token.
     *
     * @param base Base {@link Context} for this new instance.
     * @param display the {@link Display} to override.
     * @param type Window type to be used with this context.
     * @hide
     */
    public WindowContext(@NonNull Context base, @Nullable Display display, int type,
            @Nullable Bundle options) {
        // Correct base context will be built once the token is resolved, so passing 'null' here.
        super(null /* base */);

        mWms = WindowManagerGlobal.getWindowManagerService();
        mToken = new WindowTokenClient();

        final ContextImpl contextImpl = createBaseWindowContext(base, mToken, display);
        attachBaseContext(contextImpl);
        contextImpl.setOuterContext(this);

        mToken.attachContext(this);

        mWindowManager = createWindowContextWindowManager(this);

        try {
            mListenerRegistered = mWms.registerWindowContextListener(mToken, type, getDisplayId(),
                    options);
        }  catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        Reference.reachabilityFence(this);
    }

    private static ContextImpl createBaseWindowContext(Context outer, IBinder token,
            Display display) {
        final ContextImpl contextImpl = ContextImpl.getImpl(outer);
        return contextImpl.createBaseWindowContext(token, display);
    }

    @Override
    public Object getSystemService(String name) {
        if (WINDOW_SERVICE.equals(name)) {
            return mWindowManager;
        }
        return super.getSystemService(name);
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    /** Used for test to invoke because we can't invoke finalize directly. */
    @VisibleForTesting
    public void release() {
        if (mListenerRegistered) {
            mListenerRegistered = false;
            try {
                mWms.unregisterWindowContextListener(mToken);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        destroy();
    }

    void destroy() {
        mCallbacksController.clearCallbacks();
        final ContextImpl impl = (ContextImpl) getBaseContext();
        impl.scheduleFinalCleanup(getClass().getName(), "WindowContext");
        Reference.reachabilityFence(this);
    }

    @Override
    public void registerComponentCallbacks(@NonNull ComponentCallbacks callback) {
        mCallbacksController.registerCallbacks(callback);
    }

    @Override
    public void unregisterComponentCallbacks(@NonNull ComponentCallbacks callback) {
        mCallbacksController.unregisterCallbacks(callback);
    }

    /** Dispatch {@link Configuration} to each {@link ComponentCallbacks}. */
    void dispatchConfigurationChanged(@NonNull Configuration newConfig) {
        mCallbacksController.dispatchConfigurationChanged(newConfig);
    }
}
