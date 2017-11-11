package com.holgis.rcclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.erz.joysticklibrary.JoyStick;
import com.holgis.rcclient.joystick.DualSpeed;
import com.holgis.rcclient.joystick.DualTwoWayTranslator;
import com.holgis.rcclient.joystick.FourWayTranslator;
import com.holgis.rcclient.joystick.IJoystickTranslator;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivityUDP extends AppCompatActivity {

    private boolean DUAL_STICK = false;

    private DatagramSocket socketClient;
    private ByteBuffer dataBuffer;
    private JoyStick joyStickLeft;
    private JoyStick joyStickRight;

    private CheckBox ledLeft;
    private CheckBox ledRight;

    private boolean checkLeft;
    private boolean checkRight;


    private IJoystickTranslator joystickTranslator;

    Handler joystickHandler = new Handler();


//    private String SERVER_NAME = "192.168.0.131";
    private String SERVER_NAME = "192.168.0.140";
    private int SERVER_PORT = 9090;

    //private String VIDEO_PATH = "tcp://192.168.66.154:3333";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initJoystickView();

        initConnection();
    }


    void initJoystickView(){

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        DUAL_STICK = prefs.getBoolean("PREF_JOY_DUAL", DUAL_STICK);

        if(DUAL_STICK) {
            setContentView(R.layout.activity_dual);

            joyStickLeft = (JoyStick) findViewById(R.id.joyStickLeft);
            joyStickLeft.setType(JoyStick.TYPE_2_AXIS_UP_DOWN);

            joyStickRight = (JoyStick) findViewById(R.id.joyStickRight);
            joyStickRight.setType(JoyStick.TYPE_2_AXIS_UP_DOWN);

            joystickTranslator = new DualTwoWayTranslator(joyStickLeft, joyStickRight);
        }
        else
        {
            setContentView(R.layout.activity_single);

            joyStickLeft = (JoyStick) findViewById(R.id.joyStickSingle);
            joyStickLeft.setType(JoyStick.TYPE_8_AXIS);


            joystickTranslator = new FourWayTranslator(joyStickLeft);
        }


        ledLeft = (CheckBox)findViewById(R.id.ledLeft);
        ledLeft.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                checkLeft = b;
            }
        });
        ledRight = (CheckBox)findViewById(R.id.ledRight);
        ledRight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                checkRight = b;
            }
        });
    }

    void initConnection() {

        dataBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);

        new Thread(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(socketClient != null) {
            socketClient.disconnect();
        }
        joystickHandler.removeCallbacks(runnableCode);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void connect(){
        try {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            SERVER_NAME = prefs.getString("PREF_IP_ADDR", SERVER_NAME);

            DatagramSocket socket= new DatagramSocket();
            socket.connect(InetAddress.getByName(SERVER_NAME), SERVER_PORT);
            socketClient = socket;
            joystickHandler.post(runnableCode);
        } catch (UnknownHostException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    void send(int left, int right, int led, int extra){
        if(socketClient != null) {
            dataBuffer.clear();
            dataBuffer.put((byte)left);
            dataBuffer.put((byte)right);
            dataBuffer.put((byte)(led & 0xff));
            dataBuffer.put((byte)(extra & 0xff));
            try {
                socketClient.send(new DatagramPacket(dataBuffer.array(), 8,
                        InetAddress.getByName(SERVER_NAME), SERVER_PORT));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {

            final DualSpeed dualSpeed = joystickTranslator.getDualSpeed();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int led = 0;
                    led += checkLeft ? 0x02 : 0;
                    led += checkRight ? 0x01 : 0;
                    send(dualSpeed.getLeft(), dualSpeed.getRight(), led, 0);
                }
            }).start();

            joystickHandler.postDelayed(this, 40);
        }
    };
}
