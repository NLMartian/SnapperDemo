// IRemoteService.aidl
package me.nlmartian.android.snaperandroid;

// Declare any non-default types here with import statements
import me.nlmartian.android.snaperandroid.IRemoteServiceCallback;

interface IRemoteService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    int getPid();

    void registerCallback(IRemoteServiceCallback cb);

    void unregisterCallback(IRemoteServiceCallback cb);
}
