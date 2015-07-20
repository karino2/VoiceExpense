package com.livejournal.karino2.voiceexpense;


import java.util.Date;
import java.util.Hashtable;


import junit.framework.TestCase;


public class CsvImporterTest extends TestCase {
	
	long returnId;
	String argCategory;

	@Override
	public void setUp()
	{
		returnId = -1;
		argCategory = null;
	}
	
	
	EntryStorable createStorableMock()
	{
		return new EntryStorable() {
			
			@Override
			public long toId(String category) {
				argCategory = category;
				return returnId;
			}
			
			@Override
			public void save(Entry ent) {
				// do nothing
			}
		};
	}
	
	static final long BOOK_ID = 5; // whatever.
	
	public void test_readLine_newCategory() {
		long categoryId = 9; 
		returnId = categoryId;
		
		CsvImporter importer = new CsvImporter(BOOK_ID, createStorableMock());
		Entry ent = importer.parseLine("2011/11/08,Foods,-105.00,うどん");
		assertEqualsDate(2011, 11, 8, ent.getDate());
		assertEquals(categoryId, ent.getCategoryId());
		assertEquals(105, ent.getPrice());
		assertEquals("うどん", ent.getMemo());		
	}
	
	public void test_readLine_noMemo() {
		long categoryId = 9; 
		returnId = categoryId;
		
		CsvImporter importer = new CsvImporter(BOOK_ID, createStorableMock());
		Entry ent = importer.parseLine("2011/11/08,Foods,-105.00,");
		assertEqualsDate(2011, 11, 8, ent.getDate());
		assertEquals(categoryId, ent.getCategoryId());
		assertEquals(105, ent.getPrice());
	}
	
	public void test_readLine_price_int() {
		long categoryId = 9; 
		returnId = categoryId;
		
		CsvImporter importer = new CsvImporter(BOOK_ID, createStorableMock());
		Entry ent = importer.parseLine("2011/11/08,Foods,-105,うどん");
		assertEquals(105, ent.getPrice());
		
	}
	
	// also basic test of EntryStore.
	public void test_readLine_knownCategory_with_real_EntryStore() {
		long foodId = 4;
		
		Hashtable<Long, String> catMap = new Hashtable<Long, String>();
		catMap.put(3l, "Coffe");
		catMap.put(foodId, "Foods");

		EntryStore store = new EntryStore(null);
		store.setCategoryMap(catMap);
				
		CsvImporter importer = new CsvImporter(BOOK_ID, store);
		Entry ent = importer.parseLine("2011/11/08,Foods,-105.00,うどん");
		assertEquals(foodId, ent.getCategoryId());
	}
	
	void assertEqualsDate(int expectedYear, int expectedMonth, int expectedDay, Date actual)
	{
		Date expect = createDate(expectedYear, expectedMonth, expectedDay);
		assertEquals(expect, actual);
	}

	static Date createDate(int expectedYear, int expectedMonth, int expectedDay) {
		Date expect = new Date(expectedYear-1900, expectedMonth-1, expectedDay);
		return expect;
	}

	public void test_beforeDate()
	{
		Date input = createDate(2011, 11, 4);
		
		Date actual = Database.beforeDate(input, 3);
		assertEqualsDate(2011, 11, 1, actual);		
	}
	
	public void test_betweenDate()
	{
		Date inputFrom = createDate(2011, 11, 4);
		Date inputTo = createDate(2011, 11, 6);
		
		int actual = Database.betweenDate(inputFrom, inputTo);
		assertEquals(3, actual);
	}
	
	public void test_sanitize_normal()
	{
		validateSanitize("hoge", "hoge");
	}
	
	public void test_sanitize_return()
	{
		validateSanitize("ab\ncd", "ab cd");
	}
	
	public void test_sanitize_dblquote()
	{
		validateSanitize("\"abc", " abc");
	}
	
	public void test_sanitize_comma()
	{
		validateSanitize("a,b", "a b");		
	}
	
	public void test_sanitize_mix()
	{
		validateSanitize("a,b\nc", "a b c");
	}


	void validateSanitize(String input, String expect) {
		String actual = callSanitize(input);
		assertEquals(expect, actual);
	}

	String callSanitize(String str) {
		String actual = BookActivity.BookActivitySanitizer.sanitize(str);
		return actual;
	}
}
