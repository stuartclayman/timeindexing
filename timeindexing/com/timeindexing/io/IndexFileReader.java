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



// IndexFileReader.java

package com.timeindexing.io;

import com.timeindexing.index.ManagedIndexItem;
import com.timeindexing.index.DataReference;
import com.timeindexing.index.DataHolderObject;
import com.timeindexing.index.IndexProperties;
import com.timeindexing.index.IndexOpenException;
import com.timeindexing.basic.Offset;
import com.timeindexing.basic.Position;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An interface for readers of indexes.
 */
public interface IndexFileReader { 
    public long open(IndexProperties indexProperties) throws IOException, IndexOpenException;

    /**
     * Read an index header from the header stream.
     * @param headerType the type of header, e.g FileType.INLINE_INDEX or FileType.EXTERNAL_INDEX
     */
    public long readHeader(byte headerType) throws IOException, IndexOpenException;

    /**
     * Read all the meta data.
     */
    public long readMetaData() throws IOException, IndexOpenException;

    /**
     * Get the item
     * @param position the position of the index item to get
     * @param withData read the data for this IndexItem if withData is true,
     * the data needs to be read at a later time, otherwise
     */
    public ManagedIndexItem getItem(long position, boolean withData) throws IOException;

    /**
     * Read the contents of the item
     * It assumes the index file is alreayd open for writing.
     * @param offset the byte offset in the file to start reading an item from
     * @param withData read the data for this IndexItem if withData is true,
     * the data needs to be read at a later time, otherwise
     */
    public ManagedIndexItem readItem(Offset offset, boolean withData) throws IOException;

    /**
     * Read the contents of the item
     * It assumes the index file is alreayd open for writing.
     * @param offset the byte offset in the file to start reading an item from
     * @param withData read the data for this IndexItem if withData is true,
     * the data needs to be read at a later time, otherwise
     */
    public ManagedIndexItem readItem(long offset, boolean withData) throws IOException;

    /**
     * Read some data, given an offset and a size.
     * @param offset the byte offset in the file to start reading an item from
     * @param size the number of bytes to read
     */
    public ByteBuffer readData(Offset offset, long size) throws IOException;

     /**
     * Read some data, given an offset and a size.
     * @param offset the byte offset in the file to start reading an item from
     * @param size the number of bytes to read
     */
    public ByteBuffer readData(long offset, long size) throws IOException;

   /**
     * Read some data, given a DataReferenceObject
     */
    public ByteBuffer readData(DataReference ref)  throws IOException;

    /**
     * Read some data, given a DataReference
     * and return it as a DataHolderObject.
     */
    public DataHolderObject convertDataReference(DataReference ref) ;

    /**
     * Load the index data, based on a specified LoadStyle.
     */
    public long loadIndex(LoadStyle loadStyle) throws IOException;

    /**
     * Goto the first  position.
     */
    public boolean gotoFirstPosition() throws IOException;

    /**
     * Goto the append position
     */
    public boolean gotoAppendPosition() throws IOException;

    /**
     * Set the append position
     */
    public boolean setAppendPosition() throws IOException;

    /**
     * Close the  index.
     */
    public long close() throws IOException;
}
