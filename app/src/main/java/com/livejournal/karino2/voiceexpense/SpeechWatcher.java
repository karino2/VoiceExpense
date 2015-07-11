package com.livejournal.karino2.voiceexpense;

import android.content.Context;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by karino on 7/11/15.
 */
public class SpeechWatcher {
    public interface StatusListener {
        void onStartWaitSpeech();
        void onWaitSpeechError();
        void onResult(ArrayList<String> results);

    }

    Context context;
    StatusListener statusListener;

    public SpeechWatcher(Context ctx, StatusListener listener) {
        context = ctx;
        statusListener = listener;
    }

    SpeechRecognizer recognizer;
    RecognitionListener recognitionListener;

    public void setUp() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                log("onReady");
                statusListener.onStartWaitSpeech();

                /*
                showMessage("OnReady for speech");
                setEndVoiceEnabled(true);
                */
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
                statusListener.onWaitSpeechError();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> reses = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                statusListener.onResult(reses);
                // startListening();
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

    }

    public void tearDown() {
        if(recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
            // setVoiceButtonChecked(false);
        }
    }

    void log(String msg) {
        Log.d("VoiceExpense", msg);
    }

    public void startListening() {
        recognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(context));
    }

    // not notify now.
    public void stopListening() {
        if(recognizer != null)
        {
            recognizer.stopListening();
        }
    }

}
