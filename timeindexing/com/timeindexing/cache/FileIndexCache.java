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



// FileIndexCache.java

package com.timeindexing.cache;

import com.timeindexing.time.Timestamp;
import com.timeindexing.index.IndexItem;
import com.timeindexing.index.ManagedIndex;
import com.timeindexing.index.ManagedFileIndexItem;
import com.timeindexing.index.StoredIndex;
import com.timeindexing.index.DataHolderObject;
import com.timeindexing.index.DataReference;
import com.timeindexing.index.DataReferenceObject;
import com.timeindexing.index.DataAbstraction;
import com.timeindexing.index.PositionOutOfBoundsException;
import com.timeindexing.basic.AbsolutePosition;


import java.util.List;
import java.util.LinkedList;

/**
 * The implementation of a cache which holds the index items
 * for file indexes.
 */
public class FileIndexCache extends DefaultIndexCache implements IndexCache {
    /**
     * Create a FileIndexCache object.
     */
    public FileIndexCache(StoredIndex index) {
	super((ManagedIndex)index);
    }

}
