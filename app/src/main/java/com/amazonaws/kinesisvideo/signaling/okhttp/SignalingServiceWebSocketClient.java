package com.amazonaws.kinesisvideo.signaling.okhttp;

import android.util.Base64;
import android.util.Log;

import com.amazonaws.kinesisvideo.signaling.SignalingListener;
import com.amazonaws.kinesisvideo.signaling.model.Message;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Signaling service client based on websocket.
 */

public class SignalingServiceWebSocketClient {

    private static final String TAG = "SignalingServiceWebSocketClient";

    private final WebSocketClient websocketClient;

    private final ExecutorService executorService;

    private final Gson gson = new Gson();

    public SignalingServiceWebSocketClient(final String uri, final SignalingListener signalingListener,
                                           final ExecutorService executorService) {
        Log.d(TAG, "Connecting to URI " + uri + " as master");
        this.executorService = executorService;
        websocketClient = new WebSocketClient(uri, signalingListener);
    }

    public boolean isOpen() {
        return websocketClient.isOpen();
    }

    public void sendSdpOffer(final Message offer) {
        executorService.submit(() -> {
            if (offer.getAction().equalsIgnoreCase("SDP_OFFER")) {
                Log.d(TAG, "Sending Offer");
                send(offer);
            }
        });
    }

    public void sendSdpAnswer(final Message answer) {
        executorService.submit(() -> {
            if (answer.getAction().equalsIgnoreCase("SDP_ANSWER")) {
                Log.d(TAG, "Answer sent " + new String(Base64.decode(answer.getMessagePayload().getBytes(),
                        Base64.NO_WRAP | Base64.URL_SAFE)));
                send(answer);
            }
        });
    }

    public void sendIceCandidate(final Message candidate) {
        executorService.submit(() -> {
            if (candidate.getAction().equalsIgnoreCase("ICE_CANDIDATE")) {
                send(candidate);
            }
            Log.d(TAG, "Sent Ice candidate message");
        });
    }

    public void disconnect() {
        executorService.submit(websocketClient::disconnect);
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Error in disconnect");
        }
    }

    private void send(final Message message) {
        final String jsonMessage = gson.toJson(message);
        Log.d(TAG, "Sending JSON Message= " + jsonMessage);
        websocketClient.send(jsonMessage);
        Log.d(TAG, "Sent JSON Message= " + jsonMessage);
    }

}
