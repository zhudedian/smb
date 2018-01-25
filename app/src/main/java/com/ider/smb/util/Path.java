package com.ider.smb.util;

/**
 * Created by Eric on 2017/9/21.
 */

public class Path {
    private String path,mCurrentDir;

    public Path(String path , String mCurrentDir){
        this.path = path;
        this.mCurrentDir = mCurrentDir;
    }
    public String getPath(){
        return path;
    }
    public String getPathDirection(){
        return mCurrentDir;
    }
}
