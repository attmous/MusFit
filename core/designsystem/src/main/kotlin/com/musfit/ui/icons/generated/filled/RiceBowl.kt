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

public val Icons.Filled.RiceBowl: ImageVector
    get() {
        if (_riceBowl != null) {
            return _riceBowl!!
        }
        _riceBowl = materialIcon(name = "Filled.RiceBowl") {
            materialPath {
                moveTo(22.0f, 12.0f)
                lineTo(22.0f, 12.0f)
                curveToRelative(0.0f, -5.52f, -4.48f, -10.0f, -10.0f, -10.0f)
                reflectiveCurveTo(2.0f, 6.48f, 2.0f, 12.0f)
                curveToRelative(0.0f, 3.69f, 2.47f, 6.86f, 6.0f, 8.25f)
                verticalLineTo(22.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-1.75f)
                curveTo(19.53f, 18.86f, 22.0f, 15.69f, 22.0f, 12.0f)
                close()
                moveTo(20.0f, 12.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(5.08f)
                curveTo(18.39f, 6.47f, 20.0f, 9.05f, 20.0f, 12.0f)
                close()
                moveTo(14.0f, 4.26f)
                verticalLineTo(12.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(4.26f)
                curveTo(10.64f, 4.1f, 11.31f, 4.0f, 12.0f, 4.0f)
                reflectiveCurveTo(13.36f, 4.1f, 14.0f, 4.26f)
                close()
                moveTo(4.0f, 12.0f)
                curveToRelative(0.0f, -2.95f, 1.61f, -5.53f, 4.0f, -6.92f)
                verticalLineTo(12.0f)
                horizontalLineTo(4.0f)
                close()
            }
        }
        return _riceBowl!!
    }

private var _riceBowl: ImageVector? = null
