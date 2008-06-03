/*
 * Copyright 2003-2008 Stuart Clayman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



// WriteRequest.java


package com.timeindexing.io;

import com.timeindexing.util.ByteBufferRing;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A class that represents a write request in the I/O thread.
 * It holds data on the channel being written to, the buffer to
 * write, and the ByteBufferRing the buffer came from.
 */
class WriteRequest {
    FileChannel channel = null;
    ByteBuffer buffer = null;
    ByteBufferRing ring = null;

    /**
     * Construct a WriteRequest.
     */
    public WriteRequest(FileChannel fc, ByteBuffer bb, ByteBufferRing r) {
	channel = fc;
	buffer = bb;
	ring = r;
    }
}

