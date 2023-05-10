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

package com.android.server.wm;

import static com.android.internal.protolog.ProtoLogGroup.WM_DEBUG_CONTENT_RECORDING;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.view.ContentRecordingSession;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.protolog.common.ProtoLog;

/**
 * Orchestrates the handoff between displays if the recording session changes, and keeps track of
 * the current recording session state. Only supports one content recording session on the device at
 * once.
 */
final class ContentRecordingController {

    /**
     * The current recording session.
     */
    @Nullable
    private ContentRecordingSession mSession = null;

    @Nullable
    private DisplayContent mDisplayContent = null;

    /**
     * Returns the current recording session. If returns {@code null}, then recording is not taking
     * place.
     */
    @Nullable
    @VisibleForTesting
    ContentRecordingSession getContentRecordingSessionLocked() {
        // Copy out the session, to allow it to be modified without updating this reference.
        return mSession;
    }

    /**
     * Updates the current recording session.
     * <p>Handles the following scenarios:
     * <ul>
     *         <li>Invalid scenarios: The incoming session is malformed, or the incoming session is
     *         identical to the current session</li>
     *         <li>Start Scenario: Starting a new session. Recording begins immediately.</li>
     *         <li>Takeover Scenario: Occurs during a Start Scenario, if a pre-existing session was
     *         in-progress. For example, recording on VirtualDisplay "app_foo" was ongoing. A
     *         session for VirtualDisplay "app_bar" arrives. The controller stops the session on
     *         VirtualDisplay "app_foo" and allows the session for VirtualDisplay "app_bar" to
     *         begin.</li>
     *         <li>Stopping scenario: The incoming session is null and there is currently an ongoing
     *         session. The controller stops recording.</li>
     * </ul>
     *
     * @param incomingSession The incoming recording session (either an update to a current session
     *                        or a new session), or null to stop the current session.
     * @param wmService       The window manager service.
     */
    void setContentRecordingSessionLocked(@Nullable ContentRecordingSession incomingSession,
            @NonNull WindowManagerService wmService) {
        // Invalid scenario: ignore invalid incoming session.
        if (incomingSession != null && !ContentRecordingSession.isValid(incomingSession)) {
            return;
        }
        // Invalid scenario: ignore identical incoming session.
        if (ContentRecordingSession.isProjectionOnSameDisplay(mSession, incomingSession)) {
            // TODO(242833866): if incoming session is no longer waiting to record, allow
            //  the update through.

            ProtoLog.v(WM_DEBUG_CONTENT_RECORDING,
                    "Content Recording: Ignoring session on same display %d, with an existing "
                            + "session %s",
                    incomingSession.getVirtualDisplayId(), mSession.getVirtualDisplayId());
            return;
        }
        DisplayContent incomingDisplayContent = null;
        // Start scenario: recording begins immediately.
        if (incomingSession != null) {
            ProtoLog.v(WM_DEBUG_CONTENT_RECORDING,
                    "Content Recording: Handle incoming session on display %d, with a "
                            + "pre-existing session %s", incomingSession.getVirtualDisplayId(),
                    mSession == null ? null : mSession.getVirtualDisplayId());
            incomingDisplayContent = wmService.mRoot.getDisplayContentOrCreate(
                    incomingSession.getVirtualDisplayId());
            incomingDisplayContent.setContentRecordingSession(incomingSession);
            // TODO(b/270118861): When user grants consent to re-use, explicitly ask ContentRecorder
            //  to update, since no config/display change arrives. Mark recording as black.
        }
        // Takeover and stopping scenario: stop recording on the pre-existing session.
        if (mSession != null) {
            ProtoLog.v(WM_DEBUG_CONTENT_RECORDING,
                    "Content Recording: Pause the recording session on display %s",
                    mDisplayContent.getDisplayId());
            mDisplayContent.pauseRecording();
            mDisplayContent.setContentRecordingSession(null);
        }
        // Update the cached states.
        mDisplayContent = incomingDisplayContent;
        mSession = incomingSession;
    }
}
