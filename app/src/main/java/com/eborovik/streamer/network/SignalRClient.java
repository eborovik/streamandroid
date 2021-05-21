package com.eborovik.streamer.network;

import android.util.Log;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;

public class SignalRClient
{
    private static final String TAG = SignalRClient.class.getSimpleName();
    HubConnection hubConnection;

    public  interface Callback<String>{
        void execute(String param);
    }

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public SignalRClient(String url)
    {
        this.hubConnection = HubConnectionBuilder.create(url).build();
        this.handleIncomingMethods();
    }
    private void handleIncomingMethods()
    {
        this.hubConnection.on("StartRecording", (streamUrl) -> {
            Log.e(TAG, streamUrl);
            callback.execute(streamUrl);
        }, String.class);
    }

    private void register(String streamId) {
        this.hubConnection.send("Register", streamId);
    }

    public void start(String streamId)
    {
        this.callback = callback;
        try {
            this.hubConnection.start().blockingAwait();
            register(streamId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void stop()
    {
        this.hubConnection.stop().blockingAwait();
    }
}
