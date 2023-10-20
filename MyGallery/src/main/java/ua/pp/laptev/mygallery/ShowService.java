package ua.pp.laptev.mygallery;

import static com.hierynomus.msfscc.FileAttributes.FILE_ATTRIBUTE_DIRECTORY;
import static com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OPEN;
import static ua.pp.laptev.mygallery.App.load_settings;
import static ua.pp.laptev.mygallery.App.runOnUIThread;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.service.dreams.DreamService;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.exifinterface.media.ExifInterface;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ShowService extends DreamService {

    private static final int LIST_DELAY = 5000;
    private static final int HISTORY_LENGTH = 10;
    private File tmp;
    private View layout_show;
    private TextView clockTextView;
    private ArrayList<String> photoList;
    private SecureRandom random;
    private Timer clockTimer;
    private Thread listThread;
    private long listDelay = 0;
    private final Object list_f = new Object();
    private Thread showThread;
    private long showDelay = 0;
    private final Object show_f = new Object();
    private Settings settings;
    private final LinkedList<String> history = new LinkedList<>();
    private boolean show_history = false;
    private boolean pause = false;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(true);
        setFullscreen(true);
        showPause(pause);
        settings = load_settings();
        layout_show = View.inflate(this, R.layout.layout_show, null);
        clockTextView = layout_show.findViewById(R.id.clockTextView);
        clockTextView.setText(null);
        clockTextView.setVisibility(settings.showClock ? View.VISIBLE : View.GONE);
        TextView pathTextView = layout_show.findViewById(R.id.pathTextView);
        pathTextView.setText(null);
        pathTextView.setVisibility(settings.showFilename ? View.VISIBLE : View.GONE);
        ImageView imageView = layout_show.findViewById(R.id.imageView);
        if (settings.stretchImage) imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        setContentView(layout_show);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        startShow();
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        stopShow();
    }

    private void nav(boolean history) {
        showProgress(true);
        show_history = history;
        showDelay = 0;
        pause = false;
        showPause(false);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            System.out.println("key code: " + event.getKeyCode());
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    nav(false);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    nav(true);
                    break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    pause ^= true;
                    showPause(pause);
                    if (!pause) showProgress(true);
                    showDelay = 0;
                    break;
                default:
                    finish();
                    break;
            }
        }
        return false;
    }

    private void stopShow() {
        clockTimer.cancel();
        listThread.interrupt();
        showThread.interrupt();
    }

    private void startShow() {
        tmp = new File(getApplicationContext().getCacheDir(), "show.jpg");
        random = new SecureRandom();
        photoList = new ArrayList<>();

        clockTimer = new Timer();
        clockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                showClock();
                synchronized (list_f) {
                    list_f.notify();
                }
                synchronized (show_f) {
                    show_f.notify();
                }
            }
        }, 10, 500);

        listThread = new Thread(new TimerTask() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    synchronized (list_f) {
                        try {
                            list_f.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    if ((listDelay - System.currentTimeMillis()) > 0) continue;
                    try {
                        if (photoList.size() == 0) {
                            connectShare(share -> listFiles(share, settings.path));
                        }
                    } catch (IOException e) {
                        showError(e.toString());
                        listDelay = System.currentTimeMillis() + LIST_DELAY;
                    } catch (SMBRuntimeException ignored) {
                    }
                }
            }
        });
        listThread.start();

        showThread = new Thread(new TimerTask() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    synchronized (show_f) {
                        try {
                            show_f.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    if (photoList.size() == 0) continue;
                    if ((showDelay - System.currentTimeMillis()) > 0) continue;
                    if (pause) continue;
                    try {
                        connectShare(share -> {
                            if (show_history) {
                                try {
                                    history.removeLast();
                                    String src = history.getLast();
                                    if (showImg(share, src)) return;
                                } catch (NoSuchElementException ignored) {
                                    showProgress(false);
                                }
                                show_history = false;
                            } else {
                                if (!showNext(share)) return;
                            }
                            showDelay = System.currentTimeMillis() + settings.showDelay;
                        });
                    } catch (IOException e) {
                        photoList.clear();
                        showError(e.toString());
                    }

                }
            }
        });
        showThread.start();
    }

    private boolean showNext(DiskShare share) throws IOException {
        int r = random.nextInt(photoList.size());
        String src = photoList.get(r);
        if (!share.fileExists(src)) {
            photoList.remove(r);
            return false;
        }
        if (showImg(share, src)) return false;
        if (history.size() >= HISTORY_LENGTH) history.removeFirst();
        history.addLast(src);
        return true;
    }

    private boolean showImg(DiskShare share, String src) throws IOException {
        try (com.hierynomus.smbj.share.File f = share.openFile(src,
                EnumSet.of(AccessMask.FILE_READ_DATA),
                null,
                SMB2ShareAccess.ALL,
                FILE_OPEN,
                null)) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(tmp)) {
                f.read(fileOutputStream);
            }
        }
        Bitmap sourceBitmap = BitmapFactory.decodeFile(tmp.getAbsolutePath());
        if (sourceBitmap == null) return true;
        Matrix matrix = new Matrix();
        matrix.postRotate(0);
        ExifInterface exif = new ExifInterface(tmp.getAbsolutePath());
        int orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        System.out.println("orient: "+orient);
        switch (orient) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }
        Bitmap rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
        showImage(src, rotatedBitmap);
        return false;
    }

    private void listFiles(DiskShare share, String path) {
        for (FileIdBothDirectoryInformation fi : share.list(path, "*")) {
            if (fi.getFileName().startsWith(".")) continue;
            if (EnumWithValue.EnumUtils.isSet(fi.getFileAttributes(), FILE_ATTRIBUTE_DIRECTORY)) {
                listFiles(share, path + "/" + fi.getFileName());
                continue;
            }
            if (!fi.getFileName().toLowerCase().endsWith(".jpg")) {
                continue;
            }
            photoList.add(path + "/" + fi.getFileName());
        }
    }

    private void connectShare(ShareConnectListener listener) throws IOException {
        SmbConfig config = SmbConfig.builder()
                .withTimeout(30, TimeUnit.SECONDS)
                .withSoTimeout(60, TimeUnit.SECONDS)
                .build();
        try (SMBClient client = new SMBClient(config)) {
            Connection connection = client.connect(settings.server);
            AuthenticationContext ac = new AuthenticationContext(settings.user,
                    settings.pass.toCharArray(),
                    settings.domain);
            Session session = connection.authenticate(ac);
            listener.onConnect((DiskShare) session.connectShare(settings.share));
        }
    }

    private void showPause(boolean visibility) {
        runOnUIThread(() -> {
            ImageView pauseImageView = layout_show.findViewById(R.id.pauseImageView);
            pauseImageView.setVisibility(visibility ? View.VISIBLE : View.GONE);
        });
    }

    private void showProgress(boolean visibility) {
        runOnUIThread(() -> {
            ProgressBar progressBar = layout_show.findViewById(R.id.progressBar);
            progressBar.setVisibility(visibility ? View.VISIBLE : View.GONE);
        });
    }

    private void showError(String message) {
        history.clear();
        runOnUIThread(() -> {
            TextView textView = layout_show.findViewById(R.id.pathTextView);
            textView.setText(message);
            ImageView imageView = layout_show.findViewById(R.id.imageView);
            imageView.setVisibility(View.GONE);
        });
        showProgress(true);
    }

    private void showImage(String info, Bitmap myBitmap) {
        runOnUIThread(() -> {
            TextView textView = layout_show.findViewById(R.id.pathTextView);
            textView.setText(info);
            ImageView imageView = layout_show.findViewById(R.id.imageView);
            imageView.setImageBitmap(myBitmap);
            imageView.setVisibility(View.VISIBLE);
        });
        showProgress(false);
    }

    private void showClock() {
        runOnUIThread(() -> {
            clockTextView.setText(DateFormat.format(settings.clockFormat, new Date()).toString());
            //
        });
    }

}
