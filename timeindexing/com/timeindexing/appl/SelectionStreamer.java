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



// SelectionStreamer.java

package com.timeindexing.appl;

import com.timeindexing.index.Index;
import com.timeindexing.index.TimeIndex;
import com.timeindexing.index.IndexView;
import com.timeindexing.index.IndexProperties;
import com.timeindexing.index.TimeIndexException;
import com.timeindexing.appl.SelectionProcessor;
import com.timeindexing.time.Clock;
import com.timeindexing.plugin.OutputPlugin;
import java.io.OutputStream;
import java.io.IOException;

/**
 * A class to output a selection of the data.
 * The selection is made by passing some IndexProperties to doOutput().
 * The IndexProperties are passed to a SelectionProcessor.
 * <p>
 * Types of values in the IndexProperties are:
 * <ul>
 * <li> "startpos" -> value accepted by PositionParser
 * <li> "starttime" -> value accepted by TimeDateParser
 * <li> "endpos" ->  value accepted by PositionParser
 * <li> "endtime" -> value accepted by TimeDateParser
 * <li> "count" -> value accepted by CountParser
 * <li> "for" -> value accepted by TimeDateParser
 * </ul>
 */
public class SelectionStreamer extends OutputStreamer  {
    /**
     * Construct an SelectionStreamer object given
     * an index and an output stream.
     */
    public SelectionStreamer(Index anIndex, OutputStream output) {
	super(anIndex, output);
    }

    /**
     * Construct an SelectionStreamer object given
     * an index and an output stream and an OutputPlugin.
     */
    public SelectionStreamer(Index anIndex, OutputStream output, OutputPlugin plugin) {
	super(anIndex, output, plugin);
    }


    /**
     * Do some output, given some IndexProperties.
     * The IndexProperties specify a selection to make.
     * Only the selection is output.
     * The IndexProperties are passed to a SelectionProcessor.
     */
    public long doOutput(IndexProperties properties) throws IOException, TimeIndexException {
	outputProperties = properties;

	outputPlugin.setContext(index, out);

	outputPlugin.begin();

	SelectionProcessor selector = new SelectionProcessor();
 
	IndexView selection = selector.select((IndexView)index, properties);

	if (selection == null || selection.getLength() == 0) {
	    // nothing to do
	} else {
	    // process the time index
	    writeCount = processTimeIndex((IndexView)selection);
	}

	outputPlugin.end();

	//System.err.println(Clock.time.time() + " " + index.getURI() + ". output bytes = " + writeCount + ". Thread " + Thread.currentThread().getName() );

	return writeCount;
    }

}
