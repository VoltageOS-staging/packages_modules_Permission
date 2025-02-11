/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.permissioncontroller;

import android.content.Context;
import android.content.pm.PackageManager;

public class DeviceUtils {
    public static boolean isTelevision(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    public static boolean isWear(final Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
    }

    public static boolean isAuto(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    public static boolean isHandheld() {
        Context context = PermissionControllerApplication.get();
        return !isTelevision(context) && !isAuto(context) && !isWear(context);
    }

    public static boolean isHandheld(Context context) {
        return !isTelevision(context) && !isAuto(context) && !isWear(context);
    }
}
