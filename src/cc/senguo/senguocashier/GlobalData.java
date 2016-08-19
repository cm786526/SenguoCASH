package cc.senguo.senguocashier;

/**
 * Created by skipjack on 16-8-4.
 */
public class GlobalData {
    private static String[] devices_name;   //全局保存获取的配对蓝牙设备信息
    private static  String[] devices_mac;
    private static  float weight=0;     //保存获取的重量信息
    private static  String connect_mac; //选择链接的蓝牙的MAC
    private static boolean  connected=false; //判断蓝牙是否链接
    private static boolean is_new_intent=false; //判断是否重新链接蓝牙设备
    private static boolean have_click_print=false;//判断是否重复点击打印订单
    public static void setDevices_name(String[] m_device_name) {
       devices_name = m_device_name;
    }
public static void setHave_click_print(boolean m_have_click_print){
	have_click_print=m_have_click_print;
}
    public static void setDevices_mac(String[]m_device_mac){
        devices_mac=m_device_mac;
    }

    public static void setConnect_mac(String m_connect_mac){
        connect_mac=m_connect_mac;
    }

    public static void setWeight(float m_weight){
        weight=m_weight;
    }

    public static void setConnected(boolean m_connected){connected=m_connected;}
    public static void setIs_new_intent(boolean m_is_new_intent){is_new_intent=m_is_new_intent;}
    public static String[] getDevices_name(){
        return devices_name;
    }

    public static String[] getDevices_mac(){
        return devices_mac;
    }

    public static String   getConnect_mac(){
        return  connect_mac;
    }
    public static float getWeight(){
        return weight;
    }
    public static boolean getConnected(){return connected;}
    public static boolean getis_new_intent(){return is_new_intent;}
    public static boolean getHave_click_print(){return have_click_print;}
}
