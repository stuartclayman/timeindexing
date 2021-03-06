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



// KeepEverythingPolicy.java

package com.timeindexing.cache;

import com.timeindexing.util.DoubleLinkedList;

/**
 * Keep every IndexItem and it's data.
 */
public class KeepEverythingPolicy extends AbstractCachePolicy implements CachePolicy {
    /**
     * Construct this policy object
     */
    public KeepEverythingPolicy() {
	monitorList = new DoubleLinkedList();
    }

}
