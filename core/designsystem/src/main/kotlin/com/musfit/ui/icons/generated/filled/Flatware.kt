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

public val Icons.Filled.Flatware: ImageVector
    get() {
        if (_flatware != null) {
            return _flatware!!
        }
        _flatware = materialIcon(name = "Filled.Flatware") {
            materialPath {
                moveTo(16.0f, 7.08f)
                curveToRelative(0.0f, 1.77f, -0.84f, 3.25f, -2.0f, 3.82f)
                verticalLineTo(21.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(10.9f)
                curveToRelative(-1.16f, -0.57f, -2.0f, -2.05f, -2.0f, -3.82f)
                curveTo(10.01f, 4.83f, 11.35f, 3.0f, 13.0f, 3.0f)
                curveTo(14.66f, 3.0f, 16.0f, 4.83f, 16.0f, 7.08f)
                close()
                moveTo(17.0f, 3.0f)
                verticalLineToRelative(18.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(7.0f)
                curveTo(21.0f, 5.24f, 19.76f, 3.0f, 17.0f, 3.0f)
                close()
                moveTo(8.28f, 3.0f)
                curveToRelative(-0.4f, 0.0f, -0.72f, 0.32f, -0.72f, 0.72f)
                verticalLineTo(7.0f)
                horizontalLineTo(6.72f)
                verticalLineTo(3.72f)
                curveTo(6.72f, 3.32f, 6.4f, 3.0f, 6.0f, 3.0f)
                reflectiveCurveTo(5.28f, 3.32f, 5.28f, 3.72f)
                verticalLineTo(7.0f)
                horizontalLineTo(4.44f)
                verticalLineTo(3.72f)
                curveTo(4.44f, 3.32f, 4.12f, 3.0f, 3.72f, 3.0f)
                reflectiveCurveTo(3.0f, 3.32f, 3.0f, 3.72f)
                verticalLineTo(9.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                verticalLineToRelative(10.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(11.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(3.72f)
                curveTo(9.0f, 3.32f, 8.68f, 3.0f, 8.28f, 3.0f)
                close()
            }
        }
        return _flatware!!
    }

private var _flatware: ImageVector? = null
