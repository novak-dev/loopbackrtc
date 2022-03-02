package com.example.loopbackrtc

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.loopbackrtc.client.LoopbackVideoCapturer
import com.example.loopbackrtc.databinding.MainActivityBinding
import com.example.loopbackrtc.model.MainViewModel
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainActivityBinding
    private val viewModel: MainViewModel by viewModels()
    private val eglBase = EglBase.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        initializeFactory()
        binding = MainActivityBinding.inflate(layoutInflater)
        binding.signalButton.setOnClickListener { createOffer() }
        setContentView(binding.root)
        viewModel.capturer = LoopbackVideoCapturer(applicationContext.resources.openRawResource(R.raw.garden))
        binding.surfaceView.init(eglBase.eglBaseContext, null);
        binding.surfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        binding.surfaceView.setEnableHardwareScaler(false)

    }

    private fun createOffer() {
        Timber.i("createOffer")
        viewModel.createOffer(applicationContext) // TODO: need to not pass in context
        binding.signalButton.text = getString(R.string.create_answer)
        binding.signalButton.setOnClickListener { createAnswer() }
    }

    private fun createAnswer() {
        Timber.i("createAnswer")
        viewModel.createAnswer()
        binding.signalButton.text = getString(R.string.end_session)
        binding.signalButton.setOnClickListener { endSession() }
    }

    private fun endSession() {
        Timber.i("End session")
        viewModel.endSession()
        binding.signalButton.text = getString(R.string.create_offer)
        binding.signalButton.setOnClickListener { createOffer() }
    }

    private fun initializeFactory() {
        PeerConnectionFactory.initialize(PeerConnectionFactory
            .InitializationOptions.builder(applicationContext)
            .createInitializationOptions())
        viewModel.createPeerConnectionFactory()
    }


}