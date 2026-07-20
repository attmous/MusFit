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

public val Icons.Outlined.Cookie: ImageVector
    get() {
        if (_cookie != null) {
            return _cookie!!
        }
        _cookie = materialIcon(name = "Outlined.Cookie") {
            materialPath {
                moveTo(10.5f, 8.5f)
                moveToRelative(-1.5f, 0.0f)
                arcToRelative(1.5f, 1.5f, 0.0f, true, true, 3.0f, 0.0f)
                arcToRelative(1.5f, 1.5f, 0.0f, true, true, -3.0f, 0.0f)
            }
            materialPath {
                moveTo(8.5f, 13.5f)
                moveToRelative(-1.5f, 0.0f)
                arcToRelative(1.5f, 1.5f, 0.0f, true, true, 3.0f, 0.0f)
                arcToRelative(1.5f, 1.5f, 0.0f, true, true, -3.0f, 0.0f)
            }
            materialPath {
                moveTo(15.0f, 15.0f)
                moveToRelative(-1.0f, 0.0f)
                arcToRelative(1.0f, 1.0f, 0.0f, true, true, 2.0f, 0.0f)
                arcToRelative(1.0f, 1.0f, 0.0f, true, true, -2.0f, 0.0f)
            }
            materialPath {
                moveTo(21.95f, 10.99f)
                curveToRelative(-1.79f, -0.03f, -3.7f, -1.95f, -2.68f, -4.22f)
                curveToRelative(-2.97f, 1.0f, -5.78f, -1.59f, -5.19f, -4.56f)
                curveTo(7.11f, 0.74f, 2.0f, 6.41f, 2.0f, 12.0f)
                curveToRelative(0.0f, 5.52f, 4.48f, 10.0f, 10.0f, 10.0f)
                curveTo(17.89f, 22.0f, 22.54f, 16.92f, 21.95f, 10.99f)
                close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
                curveToRelative(0.0f, -3.31f, 2.73f, -8.18f, 8.08f, -8.02f)
                curveToRelative(0.42f, 2.54f, 2.44f, 4.56f, 4.99f, 4.94f)
                curveToRelative(0.07f, 0.36f, 0.52f, 2.55f, 2.92f, 3.63f)
                curveTo(19.7f, 16.86f, 16.06f, 20.0f, 12.0f, 20.0f)
                close()
            }
        }
        return _cookie!!
    }

private var _cookie: ImageVector? = null
