// Position.java

package com.timeindexing.basic;

/**
 * A position in an index
 */
public interface Position extends Absolute, Cloneable {
    /**
     * Clone me
     */
    public Object clone() throws  CloneNotSupportedException;

    /**
     * Special End Of Index value.
     */
    public final static Position END_OF_INDEX = new Position() {
	    public long value() {
		return Long.MIN_VALUE;
	    }

	    public String toString() {
		return "END_OF_INDEX";
	    }
	    
	    public Object clone() throws  CloneNotSupportedException {
		return super.clone();
	    }
	};

    /**
     * Special Too Low value.
     */
    public final static Position TOO_LOW = new Position() {
	    public long value() {
		return Long.MIN_VALUE;
	    }

	    public String toString() {
		return "TOO_LOW";
	    }
	    
	    public Object clone() throws  CloneNotSupportedException {
		return super.clone();
	    }
	};

    /**
     * Special Too High value.
     */
    public final static Position TOO_HIGH = new Position() {
	    public long value() {
		return Long.MAX_VALUE;
	    }

	    public String toString() {
		return "TOO_HIGH";
	    }
	    
	    public Object clone() throws  CloneNotSupportedException {
		return super.clone();
	    }
	};
}
