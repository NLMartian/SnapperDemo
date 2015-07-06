package me.nlmartian.android.snaperandroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.engineio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import me.nlmartian.android.snaperandroid.util.Logger;

/**
 * Created by nlmartian on 5/21/15.
 */
public class MessageService2 extends Service {

    private static final String CATEGORY_NOTIFICATION = "category_notification";

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                broadcastMessage((String) msg.obj);
            } else if (msg.what == 2) {
                try {
                    sendNotification((JSONArray) msg.obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            super.handleMessage(msg);
        }
    }

    public class Binder extends android.os.Binder {
        public MessageService2 getService() {
            return MessageService2.this;
        }
    }

    public static final String TAG = MessageService2.class.getSimpleName();

    public static final String TOKEN = "";

    public static final String ACTION_CONNECT = "com.teambition.talk.service.ACTION_CONNECT";

    private final IBinder binder = new Binder();

    final RemoteCallbackList<IRemoteServiceCallback> callbacks
            = new RemoteCallbackList<>();

    NotificationManager mNM;

    private MessageHandler messageHandler = new MessageHandler();

    public static Intent startIntent(Context context) {
        Intent intent = new Intent(context, MessageService2.class);
        intent.setAction(ACTION_CONNECT);
        return intent;
    }

    public final IRemoteService.Stub remoteBinder = new IRemoteService.Stub() {

        @Override
        public int getPid() throws RemoteException {
            return android.os.Process.myPid();
        }

        @Override
        public void registerCallback(IRemoteServiceCallback cb) throws RemoteException {
            if (cb != null) {
                callbacks.register(cb);
            }
        }

        @Override
        public void unregisterCallback(IRemoteServiceCallback cb) throws RemoteException {
            if (cb != null) {
                callbacks.unregister(cb);
            }
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        showNotification("Start!");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                App.runWatchDog(android.os.Process.myPid());
            }
        }, 1000);
    }

    @Override
    public IBinder onBind(Intent intent) {
//        return binder;
        return remoteBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "service");
        wakeLock.acquire();

        try {
            Socket.Options options = new Socket.Options();
            options.path = "/websocket";
            options.transports = new String[]{"websocket"};
            String query = "token=" + TOKEN;
            options.query = query;
            options.port = 443;
            options.secure = false;
            final Socket socket = new Socket("wss://push.teambition.net", options);
            socket.on(Socket.EVENT_OPEN, new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    messageHandler.sendMessage(messageHandler.obtainMessage(1, "open"));
                    Logger.d(TAG, "open");
                }
            }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String data = (String) args[0];
                    messageHandler.sendMessage(messageHandler.obtainMessage(1, data));
                    try {
                        JSONObject jsonMsg = new JSONObject(data);
                        int id = jsonMsg.optInt("id");
                        socket.send(String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"result\":\"OK\"}", id));

                        JSONArray params = jsonMsg.optJSONArray("params");
                        if (params != null) {
                            messageHandler.sendMessage(messageHandler.obtainMessage(2, params));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Logger.d(TAG, "message:" + data);
                }
            }).on(Socket.EVENT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... objects) {
                    Exception err = (Exception) objects[0];
                    messageHandler.sendMessage(messageHandler.obtainMessage(1, err.toString()));
                    Logger.e(TAG, "error", err);

                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    socket.open();
                }
            });
            socket.open();

        } catch (Exception e) {
            e.printStackTrace();
        }

        wakeLock.release();
        return START_REDELIVER_INTENT;
    }

    private void broadcastMessage(String msg) {
        int N = callbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                callbacks.getBroadcastItem(i).messageReceived(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        callbacks.finishBroadcast();
    }

    private void showNotification(String text) {

        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        notification.setLatestEventInfo(this, getText(R.string.hello_world),
                text, contentIntent);

        mNM.notify(R.string.hello_world, notification);
    }
    
    private void sendNotification(JSONArray array) {
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.addCategory(CATEGORY_NOTIFICATION);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);

        /* Adds the Intent that starts the Activity to the top of the stack */
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(getString(R.string.app_name));
        inboxStyle.setSummaryText("new message");
        inboxStyle.addLine(array.toString());
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.app_name))//设置通知栏标题
                .setStyle(inboxStyle)
                .setContentText(array.toString())
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent)
                .setNumber(0)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setSmallIcon(R.mipmap.ic_launcher);

        mNotificationManager.notify(90890, mBuilder.build());
    }
}
