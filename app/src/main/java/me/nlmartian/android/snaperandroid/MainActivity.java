package me.nlmartian.android.snaperandroid;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    helloText.setText((String) msg.obj);
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private IRemoteService remoteService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remoteService = IRemoteService.Stub.asInterface(service);
            try {
                int pid = remoteService.getPid();
                Log.d("PID", "IRemoteService:" + pid);
                Log.d("PID", "IRemoteService:" + android.os.Process.myPid());
                remoteService.registerCallback(remoteServiceCallback);
            } catch (DeadObjectException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
        }
    };

    private MessageHandler msgHandler = new MessageHandler();

    private IRemoteServiceCallback remoteServiceCallback = new IRemoteServiceCallback.Stub() {
        @Override
        public void messageReceived(String msg) throws RemoteException {
            msgHandler.sendMessage(msgHandler.obtainMessage(1, msg));
        }
    };

    private TextView helloText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        helloText = (TextView) findViewById(R.id.hello);
        helloText.setMovementMethod(new ScrollingMovementMethod());

//        initClient();
        initSocketIOClient();
    }

    @Override
    protected void onDestroy() {
        try {
            remoteService.unregisterCallback(remoteServiceCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initClient() {
        startService(MessageService.startIntent(this));
        bindService(MessageService.startIntent(this), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initSocketIOClient() {
        if (!App.isServiceRun(this, "me.nlmartian.android.snaperandroid.MessageService2")) {
            startService(MessageService2.startIntent(this));
        }
        bindService(MessageService2.startIntent(this), serviceConnection, Context.BIND_AUTO_CREATE);
    }
}
