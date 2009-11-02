/*
 * Copyright 2003-2009 Stuart Clayman
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



// Predicate.java

package com.timeindexing.index;

/**
 * An interface for predicate functions that are passed to filter().
 * <p>
 * The is zero function can be coded as:
 * <tt>
 * Predicate isZero = new Predicate() {
 *     public boolean test(IndexItem item) {
 *         Number n = (Number)item.getDataItem().getObject();
 *         return (n.intValue() == 0);
 *     }
 * }
 * </tt>
 */
public interface Predicate {
    /**
     * Evaluate an IndexItem in some way.
     * @return true, if the predicate is true
     */
    public boolean test(IndexItem i);
}
