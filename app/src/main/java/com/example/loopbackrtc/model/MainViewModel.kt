package com.example.loopbackrtc.model

import android.annotation.SuppressLint
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loopbackrtc.client.PCObserver
import com.example.loopbackrtc.client.SDPObserver
import kotlinx.coroutines.launch
import org.webrtc.*
import timber.log.Timber
const val VIDEO_TRACK_ID = "loopback-video"

class MainViewModel : ViewModel() {

    private lateinit var factory: PeerConnectionFactory
    private lateinit var callerPeerConnection: PeerConnection
    private lateinit var calleePeerConnection: PeerConnection

    // TODO: fix this mess
    lateinit var calleeSurface: SurfaceViewRenderer
    lateinit var track: VideoTrack

    private val callerPCObserver = PCObserver(this, isCaller = true)
    private val calleePCObserver = PCObserver(this, isCaller = false)

    private val callerSDPObserver = SDPObserver(this, isCaller = true)
    private val calleeSDPObserver = SDPObserver(this, isCaller = false)


    fun onCallerCandidate(candidate: IceCandidate) {
        Timber.i("Caller ice candidate: $candidate. Sending to callee.")
        calleePeerConnection.addIceCandidate(candidate)
    }

    fun onCalleeCandidate(candidate: IceCandidate) {
        Timber.i("Callee ice candidate: $candidate. Sending to caller.")
        callerPeerConnection.addIceCandidate(candidate)
    }

    fun onCreateSuccessCaller(sdp: SessionDescription) {
        Timber.i("Setting local description for caller and remote description for callee: ${sdp.description}")
        callerPeerConnection.setLocalDescription(callerSDPObserver, sdp)
        calleePeerConnection.setRemoteDescription(calleeSDPObserver, sdp)
    }

    fun onCreateSuccessCallee(sdp: SessionDescription) {
        Timber.i("Setting local description for callee and remote description for caller: ${sdp.description}")
        calleePeerConnection.setLocalDescription(calleeSDPObserver, sdp)
        callerPeerConnection.setRemoteDescription(callerSDPObserver, sdp)
    }

    fun createOffer() {
        viewModelScope.launch {
            callerPeerConnection = factory.createPeerConnection(ArrayList(), callerPCObserver)!!
            calleePeerConnection = factory.createPeerConnection(ArrayList(), calleePCObserver)!!
            val stream = factory.createLocalMediaStream("loopback-stream")
            stream.addTrack(track)
            callerPeerConnection.addStream(stream)
            callerPeerConnection.createOffer(callerSDPObserver, MediaConstraints())
        }
    }

    fun endSession() {
        callerPeerConnection.dispose()
        calleePeerConnection.dispose()
    }

    fun createVideoSource(): VideoSource = factory.createVideoSource(false)

    fun createVideoTrack(videoSource: VideoSource): VideoTrack {
        return factory.createVideoTrack(VIDEO_TRACK_ID, videoSource)
    }

    fun createAnswer() {
        viewModelScope.launch {
            calleePeerConnection.createAnswer(calleeSDPObserver, MediaConstraints())
        }
    }

    fun onRemoteStream(stream: MediaStream) {
        stream.videoTracks[0]?.addSink(calleeSurface)
    }

    fun createPeerConnectionFactory() {
        Timber.i("createPeerConnectionFactory")
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(SoftwareVideoEncoderFactory())
            .setVideoDecoderFactory(SoftwareVideoDecoderFactory())
            .createPeerConnectionFactory()
    }

}