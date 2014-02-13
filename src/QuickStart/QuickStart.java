/* Copyright (C) 2014 CompleteDB LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License Version 2.0 http://www.apache.org/licenses.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

import pubsubsql.Client;
import java.util.*;

/* MAKE SURE TO RUN PUBSUBSQL SERVER WHEN RUNNING THE EXAMPLE */

public class QuickStart {

	static void checkError(pubsubsql.Client client, String str) {
		if (client.Failed()) {
			System.out.println(String.format("Error: %s %s", client.Error(), str));
		}
	}

	public static void main(String[] args) {
		pubsubsql.Client client = new pubsubsql.Client();
		pubsubsql.Client subscriber = new pubsubsql.Client();

		//----------------------------------------------------------------------------------------------------
		// CONNECT
		//----------------------------------------------------------------------------------------------------

		String address = "localhost:7777";
		client.Connect(address);
		checkError(client, "client connect failed");
		subscriber.Connect(address);
		checkError(client, "subscriber connect failed");

		//----------------------------------------------------------------------------------------------------
		// SQL MUST-KNOW RULES
		//
		// All commands must be in lower case.
		//
		// Identifiers can only begin with alphabetic characters and may contain any alphanumeric characters.
		//
		// The only available (but optional) data definition commands are
		//    key (unique index)      - key table_name column_name
		//    tag (non-unique index)  - tag table_name column_name
		//
		// Tables and columns are auto-created when accessed.
		//
		// The underlying data type for all columns is String.
		// Strings do not have to be enclosed in single quotes as long as they have no special characters.
		// The special characters are
		//    , - comma
		//      - white space characters (space, tab, new line)
		//    ) - right parenthesis
		//    ' - single quote
		//----------------------------------------------------------------------------------------------------

		//----------------------------------------------------------------------------------------------------
		// INDEX
		//----------------------------------------------------------------------------------------------------

		client.Execute("key Stocks Ticker");
		client.Execute("tag Stocks MarketCap");

		//----------------------------------------------------------------------------------------------------
		// SUBSCRIBE
		//----------------------------------------------------------------------------------------------------

		subscriber.Execute("subscribe * from Stocks where MarketCap = 'MEGA CAP'");
		String pubsubid = subscriber.PubSubId();
		System.out.println("subscribed to Stocks pubsubid: " + pubsubid);
		checkError(subscriber, "subscribe failed");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH INSERT
		//----------------------------------------------------------------------------------------------------

		client.Execute("insert into Stocks (Ticker, Price, MarketCap) values (GOOG, '1,200.22', 'MEGA CAP')");
		checkError(client, "insert GOOG failed");
		client.Execute("insert into Stocks (Ticker, Price, MarketCap) values (MSFT, 38,'MEGA CAP')");
		checkError(client, "insert MSFT failed");

		//----------------------------------------------------------------------------------------------------
		// SELECT
		//----------------------------------------------------------------------------------------------------

		client.Execute("select id, Ticker from Stocks");
		checkError(client, "select failed");
		while (client.NextRow()) {
			System.out.println("*********************************");
			System.out.println(String.format("id:%s Ticker:%s \n", client.Value("id"), client.Value("Ticker")));
		}
		checkError(client, "NextRow failed");

		//----------------------------------------------------------------------------------------------------
		// PROCESS PUBLISHED INSERT
		//----------------------------------------------------------------------------------------------------

		int timeout = 100;
		while (subscriber.WaitForPubSub(timeout)) {
			System.out.println("*********************************");
			System.out.println("Action:" + subscriber.Action());
			while (subscriber.NextRow()) {
				System.out.println("New MEGA CAP stock:" + subscriber.Value("Ticker"));
				System.out.println("Price:" + subscriber.Value("Price"));
			}
			checkError(subscriber, "NextRow failed");
		}
		checkError(subscriber, "WaitForPubSub failed");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH UPDATE
		//----------------------------------------------------------------------------------------------------

		client.Execute("update Stocks set Price = '1,500.00' where Ticker = GOOG");
		checkError(client, "update GOOG failed");

		//----------------------------------------------------------------------------------------------------
		// SERVER WILL NOT PUBLISH INSERT BECAUSE WE ONLY SUBSCRIBED TO 'MEGA CAP'
		//----------------------------------------------------------------------------------------------------

		client.Execute("insert into Stocks (Ticker, Price, MarketCap) values (IBM, 168, 'LARGE CAP')");
		checkError(client, "insert IBM failed");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH ADD
		//----------------------------------------------------------------------------------------------------

		client.Execute("update Stocks set Price = 230.45, MarketCap = 'MEGA CAP' where Ticker = IBM");
		checkError(client, "update IBM to MEGA CAP failed");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH REMOVE
		//----------------------------------------------------------------------------------------------------

		client.Execute("update Stocks set Price = 170, MarketCap = 'LARGE CAP' where Ticker = IBM");
		checkError(client, "update IBM to LARGE CAP failed");

		//----------------------------------------------------------------------------------------------------
		// PUBLISH DELETE
		//----------------------------------------------------------------------------------------------------

		client.Execute("delete from Stocks");
		checkError(client, "delete failed");

		//----------------------------------------------------------------------------------------------------
		// PROCESS ALL PUBLISHED
		//----------------------------------------------------------------------------------------------------

		while (subscriber.WaitForPubSub(timeout)) {
			System.out.println("*********************************");
			System.out.println("Action:" + subscriber.Action());
			while (subscriber.NextRow()) {
				int ordinal = 0;
				for (String column : subscriber.Columns()) {
					System.out.print(String.format("%s:%s ", column, subscriber.ValueByOrdinal(ordinal)));
					ordinal++;
				}
				System.out.println(); 
			}
			checkError(subscriber, "NextRow failed");
		}
		checkError(subscriber, "WaitForPubSub failed");

		//----------------------------------------------------------------------------------------------------
		// UNSUBSCRIBE
		//----------------------------------------------------------------------------------------------------

		subscriber.Execute("unsubscribe from Stocks");
		checkError(subscriber, "NextRow failed");

		//----------------------------------------------------------------------------------------------------
		// DISCONNECT
		//----------------------------------------------------------------------------------------------------

		client.Disconnect();
		subscriber.Disconnect();
	
	}
}

