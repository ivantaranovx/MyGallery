package ua.pp.laptev.mygallery;

import java.io.Serializable;

public class Settings implements Serializable {
    private static final long serialVersionUID = 1L;
    public String server = "";
    public String user = "";
    public String pass = "";
    public String domain = "workgroup";
    public String share = "";
    public String path = "";
    public int showDelay = 15000;
    public boolean showFilename = true;
    public boolean showClock = true;
    public String clockFormat = "HH:mm:ss";
    public boolean stretchImage = false;
}
