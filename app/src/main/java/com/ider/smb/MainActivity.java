package com.ider.smb;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ider.smb.util.AutoSize;
import com.ider.smb.util.CifsExplorerProxy;
import com.ider.smb.util.CopyFileUtils;
import com.ider.smb.util.EnumConstent;
import com.ider.smb.util.FileControl;
import com.ider.smb.util.FileInfo;
import com.ider.smb.util.NormalListAdapter;
import com.ider.smb.util.Path;
import com.ider.smb.util.SmbUtil;
import com.ider.smb.util.ZoomAnimation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbFile;



public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    public ArrayList<FileInfo> folder_array;
    public ProgressDialog mWaitDialog;
    public String mCurrnetSmb = null;
    public FileControl mFileControl = null;
    public ListView mContentList = null;
    public String mDefaultPath = "first_path";
    private ArrayList<FileInfo> mSmbinfoList = new ArrayList<FileInfo>();
    private Dialog mScanProgressDialog;
    private View scanProgress;
    public SmbUtil mSmbUtil;
    private NormalListAdapter mTempListAdapter = null;
    public int mSelectedPosition=0;
    private static final int MSG_EXPAND_GROUP = 0;
    private static final int MSG_REFRESH_ADAPTER = 1;
    public FileInfo mCurListItem = null;
    private String mCurrentDir = null;
    public ProgressDialog openingDialog = null;
    public boolean isItemClick = false;
    private String mCurSmbUsername = null;
    private String mCurSmbPassword = null;
    private boolean mCurSmbAnonymous = true;
    public String fill_path = null;
    public boolean openwiththread = false;
    public static boolean mIsLastDirectory = false;
    private boolean mEnablePaste = false;
    private boolean mEnableCopy = false;
    private boolean mEnableMove = false;
    private boolean mEnableRename = false;
    private boolean mEnableSmbEdit = false;
    private boolean mEnableCopyInBg = false;
    private boolean mEnableLeft = true;
    private CifsExplorerProxy mCifsProxy;
    private ArrayList<Path> mSavePath = null;
    private int mPitSavePath = 0;
    private CopyFileUtils mCopyFileUtils = null;
    private Dialog mDialogCopy;
    private View myCopyView;
    public PowerManager mPowerManager;
    public PowerManager.WakeLock mWakeLock;
    ProgressDialog  GetFileCountDialog = null;
    private ArrayList<FileInfo> mSelectedPathList = null;
    private ArrayList<FileInfo> mSourcePathList = null;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCifsProxy = new CifsExplorerProxy(this);
        mCifsProxy.onCreate();
        mContentList = (ListView)findViewById(R.id.list_view);
        mSmbUtil = new SmbUtil(MainActivity.this,mHandler);
        mFileControl = new FileControl(this, null, mContentList);
        mCopyFileUtils = new CopyFileUtils();
        mSavePath = new ArrayList<Path>();
        mSavePath.add(new Path(mDefaultPath,"/mnt/internal_sd"));
        mSelectedPathList = new ArrayList<FileInfo>();
        try{
            mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,TAG);
        }catch (Exception e){
            e.printStackTrace();
        }
        verifyStoragePermissions(this);
        if(openDir(EnumConstent.mDirSmb))
        {
            String devicePath = mSavePath.get(mSavePath.size()-1).getPath();
            String dir = mSavePath.get(mSavePath.size()-1).getPathDirection();
            if(!(EnumConstent.mDirSmb.equals(devicePath) && dir.equals("smb")))
            {
                mSavePath.add(new Path(EnumConstent.mDirSmb,"smb"));
            }

            mPitSavePath = mSavePath.size() - 1;
            Log.d(TAG,"mDeviceListListener,mPitSavePath = "+mPitSavePath);
//            enableButton(true,true);
        }else{
            clearContentList();
            mSavePath.clear();
            mSavePath.add(new Path(mDefaultPath,""));
            mPitSavePath = mSavePath.size()-1;
//            enableButton(true, true);
        }


        mContentList.setOnItemClickListener(mListItemListener);
    }

    public static void verifyStoragePermissions(Activity activity) {
                 // Check if we have write permission
                int permission = ActivityCompat.checkSelfPermission(activity,
                               Manifest.permission.WRITE_EXTERNAL_STORAGE);

                 if (permission != PackageManager.PERMISSION_GRANTED) {
                         // We don't have permission so prompt the user
                         ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                                       REQUEST_EXTERNAL_STORAGE);
                    }
    }
    AdapterView.OnItemClickListener mListItemListener = new AdapterView.OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {

            if(position >= mFileControl.folder_array.size())
                return;

            isItemClick = true;

            FileInfo tmpFileInfo = mFileControl.folder_array.get(position);
            mCurListItem = tmpFileInfo;

//            enableButton(true,true);

            if(tmpFileInfo.mIsDir){
//                mEnableLeft = true;
                String path = tmpFileInfo.mFile.getPath();
                mCurrentDir = tmpFileInfo.mFile.getParent();
                Log.i("MainActivity","onItemClick open      File:" + path);
                Log.i("MainActivity","onItemClick open Directory:" + mCurrentDir);
                mSelectedPosition=0;
                if(openDir(path)){
                    if (!path.equals(mSavePath.get(mSavePath.size()-1).getPath())
                            || !mCurrentDir.equals(mSavePath.get(mSavePath.size()-1).getPathDirection())){
                        mSavePath.add(new Path(path,mCurrentDir));
                        mPitSavePath = mSavePath.size() - 1;
                    }else{
                        mPitSavePath = mSavePath.size() - 1;
                    }

                }else if(!path.startsWith(EnumConstent.mDirSmb+"/")&&!path.startsWith(EnumConstent.mDirSmbMoutPoint)){
                    clearContentList();
                    mSavePath.clear();
                    mSavePath.add(new Path(mDefaultPath,""));
                    mPitSavePath = mSavePath.size()-1;
                }
//                enableButton(true,true);
            }else{
                if((tmpFileInfo.mFileType!=null)&&tmpFileInfo.mFileType.equals("..")){
                    Log.i("MainActivity","File(..), so Back to Last Folder");
                    dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK));
                    return ;
                }
                String path = tmpFileInfo.mFile.getPath();
                if(path.startsWith(EnumConstent.mDirSmbMoutPoint)){
                    Log.i("MainActivity","post wait dialog");
                    showWaitDialog();

                    new Thread(new smbfileThreadRun(tmpFileInfo, position)).start();
                    return;
                }

                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);


                if(tmpFileInfo.mFileType.equals("image/*") || tmpFileInfo.mFileType.equals("audio/*")
                        || tmpFileInfo.mFileType.equals("video/*")){
                    Uri tmpUri = null;
                    if(tmpFileInfo.mUri!=null)
                        tmpUri = Uri.parse(tmpFileInfo.mUri);
                    else
                        tmpUri = getFileUri(tmpFileInfo.mFile, tmpFileInfo.mFileType);
                    if(tmpUri != null)
                        intent.setDataAndType(tmpUri, tmpFileInfo.mFileType);
                    else
                        intent.setDataAndType(Uri.fromFile(tmpFileInfo.mFile),tmpFileInfo.mFileType);
                }else
                    intent.setDataAndType(Uri.fromFile(tmpFileInfo.mFile),tmpFileInfo.mFileType);

                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
//                    Toast.makeText(MainActivity.this, getString(R.string.noapp), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    public void clearContentList()
    {
        mContentList.setAdapter(null);
        mContentList.setBackgroundDrawable(null);
        mContentList.invalidate();
        mPitSavePath --;
        if (mPitSavePath < 0)
            mPitSavePath =0;
//        enableButton(true,true);
//        setCurrentFileDir("");
    }
    public boolean openDir(String path){
        Log.i("MainActivity","openDir() path=" + path + "; dir="+mCurrentDir);

        FileControl.is_enable_fill = true;
        FileControl.is_finish_fill = false;

        if(path == null){
            return false;
        }

        if (path.equals(mDefaultPath)){
            return false;
        }

        if(path.equals(EnumConstent.mDirSmb)){
            mCifsProxy.getCifsContent(path);
            return true;
        }


        if(path.startsWith(EnumConstent.mDirSmb+"/")||path.startsWith(EnumConstent.mDirSmbMoutPoint)){
            Log.i("MainActivity","openDir() ->showWaitDialog()");
            showWaitDialog();

            new Thread(new smbThreadRun(mCurListItem, path)).start();
            return false;
        }

        fill_path = path;
        mFillHandler.removeCallbacks(mFillRun);

        File files = new File(path);
        if((files == null) || !files.exists() || !files.canRead() || (files.list() == null)){
            Toast.makeText(this, getResources().getString(R.string.read_error)+"\n"+
                    getResources().getString(R.string.read_denied), Toast.LENGTH_SHORT).show();

            FileControl.is_enable_fill = true;
            FileControl.is_finish_fill = true;

            Message msg = new Message();
            msg.what = EnumConstent.MSG_DLG_HIDE;
            mHandler.sendMessage(msg);
            return false;
        }
        long file_count = files.list().length;
        Log.i("MainActivity","openDir(), file_count=" + file_count);
        if(file_count > 1500){
            openwiththread = true;
            if(openingDialog == null){
                openingDialog = new ProgressDialog(MainActivity.this);
                openingDialog.setTitle(R.string.str_openingtitle);
                openingDialog.setMessage(getString(R.string.str_openingcontext));
                openingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // TODO Auto-generated method stub
                        mFileControl.is_enable_fill = false;
                    }
                });
                openingDialog.show();
            }else{
                openingDialog.show();
            }
        }else{
            openwiththread = false;
        }
        mOpenHandler.postDelayed(mOpeningRun, 200);
        mFillHandler.postDelayed(mFillRun, 300);
        return true;
    }
    private void showCopyDialog(){
        if(mDialogCopy == null){
            LayoutInflater factory=LayoutInflater.from(MainActivity.this);
            myCopyView=factory.inflate(R.layout.copy_dialog,null);
            mDialogCopy = new Dialog(this, R.style.MyDialog);


            int cell_w = AutoSize.getInstance().getScaleWidth(0.4f);
            int cell_h = AutoSize.getInstance().getScaleHeight(0.08f);
            LinearLayout.LayoutParams xx2 = new LinearLayout.LayoutParams(cell_w,cell_h);
            LinearLayout lt_source = (LinearLayout)myCopyView.findViewById(R.id.layout_source);
            LinearLayout lt_target = (LinearLayout)myCopyView.findViewById(R.id.layout_target);
            LinearLayout one_percent = (LinearLayout)myCopyView.findViewById(R.id.layout_one_percent);
            LinearLayout all_percent = (LinearLayout)myCopyView.findViewById(R.id.layout_all_percent);
            lt_source.setLayoutParams(xx2);
            lt_target.setLayoutParams(xx2);
            one_percent.setLayoutParams(xx2);
            all_percent.setLayoutParams(xx2);

            ((TextView)myCopyView.findViewById(R.id.source_text_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, AutoSize.getInstance().getTextSize(0.03f));
            ((TextView)myCopyView.findViewById(R.id.source_Text)).setTextSize(TypedValue.COMPLEX_UNIT_PX, AutoSize.getInstance().getTextSize(0.03f));
            ((TextView)myCopyView.findViewById(R.id.target_text_title)).setTextSize(TypedValue.COMPLEX_UNIT_PX, AutoSize.getInstance().getTextSize(0.03f));
            ((TextView)myCopyView.findViewById(R.id.target_Text)).setTextSize(TypedValue.COMPLEX_UNIT_PX, AutoSize.getInstance().getTextSize(0.03f));

            ((Button)myCopyView.findViewById(R.id.but_stop_copy)).setOnClickListener(new View.OnClickListener(){
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    if(mDialogCopy != null){
                        //CopyFileUtils.is_copy_finish = true;
                        CopyFileUtils.is_enable_copy = false;
                        UnLockScreen();
                    }
                }
            });
            mDialogCopy.setContentView(myCopyView);
            mDialogCopy.show();
        }else{
            mDialogCopy.show();
        }
    }
    private void UnLockScreen(){
        if (mWakeLock != null) {
            try {
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                    mWakeLock.setReferenceCounted(false);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public void showWaitDialog(){
        Log.i("MainActivity","show wait dialog.......");

        mWaitDialog = new ProgressDialog(MainActivity.this);
        mWaitDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mWaitDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){

            @Override
            public void onCancel(DialogInterface dialog) {
                // TODO Auto-generated method stub
                mFileControl.is_enable_fill = false;
                dialog.dismiss();
            }

        });
        mWaitDialog.setMessage(getResources().getString(R.string.openning));
        mWaitDialog.setCancelable(true);

        mWaitDialog.show();

    }
    public Uri getFileUri(File tmp_file, String tmp_type){
        String path = tmp_file.getPath();
        String name = tmp_file.getName();
        if(tmp_type.equals("image/*")){
            ContentResolver resolver = getContentResolver();
            String[] audiocols = new String[] {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.TITLE
            };
            Log.i("MainActivity","getFileUri() path = " + path);
            StringBuilder where = new StringBuilder();
            where.append(MediaStore.Images.Media.DATA + "=" + "'" + addspecialchar(path) + "'");
            Cursor cur = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    audiocols,
                    where.toString(), null, null);
            if(cur != null && cur.moveToFirst()){
                int Idx = cur.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                String id = cur.getString(Idx);
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            }
        }else if(tmp_type.equals("audio/*")){
            if(path.endsWith(".3gpp")){
                ContentResolver resolver = getContentResolver();
                String[] audiocols = new String[] {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.MIME_TYPE
                };
                if (path.startsWith("mnt/")){
                    path = "/"+path;
                }
                Log.i("MainActivity","getFileUri() path = " + path + " path.lenght:" + path.length() + " trimlength:" + path.trim().length());
                StringBuilder where = new StringBuilder();
                where.append(MediaStore.Audio.Media.DATA + "=" + "'" + path + "'");

                Cursor cur = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        audiocols,
                        MediaStore.Audio.Media.DATA + "=?", new String[]{path.trim()}, null);


                if(cur != null && cur.moveToNext()){
                    int Idx = cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    String id = cur.getString(Idx);

                    int dataIdx = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    String data = cur.getString(dataIdx);
                    return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                }
                return null;
            }
            ContentResolver resolver = getContentResolver();
            String[] audiocols = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE
            };
            if (path.startsWith("mnt/")){
                path = "/"+path;
            }
            Log.i("MainActivity","getFileUri path = " + path);
            StringBuilder where = new StringBuilder();
            where.append(MediaStore.Audio.Media.DATA + "=" + "'" + addspecialchar(path) + "'");
            Cursor cur = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    audiocols,
                    where.toString(), null, null);
            if(cur != null && cur.moveToFirst()){
                int Idx = cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                String id = cur.getString(Idx);
                return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            }
        }else if(tmp_type.equals("video/*")){
            ContentResolver resolver = getContentResolver();
            String[] audiocols = new String[] {
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media.TITLE
            };
            Log.i("MainActivity","getFileUri path = " + path);
            StringBuilder where = new StringBuilder();
            if(where != null)
            {
                where.append(MediaStore.Video.Media.DATA + "=" + "'" + addspecialchar(path) + "'");
                Cursor cur = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        audiocols,
                        where.toString(), null, null);
                if(cur != null && cur.moveToFirst()){
                    int Idx = cur.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                    String id = cur.getString(Idx);
                    return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                }
            }
        }
        return null;
    }
    public String addspecialchar(String mstring){
        String ret = mstring;
        char[] tmpchar = null;
        if(ret.contains("'")){
            int csize = 0;
            int i = 0;
            for(i = 0; i < ret.length(); i ++){
                if(ret.charAt(i) == '\''){
                    csize ++;
                }
            }
            int len = ret.length() + (csize * 1);
            tmpchar = new char[len];
            int j = 0;
            for(i = 0; i < ret.length(); i ++){
                if(ret.charAt(i) == '\''){
                    tmpchar[j] = '\'';
                    j ++;
                }
                tmpchar[j] = ret.charAt(i);
                j ++;
            }
            ret = String.valueOf(tmpchar);
        }
        return ret;
    }
    public void getCifsContent(String path){
        FileControl.is_first_path = false;
        if (path.equals(EnumConstent.mDirSmb)){
            mFileControl.currently_path = path;
            mFileControl.currently_parent = mDefaultPath;

            synchronized (mSmbinfoList) {
                //cifs dirctionary is empty
                if (mSmbinfoList.size()<1){
                    searchSmb();
                }else{
                    mFileControl.folder_array = mSmbinfoList;
                    mHandler.postDelayed(mFillSmb, 300);
                }
            }
        }
    }
    Runnable mFillSmb = new Runnable(){
        public void run() {
            setFileAdapter(false, false);
        }
    };
    public void setFileAdapter(boolean is_animation, boolean is_left)
    {
        mTempListAdapter = new NormalListAdapter(this, new ArrayList<FileInfo>(mFileControl.folder_array));
        Log.i("MainActivity","mFileControl.folder_array.size"+mFileControl.folder_array.size());
        mContentList.setAdapter(mTempListAdapter);
        mTempListAdapter.notifyDataSetChanged();
        mContentList.setSelection(mSelectedPosition >= mTempListAdapter.getCount()
                ? 0 : mSelectedPosition);
        mContentList.requestFocus();
//        setCurrentFileDir(mFileControl.get_currently_path());
    }

    public void searchSmb(){
        mScanProgressDialog = new Dialog(MainActivity.this,R.style.MyDialog);
        mScanProgressDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater layoutinflater = LayoutInflater.from(MainActivity.this);
        scanProgress =  (View) layoutinflater.inflate(R.layout.smb_search_progress, null);
        TextView textview = (TextView) scanProgress.findViewById(R.id.percent);
        textview.setText(getResources().getString(R.string.scanningSmbMessage)+"0%");
        mScanProgressDialog.setContentView(scanProgress);
        mScanProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){

            @Override
            public void onCancel(DialogInterface dialog) {
                // TODO Auto-generated method stub
                mSmbUtil.setSearchOver(true);
                dialog.dismiss();
            }

        });
        mScanProgressDialog.show();

        Thread scanthread = new Thread(mScanRun);
        scanthread.setPriority(10);
        scanthread.start();
        mHandler.postDelayed(mRefreshRun, 1000);
    }
    Runnable mScanRun = new Runnable(){

        @Override
        public void run() {
            // TODO Auto-generated method stub
            mSmbUtil.searchSmb();
        }

    };
    public Handler mFillHandler = new Handler();
    public Runnable mFillRun = new Runnable() {
        public void run() {
//            LOG("in the mFillRun, is_finish_fill = " + FileControl.is_finish_fill +" is_enable_fill:"+ FileControl.is_enable_fill);
//            LOG("in the mFillRun, adapte size = " + mFileControl.folder_array.size());
            if (!FileControl.is_enable_fill){
                FileControl.is_enable_fill = true;
                return;
            }

            if(!FileControl.is_finish_fill)
            {
                if(openwiththread)
                {
                    setFileAdapter(false, false);
                }
                mFillHandler.postDelayed(mFillRun, 1500);
            }
            else
            {
                if(openwiththread)
                    setFileAdapter(false, false);
                else
                {
                    if(mIsLastDirectory)
                    {
                        mIsLastDirectory = false;
                        setFileAdapter(false, false);
                    }
                    else
                    {
                        setFileAdapter(true, mEnableLeft);
                    }
                }
                mFillHandler.removeCallbacks(mFillRun);
                if(openingDialog != null)
                    openingDialog.dismiss();
//                enableButton(true,true);

                Message msg = new Message();
                msg.what = EnumConstent.MSG_DLG_HIDE;
                mHandler.sendMessage(msg);
            }
        }
    };
    public boolean SmbReadPermissionTest(String path){
        String host;
        String sharename;
        SmbFile smbfile;

        String split[] = mCurrnetSmb.substring(4).split("/");
        host = split[0];
        sharename = split[1];
        if (mCurSmbAnonymous){
//            LOG("smb://guest:@"+host+"/"+sharename+path.substring(24)+"/");
            try{
                smbfile = new SmbFile("smb://guest:@"+host+"/"+sharename+path.substring(24)+"/");
                smbfile.list();
            }catch (SmbAuthException e) {
                // TODO: handle exception
                e.printStackTrace();
                return false;
            }catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            }catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
                return false;
            }
        }else {
            try{
//                LOG("smb://"+mCurSmbUsername+":"+mCurSmbPassword+"@"+host+"/"+sharename+path.substring(24)+"/");
                smbfile = new SmbFile("smb://"+mCurSmbUsername+":"+mCurSmbPassword+"@"+host+"/"+sharename+path.substring(24)+"/");
                smbfile.list();
            }catch (SmbAuthException e) {
                // TODO: handle exception
                e.printStackTrace();
                return false;
            }catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            }catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
    public String parseMountDirToSmbpath(String mountdir){
        String split [] = mountdir.substring(4).split("/");
        String smbpath = "//"+split[0]+"/"+split[1];
        return smbpath;
    }
    public boolean cifsIsMountAndConnect(String smbpath){
        String line = null;
        ArrayList<String> strlist = new ArrayList<String>();
        try {
            Process pro = Runtime.getRuntime().exec("mount");
            BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            while ((line = br.readLine())!=null){
                strlist.add(line);
            }
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return false;
        }
        ArrayList<String> cifslist = new ArrayList<String>();
        for (String str : strlist){
            String split[] = str.split(" ");
            if (split[2].equals("cifs")){
                cifslist.add(split[0].replace("\\"+"040", " ").replace("\\"+"134", "/"));
            }
        }
        if(cifslist.contains(smbpath)){
            String ip = smbpath.substring(2, smbpath.indexOf("/", 2));
            if(ping(ip))
                return true;
            else
                showToast(getResources().getString(R.string.smb_host_error,ip));
        }
        return false;
    }
    public Handler mOpenHandler = new Handler();
    public Runnable mOpeningRun = new Runnable() {
        public void run() {
            Log.d("MainActivity","openwiththread = "+openwiththread);
            if(openwiththread){
                mFileControl.refillwithThread(fill_path);
            }
            else
            {
                mFileControl.refill(fill_path);
            }
            mOpenHandler.removeCallbacks(mOpeningRun);
        }
    };
    Runnable mRefreshRun = new Runnable(){

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if(mSmbUtil.searchover()){
                if (mScanProgressDialog !=null){
                    mScanProgressDialog.dismiss();
                }
                Log.i("MainActivity","searchover");
                mHandler.removeCallbacks(mRefreshRun);
            } else {
                if (mScanProgressDialog !=null){
                    int percent = (mSmbUtil.getScannedHostCount()*100)/mSmbUtil.getAllHostCount();
                    TextView textview = (TextView) scanProgress.findViewById(R.id.percent);
                    textview.setText(getResources().getString(R.string.scanningSmbMessage)+percent+"%");
                }
                mHandler.postDelayed(mRefreshRun, 1000);
            }
        }

    };
    public boolean ping(String ip){
        try  {
            Socket server = new Socket();
            InetSocketAddress address = new InetSocketAddress(ip,
                    445);
            server.connect(address, 4000);
            server.close();
        } catch (UnknownHostException e){
            try  {
                Socket server = new Socket();
                InetSocketAddress address = new InetSocketAddress(ip,
                        139);
                server.connect(address, 4000);
                server.close();
            } catch (UnknownHostException e1){
                return false;
            } catch (IOException e1){
                return false;
            }
            return true;
        } catch (IOException e){
            try  {
                Socket server = new Socket();
                InetSocketAddress address = new InetSocketAddress(ip,
                        139);
                server.connect(address, 4000);
                server.close();
            } catch (UnknownHostException e1){
                return false;
            } catch (IOException e1){
                return false;
            }
            return true;
        }
        return true;
    }
    public void showToast(String str){
        final String msg = str;
        mHandler.post(new Runnable(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });

    }
    private void StopCopy()
    {
        CopyFileUtils.is_enable_copy = false;
        CopyFileUtils.is_copy_finish = true;
        FileControl.is_enable_del = false;
        FileControl.is_enable_fill = false;

        mCopyFileUtils.setSourcePath("");
        mCopyFileUtils.setTargetPath("");

        if(mDialogCopy != null)
        {
            mDialogCopy.dismiss();
            mDialogCopy = null;
        }
    }
    private void showCopyFailDialog(int reson)
    {
        Dialog CopyFailDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.copy_fail)
                .setMessage(reson)
                .setPositiveButton(R.string.ok,new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog,int which)
                    {
                        dialog.dismiss();
                    }
                }).show();
    }
    private void showGetFileCountDialog()
    {
        GetFileCountDialog = new ProgressDialog(this);
        String msg = this.getResources().getString(R.string.copy_get_file_count);
        GetFileCountDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        GetFileCountDialog.setMessage(msg);
        GetFileCountDialog.setIcon(0);
        GetFileCountDialog.setIndeterminate(false);
        GetFileCountDialog.show();
        GetFileCountDialog.setOnCancelListener(null);
    }
    Handler mCopyingHandler = new Handler();
    Runnable mCopyingRun = new Runnable() {
        public void run() {
            mCopyFileUtils.getCopyFileCount(mSelectedPathList);
            if(GetFileCountDialog != null)
            {
                GetFileCountDialog.dismiss();
                GetFileCountDialog = null;
            }
            mCopyFileUtils.is_copy_finish = false;
            mCopyFileUtils.is_enable_copy = true;

            showCopyDialog();
            mCopyHandler.postDelayed(mCopyRun, 10);
        }
    };
    Handler mCopyHandler = new Handler();
    Runnable mCopyRun = new Runnable() {
        public void run() {
            if(CopyFileUtils.is_copy_finish){


                if ((CopyFileUtils.cope_now_sourceFile != null)
                        && (CopyFileUtils.cope_now_targetFile != null)) {
                    int percent = (int) ((((float) CopyFileUtils.mHasCopytargetFileSize) / ((float) CopyFileUtils.cope_now_sourceFile
                            .length())) * 100);
                    Log.i(TAG," CopyFileUtils.cope_now_targetFile.length() = "
                            + CopyFileUtils.mHasCopytargetFileSize);
                    Log.i(TAG," CopyFileUtils.cope_now_sourceFile.length() = "
                            + CopyFileUtils.cope_now_sourceFile.length());
                    Log.i(TAG," percent = " + percent);
                    ((ProgressBar) myCopyView
                            .findViewById(R.id.one_copy_percent))
                            .setProgress((int) percent);
                    ((TextView) myCopyView.findViewById(R.id.one_percent_Text))
                            .setText(percent + " %");

                    percent = (int) ((((float) CopyFileUtils.mhascopyfilecount) / ((float) CopyFileUtils.mallcopyfilecount)) * 100);
                    ((ProgressBar) myCopyView
                            .findViewById(R.id.all_copy_percent))
                            .setProgress((int) percent);
                    ((TextView) myCopyView.findViewById(R.id.all_percent_Text))
                            .setText("" + CopyFileUtils.mhascopyfilecount
                                    + " / " + CopyFileUtils.mallcopyfilecount);
                    Log.i(TAG," mhascopyfilecount = "
                            + CopyFileUtils.mhascopyfilecount
                            + ", CopyFileUtils.mallcopyfilecount = "
                            + CopyFileUtils.mallcopyfilecount);
                }


                UnLockScreen();
                Log.i(TAG,"mCopyRun, the CopyFileUtils.is_copy_finish = " + CopyFileUtils.is_copy_finish);
                Log.i(TAG,"mCopyRun, mEnableMove = " + mEnableMove);
                CopyFileUtils.mHasCopytargetFileSize = 0;
                if(mEnableMove && !CopyFileUtils.is_same_path){
                    mEnableMove = false;
                    mEnableCopy = false;

                    mFileControl.deleteFileInfo(mCopyFileUtils.get_has_copy_path());

                }
                Log.i(TAG,"mCopyRun, CopyFileUtils.is_enable_copy = " + CopyFileUtils.is_enable_copy);
                if(!CopyFileUtils.is_enable_copy){
                    mFileControl.deleteFile(CopyFileUtils.mInterruptFile);
                }

                for(int i = 0; i < mCopyFileUtils.get_has_copy_path().size(); i ++){
                    File tmp_file=new File(mFileControl.get_currently_path()+File.separator+mCopyFileUtils.get_has_copy_path().get(i).mFile.getName());
                    FileInfo tmp_fileinfo = mFileControl.changeFiletoFileInfo(tmp_file);
                    if(!mFileControl.isFileInFolder(tmp_file)){
                        if(!mFileControl.folder_array.contains(tmp_fileinfo)){
                            if(tmp_file.isDirectory()){	//file is folder
                                mFileControl.folder_array.add(0, tmp_fileinfo);
                            }else{
                                mFileControl.folder_array.add(tmp_fileinfo);
                            }
                        }
                    }
                }
                setFileAdapter(false, false);//reflesh

                if(mDialogCopy != null){
                    mDialogCopy.dismiss();
                    mDialogCopy = null;
                }



                CopyFileUtils.is_copy_finish = false;
                if(CopyFileUtils.is_not_free_space){
                    CopyFileUtils.is_not_free_space = false;
                    if(CopyFileUtils.pathError)
                        Toast.makeText(MainActivity.this, getString(R.string.error_invalid_path), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this, getString(R.string.err_not_free_space), Toast.LENGTH_SHORT).show();
                }

                mCopyHandler.removeCallbacks(mCopyRun);
                try
                {
                    Thread.sleep(1000);
                }catch(Exception ex){
                    Log.e(TAG, "Exception: " + ex.getMessage());
                }
                openDir(fill_path);
                setFileAdapter(false, false);

                mCopyFileUtils.setSourcePath("");
                mCopyFileUtils.setTargetPath("");

                CopyFileUtils.cope_now_sourceFile=null;
                CopyFileUtils.cope_now_targetFile=null;

            }else{
                if(mEnablePaste){
                    Log.i(TAG,"in mCopyRun, the mEnableCopy = " + mCopyRun);
                    mEnablePaste = false;
                    LockScreen();
                    mCopyFileUtils.CopyFileInfoArray(mSourcePathList, mFileControl.get_currently_path());
                }
                if((CopyFileUtils.cope_now_sourceFile != null) && (CopyFileUtils.cope_now_targetFile != null)){
                    ((TextView)myCopyView.findViewById(R.id.source_Text)).setText(getChangePath(CopyFileUtils.cope_now_sourceFile.getPath()));
                    ((TextView)myCopyView.findViewById(R.id.target_Text)).setText(getChangePath(CopyFileUtils.cope_now_targetFile.getPath()));

                    int percent = (int)((((float)CopyFileUtils.mHasCopytargetFileSize) / ((float)CopyFileUtils.cope_now_sourceFile.length())) * 100);
                    Log.i(TAG," CopyFileUtils.cope_now_targetFile.length() = " + CopyFileUtils.mHasCopytargetFileSize);
                    Log.i(TAG," CopyFileUtils.cope_now_sourceFile.length() = " + CopyFileUtils.cope_now_sourceFile.length());
                    Log.i(TAG," percent = " + percent);
                    ((ProgressBar)myCopyView.findViewById(R.id.one_copy_percent)).setProgress((int)percent);
                    ((TextView)myCopyView.findViewById(R.id.one_percent_Text)).setText(percent + " %");

                    percent = (int)((((float)CopyFileUtils.mhascopyfilecount) / ((float)CopyFileUtils.mallcopyfilecount)) * 100);
                    ((ProgressBar)myCopyView.findViewById(R.id.all_copy_percent)).setProgress((int)percent);
                    ((TextView)myCopyView.findViewById(R.id.all_percent_Text)).setText(""+CopyFileUtils.mhascopyfilecount+" / "+CopyFileUtils.mallcopyfilecount);
                    Log.i(TAG," mhascopyfilecount = " + CopyFileUtils.mhascopyfilecount+", CopyFileUtils.mallcopyfilecount = "+CopyFileUtils.mallcopyfilecount);
                }
                Log.i(TAG,"in mCopyRun, --- --- the mEnableCopy = " + mEnableCopy);

                if(CopyFileUtils.mRecoverFile != null){
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(CopyFileUtils.mRecoverFile + getString(R.string.copy_revocer_text))
                            .setPositiveButton(getString(R.string.copy_revocer_yes),new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    CopyFileUtils.is_recover = true;
                                    CopyFileUtils.is_wait_choice_recover = false;
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(getString(R.string.copy_revocer_no),new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,int which)
                                {
                                    CopyFileUtils.is_recover = false;
                                    CopyFileUtils.is_wait_choice_recover = false;
                                    dialog.dismiss();
                                }
                            }).show();
                    CopyFileUtils.mRecoverFile = null;
                }

                if ((CopyFileUtils.cope_now_sourceFile == null)
                        && (CopyFileUtils.cope_now_targetFile == null))
                    mCopyHandler.postDelayed(mCopyRun, 10);
                else mCopyHandler.postDelayed(mCopyRun, 500);

            }
        }
    };
    public String getChangePath(String content){
        String ret = "";
        if(content.startsWith(EnumConstent.mDirSmbMoutPoint)){
            String smbpath = mCifsProxy.getSmbFromMountPoint(content.substring(0, 24));
            ret = "smb:"+smbpath + content.substring(EnumConstent.mDirSmbMoutPoint.length()+15);
        }else{
            ret = content;
        }
        return ret;
    }
    private void LockScreen(){
        if (mWakeLock != null) {
            try {
                if (mWakeLock.isHeld() == false){
                    mWakeLock.acquire();
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public boolean verifyTargetPath(){
        if(mSourcePathList.get(0).mFile.getParent().equals(mFileControl.get_currently_path())){
            Toast.makeText(this, getString(R.string.err_target_same), Toast.LENGTH_SHORT).show();
            return false;
        }else{
            for(int i = 0; i < mSourcePathList.size(); i ++){
                if(mSourcePathList.get(i).mFile.isDirectory()){
                    if(mFileControl.get_currently_path().startsWith(mSourcePathList.get(i).mFile.getPath() + "/") ||
                            mFileControl.get_currently_path().equals(mSourcePathList.get(i).mFile.getPath()) ){
                        Toast.makeText(this, getString(R.string.err_target_child), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            }
        }
        return true;
    }
    private boolean verityPermission(){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String testfilename = mFileControl.get_currently_path()+"/"+sf.format(new Date());
        try{
            if (!new File(testfilename).createNewFile()){
                return false;
            }else{
                new File(testfilename).delete();
                return true;
            }
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return false;
        }
    }
    private void copyInForeground()
    {
        if(mSourcePathList.size()>0){
            Log.i(TAG,"verifyTargetPath SourceParent=" + mSourcePathList.get(0).mFile.getParent());
        }else{
            Log.i(TAG,"verifyTargetPath mSourcePathList.size()=0");
        }
        Log.i(TAG,"verifyTargetPath DistanceParent=" + mFileControl.get_currently_path());

        if(verifyTargetPath())
        {
            if(!verityPermission()){
                Toast.makeText(MainActivity.this, getResources().getString(R.string.write_error), Toast.LENGTH_SHORT).show();
                return;
            }


            if(mEnableCopy)
                mEnablePaste = true;
            mHandler.sendEmptyMessage(EnumConstent.MSG_DLG_COUNT);
        }
    }

    private void copyInBackgournd()
    {
        Intent intent = new Intent(MainActivity.this,CopyService.class);
        Bundle mBundle = new Bundle();

        if(mEnableMove)
            mBundle.putInt("command",CopyService.MOVE);
        else
            mBundle.putInt("command",CopyService.COPY);

        ArrayList<File> array = new ArrayList<File>();
        for(int i = 0; i < mSourcePathList.size(); i ++)
            array.add(mSourcePathList.get(i).mFile);
        mBundle.putSerializable("source", array);
        String currentPath = mFileControl.get_currently_path();
        mBundle.putString("target",currentPath);
        intent.putExtras(mBundle);

        MainActivity.this.startService(intent);
    }

    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case EnumConstent.MSG_ANI_ZOOM_IN:
                    View view = (View) msg.obj;
                    Animation ani = new ZoomAnimation(view, 1, 1.1f, 0, 100);
                    view.startAnimation(ani);
                    break;
                case EnumConstent.MSG_OP_STOP_COPY:
                    boolean disconnect = (msg.arg1==0);
                    String mSource = mCopyFileUtils.getSourcePath();
                    String mTarget = mCopyFileUtils.getTargetPath();
                    if( disconnect &&
                            (( mSource != null) && (mSource.startsWith(EnumConstent.mDirSmbMoutPoint))) ||
                            (( mTarget != null) && (mTarget.startsWith(EnumConstent.mDirSmbMoutPoint))))
                    {
                        StopCopy();
                        showCopyFailDialog(R.string.copy_interrupt);
                    }

                    Log.d(TAG,"mHandler,EnumConstent.MSG_OP_STOP_COPY = "+msg.arg1);
                    if(msg.arg1 == CopyFileUtils.NO_SPACE)
                    {
                        StopCopy();
                        mCopyFileUtils.mCopyResult = CopyFileUtils.COPY_OK;
                        showCopyFailDialog(R.string.full);
                    }
                    break;

                case EnumConstent.MSG_DLG_COUNT:
                    showGetFileCountDialog();
                    mCopyingHandler.postDelayed(mCopyingRun, 10);
                    break;
                case EnumConstent.MSG_DLG_SHOW:
                    showWaitDialog();
                    break;
                case EnumConstent.MSG_DLG_HIDE:
                    if (mWaitDialog != null){
                        mWaitDialog.dismiss();
                    }
                    break;

                case EnumConstent.MSG_DLG_LOGIN_FAIL:
                    FileInfo smbinfo = (FileInfo) msg.obj;
                    mCifsProxy.showLoginFailDialog(smbinfo);
                    break;

                case EnumConstent.MSG_CLEAR_CONTENT:
                    clearContentList();
                    mSavePath.clear();
                    mSavePath.add(new Path(mDefaultPath,""));
                    mPitSavePath = mSavePath.size()-1;
                    break;

                case EnumConstent.MSG_OP_START_COPY:
                    if(mEnableCopyInBg)
                        copyInBackgournd();
                    else
                        copyInForeground();
                    break;
            }
        }


    };
    public class smbfileThreadRun implements Runnable{
        private FileInfo mListItem = null;
        private int position = 0;

        public smbfileThreadRun(FileInfo mListItem, int position) {
            this.mListItem = mListItem;
            this.position = position;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            String smbpath = parseMountDirToSmbpath(mCurrnetSmb);
            if(!cifsIsMountAndConnect(smbpath)){
                Message msg = new Message();
                msg.what = EnumConstent.MSG_DLG_HIDE;
                mHandler.sendMessage(msg);
            }else {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if (mWaitDialog!=null){
//                            LOG("wait dialog dismiss.....");
                            mWaitDialog.dismiss();
                        }
                        mCifsProxy.openSmbfile(mListItem, position);
                    }
                });
            }

            return;
        }

    }
    public class smbThreadRun implements Runnable
    {
        private FileInfo mListItem = null;
        private String mSmbPath = null;
        /**
         * @param mListItem
         * @param mSmbPath
         */
        public smbThreadRun(FileInfo mListItem, String mSmbPath) {
            this.mListItem = mListItem;
            this.mSmbPath = mSmbPath;
        }
        @Override
        public void run() {
            // TODO Auto-generated method stub
            String path = mSmbPath;
            if (isItemClick){
                path = mCifsProxy.getMountPoint(mListItem,mSmbPath);
            }


            if (path == null) {
                //mount failed
                Message msg = new Message();
                msg.what = EnumConstent.MSG_DLG_HIDE;
                mHandler.sendMessage(msg);

                if (isItemClick){
                    if (mListItem.mFile.getPath().startsWith(EnumConstent.mDirSmb+"/")){
                        msg = new Message();
                        msg.what = EnumConstent.MSG_DLG_LOGIN_FAIL;
                        msg.obj = mListItem;
                        mHandler.sendMessage(msg);
                    }
                }

                return;
            }else if (path.startsWith(EnumConstent.mDirSmbMoutPoint)){

                if (mListItem.mFile.getPath().startsWith(EnumConstent.mDirSmb+"/")){
                    // path is like "SMB/IP/sharefile"
                    mCurrnetSmb = mListItem.mFile.getPath();
                    mCurSmbAnonymous = mListItem.isMAnonymous();
                    mCurSmbUsername = mListItem.getMUsername();
                    mCurSmbPassword = mListItem.getMPassword();
                }
                String curpath = mFileControl.get_currently_path();
                if (!curpath.startsWith(EnumConstent.mDirSmbMoutPoint)){
                    FileInfo smbinfo = mCifsProxy.getSmbinfoMap(path.substring(0,24));
                    if (smbinfo !=null){
                        mCurrnetSmb = smbinfo.mFile.getPath();
                        mCurSmbAnonymous = smbinfo.isMAnonymous();
                        mCurSmbUsername = smbinfo.getMUsername();
                        mCurSmbPassword = smbinfo.getMPassword();
                    }
                }else if (!curpath.substring(0,24).equals(path.substring(0,24))){
                    FileInfo smbinfo = mCifsProxy.getSmbinfoMap(path.substring(0,24));
                    if (smbinfo !=null){
                        mCurrnetSmb = smbinfo.mFile.getPath();
                        mCurSmbAnonymous = smbinfo.isMAnonymous();
                        mCurSmbUsername = smbinfo.getMUsername();
                        mCurSmbPassword = smbinfo.getMPassword();
                    }
                }



                if (mCifsProxy.openSmbDir(path)){
                    //open mount point dir
                    if (isItemClick){
                        if (!path.equals(mSavePath.get(mSavePath.size()-1).getPath())
                                || !mCurrentDir.equals(mSavePath.get(mSavePath.size()-1).getPathDirection())){
                            mSavePath.add(new Path(path,mCurrentDir));
                            mPitSavePath = mSavePath.size() - 1;
                        }else{
                            mPitSavePath = mSavePath.size() - 1;
                        }

                    }

                }else if (!isItemClick){
                    Message msg = new Message();
                    msg.what = EnumConstent.MSG_CLEAR_CONTENT;
                    mHandler.sendMessage(msg);
                }

            }else if(path.startsWith(EnumConstent.mDirSmb+"/")){
                // path is like "SMB/IP"
                if(mCifsProxy.getCifsChildContent(path)){
                    Message msg = new Message();
                    msg.what = EnumConstent.MSG_DLG_HIDE;
                    mHandler.sendMessage(msg);

                    if (isItemClick){
                        if (!path.equals(mSavePath.get(mSavePath.size()-1).getPath())
                                || !mCurrentDir.equals(mSavePath.get(mSavePath.size()-1).getPathDirection())){
                            mSavePath.add(new Path(path,mCurrentDir));
                            mPitSavePath = mSavePath.size() - 1;
                        }else{
                            mPitSavePath = mSavePath.size() - 1;
                        }
                    }

                    return;
                }else{
                    Message msg = new Message();
                    msg.what = EnumConstent.MSG_DLG_HIDE;
                    mHandler.sendMessage(msg);

                    if(isItemClick){
                        FileInfo smbinfo = mCifsProxy.getSmbinfoFromSmbPath(path);
                        msg = new Message();
                        msg.what = EnumConstent.MSG_DLG_LOGIN_FAIL;
                        msg.obj = smbinfo;
                        mHandler.sendMessage(msg);
                    }else{
                        msg = new Message();
                        msg.what = EnumConstent.MSG_CLEAR_CONTENT;
                        mHandler.sendMessage(msg);
                    }
                    return;
                }
            }
        }
    }
}
