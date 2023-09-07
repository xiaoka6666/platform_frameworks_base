/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.biometrics;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.biometrics.sensors.BiometricNotification;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculate and collect on-device False Rejection Rates (FRR).
 * FRR = All [given biometric modality] unlock failures / all [given biometric modality] unlock
 * attempts.
 */
public class AuthenticationStatsCollector {

    private static final String TAG = "AuthenticationStatsCollector";

    // The minimum number of attempts that will calculate the FRR and trigger the notification.
    private static final int MINIMUM_ATTEMPTS = 150;
    // Upload the data every 50 attempts (average number of daily authentications).
    private static final int AUTHENTICATION_UPLOAD_INTERVAL = 50;
    // The maximum number of eligible biometric enrollment notification can be sent.
    @VisibleForTesting
    static final int MAXIMUM_ENROLLMENT_NOTIFICATIONS = 1;

    @NonNull private final Context mContext;
    @NonNull private final PackageManager mPackageManager;
    @NonNull private final FaceManager mFaceManager;
    @NonNull private final FingerprintManager mFingerprintManager;

    private final float mThreshold;
    private final int mModality;
    private boolean mPersisterInitialized = false;

    @NonNull private final Map<Integer, AuthenticationStats> mUserAuthenticationStatsMap;

    // TODO(b/295582896): Find a way to make this NonNull
    @Nullable private AuthenticationStatsPersister mAuthenticationStatsPersister;
    @NonNull private BiometricNotification mBiometricNotification;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, @NonNull Intent intent) {
            final int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL);

            if (userId != UserHandle.USER_NULL
                    && intent.getAction().equals(Intent.ACTION_USER_REMOVED)) {
                Slog.d(TAG, "Removing data for user: " + userId);
                onUserRemoved(userId);
            }
        }
    };

    public AuthenticationStatsCollector(@NonNull Context context, int modality,
            @NonNull BiometricNotification biometricNotification) {
        mContext = context;
        mThreshold = context.getResources()
                .getFraction(R.fraction.config_biometricNotificationFrrThreshold, 1, 1);
        mUserAuthenticationStatsMap = new HashMap<>();
        mModality = modality;
        mBiometricNotification = biometricNotification;

        mPackageManager = context.getPackageManager();
        mFaceManager = mContext.getSystemService(FaceManager.class);
        mFingerprintManager = mContext.getSystemService(FingerprintManager.class);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_USER_REMOVED);
        context.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void initializeUserAuthenticationStatsMap() {
        try {
            mAuthenticationStatsPersister = new AuthenticationStatsPersister(mContext);
            for (AuthenticationStats stats :
                    mAuthenticationStatsPersister.getAllFrrStats(mModality)) {
                mUserAuthenticationStatsMap.put(stats.getUserId(), stats);
            }
            mAuthenticationStatsPersister.persistFrrThreshold(mThreshold);

            mPersisterInitialized = true;
        } catch (IllegalStateException e) {
            Slog.w(TAG, "Failed to initialize AuthenticationStatsPersister.", e);
        }
    }

    /** Update total authentication and rejected attempts. */
    public void authenticate(int userId, boolean authenticated) {

        // Don't collect data for single-modality devices or user has both biometrics enrolled.
        if (isSingleModalityDevice()
                || (hasEnrolledFace(userId) && hasEnrolledFingerprint(userId))) {
            return;
        }

        // SharedPreference is not ready when starting system server, initialize
        // mUserAuthenticationStatsMap in authentication to ensure SharedPreference
        // is ready for application use.
        if (mUserAuthenticationStatsMap.isEmpty()) {
            initializeUserAuthenticationStatsMap();
        }
        // Check if this is a new user.
        if (!mUserAuthenticationStatsMap.containsKey(userId)) {
            mUserAuthenticationStatsMap.put(userId, new AuthenticationStats(userId, mModality));
        }

        AuthenticationStats authenticationStats = mUserAuthenticationStatsMap.get(userId);

        if (authenticationStats.getEnrollmentNotifications() >= MAXIMUM_ENROLLMENT_NOTIFICATIONS) {
            return;
        }

        authenticationStats.authenticate(authenticated);

        sendNotificationIfNeeded(userId);

        if (mPersisterInitialized) {
            persistDataIfNeeded(userId);
        }
    }

    /** Check if a notification should be sent after a calculation cycle. */
    private void sendNotificationIfNeeded(int userId) {
        AuthenticationStats authenticationStats = mUserAuthenticationStatsMap.get(userId);
        if (authenticationStats.getTotalAttempts() < MINIMUM_ATTEMPTS) {
            return;
        }

        // Don't send notification if FRR below the threshold.
        if (authenticationStats.getEnrollmentNotifications() >= MAXIMUM_ENROLLMENT_NOTIFICATIONS
                || authenticationStats.getFrr() < mThreshold) {
            authenticationStats.resetData();
            return;
        }

        authenticationStats.resetData();

        final boolean hasEnrolledFace = hasEnrolledFace(userId);
        final boolean hasEnrolledFingerprint = hasEnrolledFingerprint(userId);

        if (hasEnrolledFace && !hasEnrolledFingerprint) {
            mBiometricNotification.sendFpEnrollNotification(mContext);
            authenticationStats.updateNotificationCounter();
        } else if (!hasEnrolledFace && hasEnrolledFingerprint) {
            mBiometricNotification.sendFaceEnrollNotification(mContext);
            authenticationStats.updateNotificationCounter();
        }
    }

    private void persistDataIfNeeded(int userId) {
        AuthenticationStats authenticationStats = mUserAuthenticationStatsMap.get(userId);
        if (authenticationStats.getTotalAttempts() % AUTHENTICATION_UPLOAD_INTERVAL == 0) {
            mAuthenticationStatsPersister.persistFrrStats(authenticationStats.getUserId(),
                    authenticationStats.getTotalAttempts(),
                    authenticationStats.getRejectedAttempts(),
                    authenticationStats.getEnrollmentNotifications(),
                    authenticationStats.getModality());
        }
    }

    private void onUserRemoved(final int userId) {
        if (!mPersisterInitialized) {
            initializeUserAuthenticationStatsMap();
        }
        if (mPersisterInitialized) {
            mUserAuthenticationStatsMap.remove(userId);
            mAuthenticationStatsPersister.removeFrrStats(userId);
        }
    }

    private boolean isSingleModalityDevice() {
        return !mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                || !mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE);
    }

    private boolean hasEnrolledFace(int userId) {
        return mFaceManager.hasEnrolledTemplates(userId);
    }

    private boolean hasEnrolledFingerprint(int userId) {
        return mFingerprintManager.hasEnrolledTemplates(userId);
    }

    /**
     * Only being used in tests. Callers should not make any changes to the returned
     * authentication stats.
     *
     * @return AuthenticationStats of the user, or null if the stats doesn't exist.
     */
    @Nullable
    @VisibleForTesting
    AuthenticationStats getAuthenticationStatsForUser(int userId) {
        return mUserAuthenticationStatsMap.getOrDefault(userId, null);
    }

    @VisibleForTesting
    void setAuthenticationStatsForUser(int userId, AuthenticationStats authenticationStats) {
        mUserAuthenticationStatsMap.put(userId, authenticationStats);
    }
}
