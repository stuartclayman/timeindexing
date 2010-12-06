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
import com.timeindexing.index.*;
import com.timeindexing.time.*;
import com.timeindexing.basic.*;
import com.timeindexing.data.DataItem;
import com.timeindexing.data.StringItem;
import com.timeindexing.data.IntegerItem;
import com.timeindexing.appl.SelectionStreamer;
import com.timeindexing.cache.*;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test of multiple TimeIndex threads to the same underlying index.
 * The index keeps getting bigger.
 */
public class ThreadSelect2 {
    Properties openProperties = null;
    TimeIndexFactory factory = null;
    IndexView index = null;
    static long id = 0;

    Thread myThread = null;

    public static void main(String [] args) {
	if (args.length != 3) {
	    error();
	} else {
	    String threadStr = args[0];
	    int threadCount = 0;
	    String delayStr = args[1];
	    int delay = 0;
	    String indexpath = args[2];

	    try {
		threadCount = Integer.parseInt(threadStr);
	    } catch (NumberFormatException nfe) {
		threadCount = 1;
	    }

	    try {
		delay = Integer.parseInt(delayStr);
	    } catch (NumberFormatException nfe) {
		delay = 0;
	    }

	    id = System.currentTimeMillis();

            ThreadSelect2 ts2 = new ThreadSelect2(indexpath);

            ts2.init();

	    for (int t=0; t < threadCount; t++) {
                new ReaderThread(ts2, t);
		delay(delay);
	    }

            // add to index
            ts2. add();

            //end
            ts2.end();
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
	System.err.println("ThreadSelect2 threadcount delay indexpath");
    }


    /**
     * Constructor
     */
    public ThreadSelect2(String indexpath) {
	openProperties = new Properties();
	openProperties.setProperty("indexpath", indexpath);
        File file = new File(indexpath);
        openProperties.setProperty("name", file.getName());
    }

    public void init() {
        try {
            factory = new TimeIndexFactory();

            index = factory.create(IndexType.EXTERNAL, openProperties);
            index.setCachePolicy(new HollowAtDataVolumePolicy(64*1024));

            //new HollowAtDataVolumeRemoveAfterTimeoutPolicy(20*1024, new ElapsedMillisecondTimestamp(200)));
            //new HollowAtDataVolumeRemoveAfterTimeoutPolicy());
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            System.exit(1);
        }
    }

    public void end() {
        try {
            System.err.println("Close " + index);
            factory.close(index);
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            System.exit(1);
        }
    }

    /**
     * Add some data
     */
    public void add() {
        int count = 0;
        Random rseq = new Random(System.currentTimeMillis());

        while (true) {
                int value = rseq.nextInt(1000);

            try {
                index.addItem(new IntegerItem(value));
            } catch (TimeIndexException tie) {
                System.err.println("TimeIndexException: " + tie);
                tie.printStackTrace();

                System.exit(1);

            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
            }

            count++;
        }
    }

    public IndexView getIndex() {
        return index;
    }
}

class ReaderThread implements Runnable {
    ThreadSelect2 ts2;
    Thread myThread;
    IndexView index;
    int id;

    public ReaderThread(ThreadSelect2 ts2, int id) {
        this.ts2 = ts2;
        this.id = id;
        index = ts2.getIndex();

	myThread = new Thread(this);        

	System.err.println("ReaderThread: Object " + this.hashCode() + ". Thread " + myThread.getName());

	myThread.start();
    }

    public void run() {
        while (true) {
            try {
                printIndex(index);

            } catch (TimeIndexException ice) {
                ice.printStackTrace();
                System.exit(1);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }

            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException ie) {
                ;
            }
        }
    }

    public void printIndex(IndexView index) throws TimeIndexException, IOException {
        System.err.println("ReaderThread: Object " + this.hashCode() + ". Thread " + myThread.getName() + ". Index " + index.getName() + " first: " + index.getFirstTime() + " last: " + index.getLastTime());

	// The selection of data to get, 
	// Get last N seconds of collected data, as per sleepTime
	Second backN = new Second(30, TimeDirection.BACKWARD);

	// From Position.END_OF_INDEX, back 30 seconds
	Interval interval = new EndPointInterval((AbsolutePosition)Position.END_OF_INDEX, backN);

	System.out.println("select: " + interval);

	// get the selection
	IndexView selection = index.select(interval);

	if (selection == null) {
	    // there was nothing to select
	    // so return an empty list
	    System.out.println("select: {}");

	} else {
	    System.out.println("select: " + interval + " = " +
			       selection.getFirstTime() + " -> " + selection.getLastTime() );

	    System.out.println("select: found " + selection.getLength());

	    Iterator selectionI = selection.iterator();

            int sum = 0;

	    while (selectionI.hasNext()) {
		IndexItem item = (IndexItem)selectionI.next();
                IntegerItem data = (IntegerItem)item.getDataItem();
                Integer anInt = (Integer)data.getObject();
                //System.err.print(anInt + " ");
                sum += anInt;
	    }

	    System.out.println("select: sum " + sum);



	}
    }

}

