/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.devhyper.openvideoeditor.videoeditor;

import android.media.MediaCodec.BufferInfo;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Metadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.muxer.Muxer;

import com.google.common.collect.ImmutableList;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;

@UnstableApi
public final class CustomMuxer implements Muxer {

    /** A {@link Muxer.Factory} for {@link CustomMuxer}. */
    public static final class Factory implements Muxer.Factory {
        private final Muxer.Factory muxerFactory;

        /** Creates an instance with {@code videoDurationMs} set to {@link C#TIME_UNSET}. */
        public Factory() {
            this(null, /* videoDurationMs= */ C.TIME_UNSET);
        }

        public Factory(FileDescriptor fd) {
            this(fd, /* videoDurationMs= */ C.TIME_UNSET);
        }

        /**
         * Creates an instance.
         *
         * @param videoDurationMs The duration of the video track (in milliseconds) to enforce in the
         *     output, or {@link C#TIME_UNSET} to not enforce. Only applicable when a video track is
         *     {@linkplain #addTrack(Format) added}.
         */
        public Factory(FileDescriptor fd, long videoDurationMs) {
            this.muxerFactory = new CustomFrameworkMuxer.Factory(fd, videoDurationMs);
        }

        @Override
        public Muxer create(String path) throws MuxerException {
            return new CustomMuxer(muxerFactory.create(path));
        }

        @Override
        public ImmutableList<String> getSupportedSampleMimeTypes(@C.TrackType int trackType) {
            return muxerFactory.getSupportedSampleMimeTypes(trackType);
        }
    }

    private final Muxer muxer;

    private CustomMuxer(Muxer muxer) {
        this.muxer = muxer;
    }

    @Override
    public TrackToken addTrack(Format format) throws MuxerException {
        return muxer.addTrack(format);
    }

    @Override
    public void writeSampleData(TrackToken trackToken, ByteBuffer byteBuffer, BufferInfo bufferInfo)
            throws MuxerException {
        muxer.writeSampleData(trackToken, byteBuffer, bufferInfo);
    }

    @Override
    public void addMetadataEntry(Metadata.Entry metadataEntry) {
        muxer.addMetadataEntry(metadataEntry);
    }

    @Override
    public void close() throws MuxerException {
        muxer.close();
    }
}
