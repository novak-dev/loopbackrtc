package com.example.loopbackrtc.client

import com.example.loopbackrtc.model.MainViewModel
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import timber.log.Timber

class SDPObserver(private val viewModel: MainViewModel, private val isCaller: Boolean): SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {
        p0?.let { sdp ->
            when (isCaller) {
                true -> viewModel.onCreateSuccessCaller(sdp)
                false ->  viewModel.onCreateSuccessCallee(sdp)
            }
        }
    }
    override fun onSetSuccess() {
        when (isCaller) {
            true -> Timber.i("SDP set successfully for caller")
            false -> Timber.i("SDP set successfully for callee")
        }
    }
    override fun onCreateFailure(p0: String?) { Timber.e("SDP creation failed: $p0") }
    override fun onSetFailure(p0: String?) { Timber.e("SDP set failed: $p0") }
}