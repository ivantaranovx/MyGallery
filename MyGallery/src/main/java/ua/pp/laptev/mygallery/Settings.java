package ua.pp.laptev.mygallery;

import java.io.Serializable;

public class Settings implements Serializable {
    private static final long serialVersionUID = 1L;
    public String server = "192.168.123.200";
    public String user = "alex";
    public String pass = "tri15ton20";
    public String domain = "workgroup";
    public String share = "kino";
    public String path = "photo";
    public int showDelay = 15000;
    public boolean showFilename = true;
    public boolean showClock = true;
    public String clockFormat = "HH:mm:ss";
    public boolean stretchImage = false;
}
