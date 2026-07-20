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

public val Icons.Filled.Balance: ImageVector
    get() {
        if (_balance != null) {
            return _balance!!
        }
        _balance = materialIcon(name = "Filled.Balance") {
            materialPath {
                moveTo(13.0f, 7.83f)
                curveToRelative(0.85f, -0.3f, 1.53f, -0.98f, 1.83f, -1.83f)
                horizontalLineTo(18.0f)
                lineToRelative(-3.0f, 7.0f)
                curveToRelative(0.0f, 1.66f, 1.57f, 3.0f, 3.5f, 3.0f)
                reflectiveCurveToRelative(3.5f, -1.34f, 3.5f, -3.0f)
                lineToRelative(-3.0f, -7.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(4.0f)
                horizontalLineToRelative(-6.17f)
                curveTo(14.42f, 2.83f, 13.31f, 2.0f, 12.0f, 2.0f)
                reflectiveCurveTo(9.58f, 2.83f, 9.17f, 4.0f)
                lineTo(3.0f, 4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(-3.0f, 7.0f)
                curveToRelative(0.0f, 1.66f, 1.57f, 3.0f, 3.5f, 3.0f)
                reflectiveCurveTo(9.0f, 14.66f, 9.0f, 13.0f)
                lineTo(6.0f, 6.0f)
                horizontalLineToRelative(3.17f)
                curveToRelative(0.3f, 0.85f, 0.98f, 1.53f, 1.83f, 1.83f)
                verticalLineTo(19.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(20.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-9.0f)
                verticalLineTo(7.83f)
                close()
                moveTo(20.37f, 13.0f)
                horizontalLineToRelative(-3.74f)
                lineToRelative(1.87f, -4.36f)
                lineTo(20.37f, 13.0f)
                close()
                moveTo(7.37f, 13.0f)
                horizontalLineTo(3.63f)
                lineTo(5.5f, 8.64f)
                lineTo(7.37f, 13.0f)
                close()
                moveTo(12.0f, 6.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                curveTo(13.0f, 5.55f, 12.55f, 6.0f, 12.0f, 6.0f)
                close()
            }
        }
        return _balance!!
    }

private var _balance: ImageVector? = null
