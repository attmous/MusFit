/*
 * Copyright 2025 The Android Open Source Project
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

package com.musfit.ui.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.FlashlightOn: ImageVector
    get() {
        if (_flashlightOn != null) {
            return _flashlightOn!!
        }
        _flashlightOn = materialIcon(name = "Outlined.FlashlightOn") {
            materialPath {
                moveTo(18.0f, 2.0f)
                horizontalLineTo(6.0f)
                verticalLineToRelative(6.0f)
                lineToRelative(2.0f, 3.0f)
                verticalLineToRelative(11.0f)
                horizontalLineToRelative(8.0f)
                verticalLineTo(11.0f)
                lineToRelative(2.0f, -3.0f)
                verticalLineTo(2.0f)
                close()
                moveTo(16.0f, 4.0f)
                verticalLineToRelative(1.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(16.0f)
                close()
                moveTo(14.0f, 10.4f)
                verticalLineTo(20.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-9.61f)
                lineToRelative(-2.0f, -3.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(0.39f)
                lineTo(14.0f, 10.4f)
                close()
            }
            materialPath {
                moveTo(12.0f, 14.0f)
                moveToRelative(-1.5f, 0.0f)
                arcToRelative(1.5f, 1.5f, 0.0f, true, true, 3.0f, 0.0f)
                arcToRelative(1.5f, 1.5f, 0.0f, true, true, -3.0f, 0.0f)
            }
        }
        return _flashlightOn!!
    }

private var _flashlightOn: ImageVector? = null
