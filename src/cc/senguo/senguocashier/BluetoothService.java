package cc.senguo.senguocashier;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Iterator;
import java.util.Set;

/**
 * Created by skipjack on 16-8-4.  this is used for blooth in order to get weight from blooth balance
 */
public class BluetoothService extends Service {
    private static final String TAG="BluetoothService";

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
        //打开蓝牙
        OpenBluetooth();
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
    //当使用startService()方法启动Service时，方法体内只需写return null
    public IBinder onBind(Intent intent)
    {
        return null;
    }
    //打开蓝牙设备
    public void OpenBluetooth(){
        if(mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
        Set<BluetoothDevice> all_devices = mBluetoothAdapter.getBondedDevices();
        int m_local=all_devices.size();
        String [] devices_name=new String[m_local];
        String [] devices_mac =new String[m_local];
        for(int i=0;i<m_local;i++){
            devices_name[i]="";
            devices_mac[i]="";
        }
        if (m_local>0) {
            int j=0;
            for(Iterator<BluetoothDevice> iterator = all_devices.iterator(); iterator.hasNext();){
                BluetoothDevice bluetoothDevice=(BluetoothDevice)iterator.next();
                devices_name[j]=bluetoothDevice.getName();
                devices_mac[j]= bluetoothDevice.getAddress();
                j++;
            }
        }
        GlobalData.setDevices_name(devices_name);
        GlobalData.setDevices_mac(devices_mac);
        Intent it = new Intent("cc.senguo.senguocashier.BluetoothService");
        super.sendBroadcast(it);
    }
}
