package cc.senguo.senguocashier;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import javax.microedition.khronos.opengles.GL;

/**
 * Created by skipjack on 16-8-4. get weight from blooth device
 */
public class GetWeightFromBluetooth extends Service {
    private static final String TAG="GetWeightFromBluetooth";
    private String connect_mac;//选择链接的蓝牙设备的MAC地址
    private boolean connected=false,connecting=false;
    private int connetTime=0;
    public boolean is_new_intent=false;
    BluetoothDevice mBluetoothDevice=null;
    BluetoothAdapter mBluetoothAdapter=null;
    @Override
    //Service时被调用
    public void onCreate()
    {
        Log.i(TAG, "Service onCreate--->");
        super.onCreate();
    }

    @Override
    //当调用者使用startService()方法启动Service时，该方法被调用
    public void onStart(Intent intent, int startId)
    {
        Log.i(TAG, "Service onStart--->");
        //从蓝牙设备获取数据
        boolean m=GlobalData.getConnected();
        if(GlobalData.getConnected()){
            is_new_intent=true;
        }else{
            is_new_intent=false;
        }
        GlobalData.setIs_new_intent(is_new_intent);
        Thread my_thread=new Thread(new Runnable(){
           @Override
            public void run(){
               GetWeightFromBluetooth();
           }
        });
        my_thread.start();
        super.onStart(intent, startId);

    }

    @Override
    //当Service不在使用时调用
    public void onDestroy()
    {
        Log.i(TAG, "Service onDestroy--->");
        super.onDestroy();
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        Log.v(TAG,"service unbind--->");
        return super.onUnbind(intent);
    }

    @Override
    //当使用startService()方法启动Service时，方法体内只需写return null
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.v(TAG, "service bind--->");
        IBinder result = null;
        if ( null == result ) result = new ServiceBinder() ;
        Toast.makeText(this, "onBind", Toast.LENGTH_LONG);
        return result;
    }

    //此方法是为了可以在Acitity中获得服务的实例
    class ServiceBinder extends Binder {
        public GetWeightFromBluetooth getService() {
            return GetWeightFromBluetooth.this;
        }
    }
    //从蓝牙设备获取数据
    public void GetWeightFromBluetooth(){
        BluetoothSocket socket=null;
        connect_mac=GlobalData.getConnect_mac();
        if(connect_mac==null){
            Log.i(TAG, "no device connected--->");
        }
        else{
            if(mBluetoothAdapter == null){
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            if(!mBluetoothAdapter.isEnabled()){
                mBluetoothAdapter.enable();
            }
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(connect_mac);
            mBluetoothAdapter.cancelDiscovery();
            Method m = null;
            try {
                m = mBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                socket = (BluetoothSocket) m.invoke(mBluetoothDevice, 1);
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //adapter.cancelDiscovery();
            while (!connected && connetTime <= 1) {
                if(connectDevice( socket)==-1){
                    return;
                }
            }
            if(!connected&&connetTime>1){
                // 添加链接失败的代码
                connetTime=0;
                sendBroadcastToActivity();
            }

        }
    }

    //跟蓝牙设备建立连接关系
    protected int connectDevice(BluetoothSocket socket) {
        boolean return_flag=false;//该变量用于判断是否是因为重新链接蓝牙设备而中断当前连接
        try {
            // 连接建立之前的先配对
            if (mBluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                Method creMethod = BluetoothDevice.class
                        .getMethod("createBond");
                Log.e("TAG", "开始配对");
                creMethod.invoke(mBluetoothDevice);
            } else {
            }
        } catch (Exception e) {
            // TODO: handle exception
            //DisplayMessage("无法配对！");
            e.printStackTrace();
        }
        mBluetoothAdapter.cancelDiscovery();
        try {
            socket.connect();
            connected=true;
            GlobalData.setConnected(connected);
            //DisplayMessage("连接成功!");
            sendBroadcastToActivity();
            //开线程读取字节数据
            if(socket!=null){
                InputStream is = socket.getInputStream();
                final byte[] bytes = new byte[7];
                byte[] tmp_bytes=new byte[7];
                byte[] single_byte=new byte[1];
                int get_byte=0;
                int normal_transfer=0;//添加代码让称开始的时候那一串无效的数据消失 检测到第一个换行
                while(( is.read(single_byte,0,single_byte.length)) > -1&&!GlobalData.getis_new_intent()) {
                    if(single_byte[0]==10){
                        normal_transfer++;
                    }
                    if(normal_transfer>0){
                        if(get_byte<bytes.length){
                            bytes[get_byte]=single_byte[0];
                            get_byte++;
                        }else{
                            if(Arrays.equals(tmp_bytes,bytes)){
                            }else{
                                //tmp_bytes=bytes; 这里我真是个大傻逼 		 数组赋值不能直接这样赋值
                                //数组赋值
                                for(int i=0;i<bytes.length;i++){
                                    tmp_bytes[i]=bytes[i];
                                }
                                float m_weight=0;
                                for(int i=0;i<bytes.length;i++){
                                    switch(i){
                                        case 2:
                                            if(bytes[i]==' ') break;
                                            m_weight+=(bytes[i]-'0')*10;
                                            break;
                                        case 3:
                                            m_weight+=bytes[i]-'0';
                                            break;
                                        case 4:
                                            m_weight+=(bytes[i]-'0')*0.1;
                                            break;
                                        case 5:
                                            m_weight+=(bytes[i]-'0')*0.01;
                                            break;
                                        case 6:
                                            m_weight+=(bytes[i]-'0')*0.001;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                                if(bytes[2]=='-'){
                                    m_weight=0;
                                }
                                //浮点数据长度太长 js只要小数点后三位数 此处做截取操作
                                String weight_str=m_weight+"",weight_str1;
                                int m_index=weight_str.indexOf(".");
                                if(m_index+4<=weight_str.length()-1){
                                    weight_str1=weight_str.substring(0,m_index+4);
                                }else{
                                    weight_str1=weight_str;
                                }
                                m_weight=Float.parseFloat(weight_str1);
                                GlobalData.setWeight(m_weight);

                            }
                            get_byte=1; //因为进入这里的时候已经从缓冲区中读取了一个数据
                            bytes[0]=single_byte[0];
                        }
                    }
                } // while
                //更换蓝牙连接的时候 称重数据置0
                try{
                    socket.close();
                    socket=null;
                }catch (IOException e_close){}
                is_new_intent=false;
                connected=false;
                GlobalData.setConnected(connected);
                GlobalData.setIs_new_intent(is_new_intent);
                GlobalData.setWeight(0);
                GlobalData.setWeight(0);
                return_flag=true;
            }//if
        } catch (IOException e_open) {
            // TODO: handle exception
            //DisplayMessage("连接失败！");
            try{
                socket.close();
            }catch (IOException e_close){}
            connetTime++;
            connected = false;
            GlobalData.setConnected(connected);
        }
        finally {
            connecting = false;
        }
        if(return_flag){
            return -1;
        }else{
            return 0;
        }
    }
    public void sendBroadcastToActivity() {
        Intent it = new Intent("cc.senguo.senguocashier.GetWeightFromBluetooth");
        it.putExtra("connect_status", this.connected);
        super.sendBroadcast(it);
    }
}
