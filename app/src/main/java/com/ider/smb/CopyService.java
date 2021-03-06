package com.ider.smb;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.Toast;

import com.ider.smb.util.EnumConstent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CopyService extends Service {
    private Context mContext = null;

    private int mServiceStartId = -1;
    public int mCommand = -1;
    public final static int COPY = 0;
    public final static int MOVE = 1;

    private String TAG = "CopyService";
    private void LOG(String msg)
    {
        if(true)
            Log.d(TAG,msg);
    }

    public void onCreate()
    {
        super.onCreate();
        mContext = this;
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }


    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent == null)
            return START_NOT_STICKY;

        LOG("onStartCommand,startId = "+startId);
        mCommand = intent.getIntExtra("command", -1);
        if(mCommand == -1)
            return START_NOT_STICKY;

        Bundle bundle = intent.getExtras();
        if(bundle == null)
            return START_NOT_STICKY;

        ArrayList<File> source = (ArrayList<File>)bundle.getSerializable("source");
        String target = bundle.getString("target");

        if(source == null || target == null)
            return START_NOT_STICKY;

        mServiceStartId =  startId;

        LOG("onStartCommand success, mTarget = "+target);

        new CopyThread(source,target).start();
        return START_STICKY;
    }

    public boolean onUnbind(Intent intent)
    {
        stopSelf(mServiceStartId);
        return true;
    }

    public void onDestroy()
    {
        super.onDestroy();
    }


    class CopyThread extends Thread
    {
        private ArrayList<File> mCopyFile = null;
        private String mTarget = null;

        private boolean isRecovery = false;
        private boolean isAwalys = false;

        private Message mMsg;

        public CopyThread(ArrayList<File> source, String target)
        {
            mCopyFile = source;
            mTarget = target;
        }

        public void run()
        {
            if((mCopyFile == null) || (mCopyFile.size() <= 0) ||(mTarget == null))
                return ;
            LOG("CopyThread, run ****************");
            for(int i = 0; i < mCopyFile.size(); i++)
            {
                File source = mCopyFile.get(i);
                if(!source.canRead())
                {
                    String noRead = mContext.getString(R.string.cannot_read);
                    showToast(source+" "+noRead);
                }
                String targetPath = (new File(mTarget)).getAbsolutePath()+File.separator;
                // copy direcotory
                try
                {
                    if(source.isDirectory())
                    {
                        if(CopyDirectory(source.getPath(),targetPath+source.getName()) == -1)
                            return ;
                    }
                    else
                    {
                        File temp = new File(mTarget);
                        File target = new File(temp.getAbsolutePath()+File.separator+source.getName());
                        if(CopyFile(source, target) == -1)
                            return ;
                    }
                }
                catch(IOException e)
                {
                    LOG("mCopyRun,e = "+e);
                    return ;
                }
            }

            showToast(R.string.copy_complete);
        }

        public int CopyFile(File source,File target)  throws IOException
        {
            if(source.getPath().equals(target.getPath()))
            {
                // do nothing, exit directly
                return 1;
            }

            if(haveSpace(source,target))
            {
                if(target.exists())
                {
                    mMsg = new Message();
                    mMsg.obj = new CopyObeject(source,target);

                    if(!isAwalys)
                    {
                        createRecoveryDialog(target);
                    }
                }
                else
                {
                    doRealCopy(source,target);
                }
                return 0;
            }
            else
            {
                // no space
                showToast(R.string.full);
                return -1;
            }
        }

        public int CopyDirectory(String sourceDir, String targetDir) throws IOException
        {
            if(sourceDir.equals(targetDir))
            {
                return 1;
            }

            // if the target Directory is not exist, then create it
            if(!(new File(targetDir)).exists())
            {
                File file = (new File(targetDir));
                if(!file.mkdirs())
                {
                    showToast(R.string.makedir_fail);
                    return -1;
                }
                if(!file.canWrite())
                {
                    String noWrite = mContext.getString(R.string.cannot_write);
                    showToast(targetDir+" "+noWrite);
                    return -1;
                }
                //			LOG("CopyDirectory(), make directory,send broadcast, targetDir = "+file.getParent());
                Intent intent = new Intent("com.rockchip.tv.reFillFile", Uri.parse("file://" + file.getParent()));
                sendBroadcast(intent);
            }
            // get the number of files in source Directory
            File[] files = (new File(sourceDir)).listFiles();

            for (int i = 0; i < files.length; i++)
            {
                if(files[i].isFile())
                {
                    File source = files[i];
                    File target = new File(new File(targetDir).getAbsolutePath()+File.separator+files[i].getName());
                    if(source != null && target != null)
                    {
                        if(CopyFile(source,target) == -1)
                        {
                            return -1;
                        }
                    }
                }
                else if (files[i].isDirectory())
                {
                    String source = sourceDir + File.separator + files[i].getName();
                    String target = targetDir + File.separator + files[i].getName();
                    if(CopyDirectory(source, target) == -1)
                    {
                        return -1;
                    }
                }
            }
            // delete this directory
            deleteFile(new File(sourceDir));
            return 0;
        }


        void createRecoveryDialog(File file)
        {
            if(isAwalys)
            {
                return ;
            }

            Looper.prepare();
            LayoutInflater flater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)flater.inflate(R.layout.recovery_dialog,null);

            CheckBox awalys = (CheckBox)view.findViewById(R.id.awalys);
            awalys.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    if(((CheckBox)v).isChecked())
                    {
                        isAwalys = true;
                    }
                    else
                    {
                        isAwalys = false;
                    }
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setMessage(file +"  "+ mContext.getString(R.string.copy_revocer_text))
                    .setView(view)
                    .setPositiveButton(R.string.copy_revocer_yes,new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            isRecovery = true;
                            dialog.dismiss();
                            mMsg.arg1 = 1;
                            mHandler.sendMessage(mMsg);
                        }
                    })
                    .setNegativeButton(R.string.str_cancel,new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            isRecovery = false;
                            dialog.dismiss();
                            mMsg.arg1 = 0;
                            mHandler.sendMessage(mMsg);
                        }
                    })
                    .create();

            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
            dialog.show();
            Looper.loop();
        }
    };

    Handler mHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {

            CopyObeject obj = (CopyObeject)msg.obj;
            boolean recovery = (msg.arg1 == 1);
            if(recovery)//isRecovery
            {
                // use new file to recovery exist file
                deleteFile(obj.mTarget);
                doRealCopy(obj.mSource,obj.mTarget);
            }
            else
            {
                // do nothing ,skip this file
            }

        }
    };


    boolean haveSpace(File source, File target)
    {
        String path = null;

       if(target.getPath().startsWith(EnumConstent.mDirSmbMoutPoint))
        {
            return true;
        }


        StatFs stat = null;
        try
        {
            stat = new StatFs(path);
        }
        catch(IllegalArgumentException e)
        {
            return false;
        }

        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long totalSize = stat.getBlockCount();

        long space = availableBlocks * blockSize;
        if(totalSize == 0)
        {
            return false;
        }

        return space > source.length();
    }

    boolean doRealCopy(File source,File target)
    {
        try
        {
            FileInputStream input = new FileInputStream(source);
            BufferedInputStream inBuff=new BufferedInputStream(input);

            FileOutputStream output = new FileOutputStream(target);
            BufferedOutputStream outBuff=new BufferedOutputStream(output);

            byte[] b = new byte[1024*8];
            int len;
            while ((len =inBuff.read(b)) != -1)
            {
                outBuff.write(b, 0, len);

            }
            outBuff.flush();

            inBuff.close();
            outBuff.close();
            output.close();
            input.close();

            if(mCommand == MOVE)
            {
                deleteFile(source);
            }

            Intent intent = new Intent("com.rockchip.tv.reFillFile",Uri.parse("file://" + target.getParent()));
            sendBroadcast(intent);
            return true;
        }
        catch(FileNotFoundException e)
        {
            LOG("FileNotFoundException: "+e);
            return false;
        }
        catch(IOException e)
        {
            LOG("IOException: "+e);
            return false;
        }

    }

    void deleteFile(File file)
    {
        if(file != null)
        {
            file.delete();
        }
    }

    void showToast(int stringId)
    {
        Looper.prepare();
        Toast.makeText(mContext,stringId,Toast.LENGTH_LONG).show();
        Looper.loop();
    }

    void showToast(String text)
    {
        Looper.prepare();
        Toast.makeText(mContext,text,Toast.LENGTH_LONG).show();
        Looper.loop();
    }

    public class CopyObeject {
        public File mSource = null;
        public File mTarget = null;

        CopyObeject(File source, File target) {
            mSource = source;
            mTarget = target;
        }
    }
}
