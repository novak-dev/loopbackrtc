package com.example.loopbackrtc.model;

import org.apache.commons.io.IOUtils;
import org.webrtc.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class CircularBuffer {

    private static final String TAG = "CircularBuffer";
    private final byte[] buffer;
    private int position = 0;

    public CircularBuffer(InputStream stream) throws IOException {
        this.buffer = IOUtils.toByteArray(stream);
    }

    public void read(ByteBuffer dst) {
        int remaining = dst.remaining();
        int finalPosition = remaining + position;
        if (finalPosition >= buffer.length) {
            finalPosition = buffer.length - 1;
        }

        for (; position < finalPosition; position++) {
            dst.put(buffer[position]);
        }

        if (position == buffer.length - 1) {
            position = 0;
        }

    }

}
