package com.jun.mediacoder.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
@SuppressLint("NewApi") 
@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
public class MediaCompression {
	private String TAG = "1yyg";
	private String inPath;
	private String changePath="/mnt/sdcard/videokit/shu_change01.mp4";
	private String changePath2="/mnt/sdcard/videokit/shu_change02.mp4";
	private String outPath;
	
	public MediaCompression(String inPath,String outPath){
		this.inPath = inPath ;
		this.outPath = outPath ;
		
	 }
	public void MediaCompressionExecute(MediaCompressionListener mediacompressionListener){
		ExecuteRunnable executeRunnable = new ExecuteRunnable(mediacompressionListener);
	    Thread thread =  new Thread(executeRunnable);
	    thread.start();
	}
	class ExecuteRunnable implements Runnable{
		/*********解析vidoe 所用********/
		private MediaExtractor mediaExtractor ;
		private InputSurface mInputSurface;
		private OutputSurface mOutputSurface;
		private MediaCodec enCoder;
		private MediaCodec deCoder ;
		/*********提取audio 所用********/
		private MediaExtractor audioExtractor ; 
		private MediaFormat outputAudioFormat;
		private  final int TIMEOUT_USEC = 10000;
		
		private int outputVideoTrack = -1;
	    private int outputAudioTrack = -1;
	    private int compressionWidth = 640;
		private int compressionHeight =270;
		private int compressionBitRate = 1000000;
		
		private MediaCompressionListener mediacompressionListener;
		private JudgeMediaSituation judgeSituation;
		/***合成输出文件***/
		private MediaMuxer mMuxer = null;
        public ExecuteRunnable(MediaCompressionListener mediacompressionListener){
        	this.mediacompressionListener = mediacompressionListener;
        }
		@Override
		public void run() {
			dealWithCompressionEvent();
		}
		/**
		 * for compression
		 */
		 @SuppressLint("NewApi") 
		 private void dealWithCompressionEvent(){
			boolean isRunFinsh = false;
			try {
				
				/******* judge is legal **********/
				judgeSituation = new JudgeMediaSituation(inPath);
				int situation = judgeSituation.mediaState();
				if(situation==judgeSituation.STATE_NOT_COMTOSEND){
					mediacompressionListener.onExecload(true);
					if(copyFile(new File(inPath), new File(outPath), true)){
						mediacompressionListener.onExecFinish(outPath);
					}else{
						mediacompressionListener.onExecFail("compress fail!");
					}
				}else if(situation==judgeSituation.STATE_TO_COMPRESS){
					mediacompressionListener.onExecload(true);
				}else{
					mediacompressionListener.onExecload(false);
				}
				if (situation!=judgeSituation.STATE_TO_COMPRESS)return;
				Log.i("1yyg", "getFileParet===>"+MediaHelper.getFileParent(outPath));
				String outPathParent = MediaHelper.getFileParent(outPath);
				changePath = outPathParent+File.separator+"1yyg_changePath01.mp4";
				changePath2 = outPathParent+File.separator+"1yyg_changePath02.mp4";
				MediaHelper.RotateVideo(inPath, changePath, -judgeSituation.getCompressionRotation()); 
				
				compressionWidth = judgeSituation.getCompressionwidth();
				compressionHeight = judgeSituation.getCompressionHeight();
				compressionBitRate = judgeSituation.getCompressionBitRate();
				/********* init objects**************/
				mediacompressionListener.onExecStart("compress starting!");
				initCompressionObjects();
				/************************** video cpmression ***************************/
				executeVideoEvent();
				/************************** audio cpmression ***************************/
				executeAudioEvent(outputAudioTrack);
				isRunFinsh = true;
			}catch(Exception e){
				//if(!isRunFinsh)mediacompressionListener.onExecFail("compress fail!");
				 e.printStackTrace();
			 }finally{
				 release();
				 MediaHelper.RotateVideo(changePath2, outPath, judgeSituation.getCompressionRotation()); 
				 deleteFile(changePath);
				 deleteFile(changePath2);
				 if(!isRunFinsh){
					 mediacompressionListener.onExecFail("compress fail!");
				 }else{
					 mediacompressionListener.onExecFinish(outPath);
				 }
				 
				 
			 }
			
	     }
		 /**
		  * 执行video压缩事件
		  */
		private void executeVideoEvent() {
			ByteBuffer[] videoDecoderInputBuffers = deCoder.getInputBuffers();
			ByteBuffer[] videoDecoderOutputBuffers = deCoder.getOutputBuffers();
			ByteBuffer[] videoEncoderOutputBuffers= enCoder.getOutputBuffers();
			
			MediaCodec.BufferInfo videoDecoderOutputBufferInfo = new MediaCodec.BufferInfo();;
			MediaCodec.BufferInfo videoEncoderOutputBufferInfo = new MediaCodec.BufferInfo();;
			MediaFormat encoderOutputVideoFormat = null;
			boolean beginCodec = true ;
			boolean videoExtractorDone = false;
			boolean videoDecoderDone = false;
			boolean videoEncoderDone = false;
			boolean startMux  = false ;
			while(beginCodec&& !videoEncoderDone&&!videoDecoderDone){
				//Log.i(TAG, "begin ==video Extractor===========") ;
				while(!videoExtractorDone){
					//Log.i(TAG, "begin ==video Extractor===========ing") ;e
			        int decoderInputBufferIndex = deCoder.dequeueInputBuffer(TIMEOUT_USEC);
			        if (decoderInputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
			            break;
			        }
			        ByteBuffer decoderInputBuffer = videoDecoderInputBuffers[decoderInputBufferIndex];
			        int size = mediaExtractor.readSampleData(decoderInputBuffer, 0);
			        long presentationTime = mediaExtractor.getSampleTime();
			        if (size >= 0) {
			        	deCoder.queueInputBuffer(decoderInputBufferIndex,0,size,
			                    presentationTime, mediaExtractor.getSampleFlags());
			        }
			        videoExtractorDone = !mediaExtractor.advance();
			        if (videoExtractorDone) {
			        	deCoder.queueInputBuffer(decoderInputBufferIndex,0,0,0,
			                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
			        }
			        break;
				}
				//Log.i(TAG, "begin ==video decoder===========") ;
				while(!videoDecoderDone){
					//Log.i(TAG, "begin ==video decoder===========ing") ;
					int decoderOutputBufferIndex =deCoder.dequeueOutputBuffer(videoDecoderOutputBufferInfo, TIMEOUT_USEC);
			        if (decoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
			            break;
			        }
			        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			            videoDecoderOutputBuffers = deCoder.getOutputBuffers();
			            break;
			        }
			        if (decoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			            break;
			        }
			        ByteBuffer decoderOutputBuffer =videoDecoderOutputBuffers[decoderOutputBufferIndex];
			        if ((videoDecoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!= 0) {
			        	//Log.i(TAG, "begin ==video      视频解码  release===>");
			        	deCoder.releaseOutputBuffer(decoderOutputBufferIndex, false);
			            break;
			        }
			        boolean render = videoDecoderOutputBufferInfo.size != 0; 
			        deCoder.releaseOutputBuffer(decoderOutputBufferIndex, render);
			        if (render) {
			        	mOutputSurface.awaitNewImage();
			        	mOutputSurface.drawImage();
			            mInputSurface.setPresentationTime(videoDecoderOutputBufferInfo.presentationTimeUs * 1000);
			            mInputSurface.swapBuffers();
			        }
			        if ((videoDecoderOutputBufferInfo.flags& MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			            videoDecoderDone = true;
			            videoDecoderOutputBufferInfo.size=0;
			        	//Log.i(TAG, "begin ==video      视频解码已结束===>");
			            try {
			        	  deCoder.signalEndOfInputStream();
						} catch (Exception e) {
							Log.i(TAG, "begin ==video      视频解码已结束===>error");
						}
			        }
			        break;
				}
				while(!videoEncoderDone){
					//Log.i(TAG, "begin ==video encoder===========ing") ;
					int encoderOutputBufferIndex = enCoder.dequeueOutputBuffer(videoEncoderOutputBufferInfo, TIMEOUT_USEC);
			        if (encoderOutputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
			            break;
			        }
			        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			            videoEncoderOutputBuffers = enCoder.getOutputBuffers();
			            break;
			        }
			        if (encoderOutputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			            if (outputVideoTrack >= 0) {
			            }
			            encoderOutputVideoFormat = enCoder.getOutputFormat();
			            break;
			        }
			        
			        ByteBuffer encoderOutputBuffer =  videoEncoderOutputBuffers[encoderOutputBufferIndex];
			        if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!= 0) {
			        	enCoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
			            break;
			        }
			        if (videoEncoderOutputBufferInfo.size != 0) {
			        	mMuxer.writeSampleData(outputVideoTrack, encoderOutputBuffer, videoEncoderOutputBufferInfo);
			        }
			        if ((videoEncoderOutputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM)!= 0) {
			        	videoEncoderOutputBufferInfo.size = 0;
			        	Log.i(TAG, "begin ==video  视频编码已结束===>");
			        	enCoder.signalEndOfInputStream();
			            videoEncoderDone = true;
			        }
			        enCoder.releaseOutputBuffer(encoderOutputBufferIndex, false);
			        break;
				}
			    if(!startMux&&encoderOutputVideoFormat!=null){
			    	outputVideoTrack = mMuxer.addTrack(encoderOutputVideoFormat);
					outputAudioTrack = mMuxer.addTrack(outputAudioFormat);
			    	mMuxer.start();
			    	startMux= true;
			    }
			}
		}
		/**
		  * 执行audio事件
		  */
		private void executeAudioEvent(int outputAudioTrack) {
			MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
			info.presentationTimeUs = 0;
			ByteBuffer buffer = ByteBuffer.allocate(500 * 1024 * 8 * 8);
			while (true) {
				int sampleSize = audioExtractor.readSampleData(buffer,0);
				if (sampleSize <= 0) {
					break;
				}
				if (audioExtractor.advance()) {
					if (audioExtractor.getSampleTime() < 0)break;
					info.offset = 0;
					info.size = sampleSize;
					info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME; // 1000*1000/15
					info.presentationTimeUs = audioExtractor.getSampleTime();
					//Log.i("1ygg", "audio num==>"+num);
					mMuxer.writeSampleData(outputAudioTrack, buffer,info);
				} else 
					break;
			}
			
		}

		/**
		 * 参数定义    
		 */
		private void initCompressionObjects() {
			mediaExtractor= createExtractor(changePath) ;
			int videoInputTrack = getAndSelectVideoTrackIndex(mediaExtractor);
			MediaFormat videoInputFormat = mediaExtractor.getTrackFormat(videoInputTrack);
			String videoMimeType =videoInputFormat.getString(MediaFormat.KEY_MIME); 
			MediaFormat outputVideoFormat =getVideoOutPutFormat(videoMimeType) ;
			
			enCoder= MediaCodec.createEncoderByType(videoMimeType);
			enCoder.configure( outputVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE );
			mInputSurface = new InputSurface(enCoder.createInputSurface()) ;
			mInputSurface.makeCurrent() ;
			enCoder.start();	
			
			deCoder = MediaCodec.createDecoderByType(videoMimeType) ;
			mOutputSurface = new OutputSurface();
			deCoder.configure(videoInputFormat, mOutputSurface.getSurface(), null, 0) ;
			deCoder.start();
			
			
			audioExtractor = createExtractor(changePath) ;
			int audioImputTrack = getAndSelectAudioTrackIndex(audioExtractor);
			outputAudioFormat = audioExtractor.getTrackFormat(audioImputTrack);
			
			setupMuxer() ;
			
			//Log.i(TAG, "init object finish=============") ;
		}	
		 
		private void release() {
			Log.i(TAG, "release======>object");
			try {
				if (mediaExtractor != null) {
					mediaExtractor.release();
				}
				if (audioExtractor != null) {
					audioExtractor.release();
				}
				if (deCoder != null) {
					deCoder.stop();
					deCoder.release();
				}
				if (enCoder != null) {
					enCoder.stop();
					enCoder.release();
				}
				if (mMuxer != null) {
					
					mMuxer.stop();
					mMuxer.release();
				}
				if (mOutputSurface != null) {
					mOutputSurface.release();
				}
				if (mInputSurface != null) {
					mInputSurface.release();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		 /**
		  * 定义合并器
		  */
		 private void setupMuxer() {
		      try {
		         mMuxer = new MediaMuxer( changePath2, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 );
		      } catch ( IOException ioe ) {
		         throw new RuntimeException( "MediaMuxer creation failed", ioe );
		      }
		   }
		 
		 private MediaFormat  getVideoOutPutFormat(String mime){
			MediaFormat outputVideoFormat = MediaFormat.createVideoFormat(mime, compressionWidth, compressionHeight);
			outputVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE,	compressionBitRate);
			outputVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,judgeSituation.MINVIDEO_FPS);
			outputVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface );
			outputVideoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
			return outputVideoFormat ;
		 }
		 /**
		  * 媒体文件提取器
		  * @param path
		  * @return
		  */
	    private MediaExtractor createExtractor(String path)  {
	        MediaExtractor extractor = null;
	        try {
	        	extractor = new MediaExtractor();
				extractor.setDataSource(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        return extractor;
	    }
	    
	    /**
	     * 返回 属于 audio format的index
	     * @param extractor
	     * @return
	     */
	   
	    private int getAndSelectAudioTrackIndex(MediaExtractor extractor) {
	        for (int index = 0; index < extractor.getTrackCount(); ++index) {
	            if (isAudioFormat(extractor.getTrackFormat(index))) {
	                extractor.selectTrack(index);
	                return index;
	            }
	        }
	        return -1;
	    }
	    /**
	     * 返回 属于 video format的index
	     * @param extractor
	     * @return
	     */
	    private int getAndSelectVideoTrackIndex(MediaExtractor extractor) {
	        for (int index = 0; index < extractor.getTrackCount(); ++index) {
	            if (isVideoFormat(extractor.getTrackFormat(index))) {
	                extractor.selectTrack(index);
	                return index;
	            }
	        }
	        return -1;
	    }
	}
 
	private static boolean isVideoFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("video/");
    }
    
    private static boolean isAudioFormat(MediaFormat format) {
        return getMimeTypeFor(format).startsWith("audio/");
    }

    private static String getMimeTypeFor(MediaFormat format) {
        return format.getString(MediaFormat.KEY_MIME);
    }
    private void deleteFile(String path){
    	try {
    		File f  = new File(path);
    		if(f.exists()){
    			f.delete();
    		}
		} catch (Exception e) {
			
		}
    }
    /**
	 * 文件的复制操作方法
	 * 
	 * @param fromFile 被复制的文件
	 * @param toFile复制的目录文件
	 * @param rewrite是否重新创建文件
	 * 
	 */
	private static boolean copyFile(File fromFile, File toFile, Boolean rewrite) {
		boolean flag = false;
		if (!fromFile.exists()) {
			return flag;
		}

		if (!fromFile.isFile()) {
			return flag;
		}
		if (!fromFile.canRead()) {
			return flag;
		}
		if (!toFile.getParentFile().exists()) {
			toFile.getParentFile().mkdirs();
		}
		if (toFile.exists() && rewrite) {
			toFile.delete();
		}

		try {
			FileInputStream fosfrom = new FileInputStream(fromFile);
			FileOutputStream fosto = new FileOutputStream(toFile);

			byte[] bt = new byte[1024];
			int c;
			while ((c = fosfrom.read(bt)) > 0) {
				fosto.write(bt, 0, c);
			}
			// 关闭输入、输出流
			fosfrom.close();
			fosto.close();
			flag = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return flag;
	}

}
