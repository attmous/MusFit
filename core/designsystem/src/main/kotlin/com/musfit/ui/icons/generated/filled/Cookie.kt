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

package com.musfit.ui.icons.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.Cookie: ImageVector
    get() {
        if (_cookie != null) {
            return _cookie!!
        }
        _cookie = materialIcon(name = "Filled.Cookie") {
            materialPath {
                moveTo(21.95f, 10.99f)
                curveToRelative(-1.79f, -0.03f, -3.7f, -1.95f, -2.68f, -4.22f)
                curveToRelative(-2.98f, 1.0f, -5.77f, -1.59f, -5.19f, -4.56f)
                curveTo(6.95f, 0.71f, 2.0f, 6.58f, 2.0f, 12.0f)
                curveToRelative(0.0f, 5.52f, 4.48f, 10.0f, 10.0f, 10.0f)
                curveTo(17.89f, 22.0f, 22.54f, 16.92f, 21.95f, 10.99f)
                close()
                moveTo(8.5f, 15.0f)
                curveTo(7.67f, 15.0f, 7.0f, 14.33f, 7.0f, 13.5f)
                reflectiveCurveTo(7.67f, 12.0f, 8.5f, 12.0f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(9.33f, 15.0f, 8.5f, 15.0f)
                close()
                moveTo(10.5f, 10.0f)
                curveTo(9.67f, 10.0f, 9.0f, 9.33f, 9.0f, 8.5f)
                reflectiveCurveTo(9.67f, 7.0f, 10.5f, 7.0f)
                reflectiveCurveTo(12.0f, 7.67f, 12.0f, 8.5f)
                reflectiveCurveTo(11.33f, 10.0f, 10.5f, 10.0f)
                close()
                moveTo(15.0f, 16.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                curveTo(16.0f, 15.55f, 15.55f, 16.0f, 15.0f, 16.0f)
                close()
            }
        }
        return _cookie!!
    }

private var _cookie: ImageVector? = null
