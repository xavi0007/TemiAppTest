/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
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

package net.majorkernelpanic.streaming.rtp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;


/**
 * An InputStream that uses data from a MediaCodec.
 * The purpose of this class is to interface existing RTP packetizers of
 * libstreaming with the new MediaCodec API. This class is not thread safe !
 */
@SuppressLint("NewApi")
public class MediaCodecInputStream extends InputStream {

	private static final String FILE_PATH = Environment.getExternalStorageDirectory() + "/movie";
	public final String TAG = "MediaCodecInputStream";

	private MediaCodec mMediaCodec = null;
	private BufferInfo mBufferInfo = new BufferInfo();
	private ByteBuffer[] mBuffers = null;
	private ByteBuffer mBuffer = null;
	private int mIndex = -1;
	private boolean mClosed = false;
	private int index =0;
	private FileOutputStream saveFile;

	public MediaFormat mMediaFormat;
	public MediaMuxer mMuxer;
	public int mVideotrack;

	//private long startTime;

	public MediaCodecInputStream(MediaCodec mediaCodec) {
		mMediaCodec = mediaCodec;
		mBuffers = mMediaCodec.getOutputBuffers();
//		mMuxer = MediaStream.mMuxer;
		//initialize muxer
		try {
			mMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getAbsolutePath() + "/movie" + Calendar.getInstance().getTime().getTime() + ".mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		}catch (Exception e){
			e.printStackTrace();
		}
		/*// try to save as raw h264 stream
		try {
			saveFile = new FileOutputStream(FILE_PATH, true);
		}catch (Exception e){
			e.printStackTrace();
		}*/
	}

	@Override
	public void close() {
		mClosed = true;
		if (mMuxer!=null && mBuffer != null){
			//mBufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
			//mMediaCodec.signalEndOfInputStream(); // only for surface
			boolean thing = (mBuffer == null);
			//Log.i(TAG, "mBuffer is null?: " + thing);
			//mMuxer.writeSampleData(mVideotrack,mBuffer,mBufferInfo);

			//set moov atom here (according to examples it is not needed

			/*mMuxer.stop();
			mMuxer.release();*/
		}
	}

	@Override
	public int read() throws IOException {
		return 0;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int min = 0;

		try {
			if (mBuffer==null) {
				while (!Thread.interrupted() && !mClosed) {
					mIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 500000);
					if (mIndex>=0 ){
						//Log.d(TAG,"Index: "+mIndex+" Time: "+mBufferInfo.presentationTimeUs+" size: "+mBufferInfo.size);
						mBuffer = mBuffers[mIndex];
						mBuffer.position(0);
						//mMuxer.writeSampleData(mVideotrack, mBuffer, mBufferInfo);
						break;
					} else if (mIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						mBuffers = mMediaCodec.getOutputBuffers();
					} else if (mIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						Log.i(TAG, "This should only run once");
						mMediaFormat = mMediaCodec.getOutputFormat();
						Log.i(TAG,mMediaFormat.toString());
						//mVideotrack = mMuxer.addTrack(mMediaFormat);
						//mMuxer.start();
					} else if (mIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
						Log.v(TAG,"No buffer available...");
						mClosed = true;
//						MainActivity.setonPreviewFrameWithMediaCodec = true;
						//mMuxer.stop();
						//mMuxer.release();
						//return 0;
					} else {
						Log.e(TAG,"Message: "+mIndex);

						if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
							// The codec config data was pulled out and fed to the muxer when we got
							// the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
							Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
							mBufferInfo.size = 0;
						}
						//return 0;
					}
				}
				/*while (!Thread.interrupted() && !mClosed) {
					drainEncoder(false);
				}
				drainEncoder(true);*/


			}

			if (mClosed) throw new IOException("This InputStream was closed");

			//mMuxer.addTrack(mMediaFormat);
			min = length < mBufferInfo.size - mBuffer.position() ? length : mBufferInfo.size - mBuffer.position();
			mBuffer.get(buffer, offset, min);

			//saveFile.write(buffer);
			if (mBuffer.position()>=mBufferInfo.size) {
				mMediaCodec.releaseOutputBuffer(mIndex, false);
				mBuffer = null;
			}
			/*if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
				Log.i(TAG, "Stopping mediaCodec....");

			}*/

		} catch (RuntimeException e) {
			e.printStackTrace();
		}

		return min;
	}

	public int available() {
		if (mBuffer != null)
			return mBufferInfo.size - mBuffer.position();
		else
			return 0;
	}

	public BufferInfo getLastBufferInfo() {
		return mBufferInfo;
	}

	private boolean muxerStarted = false;

	/*private void drainEncoder(boolean endOfStream) {
		final int TIMEOUT_USEC = 10000;
		//Log.d(TAG, "drainEncoder(" + endOfStream + ")");

		if (endOfStream) {
			Log.d(TAG, "sending EOS to encoder");
			mMediaCodec.signalEndOfInputStream();
		}

		mBuffers = mMediaCodec.getOutputBuffers();
		while (true) {
			int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
			if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				if (!endOfStream) {
					break;      // out of while
				} else {
					Log.d(TAG, "no output available, spinning to await EOS");
				}
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				mBuffers = mMediaCodec.getOutputBuffers();
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// should happen before receiving buffers, and should only happen once
				if (muxerStarted) {
					throw new RuntimeException("format changed twice");
				}
				//MediaFormat newFormat = mMediaCodec.getOutputFormat();
				//Log.d(TAG, "encoder output format changed: " + newFormat);

				// now that we have the Magic Goodies, start the muxer
				//mVideotrack = mMuxer.addTrack(newFormat);
				mMuxer.start();
				muxerStarted = true;
			} else if (encoderStatus < 0) {
				Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
						encoderStatus);
				// let's ignore it
			} else {
				mBuffer = mBuffers[encoderStatus];
				if (mBuffer == null) {
					throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
							" was null");
				}

				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					// The codec config data was pulled out and fed to the muxer when we got
					// the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
					Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
					mBufferInfo.size = 0;
				}

				if (mBufferInfo.size != 0) {
					if (!muxerStarted) {
						throw new RuntimeException("muxer hasn't started");
					}

					// adjust the ByteBuffer values to match BufferInfo (not needed?)
					mBuffer.position(mBufferInfo.offset);
					mBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

					mMuxer.writeSampleData(mVideotrack, mBuffer, mBufferInfo);
					Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
				}

				mMediaCodec.releaseOutputBuffer(encoderStatus, false);

				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					if (!endOfStream) {
						Log.w(TAG, "reached end of stream unexpectedly");
					} else {
						Log.d(TAG, "end of stream reached");
					}
					break;      // out of while
				}
			}
		}
	}*/

}
