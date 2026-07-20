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

public val Icons.Outlined.Eco: ImageVector
    get() {
        if (_eco != null) {
            return _eco!!
        }
        _eco = materialIcon(name = "Outlined.Eco") {
            materialPath {
                moveTo(6.05f, 8.05f)
                curveToRelative(-2.73f, 2.73f, -2.73f, 7.17f, 0.0f, 9.9f)
                curveTo(7.42f, 19.32f, 9.21f, 20.0f, 11.0f, 20.0f)
                reflectiveCurveToRelative(3.58f, -0.68f, 4.95f, -2.05f)
                curveTo(19.43f, 14.47f, 20.0f, 4.0f, 20.0f, 4.0f)
                reflectiveCurveTo(9.53f, 4.57f, 6.05f, 8.05f)
                close()
                moveTo(14.54f, 16.54f)
                curveTo(13.59f, 17.48f, 12.34f, 18.0f, 11.0f, 18.0f)
                curveToRelative(-0.89f, 0.0f, -1.73f, -0.25f, -2.48f, -0.68f)
                curveToRelative(0.92f, -2.88f, 2.62f, -5.41f, 4.88f, -7.32f)
                curveToRelative(-2.63f, 1.36f, -4.84f, 3.46f, -6.37f, 6.0f)
                curveToRelative(-1.48f, -1.96f, -1.35f, -4.75f, 0.44f, -6.54f)
                curveTo(9.21f, 7.72f, 14.04f, 6.65f, 17.8f, 6.2f)
                curveTo(17.35f, 9.96f, 16.28f, 14.79f, 14.54f, 16.54f)
                close()
            }
        }
        return _eco!!
    }

private var _eco: ImageVector? = null
