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

    public  interface StopStreamCallback{
        void execute();
    }

    private Callback callback;
    private StopStreamCallback stopStreamCallback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    public void setStopStreamCallback(StopStreamCallback callback) {
        this.stopStreamCallback = callback;
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

        this.hubConnection.on("StopStream", () -> {
            Log.e(TAG, "stream stopped");
            stopStreamCallback.execute();
        });
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
