// IRemoteServiceCallback.aidl
package me.nlmartian.android.snaperandroid;

// Declare any non-default types here with import statements

oneway interface IRemoteServiceCallback {
    void messageReceived(String msg);
}
