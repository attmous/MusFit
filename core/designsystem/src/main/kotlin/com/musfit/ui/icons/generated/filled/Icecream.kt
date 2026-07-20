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

public val Icons.Filled.Icecream: ImageVector
    get() {
        if (_icecream != null) {
            return _icecream!!
        }
        _icecream = materialIcon(name = "Filled.Icecream") {
            materialPath(pathFillType = EvenOdd) {
                moveTo(8.79f, 12.4f)
                lineToRelative(3.26f, 6.22f)
                lineToRelative(3.17f, -6.21f)
                curveToRelative(-0.11f, -0.08f, -0.21f, -0.16f, -0.3f, -0.25f)
                curveTo(14.08f, 12.69f, 13.07f, 13.0f, 12.0f, 13.0f)
                reflectiveCurveToRelative(-2.08f, -0.31f, -2.92f, -0.84f)
                curveTo(8.99f, 12.25f, 8.89f, 12.33f, 8.79f, 12.4f)
                close()
                moveTo(6.83f, 12.99f)
                curveTo(5.25f, 12.9f, 4.0f, 11.6f, 4.0f, 10.0f)
                curveToRelative(0.0f, -1.49f, 1.09f, -2.73f, 2.52f, -2.96f)
                curveTo(6.75f, 4.22f, 9.12f, 2.0f, 12.0f, 2.0f)
                reflectiveCurveToRelative(5.25f, 2.22f, 5.48f, 5.04f)
                curveTo(18.91f, 7.27f, 20.0f, 8.51f, 20.0f, 10.0f)
                curveToRelative(0.0f, 1.59f, -1.24f, 2.9f, -2.81f, 2.99f)
                lineTo(12.07f, 23.0f)
                lineTo(6.83f, 12.99f)
                close()
            }
        }
        return _icecream!!
    }

private var _icecream: ImageVector? = null
