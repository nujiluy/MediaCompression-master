package com.jun.mediacoder.example;

/**
 * @author 作者 :
 * @version 创建时间：2016-6-29 上午10:47:26
 * 类说明: 视频压缩 条件集合
 */
public class LimitBean {
      
	public final int MIN_SDKVERSION = 18;
	public final int MAXSCREEN = 640;
	public final int MAX_DURATION = 1000*60*3;//ms
	public final int MAX_SIZE = 30*1024;//KB
	public final int MINVIDEO_BITRATE = 1000000;
	public final int MINVIDEO_FPS = 25;
    public LimitBean(){} 
	
	
}
