package com.jun.mediacoder.example;

/**
 * @author 作者 :
 * @version 创建时间：2016-6-29 上午9:59:08
 * 类说明:
 */
public interface MediaCompressionListener {
	/**
	 ** true is cancompress  or cann't 
	 * @return
	 */
     public void onExecload(boolean state);
     /**
      * 开始压缩
      * @param message
      */
     public void onExecStart(String message);
     /**
      * 压缩失败
      * @param message
      */
     public void onExecFail(String message);
     /**
      * 压缩完成
      * @param message
      */
     public void onExecFinish(String message);
}
