/*
 * Copyright 2003-2008 Stuart Clayman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package uk.ti;

import com.timeindexing.basic.ID;
import com.timeindexing.basic.UID;
import com.timeindexing.index.Index;
import com.timeindexing.index.IndexView;
import com.timeindexing.index.IndexItem;
import com.timeindexing.index.IndexType;
import com.timeindexing.index.TimeIndexFactory;
import com.timeindexing.index.TimeIndexException;
import com.timeindexing.index.DataType;
import com.timeindexing.index.IndexCreateException;
import com.timeindexing.time.Timestamp;
import com.timeindexing.time.MillisecondTimestamp;
import com.timeindexing.time.ElapsedMillisecondTimestamp;
import com.timeindexing.time.TimeCalculator;
import com.timeindexing.data.DataItem;
import com.timeindexing.data.StringItem;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.nio.ByteBuffer;

/**
 * Test of multiple TimeIndex threads to the same underlying index.
 */
public class Thread1 implements Runnable {
    Properties openProperties = null;
    TimeIndexFactory factory = null;
    Index index = null;

    Thread myThread = null;

    public static void main(String [] args) {
	if (args.length != 2) {
	    error();
	} else {
	    String delayStr = args[0];
	    int delay = 0;
	    String indexpath = args[1];

	    try {
		delay = Integer.parseInt(delayStr);
	    } catch (NumberFormatException nfe) {
		delay = 0;
	    }

	    new Thread1(indexpath);

	    delay(delay);

	    new Thread1(indexpath);
	    
	    delay(delay);

	    new Thread1(indexpath);
	    
	}
    }

    public static void delay(long ms) {
	try {
	    Thread.sleep(ms);
	} catch (java.lang.InterruptedException ie) {
	    ;
	}
    }

    public static void error() {
	System.err.println("Thread1 delay indexpath");
    }

    public Thread1(String indexpath) {
	openProperties = new Properties();
	openProperties.setProperty("indexpath", indexpath);

	myThread = new Thread(this);

	System.err.println("Thread1: Object " + this.hashCode() + ". Thread " + myThread.getName());

	myThread.start();
    }

    public void run() {
	factory = new TimeIndexFactory();

	try {
	    IndexView index = factory.open(openProperties);

	    System.err.println("Thread1: Object " + this.hashCode() + ". Thread " + myThread.getName() + ". Index " + index.getID() + " opened");
	    printIndex(index);

	    factory.close(index);

	} catch (TimeIndexException ice) {
	    ice.printStackTrace();
	    System.exit(1);
	}
    }

    public void printIndex(Index index) throws TimeIndexException {
	System.out.print(myThread.getName() + "Name: " + index.getName() + "\n");
	System.out.print(myThread.getName() + "Start:\t" + index.getStartTime() + "\n");
	System.out.print(myThread.getName() + "First:\t" + index.getFirstTime() + "\n");
	System.out.print(myThread.getName() + "Last:\t" + index.getLastTime() + "\n");
	System.out.print(myThread.getName() + "End:\t" + index.getEndTime() + "\n");

	System.out.println(myThread.getName() + "Length: " + index.getLength() + " items\n\n");

	long total = index.getLength();
	for (long i=0; i<total; i++) {
	    IndexItem itemN = index.getItem(i);
	    printIndexItem(itemN);
	    //delay(10);
	}
	    
	    
    }

    public void printIndexItem(IndexItem item) throws TimeIndexException {
	System.out.print(myThread.getName() + ":\t");

	System.out.print(item.getDataTimestamp() + "\t");

	System.out.print(item.getIndexTimestamp() + "\t");

	ByteBuffer itemdata = item.getData();
        byte[] array = new byte[16];
        int dataSize = (int)item.getDataSize().value();
        
	if (dataSize > 16) {
            itemdata.get(array, 0, 11);
	    System.out.print(new String(array) + "....\t");
	} else {
            itemdata.get(array, 0, dataSize);
	    System.out.print(new String(array, 0, dataSize) + "\t");
	}

	System.out.print(item.getDataSize() + "\t");

	System.out.print(item.getItemID() + "\t");

	System.out.print(item.getAnnotationMetaData() + "\n");

        /*
	ByteBuffer itemdata = item.getData();
        byte[] array = new byte[(int)item.getDataSize().value()];
	itemdata.get(array);
	System.out.print(new String(array));
	*/
    }

}

