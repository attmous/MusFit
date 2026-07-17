package com.musfit.ui.profile

import com.musfit.domain.profile.RecommendedTargets
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileEditContentTest {
    private val requested = RecommendedTargets(2_400.0, 150.0, 220.0, 70.0)
    private val changed = requested.copy(caloriesKcal = 2_500.0)

    @Test
    fun applyState_onlyAcknowledgesMatchingRepositoryConfirmedTargets() {
        assertEquals(
            TargetApplyState.Success,
            targetApplyStateForTargets(TargetApplyState.Success, requested, requested),
        )
        assertEquals(
            TargetApplyState.Failure,
            targetApplyStateForTargets(TargetApplyState.Failure, requested, requested),
        )
        assertEquals(
            TargetApplyState.Idle,
            targetApplyStateForTargets(TargetApplyState.Success, requested, changed),
        )
    }

    @Test
    fun applyingState_staysVisibleWhenDraftChangesDuringWrite() {
        assertEquals(
            TargetApplyState.Applying,
            targetApplyStateForTargets(TargetApplyState.Applying, requested, changed),
        )
    }
}
