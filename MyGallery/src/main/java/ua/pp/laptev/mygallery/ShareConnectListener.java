package ua.pp.laptev.mygallery;

import com.hierynomus.smbj.share.DiskShare;

import java.io.IOException;

public interface ShareConnectListener {
    void onConnect(DiskShare share) throws IOException;
}
