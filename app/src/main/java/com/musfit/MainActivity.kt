package com.musfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.musfit.data.repository.AccountRepository
import com.musfit.ui.AppNavGraph
import com.musfit.ui.theme.MusFitTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var accountRepository: AccountRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            accountRepository.ensureActiveAccount()
        }
        configureMusFitEdgeToEdge()
        setContent {
            MusFitTheme {
                AppNavGraph()
            }
        }
    }
}
