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

public val Icons.Outlined.FlashlightOff: ImageVector
    get() {
        if (_flashlightOff != null) {
            return _flashlightOff!!
        }
        _flashlightOff = materialIcon(name = "Outlined.FlashlightOff") {
            materialPath {
                moveTo(2.81f, 2.81f)
                lineTo(1.39f, 4.22f)
                lineTo(8.0f, 10.83f)
                verticalLineTo(22.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-3.17f)
                lineToRelative(3.78f, 3.78f)
                lineToRelative(1.41f, -1.41f)
                lineTo(2.81f, 2.81f)
                close()
                moveTo(14.0f, 20.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-7.17f)
                lineToRelative(4.0f, 4.0f)
                verticalLineTo(20.0f)
                close()
            }
            materialPath {
                moveTo(16.0f, 4.0f)
                lineToRelative(0.0f, 1.0f)
                lineToRelative(-8.17f, 0.0f)
                lineToRelative(2.0f, 2.0f)
                lineToRelative(6.17f, 0.0f)
                lineToRelative(0.0f, 0.39f)
                lineToRelative(-2.0f, 3.01f)
                lineToRelative(0.0f, 0.77f)
                lineToRelative(2.0f, 2.0f)
                lineToRelative(0.0f, -2.17f)
                lineToRelative(2.0f, -3.0f)
                lineToRelative(0.0f, -6.0f)
                lineToRelative(-12.0f, 0.0f)
                lineToRelative(0.0f, 1.17f)
                lineToRelative(0.83f, 0.83f)
                close()
            }
        }
        return _flashlightOff!!
    }

private var _flashlightOff: ImageVector? = null
