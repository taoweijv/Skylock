package com.tao.skylock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.tao.skylock.utils.StreamTools;

public class SplashActivity extends Activity {
	protected static final String TAG = "SplashActivity";
	protected static final int SHOW_UPDATE_DIALOG = 0;
	protected static final int ENTER_HOME = 1;	
	protected static final int URL_ERROR = 2;
	protected static final int NETWORK_ERROR = 3;
	protected static final int JSON_ERROR = 4;
	private TextView tv_splash_version;
	private TextView tv_update_info;
	private String version;
	private String description;
	private String apkurl;	
	private Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case SHOW_UPDATE_DIALOG://显示升级对话框
				Log.i(TAG, "显示对话框");
				showUpdateDialog();
				break;
			case ENTER_HOME://进入主页面
				enterHome();
				break;
			case URL_ERROR://URL错误
				enterHome();
				Toast.makeText(getApplicationContext(), "URL错误", 0).show();
				break;
			case NETWORK_ERROR://网络异常
				enterHome();
				Toast.makeText(getApplicationContext(), "网络异常", 0).show();
				break;
			case JSON_ERROR://JSON解析错误
				enterHome();
				Toast.makeText(SplashActivity.this, "JSON解析错误", 0).show();
				break;
			default:
				break;
			}
		}  	 
     };
     
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        tv_splash_version = (TextView) findViewById(R.id.tv_splash_version);
        tv_splash_version.setText("版本号"+getVersionName());
        tv_update_info = (TextView) findViewById(R.id.tv_update_info);
        //检查升级
        checkUpdate();
        //页面渐变设置
        AlphaAnimation alAn = new AlphaAnimation(0.2f, 1.0f);
        alAn.setDuration(500);
        findViewById(R.id.rl_root_splash).startAnimation(alAn);
    } 
         
    /**
     * 弹出升级对话框
     */
    protected void showUpdateDialog() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("提示升级");
    	builder.setMessage(description);
    	builder.setPositiveButton("立即升级", new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//下载APK, 替换安装
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){										
					//SDcard存在
					Log.i(TAG, "SDcard存在");
					//afinal_0.5_bin.jar应用
					FinalHttp finalhttp = new FinalHttp();
					//断点下载
					Log.i(TAG, "储存地址:"+Environment.getExternalStorageDirectory()
					  .getAbsolutePath()+"/skylock.2.0.apk");				
					finalhttp.download(apkurl, Environment.getExternalStorageDirectory()
					  .getAbsolutePath()+"/skylock.2.0.apk", new AjaxCallBack<File>() {
						@Override
						public void onFailure(Throwable t, int errorNo,String strMsg) {
							//打印下载失败时间
							t.printStackTrace();
							Toast.makeText(getApplicationContext(), "下载失败", 1).show();
							super.onFailure(t, errorNo, strMsg);
							Log.i(TAG,"错误代码:"+errorNo+"  错误信息:"+strMsg);
						}
						@Override
						public void onLoading(long count, long current) {
							super.onLoading(count, current);
							//当前下载百分比
							int progress = (int) (current*100/count);							
							tv_update_info.setText("下载进度"+progress+"%");
							Log.i(TAG,"下载进度"+progress+"%");
						}
						@Override
						public void onSuccess(File t) {
							super.onSuccess(t);
							Log.i(TAG,"下载成功");
							installAPK(t);				
						}
						/**
						 * 安装APK
						 * @param t
						 */
						private void installAPK(File t) {
							Intent intent = new Intent();
							intent.setAction("android.intent.action.VIEW");
							intent.addCategory("android.intent.category.DEFAULT");
							intent.setDataAndType(Uri.fromFile(t), "application/vnd.android.package-archive");
							startActivity(intent);
							Log.i(TAG,"进入安装程序");
						}					
					});
				}else{
					//SDcard不存在
					Toast.makeText(getApplicationContext(), "SDcard不存在, 请安装后重试", 0).show();
					return;
				}
			}});
    	builder.setNegativeButton("下次再说", new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				enterHome();
			}});
    	builder.show();
	}

	/**
	 * 进入主界面
	 */
	protected void enterHome() {
		Intent intent = new Intent(this,HomeActivity.class);
		startActivity(intent);
		//关闭当前页面*****
		finish();
	}
	
    /**
     * 检查是否有新版本,若有,则升级,若无,则进入主页面
     */
    private void checkUpdate() {
    	new Thread(){
    		public void run(){
    			Message mes = Message.obtain();
    			long startTime = System.currentTimeMillis();
    			try {
    				URL url = new URL(getString(R.string.serverurl));
					HttpURLConnection conn= (HttpURLConnection) url.openConnection();
					conn.setRequestMethod("GET");
					conn.setReadTimeout(5000);
					int code = conn.getResponseCode();
					if(code == 200){
						//联网成功
						InputStream is = conn.getInputStream();
						//转换流为String类型
						String result = StreamTools.readFromStream(is);
						Log.i(TAG, "联网成功"+result);
						//JSON解析
						JSONObject obj = new JSONObject(result);
						//服务器的版本号
						version = (String) obj.get("version");
						description = (String) obj.get("description");
						apkurl = (String) obj.get("apkurl");
						//Log.i(TAG, "网址:"+apkurl);
						//校验新版本
						Log.i(TAG,"当前版本号:"+getVersionName());
						if(getVersionName().equals(version)){
							//版本一致, 无新版本, 进入主页面
							mes.what = ENTER_HOME;
						}else{
							//有新版本, 弹出升级对话框
							mes.what = SHOW_UPDATE_DIALOG;
						}				
					}
    			} catch (MalformedURLException e) {
    				mes.what = URL_ERROR;
					e.printStackTrace();
				} catch (IOException e) {
					mes.what = NETWORK_ERROR;
					e.printStackTrace();
				} catch (JSONException e) {
					mes.what = JSON_ERROR;
					e.printStackTrace();
				}finally{
					long endTime = System.currentTimeMillis();
					long dTime = startTime - endTime;
					if(dTime < 2000){
						try {
							Thread.sleep(2000-dTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					handler.sendMessage(mes);
				}
    		};
    	}.start();
	}
    
	/**
     * 得到应用程序的版本名称
     */
	private String getVersionName() {
		//管理手机APK
    	PackageManager pm = getPackageManager();  	
    	try {
    		//获得指定APK的功能清单文件
			PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
			return info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "";
		}
	}
}
