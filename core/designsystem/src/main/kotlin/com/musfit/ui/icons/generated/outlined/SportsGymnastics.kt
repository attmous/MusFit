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

public val Icons.Outlined.SportsGymnastics: ImageVector
    get() {
        if (_sportsGymnastics != null) {
            return _sportsGymnastics!!
        }
        _sportsGymnastics = materialIcon(name = "Outlined.SportsGymnastics") {
            materialPath {
                moveTo(4.0f, 6.0f)
                curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
                reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
                reflectiveCurveTo(7.1f, 8.0f, 6.0f, 8.0f)
                reflectiveCurveTo(4.0f, 7.1f, 4.0f, 6.0f)
                close()
                moveTo(1.0f, 9.0f)
                horizontalLineToRelative(6.0f)
                lineToRelative(7.0f, -5.0f)
                lineToRelative(1.31f, 1.52f)
                lineTo(11.14f, 8.5f)
                horizontalLineTo(14.0f)
                lineTo(21.8f, 4.0f)
                lineTo(23.0f, 5.4f)
                lineTo(14.5f, 12.0f)
                lineTo(14.0f, 22.0f)
                horizontalLineToRelative(-2.0f)
                lineToRelative(-0.5f, -10.0f)
                lineTo(8.0f, 11.0f)
                horizontalLineTo(1.0f)
                verticalLineTo(9.0f)
                close()
            }
        }
        return _sportsGymnastics!!
    }

private var _sportsGymnastics: ImageVector? = null
