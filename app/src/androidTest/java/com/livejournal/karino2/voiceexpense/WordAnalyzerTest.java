package com.livejournal.karino2.voiceexpense;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;

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
    @Override
    protected void setUp() {
        target = new WordAnalyzer(getCategories());
    }

    public void testTokenize() {
        String[] tokens = normalEntry.split(" ");
        assertEquals(4, tokens.length);
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

}
