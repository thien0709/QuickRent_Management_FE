package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import com.bxt.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
) :ViewModel(){

}