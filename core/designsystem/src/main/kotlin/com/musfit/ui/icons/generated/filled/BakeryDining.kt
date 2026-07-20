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

public val Icons.Filled.BakeryDining: ImageVector
    get() {
        if (_bakeryDining != null) {
            return _bakeryDining!!
        }
        _bakeryDining = materialIcon(name = "Filled.BakeryDining") {
            materialPath(pathFillType = EvenOdd) {
                moveTo(19.28f, 16.34f)
                curveTo(18.07f, 15.45f, 17.46f, 15.0f, 17.46f, 15.0f)
                reflectiveCurveToRelative(0.32f, -0.59f, 0.96f, -1.78f)
                curveToRelative(0.38f, -0.59f, 1.22f, -0.59f, 1.6f, 0.0f)
                lineToRelative(0.81f, 1.26f)
                curveToRelative(0.19f, 0.3f, 0.21f, 0.68f, 0.06f, 1.0f)
                lineToRelative(-0.22f, 0.47f)
                curveTo(20.42f, 16.49f, 19.76f, 16.67f, 19.28f, 16.34f)
                close()
                moveTo(4.72f, 16.34f)
                curveToRelative(-0.48f, 0.33f, -1.13f, 0.15f, -1.39f, -0.38f)
                lineTo(3.1f, 15.49f)
                curveToRelative(-0.15f, -0.32f, -0.13f, -0.7f, 0.06f, -1.0f)
                lineToRelative(0.81f, -1.26f)
                curveToRelative(0.38f, -0.59f, 1.22f, -0.59f, 1.6f, 0.0f)
                curveTo(6.22f, 14.41f, 6.54f, 15.0f, 6.54f, 15.0f)
                reflectiveCurveTo(5.93f, 15.45f, 4.72f, 16.34f)
                close()
                moveTo(15.36f, 9.37f)
                curveToRelative(0.09f, -0.68f, 0.73f, -1.06f, 1.27f, -0.75f)
                lineToRelative(1.59f, 0.9f)
                curveToRelative(0.46f, 0.26f, 0.63f, 0.91f, 0.36f, 1.41f)
                lineTo(16.5f, 15.0f)
                horizontalLineToRelative(-1.8f)
                lineTo(15.36f, 9.37f)
                close()
                moveTo(8.63f, 9.37f)
                lineTo(9.3f, 15.0f)
                horizontalLineTo(7.5f)
                lineToRelative(-2.09f, -4.08f)
                curveToRelative(-0.27f, -0.5f, -0.1f, -1.15f, 0.36f, -1.41f)
                lineToRelative(1.59f, -0.9f)
                curveTo(7.89f, 8.31f, 8.54f, 8.69f, 8.63f, 9.37f)
                close()
                moveTo(13.8f, 15.0f)
                horizontalLineToRelative(-3.6f)
                lineTo(9.46f, 8.12f)
                curveTo(9.39f, 7.53f, 9.81f, 7.0f, 10.34f, 7.0f)
                horizontalLineToRelative(3.3f)
                curveToRelative(0.53f, 0.0f, 0.94f, 0.53f, 0.88f, 1.12f)
                lineTo(13.8f, 15.0f)
                close()
            }
        }
        return _bakeryDining!!
    }

private var _bakeryDining: ImageVector? = null
