/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bixly.util;

import java.util.Vector;

/**
 *
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

    /** Break the pull(), allowing the calling thread to exit
     */
    private synchronized void stop() {
	stopWaiting=true;
	// just a hack to stop waiting 
	if( waiting ) notify();
    }
    
    /**
     * Pull the first object out of the queue. Wait if the queue is
     * empty.
     */
    public synchronized T pop() {
	while (isEmpty()) {
	    try {
		waiting=true;
		wait();
	    } catch (InterruptedException ex) {
	    }
	    waiting=false;
	    if( stopWaiting ) return null;
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
	if (isEmpty())
	    return null;
	return vector.elementAt(0);
    }
    
    /**
     * Is the queue empty?
     */
    public boolean isEmpty() {
	return vector.isEmpty();
    }

    /**
     * How many elements are there in this queue?
     */
    public int size() {
	return vector.size();
    }
}
