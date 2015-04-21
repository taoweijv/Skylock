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
			case SHOW_UPDATE_DIALOG://��ʾ�����Ի���
				Log.i(TAG, "��ʾ�Ի���");
				showUpdateDialog();
				break;
			case ENTER_HOME://������ҳ��
				enterHome();
				break;
			case URL_ERROR://URL����
				enterHome();
				Toast.makeText(getApplicationContext(), "URL����", 0).show();
				break;
			case NETWORK_ERROR://�����쳣
				enterHome();
				Toast.makeText(getApplicationContext(), "�����쳣", 0).show();
				break;
			case JSON_ERROR://JSON��������
				enterHome();
				Toast.makeText(SplashActivity.this, "JSON��������", 0).show();
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
        tv_splash_version.setText("�汾��"+getVersionName());
        tv_update_info = (TextView) findViewById(R.id.tv_update_info);
        //�������
        checkUpdate();
        //ҳ�潥������
        AlphaAnimation alAn = new AlphaAnimation(0.2f, 1.0f);
        alAn.setDuration(500);
        findViewById(R.id.rl_root_splash).startAnimation(alAn);
    } 
         
    /**
     * ���������Ի���
     */
    protected void showUpdateDialog() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("��ʾ����");
    	builder.setMessage(description);
    	builder.setPositiveButton("��������", new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//����APK, �滻��װ
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){										
					//SDcard����
					Log.i(TAG, "SDcard����");
					//afinal_0.5_bin.jarӦ��
					FinalHttp finalhttp = new FinalHttp();
					//�ϵ�����
					Log.i(TAG, "�����ַ:"+Environment.getExternalStorageDirectory()
					  .getAbsolutePath()+"/skylock.2.0.apk");				
					finalhttp.download(apkurl, Environment.getExternalStorageDirectory()
					  .getAbsolutePath()+"/skylock.2.0.apk", new AjaxCallBack<File>() {
						@Override
						public void onFailure(Throwable t, int errorNo,String strMsg) {
							//��ӡ����ʧ��ʱ��
							t.printStackTrace();
							Toast.makeText(getApplicationContext(), "����ʧ��", 1).show();
							super.onFailure(t, errorNo, strMsg);
							Log.i(TAG,"�������:"+errorNo+"  ������Ϣ:"+strMsg);
						}
						@Override
						public void onLoading(long count, long current) {
							super.onLoading(count, current);
							//��ǰ���ذٷֱ�
							int progress = (int) (current*100/count);							
							tv_update_info.setText("���ؽ���"+progress+"%");
							Log.i(TAG,"���ؽ���"+progress+"%");
						}
						@Override
						public void onSuccess(File t) {
							super.onSuccess(t);
							Log.i(TAG,"���سɹ�");
							installAPK(t);				
						}
						/**
						 * ��װAPK
						 * @param t
						 */
						private void installAPK(File t) {
							Intent intent = new Intent();
							intent.setAction("android.intent.action.VIEW");
							intent.addCategory("android.intent.category.DEFAULT");
							intent.setDataAndType(Uri.fromFile(t), "application/vnd.android.package-archive");
							startActivity(intent);
							Log.i(TAG,"���밲װ����");
						}					
					});
				}else{
					//SDcard������
					Toast.makeText(getApplicationContext(), "SDcard������, �밲װ������", 0).show();
					return;
				}
			}});
    	builder.setNegativeButton("�´���˵", new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				enterHome();
			}});
    	builder.show();
	}

	/**
	 * ����������
	 */
	protected void enterHome() {
		Intent intent = new Intent(this,HomeActivity.class);
		startActivity(intent);
		//�رյ�ǰҳ��*****
		finish();
	}
	
    /**
     * ����Ƿ����°汾,����,������,����,�������ҳ��
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
						//�����ɹ�
						InputStream is = conn.getInputStream();
						//ת����ΪString����
						String result = StreamTools.readFromStream(is);
						Log.i(TAG, "�����ɹ�"+result);
						//JSON����
						JSONObject obj = new JSONObject(result);
						//�������İ汾��
						version = (String) obj.get("version");
						description = (String) obj.get("description");
						apkurl = (String) obj.get("apkurl");
						//Log.i(TAG, "��ַ:"+apkurl);
						//У���°汾
						Log.i(TAG,"��ǰ�汾��:"+getVersionName());
						if(getVersionName().equals(version)){
							//�汾һ��, ���°汾, ������ҳ��
							mes.what = ENTER_HOME;
						}else{
							//���°汾, ���������Ի���
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
     * �õ�Ӧ�ó���İ汾����
     */
	private String getVersionName() {
		//�����ֻ�APK
    	PackageManager pm = getPackageManager();  	
    	try {
    		//���ָ��APK�Ĺ����嵥�ļ�
			PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
			return info.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "";
		}
	}
}
