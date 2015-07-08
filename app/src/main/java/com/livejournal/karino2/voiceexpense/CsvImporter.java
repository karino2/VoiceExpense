package com.livejournal.karino2.voiceexpense;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

public class CsvImporter {
	long bookId;
	EntryStorable storable;
	
	public CsvImporter(long bookId, EntryStorable storable)
	{
		this.bookId = bookId;
		this.storable = storable;
	}
	
	public void importCsv(String path) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(path), 8*1024);
		String line = br.readLine();
		while(line != null)
		{
			if(!"".equals(line))
			{
				Entry ent = parseLine(line);
				storable.save(ent);
			}
			line = br.readLine();
		}
	}

	public Entry parseLine(String line) {
		String[] vals = line.split(",");
		if(vals.length == 4 ||
				vals.length == 3)
		{
			// date, category, price, memo
			Date dt = new Date(vals[0]);
			long category = storable.toId(vals[1]);
			int price = Math.abs(((int)Double.parseDouble(vals[2])));
			String memo = "";
			if(vals.length == 4)
				memo = vals[3];
			return new Entry(dt, category, memo, price, bookId, false);
		}
		else if (vals.length == 5)
		{
			// date, category, price, memo, business
			Date dt = new Date(vals[0]);
			long category = storable.toId(vals[1]);
			int price = Math.abs(((int)Double.parseDouble(vals[2])));
			String memo = vals[3];
			boolean business = "business".equals(vals[4]);
			return new Entry(dt, category, memo, price, bookId, business);
		}
		throw new RuntimeException("invalid csv, NYI");
	}
	
}
