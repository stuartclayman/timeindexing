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



// AbstractIndexIO.java

package com.timeindexing.io;

import com.timeindexing.index.StoredIndex;
import com.timeindexing.index.ManagedIndexItem;
import com.timeindexing.basic.Position;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.io.IOException;

/**
 * An object for doing IO for an Index.
 * It has a handle on the Index it's doing I/O for, and
 * handles the thread for the I/O.
 */
public abstract class AbstractIndexIO implements IndexInteractor,  Runnable {
    // The index this is doing I/O for
    StoredIndex myIndex = null;

    // The Thread for this I/O
    Thread myThread = null;

    // Should the thread be running
    boolean threadRunning = false;

    // Have we got to end of run()
    CountDownLatch latch = null;
    boolean endOfRun = false;

    // A work queue for read requests
    LinkedList readQueue = null;


    // A work queue for write requests
    LinkedList writeQueue = null;


    /**
     * Get the index which this is doing I/O for.
     */
    public StoredIndex getIndex() {
	return myIndex;
    }

    /**
     * Initialize the thread
     * @param name the name of the thread
     */
    public Thread initThread(String name) {
	myThread = new Thread(this, name);
	readQueue = new LinkedList();
	writeQueue = new LinkedList();
        latch = new CountDownLatch(1);

	return myThread;
    }


    /**
     * Get the thread
     */
    public Thread getThread() {
	return myThread;
    }

    /**
     * Start the thread
     */
    public Thread startThread() {
	if (myThread != null) {
	    threadRunning = true;
            endOfRun = false;
	    myThread.start();
	    //System.err.println("Started Thread " + myThread);
	    return myThread;
	} else {
	    return null;
	}
    }

    /**
     * Stop the thread
     */
    public Thread stopThread() {
	if (myThread != null) {
	    //myThread.stop();

	    threadRunning = false;


            doStop();

	    //Thread retVal = myThread;
	    //myThread = null;
	    //return retVal;
            return myThread;
	} else {
	    return null;
	}
    }

    private void doStop() {

        //System.err.println(getIndex().getName() + " About to interrupt thread " + myThread);

        // interrupt any methods waiting for work or IO
        myThread.interrupt();
 
        // join myThread if it is not finished
        try {
            latch.await();
        } catch (InterruptedException ie) {
        }
  

        //System.err.println(getClass().getSimpleName() + " " + getIndex().getName() + " Stopped Thread " + myThread);

    }

    /**
     * Is the thread still running.
     */
    public boolean isRunning() {
	return threadRunning;
    }

}
