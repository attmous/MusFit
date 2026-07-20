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
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.LunchDining: ImageVector
    get() {
        if (_lunchDining != null) {
            return _lunchDining!!
        }
        _lunchDining = materialIcon(name = "Filled.LunchDining") {
            materialPath(pathFillType = EvenOdd) {
                moveTo(22.0f, 10.0f)
                curveToRelative(0.32f, -3.28f, -4.28f, -6.0f, -9.99f, -6.0f)
                curveTo(6.3f, 4.0f, 1.7f, 6.72f, 2.02f, 10.0f)
                horizontalLineTo(22.0f)
                close()
            }
            materialPath(pathFillType = EvenOdd) {
                moveTo(5.35f, 13.5f)
                curveToRelative(0.55f, 0.0f, 0.78f, 0.14f, 1.15f, 0.36f)
                curveToRelative(0.45f, 0.27f, 1.07f, 0.64f, 2.18f, 0.64f)
                reflectiveCurveToRelative(1.73f, -0.37f, 2.18f, -0.64f)
                curveToRelative(0.37f, -0.23f, 0.59f, -0.36f, 1.15f, -0.36f)
                curveToRelative(0.55f, 0.0f, 0.78f, 0.14f, 1.15f, 0.36f)
                curveToRelative(0.45f, 0.27f, 1.07f, 0.64f, 2.18f, 0.64f)
                curveToRelative(1.11f, 0.0f, 1.73f, -0.37f, 2.18f, -0.64f)
                curveToRelative(0.37f, -0.23f, 0.59f, -0.36f, 1.15f, -0.36f)
                curveToRelative(0.55f, 0.0f, 0.78f, 0.14f, 1.15f, 0.36f)
                curveToRelative(0.45f, 0.27f, 1.07f, 0.63f, 2.17f, 0.64f)
                verticalLineToRelative(-1.98f)
                curveToRelative(0.0f, 0.0f, -0.79f, -0.16f, -1.16f, -0.38f)
                curveToRelative(-0.45f, -0.27f, -1.07f, -0.64f, -2.18f, -0.64f)
                curveToRelative(-1.11f, 0.0f, -1.73f, 0.37f, -2.18f, 0.64f)
                curveToRelative(-0.37f, 0.23f, -0.6f, 0.36f, -1.15f, 0.36f)
                reflectiveCurveToRelative(-0.78f, -0.14f, -1.15f, -0.36f)
                curveToRelative(-0.45f, -0.27f, -1.07f, -0.64f, -2.18f, -0.64f)
                reflectiveCurveToRelative(-1.73f, 0.37f, -2.18f, 0.64f)
                curveToRelative(-0.37f, 0.23f, -0.59f, 0.36f, -1.15f, 0.36f)
                curveToRelative(-0.55f, 0.0f, -0.78f, -0.14f, -1.15f, -0.36f)
                curveToRelative(-0.45f, -0.27f, -1.07f, -0.64f, -2.18f, -0.64f)
                curveToRelative(-1.11f, 0.0f, -1.73f, 0.37f, -2.18f, 0.64f)
                curveTo(2.78f, 12.37f, 2.56f, 12.5f, 2.0f, 12.5f)
                verticalLineToRelative(2.0f)
                curveToRelative(1.11f, 0.0f, 1.73f, -0.37f, 2.21f, -0.64f)
                curveTo(4.58f, 13.63f, 4.8f, 13.5f, 5.35f, 13.5f)
                close()
            }
            materialPath(pathFillType = EvenOdd) {
                moveTo(2.0f, 16.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(2.0f)
                close()
            }
        }
        return _lunchDining!!
    }

private var _lunchDining: ImageVector? = null
