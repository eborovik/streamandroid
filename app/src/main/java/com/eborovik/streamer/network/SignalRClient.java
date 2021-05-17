package com.eborovik.streamer.network;

import android.content.Context;
import android.util.Log;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

public class SignalRClient
{
    private static final String TAG = SignalRClient.class.getSimpleName();
    HubConnection hubConnection;
    private Context context;

    public SignalRClient(String url)
    {
        this.hubConnection = HubConnectionBuilder.create(url).build();
        this.handleIncomingMethods();
    }
    private void handleIncomingMethods()
    {
        this.hubConnection.on("ReceiveMessage", (user, message) -> { // OK
            Log.e(TAG, user + ": " + message);
        }, String.class, String.class);
    }

    public void start()
    {
        try {
            this.hubConnection.start().blockingAwait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void stop()
    {
        this.hubConnection.stop().blockingAwait();
    }
}
