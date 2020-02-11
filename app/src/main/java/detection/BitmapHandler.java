package detection;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * How to user this class
 * 1. create object with class, passing in bitmap, file name, and current datetime
 * 2. run save()
 * 3. run uploadFile() function, passing in hostname, username, port number(Integer), password
 * OPTIONAL: reconfigure the bitmap and filename with reconfigure object function
 *
 */

public class BitmapHandler {
    public Bitmap bmp;
    public String filename;
    public long dateTime;
public SFTPClient sftpClient;
    public String savedFileName;
    public String destinationFileOnServer;

    public BitmapHandler(Bitmap bmp, String filename, long dateTime){
        this.bmp = bmp;
        this.filename = filename;
        this.dateTime = dateTime;
    }

    public void reconfigureObject (Bitmap bmp, String filename, long dateTime){
        this.bmp = bmp;
        this.filename = filename;
        this.dateTime = dateTime;
    }

    public void save() throws IOException {
        savedFileName = "sdcard/" + this.filename;
        File file = new File(savedFileName);
        FileOutputStream out = new FileOutputStream(file);
        this.bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
    }

    public void setupSFTPClient(String hostname, String username, int portnumber, String password, String source, String destination){
        sftpClient = new SFTPClient(username, hostname, portnumber, password, source, destination);
        sftpClient.setUploadFile(true);
    }

    public void uploadFile(String hostname, String username, int portnumber, String password, String source, String destination){
        setupSFTPClient(hostname, username, portnumber, password, source, destination);
        sftpClient.execute();
    }

    public void setupSourceDestination(String source, String destination){
        this.savedFileName = source;
        this.destinationFileOnServer = destination;
    }

    public String getFilename() {
        return filename;
    }
}
