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

public val Icons.Outlined.LunchDining: ImageVector
    get() {
        if (_lunchDining != null) {
            return _lunchDining!!
        }
        _lunchDining = materialIcon(name = "Outlined.LunchDining") {
            materialPath {
                moveTo(2.0f, 19.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineTo(2.0f)
                verticalLineTo(19.0f)
                close()
                moveTo(4.0f, 18.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(1.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(18.0f)
                close()
            }
            materialPath {
                moveTo(18.66f, 11.5f)
                curveToRelative(-1.95f, 0.0f, -2.09f, 1.0f, -3.33f, 1.0f)
                curveToRelative(-1.19f, 0.0f, -1.42f, -1.0f, -3.33f, -1.0f)
                curveToRelative(-1.95f, 0.0f, -2.09f, 1.0f, -3.33f, 1.0f)
                curveToRelative(-1.19f, 0.0f, -1.42f, -1.0f, -3.33f, -1.0f)
                curveToRelative(-1.95f, 0.0f, -2.09f, 1.0f, -3.33f, 1.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(1.9f, 0.0f, 2.17f, -1.0f, 3.35f, -1.0f)
                curveToRelative(1.19f, 0.0f, 1.42f, 1.0f, 3.33f, 1.0f)
                curveToRelative(1.95f, 0.0f, 2.09f, -1.0f, 3.33f, -1.0f)
                curveToRelative(1.19f, 0.0f, 1.42f, 1.0f, 3.33f, 1.0f)
                curveToRelative(1.95f, 0.0f, 2.09f, -1.0f, 3.33f, -1.0f)
                curveToRelative(1.19f, 0.0f, 1.4f, 0.98f, 3.32f, 1.0f)
                lineToRelative(-0.01f, -1.98f)
                curveTo(20.38f, 12.19f, 20.37f, 11.5f, 18.66f, 11.5f)
                close()
            }
            materialPath {
                moveTo(22.0f, 9.0f)
                curveToRelative(0.02f, -4.0f, -4.28f, -6.0f, -10.0f, -6.0f)
                curveTo(6.29f, 3.0f, 2.0f, 5.0f, 2.0f, 9.0f)
                verticalLineToRelative(1.0f)
                horizontalLineToRelative(20.0f)
                lineTo(22.0f, 9.0f)
                lineTo(22.0f, 9.0f)
                close()
                moveTo(4.18f, 8.0f)
                curveTo(5.01f, 5.81f, 8.61f, 5.0f, 12.0f, 5.0f)
                curveToRelative(3.31f, 0.0f, 5.93f, 0.73f, 7.19f, 1.99f)
                curveTo(19.49f, 7.3f, 19.71f, 7.63f, 19.84f, 8.0f)
                horizontalLineTo(4.18f)
                close()
            }
        }
        return _lunchDining!!
    }

private var _lunchDining: ImageVector? = null
