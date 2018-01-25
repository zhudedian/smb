package com.ider.smb.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ider.smb.R;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/************************************
 * GridView adapter to show the list of applications and shortcuts  GalleryAdapter
 ************************************/
public class NormalListAdapter extends ArrayAdapter<FileInfo>{
	private final String TAG = "NormalListAdapter";
	private final LayoutInflater mInflater;
	private ArrayList<FileInfo> mListFile;
	
	private Resources mResources;
	
	public NormalListAdapter(Context context, ArrayList<FileInfo> files) {
		super(context, 0, files);
		mInflater = LayoutInflater.from(context);
		mListFile = files;
		mResources = context.getResources();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final FileInfo info = getItem(position);
		if (convertView == null) {
			//convertView = mInflater.inflate(R.layout.folder_listview_adapter, parent, false);
			convertView = mInflater.inflate(R.layout.normal_adapter, null);
		}   
		//-----------------------------
        final ImageView image = (ImageView) convertView.findViewById(R.id.nor_image);
		final TextView text = (TextView) convertView.findViewById(R.id.nor_text);
		final TextView text_right = (TextView) convertView.findViewById(R.id.nor_right_text);
        final TextView text_choice = (TextView) convertView.findViewById(R.id.nor_text_choice);
        image.setImageDrawable(info.mIcon);

//		text.setTextSize(TypedValue.COMPLEX_UNIT_PX,AutoSize.getInstance().getTextSize(0.05f));
		//text_right.setTextSize(TypedValue.COMPLEX_UNIT_PX,AutoSize.getInstance().getTextSize(0.03f));
		//text_choice.setTextSize(TypedValue.COMPLEX_UNIT_PX,AutoSize.getInstance().getTextSize(0.03f));

		image.setLeft(AutoSize.getInstance().getMargin(0.02f));
		text.setLeft(AutoSize.getInstance().getMargin(0.02f));
//		Log.i("NormalListAdapter",info.mFile.getPath());
        if(info.mFile.getPath().startsWith(EnumConstent.mDirSmb)){
        	text.setText(info.mDescription);
		}else {
			text.setText(info.mFile.getName());
		}

        if(info.mIsSelected){
        	 if(info.mFile.getPath().startsWith(EnumConstent.mDirSmb)){
		 		text_choice.setText(info.mDescription);
		 	}else
            	text_choice.setText(info.mFile.getName());
        }else{
        	text_choice.setText(null);
        }
        
        if(info.mFile.getPath().startsWith(EnumConstent.mDirSmb)){
	 		text_right.setText(null);
			int height = AutoSize.getInstance().getHeight()/8;
			convertView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height));
	 		return convertView;
	 	}
        
        String temp_right = null;
        if(info.mIsDir){
        	temp_right = mResources.getString(R.string.str_folder);
        }else{
        	long temp_size = info.mFile.length();
        	temp_right = new String("");
        	temp_right += change_Long_to_String(temp_size);
        }
        
        temp_right += " | ";        
        long lastModified = info.mFile.lastModified();
        temp_right += change_long_to_time(lastModified);
        
        //temp_right += " | ";        
        //temp_right += getAttribute(info.mFile);
        
        if(info.mFile.getPath().endsWith(EnumConstent.mDirSmb)){
        	text_right.setText("");
        }else{        	
        	text_right.setText(temp_right);
        }

		int height = AutoSize.getInstance().getHeight()/8;
		convertView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height));

		return convertView;
	}	
	
	public String change_Long_to_String(long temp_change){
		String temp_str = new String("");
		DecimalFormat df = new DecimalFormat("########.00"); 
		
		if(temp_change >= 1024){
			float i =  temp_change / 1024f;
			if(i >= 1024f){	
				float j = i / 1024f;
				if(j >= 1024f){
					float k = j / 1024f;
                                        temp_str += df.format(k);
                                        temp_str += " G";
				}else{
					temp_str += df.format(j);
					temp_str += " M";
				}
			}else{
				temp_str += df.format(i);
				temp_str += " K";
			}
		}else{
			temp_str += temp_change;
			temp_str += " B";
		}
		return temp_str;
	}
	
	public String change_long_to_time(long temp_time){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		String mDateTime1=formatter.format(temp_time); 
		return mDateTime1;
	}
	
	public String getAttribute(File file){
		String temp_Attr = new String("");
		if(file.isDirectory()){
			temp_Attr += "d";
		}else{
			temp_Attr +="-";
		}
		
		if(file.canRead()){
			temp_Attr += "r";
		}else{
			temp_Attr +="-";
		}
		
		if(file.canWrite()){
			temp_Attr += "w";
		}else{
			temp_Attr +="-";
		}
		
		return temp_Attr;
	}
}
