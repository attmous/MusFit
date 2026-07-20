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

public val Icons.Outlined.DocumentScanner: ImageVector
    get() {
        if (_documentScanner != null) {
            return _documentScanner!!
        }
        _documentScanner = materialIcon(name = "Outlined.DocumentScanner") {
            materialPath {
                moveTo(7.0f, 3.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(3.0f)
                horizontalLineTo(2.0f)
                verticalLineTo(1.0f)
                horizontalLineToRelative(5.0f)
                verticalLineTo(3.0f)
                close()
                moveTo(22.0f, 6.0f)
                verticalLineTo(1.0f)
                horizontalLineToRelative(-5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(3.0f)
                horizontalLineTo(22.0f)
                close()
                moveTo(7.0f, 21.0f)
                horizontalLineTo(4.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(5.0f)
                horizontalLineToRelative(5.0f)
                verticalLineTo(21.0f)
                close()
                moveTo(20.0f, 18.0f)
                verticalLineToRelative(3.0f)
                horizontalLineToRelative(-3.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(5.0f)
                verticalLineToRelative(-5.0f)
                horizontalLineTo(20.0f)
                close()
                moveTo(17.0f, 6.0f)
                horizontalLineTo(7.0f)
                verticalLineToRelative(12.0f)
                horizontalLineToRelative(10.0f)
                verticalLineTo(6.0f)
                close()
                moveTo(19.0f, 18.0f)
                curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                horizontalLineTo(7.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                verticalLineTo(6.0f)
                curveToRelative(0.0f, -1.1f, 0.9f, -2.0f, 2.0f, -2.0f)
                horizontalLineToRelative(10.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                verticalLineTo(18.0f)
                close()
                moveTo(15.0f, 8.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(6.0f)
                verticalLineTo(8.0f)
                close()
                moveTo(15.0f, 11.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(6.0f)
                verticalLineTo(11.0f)
                close()
                moveTo(15.0f, 14.0f)
                horizontalLineTo(9.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(6.0f)
                verticalLineTo(14.0f)
                close()
            }
        }
        return _documentScanner!!
    }

private var _documentScanner: ImageVector? = null
