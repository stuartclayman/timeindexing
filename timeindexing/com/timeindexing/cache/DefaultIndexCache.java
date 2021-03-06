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



// DefaultIndexCache.java

package com.timeindexing.cache;

import com.timeindexing.time.Timestamp;
import com.timeindexing.time.TimestampMapping;
import com.timeindexing.time.TimeCalculator;
import com.timeindexing.time.Lifetime;
import com.timeindexing.basic.Interval;
import com.timeindexing.basic.Position;
import com.timeindexing.index.IndexTimestampSelector;
import com.timeindexing.index.IndexItem;
import com.timeindexing.index.ManagedIndex;
import com.timeindexing.index.ManagedIndexItem;
import com.timeindexing.index.PositionOutOfBoundsException;
import com.timeindexing.index.DataHolder;
import com.timeindexing.index.DataAbstraction;
import com.timeindexing.util.DoubleLinkedList;
import com.timeindexing.util.MassiveBitSet;
import com.timeindexing.util.LRUHashMap;


import java.util.Comparator;

/**
 * The default implementation of a cache which holds the index items.
 */
public class DefaultIndexCache implements IndexCache {
    // a tree of all the IndexItems
    LRUHashMap indexItems = null;


    Timestamp firstIndexTime = Timestamp.ZERO;
    Timestamp lastIndexTime = Timestamp.ZERO;
    Timestamp firstDataTime = Timestamp.ZERO;
    Timestamp lastDataTime = Timestamp.ZERO;
    ManagedIndex myIndex = null;


    int count = 0;
    int hits = 0;

    /**
     * Create a DefaultIndexCache object.
     */
    public DefaultIndexCache(ManagedIndex index) {
	myIndex = index;
	indexItems = new LRUHashMap(10000);
    }

    /**
     * Get the no of items in the cache
     */
    public synchronized long size() {
	return indexItems.size();
    }

    /**
     * Add an Index Item to the Index.
     * @param item the IndexItem to add
     * @return the no of items in the index.
     */
    public synchronized long addItem(IndexItem item, Position position) {
	return addItem(item, position.value());
    }

    /**
     * Add an Index Item to the Index.
     * @param item the IndexItem to add
     * @return the no of items in the index.
     */
    public synchronized long addItem(IndexItem item, long position) {
	// create a new list if needed
	if (indexItems == null) {
	    indexItems = new LRUHashMap(10000);
	}

	indexItems.put(position, item);

	// return the cacheSize
	return size();

    }

    /**
     * Get an Index Item from the Index.
     */
    public synchronized IndexItem getItem(long pos) {
	if (indexItems == null) {
	    return null;
	} else {
	    if (pos < 0) {
		throw new PositionOutOfBoundsException("Index value too low: " + pos);
	    } else if (pos >= myIndex.getLength()) {
		throw new PositionOutOfBoundsException("Index value too high: " + pos);
	    } else {
		IndexItem item = (IndexItem)indexItems.get(pos);
                
		return item;
	    }
	}
    }

    /**
     * Get an Index Item from the Index.
     */
    public synchronized IndexItem getItem(Position p) {
	return getItem(p.value());
    }


    /**
     * Contains the IndexItem at the speicifed position.
     * If the cache contains the item, it means it is loaded.
     */
    public synchronized boolean containsItem(long pos) {
	boolean found = indexItems.containsKey(pos);

        if (found) {
            hits++;
        }
        count++;


        if (count == 100) {
            System.err.println("DefaultIndexCache: " + myIndex.getName() + " Hits: " + hits + "/" + count + " Size: " + indexItems.size());
            count = 0;
            hits = 0;
        }

        return found;
    }


    /**
     * Contains the IndexItem at the speicifed position.
     * If the cache contains the item, it means it is loaded.
     */
    public synchronized boolean containsItem(Position p) {
	return containsItem(p.value());
    }
	    
    /**
     * Hollow the IndexItem at the position.
     * This does nothing by default as the data will be lost.
     */
    public synchronized boolean hollowItem(long pos) {
        return false;
    }
	

    /**
     * Hollow the IndexItem at the position.
     */
    public synchronized boolean hollowItem(Position p) {
	return hollowItem(p.value());
    }


    /**
     * Remove the IndexItem at the speicifed position.
     */
    public synchronized boolean removeItem(long pos) {
	if (indexItems == null) {
	    return false;
	} else {
	    if (pos < 0) {
		throw new PositionOutOfBoundsException("Index value too low: " + pos);
	    } else if (pos >= myIndex.getLength()) {
		throw new PositionOutOfBoundsException("Index value too high: " + pos);
	    } else {
                indexItems.remove(pos);
		return true;
	    }
	}
    }

    /**
     * Remove the IndexItem at the speicifed position.
     */
    public synchronized boolean removeItem(Position p) {
	return removeItem(p.value());
    }

    /**
     * Clear the whole cache
     */
    public synchronized boolean clear() {
        indexItems.clear();
        return true;
    }

    /**
     * Get the current data volume held by IndexItems in this cache.
     * This return 0 by default.
     */
    public long getDataVolume() {
	return 0;
    }

    /**
     * Increase the data volume
     * This does nothing by default as the data will be lost.
     * @return the new data volume
     */
    public long increaseDataVolume(long v) {
	return 0;
    }
    

    /**
     * Decrease the data volume
     * This does nothing by default as the data will be lost.
     * @return the new data volume
     */
    public long decreaseDataVolume(long v) {
	return 0;
    }
    

    /**
     * Get the time the first IndexItem was put into the Index.
     */
    public Timestamp getFirstIndexTime() {
	return firstIndexTime;
    }

    /**
     * Get the time the last IndexItem was put into the Index.
     */
    public Timestamp getLastIndexTime() {
	return lastIndexTime;
    }

    /**
     * Get the time the first IndexItem was put into the Index.
     */
    public Timestamp getFirstDataTime() {
	return firstDataTime;
    }

    /**
     * Get the time the last IndexItem was put into the Index.
     */
    public Timestamp getLastDataTime() {
	return lastDataTime;
    }

    /**
     * Set the cache policy.
     * This does nothing by default as the data will be lost.
     * @return null
     */
    public synchronized CachePolicy setPolicy(CachePolicy pol) {
	return null;
    }

    /**
     * Get the current cache policy.
     */
    public synchronized CachePolicy getPolicy() {
	return null;
    }
}
