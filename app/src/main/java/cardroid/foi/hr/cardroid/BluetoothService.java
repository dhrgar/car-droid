package cardroid.foi.hr.cardroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.HeadersOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdRawCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.SpacesOffCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Hrgar on 7.6.2016..
 */
public class BluetoothService {

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "BluetoothService";
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;
    private int mState;
  //  private AcceptThread mSecureAcceptThread;
   // private AcceptThread mInsecureAcceptThread;

    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private static final String NAME = "BluetoothSecure";

    public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    public synchronized int getState() {
        return mState;
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MessageHandlingConstants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
           mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }

    }


    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);


        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }





    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket = null;
        private final BluetoothDevice mmDevice;



        public ConnectThread(BluetoothDevice device) {
                   mmDevice = device;
            try {
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
        }


        public void run() {

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }

                return;
            }

            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }


            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {

                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME,
                            MY_UUID);

            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }



    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            ObdResetCommand obdResetCommand = new ObdResetCommand();

           try {
               new ObdRawCommand("ATD").run(mmInStream, mmOutStream);
               obdResetCommand.run(mmInStream, mmOutStream);
               Log.i("RESET COMMAND:",obdResetCommand.getFormattedResult());
               Log.i("RESET COMMAND NAME:",obdResetCommand.getName());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
               e.printStackTrace();
           }

            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
            try {


                new EchoOffCommand().run(mmInStream, mmOutStream);
                new LineFeedOffCommand().run(mmInStream, mmOutStream);
                new SpacesOffCommand().run(mmInStream, mmOutStream);
                new HeadersOffCommand().run(mmInStream, mmOutStream);
                new TimeoutCommand(200).run(mmInStream, mmOutStream);
                new SelectProtocolCommand(ObdProtocols.AUTO).run(mmInStream, mmOutStream);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            RPMCommand engineRpmCommand = new RPMCommand();
            SpeedCommand speedCommand = new SpeedCommand();

            // Keep listening to the InputStream while connected

            while (!Thread.currentThread().isInterrupted()) {
                try {

/*
                    byte b = 0;
                    StringBuilder res = new StringBuilder();
                    char c;
                    // -1 if the end of the stream is reached
                    while (((b = (byte) mmInStream.read()) > -1)) {
                        c = (char) b;
                        if (c == '>') // read until '>' arrives
                        {
                            break;
                        }
                        res.append(c);
                    }
*/
                    engineRpmCommand.run(mmInStream, mmOutStream);
                    speedCommand.run(mmInStream, mmOutStream);
                    // TODO handle commands result
                    Log.d(TAG, "RPM: " + engineRpmCommand.getFormattedResult());
                    Log.d(TAG, "Speed: " + speedCommand.getFormattedResult());

                    // Read from the InputStream
/*
                    rawData = res.toString().replaceAll("SEARCHING", "");

                    rawData = rawData.replaceAll("\\s", "");

                    Log.i("RESPONSEEE: ", rawData);
                    // bytes = mmInStream.read(rawData.getBytes());

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MessageHandlingConstants.MESSAGE_READ, rawData.getBytes().length, -1, rawData.getBytes())
                            .sendToTarget();
                            */
                    sleep(100);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    //connectionLost();
                    // Start the service over to restart listening mode
                  //  BluetoothChatService.this.start();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

    }


}
