package com.boostcampwm2023.snappoint.presentation.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.marginTop
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.boostcampwm2023.snappoint.R
import com.boostcampwm2023.snappoint.databinding.ActivityMainBinding
import com.boostcampwm2023.snappoint.presentation.auth.AuthActivity
import com.boostcampwm2023.snappoint.presentation.base.BaseActivity
import com.boostcampwm2023.snappoint.presentation.model.PositionState
import com.boostcampwm2023.snappoint.presentation.model.PostBlockState
import com.boostcampwm2023.snappoint.presentation.model.PostSummaryState
import com.boostcampwm2023.snappoint.presentation.model.SnapPointTag
import com.boostcampwm2023.snappoint.presentation.util.Constants
import com.boostcampwm2023.snappoint.presentation.util.Constants.API_KEY
import com.boostcampwm2023.snappoint.presentation.util.PermissionUtil.LOCATION_PERMISSION_REQUEST_CODE
import com.boostcampwm2023.snappoint.presentation.util.PermissionUtil.isMyLocationGranted
import com.boostcampwm2023.snappoint.presentation.util.PermissionUtil.isPermissionGranted
import com.boostcampwm2023.snappoint.presentation.util.PermissionUtil.locationPermissionRequest
import com.boostcampwm2023.snappoint.presentation.util.snapPointHeight
import com.boostcampwm2023.snappoint.presentation.util.snapPointWidth
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.search.SearchView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity(
) : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var placesClient: PlacesClient
    private val token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
    private val geocoder: Geocoder by lazy { Geocoder(applicationContext) }
    private lateinit var mapManager: MapManager

    private val navController: NavController by lazy {
        (supportFragmentManager.findFragmentById(R.id.fcv) as NavHostFragment).findNavController()
    }

    private val bottomSheetBehavior: BottomSheetBehavior<LinearLayout> by lazy {
        BottomSheetBehavior.from(binding.bs)
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initPlacesClient()

        initBinding()

        initBottomSheetWithNavigation()

        initMapFragment()

        collectViewModelData()

        setBottomNavigationEvent()

        initLocationData()

        cachingBottomSheetSize()
    }

    private fun initPlacesClient() {
        Places.initializeWithNewPlacesApiEnabled(applicationContext, API_KEY)
        placesClient = Places.createClient(this)
    }

    private fun initLocationData() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun initMapFragment() {
        val map: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.fcv_main_map) as SupportMapFragment
        mapManager = MapManager(viewModel, this)
        map.getMapAsync(mapManager)
    }

    private fun cachingBottomSheetSize() {
        with(binding) {
            root.post {
                viewModel.bottomSheetHeight =
                    (cl.height * Constants.BOTTOM_SHEET_HALF_EXPANDED_RATIO).toInt()
            }
        }
    }

    private fun collectViewModelData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {

                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            MainActivityEvent.OpenDrawer -> {
                                openDrawer()
                            }

                            MainActivityEvent.NavigatePrev -> {
                                navController.popBackStack()
                            }

                            MainActivityEvent.NavigateClose -> {
                                navController.popBackStack(R.id.previewFragment, true)
                                navController.popBackStack(R.id.clusterPreviewFragment, true)
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            }

                            is MainActivityEvent.NavigatePreview -> {
                                if (navController.currentDestination?.id != R.id.previewFragment) {
                                    cachingBottomSheetSize()
                                    openPreviewFragment()
                                }
                                moveCameraToFitScreen()
                            }

                            is MainActivityEvent.MoveCameraToAddress -> {
                                val address = viewModel.searchViewUiState.value.texts[event.index]
                                moveCameraToAddress(address)
                            }

                            is MainActivityEvent.NavigateSignIn -> {
                                navigateAuthActivity()
                            }

                            MainActivityEvent.CheckPermissionAndMoveCameraToUserLocation -> {
                                checkPermissionAndMoveCameraToUserLocation(true)
                            }

                            is MainActivityEvent.HalfOpenBottomSheet -> {
                                halfOpenBottomSheetWhenCollapsed()
                            }

                            is MainActivityEvent.GetAroundPostFailed -> {
                                showToastMessage(R.string.get_around_posts_failed)
                            }

                            is MainActivityEvent.NavigateCluster -> {
                                openClusterListFragment(event.tags)
                            }

                            is MainActivityEvent.AroundPostNotExist -> {
                                showToastMessage(R.string.post_not_exist)
                            }

                            is MainActivityEvent.CollapseBottomSheet -> {
                                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            }

                            is MainActivityEvent.NavigateAround -> {
                                while (true) {
                                    if (navController.popBackStack().not()) {
                                        break
                                    }
                                }
                                navController.navigate(R.id.aroundFragment)
                            }

                            is MainActivityEvent.DisplaySnapPoints -> {
                                updateMarkers(viewModel.getPosts())
                            }

                            is MainActivityEvent.SearchAroundPosts -> {
                                mapManager.searchSnapPoints()
                            }
                        }
                    }
                }

                launch {

                    viewModel.markerState.collect { markerState ->
                        if (markerState.selectedIndex < 0 || markerState.focusedIndex < 0) {
                            if(mapManager.googleMap != null) mapManager.removeMarkerFocus()
                            return@collect
                        }

                        val post = viewModel.getPosts()[markerState.selectedIndex]
                        val blocks = post.postBlocks.filter { it is PostBlockState.IMAGE || it is PostBlockState.VIDEO }[markerState.focusedIndex]
                        mapManager.changeSelectedMarker(blocks, SnapPointTag(post.uuid, blocks.uuid))

                        if (mapManager.prevSelectedIndex != markerState.selectedIndex) {
                            mapManager.changeRoute(viewModel.getPosts()[markerState.selectedIndex].postBlocks)
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect {
                        setMapGestureEnabled(it.isPreviewFragmentShowing || it.isClusterPreviewShowing)
                        mapManager.setClusteringEnabled(it.isPreviewFragmentShowing)
                        if (it.isClusterPreviewShowing.not() && mapManager.googleMap != null) {
                            mapManager.removeClusterFocus()
                        }
                    }
                }

                launch {
                    viewModel.postState.collect { postState ->
                        if(viewModel.uiState.value.isSubscriptionFragmentShowing.not()) {
                            updateMarkers(postState)
                        }
                    }
                }

                launch {
                    viewModel.localPostState.collect { localPostState ->
                        if(viewModel.uiState.value.isSubscriptionFragmentShowing) {
                            updateMarkers(localPostState)
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateMarkers(postState: List<PostSummaryState>) {
        while (mapManager.googleMap == null) { delay(100) }
        mapManager.updateMarkers(postState)
    }

    private fun setMapGestureEnabled(boolean: Boolean) {
        mapManager.setZoomGesturesEnabled(boolean.not())
        mapManager.setScrollGesturesEnabled(boolean.not())
    }

    private fun getMediaPositions(): List<PositionState> {
        val postIndex = viewModel.markerState.value.selectedIndex
        val snapPoints = viewModel.getPosts()[postIndex].postBlocks.filterNot { block ->
            block is PostBlockState.TEXT
        }
        val positions: List<PositionState> = snapPoints.map { block ->
            when (block) {
                is PostBlockState.IMAGE -> block.position
                is PostBlockState.VIDEO -> block.position
                else -> PositionState(0.0, 0.0)
            }
        }

        return positions
    }

    private fun moveCameraToFitScreen() {
        val positions: List<PositionState> = getMediaPositions()

        // 화면의 보이는 부분의 가로-세로 비율 계산
        // 단위: Pixel
        val widthOfMap: Double = binding.fcvMainMap.width.toDouble()
        val heightOfMap: Double = binding.fcvMainMap.height.toDouble()
        val heightOfLayout: Double = binding.cl.height.toDouble()
        val heightOfSearchBar: Int = maxOf(binding.topAppBar.height, binding.sb.height)

        val topSideRatio: Double = (heightOfSearchBar + snapPointHeight) / heightOfMap
        val bottomSideRatio: Double = (binding.bnv.height + Constants.BOTTOM_SHEET_HALF_EXPANDED_RATIO * heightOfLayout) / heightOfMap

        val visibleHeightRatio: Double = 1.0 - (topSideRatio + bottomSideRatio)
        val visibleWidthRatio: Double = (widthOfMap - snapPointWidth) / heightOfMap


        // 경로의 가로-세로 비율 계산
        // latitude: 북반구(+) 남반구(-)
        // longitude: 서쪽(-) 동쪽(+)
        val topOfBound: Double = positions.maxOf { it.latitude }
        val bottomOfBound: Double = positions.minOf { it.latitude }
        val leftOfBound: Double = positions.minOf { it.longitude }
        val rightOfBound: Double = positions.maxOf { it.longitude }

        val heightOfBound: Double = topOfBound - bottomOfBound
        val widthOfBound: Double = rightOfBound - leftOfBound

        // 경로가 가져야 하는 최소 높이 // 0으로 나누기 방지 최소값 적용
        var normalHeightOfBound: Double = maxOf(visibleHeightRatio * widthOfBound / visibleWidthRatio, 0.00001)

        var normalTopOfBound: Double = topOfBound
        var normalBottomOfBound: Double = bottomOfBound

        // 최소 높이보다 작으면 위 아래로 늘려주기
        if (heightOfBound < normalHeightOfBound) {
            normalTopOfBound += (normalHeightOfBound - heightOfBound) / 2
            normalBottomOfBound -= (normalHeightOfBound - heightOfBound) / 2
        }

        normalHeightOfBound = normalTopOfBound - normalBottomOfBound

        //
        val newTopOfBound: Double =
            normalTopOfBound + normalHeightOfBound * topSideRatio / visibleHeightRatio
        val newBottomOfBound: Double =
            normalBottomOfBound - normalHeightOfBound * bottomSideRatio / visibleHeightRatio

        val bound = LatLngBounds(
            LatLng(newBottomOfBound, leftOfBound),
            LatLng(newTopOfBound, rightOfBound)
        )

        mapManager.moveCamera(bound, 0)
    }

    private fun initBottomSheetWithNavigation() {
        binding.bnv.setupWithNavController(navController)
        bottomSheetBehavior.halfExpandedRatio = Constants.BOTTOM_SHEET_HALF_EXPANDED_RATIO
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        binding.sb.doOnLayout {
            bottomSheetBehavior.expandedOffset = binding.sb.height + binding.sb.marginTop * 2
            binding.bs.setPadding(0,0,0,binding.sb.height + binding.sb.marginTop * 2)
        }
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, state: Int) {
                viewModel.onBottomSheetChanged(state == BottomSheetBehavior.STATE_EXPANDED)
            }

            override fun onSlide(p0: View, p1: Float) {

            }
        })

        binding.bnv.setOnItemReselectedListener { _ ->
            if (viewModel.uiState.value.isPreviewFragmentShowing) {
                return@setOnItemReselectedListener
            }

            bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_HALF_EXPANDED -> BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                else -> BottomSheetBehavior.STATE_HALF_EXPANDED
            }
        }
    }

    private fun setBottomNavigationEvent() {
        binding.bnv.setOnItemSelectedListener { menuItem ->
            lifecycleScope.launch { mapManager.removeMarkerFocus() }
            while (true) {
                if (navController.popBackStack().not()) break
            }
            navController.navigate(menuItem.itemId)
            halfOpenBottomSheetWhenCollapsed()
            true
        }
    }

    private fun halfOpenBottomSheetWhenCollapsed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }
    }

    private fun openPreviewFragment() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        navController.navigate(R.id.previewFragment)
    }

    private fun openClusterListFragment(tags: List<SnapPointTag>) {
        val bundle = bundleOf(Constants.TAG_BUNDLE_KEY to tags.toTypedArray())
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        navController.navigate(R.id.clusterPreviewFragment, bundle)
    }

    private fun navigateAuthActivity() {
        startActivity(
            Intent(this, AuthActivity::class.java).apply {
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
            }
        )
        finish()
    }

    private fun openDrawer() {
        binding.dl.open()
    }

    private fun initBinding() {
        with(binding) {
            vm = viewModel

            fab.setOnClickListener {
                checkPermissionAndMoveCameraToUserLocation(false)
            }

            sv.editText.setOnEditorActionListener { v, _, _ ->
                getAddressAutoCompletion(v.text.toString())
                true
            }

            sv.addTransitionListener { _, _, afterState ->
                if (afterState == SearchView.TransitionState.HIDDEN) {
                    viewModel.updateAutoCompleteTexts(emptyList())
                }
            }
        }
    }

    private fun moveCameraToAddress(address: String) {

        with(binding) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(address, 1) { results ->
                    if (results.size == 0) {
                        runOnUiThread { showToastMessage(R.string.search_location_fail) }
                    } else {
                        runOnUiThread {
                            mapManager.moveCamera(results[0].latitude, results[0].longitude)
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                            sv.hide()
                        }
                    }
                }
            } else {
                // TODO - runBlocking 대체
                val results =
                    runBlocking(Dispatchers.IO) { geocoder.getFromLocationName(address, 1) }

                if (results == null || results.size == 0) {
                    showToastMessage(R.string.search_location_fail)
                } else {
                    mapManager.moveCamera(results[0].latitude, results[0].longitude)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    sv.hide()
                }
            }
        }
    }

    private fun getAddressAutoCompletion(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                viewModel.updateAutoCompleteTexts(response.autocompletePredictions.map {
                    it.getFullText(null).toString()
                })
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    Log.e("TAG", "Place not found: ${exception.statusCode}")
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun checkPermissionAndMoveCameraToUserLocation(boolean: Boolean) {
        if(this.isMyLocationGranted()){
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location ?: return@addOnSuccessListener
                    mapManager.moveCamera(latitude = location.latitude, longitude = location.longitude, zoom = 17.5f)
                    if (boolean) mapManager.searchSnapPoints()
                }
        }else{
            locationPermissionRequest()
        }
    }

    override fun onResume() {
        super.onResume()
        if(this.isMyLocationGranted()){
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(1000L).build(),
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
            return
        }

        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) || isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            showToastMessage(R.string.activity_main_permission_allow)
        } else {
            showToastMessage(R.string.activity_main_permission_deny)
        }
    }
}