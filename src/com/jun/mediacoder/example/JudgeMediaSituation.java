package com.jun.mediacoder.example;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import android.net.Uri;
import android.util.Log;
/**
 * @author 作者 :
 * @version 创建时间：2016-6-29 上午10:10:17
 * 类说明:判断视频是否符合压缩要素 
 */
public class JudgeMediaSituation extends LimitBean{
	private Uri uri ;
	private String path;
	private int compressionwidth = 640;
	private int compressionHeight =270;
	private int compressionBitRate = 1000000;
	private int CompressionRotation  = 0;
	public final int STATE_INLEGAL = 1; 
	public final int STATE_NOT_COMTOSEND = 2; 
	public final int STATE_TO_COMPRESS = 3; 
	
	public JudgeMediaSituation( String path) {
		this.uri = Uri.parse(path);
		this.path = path;
	}
	/**
	 ** state:   -1                                未知错误 
	 * @return
	 */
	public int mediaState(){
		try{
			Log.i("1yyg", "getAndroidSDKVersion()==>"+getAndroidSDKVersion());
			if(getAndroidSDKVersion()<MIN_SDKVERSION){//低于4.3
				if(fileSize(path)<=MAX_SIZE){
					return STATE_NOT_COMTOSEND;
				}else return STATE_INLEGAL;
			}else{
				if(judgeMediaDuration()){//小于MAX_DURATION to compress
					if(judgeMediaW_H()||judgeMediaBitRate()){
						CompressionRotation = getRotation();
						Log.i("1yyg", "CompressionRotation===>"+CompressionRotation);
						return STATE_TO_COMPRESS;
					}else return STATE_NOT_COMTOSEND;
				}else return STATE_INLEGAL;
			}
		}catch(Exception ex){
			return STATE_INLEGAL;
		}
	}
	
	/**
	 * 帧率判断
	 */
	private boolean judgeMediaFPS(){
		int duration =MediaHelper.GetDuration(uri);
		int frame = MediaHelper.GetFrameRate(uri);
		int frameInterval=  MediaHelper.GetIFrameInterval(uri);
		int fps = 0;//do some thing
		Log.i("1yyg", "duration==>"+duration+"==frame==>"+frame+"==frameInterval==>"+frameInterval);
		return (fps>=MINVIDEO_FPS)?true:false;
	} 
	
	
	
	/**
	 * 比特率判断
	 */
	private boolean judgeMediaBitRate(){
		int mediaBitRate = MediaHelper.GetBitRate(uri);
		Log.i("1yyg", "mediaBitRate==>"+mediaBitRate);
		if(mediaBitRate>MINVIDEO_BITRATE){
			compressionBitRate = MINVIDEO_BITRATE;
			return  true ;
		}else{
			compressionBitRate = mediaBitRate;
			return  false ;
		}
	}
	/**
	 * 获取视频旋转角度
	 * @return
	 */
	private int getRotation(){
		return MediaHelper.GetRotation(uri);
	}
	
	/**
	 * 时间是否符合
	 */
	private boolean judgeMediaDuration(){
		if(MediaHelper.GetDuration(uri)<MAX_DURATION){
			return true;
		}else return false;
	}
	
	/**
	 * 判断分辨率大小 并得到相应的分辨率 
	 */
	private boolean  judgeMediaW_H(){
		int width  =  MediaHelper.GetWidth(uri);
		int height =  MediaHelper.GetHeight(uri);
		int compare = (width>=height)?width:height; 
		boolean WLargeH =(width>=height)?true:false; 
		
		if(compare>MAXSCREEN){
			if(WLargeH){
				compressionwidth = MAXSCREEN;
				compressionHeight = calculateScreen(width, height);
			}else{
				compressionwidth = calculateScreen(height,width );
				compressionHeight =  MAXSCREEN;
			}
			return true;
		}else{
			compressionwidth = width;
			compressionHeight = height;
			return false;
		}
		
	}
	private int calculateScreen(int large,int small){
		return  (MAXSCREEN*small)/large;
	}
	
	/**
	 * 判断文件大小
	 * @param strFile
	 * @return
	 */
	private int fileSize(String strFile) {
		try {
			File f = new File(strFile);
			if (!f.exists()) {
				return -1;
			}
			int size = (int) (f.length()/1024) ;
			//Log.i("1yyg", "文件大小为==>"+size) ;
			return size;
		} catch (Exception e) {
			return -1;
		}
	}
	/**
	 * 判断android 版本
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private static int getAndroidSDKVersion() { 
		return  Integer.valueOf(android.os.Build.VERSION.SDK); 
    }

	public int getCompressionwidth() {
		return compressionwidth;
	}
	public int getCompressionHeight() {
		return compressionHeight;
	}
	public int getCompressionBitRate() {
		return compressionBitRate;
	}
	public int getCompressionRotation() {
		return CompressionRotation;
	}
}
