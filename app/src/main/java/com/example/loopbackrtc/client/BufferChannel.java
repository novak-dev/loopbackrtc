package com.example.loopbackrtc.client;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.webrtc.Logging;

public class BufferChannel {
    private static final String TAG = "BufferChannel";

    private final byte[] buffer;
    private int position = 0;

    public BufferChannel(InputStream stream) throws IOException {
        this.buffer = IOUtils.toByteArray(stream);
    }

    public long read(ByteBuffer dst) {
        int remaining = dst.remaining();
        int finalPosition = remaining + position - 1;
        if (finalPosition > buffer.length) {
            finalPosition = buffer.length;
        }
        int bytesRead = finalPosition - position;

        for (; position <= finalPosition; position++) {
            dst.put(buffer[position]);
        }

        if (position == buffer.length) {
            position = 0;
        }
        return bytesRead;
    }

}
