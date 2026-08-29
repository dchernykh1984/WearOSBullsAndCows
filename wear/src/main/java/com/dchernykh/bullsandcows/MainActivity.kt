package com.dchernykh.bullsandcows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dchernykh.bullsandcows.store.DataStoreRecordStore
import com.dchernykh.bullsandcows.ui.BullsAndCowsApp

/**
 * The one and only activity. A watch game is a single full-screen surface with no
 * navigation to speak of, so there is nothing for a second one to do.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { DataStoreRecordStore(applicationContext) }
            BullsAndCowsApp(viewModel(factory = BullsAndCowsViewModel.factory(store)))
        }
    }
}
