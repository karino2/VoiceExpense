package com.livejournal.karino2.voiceexpense;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Created by karino on 7/11/15.
 */
public class SpeechWatcher {
    public interface StatusListener {
        void onStartWaitSpeech();
        void onWaitSpeechError(int errorno);
        void onResult(ArrayList<String> results);

    }

    Context context;
    StatusListener statusListener;

    public SpeechWatcher(Context ctx, StatusListener listener) {
        context = ctx;
        statusListener = listener;
        state = State.INIT;
    }

    enum State {
        INIT,
        NOT_LISTENING,
        WAIT_SPEECH_READY,
        SPEECH_READY
    }

    State state;


    SpeechRecognizer recognizer;
    RecognitionListener recognitionListener;

    public void setUp() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                log("onReady");
                statusListener.onStartWaitSpeech();
                state = State.SPEECH_READY;
            }

            @Override
            public void onBeginningOfSpeech() {
                log("onBeginning");

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Log.d("VoiceExpense", "onRms");
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                log("onBuf");
            }

            @Override
            public void onEndOfSpeech() {
                log("EndOfSpeech");

            }

            @Override
            public void onError(int error) {
                log("onError");
                state = State.NOT_LISTENING;
                statusListener.onWaitSpeechError(error);
            }

            @Override
            public void onResults(Bundle results) {
                state = State.NOT_LISTENING;
                ArrayList<String> reses = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                statusListener.onResult(reses);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                log("onPartialResults");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                log("onEvent");
            }
        };
        recognizer.setRecognitionListener(recognitionListener);
        state = State.NOT_LISTENING;

    }

    public void tearDown() {
        if(recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
            // setVoiceButtonChecked(false);
        }
        state = State.INIT;
    }

    void log(String msg) {
        // Log.d("VoiceExpense", msg);
    }

    Handler handler = new Handler();
    boolean startRegistered = false;


    public void startListening() {
        startRegistered = true;
        state = State.WAIT_SPEECH_READY;

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(startRegistered) {
                    state = State.WAIT_SPEECH_READY;
                    recognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
                }
                startRegistered = false;

            }
        }, 800);

    }

    public boolean isWaitSpeech() {
        return state == State.SPEECH_READY ||
                state == State.WAIT_SPEECH_READY;
    }

    // not notify now.
    public void stopListening() {
        if(isWaitSpeech())
        {
            recognizer.stopListening();
        }
        state = State.NOT_LISTENING;
    }

}
