package com.livejournal.karino2.voiceexpense;

public interface EntryStorable {
	void save(Entry ent);
	long toId(String category);
}
