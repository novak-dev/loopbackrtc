package com.example.loopbackrtc.client;
/*
 *  This is a refactor of VideoFileCapturer
 *  to use an Input Stream as we are not storing
 *  the resource on the file system.
 */
import android.content.Context;
import android.os.SystemClock;
import org.webrtc.CapturerObserver;
import org.webrtc.JavaI420Buffer;
import org.webrtc.Logging;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class LoopbackVideoCapturer implements VideoCapturer {

    private interface VideoReader {
        VideoFrame getNextFrame();
    }
    /**
     * Read video data from input stream .y4m file.
     */
    private static class VideoReaderY4M implements VideoReader {
        private static final String TAG = "VideoReaderY4M";
        private static final String FRAME_DELIMITER = "FRAME\n";
        private static final int FRAME_DELIMETER_LENGTH = FRAME_DELIMITER.length();
        private final int frameWidth;
        private final int frameHeight;
        private final BufferChannel videoBuffer;

        public VideoReaderY4M(InputStream stream) throws IOException {
            StringBuilder builder = new StringBuilder();
            for (; ; ) {
                int c = stream.read();
                Logging.d(TAG, "Got character: " + (char)c);
                if (c == -1) {
                    // End of file reached.
                    throw new RuntimeException("Found end of header before end of stream");
                }
                if (c == '\n') {
                    // End of header found.
                    break;
                }
                builder.append((char) c);
            }
            videoBuffer = new BufferChannel(stream);
            String header = builder.toString();
            String[] headerTokens = header.split("[ ]");
            int w = 0;
            int h = 0;
            String colorSpace = "420";
            for (String tok : headerTokens) {
                char c = tok.charAt(0);
                switch (c) {
                    case 'W':
                        w = Integer.parseInt(tok.substring(1));
                        break;
                    case 'H':
                        h = Integer.parseInt(tok.substring(1));
                        break;
                    case 'C':
                        colorSpace = tok.substring(1);
                        break;
                }
            }
            Logging.d(TAG, "Color space: " + colorSpace);
            if (!colorSpace.equals("420") && !colorSpace.equals("420mpeg2")) {
                throw new IllegalArgumentException(
                        "Does not support any other color space than I420 or I420mpeg2");
            }
            if ((w % 2) == 1 || (h % 2) == 1) {
                throw new IllegalArgumentException("Does not support odd width or height");
            }
            frameWidth = w;
            frameHeight = h;
            Logging.d(TAG, "frame dim: (" + w + ", " + h + ")");
        }

        @Override
        public VideoFrame getNextFrame() {
            final long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            final JavaI420Buffer buffer = JavaI420Buffer.allocate(frameWidth, frameHeight);
            final ByteBuffer dataY = buffer.getDataY();
            final ByteBuffer dataU = buffer.getDataU();
            final ByteBuffer dataV = buffer.getDataV();
            ByteBuffer frameDelimiter = ByteBuffer.allocate(FRAME_DELIMETER_LENGTH);
            videoBuffer.read(frameDelimiter); // If reach end of file, loops automatically
            String frameDelimiterString = new String(frameDelimiter.array(), StandardCharsets.US_ASCII);
            if (!frameDelimiterString.equals(FRAME_DELIMITER)) {
                throw new RuntimeException(
                        "Frames should be delimited by FRAME plus newline, found delimiter was: '"
                                + frameDelimiterString + "'");
            }

            videoBuffer.read(dataY);
            videoBuffer.read(dataU);
            videoBuffer.read(dataV);
            return new VideoFrame(buffer, 0 /* rotation */, captureTimeNs);
        }
    }

    private final static String TAG = "FileVideoCapturer";
    private final VideoReader videoReader;
    private CapturerObserver capturerObserver;
    private final Timer timer = new Timer();

    private final TimerTask tickTask = new TimerTask() {
        @Override
        public void run() {
            tick();
        }
    };

    public LoopbackVideoCapturer(InputStream stream) throws IOException {
        try {
            videoReader = new VideoReaderY4M(stream);
        } catch (IOException e) {
            Logging.d(TAG, "Could not open video stream");
            throw e;
        }
    }

    public void tick() {
        VideoFrame videoFrame = videoReader.getNextFrame();
        capturerObserver.onFrameCaptured(videoFrame);
        videoFrame.release();
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
                           CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }
    @Override
    public void startCapture(int width, int height, int framerate) {
        timer.schedule(tickTask, 0, 1000 / framerate);
    }

    @Override
    public void stopCapture() {
        timer.cancel();
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) { }
    @Override
    public void dispose() { }
    @Override
    public boolean isScreencast() {
        return false;
    }
}