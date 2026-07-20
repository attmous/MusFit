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

public val Icons.Filled.Egg: ImageVector
    get() {
        if (_egg != null) {
            return _egg!!
        }
        _egg = materialIcon(name = "Filled.Egg") {
            materialPath {
                moveTo(12.0f, 3.0f)
                curveTo(8.5f, 3.0f, 5.0f, 9.33f, 5.0f, 14.0f)
                curveToRelative(0.0f, 3.87f, 3.13f, 7.0f, 7.0f, 7.0f)
                reflectiveCurveToRelative(7.0f, -3.13f, 7.0f, -7.0f)
                curveTo(19.0f, 9.33f, 15.5f, 3.0f, 12.0f, 3.0f)
                close()
                moveTo(13.0f, 18.0f)
                curveToRelative(-3.0f, 0.0f, -5.0f, -1.99f, -5.0f, -5.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                curveToRelative(0.0f, 2.92f, 2.42f, 3.0f, 3.0f, 3.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                reflectiveCurveTo(13.55f, 18.0f, 13.0f, 18.0f)
                close()
            }
        }
        return _egg!!
    }

private var _egg: ImageVector? = null
