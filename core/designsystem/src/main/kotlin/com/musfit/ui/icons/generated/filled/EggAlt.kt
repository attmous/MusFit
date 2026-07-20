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

public val Icons.Filled.EggAlt: ImageVector
    get() {
        if (_eggAlt != null) {
            return _eggAlt!!
        }
        _eggAlt = materialIcon(name = "Filled.EggAlt") {
            materialPath {
                moveTo(19.0f, 9.0f)
                curveTo(17.0f, 7.0f, 15.99f, 2.0f, 9.97f, 2.0f)
                curveTo(4.95f, 2.0f, 1.94f, 6.0f, 2.0f, 11.52f)
                curveTo(2.06f, 17.04f, 6.96f, 19.0f, 9.97f, 19.0f)
                curveToRelative(2.01f, 0.0f, 2.01f, 3.0f, 6.02f, 3.0f)
                curveTo(19.0f, 22.0f, 22.0f, 19.0f, 22.0f, 15.02f)
                curveTo(22.0f, 12.0f, 21.01f, 11.0f, 19.0f, 9.0f)
                close()
                moveTo(12.0f, 15.5f)
                curveToRelative(-1.93f, 0.0f, -3.5f, -1.57f, -3.5f, -3.5f)
                reflectiveCurveToRelative(1.57f, -3.5f, 3.5f, -3.5f)
                reflectiveCurveToRelative(3.5f, 1.57f, 3.5f, 3.5f)
                reflectiveCurveTo(13.93f, 15.5f, 12.0f, 15.5f)
                close()
            }
        }
        return _eggAlt!!
    }

private var _eggAlt: ImageVector? = null
