package com.holgis.rcclient;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.erz.joysticklibrary.JoyStick;
import com.holgis.rcclient.joystick.DualSpeed;
import com.holgis.rcclient.joystick.DualTwoWayTranslator;
import com.holgis.rcclient.joystick.IJoystickTranslator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class MainActivityTCP extends AppCompatActivity {

    private Socket socketClient;
    private ByteBuffer dataBuffer;
    private JoyStick joyStickLeft;
    private JoyStick joyStickRight;

    private IJoystickTranslator joystickTranslator;

    Handler joystickHandler = new Handler();

 //  private String SERVER_NAME = "192.168.0.131";
   private String SERVER_NAME = "192.168.0.140";
    private int SERVER_PORT = 9090;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual);

        joyStickLeft = (JoyStick)findViewById(R.id.joyStickLeft);
        joyStickLeft.setType(JoyStick.TYPE_2_AXIS_UP_DOWN);
        joyStickRight = (JoyStick)findViewById(R.id.joyStickRight);
        joyStickRight.setType(JoyStick.TYPE_2_AXIS_UP_DOWN);

        joystickTranslator = new DualTwoWayTranslator(joyStickLeft, joyStickRight);
        joystickHandler = new Handler();

        init();

    }

    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {

            final DualSpeed dualSpeed = joystickTranslator.getDualSpeed();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    send(dualSpeed.getLeft(), dualSpeed.getRight(), 0, 0);
                }
            }).start();

            joystickHandler.postDelayed(this, 40);
        }
    };

    void init() {

        dataBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);

        new Thread(new Runnable() {
            @Override
            public void run() {
                connect();
            }
        }).start();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if(socketClient != null) {
            try {
                socketClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socketClient = null;
        }

        joystickHandler.removeCallbacks(runnableCode);
    }

    void connect() {
        try {
            Socket socket = new Socket(InetAddress.getByName(SERVER_NAME), SERVER_PORT);
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
                socketClient.getOutputStream().write(dataBuffer.array());
                socketClient.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
