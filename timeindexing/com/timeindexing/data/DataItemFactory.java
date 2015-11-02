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



// DataItemFactory.java

package com.timeindexing.data;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.io.Serializable;

import com.timeindexing.index.DataType;

/**
 * The DataItemFactory is responsible for converting objects.
 * It can convert a ByteBuffer
 * into a DataItem, given a specific DataType.
 * It can convert an object into a DataItem.
 */
public class DataItemFactory {
    /**
     * Convert a ByteBuffer, given a DataType into a DataItem.
     */
    public DataItem convert(ByteBuffer theBuffer, DataType type) {
	if (type.equals(DataType.STRING)) {
	    return new StringItem(theBuffer);
	} else if (type.equals(DataType.INTEGER)) {
	    return new IntegerItem(theBuffer);
	} else if (type.equals(DataType.BIG_INTEGER)) {
	    return new BigIntegerItem(theBuffer);
	} else if (type.equals(DataType.BIG_DECIMAL)) {
	    return new BigDecimalItem(theBuffer);
	} else if (type.equals(DataType.LONG)) {
	    return new LongItem(theBuffer);
	} else if (type.equals(DataType.SHORT)) {
	    return new ShortItem(theBuffer);
	} else if (type.equals(DataType.FLOAT)) {
	    return new FloatItem(theBuffer);
	} else if (type.equals(DataType.DOUBLE)) {
	    return new DoubleItem(theBuffer);
	} else if (type.equals(DataType.CHAR)) {
	    return new CharItem(theBuffer);
	} else if (type.equals(DataType.BYTE)) {
	    return new ByteItem(theBuffer);
	} else if (type.equals(DataType.BOOLEAN)) {
	    return new BooleanItem(theBuffer);
	} else if (type.equals(DataType.VOID)) {
	    return new NullItem();
	} else if (type.equals(DataType.SERIALIZABLE)) {
	    return new SerializableItem(theBuffer);
	} else {
	    return new ByteBufferItem(theBuffer, type);
	}
    }

    /**
     * Convert an Object to a DataItem.
     * @throws IllegalArgumentException if the type of the object passed in 
     * cannot be converted to a DataItem
     */
    public DataItem convert(Object obj) throws IllegalArgumentException {
	if (obj instanceof String) {
	    return new StringItem((String)obj);
	} else if  (obj instanceof Integer) {
	    return new IntegerItem((Integer)obj);
	} else if  (obj instanceof BigInteger) {
	    return new BigIntegerItem((BigInteger)obj);
	} else if  (obj instanceof BigDecimal) {
	    return new BigDecimalItem((BigDecimal)obj);
	} else if  (obj instanceof Long) {
	    return new LongItem((Long)obj);
	} else if  (obj instanceof Short) {
	    return new ShortItem((Short)obj);
	} else if  (obj instanceof Float) {
	    return new FloatItem((Float)obj);
	} else if  (obj instanceof Double) {
	    return new DoubleItem((Double)obj);
	} else if  (obj instanceof Character) {
	    return new CharItem((Character)obj);
	} else if  (obj instanceof Byte) {
	    return new ByteItem((Byte)obj);
	} else if  (obj instanceof Boolean) {
	    return new BooleanItem((Boolean)obj);
	} else if  (obj instanceof Serializable) {
	    return new SerializableItem((Serializable)obj);
	} else if  (obj instanceof ByteBuffer) {
	    return new ByteBufferItem((ByteBuffer)obj);
	} else {
	    throw new IllegalArgumentException("Cannot convert " + obj.getClass().getName() + " into a DataItem.  Try using a Serializable");
	}
    }
}
