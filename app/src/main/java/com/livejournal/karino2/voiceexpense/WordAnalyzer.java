package com.livejournal.karino2.voiceexpense;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by karino on 7/8/15.
 */
public class WordAnalyzer {
    ArrayList<String> categories;

    public WordAnalyzer(ArrayList<String> cats) {
        categories =  cats;
    }

    public boolean isDate(String word) {
        return false;
    }
    public Date toDate(String word) {
        return null;
    }

    public boolean contains(String word, ArrayList<String> candidates) {
        return candidates.contains(word);
    }

    public boolean isCategory(String word) {
        return contains(word, categories);
    }
}
