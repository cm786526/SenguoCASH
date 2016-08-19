/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package cc.senguo.senguocashier;

import hdx.HdxUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.KeyCharacterMap.UnavailableException;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class ConsoleActivity extends SerialPortActivity {


	private final int ENABLE_BUTTON = 2;
	private final int SHOW_VERSION = 3;
	private final int UPDATE_FW = 4;
	private final int SHOW_PROGRESS = 5;
	private final int DISABLE_BUTTON = 6;	
	private final int HIDE_PROGRESS=7;	
	private final int REFRESH_PROGRESS=8;	
	private final int SHOW_FONT_UPTAE_INFO=9;
	private final int SHOW_PRINTER_INFO_WHEN_INIT=10;
	private final byte  HDX_ST_NO_PAPER1 = (byte)(1<<0);     // 1 缺纸
	//private final byte  HDX_ST_BUF_FULL  = (byte)(1<<1);     // 1 缓冲满
	//private final byte  HDX_ST_CUT_ERR   = (byte)(1<<2);     // 1 打印机切刀错误
	private final byte  HDX_ST_HOT       = (byte)(1<<4);     // 1 打印机太热
	private final byte  HDX_ST_WORK      = (byte)(1<<5);     // 1 打印机在工作状态
	
	private boolean stop = false;
	public static int BinFileNum = 0;
	public static boolean ver_start_falg = false;
	boolean Status_Start_Falg = false;
	byte [] Status_Buffer=new byte[300];
	int Status_Buffer_Index = 0;
	public static int update_ver_event = 0;
	public static boolean update_ver_event_err = false;
	public static StringBuilder strVer=new StringBuilder("922");
	public static StringBuilder oldVer=new StringBuilder("922");
	public static File BinFile;
	// EditText mReception;
	private static final String TAG = "ConsoleActivity";
	private static   String Error_State = "";
	Time time = new Time();
	int TimeSecond;
	public CheckBox myCheckBox;
	public ProgressDialog myDialog = null;
	private int iProgress   = 0;
	String Printer_Info =new String();
	
	public static boolean flow_start_falg = false;	
	byte [] flow_buffer=new byte[300];
	
	public TextView TextViewSerialRx;
	public static Context context;
	MyHandler handler;
	EditText Emission;
	Button ButtonCodeDemo;
	Button ButtonImageDemo;
	Button ButtonGetVersion;
	Button ButtonUpdateVersion;
	Button ButtonCharacterDemo;
	Button ButtonUpdateFontLib;
	
	ExecutorService pool = Executors.newSingleThreadExecutor();
	WakeLock lock;
	int printer_status = 0;
	private ProgressDialog m_pDialog;
//add by chenming 2016.7.26
	boolean have_print_ready=false;
	boolean have_finish_print=false;
	boolean out_of_paper=false;
	private class MyHandler extends Handler {
		public void handleMessage(Message msg) {
			if (stop == true)
				return;
			switch (msg.what) {
			case DISABLE_BUTTON:
//				Close_Button();
				Log.d(TAG,"DISABLE_BUTTON");
				break;			
			case ENABLE_BUTTON:
//				ButtonCodeDemo.setEnabled(true);
//				ButtonImageDemo.setEnabled(true);
//				ButtonGetVersion.setEnabled(true);
//				ButtonUpdateVersion.setEnabled(true);
//				ButtonCharacterDemo.setEnabled(true);
//				ButtonUpdateFontLib.setEnabled(true);
				Log.d(TAG,"ENABLE_BUTTON");
				break;
			case SHOW_FONT_UPTAE_INFO:
				TextView tv3 = new TextView(ConsoleActivity.this);
				tv3.setText((String)msg.obj);
				tv3.setGravity(Gravity.CENTER);
				tv3.setTextSize(25);
				tv3.findFocus();
				new AlertDialog.Builder(ConsoleActivity.this)
						.setIcon(R.drawable.icon)
						.setView(tv3)
						.setCancelable(false)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {
								handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1,0, null));	
							}
						}).show();
				break;
			case SHOW_VERSION:
				TextView tv2 = new TextView(ConsoleActivity.this);
				tv2.setText(getString(R.string.currentFWV)
						+ ConsoleActivity.strVer.toString());
				tv2.setGravity(Gravity.CENTER);
				tv2.setTextSize(25);
				tv2.findFocus();
				new AlertDialog.Builder(ConsoleActivity.this)
						.setTitle(getString(R.string.getV))
						.setIcon(R.drawable.icon)
						.setView(tv2)
						.setCancelable(false)
						.setPositiveButton("OK", null).show();
				break;				
			case UPDATE_FW:
				m_pDialog.hide();
				TextView tv4 = new TextView(ConsoleActivity.this);
				// if(!ConsoleActivity.oldVer.toString().isEmpty())
				{
					tv4.setText(getString(R.string.previousFWV)
							+ ConsoleActivity.oldVer.toString() + "\n"
							+ getString(R.string.currentFWV)
							+ ConsoleActivity.strVer.toString());
//					TextViewSerialRx.setText(Printer_Info
//							+getString(R.string.previousFWV)
//							+ ConsoleActivity.oldVer.toString() + "\n"
//							+ getString(R.string.currentFWV)
//							+ ConsoleActivity.strVer.toString());
				}
				// else
				{
					// tv3.setText("update firmware version failed ");
				}
				tv4.setGravity(Gravity.CENTER);
				tv4.setTextSize(22);
				tv4.findFocus();
				new AlertDialog.Builder(ConsoleActivity.this)
						.setTitle(getString(R.string.updateFWFinish))
						.setIcon(R.drawable.icon).setView(tv4)
						.setCancelable(false)
						.setPositiveButton("OK", null).show();
				break;
			case SHOW_PROGRESS:
//				m_pDialog = new ProgressDialog(ConsoleActivity.this);
//				m_pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//				m_pDialog.setMessage((String)msg.obj);
//				m_pDialog.setIndeterminate(false);
//				m_pDialog.setCancelable(false);
//				m_pDialog.show();
				break;
			case  HIDE_PROGRESS:	
//				m_pDialog.hide();
				break;
			case   REFRESH_PROGRESS :	
//				m_pDialog.setProgress(iProgress);		
				break;	
			case     SHOW_PRINTER_INFO_WHEN_INIT:	
//				TextViewSerialRx.setText(Printer_Info+strVer.toString());
				break;				
			default:
				break;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "ConsoleActivity====onCreate");
		super.onCreate(savedInstanceState);
		 //先去除应用程序标题栏  注意：一定要在setContentView之前  
        requestWindowFeature(Window.FEATURE_NO_TITLE);  
		setContentView(R.layout.console);
		setTitle(getString(R.string.appName));
		context = ConsoleActivity.this;
		//新页面接收数据
        Bundle bundle = this.getIntent().getExtras();
        //接收name值
        final String printContent = bundle.getString("printContent");
        Log.i("获取到的name值为",printContent);     
		handler = new MyHandler();
		try{
		HdxUtil.SwitchSerialFunction(HdxUtil.SERIAL_FUNCTION_PRINTER);
		}catch(UnsatisfiedLinkError e){
			Toast.makeText(getApplicationContext(), "没有发现打印机设备，将通过无线打印机打印！", Toast.LENGTH_SHORT).show();
			 ConsoleActivity.this.setResult(Activity.RESULT_OK);
			 ConsoleActivity.this.finish();
		} catch(UnavailableException e){
			Toast.makeText(getApplicationContext(), "没有发现打印机设备，将通过无线打印机打印！", Toast.LENGTH_SHORT).show();
			 ConsoleActivity.this.setResult(Activity.RESULT_OK);
			 ConsoleActivity.this.finish();
		} catch(NoClassDefFoundError e){
			Toast.makeText(getApplicationContext(), "没有发现打印机设备，将通过无线打印机打印！", Toast.LENGTH_SHORT).show();
			 ConsoleActivity.this.setResult(Activity.RESULT_OK);
			 ConsoleActivity.this.finish();
		}         
		PowerManager pm = (PowerManager) getApplicationContext()
				.getSystemService(Context.POWER_SERVICE);
		lock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
		ConsoleActivity.strVer = new StringBuilder();
		ConsoleActivity.oldVer = new StringBuilder();
//		ButtonCodeDemo = (Button) findViewById(R.id.ButtonCodeDemo);
//		ButtonImageDemo = (Button) findViewById(R.id.ButtonImageDemo);
//		ButtonGetVersion = (Button) findViewById(R.id.GetVersion);
//		ButtonUpdateVersion = (Button) findViewById(R.id.UpdateVersion);
//		ButtonCharacterDemo = (Button) findViewById(R.id.ButtonCharacterDemo);
//		ButtonUpdateFontLib = (Button) findViewById(R.id.UpdateFontLib);
//		final Button ButtonQuit = (Button) findViewById(R.id.quit);
//		TextViewSerialRx = (TextView) findViewById(R.id.TextViewSerialRx);
//		Close_Button();

		 new InitThread(3).start();
		 while(!have_print_ready){
			 //一直等待直到打印完成
		 }
//	       ProgressDialog progressBar = new ProgressDialog(this);
//	        progressBar.setMessage("正在打印订单，请稍后…");
//	        progressBar.show();
		 new WriteThread(0,printContent).start();
		 while(!have_finish_print&&!out_of_paper){
			 //一直等待打印完成
		 }
//		 if (progressBar.isShowing()) {
//             progressBar.dismiss();
//         }
		 try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 if(out_of_paper){
			 Toast.makeText(getApplicationContext(), "打印失败，打印机缺纸或者纸没有装好！",Toast.LENGTH_LONG).show();
		 }else{
			 Toast.makeText(getApplicationContext(), "订单打印完成！",Toast.LENGTH_LONG).show();
		 }
		 ConsoleActivity.this.setResult(Activity.RESULT_OK);
		 ConsoleActivity.this.finish();
		//Init_Data_When_Start();
//		ButtonCharacterDemo.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//				new WriteThread(0,printContent).start();
//			
//			}
//		});
//		ButtonQuit.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				ConsoleActivity.this.finish();
//			}
//		});
//		ButtonCodeDemo.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//				new WriteThread(1,printContent).start();				
//			}
//		});
//
//		ButtonImageDemo.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//				new BmpThread().start();
//
//			}
//		});
//
//		ButtonGetVersion.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				boolean result;
//				handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//
//				result = get_fw_version();
//				if (result) {
//					
//					handler.sendMessage(handler.obtainMessage(SHOW_VERSION, 1,0, null));
//
//				}
//				handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1, 0,null));
//			}
//		});
//
//		ButtonUpdateVersion.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//
//				boolean result = get_fw_version();
//				if (!result) {
//
//					return;
//				}
//				ConsoleActivity.oldVer=ConsoleActivity.strVer;
//				String[] TransactionType = new String[] {
//						getResources().getString(R.string.TransactionType1),
//						getResources().getString(R.string.TransactionType2),
//						getResources().getString(R.string.TransactionType3),
//						getResources().getString(R.string.TransactionType4),
//			
//
//				};
//				AlertDialog.Builder builder = new AlertDialog.Builder(
//						ConsoleActivity.this);
//				builder.setTitle(getString(R.string.selectFW));
//				builder.setIcon(android.R.drawable.ic_dialog_info);
//				builder.setSingleChoiceItems(TransactionType, 0,
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog,
//									int which) {
//								System.out.println("whichwhichwhichwhich == "
//										+ which);
//								switch (which) {
//								case 0:
//									ConsoleActivity.strVer = new StringBuilder("945");
//									BinFileNum=R.raw.hdxcut945;
//									break;
//								case 1:
//									ConsoleActivity.strVer = new StringBuilder("946");	
//									BinFileNum=R.raw.hdxcut946;
//									break;
//								case 2:
//									ConsoleActivity.strVer = new StringBuilder("945");	
//									BinFileNum=R.raw.hdxlp945;
//									break;
//								case 3:
//									ConsoleActivity.strVer = new StringBuilder("946");	
//									BinFileNum=R.raw.hdxlp946;
//									break;
//				
//								default:
//									return;
//								}
//								String strOldOne = ConsoleActivity.oldVer
//										.toString().trim();
//								String strNewOne = ConsoleActivity.strVer
//										.toString().trim();
//								if (strOldOne.compareTo(strNewOne) >= 0)// 更新到老版本,发出警告信息
//								{
//									Log.e("quck2",
//											"bin : strOldOne.compareTo(strNewOne) >= 0 ");
//									// TextView warnText =new
//									// TextView(ConsoleActivity.this);
//									// warnText.setText(R.string.UpdateFWWarn);
//									new AlertDialog.Builder(
//											ConsoleActivity.this)
//											.setTitle(R.string.UpdateFWWarn)
//											.setIcon(R.drawable.icon)
//											// .setView(warnText)
//											.setPositiveButton(
//													R.string.Determine,
//													new DialogInterface.OnClickListener() {
//														public void onClick(
//																DialogInterface arg0,
//																int arg1) {
//
//															handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//
//															new UpdateFWThread(0)
//																	.start();
//														}
//													})	
//											.setNegativeButton(
//													R.string.cancel,
//													new DialogInterface.OnClickListener() {
//
//														public void onClick(
//																DialogInterface arg0,
//																int arg1) {
//														}
//													}).show();
//								} else {
//									handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//
//									new UpdateFWThread(0).start();
//
//								}
//								Log.e("quck2", "go to update fw  ");
//								dialog.dismiss();
//							}
//						});
//				builder.setCancelable(true);
//				builder.setNegativeButton(
//						"从其他地方选择固件",
//						new DialogInterface.OnClickListener() {
//							public void onClick(
//									DialogInterface arg0,int arg1) {
//								SDFileExplorer.FileType=0;
//								Intent intent = new Intent(ConsoleActivity.this,SDFileExplorer.class);
//								//startActivity(intent);
//								startActivityForResult(intent,0);
//								Log.e("quck2", "finish->从其他地方选择固件  "); 
//								
//							}
//						});				
//				builder.setPositiveButton(R.string.cancel, null);
//				builder.show();
//			}
//		});
//		
//		ButtonUpdateFontLib.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				boolean result = get_fw_version();
//				if (!result) {
//
//					return;
//				}
//				ConsoleActivity.oldVer=ConsoleActivity.strVer;
//				String[] TransactionType = new String[] {
//						getResources().getString(R.string.ZiKuFile1),
//				};
//				AlertDialog.Builder builder = new AlertDialog.Builder(
//						ConsoleActivity.this);
//				builder.setTitle(R.string.WarningTimeIsLong);
//				builder.setIcon(android.R.drawable.ic_dialog_info);
//				builder.setSingleChoiceItems(TransactionType, 0,
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog,
//									int which) {
//								
//								switch (which) {
//								case 0:
//									BinFileNum=R.raw.hdx_ziku_20151230;
//									break;						
//								default:
//									return;
//								}
//								handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//							    new UpdateFontLib_Thread(0).start();
//							    dialog.dismiss();
//							}
//							
//				});
//				builder.setCancelable(true);
//				builder.setNegativeButton(
//						"从其他地方选择固件",
//						new DialogInterface.OnClickListener() {
//							public void onClick(
//									DialogInterface arg0,int arg1) {
//								SDFileExplorer.FileType=1;
//								Intent intent = new Intent(ConsoleActivity.this,SDFileExplorer.class);
//								//startActivity(intent);
//								startActivityForResult(intent,0);
//								Log.e("quck2", "finish->从其他地方选择固件  "); 
//								
//							}
//						});				
//				builder.setPositiveButton(R.string.cancel, null);
//				builder.show();
//				 
//				
// 
//
//			}
//		});
//		Toast.makeText(getApplicationContext(), "apk version:2.6.1",Toast.LENGTH_SHORT).show();
//
//		
//	
	}
	@Override
	protected void onDataReceived(final byte[] buffer, final int size,final int n) 
	{
		int i;
		String strTemp;
		if(Status_Start_Falg == true)
		{
			for (i = 0; i < size; i++) 
			{
					Status_Buffer[getStatus_Buffer_Index()]=buffer[i];
					setStatus_Buffer_Index(1+i);
			}
		}
		
		if (ConsoleActivity.ver_start_falg == true) {
			for (i = 0; i < size; i++) {
				ConsoleActivity.strVer.append(String.format("%c",(char) buffer[i]));
			}

		}
		/*
		 * 	public static boolean flow_start_falg = false;	
		byte [] flow_buffer=new byte[300];
		
		 * */
 
		StringBuilder str = new StringBuilder();
		StringBuilder strBuild = new StringBuilder();
		for (i = 0; i < size; i++) {
			if(flow_start_falg == true)
			{
				if( (buffer[i] ==0x13) || ( buffer[i] ==0x11)  )
				{
					flow_buffer[0]= buffer[i];
					
				}
			}
			str.append(String.format(" %x", buffer[i]));
			strBuild.append(String.format("%c", (char) buffer[i]));
		}
		Log.e(TAG, "onReceivedC= " + strBuild.toString());
		Log.e(TAG, "onReceivedx= " + str.toString());

	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.e("quck3", "finish->浠庡叾浠栧湴鏂归€夋嫨鍥轰欢 -->3 "+ requestCode +"  "+resultCode); 
		 if (resultCode != 1)
		 {
			 Log.e("quck3", "finish->  resultCode != 1 "); 
			 return ;
		 }
		 if(SDFileExplorer.FileType ==0 )
		 {
			handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
			new UpdateFWThread(1).start();			 	 
		 }
		 else
		 {
			handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
		    new UpdateFontLib_Thread(1).start(); 
		 }

		
	}
	int getStatus_Buffer_Index()
	{
		return Status_Buffer_Index;
		
	}
	void setStatus_Buffer_Index(int v)
	{
		Status_Buffer_Index=v;
	}
	
	void Close_Button()
	{
	 
//			ButtonCodeDemo.setEnabled(false);
//			ButtonImageDemo.setEnabled(false);
//			ButtonGetVersion.setEnabled(false);
//			ButtonUpdateVersion.setEnabled(false);
//			ButtonCharacterDemo.setEnabled(false);
//			ButtonUpdateFontLib.setEnabled(false);	
	}	
	byte Get_Printer_Status() 
	{
		Status_Buffer[0]=0;
		Status_Buffer[1]=0;
		Status_Start_Falg = true;
		setStatus_Buffer_Index(0);
		sendCommand(0x1b,0x76);
		Log.i(TAG,"Get_Printer_Status->0x1b,0x76");
		Time_Check_Start();
		
		while(true)
		{
			if(getStatus_Buffer_Index()>0)
			{
				
				Status_Start_Falg = false;
				Log.e(TAG,"Get_Printer_Status :"+Status_Buffer[0]);
				return Status_Buffer[0] ;
			}			
			if(TimeIsOver(5))
			{
				Status_Start_Falg = false;
				Log.e(TAG,"Get_Printer_Status->TIME OVER:"+Status_Buffer[0]);
				return (byte)0xff;
				
			}
			sleep(50);
		}


	}
	
	void PrinterPowerOnAndWaitReady()
	{
		
		//Status_Buffer_Index=0;
		//Status_Start_Falg = true;
		HdxUtil.SetPrinterPower(1);	
		sleep(500); 
	}
	void PrinterPowerOff()
	{
		HdxUtil.SetPrinterPower(0);	
	}
	
	void Wait_Printer_Ready()
	{
		byte status;
		
		while(true)
		{
			status = Get_Printer_Status() ;
			if(status== 0xff)
			{
				Log.e(TAG," time is out");
				return ;
				
			}
			
			if( (status & HDX_ST_WORK)>0 )
			{
				
				Log.d(TAG,"printer is busy"); 
			}			
			else
			{
				Log.d(TAG," printer is ready");
				return;
				
			}
			sleep(50);
		}
	}
	//返回真, 有纸, 返回假 没有纸
	boolean  Printer_Is_Normal()
	{
		byte status;
		
	 
			status = Get_Printer_Status() ;
			 
			if(status== 0xff)
			{
				Log.e(TAG,"huck time is out");
				Error_State="huck unkown err";
				return  false;
				
			}
		 
			if( (status & HDX_ST_NO_PAPER1 )>0 )
			{
				
				Log.d(TAG,"huck is not paper"); 
				Error_State=getResources().getString(R.string.IsOutOfPaper);
				return false;
			}		
			else if( (status & HDX_ST_HOT )>0 ) 
			{
				Log.d(TAG,"huck is too hot"); 
				Error_State=getResources().getString(R.string.PrinterNotNormal1);
				return false;	
			}
			else
			{
				Log.d(TAG," huck is ready");
				return true;
			}
		 
		 
	}	
	//判断打印机装好纸 ,如果有 ,返回真,否者返回假
	boolean Warning_When_Not_Normal()
	{
		 
	 	
			handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
			if(  Printer_Is_Normal() )
			{
				
				Log.i(TAG,"quck_Is_Normal ok");
				return true;
			}			
			else
			{				
				handler.sendMessage(handler.obtainMessage(SHOW_FONT_UPTAE_INFO, 1, 0, Error_State));
				Log.d(TAG," quck_Is not_Paper");
				return false;
				
			}
	 
	}	
	/*
	 * 	public static boolean flow_start_falg = false;	
	byte [] flow_buffer=new byte[300];
	
	 * */
	
	void flow_begin()
	{
		
		flow_start_falg = true;
		flow_buffer[0]=  0x0;
		Log.i(TAG,"flow_begin ");
		
	}
	void flow_end()
	{
		
		flow_start_falg = false;
		flow_buffer[0]=  0x0;
		Log.i(TAG,"flow_end ");
	}
	
	
	boolean  flow_check_and_Wait(int timeout) 
	{
		
		
		boolean flag=false;
	 
		Time_Check_Start();
		
		while(true)
		{
			sleep(5);
			if(flow_buffer[0]== 0)
			{
				return true;
				//flow_start_falg = false;
				//Log.e(TAG,"Get flow ready" );
				//return true ;
			}
			sleep(50);
			if(flow_buffer[0]== 0x13)//暂停标志
			{
				
				if(flag ==false )
				{
					flag=true;
					Log.e(TAG,"Get flow 13" );
				}
				
				continue;
				//flow_start_falg = false;
				
				//return true ;
			}
			
			if(flow_buffer[0]== 0x11)
			{
				
				Log.e(TAG,"Get flow 11" );
				flow_buffer[0]=  0x0;
				return true;
				//flow_start_falg = false;
				//Log.e(TAG,"Get flow ready" );
				//return true ;
			}	
			

			if(timeout !=0)
			{
				if(TimeIsOver(timeout))
				{
				 
					Log.e(TAG,"Get_Printer flow timeout");
					return false;
					
				}				
				
			}

			sleep(50);
		}


	}	
	private class BmpThread extends Thread {
		public BmpThread() {
		}

		public void run() {
			super.run();
			PrinterPowerOnAndWaitReady();
			if(!Warning_When_Not_Normal())
			{
				PrinterPowerOff();
				return;
			}
			Wait_Printer_Ready();
			//ConsoleActivity.this.sleep(1000);
			lock.acquire();
			try {
				Resources r = getResources();
				// 以数据流的方式读取资源
				InputStream is = r.openRawResource(R.drawable.test);
				BitmapDrawable bmpDraw = new BitmapDrawable(is);
				Bitmap bmp = bmpDraw.getBitmap();
				PrintBmp(10, bmp);

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Wait_Printer_Ready();
				lock.release();
				//ConsoleActivity.this.sleep(1000);
				//HdxUtil.SetPrinterPower(0);
				PrinterPowerOff();
			}
			handler.sendMessage(handler
					.obtainMessage(ENABLE_BUTTON, 1, 0, null));
		}
	}

	private class GetVersionThread extends Thread {
		int type =0;
		public GetVersionThread(int type) {
			this.type=type;
		}

		public void run() {
			super.run();
			HdxUtil.SetPrinterPower(1);
			ConsoleActivity.this.sleep(500);
			lock.acquire();
			try {

				ConsoleActivity.strVer = new StringBuilder();
				ConsoleActivity.ver_start_falg = true;
				if(type == 0)
				{
					byte[] start2 = { 0x1D, 0x67, 0x66 };
					mOutputStream.write(start2);
				}
				else
				{
					byte[] start2 = { 0x1D, 0x67, 0x33 };
					mOutputStream.write(start2);
				}
								
				
			} catch (Exception e) {
				Log.e(TAG, "quck =" + "here1");
				e.printStackTrace();
			} finally {
				lock.release();
				ConsoleActivity.this.sleep(500);
				// HdxUtil.SetPrinterPower(0);
			}

		}
	}

	void Time_Check_Start() {
		time.setToNow(); // ȡ��ϵͳʱ�䡣
		TimeSecond = time.second;
		

	}

	boolean TimeIsOver(int second) {

		time.setToNow(); // ȡ��ϵͳʱ�䡣
		int t = time.second;
		if (t < TimeSecond) {
			t += 60;
		}

		if (t - TimeSecond > second) {
			return true;
		}
		return false;
	}

	// get current fw version
	public boolean get_fw_version() {
		// Timer timer;
		// get current fw version

		new GetVersionThread(0).start();
		Time_Check_Start();
		String strTemp;
		int i;
		while (true) {
			strTemp = strVer.toString().trim();
			if (strTemp.length() >=20) {
				i = strTemp.indexOf("vision:");
				if (i == -1) 
				{
					 
			
					continue;
					//return false;
				}
				else
				{
		
					strTemp = strTemp.substring(i + 2+6).trim();
					strVer = new StringBuilder(strTemp);	 	
				 
				}
				ConsoleActivity.ver_start_falg = false;
				try {
				 i = Integer.parseInt(strVer.toString());
				 
				} catch (Exception e) {
					Log.e(TAG, " onDataReceivet= Integer.parseInt Exception " );
					e.printStackTrace();
					return false;
				}			
				return true;

			}

			if (TimeIsOver(4)) {
				return false;

			}
			ConsoleActivity.this.sleep(50);
		}

	}
	

	// get current fw version
	public boolean get_Language()
	{
		new GetVersionThread(1).start();
		Time_Check_Start();
		Log.i(TAG," get_Language  "  );
		String strTemp;
		int i;
		while (true) 
		{	
			if (TimeIsOver(3))
			{
				Log.e(TAG, " faild ,TimeIsOver " );
				return false;

			}
			ConsoleActivity.this.sleep(10);
			strTemp = strVer.toString().trim();
			if (strTemp.length() >= 10) 
			{
				i = strTemp.indexOf(':');//i = strTemp.indexOf(".bin");
				if (i == -1) 
				{
					 
					Log.e(TAG, " faild ,onDataReceivee= "+ strTemp.length());
					//return false;
				}
				else
				{
					
					strTemp = strTemp.substring(i + 2).trim();
					strVer = new StringBuilder(strTemp);
					ConsoleActivity.ver_start_falg = false;
					Log.e(TAG, " ok ,onDataReceivet= "+ strVer.toString() );
					try {
					 i = Integer.parseInt(strVer.toString());
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
					return true;
			 		 				
				}

			}
			

		
		}

	}
	void int2ByteAtr(int pData, byte sumBuf[]) {
		for (int ix = 0; ix < 4; ++ix) {
			int offset = ix * 8;
			sumBuf[ix] = (byte) ((pData >> offset) & 0xff);
		}

	}

	// 4�ֽ����
	void Get_Buf_Sum(byte dataBuf[], int dataLen, byte sumBuf[]) {

		int i;
		long Sum = 0;
		// byte[] byteNum = new byte[8];
		long temp;

		for (i = 0; i < dataLen; i++) {
			if (dataBuf[i] < 0) {
				temp = dataBuf[i] & 0x7f;
				temp |= 0x80L;

			} else {
				temp = dataBuf[i];
			}
			Sum += temp;
			temp = dataBuf[i];

		}

		for (int ix = 0; ix < 4; ++ix) {
			int offset = ix * 8;
			sumBuf[ix] = (byte) ((Sum >> offset) & 0xff);
		}

	}

	private class UpdateFWThread extends Thread {
		int type;
		public UpdateFWThread(int type) {
			this.type=type;
		}

		public void run() {

			byte[] start2 = { 0x1B, 0x23, 0x23, 0x55, 0x50, 0x50, 0x47 };
			int temp;
			super.run();

			HdxUtil.SetPrinterPower(1);
			ConsoleActivity.this.sleep(500);

			lock.acquire();
			
			Message message = new Message();
			handler.sendMessage(handler.obtainMessage(SHOW_PROGRESS, 1, 0,getResources().getString(R.string.itpw)  ));
			try {
				if(type == 0 )
				{
					SendLongDataToUart(BinFileNum,start2,100,50);
				}
				else
				{
					SendLongDataToUart(BinFile,start2,100,50);
				}
				Log.e("quck2", "all data have send!!  ");
				sleep(3000);
				get_fw_version();
				message = new Message();
				message.what = UPDATE_FW;
				handler.sendMessage(message);

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			
				ConsoleActivity.this.sleep(200);
				lock.release();
				// HdxUtil.SetPrinterPower(0);
			}
			
			handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1, 0, null));
			
		}
	}
	
	private class UpdateFontLib_Thread extends Thread
	{
		int type;
		public UpdateFontLib_Thread( int type)
		{
			this.type=type;
		}


	public void run() {
		super.run();
		HdxUtil.SetPrinterPower(1);

		ConsoleActivity.this.sleep(500);

		//,0x1B,0x23,0x23,0x55 ,0x50 ,0x46 ,0x54 checksum len X1 X2 ... Xlen
		byte[] cmdHead= {0x1B,0x23,0x23,0x55 ,0x50 ,0x46 ,0x54};
		lock.acquire();
		handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1, 0,null));
		handler.sendMessage(handler.obtainMessage(SHOW_PROGRESS, 1, 0,getResources().getString(R.string.itpw2)));	
		ConsoleActivity.this.sleep(1000);
		if(type ==0)
		{
			SendLongDataToUart(BinFileNum,cmdHead,30*1000,40);
		}
		else
		{
			SendLongDataToUart(BinFile,cmdHead,30*1000,40);
		}
		
		//lock.release();
		handler.sendMessage(handler.obtainMessage(HIDE_PROGRESS, 1, 0,null));
		handler.sendMessage(handler.obtainMessage(SHOW_FONT_UPTAE_INFO, 1, 0,getResources().getString(R.string.itpw4)));
		handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1, 0,null));
		
	    }	
	}

	public void SendLongDataToUart(int fileID,byte [] command_head,int delay_time,int delay_time2 ) 
	{
		byte[] byteNum = new byte[4]; 
		byte[] byteNumCrc = new byte[4];
		byte[] byteNumLen = new byte[4];
	 	int i;
		int temp;
		Log.e(TAG,"  TEST_quck2");
		flow_begin();
		try {
				Resources r =getResources();;
				InputStream is = r.openRawResource(fileID);
				int count = is.available();
				byte[] b = new byte[count];
				is.read(b);
				byte SendBuf[] = new byte[count  +1023];
				Arrays.fill(SendBuf,(byte)0);

				Log.e("quck2", " read file is .available()= "+ count  );	
				//get command HEAD
			  
				
				//get crc 
				Get_Buf_Sum(b,count,byteNum);// 17	01 7E 00   CRC
				System.arraycopy(byteNum,0,byteNumCrc,0,4); 
				Log.e("quck2", "crc0  "+ String.format("0x%02x", byteNum[0] )  );	
				Log.e("quck2", "crc1  "+ String.format("0x%02x", byteNum[1] )	);	
				Log.e("quck2", "crc2  "+ String.format("0x%02x", byteNum[2] )	);	
				Log.e("quck2", "crc3  "+ String.format("0x%02x", byteNum[3] )  );	
				 
				
				//get len
				int2ByteAtr(count,byteNum); //58 54 01 00	LEN
				System.arraycopy(byteNum,0, byteNumLen,0,4);
				Log.e("quck2", "len0  "+ String.format("0x%02x", byteNum[0] )  );	
				Log.e("quck2", "len1  "+ String.format("0x%02x", byteNum[1] )	);	
				Log.e("quck2", "len2  "+ String.format("0x%02x", byteNum[2] )	);	
				Log.e("quck2", "len3  "+ String.format("0x%02x", byteNum[3] )  );
				
				//send command_head
				mOutputStream.write(command_head);
				//send crc
				mOutputStream.write(byteNumCrc);
				//send len
				mOutputStream.write(byteNumLen);
				//send bin file
				System.arraycopy(b,0,SendBuf,0, count); 
				temp= (count +63)/64;
				byte[] databuf= new byte[64]; 
				sleep(delay_time);
				for(i=0;i<temp;i++)
				{
					System.arraycopy(SendBuf,i*64,databuf,0,64); 
					
					//if((i%2) == 0)
					{
						//sleep(delay_time2);
						
					}
					Log.e("quck2", " updating ffont finish:"  +((i+1)*100)/temp +"%");	
					iProgress=((i+1)*100)/temp;
					handler.sendMessage(handler.obtainMessage(REFRESH_PROGRESS, 1, 0,null));
					mOutputStream.write(databuf);
					flow_check_and_Wait(10);
					//sleep(delay_time2);
					
				}
				
				Log.e("quck2", "all data have send!!  "   );
				sleep(3000);
				  
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			ConsoleActivity.this.sleep(200);
			flow_end();
			//HdxUtil.SetPrinterPower(0);	
		}

		handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1, 0,null));
	}

	public void SendLongDataToUart(File file,byte [] command_head,int delay_time,int delay_time2 ) 
	{
		byte[] byteNum = new byte[4]; 
		byte[] byteNumCrc = new byte[4];
		byte[] byteNumLen = new byte[4];
	 	int i;
		int temp;
		FileInputStream is ;
		flow_begin();
		try {

				is  = new FileInputStream(file);
				int count = is.available();
				byte[] b = new byte[count];
				is.read(b);
				byte SendBuf[] = new byte[count  +1023];
				Arrays.fill(SendBuf,(byte)0);

				Log.e("quck2", " read file is .available()= "+ count  );	
				//get command HEAD
				//get crc 
				Get_Buf_Sum(b,count,byteNum);// 17	01 7E 00   CRC
				System.arraycopy(byteNum,0,byteNumCrc,0,4); 
				Log.e("quck2", "crc0  "+ String.format("0x%02x", byteNum[0] )  );	
				Log.e("quck2", "crc1  "+ String.format("0x%02x", byteNum[1] )	);	
				Log.e("quck2", "crc2  "+ String.format("0x%02x", byteNum[2] )	);	
				Log.e("quck2", "crc3  "+ String.format("0x%02x", byteNum[3] )  );	
				 
				
				//get len
				int2ByteAtr(count,byteNum); //58 54 01 00	LEN
				System.arraycopy(byteNum,0, byteNumLen,0,4);
				Log.e("quck2", "len0  "+ String.format("0x%02x", byteNum[0] )  );	
				Log.e("quck2", "len1  "+ String.format("0x%02x", byteNum[1] )	);	
				Log.e("quck2", "len2  "+ String.format("0x%02x", byteNum[2] )	);	
				Log.e("quck2", "len3  "+ String.format("0x%02x", byteNum[3] )  );
				
				//send command_head
				mOutputStream.write(command_head);
				//send crc
				mOutputStream.write(byteNumCrc);
				//send len
				mOutputStream.write(byteNumLen);
				//send bin file
				System.arraycopy(b,0,SendBuf,0, count); 
				temp= (count +63)/64;
				byte[] databuf= new byte[64]; 
				sleep(delay_time);
				for(i=0;i<temp;i++)
				{
					System.arraycopy(SendBuf,i*64,databuf,0,64); 
					
					//if((i%2) == 0)
					{
						//sleep(delay_time2);
						
					}
					Log.e("quck2", " updating ffont finish:"  +((i+1)*100)/temp +"%");	
					iProgress=((i+1)*100)/temp;
					handler.sendMessage(handler.obtainMessage(REFRESH_PROGRESS, 1, 0,null));
					mOutputStream.write(databuf);
					flow_check_and_Wait(10);
					
				}
				
				Log.e("quck2", "all data have send!!  "   );
				sleep(3000);
				  
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			ConsoleActivity.this.sleep(200);
			flow_end();
			//HdxUtil.SetPrinterPower(0);	
		}

		handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1, 0,null));
	}
	private class WriteThread extends Thread {
		int  action_code;
		String printContent;
		public WriteThread(int  code,String m_printContent) {
			action_code = code;
			printContent=m_printContent;
		}

		public void run() {
			super.run();
			PrinterPowerOnAndWaitReady();
			if(!Warning_When_Not_Normal())
			{
				PrinterPowerOff();
				out_of_paper=true;  //打印机没有纸的时候
				return;
			}
			
			lock.acquire();
			try {
				
				Wait_Printer_Ready();
				switch(action_code)
				{
					case 0:
			 
						sendCharacterDemo(printContent);
						sendCommand(0x0a);
						sendCommand(0x1d,0x56,0x42,0x20);  
						sendCommand(0x1d, 0x56, 0x30); 
						Log.e("quck2", " print char test"   );				
						break;
					case 1:
						Log.e("quck2", "Print Code test  "   );
						sendCodeDemo();
					default:
						break;
				}
				ConsoleActivity.this.sleep(4000);	//给打印留出4s的时间开销
				have_finish_print=true;
			} finally {
				Wait_Printer_Ready();
				lock.release();
				PrinterPowerOff();
				handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1,0, null));
			}

		}
	}

	private class InitThread extends Thread {
		int  action_code;

		public InitThread(int  code) {
			action_code = code;
		}

		public void run() {
			super.run();
			//PrinterPowerOnAndWaitReady();
			lock.acquire();
			try {
				
				switch(action_code)
				{
					
					case 3:
						Init_Data_When_Start();	
						have_print_ready=true;
						break;
					default:
						break;
				}
				//ConsoleActivity.this.sleep(4000);	
				
			} finally {
				
				lock.release();
			
				
			}

		}
	}
void Init_Data_When_Start()
{
	
	handler.sendMessage(handler.obtainMessage(DISABLE_BUTTON, 1,0, null));
//	handler.sendMessage(handler.obtainMessage(SHOW_PROGRESS, 1, 0,getResources().getString(R.string.itpw3)));
	//PrinterPowerOnAndWaitReady();
 
	int i =15;// Integer.parseInt(strVer.toString());
	String[] city=getResources().getStringArray(R.array.language);
	Printer_Info=getResources().getString(R.string.CurrentLanguageis);
	if(!city[i].isEmpty())
	{
		
		Printer_Info +=city[i];
		Printer_Info +="\n";
	}	
	else
	{
		Printer_Info="";
		Printer_Info +="\n";
	}
	iProgress=3;
	handler.sendMessage(handler.obtainMessage(REFRESH_PROGRESS, 1, 0,null));
	String str;	
	if(get_fw_version())
	{
		
		str=getResources().getString(R.string.currentFWV);
		str +=strVer.toString();
		str +="\n";  
		strVer=new StringBuilder(str);
	}
    if(Warning_When_Not_Normal())
    {
		//if(get_fw_version())
		{
			
			//str=getResources().getString(R.string.currentFWV);
			///str +=strVer.toString();
			//str +="\n";  
			//str2  =getResources().getString(R.string.HavePaper);
		//	str2 +="\n";
			//strVer=new StringBuilder(str);
		
			//handler.sendMessage(handler.obtainMessage(SHOW_PRINTER_INFO_WHEN_INIT, 1,0, null));	
			//iProgress=50;
			//handler.sendMessage(handler.obtainMessage(REFRESH_PROGRESS, 1, 0,null));
			//handler.sendMessage(handler.obtainMessage(HIDE_PROGRESS, 1, 0,null));
			//handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1,0, null));	
		}					    	
    }
    
 
 		//iProgress=50;
		//handler.sendMessage(handler.obtainMessage(REFRESH_PROGRESS, 1, 0,null));
		handler.sendMessage(handler.obtainMessage(HIDE_PROGRESS, 1, 0,null));	
		//handler.sendMessage(handler.obtainMessage(SHOW_FONT_UPTAE_INFO, 1, 0,getResources().getString(R.string.IsOutOfPaper)));
		handler.sendMessage(handler.obtainMessage(ENABLE_BUTTON, 1,0, null));
		handler.sendMessage(handler.obtainMessage(SHOW_PRINTER_INFO_WHEN_INIT, 1,0, null));	
		
		
    
    
    //PrinterPowerOff();
    
}


private void sendCodeDemo() {
		sendCommand(0x1d, 0x45, 0x43, 0x1);//条码开关指令-->打开

		// ufc-a
		// sendCommand(0x1d,0x6b,0x00,0x30,0x31,0x32,0x33,0x34,0x35,0x36,0x37,
		// 0x38,0x39,0x30,0x35,0x00);

		// code 128
		//sendCommand(0x1d, 0x6b, 0x49, 0x05, 0x31, 0x32, 0x33, 0x34, 0x35);
		sendCommand(0x1D ,0x6B ,0x41 ,0x0B ,0x30 ,0x31 ,0x32 ,0x33 ,0x34 ,0x35 ,0x36 ,0x37 ,0x38 ,0x39 ,0x30 );	
		// qr code,not supported by all platform
		//sendCommand(0x1B, 0x23, 0x23, 0x51, 0x50, 0x49, 0x58, 0xa);
		//打印二维码1
		//sendCommand(0x1d, 0x28, 0x6b, 71 + 3 + 80, 0x00, 0x31, 0x50, 0x30,
		sendCommand(0x1d, 0x28, 0x6b, 150 + 3 + 1, 0x00, 0x31, 0x50, 0x30,	
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
				0x51, 0x64, 0x69, 0x6E, 0x47, 0x35, 0x36, 0x37, 0x38, 0x39,
				0xB4, 0x38, 0x52, 0x16, 0x55, 0x73, 0x63, 0x15, 0x15, 0xA3,
				
				0x4B, 0x00, 0xC8, 0xA1, 0x35, 0x34, 0xDC, 0x36, 0x99, 0x18,
				0x89, 0x76, 0xD1, 0x3C, 0xF5, 0x70, 0x89, 0xA8, 0x5F, 0xE6,
				0x2C, 0x42, 0x5C, 0xE6, 0x5C, 0x20, 0x5A, 0xEB, 0xF2, 0x1A,
				0x5D, 0xE2, 0x55, 0x74, 0x59, 0xE1, 0x50, 0x63, 0x5A, 0xE4,
				0x8C, 0x48, 0x54, 0x92, 0x23, 0xB9, 0x2D, 0xE3, 0x55, 0xA9,
				
				0x6D);
		sendCommand(0x1d, 0x28, 0x6b, 0x03, 0x00, 0x31, 0x51, 0x30);
		sendCommand(0x1b, 0x4a, 0x30); // line feed

		sendCommand(0x1B, 0x23, 0x23, 0x51, 0x50, 0x49, 0x58, 0x5);
		//sendCommand(0x1d, 0x28, 0x6b,/* (71+3+80+150)%256 */255, 48, 0x31, 0x50,
		//打印二维码2
		sendCommand(0x1d, 0x28, 0x6b,/* (71+3+80+150)%256 */200+3+2, 0, 0x31, 0x50,	0x30,
				0x30, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,
				0x39, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38,

				0x39, 0x6D);
		sendCommand(0x1d, 0x28, 0x6b, 0x03, 0x00, 0x31, 0x51, 0x30);
		sendCommand(0x1b, 0x4a, 0x30); // line feed
		sendCommand(0x1b, 0x4a, 0x30); // line feed
		sendCommand(0x1b, 0x4a, 0x30); // line feed
	}

	private void sendCommand(int... command) {
		try {
			for (int i = 0; i < command.length; i++) {
				mOutputStream.write(command[i]);
				// Log.e(TAG,"command["+i+"] = "+Integer.toHexString(command[i]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// / sleep(1);
	}

	private void sendCharacterDemo(String printContent) {

//		Log.e(TAG, "#########sendCharacterDemo##########");//,0x1B,0x23,0x46
//		sendCommand(0x1B, 0x23, 0x23, 0x53, 0x4C, 0x41, 0x4E, 0x0e ); // taiwan
//		try {
//			mOutputStream.write("撥出受固定撥號限制".getBytes("Big5"));
//			mOutputStream.write("目前無法連上這個網路".getBytes("Big5"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		sendCommand(0x0a);
		
		try {
			sendCommand(0x1B, 0x23, 0x23, 0x53, 0x4C, 0x41, 0x4E, 0x0f); // china
			mOutputStream.write(printContent.getBytes("cp936"));
//			sendCommand(0x1D, 0x21, 0x01); // double height
//			mOutputStream.write("倍高命令".getBytes("cp936"));
//			sendCommand(0x0a);
//			sendCommand(0x1D, 0x21, 0x00); // cancel double height
//			mOutputStream.write("取消倍高命令".getBytes("cp936"));
//			sendCommand(0x0a);
//			sendCommand(0x1D, 0x21, 0x10); // double width
//			mOutputStream.write("倍宽命令".getBytes("cp936"));
//			sendCommand(0x0a);
//			sendCommand(0x1D, 0x21, 0x00); // cancel double width
//			mOutputStream.write("取消倍宽命令".getBytes("cp936"));
//			sendCommand(0x0a);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
//		try {
//			mOutputStream.write("english test".getBytes());
//			sendCommand(0x0a);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

//		sendCommand(0x1B, 0x23, 0x23, 0x53, 0x4C, 0x41, 0x4E, 0x20); // thailand
//		try {
//			mOutputStream.write("แต่ถ้าหากเธอไม่สามารถช่วยพี่ชแต่ถ้าหากเธอไม่สามารถช่วยพี่ชแต่ถ้าหากเธอไม่สามารถช่วยพี่ชแต่ถ้าหากเธอไม่สามารถช่วยพี่ช"
//					.getBytes("cp874"));
//			sendCommand(0x0a);
//			//40个泰文
//			mOutputStream.write("แต่ถ้าหากเธอแต่ถ้าหากเธอแต่ถ้าหากเธอแต่ถ้าหากเธอ".getBytes("cp874"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		sendCommand(0x0a);

//		sendCommand(0x1B, 0x23, 0x23, 0x53, 0x4C, 0x41, 0x4E, 0x22); // russia
//		try {
//			mOutputStream
//					.write("У этого сайта проблемы с сертификатом безопасности."
//							.getBytes("CP1251"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		sendCommand(0x0a);



	}

	private void sleep(int ms) {
		// Log.d(TAG,"start sleep "+ms);
		try {
			java.lang.Thread.sleep(ms);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Log.d(TAG,"end sleep "+ms);
	}

	public void PrintBmp(int startx, Bitmap bitmap) throws IOException {
		// byte[] start1 = { 0x0d,0x0a};
		byte[] start2 = { 0x1D, 0x76, 0x30, 0x30, 0x00, 0x00, 0x01, 0x00 };

		int width = bitmap.getWidth() + startx;
		int height = bitmap.getHeight();

		if (width > 384)
			width = 384;
		int tmp = (width + 7) / 8;
		byte[] data = new byte[tmp];
		byte xL = (byte) (tmp % 256);
		byte xH = (byte) (tmp / 256);
		start2[4] = xL;
		start2[5] = xH;
		start2[6] = (byte) (height % 256);
		;
		start2[7] = (byte) (height / 256);
		;
		mOutputStream.write(start2);
		for (int i = 0; i < height; i++) {

			for (int x = 0; x < tmp; x++)
				data[x] = 0;
			for (int x = startx; x < width; x++) {
				int pixel = bitmap.getPixel(x - startx, i);
				if (Color.red(pixel) == 0 || Color.green(pixel) == 0
						|| Color.blue(pixel) == 0) {
					// 高位在左，所以使用128 右移
					data[x / 8] += 128 >> (x % 8);// (byte) (128 >> (y % 8));
				}
			}
			
			while ((printer_status & 0x13) != 0) {
				Log.e(TAG, "printer_status=" + printer_status);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			mOutputStream.write(data);
			/*
			 * try { Thread.sleep(5); } catch (InterruptedException e) { }
			 */
		}
	}

	protected void onDestroy() 
	{
		super.onDestroy();
		stop = true;
		//PrinterPowerOff();
		Log.e(TAG, "onDestroy"  );
		
	 }
}
