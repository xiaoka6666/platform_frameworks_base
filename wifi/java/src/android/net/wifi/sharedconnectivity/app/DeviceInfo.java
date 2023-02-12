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

package android.net.wifi.sharedconnectivity.app;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.wifi.sharedconnectivity.service.SharedConnectivityService;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A data class representing a device providing connectivity.
 * This class is used in IPC calls between the implementer of {@link SharedConnectivityService} and
 * the consumers of {@link com.android.wifitrackerlib}.
 *
 * @hide
 */
@SystemApi
public final class DeviceInfo implements Parcelable {

    /**
     * Device type providing connectivity is unknown.
     */
    public static final int DEVICE_TYPE_UNKNOWN = 0;

    /**
     * Device providing connectivity is a mobile phone.
     */
    public static final int DEVICE_TYPE_PHONE = 1;

    /**
     * Device providing connectivity is a tablet.
     */
    public static final int DEVICE_TYPE_TABLET = 2;

    /**
     * Device providing connectivity is a laptop.
     */
    public static final int DEVICE_TYPE_LAPTOP = 3;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DEVICE_TYPE_UNKNOWN,
            DEVICE_TYPE_PHONE,
            DEVICE_TYPE_TABLET,
            DEVICE_TYPE_LAPTOP
    })
    public @interface DeviceType {}

    @DeviceType private final int mDeviceType;
    private final String mDeviceName;
    private final String mModelName;
    private final int mBatteryPercentage;
    private final int mConnectionStrength;

    /**
     * Builder class for {@link DeviceInfo}.
     */
    public static final class Builder {
        private int mDeviceType;
        private String mDeviceName;
        private String mModelName;
        private int mBatteryPercentage;
        private int mConnectionStrength;

        public Builder() {}

        /**
         * Sets the device type that provides connectivity.
         *
         * @param deviceType Device type as represented by IntDef {@link DeviceType}.
         * @return Returns the Builder object.
         */
        @NonNull
        public Builder setDeviceType(@DeviceType int deviceType) {
            mDeviceType = deviceType;
            return this;
        }

        /**
         * Sets the device name of the remote device.
         *
         * @param deviceName The user configurable device name.
         * @return Returns the Builder object.
         */
        @NonNull
        public Builder setDeviceName(@NonNull String deviceName) {
            mDeviceName = deviceName;
            return this;
        }

        /**
         * Sets the model name of the remote device.
         *
         * @param modelName The OEM configured name for the device model.
         * @return Returns the Builder object.
         */
        @NonNull
        public Builder setModelName(@NonNull String modelName) {
            mModelName = modelName;
            return this;
        }

        /**
         * Sets the battery charge percentage of the remote device.
         *
         * @param batteryPercentage The battery charge percentage in the range 0 to 100.
         * @return Returns the Builder object.
         */
        @NonNull
        public Builder setBatteryPercentage(@IntRange(from = 0, to = 100) int batteryPercentage) {
            mBatteryPercentage = batteryPercentage;
            return this;
        }

        /**
         * Sets the displayed connection strength of the remote device to the internet.
         *
         * @param connectionStrength Connection strength in range 0 to 3.
         * @return Returns the Builder object.
         */
        @NonNull
        public Builder setConnectionStrength(@IntRange(from = 0, to = 3) int connectionStrength) {
            mConnectionStrength = connectionStrength;
            return this;
        }

        /**
         * Builds the {@link DeviceInfo} object.
         *
         * @return Returns the built {@link DeviceInfo} object.
         */
        @NonNull
        public DeviceInfo build() {
            return new DeviceInfo(mDeviceType, mDeviceName, mModelName, mBatteryPercentage,
                    mConnectionStrength);
        }
    }

    private static void validate(int deviceType, String deviceName, String modelName,
            int batteryPercentage, int connectionStrength) {
        if (deviceType != DEVICE_TYPE_UNKNOWN && deviceType != DEVICE_TYPE_PHONE
                && deviceType != DEVICE_TYPE_TABLET && deviceType != DEVICE_TYPE_LAPTOP) {
            throw new IllegalArgumentException("Illegal device type");
        }
        if (Objects.isNull(deviceName)) {
            throw new IllegalArgumentException("DeviceName must be set");
        }
        if (Objects.isNull(modelName)) {
            throw new IllegalArgumentException("ModelName must be set");
        }
        if (batteryPercentage < 0 || batteryPercentage > 100) {
            throw new IllegalArgumentException("BatteryPercentage must be in range 0-100");
        }
        if (connectionStrength < 0 || connectionStrength > 3) {
            throw new IllegalArgumentException("ConnectionStrength must be in range 0-3");
        }
    }

    private DeviceInfo(@DeviceType int deviceType, @NonNull String deviceName,
            @NonNull String modelName, int batteryPercentage, int connectionStrength) {
        validate(deviceType, deviceName, modelName, batteryPercentage, connectionStrength);
        mDeviceType = deviceType;
        mDeviceName = deviceName;
        mModelName = modelName;
        mBatteryPercentage = batteryPercentage;
        mConnectionStrength = connectionStrength;
    }

    /**
     * Gets the device type that provides connectivity.
     *
     * @return Returns the device type as represented by IntDef {@link DeviceType}.
     */
    @DeviceType
    public int getDeviceType() {
        return mDeviceType;
    }

    /**
     * Gets the device name of the remote device.
     *
     * @return Returns the user configurable device name.
     */
    @NonNull
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Gets the model name of the remote device.
     *
     * @return Returns the OEM configured name for the device model.
     */
    @NonNull
    public String getModelName() {
        return mModelName;
    }

    /**
     * Gets the battery charge percentage of the remote device.
     *
     * @return Returns the battery charge percentage in the range 0 to 100.
     */
    @IntRange(from = 0, to = 100)
    public int getBatteryPercentage() {
        return mBatteryPercentage;
    }

    /**
     * Gets the displayed connection strength of the remote device to the internet.
     *
     * @return Returns the connection strength in range 0 to 3.
     */
    @IntRange(from = 0, to = 3)
    public int getConnectionStrength() {
        return mConnectionStrength;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DeviceInfo)) return false;
        DeviceInfo other = (DeviceInfo) obj;
        return mDeviceType == other.getDeviceType()
                && Objects.equals(mDeviceName, other.mDeviceName)
                && Objects.equals(mModelName, other.mModelName)
                && mBatteryPercentage == other.mBatteryPercentage
                && mConnectionStrength == other.mConnectionStrength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDeviceType, mDeviceName, mModelName, mBatteryPercentage,
                mConnectionStrength);
    }
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mDeviceType);
        dest.writeString(mDeviceName);
        dest.writeString(mModelName);
        dest.writeInt(mBatteryPercentage);
        dest.writeInt(mConnectionStrength);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
        @Override
        public DeviceInfo createFromParcel(Parcel in) {
            return new DeviceInfo(in.readInt(), in.readString(), in.readString(), in.readInt(),
                    in.readInt());
        }

        @Override
        public DeviceInfo[] newArray(int size) {
            return new DeviceInfo[size];
        }
    };

    @Override
    public String toString() {
        return new StringBuilder("DeviceInfo[")
                .append("deviceType=").append(mDeviceType)
                .append(", deviceName=").append(mDeviceName)
                .append(", modelName=").append(mModelName)
                .append(", batteryPercentage=").append(mBatteryPercentage)
                .append(", connectionStrength=").append(mConnectionStrength)
                .append("]").toString();
    }
}
