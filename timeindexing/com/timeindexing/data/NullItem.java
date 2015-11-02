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



// NullItem.java

package com.timeindexing.data;

import com.timeindexing.index.DataType;
import java.nio.ByteBuffer;

/** 
 * An item that holds no data, but acts as a marker for the end of the data stream
 * has a time span from the previous item.
 */
public class NullItem extends AbstractDataItem implements DataItem {
    private static ByteBuffer buf = ByteBuffer.allocate(0);
    /**
     * Construct a NullItem
     */
    public NullItem() {
    }

    /**
     * Get the data itself
     */
    public ByteBuffer getBytes() {
        return buf;
    }

    /**
     * Get the size of the data item
     */
    public long getSize() {
        return 0;
    }

    /**
     * Get the DataType of the DataItem.
     */
    public DataType getDataType() {
        return DataType.VOID;
    }

    /**
     * Get the  object
     * @return null
     */
    public Object getObject() {
        return null;
    }

    /**
     * Stringify.
     */
    public String toString() {
	return ".";
    }

}


