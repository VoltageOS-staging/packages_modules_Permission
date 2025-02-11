/*
 * Copyright (C) 2019 The Android Open Source Project
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
@file:Suppress("DEPRECATION")

package com.android.permissioncontroller.permission.model.livedatatypes

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.Attribution
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import android.util.Log
import com.android.modules.utils.build.SdkLevel
import com.android.permissioncontroller.permission.utils.ContextCompat
import com.android.permissioncontroller.permission.utils.Utils

/**
 * A lighter version of the system's PackageInfo class, containing select information about the
 * package.
 *
 * @param packageName The name of the packages
 * @param permissions The list of LightPermInfos representing the permissions this package defines
 * @param requestedPermissions The names of the permissions this package requests
 * @param requestedPermissionsFlags The grant state of the permissions this package requests
 * @param uid The UID of this package
 * @param targetSdkVersion The target SDK of this package
 * @param isInstantApp Whether or not this package is an instant app
 * @param enabled Whether or not this package is enabled.
 */
data class LightPackageInfo(
    val packageName: String,
    val permissions: List<LightPermInfo>,
    val requestedPermissions: List<String>,
    var requestedPermissionsFlags: List<Int>,
    val uid: Int,
    val targetSdkVersion: Int,
    val isInstantApp: Boolean,
    val enabled: Boolean,
    val appFlags: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val areAttributionsUserVisible: Boolean,
    val attributionTagsToLabels: Map<String, Int>,
    var deviceId: Int
) {
    constructor(
        pI: PackageInfo
    ) : this(
        pI.packageName,
        pI.permissions?.map { perm ->
            LightPermInfo(perm, pI.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM != 0)
        } ?: emptyList(),
        pI.requestedPermissions?.toList() ?: emptyList(),
        pI.requestedPermissionsFlags?.toList() ?: emptyList(),
        pI.applicationInfo!!.uid,
        pI.applicationInfo!!.targetSdkVersion,
        pI.applicationInfo!!.isInstantApp,
        pI.applicationInfo!!.enabled,
        pI.applicationInfo!!.flags,
        pI.firstInstallTime,
        pI.lastUpdateTime,
        if (SdkLevel.isAtLeastS()) pI.applicationInfo!!.areAttributionsUserVisible() else false,
        if (SdkLevel.isAtLeastS()) buildAttributionTagsToLabelsMap(pI.attributions) else emptyMap(),
        ContextCompat.DEVICE_ID_DEFAULT
    )

    constructor(
        pI: PackageInfo,
        deviceId: Int,
        requestedPermissionsFlagsForDevice: List<Int>
    ) : this(pI) {
        this.deviceId = deviceId
        this.requestedPermissionsFlags = requestedPermissionsFlagsForDevice
    }

    /** Permissions which are granted according to the [requestedPermissionsFlags] */
    val grantedPermissions: List<String>
        get() {
            val grantedPermissions = mutableListOf<String>()
            for (i in 0 until requestedPermissions.size) {
                if (
                    (requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                ) {
                    grantedPermissions.add(requestedPermissions[i])
                }
            }
            return grantedPermissions
        }

    /**
     * Gets the ApplicationInfo for this package from the system. Can be expensive if called too
     * often.
     *
     * @param app The current application, which will be used to get the ApplicationInfo
     * @return The ApplicationInfo corresponding to this package, with this UID, or null, if no such
     *   package exists
     */
    fun getApplicationInfo(app: Application): ApplicationInfo? {
        try {
            val userContext = Utils.getUserContext(app, UserHandle.getUserHandleForUid(uid))
            return userContext.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {}
        return null
    }

    /**
     * Gets the PackageInfo for this package from the system. Can be expensive if called too often.
     *
     * @param app The current application, which will be used to get the PackageInfo
     * @return The PackageInfo corresponding to this package, with this UID, or null, if no such
     *   package exists
     */
    fun toPackageInfo(app: Application): PackageInfo? {
        try {
            val userContext = Utils.getUserContext(app, UserHandle.getUserHandleForUid(uid))
            return userContext.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(
                LightPackageInfo::class.java.simpleName,
                "Failed to get real package info for $packageName, $uid",
                e
            )
        }
        return null
    }

    /** Companion object for [LightPackageInfo]. */
    companion object {
        /** Creates a mapping of attribution tag to labels from the provided attributions. */
        fun buildAttributionTagsToLabelsMap(attributions: Array<Attribution>?): Map<String, Int> {
            val attributionTagToLabel = mutableMapOf<String, Int>()
            attributions?.forEach { attributionTagToLabel[it.tag] = it.label }

            return attributionTagToLabel.toMap()
        }
    }
}
