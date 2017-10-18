package com.espressif.iot.ui.pingpang;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.espressif.iot.R;
import com.espressif.iot.base.api.EspBaseApiUtil;
import com.espressif.iot.db.IOTApDBManager;
import com.espressif.iot.object.db.IApDB;
import com.espressif.iot.ui.main.EspActivityAbs;
import com.espressif.iot.user.IEspUser;
import com.google.zxing.qrcode.ui.ShareCaptureActivity;

public class PingpangEspActivity extends EspActivityAbs implements OnCheckedChangeListener, OnClickListener,
    OnMenuItemClickListener
{
    private IOTApDBManager mIOTApDBManager;
    
    private WifiManager mWifiManager;
    private LocalBroadcastManager mBraodcastManager;
    
    private static final String ESPTOUCH_VERSION = "v0.3.4.3";
    
    
    private IEspUser mUser;
    
    private static final int POPUPMENU_ID_GET_SHARE = 1;
    private static final int POPUPMENU_ID_SOFTAP_CONFIGURE = 2;
    
    private List<String> mEsptouchDeviceBssidList = new ArrayList<String>();
    private List<String> mRegisteredDeviceBssidList = new ArrayList<String>();
    private List<String> mRegisteredDeviceNameList = new ArrayList<String>();
    
    private static final int REQUEST_SOFTAP_CONFIGURE = 10;
    private static final int REQUEST_GET_SHARED = 12;
    private ImageView myImage;
    private static final String IMAGEVIEW_TAG = "The Pingpang";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.esp_pingpang);
        myImage = (ImageView)findViewById(R.id.image);
	    // Sets the tag
	    myImage.setTag(IMAGEVIEW_TAG);
	    
	    // set the listener to the dragging data
	    myImage.setOnLongClickListener(new MyClickListener());
	   
	    findViewById(R.id.toplinear).setOnDragListener(new MyDragListener());
	    findViewById(R.id.bottomlinear).setOnDragListener(new MyDragListener());
    }

	private final class MyClickListener implements OnLongClickListener {

	    // called when the item is long-clicked
		@Override
		public boolean onLongClick(View view) {
		// TODO Auto-generated method stub
		
			// create it from the object's tag
			ClipData.Item item = new ClipData.Item((CharSequence)view.getTag());

	        String[] mimeTypes = { ClipDescription.MIMETYPE_TEXT_PLAIN };
	        ClipData data = new ClipData(view.getTag().toString(), mimeTypes, item);
	        DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
	   
	        view.startDrag( data, //data to be dragged
	        				shadowBuilder, //drag shadow
	        				view, //local data about the drag and drop operation
	        				0   //no needed flags
	        			  );
	        
	        
	        view.setVisibility(View.INVISIBLE);
	        return true;
		}	
	}

	class MyDragListener implements OnDragListener {
		Drawable normalShape = getResources().getDrawable(R.drawable.normal_shape);
		Drawable targetShape = getResources().getDrawable(R.drawable.target_shape);

		@Override
		public boolean onDrag(View v, DragEvent event) {
	  
			// Handles each of the expected events
		    switch (event.getAction()) {
		    
		    //signal for the start of a drag and drop operation.
		    case DragEvent.ACTION_DRAG_STARTED:
		        // do nothing
		        break;
		        
		    //the drag point has entered the bounding box of the View
		    case DragEvent.ACTION_DRAG_ENTERED:
		        v.setBackground(targetShape);	//change the shape of the view
		        break;
		        
		    //the user has moved the drag shadow outside the bounding box of the View
		    case DragEvent.ACTION_DRAG_EXITED:
		        v.setBackground(normalShape);	//change the shape of the view back to normal
		        break;
		        
		    //drag shadow has been released,the drag point is within the bounding box of the View
		    case DragEvent.ACTION_DROP:
		        // if the view is the bottomlinear, we accept the drag item
		    	  if(v == findViewById(R.id.bottomlinear)) {
		    		  View view = (View) event.getLocalState();
		    		  ViewGroup viewgroup = (ViewGroup) view.getParent();
		    		  viewgroup.removeView(view);
		        
		    		  //change the text
		    		  TextView text = (TextView) v.findViewById(R.id.text);
		    		  text.setText("The item is dropped");
		           
		    		  LinearLayout containView = (LinearLayout) v;
		    		  containView.addView(view);
		    		  view.setVisibility(View.VISIBLE);
		    	  } else {
		    		  View view = (View) event.getLocalState();
		    		  view.setVisibility(View.VISIBLE);
		    		  Context context = getApplicationContext();
		    		  Toast.makeText(context, "You can't drop the image here", 
                                                 Toast.LENGTH_LONG).show();
		    		  break;
		    	   }
		    	  break;
		    	  
		    //the drag and drop operation has concluded.
		    case DragEvent.ACTION_DRAG_ENDED:
		        v.setBackground(normalShape);	//go back to normal shape
		    
		    default:
		        break;
		    }
		    return true;
		}
	}
    @Override
    protected void onResume()
    {
        super.onResume();
        
        IntentFilter wifiFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mWifiReceiver, wifiFilter);
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        
        unregisterReceiver(mWifiReceiver);
    }
    
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        
    }
    
    @Override
    protected void onTitleRightIconClick(View rightIcon)
    {
        PopupMenu popupMenu = new PopupMenu(this, rightIcon);
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, POPUPMENU_ID_GET_SHARE, 0, R.string.esp_esptouch_menu_get_share);
        menu.add(Menu.NONE, POPUPMENU_ID_SOFTAP_CONFIGURE, 0, R.string.esp_esptouch_menu_softap_configure);
//        menu.add(Menu.NONE, POPUPMENU_ID_BROWSER_CONFIGURE, 0, R.string.esp_esptouch_menu_browser_configure);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }
    
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
    }

    @Override
    public void onClick(View v)
    {
    }
    
    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        switch(item.getItemId())
        {
            case POPUPMENU_ID_GET_SHARE:
                startActivityForResult(new Intent(this, ShareCaptureActivity.class), REQUEST_GET_SHARED);
                return true;
//            case POPUPMENU_ID_SOFTAP_CONFIGURE:
//                startActivityForResult(new Intent(this, DeviceSoftAPConfigureActivity.class), REQUEST_SOFTAP_CONFIGURE);
//                return true;
        }
        return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case REQUEST_SOFTAP_CONFIGURE:
                if (resultCode == RESULT_OK) {
                    finish();
                }
                break;
            case REQUEST_GET_SHARED:
                if (resultCode == RESULT_OK) {
                    finish();
                }
                break;
        }
    }
    
    private String getConnectionBssid()
    {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null)
        {
            return wifiInfo.getBSSID();
        }
        
        return null;
    }
    
    private String getCurrentWifiPassword(String currentBssid)
    {
        List<IApDB> apDBList = mIOTApDBManager.getAllApDBList();
        for (IApDB ap : apDBList)
        {
            if (ap.getBssid().equals(currentBssid))
            {
                return ap.getPassword();
            }
        }
        
        return "";
    }
    
    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            boolean isWifiConnected = EspBaseApiUtil.isWifiConnected();
        }
        
    };
    
}
