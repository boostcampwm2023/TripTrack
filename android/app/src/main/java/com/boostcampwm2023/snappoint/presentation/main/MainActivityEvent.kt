package com.boostcampwm2023.snappoint.presentation.main

import com.boostcampwm2023.snappoint.presentation.model.SnapPointTag

sealed class MainActivityEvent {
    data object OpenDrawer: MainActivityEvent()
    data object NavigatePrev: MainActivityEvent()
    data object NavigateClose: MainActivityEvent()
    data object NavigatePreview: MainActivityEvent()
    data class MoveCameraToAddress(val index: Int): MainActivityEvent()
    data object NavigateSignIn: MainActivityEvent()
    data object CheckPermissionAndMoveCameraToUserLocation : MainActivityEvent()
    data object HalfOpenBottomSheet: MainActivityEvent()
    data object GetAroundPostFailed: MainActivityEvent()
    data class NavigateCluster(val tags: List<SnapPointTag>): MainActivityEvent()
    data object AroundPostNotExist: MainActivityEvent()
    data object CollapseBottomSheet: MainActivityEvent()
    data object NavigateAround: MainActivityEvent()
    data object DisplaySnapPoints: MainActivityEvent()
    data object SearchAroundPosts: MainActivityEvent()
}