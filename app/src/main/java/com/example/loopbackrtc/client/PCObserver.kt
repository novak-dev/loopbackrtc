package com.example.loopbackrtc.client
import com.example.loopbackrtc.model.MainViewModel
import org.webrtc.*
import timber.log.Timber

class PCObserver(private val viewModel: MainViewModel, private val isCaller: Boolean) : PeerConnection.Observer {
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        p0.let { state ->
            when (isCaller) {
                true -> Timber.i("Signaling changed state for caller to $state")
                false -> Timber.i("Signaling changed state for callee to $state")
            }
        }
    }
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        p0.let { state ->
            when (isCaller) {
                true -> Timber.i("Ice connection changed state for caller to $state")
                false -> Timber.i("Ice connection changed state for callee to $state")
            }
        }
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Timber.i("onIceConnectionReceivingChange $p0")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        when (isCaller) {
            true -> Timber.i("Caller: ICE gathering state changed to $p0")
            false ->Timber.i("Callee: ICE gathering state changed to $p0")
        }
    }

    override fun onIceCandidate(p0: IceCandidate?) {
        p0?.let { candidate ->
            when (isCaller) {
                true -> viewModel.onCallerCandidate(candidate)
                else -> viewModel.onCalleeCandidate(candidate)
            }
        }
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Timber.i("onIceCandidatesRemoved")
    }
    override fun onAddStream(p0: MediaStream?) {
        Timber.i("onAddStream")
    }
    override fun onRemoveStream(p0: MediaStream?) {
        Timber.i("onRemoveStream")
    }
    override fun onDataChannel(p0: DataChannel?) {
        Timber.i("onDataChannel")
    }
    override fun onRenegotiationNeeded() {
        Timber.i("onRenegotiationNeeded")
    }
    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Timber.i("onAddTrack receiver id: ${p0?.id()}")
    }
}