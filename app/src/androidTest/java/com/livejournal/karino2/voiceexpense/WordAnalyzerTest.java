package com.livejournal.karino2.voiceexpense;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by karino on 7/9/15.
 */
public class WordAnalyzerTest extends TestCase {
    String[] categoriesArray = new String[] {
      "図書研究費", "接待交際費","旅費交通費", "雑費", "消耗品費", "租税公課",
            "通信費", "会議費", "医療費"
    };
    String normalEntry = "接待交際費 7月8日 210円 次";

    WordAnalyzer target;
    Date baseDate;
    @Override
    protected void setUp() {
        baseDate = new Date(2015, 7, 8);
        target = new WordAnalyzer(getCategories(), baseDate);
    }

    public void testTokenize() {
        assertEquals(4, target.tokenize(normalEntry).size());
    }

    public void testTokenize_SpecialHandlingOfNext() {
        assertEquals(4, target.tokenize("接待交際費 7月8日 210円次").size());
    }

    ArrayList<String> getCategories() {
        return new ArrayList<String>(Arrays.asList(categoriesArray));
    }

    public void testContainWord() {
        assertTrue(target.contains("接待交際費", getCategories()));
        assertFalse(target.contains("7月8日", getCategories()));
    }

    public void testIsCategory() {
        assertTrue(target.isCategory("接待交際費"));
        assertFalse(target.isCategory("7月8日"));
    }

    public void testIsPrice() {
        assertTrue(target.isPrice("720円"));
        assertFalse(target.isPrice("8日"));
        assertFalse(target.isPrice("720"));
        assertFalse(target.isPrice("これは720円"));
    }

    public void testToPrice() {
        assertEquals(720, target.toPrice("720円"));
    }

    public void testIsDate() {
        assertTrue(target.isDate("7月8日"));
        assertTrue(target.isDate("8日"));
        assertFalse(target.isDate("接待交際費"));
        assertFalse(target.isDate("720円"));
    }

    public void testToDate() {
        assertDateEqual(baseDate.getYear(), 7, 8, target.toDate("7月8日"));
        assertDateEqual(2013, 7, 8, target.toDate("2013年7月8日"));
    }

    public void assertDateEqual(int year, int month, int day, Date dt) {
        assertEquals(year, dt.getYear());
        assertEquals(month, dt.getMonth());
        assertEquals(day, dt.getDate());
    }
}