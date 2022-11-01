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

package com.android.systemui.keyguard.data.repository

import androidx.test.filters.SmallTest
import com.android.systemui.SysuiTestCase
import com.android.systemui.common.shared.model.Position
import com.android.systemui.doze.DozeHost
import com.android.systemui.keyguard.WakefulnessLifecycle
import com.android.systemui.keyguard.shared.model.WakefulnessModel
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.statusbar.policy.KeyguardStateController
import com.android.systemui.util.mockito.argumentCaptor
import com.android.systemui.util.mockito.whenever
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(JUnit4::class)
class KeyguardRepositoryImplTest : SysuiTestCase() {

    @Mock private lateinit var statusBarStateController: StatusBarStateController
    @Mock private lateinit var dozeHost: DozeHost
    @Mock private lateinit var keyguardStateController: KeyguardStateController
    @Mock private lateinit var wakefulnessLifecycle: WakefulnessLifecycle

    private lateinit var underTest: KeyguardRepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        underTest =
            KeyguardRepositoryImpl(
                statusBarStateController,
                keyguardStateController,
                dozeHost,
                wakefulnessLifecycle,
            )
    }

    @Test
    fun animateBottomAreaDozingTransitions() = runBlockingTest {
        assertThat(underTest.animateBottomAreaDozingTransitions.value).isEqualTo(false)

        underTest.setAnimateDozingTransitions(true)
        assertThat(underTest.animateBottomAreaDozingTransitions.value).isTrue()

        underTest.setAnimateDozingTransitions(false)
        assertThat(underTest.animateBottomAreaDozingTransitions.value).isFalse()

        underTest.setAnimateDozingTransitions(true)
        assertThat(underTest.animateBottomAreaDozingTransitions.value).isTrue()
    }

    @Test
    fun bottomAreaAlpha() = runBlockingTest {
        assertThat(underTest.bottomAreaAlpha.value).isEqualTo(1f)

        underTest.setBottomAreaAlpha(0.1f)
        assertThat(underTest.bottomAreaAlpha.value).isEqualTo(0.1f)

        underTest.setBottomAreaAlpha(0.2f)
        assertThat(underTest.bottomAreaAlpha.value).isEqualTo(0.2f)

        underTest.setBottomAreaAlpha(0.3f)
        assertThat(underTest.bottomAreaAlpha.value).isEqualTo(0.3f)

        underTest.setBottomAreaAlpha(0.5f)
        assertThat(underTest.bottomAreaAlpha.value).isEqualTo(0.5f)

        underTest.setBottomAreaAlpha(1.0f)
        assertThat(underTest.bottomAreaAlpha.value).isEqualTo(1f)
    }

    @Test
    fun clockPosition() = runBlockingTest {
        assertThat(underTest.clockPosition.value).isEqualTo(Position(0, 0))

        underTest.setClockPosition(0, 1)
        assertThat(underTest.clockPosition.value).isEqualTo(Position(0, 1))

        underTest.setClockPosition(1, 9)
        assertThat(underTest.clockPosition.value).isEqualTo(Position(1, 9))

        underTest.setClockPosition(1, 0)
        assertThat(underTest.clockPosition.value).isEqualTo(Position(1, 0))

        underTest.setClockPosition(3, 1)
        assertThat(underTest.clockPosition.value).isEqualTo(Position(3, 1))
    }

    @Test
    fun isKeyguardShowing() = runBlockingTest {
        whenever(keyguardStateController.isShowing).thenReturn(false)
        var latest: Boolean? = null
        val job = underTest.isKeyguardShowing.onEach { latest = it }.launchIn(this)

        assertThat(latest).isFalse()
        assertThat(underTest.isKeyguardShowing()).isFalse()

        val captor = argumentCaptor<KeyguardStateController.Callback>()
        verify(keyguardStateController).addCallback(captor.capture())

        whenever(keyguardStateController.isShowing).thenReturn(true)
        captor.value.onKeyguardShowingChanged()
        assertThat(latest).isTrue()
        assertThat(underTest.isKeyguardShowing()).isTrue()

        whenever(keyguardStateController.isShowing).thenReturn(false)
        captor.value.onKeyguardShowingChanged()
        assertThat(latest).isFalse()
        assertThat(underTest.isKeyguardShowing()).isFalse()

        job.cancel()
    }

    @Test
    fun isDozing() = runBlockingTest {
        var latest: Boolean? = null
        val job = underTest.isDozing.onEach { latest = it }.launchIn(this)

        val captor = argumentCaptor<DozeHost.Callback>()
        verify(dozeHost).addCallback(captor.capture())

        captor.value.onDozingChanged(true)
        assertThat(latest).isTrue()

        captor.value.onDozingChanged(false)
        assertThat(latest).isFalse()

        job.cancel()
        verify(dozeHost).removeCallback(captor.value)
    }

    @Test
    fun `isDozing - starts with correct initial value for isDozing`() = runBlockingTest {
        var latest: Boolean? = null

        whenever(statusBarStateController.isDozing).thenReturn(true)
        var job = underTest.isDozing.onEach { latest = it }.launchIn(this)
        assertThat(latest).isTrue()
        job.cancel()

        whenever(statusBarStateController.isDozing).thenReturn(false)
        job = underTest.isDozing.onEach { latest = it }.launchIn(this)
        assertThat(latest).isFalse()
        job.cancel()
    }

    @Test
    fun dozeAmount() = runBlockingTest {
        val values = mutableListOf<Float>()
        val job = underTest.dozeAmount.onEach(values::add).launchIn(this)

        val captor = argumentCaptor<StatusBarStateController.StateListener>()
        verify(statusBarStateController).addCallback(captor.capture())

        captor.value.onDozeAmountChanged(0.433f, 0.4f)
        captor.value.onDozeAmountChanged(0.498f, 0.5f)
        captor.value.onDozeAmountChanged(0.661f, 0.65f)

        assertThat(values).isEqualTo(listOf(0f, 0.4f, 0.5f, 0.65f))

        job.cancel()
        verify(statusBarStateController).removeCallback(captor.value)
    }

    @Test
    fun wakefullness() = runBlockingTest {
        val values = mutableListOf<WakefulnessModel>()
        val job = underTest.wakefulnessState.onEach(values::add).launchIn(this)

        val captor = argumentCaptor<WakefulnessLifecycle.Observer>()
        verify(wakefulnessLifecycle).addObserver(captor.capture())

        captor.value.onStartedWakingUp()
        captor.value.onFinishedWakingUp()
        captor.value.onStartedGoingToSleep()
        captor.value.onFinishedGoingToSleep()

        assertThat(values)
            .isEqualTo(
                listOf(
                    // Initial value will be ASLEEP
                    WakefulnessModel.ASLEEP,
                    WakefulnessModel.STARTING_TO_WAKE,
                    WakefulnessModel.AWAKE,
                    WakefulnessModel.STARTING_TO_SLEEP,
                    WakefulnessModel.ASLEEP,
                )
            )

        job.cancel()
        verify(wakefulnessLifecycle).removeObserver(captor.value)
    }
}
