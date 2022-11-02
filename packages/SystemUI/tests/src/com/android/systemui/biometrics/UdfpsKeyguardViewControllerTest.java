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

package com.android.systemui.biometrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.testing.AndroidTestingRunner;
import android.testing.TestableLooper.RunWithLooper;

import androidx.test.filters.SmallTest;

import com.android.systemui.shade.ShadeExpansionListener;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.KeyguardBouncer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

@SmallTest
@RunWith(AndroidTestingRunner.class)
@RunWithLooper
public class UdfpsKeyguardViewControllerTest extends UdfpsKeyguardViewControllerBaseTest {
    private @Captor ArgumentCaptor<KeyguardBouncer.BouncerExpansionCallback>
            mBouncerExpansionCallbackCaptor;
    private KeyguardBouncer.BouncerExpansionCallback mBouncerExpansionCallback;

    @Override
    public UdfpsKeyguardViewController createUdfpsKeyguardViewController() {
        return createUdfpsKeyguardViewController(/* useModernBouncer */ false);
    }

    @Test
    public void testShouldPauseAuth_bouncerShowing() {
        mController.onViewAttached();
        captureStatusBarStateListeners();
        sendStatusBarStateChanged(StatusBarState.KEYGUARD);

        captureBouncerExpansionCallback();
        when(mStatusBarKeyguardViewManager.isBouncerShowing()).thenReturn(true);
        when(mStatusBarKeyguardViewManager.bouncerIsOrWillBeShowing()).thenReturn(true);
        mBouncerExpansionCallback.onVisibilityChanged(true);

        assertTrue(mController.shouldPauseAuth());
    }



    @Test
    public void testRegistersExpansionChangedListenerOnAttached() {
        mController.onViewAttached();
        captureStatusBarExpansionListeners();
    }

    @Test
    public void testRegistersStatusBarStateListenersOnAttached() {
        mController.onViewAttached();
        captureStatusBarStateListeners();
    }

    @Test
    public void testViewControllerQueriesSBStateOnAttached() {
        mController.onViewAttached();
        verify(mStatusBarStateController).getState();
        verify(mStatusBarStateController).getDozeAmount();

        final float dozeAmount = .88f;
        when(mStatusBarStateController.getState()).thenReturn(StatusBarState.SHADE_LOCKED);
        when(mStatusBarStateController.getDozeAmount()).thenReturn(dozeAmount);
        captureStatusBarStateListeners();

        mController.onViewAttached();
        verify(mView, atLeast(1)).setPauseAuth(true);
        verify(mView).onDozeAmountChanged(dozeAmount, dozeAmount,
                UdfpsKeyguardView.ANIMATION_BETWEEN_AOD_AND_LOCKSCREEN);
    }

    @Test
    public void testListenersUnregisteredOnDetached() {
        mController.onViewAttached();
        captureStatusBarStateListeners();
        captureStatusBarExpansionListeners();
        captureKeyguardStateControllerCallback();
        mController.onViewDetached();

        verify(mStatusBarStateController).removeCallback(mStatusBarStateListener);
        for (ShadeExpansionListener listener : mExpansionListeners) {
            verify(mShadeExpansionStateManager).removeExpansionListener(listener);
        }
        verify(mKeyguardStateController).removeCallback(mKeyguardStateControllerCallback);
    }

    @Test
    public void testDozeEventsSentToView() {
        mController.onViewAttached();
        captureStatusBarStateListeners();

        final float linear = .55f;
        final float eased = .65f;
        mStatusBarStateListener.onDozeAmountChanged(linear, eased);

        verify(mView).onDozeAmountChanged(linear, eased,
                UdfpsKeyguardView.ANIMATION_BETWEEN_AOD_AND_LOCKSCREEN);
    }

    @Test
    public void testShouldPauseAuthUnpausedAlpha0() {
        mController.onViewAttached();
        captureStatusBarStateListeners();

        when(mView.getUnpausedAlpha()).thenReturn(0);
        sendStatusBarStateChanged(StatusBarState.KEYGUARD);

        assertTrue(mController.shouldPauseAuth());
    }

    @Test
    public void testFadeFromDialogSuggestedAlpha() {
        // GIVEN view is attached and status bar expansion is 1f
        mController.onViewAttached();
        captureStatusBarStateListeners();
        captureStatusBarExpansionListeners();
        updateStatusBarExpansion(1f, true);
        reset(mView);

        // WHEN dialog suggested alpha is .6f
        when(mView.getDialogSuggestedAlpha()).thenReturn(.6f);
        sendStatusBarStateChanged(StatusBarState.KEYGUARD);

        // THEN alpha is updated based on dialog suggested alpha
        verify(mView).setUnpausedAlpha((int) (.6f * 255));
    }

    @Test
    public void testShouldNotPauseAuthOnKeyguard() {
        mController.onViewAttached();
        captureStatusBarStateListeners();

        sendStatusBarStateChanged(StatusBarState.KEYGUARD);

        assertFalse(mController.shouldPauseAuth());
    }

    @Test
    public void testShouldPauseAuthIsLaunchTransitionFadingAway() {
        // GIVEN view is attached and we're on the keyguard (see testShouldNotPauseAuthOnKeyguard)
        mController.onViewAttached();
        captureStatusBarStateListeners();
        sendStatusBarStateChanged(StatusBarState.KEYGUARD);

        // WHEN isLaunchTransitionFadingAway=true
        captureKeyguardStateControllerCallback();
        when(mKeyguardStateController.isLaunchTransitionFadingAway()).thenReturn(true);
        mKeyguardStateControllerCallback.onLaunchTransitionFadingAwayChanged();

        // THEN pause auth
        assertTrue(mController.shouldPauseAuth());
    }

    @Test
    public void testShouldPauseAuthOnShadeLocked() {
        mController.onViewAttached();
        captureStatusBarStateListeners();

        sendStatusBarStateChanged(StatusBarState.SHADE_LOCKED);

        assertTrue(mController.shouldPauseAuth());
    }

    @Test
    public void testShouldPauseAuthOnShade() {
        mController.onViewAttached();
        captureStatusBarStateListeners();

        // WHEN not on keyguard yet (shade = home)
        sendStatusBarStateChanged(StatusBarState.SHADE);

        // THEN pause auth
        assertTrue(mController.shouldPauseAuth());
    }

    @Test
    public void testShouldPauseAuthAnimatingScreenOffFromShade() {
        mController.onViewAttached();
        captureStatusBarStateListeners();

        // WHEN transitioning from home/shade => keyguard + animating screen off
        mStatusBarStateListener.onStatePreChange(StatusBarState.SHADE, StatusBarState.KEYGUARD);
        when(mKeyguardViewMediator.isAnimatingScreenOff()).thenReturn(true);

        // THEN pause auth
        assertTrue(mController.shouldPauseAuth());
    }

    @Test
    public void testDoNotPauseAuthAnimatingScreenOffFromLS() {
        mController.onViewAttached();
        captureStatusBarStateListeners();

        // WHEN animating screen off transition from LS => AOD
        sendStatusBarStateChanged(StatusBarState.KEYGUARD);
        when(mKeyguardViewMediator.isAnimatingScreenOff()).thenReturn(true);

        // THEN don't pause auth
        assertFalse(mController.shouldPauseAuth());
    }

    @Test
    public void testOverrideShouldPauseAuthOnShadeLocked() {
        mController.onViewAttached();
        captureStatusBarStateListeners();
        captureAltAuthInterceptor();

        sendStatusBarStateChanged(StatusBarState.SHADE_LOCKED);
        assertTrue(mController.shouldPauseAuth());

        mAltAuthInterceptor.showAlternateAuthBouncer(); // force show
        assertFalse(mController.shouldPauseAuth());
        assertTrue(mAltAuthInterceptor.isShowingAlternateAuthBouncer());

        mAltAuthInterceptor.hideAlternateAuthBouncer(); // stop force show
        assertTrue(mController.shouldPauseAuth());
        assertFalse(mAltAuthInterceptor.isShowingAlternateAuthBouncer());
    }

    @Test
    public void testOnDetachedStateReset() {
        // GIVEN view is attached
        mController.onViewAttached();
        captureAltAuthInterceptor();

        // WHEN view is detached
        mController.onViewDetached();

        // THEN remove alternate auth interceptor
        verify(mStatusBarKeyguardViewManager).removeAlternateAuthInterceptor(mAltAuthInterceptor);
    }

    @Test
    public void testHiddenUdfpsBouncerOnTouchOutside_nothingHappens() {
        // GIVEN view is attached
        mController.onViewAttached();
        captureAltAuthInterceptor();

        // GIVEN udfps bouncer isn't showing
        mAltAuthInterceptor.hideAlternateAuthBouncer();

        // WHEN touch is observed outside the view
        mController.onTouchOutsideView();

        // THEN bouncer / alt auth methods are never called
        verify(mStatusBarKeyguardViewManager, never()).showBouncer(anyBoolean());
        verify(mStatusBarKeyguardViewManager, never()).resetAlternateAuth(anyBoolean());
    }

    @Test
    public void testShowingUdfpsBouncerOnTouchOutsideWithinThreshold_nothingHappens() {
        // GIVEN view is attached
        mController.onViewAttached();
        captureAltAuthInterceptor();

        // GIVEN udfps bouncer is showing
        mAltAuthInterceptor.showAlternateAuthBouncer();

        // WHEN touch is observed outside the view 200ms later (just within threshold)
        mSystemClock.advanceTime(200);
        mController.onTouchOutsideView();

        // THEN bouncer / alt auth methods are never called because not enough time has passed
        verify(mStatusBarKeyguardViewManager, never()).showBouncer(anyBoolean());
        verify(mStatusBarKeyguardViewManager, never()).resetAlternateAuth(anyBoolean());
    }

    @Test
    public void testShowingUdfpsBouncerOnTouchOutsideAboveThreshold_showInputBouncer() {
        // GIVEN view is attached
        mController.onViewAttached();
        captureAltAuthInterceptor();

        // GIVEN udfps bouncer is showing
        mAltAuthInterceptor.showAlternateAuthBouncer();

        // WHEN touch is observed outside the view 205ms later
        mSystemClock.advanceTime(205);
        mController.onTouchOutsideView();

        // THEN show the bouncer
        verify(mStatusBarKeyguardViewManager).showBouncer(eq(true));
    }

    @Test
    public void testFadeInWithStatusBarExpansion() {
        // GIVEN view is attached
        mController.onViewAttached();
        captureStatusBarExpansionListeners();
        captureKeyguardStateControllerCallback();
        reset(mView);

        // WHEN status bar expansion is 0
        updateStatusBarExpansion(0, true);

        // THEN alpha is 0
        verify(mView).setUnpausedAlpha(0);
    }

    @Test
    public void testShowUdfpsBouncer() {
        // GIVEN view is attached and status bar expansion is 0
        mController.onViewAttached();
        captureStatusBarExpansionListeners();
        captureKeyguardStateControllerCallback();
        captureAltAuthInterceptor();
        updateStatusBarExpansion(0, true);
        reset(mView);
        when(mView.getContext()).thenReturn(mResourceContext);
        when(mResourceContext.getString(anyInt())).thenReturn("test string");

        // WHEN status bar expansion is 0 but udfps bouncer is requested
        mAltAuthInterceptor.showAlternateAuthBouncer();

        // THEN alpha is 255
        verify(mView).setUnpausedAlpha(255);
    }

    @Test
    public void testTransitionToFullShadeProgress() {
        // GIVEN view is attached and status bar expansion is 1f
        mController.onViewAttached();
        captureStatusBarExpansionListeners();
        updateStatusBarExpansion(1f, true);
        reset(mView);
        when(mView.getDialogSuggestedAlpha()).thenReturn(1f);

        // WHEN we're transitioning to the full shade
        float transitionProgress = .6f;
        mController.setTransitionToFullShadeProgress(transitionProgress);

        // THEN alpha is between 0 and 255
        verify(mView).setUnpausedAlpha((int) ((1f - transitionProgress) * 255));
    }

    @Test
    public void testShowUdfpsBouncer_transitionToFullShadeProgress() {
        // GIVEN view is attached and status bar expansion is 1f
        mController.onViewAttached();
        captureStatusBarExpansionListeners();
        captureKeyguardStateControllerCallback();
        captureAltAuthInterceptor();
        updateStatusBarExpansion(1f, true);
        mAltAuthInterceptor.showAlternateAuthBouncer();
        reset(mView);

        // WHEN we're transitioning to the full shade
        mController.setTransitionToFullShadeProgress(1.0f);

        // THEN alpha is 255 (b/c udfps bouncer is requested)
        verify(mView).setUnpausedAlpha(255);
    }

    @Test
    public void testUpdatePanelExpansion_pauseAuth() {
        // GIVEN view is attached + on the keyguard
        mController.onViewAttached();
        captureStatusBarStateListeners();
        captureStatusBarExpansionListeners();
        sendStatusBarStateChanged(StatusBarState.KEYGUARD);
        reset(mView);

        // WHEN panelViewExpansion changes to hide
        when(mView.getUnpausedAlpha()).thenReturn(0);
        updateStatusBarExpansion(0f, false);

        // THEN pause auth is updated to PAUSE
        verify(mView, atLeastOnce()).setPauseAuth(true);
    }

    @Test
    public void testUpdatePanelExpansion_unpauseAuth() {
        // GIVEN view is attached + on the keyguard + panel expansion is 0f
        mController.onViewAttached();
        captureStatusBarStateListeners();
        captureStatusBarExpansionListeners();
        sendStatusBarStateChanged(StatusBarState.KEYGUARD);
        reset(mView);

        // WHEN panelViewExpansion changes to expanded
        when(mView.getUnpausedAlpha()).thenReturn(255);
        updateStatusBarExpansion(1f, true);

        // THEN pause auth is updated to NOT pause
        verify(mView, atLeastOnce()).setPauseAuth(false);
    }

    private void captureBouncerExpansionCallback() {
        verify(mBouncer).addBouncerExpansionCallback(mBouncerExpansionCallbackCaptor.capture());
        mBouncerExpansionCallback = mBouncerExpansionCallbackCaptor.getValue();
    }
}
