/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.wm.shell.pip.tv;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.util.Size;
import android.view.Gravity;

import com.android.wm.shell.pip.PipBoundsAlgorithm;
import com.android.wm.shell.pip.PipBoundsState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * TV specific values of the current state of the PiP bounds.
 */
public class TvPipBoundsState extends PipBoundsState {

    public static final int ORIENTATION_UNDETERMINED = 0;
    public static final int ORIENTATION_VERTICAL = 1;
    public static final int ORIENTATION_HORIZONTAL = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"ORIENTATION_"}, value = {
            ORIENTATION_UNDETERMINED,
            ORIENTATION_VERTICAL,
            ORIENTATION_HORIZONTAL
    })
    public @interface Orientation {
    }

    public static final int DEFAULT_TV_GRAVITY = Gravity.BOTTOM | Gravity.RIGHT;

    private final boolean mIsTvExpandedPipSupported;
    private boolean mIsTvPipExpanded;
    private boolean mTvPipManuallyCollapsed;
    private float mDesiredTvExpandedAspectRatio;
    private @Orientation int mTvFixedPipOrientation;
    private int mTvPipGravity;
    private @Nullable Size mTvExpandedSize;
    private @NonNull Insets mPipMenuPermanentDecorInsets = Insets.NONE;
    private @NonNull Insets mPipMenuTemporaryDecorInsets = Insets.NONE;

    public TvPipBoundsState(@NonNull Context context) {
        super(context);
        mIsTvExpandedPipSupported = context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_EXPANDED_PICTURE_IN_PICTURE);
    }

    /**
     * Initialize states when first entering PiP.
     */
    @Override
    public void setBoundsStateForEntry(ComponentName componentName, ActivityInfo activityInfo,
            PictureInPictureParams params, PipBoundsAlgorithm pipBoundsAlgorithm) {
        super.setBoundsStateForEntry(componentName, activityInfo, params, pipBoundsAlgorithm);
        if (params == null) {
            return;
        }
        setDesiredTvExpandedAspectRatio(params.getExpandedAspectRatioFloat(), true);
    }

    /** Resets the TV PiP state for a new activity. */
    public void resetTvPipState() {
        mTvFixedPipOrientation = ORIENTATION_UNDETERMINED;
        mTvPipGravity = DEFAULT_TV_GRAVITY;
    }

    /** Set the tv expanded bounds of PiP */
    public void setTvExpandedSize(@Nullable Size size) {
        mTvExpandedSize = size;
    }

    /** Get the expanded size of the PiP. */
    @Nullable
    public Size getTvExpandedSize() {
        return mTvExpandedSize;
    }

    /** Set the PiP aspect ratio for the expanded PiP (TV) that is desired by the app. */
    public void setDesiredTvExpandedAspectRatio(float aspectRatio, boolean override) {
        if (override || mTvFixedPipOrientation == ORIENTATION_UNDETERMINED) {
            mDesiredTvExpandedAspectRatio = aspectRatio;
            resetTvPipState();
            return;
        }
        if ((aspectRatio > 1 && mTvFixedPipOrientation == ORIENTATION_HORIZONTAL)
                || (aspectRatio <= 1 && mTvFixedPipOrientation == ORIENTATION_VERTICAL)
                || aspectRatio == 0) {
            mDesiredTvExpandedAspectRatio = aspectRatio;
        }
    }

    /**
     * Get the aspect ratio for the expanded PiP (TV) that is desired, or {@code 0} if it is not
     * enabled by the app.
     */
    public float getDesiredTvExpandedAspectRatio() {
        return mDesiredTvExpandedAspectRatio;
    }

    /** Sets the orientation the expanded TV PiP activity has been fixed to. */
    public void setTvFixedPipOrientation(@Orientation int orientation) {
        mTvFixedPipOrientation = orientation;
    }

    /** Returns the fixed orientation of the expanded PiP on TV. */
    @Orientation
    public int getTvFixedPipOrientation() {
        return mTvFixedPipOrientation;
    }

    /** Sets the current gravity of the TV PiP. */
    public void setTvPipGravity(int gravity) {
        mTvPipGravity = gravity;
    }

    /** Returns the current gravity of the TV PiP. */
    public int getTvPipGravity() {
        return mTvPipGravity;
    }

    /** Sets whether the TV PiP is currently expanded. */
    public void setTvPipExpanded(boolean expanded) {
        mIsTvPipExpanded = expanded;
    }

    /** Returns whether the TV PiP is currently expanded. */
    public boolean isTvPipExpanded() {
        return mIsTvPipExpanded;
    }

    /** Sets whether the user has manually collapsed the TV PiP. */
    public void setTvPipManuallyCollapsed(boolean collapsed) {
        mTvPipManuallyCollapsed = collapsed;
    }

    /** Returns whether the user has manually collapsed the TV PiP. */
    public boolean isTvPipManuallyCollapsed() {
        return mTvPipManuallyCollapsed;
    }

    /** Returns whether expanded PiP is supported by the device. */
    public boolean isTvExpandedPipSupported() {
        return mIsTvExpandedPipSupported;
    }

    public void setPipMenuPermanentDecorInsets(@NonNull Insets permanentInsets) {
        mPipMenuPermanentDecorInsets = permanentInsets;
    }

    public @NonNull Insets getPipMenuPermanentDecorInsets() {
        return mPipMenuPermanentDecorInsets;
    }

    public void setPipMenuTemporaryDecorInsets(@NonNull Insets temporaryDecorInsets) {
        mPipMenuTemporaryDecorInsets = temporaryDecorInsets;
    }

    public @NonNull Insets getPipMenuTemporaryDecorInsets() {
        return mPipMenuTemporaryDecorInsets;
    }
}
