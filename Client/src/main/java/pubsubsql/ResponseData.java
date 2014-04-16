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

package pubsubsql; 

import java.util.ArrayList;

class ResponseData {
	public String status;
	public String msg;
	public String action;
	public String pubsubid;
	public int rows;
	public int fromrow;
	public int torow;
	public ArrayList<String> columns = new ArrayList<String>();
	public ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
}

