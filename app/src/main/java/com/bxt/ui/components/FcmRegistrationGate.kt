package com.bxt.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.bxt.viewmodel.FcmRegistrationViewModel

@Composable
fun FcmRegistrationGate(vm: FcmRegistrationViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.registerIfLoggedIn() }
}
