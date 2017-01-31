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
    Pattern priceAltPat = Pattern.compile("^¥([0-9]+)");
    Pattern fullDatePat = Pattern.compile("^([0-9]+)年([0-9]+)月([0-9]+)日");
    Pattern fullDateAltPat = Pattern.compile("^([0-9]+)[/\\.]([0-9]+)[/\\.]([0-9]+)");
    Pattern monthDatePat = Pattern.compile("^([0-9]+)月([0-9]+)日");
    Pattern monthDateAltPat = Pattern.compile("^([0-9]+)dec");
    Pattern dateOnlyPat = Pattern.compile("^([0-9]+)日");
    Pattern subtractPat = Pattern.compile("^(ひく|引く|マイナス|-)");
    Pattern categorySeparatorPat = Pattern.compile("¥?[0-9]+");

    int findCategorySeparator(String word) {
        Matcher matcher = categorySeparatorPat.matcher(word);
        if(!matcher.find()) {
            return word.length();
        }
        return matcher.start();
    }


    ArrayList<String> categories;
    Date baseDate;

    public WordAnalyzer(ArrayList<String> cats, Date baseDt) {
        categories =  cats;
        baseDate = baseDt;
    }

    public boolean isDate(String word) {
        return isMatch(word, fullDatePat) ||
                isMatch(word, fullDateAltPat) ||
                isMatch(word, monthDatePat) ||
                isMatch(word, monthDateAltPat) ||
                isMatch(word, dateOnlyPat);
    }

    int remaining = -1;
    public int remainingPos() {
        return remaining;
    }

    public Date toDate(String word) {
        if(isMatch(word, fullDatePat)) {
            Matcher matcher = match(word, fullDatePat);
            return new Date(getMatchedInt(word, matcher, 1) - 1900,
                    getMatchedInt(word, matcher, 2) - 1,
                    getMatchedInt(word, matcher, 3));

        }else if(isMatch(word, fullDateAltPat)) {
            Matcher matcher = match(word, fullDateAltPat);
            return new Date(getMatchedInt(word, matcher, 1) - 1900,
                    getMatchedInt(word, matcher, 2) - 1,
                    getMatchedInt(word, matcher, 3));
        } else if(isMatch(word, monthDatePat)) {
            Matcher matcher = match(word, monthDatePat);
            return new Date(baseDate.getYear(),
                    getMatchedInt(word, matcher, 1)-1,
                    getMatchedInt(word, matcher, 2));

        } else if(isMatch(word, monthDateAltPat)) {
            Matcher matcher = match(word, monthDateAltPat);
            return new Date(baseDate.getYear(),
                    12-1, // now only support december.
                    getMatchedInt(word, matcher, 1));
        }
        Matcher matcher = match(word, dateOnlyPat);
        return new Date(baseDate.getYear(),
                baseDate.getMonth(),
                getMatchedInt(word, matcher, 1));
    }
    public boolean isPrice(String word)
    {
        return isMatch(word, pricePat) || isMatch(word, priceAltPat);
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
        if(isMatch(word, pricePat)) {
            Matcher matcher = match(word, pricePat);

            return getMatchedInt(word, matcher, 1);
        } else{
            Matcher matcher = match(word, priceAltPat);

            return getMatchedInt(word, matcher, 1);

        }
    }

    private int getMatchedInt(String word, Matcher matcher, int groupId) {
        return Integer.parseInt(word.substring(matcher.start(groupId), matcher.end(groupId)));
    }


    public boolean contains(String word, ArrayList<String> candidates) {
        return candidates.contains(word);
    }

    public static class CategoryResult {
        public String matchedCategory;
        boolean matched = false;

        public boolean isMatched() {
            return matched;
        }

        int tokenLen = 0;
        public int matchedTokenLen() {
            return tokenLen;
        }

        public void setResult(String category, int tokenLength) {
            matched = true;
            matchedCategory = category;
            tokenLen = tokenLength;
        }
    }

    CategoryResult categoryResult = new CategoryResult();

    public CategoryResult findCategory(String word) {
        categoryResult.matched = false;

        for(String cat : categories) {
            if(word.startsWith(cat)) {
                categoryResult.setResult(cat, cat.length());
                return categoryResult;
            }
        }

        // support tail match.
        int sep = findCategorySeparator(word);
        if(sep < 2)
            return categoryResult;

        String categoryCand = word.substring(0, sep);
        for(String cat : categories) {
            if(cat.endsWith(categoryCand)) {
                categoryResult.setResult(cat, categoryCand.length());
                return categoryResult;
            }
        }


        return categoryResult;
    }

    public boolean isCategory(String word) {
        return findCategory(word).isMatched();
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
