package cc.senguo.senguocashier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import cc.senguo.senguocashier.R;


/**
 * Description:
 * <br/>site: <a href="http://www.crazyit.org">crazyit.org</a>
 * <br/>Copyright (C), 2001-2014, Yeeku.H.Lee
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:
 * <br/>Date:
 * @author  Yeeku.H.Lee kongyeeku@163.com
 * @version  1.0
 */
public class SDFileExplorer extends Activity
{
	final String RootDirConst="/";
	final String TAG="SDFileExplorer";
	final String OpenDirConst="/mnt/usb_storage";
	final String OpenDirConst2="/mnt/";
	private Intent intent;
	ListView listView;
	TextView textView;
	static int FileType=0;
	// ��¼��ǰ�ĸ��ļ���
	File currentParent;
	// ��¼��ǰ·���µ������ļ����ļ�����
	File[] currentFiles;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		intent = getIntent();
		// ��ȡ�г�ȫ���ļ���ListView
		listView = (ListView) findViewById(R.id.list);
		textView = (TextView) findViewById(R.id.path);
		// ��ȡϵͳ��SD����Ŀ¼
		File root = new File(OpenDirConst);
		// ��� SD������
			//if (root.exists())
		File[] tmp = root.listFiles();
		if (tmp != null && tmp.length != 0)
		{
			currentParent = root;
			currentFiles = root.listFiles();
			// ʹ�õ�ǰĿ¼�µ�ȫ���ļ����ļ��������ListView
			inflateListView(currentFiles);	

		}
		else
		{
			root = new File(OpenDirConst2);
			currentParent = root;
			currentFiles = root.listFiles();
			// ʹ�õ�ǰĿ¼�µ�ȫ���ļ����ļ��������ListView
			inflateListView(currentFiles);			
		}
		// ΪListView���б���ĵ����¼��󶨼�����
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			private FileInputStream is;
			

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
				int position, long id)
			{
				// �û��������ļ���ֱ�ӷ��أ������κδ���
				if (currentFiles[position].isFile())
				{
					Log.d(TAG," file name :" +currentFiles[position].getName());
					try
					{
						Log.d(TAG," file name :" +currentFiles[position].getCanonicalPath());
						//FileInputStream is = openFileInput(currentFiles[position].getCanonicalPath());
						File file = new File(currentFiles[position].getCanonicalPath());
						is = new FileInputStream(file);
						ConsoleActivity.BinFile=file;
						int count = is.available();
						Log.d(TAG," file name :" +count);

						if(SDFileExplorer.FileType==0)
						{
							if(count >100000)
							{
								return;
							}	
							
							String strTemp=currentFiles[position].getName();
							
							int i = strTemp.indexOf(".bin");
							if (i == -1) {
								return;
							}	
							i = strTemp.indexOf("HDX");
							if (i == -1) {
			
								return;
							}	
								
						}
						else
						{
							
							if(count <100000)
							{
								return;
							}	
							String strTemp=currentFiles[position].getName();
							
							int i = strTemp.indexOf(".bin");
							if (i == -1) {
			 ;
								return;
							}
							i = strTemp.indexOf("ziku");
							if (i == -1) {
			
								return;
							}
						}
					 
						/*byte[] b = new byte[count];
						is.read(b);
						//get crc 
						byte []byteNum =new byte[4];
						Get_Buf_Sum(b,count,byteNum);// 17	01 7E 00   CRC
						Log.e("quck2", "crc0  "+ String.format("0x%02x", byteNum[0] )  );	
						Log.e("quck2", "crc1  "+ String.format("0x%02x", byteNum[1] )	);	
						Log.e("quck2", "crc2  "+ String.format("0x%02x", byteNum[2] )	);	
						Log.e("quck2", "crc3  "+ String.format("0x%02x", byteNum[3] )  );*/	
						SDFileExplorer.this.setResult(1, intent);
						SDFileExplorer.this.finish();
						
						
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;	
				}
				// ��ȡ�û�������ļ����µ������ļ�
				File[] tmp = currentFiles[position].listFiles();
				if (tmp == null || tmp.length == 0)
				{
					
				
					Log.d(TAG," file name :" +currentFiles[position].getName());
					Toast.makeText(SDFileExplorer.this
						, "��ǰ·�����ɷ��ʻ��·����û���ļ�",
						Toast.LENGTH_SHORT).show();
				}
				else
				{
					// ��ȡ�û��������б����Ӧ���ļ��У���Ϊ��ǰ�ĸ��ļ���
					currentParent = currentFiles[position]; //��
					// ���浱ǰ�ĸ��ļ����ڵ�ȫ���ļ����ļ���
					currentFiles = tmp;
					// �ٴθ���ListView
					inflateListView(currentFiles);
				}
			}
		});
		// ��ȡ��һ��Ŀ¼�İ�ť
		Button parent = (Button) findViewById(R.id.parent);
		parent.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View source)
			{
				try
				{
					if (!currentParent.getCanonicalPath()
						.equals(RootDirConst))
					{
						// ��ȡ��һ��Ŀ¼
						currentParent = currentParent.getParentFile();
						// �г���ǰĿ¼�������ļ�
						currentFiles = currentParent.listFiles();
						// �ٴθ���ListView
						inflateListView(currentFiles);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
	}
	// 4�ֽ����?
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

	private void inflateListView(File[] files) //��
	{
		// ����һ��List���ϣ�List���ϵ�Ԫ����Map
		List<Map<String, Object>> listItems = 
			new ArrayList<Map<String, Object>>();
		for (int i = 0; i < files.length; i++)
		{
			Map<String, Object> listItem = 
				new HashMap<String, Object>();
			// �����ǰFile���ļ��У�ʹ��folderͼ�ꣻ����ʹ��fileͼ��
			if (files[i].isDirectory())
			{
				listItem.put("icon", R.drawable.folder);
			}
			else
			{
				listItem.put("icon", R.drawable.file);
			}
			listItem.put("fileName", files[i].getName());
			// ���List��
			listItems.add(listItem);
		}
		// ����һ��SimpleAdapter
		SimpleAdapter simpleAdapter = new SimpleAdapter(this
			, listItems, R.layout.line
			, new String[]{ "icon", "fileName" }
			, new int[]{R.id.icon, R.id.file_name });
		// ΪListView����Adapter
		listView.setAdapter(simpleAdapter);
		try
		{
			textView.setText("��ǰ·��Ϊ��" 
				+ currentParent.getCanonicalPath());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}