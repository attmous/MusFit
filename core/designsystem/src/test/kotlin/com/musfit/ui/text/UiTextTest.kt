package com.musfit.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.musfit.core.designsystem.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UiTextTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `resource resolves typed arguments`() {
        val text = uiText(
            R.string.common_progress_count,
            UiText.Argument.Integer(2),
            UiText.Argument.Integer(5),
        )

        assertEquals("2 / 5", text.resolve(resources))
    }

    @Test
    fun `plural resolves quantity and typed argument`() {
        val one = pluralUiText(
            R.plurals.common_item_count,
            quantity = 1,
            UiText.Argument.Integer(1),
        )
        val many = pluralUiText(
            R.plurals.common_item_count,
            quantity = 3,
            UiText.Argument.Integer(3),
        )

        assertEquals("1 item", one.resolve(resources))
        assertEquals("3 items", many.resolve(resources))
    }

    @Test
    fun `resource resolves nested resource argument`() {
        val text = uiText(
            R.string.common_selected_value,
            UiText.Argument.Resource(R.string.common_save),
        )

        assertEquals("Selected: Save", text.resolve(resources))
    }

    @Test
    fun `resource resolves nested formatted text argument`() {
        val nested = uiText(
            R.string.common_selected_value,
            UiText.Argument.Resource(R.string.common_save),
        )
        val text = uiText(
            R.string.common_selected_value,
            UiText.Argument.Nested(nested),
        )

        assertEquals("Selected: Selected: Save", text.resolve(resources))
    }

    @Test
    fun `verbatim preserves user supplied text`() {
        assertEquals("User recipe", UiText.Verbatim("User recipe").resolve(resources))
    }
}
