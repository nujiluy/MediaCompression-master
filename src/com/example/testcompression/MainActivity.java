package com.example.testcompression;

import com.jun.mediacoder.example.MediaCompression;
import com.jun.mediacoder.example.MediaCompressionListener;
import com.jun.mediacoder.example.MediaHelper;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener{

	
	private Button myButton ,toRatation;
	private final String inPath ="/mnt/sdcard/videokit/heng_in02.mp4" ;
	private final String outPath = "/mnt/sdcard/videokit/heng_out02.mp4";
	private final String outPath_rota = "/mnt/sdcard/videokit/shu_out01_rotation.mp4";
	private final String TAG = "1yyg";
	private MediaCompression mediaCompression;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myButton =  (Button) findViewById(R.id.compress);
        toRatation=  (Button) findViewById(R.id.rotation);
        toRatation.setOnClickListener(this);
        
        myButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mediaCompression = new MediaCompression(inPath, outPath);
				mediaCompression.MediaCompressionExecute(new MediaCompressionListener() {
					@Override
					public void onExecload(boolean state) {
						Log.i(TAG, "onExecload==>"+state);
					}
					@Override
					public void onExecStart(String message) {
						Log.i(TAG, "onExecStart==>"+message);
					}
					@Override
					public void onExecFinish(String message) {
						Log.i(TAG, "onExecFinish==>"+message);
					}
					@Override
					public void onExecFail(String reason) {
						Log.i(TAG, "onExecFail==>"+reason);
					}
				});
			}
		});
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.rotation:
			MediaHelper.RotateVideo(outPath, outPath_rota, 90);
			break;
		default:
			break;
		}
	}
    
}
