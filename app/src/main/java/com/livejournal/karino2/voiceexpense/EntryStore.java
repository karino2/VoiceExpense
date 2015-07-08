package com.livejournal.karino2.voiceexpense;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class EntryStore implements EntryStorable {
	Database database;
	Hashtable<String, Long> categoryReverseMap;
	public EntryStore(Database database)
	{
		this.database = database;
		categoryReverseMap = new Hashtable<String, Long>();
	}
	public void setCategoryMap(Hashtable<Long, String> categoryMap)
	{
		Set<Map.Entry<Long, String>> entSet = categoryMap.entrySet();
		for(Map.Entry<Long, String> ent : entSet)
		{
			categoryReverseMap.put(ent.getValue(), ent.getKey());
		}		
	}

	@Override
	public void save(Entry ent) {
		database.insert(ent);
	}

	@Override
	public long toId(String category) {
		if(categoryReverseMap.containsKey(category))
		{
			return categoryReverseMap.get(category);
		}
		long id = database.newCategory(category);
		categoryReverseMap.put(category, id);
		return id;
	}

}
