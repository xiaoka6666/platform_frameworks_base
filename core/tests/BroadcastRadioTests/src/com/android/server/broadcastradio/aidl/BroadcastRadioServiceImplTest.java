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
package com.android.server.broadcastradio.aidl;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doAnswer;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;

import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.broadcastradio.IBroadcastRadio;
import android.hardware.radio.ITuner;
import android.hardware.radio.ITunerCallback;
import android.hardware.radio.RadioManager;
import android.hardware.radio.RadioTuner;
import android.os.IBinder;
import android.os.IServiceCallback;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.dx.mockito.inline.extended.StaticMockitoSessionBuilder;
import com.android.server.broadcastradio.ExtendedRadioMockitoTestCase;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;

public final class BroadcastRadioServiceImplTest extends ExtendedRadioMockitoTestCase {

    private static final int FM_RADIO_MODULE_ID = 0;
    private static final int DAB_RADIO_MODULE_ID = 1;
    private static final ArrayList<String> SERVICE_LIST =
            new ArrayList<>(Arrays.asList("FmService", "DabService"));

    private BroadcastRadioServiceImpl mBroadcastRadioService;
    private IBinder.DeathRecipient mFmDeathRecipient;

    @Mock
    private RadioManager.ModuleProperties mFmModuleMock;
    @Mock
    private RadioManager.ModuleProperties mDabModuleMock;
    @Mock
    private RadioModule mFmRadioModuleMock;
    @Mock
    private RadioModule mDabRadioModuleMock;
    @Mock
    private IBroadcastRadio mFmHalServiceMock;
    @Mock
    private IBroadcastRadio mDabHalServiceMock;
    @Mock
    private IBinder mFmBinderMock;
    @Mock
    private IBinder mDabBinderMock;
    @Mock
    private TunerSession mFmTunerSessionMock;
    @Mock
    private ITunerCallback mTunerCallbackMock;

    @Override
    protected void initializeSession(StaticMockitoSessionBuilder builder) {
        builder.spyStatic(ServiceManager.class)
                .spyStatic(RadioModule.class);
    }

    @Test
    public void listModules_withMultipleServiceNames() throws Exception {
        createBroadcastRadioService();

        assertWithMessage("Radio modules in AIDL broadcast radio HAL client")
                .that(mBroadcastRadioService.listModules())
                .containsExactly(mFmModuleMock, mDabModuleMock);
    }

    @Test
    public void hasModules_withIdFoundInModules() throws Exception {
        createBroadcastRadioService();

        assertWithMessage("DAB radio module in AIDL broadcast radio HAL client")
                .that(mBroadcastRadioService.hasModule(DAB_RADIO_MODULE_ID)).isTrue();
    }

    @Test
    public void hasModules_withIdNotFoundInModules() throws Exception {
        createBroadcastRadioService();

        assertWithMessage("Radio module of id not found in AIDL broadcast radio HAL client")
                .that(mBroadcastRadioService.hasModule(DAB_RADIO_MODULE_ID + 1)).isFalse();
    }

    @Test
    public void hasAnyModules_withModulesExist() throws Exception {
        createBroadcastRadioService();

        assertWithMessage("Any radio module in AIDL broadcast radio HAL client")
                .that(mBroadcastRadioService.hasAnyModules()).isTrue();
    }

    @Test
    public void openSession_withIdFound() throws Exception {
        createBroadcastRadioService();

        ITuner session = mBroadcastRadioService.openSession(FM_RADIO_MODULE_ID,
                /* legacyConfig= */ null, /* withAudio= */ true, mTunerCallbackMock);

        assertWithMessage("Session opened in FM radio module")
                .that(session).isEqualTo(mFmTunerSessionMock);
    }

    @Test
    public void openSession_withIdNotFound() throws Exception {
        createBroadcastRadioService();

        ITuner session = mBroadcastRadioService.openSession(DAB_RADIO_MODULE_ID + 1,
                /* legacyConfig= */ null, /* withAudio= */ true, mTunerCallbackMock);

        assertWithMessage("Session opened with id not found").that(session).isNull();
    }

    @Test
    public void binderDied_forDeathRecipient() throws Exception {
        createBroadcastRadioService();

        mFmDeathRecipient.binderDied();

        verify(mFmRadioModuleMock).closeSessions(eq(RadioTuner.ERROR_HARDWARE_FAILURE));
        assertWithMessage("FM radio module after FM broadcast radio HAL service died")
                .that(mBroadcastRadioService.hasModule(FM_RADIO_MODULE_ID)).isFalse();
    }

    private void createBroadcastRadioService() throws RemoteException {
        mockServiceManager();
        mBroadcastRadioService = new BroadcastRadioServiceImpl(SERVICE_LIST);
    }

    private void mockServiceManager() throws RemoteException {
        doAnswer((Answer<Void>) invocation -> {
            String serviceName = (String) invocation.getArguments()[0];
            IServiceCallback serviceCallback = (IServiceCallback) invocation.getArguments()[1];
            IBinder mockBinder = serviceName.equals("FmService") ? mFmBinderMock : mDabBinderMock;
            serviceCallback.onRegistration(serviceName, mockBinder);
            return null;
        }).when(() -> ServiceManager.registerForNotifications(anyString(),
                any(IServiceCallback.class)));

        doReturn(mFmRadioModuleMock).when(() -> RadioModule.tryLoadingModule(
                eq(FM_RADIO_MODULE_ID), anyString(), any(IBinder.class), any(Object.class)));
        doReturn(mDabRadioModuleMock).when(() -> RadioModule.tryLoadingModule(
                eq(DAB_RADIO_MODULE_ID), anyString(), any(IBinder.class), any(Object.class)));

        when(mFmRadioModuleMock.getProperties()).thenReturn(mFmModuleMock);
        when(mDabRadioModuleMock.getProperties()).thenReturn(mDabModuleMock);

        when(mFmRadioModuleMock.getService()).thenReturn(mFmHalServiceMock);
        when(mDabRadioModuleMock.getService()).thenReturn(mDabHalServiceMock);

        when(mFmHalServiceMock.asBinder()).thenReturn(mFmBinderMock);
        when(mDabHalServiceMock.asBinder()).thenReturn(mDabBinderMock);

        doAnswer(invocation -> {
            mFmDeathRecipient = (IBinder.DeathRecipient) invocation.getArguments()[0];
            return null;
        }).when(mFmBinderMock).linkToDeath(any(), anyInt());

        when(mFmRadioModuleMock.openSession(eq(mTunerCallbackMock)))
                .thenReturn(mFmTunerSessionMock);
    }
}
