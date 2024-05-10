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

package com.android.systemui.deviceentry.domain.interactor

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.keyguard.data.repository.DeviceEntryFaceAuthRepository
import com.android.systemui.keyguard.shared.model.FailedFaceAuthenticationStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance

@SysUISingleton
class DeviceEntryFaceAuthInteractor
@Inject
constructor(
    repository: DeviceEntryFaceAuthRepository,
) {
    val faceFailure: Flow<FailedFaceAuthenticationStatus> =
        repository.authenticationStatus.filterIsInstance<FailedFaceAuthenticationStatus>()
}
