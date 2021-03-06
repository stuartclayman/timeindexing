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



// Second.java

package com.timeindexing.time;

/**
 * A TimeSpecifier for seconds.
 */
public class Second extends AbstractTimeSpecifier implements TimeSpecifier {
    /**
     * Construct a Second TimeSpecifier.
     */
    public Second(long count) {
	setHowMany(count);
	setDirection(TimeDirection.FORWARD);
    }

    /**
     * Construct a Second TimeSpecifier.
     */
    public Second(long count, TimeDirection direction) {
	setHowMany(count);
	setDirection(direction);
    }

    /**
     * Construct a Second TimeSpecifier.
     */
    public Second(long count, TimeDirection direction, TimeSpecifier modifier) {
	setHowMany(count);
	setDirection(direction);
	afterDoing(modifier);
    }

    /**
     * Instantiate this TimeSpecifier w.r.t a particular Timestamp.
     * It returns a Timestamp which has been modified by the amount of the TimeSpecifier.
     */
    public Timestamp instantiate(Timestamp t) {
	Timestamp timestampToUse = null;

	// determine what timestamp to use
	if (getPredecessor() == null) {
	    // there is no sub modifier so we use t directly
	    timestampToUse = t;
	} else {
	    // there is a sub modifier, so we use the Timestamp 
	    // returned by instantiating the getPredecessor() with t
	    timestampToUse = getPredecessor().instantiate(t);
	}

	// seconds
	long seconds = howMany;

	Timestamp elapsedTimestamp = new ElapsedSecondTimestamp(seconds);
	Timestamp returnTimestamp = null;

	if (modificationDirection == TimeDirection.FORWARD) {
	    returnTimestamp = TimeCalculator.addTimestamp(timestampToUse, elapsedTimestamp);
	} else {
	    returnTimestamp = TimeCalculator.subtractTimestamp(timestampToUse, elapsedTimestamp);
	}

	return returnTimestamp;
    }

    /**
     * To String
     */
    public String toString() {
	StringBuffer buffer = new StringBuffer(128);

	if (hasPredecessor()) {
	    buffer.append(getPredecessor().toString());
	    buffer.append(" then ");
	}

	buffer.append(howMany);
	buffer.append(" second ");
	buffer.append(modificationDirection.toString());

	return buffer.toString();
    }
}

