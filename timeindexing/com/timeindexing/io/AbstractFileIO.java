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



// AbstractFileIO.java

package com.timeindexing.io;
import com.timeindexing.index.DataType;
import com.timeindexing.index.Index;
import com.timeindexing.index.ManagedIndex;
import com.timeindexing.index.StoredIndex;
import com.timeindexing.index.IndexItem;
import com.timeindexing.index.ManagedIndexItem;
import com.timeindexing.index.ManagedFileIndexItem;
import com.timeindexing.index.FileIndexItem;
import com.timeindexing.index.DataAbstraction;
import com.timeindexing.index.DataHolder;
import com.timeindexing.index.DataHolderObject;
import com.timeindexing.index.DataReference;
import com.timeindexing.index.DataReferenceObject;
import com.timeindexing.index.IndexReferenceDataHolder;
import com.timeindexing.index.DataTypeDirectory;
import com.timeindexing.index.IndexProperties;
import com.timeindexing.index.IndexOpenException;
import com.timeindexing.index.IndexCreateException;
import com.timeindexing.event.*;
import com.timeindexing.basic.ID;
import com.timeindexing.basic.SID;
import com.timeindexing.basic.Size;
import com.timeindexing.basic.Position;
import com.timeindexing.basic.AbsolutePosition;
import com.timeindexing.basic.Offset;
import com.timeindexing.time.TimestampDecoder;
import com.timeindexing.time.Timestamp;
import com.timeindexing.util.ByteBufferRing;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.LinkedList;

/**
 * Has code for indexes that are file-based.
 */
public abstract class AbstractFileIO extends AbstractIndexIO implements IndexFileInteractor {
    // The header for this index
    IndexHeaderIO headerInteractor = null;

    String indexName = null;
    ID indexID = null;
    //DataType dataType = DataType.NOTSET;

    String originalIndexSpecifier = null; // the original spec for an index
    String headerFileName = null; // the resolved header file name

    // index file objs
    String indexFileName = null; // the resolved index file name
    RandomAccessFile indexFile = null; // the actual index file
    FileChannel indexChannel = null;
    long indexChannelPosition = 0;
    long indexFirstPosition = 0;
    long indexAppendPosition = 0;

    // buffers
    ByteBuffer headerBuf = null;
    ByteBuffer indexBufWrite = null;
    ByteBuffer indexBufRead = null;
    ByteBufferRing indexFlushBuffers = null;

    // TimestampDecoder
    TimestampDecoder timestampDecoder = new TimestampDecoder();

    // The size of buffers for References
    final static int REFERENCE_BUFFER_SIZE = 16;

    int versionMajor = 0;
    int versionMinor = 0;
    
    // are we creating or opening
    boolean creating = false;
    // Was the Index locked when we tried to activate it
    boolean hasBeenLocked = false;

    // has a timeout happened whilst waiting for some work
    boolean timeoutHappened = false;

    // A sync object
    Object syncObject = new Object();

    /*
     * The size of a header
     */
    final static int HEADER_SIZE = 512;

    /*
     * The size of an index item in the index file
     */
    int INDEX_ITEM_SIZE = 52;


    /*
     * The size of a flush buffer
     */
    final static int FLUSH_SIZE = 8 * 1024;


    /**
     * Read an index header from the header stream.
     */
    public long readHeader(byte headerType) throws IOException, IndexOpenException {
	// seek to start
	seekToIndex(0);

	// The first two bytes are T & I
	byte byteT = indexFile.readByte();
	byte byteI = indexFile.readByte();
	// byte 3 = 0x03
	byte three = indexFile.readByte();
	// the type of the header
	byte type = indexFile.readByte();

	// check first 4 bytes
	if (byteT == FileType.T &&
	    byteI == FileType.I &&
	    three == FileType.BYTE_3 &&
	    type == headerType) {
	    // we've opened a TimeIndex Header

	    
	    // get the version no
	    versionMajor = (int)indexFile.readByte();
	    versionMinor = (int)indexFile.readByte();

	    // get the index ID
	    indexID = new SID(indexFile.readLong());

	    // read the Index name
	    short nameSize = indexFile.readShort();
	    byte[] nameRaw = new byte[nameSize-1];
	    indexFile.readFully(nameRaw, 0, nameSize-1);
	    indexName = new String(nameRaw);

	    // soak up index name padding
	    indexFile.readByte();

	    //System.err.println("Index Header read size = " + indexFile.getFilePointer());

	    indexChannelPosition = indexChannel.position();
	    indexFirstPosition = indexChannelPosition;
	    return indexChannelPosition;
	
	} else {
	    throw new IndexOpenException("File is not a time index file");
	}
    }

    /**
     * Write the contents of the header out
     * It assumes the index file is alreayd open for writing.
     */
    public long writeHeader(byte headerType) throws IOException {
	// seek to start
	seekToIndex(0);

	// clear the header buffer
	headerBuf.clear();

	// fill the buffer with the bytes

	// TimeIndex Header magic
	headerBuf.put(FileType.T);
	headerBuf.put(FileType.I);
	headerBuf.put(FileType.BYTE_3);
	headerBuf.put(headerType);

	// version major
	headerBuf.put((byte)versionMajor);
	// version minor
	headerBuf.put((byte)versionMinor);

	// write the ID
	headerBuf.putLong(indexID.value());

	// size of name, +1 for null terminator
	headerBuf.putShort((short)(indexName.length()+1));

	// the name
	headerBuf.put(indexName.getBytes());
	// plus null terminator
	headerBuf.put((byte)0x00);

	// now write it out
	headerBuf.flip();
	long writeCount = indexChannel.write(headerBuf);

	indexChannelPosition = indexChannel.position();

	
	//System.err.println("Index Header size = " + writeCount);

	return writeCount;

    }


    /**
     * Add an item.
     */
    public long addItem(ManagedIndexItem itemM) throws IOException {
	return writeItem(itemM);
    }

    /**
     * Write the contents of the item
     * It assumes the index file is alreayd open for writing.
     */
    public synchronized long writeItem(ManagedIndexItem itemM) throws IOException {
	if (itemM.isReference()) {
	    // write out a reference
	    return writeReference(itemM);
	} else {
	    // write out normal data
	    return writeNormal(itemM);
	}
    }

    /**
     * Write the contents of the item with normal data
     * It assumes the index file is alreayd open for writing.
     */
    public long writeNormal(ManagedIndexItem itemM) throws IOException {

	// cast the item to the correct class
	ManagedFileIndexItem item = (ManagedFileIndexItem)itemM;

	long count = 0;

	long actualSize = itemM.getDataSize().value();

	if (actualSize >= Integer.MAX_VALUE) {
	    // buffers can only be so big
	    // check we can allocate one big enough
		throw new Error("InlineIndexIO: writeItem() has not YET implemented reading of data > " + Integer.MAX_VALUE + ". Actual size is " + actualSize);
	} else {

	    // where are we in the file
	    long currentIndexPosition = alignForIndexItem();

	    //System.err.println("P(W) = " + currentIndexPosition);

	    // tell the IndexItem where its index is
	    item.setIndexOffset(new Offset(currentIndexPosition));

	    // set the data position
	    long currentDataPosition = alignForData();

	    // tell the IndexItem where its data is
	    item.setDataOffset(new Offset(currentDataPosition));

	    // clear the index buf
	    indexBufWrite.clear();

	    // fill the buffer
	    indexBufWrite.putLong(item.getIndexTimestamp().value());
	    indexBufWrite.putLong(item.getDataTimestamp().value());
	    indexBufWrite.putLong(currentDataPosition);
	    indexBufWrite.putLong(item.getDataSize().value());
	    indexBufWrite.putInt(item.getDataType().value());
	    indexBufWrite.putLong(item.getItemID().value());
	    indexBufWrite.putLong(item.getAnnotationMetaData());

	    // make it ready for writing
	    indexBufWrite.flip();

	    // write the index item
	    count +=  processIndexItem(indexBufWrite);
	
	    // make the data ready for writing
            ByteBuffer dataBuf = item.getData();

	    if (dataBuf.position() == dataBuf.limit()) {
		dataBuf.flip();
	    }
	    

            // write the data
            count += processData(dataBuf);

	    // return how many bytes were written
	    return count;
	}
    }

    /**
     * Write the contents of the item with a reference.
     * It assumes the index file is alreayd open for writing.
     */
    public long writeReference(ManagedIndexItem itemM) throws IOException {

	// cast the item to the correct class
	ManagedFileIndexItem item = (ManagedFileIndexItem)itemM;

	long count = 0;

	long actualSize = REFERENCE_BUFFER_SIZE;

	if (actualSize >= Integer.MAX_VALUE) {
	    // buffers can only be so big
	    // check we can allocate one big enough
		throw new Error("InlineIndexIO: writeItem() has not YET implemented reading of data > " + Integer.MAX_VALUE + ". Actual size is " + actualSize);
	} else {

	    // where are we in the file
	    long currentIndexPosition = alignForIndexItem();

	    //System.err.println("P(W) = " + currentIndexPosition);

	    // tell the IndexItem where its index is
	    item.setIndexOffset(new Offset(currentIndexPosition));

	    // set the data position
	    long currentDataPosition = alignForData();

	    // tell the IndexItem where its data is
	    item.setDataOffset(new Offset(currentDataPosition));

	    // clear the index buf
	    indexBufWrite.clear();

	    // fill the buffer
	    indexBufWrite.putLong(item.getIndexTimestamp().value());
	    indexBufWrite.putLong(item.getDataTimestamp().value());
	    indexBufWrite.putLong(currentDataPosition);
	    indexBufWrite.putLong(REFERENCE_BUFFER_SIZE);
	    indexBufWrite.putInt(DataType.REFERENCE_VALUE);
	    indexBufWrite.putLong(item.getItemID().value());
	    indexBufWrite.putLong(item.getAnnotationMetaData());

	    // make it ready for writing
	    indexBufWrite.flip();

	    // write the index item
	    count +=  processIndexItem(indexBufWrite);
	
	    // write the data
	    IndexReferenceDataHolder reference = (IndexReferenceDataHolder)itemM.getDataAbstraction();
	    ByteBuffer referenceBuffer = ByteBuffer.allocate(REFERENCE_BUFFER_SIZE);
	    referenceBuffer.putLong(reference.getIndexID().value());
	    referenceBuffer.putLong(reference.getIndexItemPosition().value());
	    referenceBuffer.flip();

	    count += processData(referenceBuffer);

	    // return how many bytes were written
	    return count;
	}
    }

    /**
     * Align the index for an append of an IndexItem.
     */
    protected long alignForIndexItem()  throws IOException  {
	// seek to the append position in the index
	seekToIndex(indexAppendPosition);

	return indexChannelPosition;
    }

    /**
     * Align the index for an append of the Data
     * This is done differently for each type of index.
     */
    protected abstract long alignForData() throws IOException ;

    /**
     * Processing of the idnex item.
     */
    protected abstract long processIndexItem(ByteBuffer buffer) throws IOException;

    /**
     * Processing of the data.
     */
    protected abstract long processData(ByteBuffer buffer) throws IOException;

    /**
     * Write a buffer of index items.
     */
    protected abstract long bufferedIndexWrite(ByteBuffer buffer) throws IOException;

    /**
     * Write a buffer of data.
     */
    protected abstract long bufferedDataWrite(ByteBuffer buffer) throws IOException;


    /**
     * Write a buffer of data.
     * This flushes out large buffers a slice at a time.
     */
    protected long bufferedWrite(ByteBuffer buffer, FileChannel channel, ByteBufferRing ring) throws IOException {
        long written = 0;
        int origLimit = buffer.limit();
        ByteBuffer slice = null;
	ByteBuffer flushBuffer = null;

	//System.err.println("bufferedWrite: ring = " + ring);

	// get the current buffer from the ring
	flushBuffer = ring.current();

        while (buffer.hasRemaining()) {

	    /*
            System.err.println("flushBuffer() FB(P) = " + flushBuffer.position() +
                           " FB(C) = " + flushBuffer.capacity() +
                           " B(P) = " + buffer.position() + 
                           " B(L) = " + buffer.limit() +
                           " B(C) = " + buffer.capacity());
	    */

            // no of bytes available in flushBuffer
            int available = flushBuffer.capacity() - flushBuffer.position();
            // no of bytes to place
            int todo = buffer.limit() - buffer.position();

            // if the flushBuffer is too full to take the specified buffer
            if (todo > available) {
                // take some bytes from the input buffer
                // set the limit to be the amount available
                buffer.limit(buffer.position() + available);

                // take a slice
                slice = buffer.slice();

                //  put the slice in
                flushBuffer.put(slice);
                
                // this should have filled the flushBuffer
		// lock it
                // and then flush the buffer

		ring.lock();

                written += flushBuffer(channel, flushBuffer, ring);

		// get another buffer to use
		flushBuffer = ring.current();

                // adjust the pointers into the buffer
                buffer.position(buffer.limit());
                buffer.limit(origLimit);
            } else {

                //  put the new contents in
                flushBuffer.put(buffer);
            }

        
        }

	return written;
    }


    static int count = 0;

    /**
     * Actually flush the buffer out.
     * Returns how man bytes were written.
     */
    protected synchronized long flushBuffer(FileChannel channel, ByteBuffer flushBuffer, ByteBufferRing ring)  throws IOException {
	long written = 0;

	if (flushBuffer.position() > 0) {
	    flushBuffer.flip();

	    /* WAS
            written = channel.write(flushBuffer);

	    // clear it
	    flushBuffer.clear();
	    */

	    requestWriteWork(channel, flushBuffer, ring);
	    //System.err.println("Added WriteRequest [" + (count++) + "] " + channel + " for " + flushBuffer);

	    //System.err.println("flushBuffer() writeQueue length = " + writeQueue.size());

	    timeoutHappened = false;

            notifyAllListeners();

	}

	return written;
    }

    private void notifyAllListeners() {
        synchronized (syncObject) {
            syncObject.notifyAll();
        }
    }

    /**
     * Get the item at index position Position.
     */
    public ManagedIndexItem getItem(Position position, boolean doLoadData) throws IOException {
	return getItem(position.value(), doLoadData);  // sclayman 7/9/04
    }

    /**
     * Get the item at index position Position.
     */
    public abstract ManagedIndexItem getItem(long position, boolean doLoadData) throws IOException ;

    /**
     * Read the contents of the item
     * It assumes the index file is alreayd open for writing.
     * @param offset the byte offset in the file to start reading an item from
     * @param withData read the data for this IndexItem if withData is true,
     * the data needs to be read at a later time, otherwise
     */
    public ManagedIndexItem readItem(Offset offset, boolean withData) throws IOException {
	return readItem(offset.value(), withData);
    }

    /**
     * Read the contents of the item
     * It assumes the index file is alreayd open for writing.
     * @param startOffset the byte offset in the file to start reading an item from
     * @param withData read the data for this IndexItem if withData is true,
     * the data needs to be read at a later time, otherwise
     */
    public ManagedIndexItem readItem(long startOffset, boolean withData) throws IOException {
	// tmp var for reading index item values
	Timestamp indexTS = null;
	Timestamp dataTS = null;
	DataAbstraction data = null;
	long offset = -1;
	long size = 0;
	int type = DataType.NOTSET_VALUE;
	long id = 0;
	long annotationValue = 0;
	ManagedFileIndexItem indexItem = null;
	
	// goto an offset in the index
	seekToIndex(startOffset);

	// where are we in the index file
	long currentIndexPosition = indexChannelPosition;

	// read an IndexItem into indexBufRead
	readIndexItem(currentIndexPosition);

	// we read the right amount, so carry on

	indexTS = timestampDecoder.decode(indexBufRead.getLong());
	dataTS = timestampDecoder.decode(indexBufRead.getLong());
	offset = indexBufRead.getLong();
	size = indexBufRead.getLong();
	type = indexBufRead.getInt();
	id = indexBufRead.getLong();
	annotationValue = indexBufRead.getLong();

        try { // sclayman 20130122

            if (type == DataType.REFERENCE_VALUE) {
                data = readReferenceData(offset, size);
                indexItem = new FileIndexItem(dataTS, indexTS, data, new Size(0),  DataType.REFERENCE, new SID(id), annotationValue);
                ((IndexReferenceDataHolder)data).setIndexItem(indexItem);

            } else {
                data = readNormalData(offset, size, withData);
                indexItem = new FileIndexItem(dataTS, indexTS, data, DataTypeDirectory.find(type), new SID(id), annotationValue);

            }
        } catch (Error e) {
            
            System.err.println("readItem() Error: " + e.getMessage() +
                               " startOffset = " + startOffset +
                               " indexTS = " + indexTS +
                               " dataTS = " + dataTS +
                               " offset = " + offset +
                               " size = " + size +
                               " type = " + type +
                               " id = " + id +
                               " indexChannelPosition = " + indexChannelPosition);



            throw e;

        }

	// tell the IndexItem where its index is
	indexItem.setIndexOffset(new Offset(currentIndexPosition));

	indexItem.setDataOffset(new Offset(offset));

	indexItem.setIndex(getIndex());


	//System.err.println("Item size = (52 + " + size + ")");
			   
	return indexItem;
    }

    /**
     * Read some data, from a specified offset for a number of bytes.
     */
    protected DataAbstraction readNormalData(long offset, long size, boolean withData) throws IOException{
	DataAbstraction data = null;

	if (withData) {	// go and get the data now, if it's needed
	    // TODO: add code that checks how big the data
	    // actually is.
	    // only read it if the index isn't too big
	    // and the data isn't too big
	    ByteBuffer buffer = readData(offset, size);

	    // we got the data successfully, so build a DataHolderObject
	    //System.err.println("AbstractIndexIO: readNormalData. size=" + size);
	    data = new DataHolderObject(buffer, new Size(size));
	} else {	    // don;t get the data now
	    skipData(offset, size);

	    //System.err.println("AbstractIndexIO: readNormalData. DataReferenceObject offset=" + offset + " size="+size);
	    // no need to get the  data, so build a DataReferenceObject
	    data = new DataReferenceObject(new Offset(offset), new Size(size));
	}

	return data;
    }

    /**
     * Read a reference, from a specified offset for a number of bytes.
     */
    protected IndexReferenceDataHolder readReferenceData(long offset, long size) throws IOException{
	IndexReferenceDataHolder data = null;

	ByteBuffer buffer = readData(offset, size);

	// the id of the other Index
	long indexIDValue = buffer.getLong();

	// the id as an ID object
	ID indexID = new SID(indexIDValue);
	// the position of the IndexItem in the other Index
	long itemPosition =  buffer.getLong();

	//System.err.println("Read reference " + indexID + " @ " + itemPosition);
	data = new IndexReferenceDataHolder(indexID , new AbsolutePosition(itemPosition));

	return data;	
    }

    /**
     * Read an IndexItem given an offset.
     * @param offset the byte offset in the file to start reading an item from
     */
    public ByteBuffer readIndexItem(Offset offset, long size) throws IOException {
	return readIndexItem(offset.value());
    }

    /**
     * Read an IndexItem given an offset.
     * @param offset the byte offset in the file to start reading an item from
     */
    public ByteBuffer readIndexItem(long offset) throws IOException {
	int readCount = 0;


	// goto a position in the index
	seekToIndex(offset);

	//System.err.println("P(R) = " + indexChannel.position());

	// clear the index buf
	indexBufRead.clear();

	// read a block of data
	if ((readCount = indexChannel.read(indexBufRead)) != INDEX_ITEM_SIZE) {
	    throw new IOException("Index Item too short: position = " +
				  offset + " read count = " + readCount);
	}

	// make buffer ready to get data from
	indexBufRead.flip();

	// update indexChannelPosition
	indexChannelPosition += readCount;

	return indexBufRead;
    }

    /**
     * Read some data, given an offset and a size.
     * @param offset the byte offset in the file to start reading an item from
     * @param size the number of bytes to read
     */
    public ByteBuffer readData(Offset offset, long size) throws IOException {
	return readData(offset.value(), size);
    }


    /**
     * Read some data, given an offset and a size.
     * @param offset the byte offset in the file to start reading an item from
     * @param size the number of bytes to read
     */
    public ByteBuffer readData(long offset, long size) throws IOException {
	ByteBuffer buffer = null;
	long readCount = 0;

	if (size < 0) {
	    throw new Error("AbstractFileIO: readItem() can;t have size < 0");
	} else if (size >= Integer.MAX_VALUE) {
	    // buffers can only be so big
	    // check we can allocate one big enough
		throw new Error("AbstractFileIO: readItem() has not YET implemented reading of data > " + Integer.MAX_VALUE + ". Actual size is " + size);
	} else if (size <= 4096) {
	    // the data is less than a page size so read it
	    // allocate a buffer
	    buffer = ByteBuffer.allocate((int)size);

	    // seek to the right place
	    seekToData(offset);

	    // read some data
	    readCount = readDataIntoBuffer(buffer, size);

	    // check got correct amount
	    if (readCount == size) {
		buffer.limit((int)size);
		buffer.position(0);

		return buffer;
	    } else {
		// got wrong amount
		throw new IOException("IO Error trying to read " + size + " bytes from offset " + offset);
	    }

	} else {
	    // the data is bigger than a page size
	    // so its better to
	    // getting data using memory mapping
	    buffer = memoryMapData(offset, size);
	    seekToData(offset+size);
	    return buffer;

	}
    }


    /**
     * Actually read in the data.
     */
    protected abstract long readDataIntoBuffer(ByteBuffer buffer, long size) throws IOException;

    /**
     * Read some data, given a DataReference.
     */
    public ByteBuffer readData(DataReference ref) throws IOException {
	long offset = ref.getOffset().value();
	long size = ref.getSize().value();

	return readData(offset, size);	
    }

    /**
     * Memory map some data from a channel.
     */
    protected abstract ByteBuffer memoryMapData(long offset, long size) throws IOException ;


    /**
     * Seek to a certain position.
     * @return true if actually had to move the position,
     * returns false if in correct place
     */
    protected boolean seekToIndex(Offset offset) throws IOException {
	return seekToIndex(offset.value());
    }

   /**
     * Seek to a certain position.
     * @return true if actually had to move the position,
     * returns false if in correct place
     */
    protected  abstract boolean seekToIndex(long position) throws IOException;

    /**
     * Seek to a certain position.
     * @return true if actually had to move the position,
     * returns false if in correct place
     */
    protected boolean seekToData(Offset offset) throws IOException {
	return seekToData(offset.value());
    }

    /**
     * Seek to a certain position in the data file.
     * @return true if actually had to move the position,
     * returns false if in correct place
     */
    protected abstract boolean seekToData(long position) throws IOException;

    /**
     * Skip over some data, given an offset and a size.
     * @param offset the byte offset in the file to start reading an item from
     * @param size the number of bytes to read
     */
    public boolean skipData(long offset, long size) throws IOException {
	// skip to right place
	seekToData(offset + size);

	return true;
    }

    /**
     * Read some data, given a DataReference
     * and return it as a DataHolderObject.
     */
    public DataHolderObject convertDataReference(DataReference dataReference) {
	try { 
	    ByteBuffer rawData = readData(dataReference);
	    return new DataHolderObject(rawData, dataReference.getSize());

 	} catch (IOException ioe) {
	    ioe.printStackTrace();
	    return null;
	}
   }


    /**
     * Load the index
     */
    public long loadIndex(LoadStyle loadStyle) throws IOException {
	gotoFirstPosition();

	if (loadStyle == LoadStyle.ALL) {
	    indexAppendPosition = loadAll(true);
	    setAppendPosition();
	    return indexAppendPosition;

	} else if (loadStyle == LoadStyle.HOLLOW) {
	    indexAppendPosition = loadAll(false);
	    setAppendPosition();
	    return indexAppendPosition;

	} else if (loadStyle == LoadStyle.NONE) {
            if (headerInteractor.getLength() == 0) {
		 // the index has zero items
		// so there is nothing to read
                setAppendPosition();
	    } else {
		calculateAppendPosition();
	    }

	    return indexAppendPosition;

	} else {
	    throw new RuntimeException("Unknow LoadStyle" + loadStyle);
	}
    }

    /**
     * Load all of the items.
     * @return the position in the index after loading all the items
     */
    private long loadAll(boolean doLoadData) throws IOException {
	ManagedIndexItem item = null;
	long position = indexChannelPosition;
	long itemCount = headerInteractor.getLength();
	long count = 0;


	// we have just read the header
	// so we are going to read all the items
	
        if (itemCount == 0) {
	    //System.err.println("LoadAll: 0 items");
	    // the index has zero items
	    // so there is nothing to read
	    gotoFirstPosition();
	} else {
	    for (count=0; count < itemCount; count++) {
		// read an item
		item =  readItem(position, doLoadData);
		//item = getItem(count, doLoadData);

		// set the position for next time
		position = indexChannelPosition;

		// post the read item into the index
		// this is the Index callback
		getIndex().retrieveItem(item, count);
	    }
	}

	return indexChannelPosition;

    }

    /**
     * Get the append position
     */
    public long getAppendPosition() {
	return indexAppendPosition;
    }

    /**
     * Goto the append position
     */
    public boolean gotoAppendPosition() throws IOException {
	return seekToIndex(indexAppendPosition);
    }

    /**
     * Goto the first position
     */
    public boolean gotoFirstPosition() throws IOException {
	return seekToIndex(indexFirstPosition);
    }

    /**
     * Set the append position from the indexChannelPosition.
     */
    public boolean setAppendPosition() throws IOException {
	//System.err.println("Index append position = " + indexChannelPosition);
	indexAppendPosition = indexChannelPosition;

	return true;
    }

    /**
     * Calculate the append position from the last item of the index.
     */
    public abstract long calculateAppendPosition() throws IOException; 

    /**
     * Set the index item size.
     * The size is determined by the header I/O object
     * at index create time.
     * Return the old index item size in bytes.
     */
    protected IndexFileInteractor setItemSize(int itemSize) {	
	INDEX_ITEM_SIZE = itemSize;
	return this;
    }

    /**
     * Get a write-lock on this index.
     */
    public FileLock getWriteLock() {
	// Lock the Index, by locking the header
	FileLock newLock = headerInteractor.getWriteLock();


	if (newLock == null) {
	    if (hasBeenLocked == false) {
		// no lock was returned, so it's locked by someone else
		// we remeber it's been locked
		//System.err.println(originalIndexSpecifier + " first lock observation");
		hasBeenLocked = true;
		return null;
	    } else {
		// no lock was returned, and hasBeenLocked is true
		// so we've notived it's been locked before
		//System.err.println(originalIndexSpecifier + " still locked");
		return null;
	    }
	} else {
	    // got given a lock, so we have it locked

	    // we have to check if the Index has been locked before
	    // if it has, then it's state may have changed since
	    // we opened it, so we need to get the Index's current state.
	    // This is done by rereading the index data.
	    if (hasBeenLocked) {
		//System.err.println(originalIndexSpecifier + " has been locked and now free");

		/*
		// temporary
		releaseWriteLock();
		return null;
		*/

		//System.err.println(originalIndexSpecifier + " rereading meta data");
		try {
		    headerInteractor.read();
		    readMetaData();
		    loadIndex(LoadStyle.NONE);
		    return newLock;
		} catch (IOException ioe) {
		    releaseWriteLock();
		    return null;
		} catch (IndexOpenException ioe) {
		    releaseWriteLock();
		    return null;
		}

	    } else {
		//System.err.println(originalIndexSpecifier + "  LOCKED");
		return newLock;
	    }
	}
    }

    /**
     * Release a FileLock.
     */
    public boolean releaseWriteLock() {
	return headerInteractor.releaseWriteLock();
    }

    /**
     * Has the Index been write-locked.
     */
    public boolean isWriteLocked() {
	return headerInteractor.isWriteLocked();
    }

    /*
     * The following methods are those associated with the
     * thread activity of this class.
     */

    /**
     * Add some work to the write queue.
     */
    public synchronized void requestWriteWork(FileChannel channel, ByteBuffer flushBuffer, ByteBufferRing ring) {
	writeQueue.add(new WriteRequest(channel, flushBuffer, ring));
    }

    /**
     * Add some work to the read queue.
     */
    public synchronized void requestReadWork(Position position, boolean doLoadData) {
	readQueue.add(new ReadRequest(position, doLoadData));
    }

    /**
     * Write the contents of the ffirst ByteBuffer in the work queue
     * to a FileChannel.
     * It assumes the index file is alreayd open for writing.
     */
    public synchronized long writeFromWorkQueue() throws IOException  {
	long written = 0;

	// System.err.println("AbstractFileIO:writeFromWorkQueue() writeQueue length = " + writeQueue.size());

	// get the buffer from the queue
	WriteRequest writeRequest = (WriteRequest)writeQueue.getFirst();
	// remove the buffer from the queue
	writeQueue.removeFirst();

	// get the write request details
	FileChannel channel = writeRequest.channel;
	ByteBuffer buffer = writeRequest.buffer;
	ByteBufferRing ring = writeRequest.ring;

	// write out the buffer
	written += channel.write(buffer);

	// clear it
	buffer.clear();

	// unlock it, and make it ready for use
	ring.unlock(buffer);

	return written;
    }

    /**
     * This drains the write request queue by processing
     * all the WriteRequests.
     */
    public  void drainWriteQueue() throws IOException {
	//System.err.println(getThread() + ": drainWriteQueue length = " + writeQueue.size());

	while (writeQueue.size() > 0) {
	    writeFromWorkQueue();
	}
    }

    /**
     * Wait for the timeout to go off.
     * @return true if the timeout happened, false if it dod not.
     */
    public boolean timeOut(long timeout) {
        synchronized (syncObject) {
            try {
                timeoutHappened = true;

                // Now wait.
                // Other methods can set timeoutHappened
                // during the wait.
                syncObject.wait(timeout);

                // if we get to here an timeoutHappened is still true
                // the wait ended because the amount of time has expired
                // if timeoutHappened is false, then it was set false
                // by a sendItem() or a receiveItem().
                if (timeoutHappened) {
                    return true;
                } else {
                    return false;
                }
            } catch (InterruptedException ie) {
                //System.err.println(getIndex().getName() + " wait interrupted");
                // a timeout didn;t happen, it was an interrupt
                timeoutHappened = false;
                return false;
            }
        }
    }

    public boolean awaitWork() {
	// wait for some work
	timeOut(5 * 1000);

	if (timeoutHappened) {
	    // there was a timeout, i.e. no work
	    //System.err.println(getIndex().getName() + " awaitWork() sleep finished. No work");
	    return false;
	} else {
	    // we got some work
	    return true;
	}
    }

    /**
     * The Thread run method.
     */
    public void run() {
	try {
	    while (isRunning()) {

		if (awaitWork() == true) {
                    if (!isRunning()) {
                        break;
                    } else {
                        drainWriteQueue();
                    }

		} else {
                    if (!isRunning()) {   // on thread stop
                        break;
                    } else {              // timeoutHappened
                        if (! headerInteractor.isReadOnly()) {
                            // the index is open for read and write
                            // so occassionally flush the header
                            //headerInteractor.flush();
                            //System.err.println(getClass().getSimpleName() + " " + getIndex().getName() + " flush() in thread run() " + myThread);
                        }
                    }
		}
	    }

	} catch (IOException ioe) {
	    System.err.println("Run Got IOException: " + ioe);
	    ioe.printStackTrace();
	}

        // reduce latch count by 1
        latch.countDown();

        endOfRun = true;

	//System.err.println(getClass().getSimpleName() + " " + getIndex().getName() + " Thread run end " + myThread);

    }
}
