package com.boostcampwm2023.snappoint.presentation.main.setting

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boostcampwm2023.snappoint.data.repository.RoomRepository
import com.boostcampwm2023.snappoint.data.repository.SignInRepository
import com.boostcampwm2023.snappoint.data.repository.UserInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val userInfoRepository: UserInfoRepository,
    private val signInRepository: SignInRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _event: MutableSharedFlow<SettingEvent> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val event: SharedFlow<SettingEvent> = _event.asSharedFlow()

    fun onSignOutClick() {
        _event.tryEmit(SettingEvent.SignOut)
    }

    fun signOut() {
        signInRepository.getSignOut()
            .onEach {
                userInfoRepository.clearUserAuthData()
                _event.emit(SettingEvent.SuccessToSignOut)
            }
            .catch {
                _event.emit(SettingEvent.FailToSignOut)
            }
            .launchIn(viewModelScope)
    }

    fun onClearSnapPointClick() {
        _event.tryEmit(SettingEvent.RemoveSnapPoint)
    }

    fun getSavedPost() {
        roomRepository.getAllLocalPost(userInfoRepository.getEmail())
            .onEach {
                Log.d("LOG", it.toString())
            }.catch {
                Log.d("LOG", "Catch: ${it.message}}")
            }.launchIn(viewModelScope)
    }
}