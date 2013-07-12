/*
 * Queue.java
 *
 * Version 1.0
 * 
 * 17 May 2013
 */
package com.bixly.pastevid.structs;

import java.util.Vector;

/**
 * Implementation of the queue data structure.
 * @author cevaris
 */
public class Queue<T> {
    
    private Vector<T> vector = new Vector<T>();
    private boolean stopWaiting=false;
    private boolean waiting=false;
    
    /** 
     * Put the object into the queue.
     * 
     * @param	object		the object to be appended to the
     * 				queue. 
     */
    public synchronized void push(T object) {
	vector.addElement(object);
	notify();
    }
    
    /**
     * Pull the first object out of the queue. Wait if the queue is
     * empty.
     */
    public synchronized T pop() {
	while (isEmpty()) {
	    try {
		waiting = true;
		wait();
	    } catch (InterruptedException ex) {
	    }
	    waiting = false;
	    if ( stopWaiting ) {
                return null;
            }
	}
	return get();
    }

    /**
     * Get the first object out of the queue. Return null if the queue
     * is empty. 
     */
    private synchronized T get() {
	T object = peek();
	if (object != null)
	    vector.removeElementAt(0);
	return object;
    }

    /**
     * Peek to see if something is available.
     */
    public T peek() {
	if (isEmpty()) {
            return null;
        }
	return vector.elementAt(0);
    }
    
    /**
     * Checks if the queue is empty.
     * @return True if empty
     */
    public boolean isEmpty() {
	return vector.isEmpty();
    }

    /**
     * Returns the number of elements in the queue.
     * @return 
     */
    public int size() {
	return vector.size();
    }

    public synchronized void clear() {
        vector.clear();
    }
}
