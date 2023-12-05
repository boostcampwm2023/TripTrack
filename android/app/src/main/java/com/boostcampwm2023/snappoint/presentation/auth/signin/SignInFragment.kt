package com.boostcampwm2023.snappoint.presentation.auth.signin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.boostcampwm2023.snappoint.R
import com.boostcampwm2023.snappoint.databinding.FragmentSignInBinding
import com.boostcampwm2023.snappoint.presentation.auth.AuthViewModel
import com.boostcampwm2023.snappoint.presentation.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignInFragment : BaseFragment<FragmentSignInBinding>(R.layout.fragment_sign_in) {

    private val viewModel: SignInViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBinding()

        collectViewModelData()
    }

    private fun initBinding() {
        with(binding) {
            vm = viewModel
        }
    }

    private fun collectViewModelData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is SignInEvent.ShowMessage -> {
                            showToastMessage(event.errorResId)
                        }

                        is SignInEvent.NavigateToMainActivity -> {
                            navigateToMainActivity()
                        }

                        is SignInEvent.NavigateToSignup -> {
                            navigateToSignup()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(SignInFragmentDirections.actionLoginFragmentToMainActivity())
        requireActivity().finish()

    }

    private fun navigateToSignup() {
        findNavController().navigate(SignInFragmentDirections.actionSignInFragmentToSignUpFragment())
    }
}