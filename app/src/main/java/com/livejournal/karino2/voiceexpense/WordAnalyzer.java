package com.livejournal.karino2.voiceexpense;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by karino on 7/8/15.
 */
public class WordAnalyzer {
    Pattern pricePat = Pattern.compile("(^[0-9]+)円$");
    Pattern fullDatePat = Pattern.compile("^([0-9]+)年([0-9]+)月([0-9]+)日");
    Pattern monthDatePat = Pattern.compile("^([0-9]+)月([0-9]+)日");
    Pattern dateOnlyPat = Pattern.compile("^([0-9]+)日");


    ArrayList<String> categories;
    Date baseDate;

    public WordAnalyzer(ArrayList<String> cats, Date baseDt) {
        categories =  cats;
        baseDate = baseDt;
    }

    public boolean isDate(String word) {
        return isMatch(word, fullDatePat) ||
                isMatch(word, monthDatePat) ||
                isMatch(word, dateOnlyPat);
    }

    public Date toDate(String word) {
        if(isMatch(word, fullDatePat)) {
            Matcher matcher = fullDatePat.matcher(word);
            matcher.find();
            return new Date(getMatchedInt(word, matcher, 1),
                    getMatchedInt(word, matcher, 2),
                    getMatchedInt(word, matcher, 3));

        } else if(isMatch(word, monthDatePat)) {
            Matcher matcher = monthDatePat.matcher(word);
            matcher.find();
            return new Date(baseDate.getYear(),
                    getMatchedInt(word, matcher, 1),
                    getMatchedInt(word, matcher, 2));

        }
        Matcher matcher = dateOnlyPat.matcher(word);
        matcher.find();
        return new Date(baseDate.getYear(),
                baseDate.getMonth(),
                getMatchedInt(word, matcher, 1));
    }
    public boolean isPrice(String word)
    {
        return isMatch(word, pricePat);
    }

    private boolean isMatch(String word, Pattern pat) {
        Matcher matcher = pat.matcher(word);
        return matcher.lookingAt();
    }

    public int toPrice(String word)
    {
        Matcher matcher = pricePat.matcher(word);
        matcher.find();

        return getMatchedInt(word, matcher, 1);
    }

    private int getMatchedInt(String word, Matcher matcher, int groupId) {
        return Integer.parseInt(word.substring(matcher.start(groupId), matcher.end(groupId)));
    }


    public boolean contains(String word, ArrayList<String> candidates) {
        return candidates.contains(word);
    }

    public boolean isCategory(String word) {
        return contains(word, categories);
    }

    public ArrayList<String> tokenize(String fullEntry) {
        ArrayList<String> res = new ArrayList<>();

        // special handling for last "NEXT" because of SpeechRecognizer behavior.
        if(fullEntry.length() > 2 && fullEntry.endsWith("次") && !fullEntry.endsWith(" 次")) {
            String[] tokens1 = fullEntry.substring(0, fullEntry.length()-1).split(" ");
            res.addAll(Arrays.asList(tokens1));
            res.add("次");
            return res;
        }
        res.addAll(Arrays.asList(fullEntry.split(" ")));
        return res;
    }
}
