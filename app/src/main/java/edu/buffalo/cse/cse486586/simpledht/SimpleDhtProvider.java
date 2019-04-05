package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    public static final String TAG = SimpleDhtProvider.class.getSimpleName();
    public static final String[] REMOTE_PORTS = new String[]{"11108", "11112", "11116", "11120", "11124"};
    public static final String MASTER_NODE = "11108";
    public static final int SERVER_PORT = 10000;
    public static String CURRENT_NODE;
    public static String CURRENT_NODE_HASH;

    public static String PREDECESSOR_NODE;
    public static String PREDECESSOR_NODE_HASH;
    public static String SUCCESSOR_NODE;
    public static String SUCCESSOR_NODE_HASH;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        CURRENT_NODE = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.d(TAG, "Current node is: " + CURRENT_NODE);

        CURRENT_NODE_HASH = genHash(String.valueOf(Integer.valueOf(CURRENT_NODE) / 2));


//        mySQLiteOpenHelper = new MySQLiteOpenHelper(context, DB_NAME, null, DB_VERSION);
//        db = mySQLiteOpenHelper.getWritableDatabase();


        // new a ServerTask()
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
            new JoinChordAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
        }

        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private static String genHash(String input) {
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    public static void forwardJoinPortRequest(String receivedMessage) {
        new ClientAsyncTask(SUCCESSOR_NODE, receivedMessage, Util.MessageType.MESSAGE_TYPE_JOIN_CHORD).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }


    public static class JoinChordAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (!CURRENT_NODE.equals(String.valueOf(MASTER_NODE))) {
                Log.d(TAG, "Sending request to MASTER_NODE for joining the DHT.");
                Socket socket;
                PrintWriter printWriter;

                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.valueOf(MASTER_NODE));
                    printWriter = new PrintWriter(socket.getOutputStream(), true);
                    printWriter.println(Util.createJoinChordMessage(CURRENT_NODE));
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String receivedMessage = bufferedReader.readLine();
                    if (Util.getMessageType(receivedMessage) == Util.MessageType.MESSAGE_TYPE_JOIN_CHORD_ACK) {
                        bufferedReader.close();
                        printWriter.close();
                        socket.close();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public static class ClientAsyncTask extends AsyncTask<Void, Void, Void> {

        String recipient;
        String message;
        Util.MessageType messageType;

        public ClientAsyncTask(String recipient, String message, Util.MessageType messageType) {
            this.recipient = recipient;
            this.message = message;
            this.messageType = messageType;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (messageType.equals(Util.MessageType.MESSAGE_TYPE_JOIN_CHORD)) {
                Log.d(TAG, "Forwarding request to SUCCESSOR_NODE for joining the DHT.");
                Socket socket;
                PrintWriter printWriter;

                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.valueOf(recipient));
                    printWriter = new PrintWriter(socket.getOutputStream(), true);
                    printWriter.println(message);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String receivedMessage = bufferedReader.readLine();
                    if (Util.getMessageType(receivedMessage) == Util.MessageType.MESSAGE_TYPE_JOIN_CHORD_ACK) {
                        bufferedReader.close();
                        printWriter.close();
                        socket.close();
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public static class ForwardRequestAsyncTask extends AsyncTask<Void, Void, Void> {

        String messageToForward;

        public ForwardRequestAsyncTask(String messageToForward) {
            this.messageToForward = messageToForward;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Forwarding request to SUCCESSOR_NODE for joining the DHT.");
            Socket socket;
            PrintWriter printWriter;

            try {
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.valueOf(SUCCESSOR_NODE));
                printWriter = new PrintWriter(socket.getOutputStream(), true);
                printWriter.println(messageToForward);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String receivedMessage = bufferedReader.readLine();
                if (Util.getMessageType(receivedMessage) == Util.MessageType.MESSAGE_TYPE_JOIN_CHORD_ACK) {
                    bufferedReader.close();
                    printWriter.close();
                    socket.close();
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class ServerAsyncTask extends AsyncTask<ServerSocket, Void, Void> {
        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            while (true) {
                Socket incomingSocket;
                BufferedReader bufferedReader;
                try {
                    incomingSocket = serverSockets[0].accept();
                    bufferedReader = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
                    String receivedMessage = bufferedReader.readLine();
                    switch (Util.getMessageType(receivedMessage)) {
                        case MESSAGE_TYPE_JOIN_CHORD:
                            String requestingPort = receivedMessage.split(Util.JOIN_CHORD_SEPARATOR)[1];
                            String requestingPortHash = genHash(requestingPort);
                            PrintWriter printWriter = new PrintWriter(incomingSocket.getOutputStream(), true);
                            printWriter.println(Util.JOIN_CHORD_ACK);
                            printWriter.close();
                            bufferedReader.close();
                            incomingSocket.close();
                            if (shouldNodeBeAddedAsSuccessor(requestingPortHash)) {
                                processIncomingJoinChordMessage(requestingPort, requestingPortHash);
                            } else {
                                forwardJoinPortRequest(receivedMessage);
                            }
                    }
                } catch (IOException e) {

                }
            }
        }
    }

    public static void processIncomingJoinChordMessage(String requestingPort, String requestingPortHash) {

        if (CURRENT_NODE.equals(MASTER_NODE)) {
            if (PREDECESSOR_NODE.equals(CURRENT_NODE) && SUCCESSOR_NODE.equals(CURRENT_NODE)) {
                // First node is requesting.
                PREDECESSOR_NODE = requestingPort;
                PREDECESSOR_NODE_HASH = requestingPortHash;
                SUCCESSOR_NODE = requestingPort;
                SUCCESSOR_NODE_HASH = requestingPortHash;
            }

        }
//        if (genHash(requestingPort).compareTo(genHash()))
    }

    public static boolean shouldNodeBeAddedAsSuccessor(String requestingPortHash) {
        if (requestingPortHash.compareTo(PREDECESSOR_NODE_HASH) > 0 && requestingPortHash.compareTo(CURRENT_NODE) < 0) {
            // This requesting port must be added as predecessor.
            return true;
        } else if (requestingPortHash.compareTo(PREDECESSOR_NODE_HASH) > 0 && requestingPortHash.compareTo(CURRENT_NODE) > 0) {
            if (PREDECESSOR_NODE_HASH.compareTo(CURRENT_NODE_HASH) > 0) {
                return true;
            } else {
                return false;
            }
        } else if (requestingPortHash.compareTo(PREDECESSOR_NODE_HASH) < 0 && requestingPortHash.compareTo(CURRENT_NODE) < 0) {
            if (PREDECESSOR_NODE_HASH.compareTo(CURRENT_NODE_HASH) < 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}