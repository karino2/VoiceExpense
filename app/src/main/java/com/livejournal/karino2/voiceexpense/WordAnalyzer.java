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
    Pattern pricePat = Pattern.compile("(^[0-9]+)(円|en|園)");
    Pattern fullDatePat = Pattern.compile("^([0-9]+)年([0-9]+)月([0-9]+)日");
    Pattern monthDatePat = Pattern.compile("^([0-9]+)月([0-9]+)日");
    Pattern dateOnlyPat = Pattern.compile("^([0-9]+)日");
    Pattern subtractPat = Pattern.compile("^(ひく|引く|マイナス|-)");


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

    int remaining = -1;
    public int remainingPos() {
        return remaining;
    }

    public Date toDate(String word) {
        if(isMatch(word, fullDatePat)) {
            Matcher matcher = match(word, fullDatePat);
            return new Date(getMatchedInt(word, matcher, 1)-1900,
                    getMatchedInt(word, matcher, 2)-1,
                    getMatchedInt(word, matcher, 3));

        } else if(isMatch(word, monthDatePat)) {
            Matcher matcher = match(word, monthDatePat);
            return new Date(baseDate.getYear(),
                    getMatchedInt(word, matcher, 1)-1,
                    getMatchedInt(word, matcher, 2));

        }
        Matcher matcher = match(word, dateOnlyPat);
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

    Matcher match(String word, Pattern pat) {
        Matcher matcher = pat.matcher(word);
        matcher.find();
        remaining = matcher.end();
        return matcher;
    }

    public int toPrice(String word)
    {
        Matcher matcher = match(word, pricePat);

        return getMatchedInt(word, matcher, 1);
    }

    private int getMatchedInt(String word, Matcher matcher, int groupId) {
        return Integer.parseInt(word.substring(matcher.start(groupId), matcher.end(groupId)));
    }


    public boolean contains(String word, ArrayList<String> candidates) {
        return candidates.contains(word);
    }

    public String findCategory(String word) {
        for(String cat : categories) {
            if(word.startsWith(cat))
                return cat;
        }
        return "";
    }

    public boolean isCategory(String word) {
        return (findCategory(word).length() != 0);
    }

    public ArrayList<String> tokenize(String fullEntry) {
        return new ArrayList<>(Arrays.asList(fullEntry.split(" ")));
    }

    public void setBaseDate(Date baseDate) {
        this.baseDate = baseDate;
    }

    public boolean isSubtract(String word) {
        return isMatch(word, subtractPat);
    }

    public int subtractRemainingPos(String word) {
        match(word, subtractPat);
        return remainingPos();
    }
}
