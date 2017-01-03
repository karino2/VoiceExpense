package com.livejournal.karino2.voiceexpense;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by karino on 7/11/15.
 */
public class SpeechParser {
    WordAnalyzer wordAnalyzer;
    OnActionListener actionListener;

    public SpeechParser(WordAnalyzer analyzer, OnActionListener listener) {
        wordAnalyzer = analyzer;
        actionListener = listener;
    }

    StringBuffer otherBuf;
    public void parseEntry(String entry) {
        otherBuf = new StringBuffer();
        ArrayList<String> tokens = wordAnalyzer.tokenize(entry);
        for(String token : tokens) {
            parseToken(token);
        }

        actionListener.actionOther(otherBuf.toString());
    }

    public void setBaseDate(Date baseDate) {
        wordAnalyzer.setBaseDate(baseDate);
    }

    public interface OnActionListener {
        void actionDate(Date dt);
        void actionPrice(int price);
        void actionCategory(String categoryName);
        void actionSubtractMode();
        void actionOther(String other);
    }

    private void parseToken(String token) {
        while (token.length() > 0) {
            if (wordAnalyzer.isDate(token)) {
                Date dt = wordAnalyzer.toDate(token);
                actionListener.actionDate(dt);
                token = token.substring(wordAnalyzer.remainingPos());
            } else if (wordAnalyzer.isPrice(token)) {
                actionListener.actionPrice(wordAnalyzer.toPrice(token));
                token = token.substring(wordAnalyzer.remainingPos());
            } else if (wordAnalyzer.isCategory(token)) {
                WordAnalyzer.CategoryResult cat = wordAnalyzer.findCategory(token);
                actionListener.actionCategory(cat.matchedCategory);
                token = token.substring(cat.matchedTokenLen());
            } else if (wordAnalyzer.isSubtract(token)) {
                actionListener.actionSubtractMode();

                token = token.substring(wordAnalyzer.subtractRemainingPos(token));
            } else {
                if(otherBuf.length() != 0) {
                    otherBuf.append(" ");
                }
                otherBuf.append(token);
                return;
            }
        }
    }
}
