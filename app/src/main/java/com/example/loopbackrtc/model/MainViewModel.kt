package com.example.loopbackrtc.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loopbackrtc.client.LoopbackVideoCapturer
import com.example.loopbackrtc.client.PCObserver
import com.example.loopbackrtc.client.SDPObserver
import kotlinx.coroutines.launch
import org.webrtc.*
import timber.log.Timber
import java.io.InputStream


const val AUDIO_TRACK_ID = "loopback-audio"
const val VIDEO_TRACK_ID = "loopback-video"

class MainViewModel : ViewModel() {

    private lateinit var factory: PeerConnectionFactory
    private lateinit var callerPeerConnection: PeerConnection
    private lateinit var calleePeerConnection: PeerConnection
    private val eglBase = EglBase.create()
    lateinit var capturer: LoopbackVideoCapturer

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


    fun createOffer(context: Context) {
        viewModelScope.launch {
            callerPeerConnection = factory.createPeerConnection(ArrayList(), callerPCObserver)!!
            calleePeerConnection = factory.createPeerConnection(ArrayList(), calleePCObserver)!!
            callerPeerConnection.addTrack(createVideoTrack(context))
            callerPeerConnection.createOffer(callerSDPObserver, MediaConstraints())
        }
    }

    fun endSession() {
        callerPeerConnection.dispose()
        calleePeerConnection.dispose()
    }

    private fun createVideoTrack(context: Context): VideoTrack? {

        val videoSource = factory.createVideoSource(false)
        val track = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            "captureThread", eglBase.eglBaseContext);
        capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver);
        track.setEnabled(true)
        capturer.startCapture(352, 240, 30);
        return track;
    }

    fun createAnswer() {
        viewModelScope.launch {
            calleePeerConnection.createAnswer(calleeSDPObserver, MediaConstraints())
        }
    }

    fun createPeerConnectionFactory() {
        Timber.i("createPeerConnectionFactory")
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

}