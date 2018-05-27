package com.example.fahad.logger;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private TextView tv;
    DatagramSocket clientSocket = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv=findViewById(R.id.textView);

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_LOGS,
                        Manifest.permission.INTERNET)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                            readLogs();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            readLogs();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
        Log.d("MAINACTIVITY--------->","Hi msg to test...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(clientSocket.isConnected()){
            clientSocket.close();
        }
    }

    public void readLogs(){
        try {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try{

                        Process process = Runtime.getRuntime().exec("logcat -d com.example.fahad.logger");
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));

                        final StringBuilder log=new StringBuilder();
                        String line = "";
                        while ((line = bufferedReader.readLine()) != null) {
                            log.append(line+"\n");
                        }
                        tv.post(new Runnable() {
                            @Override
                            public void run() {
                                tv.append(log.toString());
                                try {
                                    sendUdp(log.toString());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                clearLog();
                            }
                        });
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            },0,5000);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendUdp(final String msg) throws IOException {
//        BufferedReader inFromUser =
//                new BufferedReader(new InputStreamReader(System.in));
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    clientSocket = new DatagramSocket();

                    InetAddress IPAddress = InetAddress.getByName("192.168.100.5");
                    byte[] sendData = new byte[1024];
                    String sentence = msg;
                    sendData = sentence.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
                    clientSocket.send(sendPacket);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    clientSocket.close();
                }


            }
        }).start();
    }
    public void clearLog(){
        try {
            Process process = new ProcessBuilder()
                    .command("logcat", "-c")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
        }
    }
}
