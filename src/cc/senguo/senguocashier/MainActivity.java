package cc.senguo.senguocashier;

import hdx.HdxUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.SyncStateContract.Constants;
import android.util.Log;
import android.view.KeyCharacterMap.UnavailableException;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;

 public class MainActivity extends Activity {
	 private final static String TAG = MainActivity.class.getSimpleName();
    static WebView main_web;           //webview ID
    final  static String murl="http://test.senguo.cc/admin/cashier"; //全局url
    final  static String logouturl="http://test.senguo.cc/admin/logout"; //退出登陆url
    String[] devices;//android设备拥有的所有串口设备
    final static  String[] rate={"300","600","1200","1800","2400","3600","4800","7200","9600","14400","19200","28800"};//常量 代表串口波特率
	public static int connetTime = 0;  //蓝牙链接次数 在第一次连接失败的情况下 尝试多进行几次连接
    String m_device; //选择的串口设备
    static String[]  devices_mac=null; //所有配对蓝牙设备的MAC地址
    static String[]  devices_name=null; //所有配对蓝牙设备的名称
    static String[]  devices_info=null;//MAC地址和名连接起来的字符串
    String connect_mac=null; //选择链接的蓝牙的MAC
    int baudrate=9600;//串口设备选择的波特率
   static float weight=0;//通过串口或者蓝牙设备获取的称的重量
    ValueCallback<Uri> valueCallback;
    ProgressDialog progressBar; //进度提示框
    AlertDialog alertDialog; //提示弹出框
    AlertDialog.Builder    shezhiAlertDialog;//点击设置弹出的列表提示框
    BluetoothAdapter  mBluetoothAdapter;
    BluetoothDevice  mBluetoothDevice;
    boolean connecting,connected=false;//蓝牙链接状态 正在链接和链接完成
	static boolean have_getblooth=false;//判断线程获取蓝牙列表是否完成，完成之后变为真，然后即可进行后续操作
	public UUID uuid;
	private BluetoothSocket socket;
	GlobalHandler mGlobalHandler=new GlobalHandler();//自定义handler句柄
	String[] shezhi={"连接蓝牙设备","设置钱箱开关"};//用于匹配设置点击的是什么操作
 	boolean  money_box_open=false;//钱箱打开状态  #false 没打开 #true 打开
 	private static final int REQUEST_CODE= 200;
 	private static final int REQUEST_CODE_SCAN=400;
 	boolean is_connected=false;  //蓝牙是否连接
 	private BroadcastReceiver rec_fromGetWeightService = null,rec_fromBluetoothService=null;
 	private String mDeviceName;
	private String mDeviceAddress;
	private ExpandableListView mGattServicesList;
	private BluetoothLeService mBluetoothLeService;
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
	private boolean  onbind_service=false;
	 private static final String TARGET_SERVICE_UUID="0000ffe0-0000-1000-8000-00805f9b34fb";
	 private static final String TARGET_CHAACTERISTIC_UUID="0000ffe1-0000-1000-8000-00805f9b34fb";
	 private static final String CHOSE_OBE_BLE="cc.senguo.SenguoAdmin.DeviceScanActivity";
	static Handler myhandler=new Handler(){
		@SuppressWarnings("deprecation")
		public void handleMessage(Message msg){
			switch(msg.what){
			case 0:
				break;
			}
		}
	};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
        //自定义标题
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        //设置标题为某个layout
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
        main_web=(WebView)findViewById(R.id.main_web);
        
      //注册接收来自于取重的广播
		 this.rec_fromGetWeightService = new MyBroadcastReceiver();
		 IntentFilter filter_fromweight = new IntentFilter();
		 filter_fromweight.addAction("cc.senguo.senguocashier.GetWeightFromBluetooth");// 这里的Action要和Service发送广播时使用的Action一样
		 MainActivity.this.registerReceiver(this.rec_fromGetWeightService, filter_fromweight);  // 动态注册BroadcastReceiver

		 //接收来自于获取蓝牙设备的广播
		 this.rec_fromBluetoothService = new MyBroadcastReceiver();
		 IntentFilter filter_from_bluetooth = new IntentFilter();
		 filter_from_bluetooth.addAction("cc.senguo.senguocashier.BluetoothService");// 这里的Action要和Service发送广播时使用的Action一样
		 MainActivity.this.registerReceiver(this.rec_fromGetWeightService, filter_from_bluetooth);  // 动态注册BroadcastReceiver
        //设置按钮添加相应事件
        ImageButton get_lanya = (ImageButton) findViewById(R.id.lanya);
        get_lanya.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
        // TODO Auto-generated method stub
        	AlertDialog.Builder builder_shezhi = new AlertDialog.Builder(MainActivity.this);
        	builder_shezhi.setIcon(android.R.drawable.ic_dialog_info);
        	builder_shezhi.setTitle("设置");
        	builder_shezhi.setItems(shezhi, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                	if(shezhi[which]=="连接蓝牙设备"){
                		int currentapiVersion=android.os.Build.VERSION.SDK_INT; 
                		if(currentapiVersion<18){
                    		//普通方式打开蓝牙设备
                    		Intent intent_blue = new Intent(MainActivity.this, BluetoothService.class);
                    		MainActivity.this.startService(intent_blue);
                		}else{
//                			蓝牙4.0打开蓝牙设备
                			if(onbind_service){
        						unbindService(mServiceConnection);
        						mBluetoothLeService = null;
        						onbind_service=false;
        					}
        					Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
        					startActivityForResult(intent,REQUEST_CODE_SCAN);
                		}
                	}
                	else if(shezhi[which]=="设置钱箱开关"){
                		if(money_box_open){
                			try{
                				HdxUtil.SetV12Power(0);
                				money_box_open=false;
                    			Toast.makeText(getApplicationContext(), "钱箱已关闭！", Toast.LENGTH_SHORT).show();
                			}catch(UnsatisfiedLinkError e){
                				Toast.makeText(getApplicationContext(), "没有发现钱箱设备！", Toast.LENGTH_SHORT).show();
                			}  catch(UnavailableException e){
            				Toast.makeText(getApplicationContext(), "没有发现钱箱设备！", Toast.LENGTH_SHORT).show();
            			} catch(NoClassDefFoundError e){
            				Toast.makeText(getApplicationContext(), "没有发现钱箱设备！", Toast.LENGTH_SHORT).show();
            			}         
                		}else{
                			try{
                				HdxUtil.SetV12Power(1);
                				money_box_open=true;
                    			Toast.makeText(getApplicationContext(), "钱箱已打开！", Toast.LENGTH_SHORT).show();
                			}catch(UnsatisfiedLinkError e){
                				Toast.makeText(getApplicationContext(), "没有发现钱箱设备！", Toast.LENGTH_SHORT).show();
                			} catch(UnavailableException e){
                				Toast.makeText(getApplicationContext(), "没有发现钱箱设备！", Toast.LENGTH_SHORT).show();
                			} catch(NoClassDefFoundError e){
                				Toast.makeText(getApplicationContext(), "没有发现钱箱设备！", Toast.LENGTH_SHORT).show();
                			}         
                		}
                	}
                }
            });
        	builder_shezhi.show();
        }
        });
        
        //退出按钮响应事件
        ImageButton logout = (ImageButton) findViewById(R.id.logout);
        logout.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
        // TODO Auto-generated method stub
        	 new AlertDialog.Builder(MainActivity.this).setTitle("退出提示")//设置对话框标题  		  
			     .setMessage("你确定退出森果收银机吗?")//设置显示的内容  
			     .setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮  
			         @Override  
			         public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件  	  
			             // TODO Auto-generated method stub  
			            // finish(); 
			            main_web.loadUrl(logouturl);
			        	System.exit(0);
			         }  
			     }).setNegativeButton("取消",new DialogInterface.OnClickListener() {//添加返回按钮  
			         @Override  
			         public void onClick(DialogInterface dialog, int which) {//响应事件  
			             // TODO Auto-generated method stub
			         }  
			     }).show();//在按键响应事件中显示此对话框  
        }
        });
        
        //设置webview的user-agent 用于后台服务器鉴别这是android webview中的事件
        String ua = main_web.getSettings().getUserAgentString();
        main_web.getSettings().setUserAgentString(ua+"; HFWSH /"+"senguo:cashierapp");
        main_web.loadUrl(murl);
        progressBar = new ProgressDialog(this);
        WebSettings webSettings = main_web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("GBK");//设置字符编码
        main_web.addJavascriptInterface(new WebAppInterface(this), "AndroidCashier");
       // main_web.loadUrl("javascript:getweightback('"+weight+"')");
        //加上下面这段代码可以使网页中的链接不以浏览器的方式打开
        main_web.setWebChromeClient(new WebChromeClient() {
            // Android > 4.1.1 调用这个方法
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType,
                                        String capture) {
                valueCallback = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, null),
                        1);
            }
            // 3.0 + 调用这个方法
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType) {
                valueCallback = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(
                        Intent.createChooser(intent, "完成操作需要使用"), 1);
            }
            // Android < 3.0 调用这个方法
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                valueCallback = uploadMsg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(
                        Intent.createChooser(intent, "完成操作需要使用"), 1);
            }
            
            @Override
            //重写webview的 js弹出框
            public boolean onJsAlert(WebView view, String url, String message,
                                     final JsResult result) {
                AlertDialog.Builder b2 = new AlertDialog.Builder(
                        MainActivity.this)
                        .setTitle("温馨提示")
                        .setMessage(message)
                        .setPositiveButton("确认",
                                new AlertDialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                });
                b2.setCancelable(false);
                b2.create();
                b2.show();
                return true;
            }
        });
        
        //设置webview的url重载
        main_web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;

            }
            
          //页面加载完成的事件处理
            @Override
            public void onPageFinished(WebView view, String url) {

                if (progressBar.isShowing()) {
                    //progressBar.dismiss();
                }
                super.onPageFinished(view, url);
            }

            @SuppressWarnings("deprecation")
            @Override
            //重载网页连接异常处理代码
            public void onReceivedError(WebView view, int errorCode,
                                        String description, final String failingUrl) {
                alertDialog.setTitle("页面加载出错");
                alertDialog.setMessage("网络连接不正常,请检查网络..");
                alertDialog.setButton("刷新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO Auto-generated method stub
                        main_web.loadUrl(failingUrl);
                    }
                });
                alertDialog.show();
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
    }
    
    private long exitTime = 0; //按返回健退出程序的两次点击事件间隔时间
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
	    	if(keyCode == KeyEvent.KEYCODE_BACK)
		    {
	    		System.out.println(main_web.getUrl());
	    		if (main_web.getUrl().equals(murl)) {
	 				 new AlertDialog.Builder(MainActivity.this).setTitle("退出提示")//设置对话框标题  		  
	 			     .setMessage("你确定退出森果收银机吗?退出之后将会断开蓝牙链接，再次进入之后需要重新链接蓝牙！")//设置显示的内容  
	 			     .setPositiveButton("确定",new DialogInterface.OnClickListener() {//添加确定按钮  
	 			         @Override  
	 			         public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件  	  
	 			             // TODO Auto-generated method stub  
	 			            // finish();  
	 			        	System.exit(0);
	 			         }  
	 			     }).setNegativeButton("取消",new DialogInterface.OnClickListener() {//添加返回按钮  
	 			         @Override  
	 			         public void onClick(DialogInterface dialog, int which) {//响应事件  
	 			             // TODO Auto-generated method stub
	 			         }  
	 			     }).show();//在按键响应事件中显示此对话框  
	 				return true;
	    		}else if(main_web.getUrl().equals("http://test.senguo.cc/customer/login?next=%2Fadmin%2Fcashier")){
	    			if((System.currentTimeMillis()-exitTime) > 2000){  
	    	            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();                                
	    	            exitTime = System.currentTimeMillis();   
	    	        } else {
	    	            //finish();
	                System.exit(0);
	    	        }
	    	        return true;   
	    		}
		    	main_web.goBack();
		    	return true;
		    }
	    }
	    return super.onKeyDown(keyCode, event);
	}
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(onbind_service){
			 unbindService(mServiceConnection);
			 mBluetoothLeService=null;
		 }
        System.exit(0);  // 出现空白页的地方，找了哥哥我好久啊啊啊啊啊
    }
    
    //后续串口称使用开发使用 获取用户选择的串口设备和波特率
    private void get_device() {  
    	AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setIcon(android.R.drawable.ic_dialog_info);
        builder1.setTitle("请选择波特率");
        builder1.setItems(rate, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	baudrate=Integer.valueOf( rate[which]).intValue();
                Toast.makeText(MainActivity.this, "选择的波特率为：" + rate[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder1.show();
        AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
        builder2.setIcon(android.R.drawable.ic_dialog_info);
        builder2.setTitle("请选择串口");
        //    设置一个下拉的列表选择项
        builder2.setItems(devices, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	m_device=devices[which];
                Toast.makeText(MainActivity.this, "选择的串口为：" + devices[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder2.show();
    }  
    public void get_blooth(){
        //根据选择的设备链接蓝牙设备
    	AlertDialog.Builder builder3 = new AlertDialog.Builder(MainActivity.this);
        builder3.setIcon(android.R.drawable.ic_dialog_info);
        builder3.setTitle("请选择蓝牙设备进行连接");
        builder3.setItems(devices_name, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
            	connect_mac= devices_mac[which];
            	progressBar.setMessage("正在连接蓝牙："+ devices_name[which]);
            	progressBar.show();
                //Toast.makeText(MainActivity.this, "选择的蓝牙设备为：" + devices_name[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder3.show();  
    }
    @Override
 	protected void onActivityResult(int requestCode, int resultCode,
 			Intent intent) {
    	if (requestCode==200){
			switch (resultCode){
				case Activity.RESULT_OK:
					GlobalData.setHave_click_print(false);//判断一次点击时间成功返回东西了，防止连续点击
					break;
				case 500:
					break;
			}
		}else if(requestCode==400){
			switch (resultCode){
			case Activity.RESULT_OK:
				mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
				mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
				Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
				bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
				onbind_service=true;
		}
	}
    }
    //js中调用android方法的接口 有待开发
    class WebAppInterface {
        Context mContext;
        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        //js中调用android的toast方法
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
        //下面这个@JavascriptInterface 非常重要 如果没有添加 那么这个函数是无法调用的
       //获取称的打印数据
        @JavascriptInterface  
        public boolean BeginGetWeight() {  
                    Handler mHandler;
			//调用js中的onJsAndroid方法 
                 myhandler.post(new Runnable() { 
                 public void run() { 
                	 main_web.loadUrl("javascript:getweightback('"+GlobalData.getWeight()+"')");
                 } 
             }); 
                	// Toast.makeText(mContext, String.valueOf(weight), Toast.LENGTH_SHORT).show();
                	 return true;
        } 
      //webview调用android的打印函数
        @JavascriptInterface  
        public String PrintOrder(final String printContent ) {  
                    //调用js中的onJsAndroid方法 
        	GlobalData.setHave_click_print(true);
        	 myhandler.post(new Runnable() { 
				public void run() { 
                	 Intent intent =new Intent(MainActivity.this,ConsoleActivity.class); 
                	    //用Bundle携带数据
                	    Bundle bundle=new Bundle();
                	    //传递name参数为tinyphp
                	    bundle.putString("printContent",printContent);
                	    intent.putExtras(bundle);
//                	    startActivity(intent); 
                	    startActivityForResult(intent,REQUEST_CODE);
                 } 
             }); 
        	 while(GlobalData.getHave_click_print()){     		 
        	 }
                	 return "success";
        } 
    }
    
    //自定义handler 用于处理子线程和主线程之间的关系
    public class GlobalHandler extends Handler{
    	public GlobalHandler(){
    	}	
    	@Override
    	public void handleMessage(Message msg){
    		switch(msg.what){
    		case 1:   //蓝牙连接失败
    			 if (progressBar.isShowing()) {
                     progressBar.dismiss();
                 }
          	Toast.makeText(MainActivity.this, "蓝牙连接失败，请检查串口称蓝牙设备是否正常工作！", Toast.LENGTH_LONG).show();
    			break;
    		case 2:  //蓝牙连接成功
   			 if (progressBar.isShowing()) {
                 progressBar.dismiss();
             }
    		Toast.makeText(MainActivity.this, "蓝牙连接成功......", Toast.LENGTH_LONG).show();
    		case 3:  //连接异常处理
    			Bundle b_exception=msg.getData();
    			Toast.makeText(MainActivity.this, b_exception.getString("exception"), Toast.LENGTH_LONG).show();
                break;
    		default:
    			break;
    		}
    	}
    }
  //开始称重收银的取重以及开启蓝牙

  	public void BeginOpenBluetooth(){
  		//打开蓝牙设备
  		Intent intent_blue = new Intent(MainActivity.this, BluetoothService.class);
  		MainActivity.this.startService(intent_blue);
  	}
  	//实例化自己的广播接收处理
  	private class MyBroadcastReceiver extends BroadcastReceiver {
  		@Override
  		public void onReceive(Context context, Intent intent) {
  			String action = intent.getAction();
			if(action.equals("cc.senguo.senguocashier.GetWeightFromBluetooth")) {
  			boolean connected=intent.getBooleanExtra("connect_status",false);
  			is_connected=connected;
  			if(progressBar.isShowing()){
  				progressBar.dismiss();
  			}
  			if(connected){
  				Toast.makeText(MainActivity.this, "蓝牙连接成功......", Toast.LENGTH_LONG).show();
  			}
  			else{
  				Toast.makeText(MainActivity.this, "蓝牙连接失败，请检查串口称是否正常工作！", Toast.LENGTH_SHORT).show();
  			}
  		}
			if(action.equals("cc.senguo.senguocashier.BluetoothService")){
				choose_blooth_device();
			}
  		}
  	}

  	public void choose_blooth_device(){
  		//根据选择的设备链接蓝牙设备
  		AlertDialog.Builder builder3 = new AlertDialog.Builder(MainActivity.this);
  		builder3.setIcon(android.R.drawable.ic_dialog_info);
  		builder3.setTitle("请选择蓝牙设备进行连接");
  		final String[] devices_mac=GlobalData.getDevices_mac();
  		final String[] devices_name=GlobalData.getDevices_name();
  		builder3.setItems(devices_name, new DialogInterface.OnClickListener()
  		{
  			@Override
  			public void onClick(DialogInterface dialog, int which)
  			{
  				String tem_mac=devices_mac[which];
  				connect_mac= devices_mac[which];
  				//链接蓝牙设备并进行称重
  				progressBar.setMessage("正在连接蓝牙："+ devices_name[which]);
  				progressBar.show();
  				if(is_connected && tem_mac.equals(GlobalData.getConnect_mac())){
  					if(progressBar.isShowing()){
  						progressBar.dismiss();
  					}
  					Toast.makeText(MainActivity.this, "蓝牙已经连接成功，不需重新连接！", Toast.LENGTH_LONG).show();
  				}
  				else{
  					if(is_connected){
  						Intent stop_intent=new Intent(MainActivity.this,GetWeightFromBluetooth.class);
  						stopService(stop_intent);
  					}
  						Intent intent_weight = new Intent(MainActivity.this, GetWeightFromBluetooth.class);
  						GlobalData.setConnect_mac(connect_mac);
  						MainActivity.this.startService(intent_weight);
  				}
  			}
  		});
  		builder3.show();
  	}
  	
	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};
 // Handles various events fired by the Service.
 	// ACTION_GATT_CONNECTED: connected to a GATT server.
 	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
 	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
 	// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
 	//                        or notification operations.
 	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			final String action = intent.getAction();
 			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
//                 mConnected = true;
//                 updateConnectionState(R.string.connected);
//                 invalidateOptionsMenu();
 				Toast.makeText(MainActivity.this,"蓝牙连接成功.....",Toast.LENGTH_SHORT);
 			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
//                 mConnected = false;
//                 updateConnectionState(R.string.disconnected);
//                 invalidateOptionsMenu();
//                 clearUI();
 				Toast.makeText(MainActivity.this, "蓝牙连接失败，请检查串口称是否正常工作！", Toast.LENGTH_SHORT).show();
 			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
 				// Show all the supported services and characteristics on the user interface.
                 displayGattServices(mBluetoothLeService.getSupportedGattServices());

 			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
 				GlobalData.setWeight(intent.getFloatExtra(BluetoothLeService.EXTRA_DATA,0));
 			}else if(CHOSE_OBE_BLE.equals(action)){
 				mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
 				mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
 				Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
 				bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
 				onbind_service=true;
 			}
 		}
 	};
 	private static IntentFilter makeGattUpdateIntentFilter() {
 		final IntentFilter intentFilter = new IntentFilter();
 		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
 		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
 		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
 		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
 		intentFilter.addAction(CHOSE_OBE_BLE);
 		return intentFilter;
 	}
 	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
 	// In this sample, we populate the data structure that is bound to the ExpandableListView
 	// on the UI.
 	private void displayGattServices(List<BluetoothGattService> gattServices) {
 		if (gattServices == null) return;
 		String uuid = null;
 		String unknownServiceString = getResources().getString(R.string.unknown_service);
 		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
 		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
 		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
 				= new ArrayList<ArrayList<HashMap<String, String>>>();
 		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

 		// Loops through available GATT Services.
 		for (BluetoothGattService gattService : gattServices) {
 			HashMap<String, String> currentServiceData = new HashMap<String, String>();
 			uuid = gattService.getUuid().toString();
 			if(uuid.equals(TARGET_SERVICE_UUID)){
 				currentServiceData.put(
 						LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
 				currentServiceData.put(LIST_UUID, uuid);
 				gattServiceData.add(currentServiceData);

 				ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
 						new ArrayList<HashMap<String, String>>();
 				List<BluetoothGattCharacteristic> gattCharacteristics =
 						gattService.getCharacteristics();
 				ArrayList<BluetoothGattCharacteristic> charas =
 						new ArrayList<BluetoothGattCharacteristic>();

 				// Loops through available Characteristics.
 				for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
 					charas.add(gattCharacteristic);
 					HashMap<String, String> currentCharaData = new HashMap<String, String>();
 					uuid = gattCharacteristic.getUuid().toString();
 					//连接对应的gattCharacteristic
 					if(uuid.equals(TARGET_CHAACTERISTIC_UUID)){
 						final int charaProp = gattCharacteristic.getProperties();
 						if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
 							// If there is an active notification on a characteristic, clear
 							// it first so it doesn't update the data field on the user interface.
 							if (mNotifyCharacteristic != null) {
 								mBluetoothLeService.setCharacteristicNotification(
 										mNotifyCharacteristic, false);
 								mNotifyCharacteristic = null;
 							}
 							mBluetoothLeService.readCharacteristic(gattCharacteristic);
 						}
 						if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
 							mNotifyCharacteristic = gattCharacteristic;
 							mBluetoothLeService.setCharacteristicNotification(
 									gattCharacteristic, true);
 						}
 					}
 					currentCharaData.put(
 							LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
 					currentCharaData.put(LIST_UUID, uuid);
 					gattCharacteristicGroupData.add(currentCharaData);
 				}
 				mGattCharacteristics.add(charas);
 				gattCharacteristicData.add(gattCharacteristicGroupData);


 			}
 		}
 	}

}


