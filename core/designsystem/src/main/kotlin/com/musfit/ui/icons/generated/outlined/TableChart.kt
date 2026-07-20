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

public val Icons.Outlined.TableChart: ImageVector
    get() {
        if (_tableChart != null) {
            return _tableChart!!
        }
        _tableChart = materialIcon(name = "Outlined.TableChart") {
            materialPath {
                moveTo(20.0f, 3.0f)
                lineTo(5.0f, 3.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(15.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(22.0f, 5.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(20.0f, 5.0f)
                verticalLineToRelative(3.0f)
                lineTo(5.0f, 8.0f)
                lineTo(5.0f, 5.0f)
                horizontalLineToRelative(15.0f)
                close()
                moveTo(15.0f, 19.0f)
                horizontalLineToRelative(-5.0f)
                verticalLineToRelative(-9.0f)
                horizontalLineToRelative(5.0f)
                verticalLineToRelative(9.0f)
                close()
                moveTo(5.0f, 10.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(9.0f)
                lineTo(5.0f, 19.0f)
                verticalLineToRelative(-9.0f)
                close()
                moveTo(17.0f, 19.0f)
                verticalLineToRelative(-9.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(9.0f)
                horizontalLineToRelative(-3.0f)
                close()
            }
        }
        return _tableChart!!
    }

private var _tableChart: ImageVector? = null
