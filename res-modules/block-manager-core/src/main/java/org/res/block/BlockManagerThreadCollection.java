//  Copyright (c) 2024 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License
//  
//  In the context of this license, a 'Patron' means any individual who has made a 
//  membership pledge, a purchase of merchandise, a donation, or any other 
//  completed and committed financial contribution to Robert Elder Software Inc. 
//  for an amount of money greater than $1.  For a list of ways to contribute 
//  financially, visit https://blog.robertelder.org/patron
//  
//  Permission is hereby granted, to any 'Patron' the right to use this software 
//  and associated documentation under the following conditions:
//  
//  1) The 'Patron' must be a natural person and NOT a commercial entity.
//  2) The 'Patron' may use or modify the software for personal use only.
//  3) The 'Patron' is NOT permitted to re-distribute this software in any way, 
//  either unmodified, modified, or incorporated into another software product.
//  
//  An individual natural person may use this software for a temporary one-time 
//  trial period of up to 30 calendar days without becoming a 'Patron'.  After 
//  these 30 days have elapsed, the individual must either become a 'Patron' or 
//  stop using the software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
//  SOFTWARE.
package org.res.block;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.lang.Thread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.lang.InterruptedException;

public class BlockManagerThreadCollection {

	private Object lock = new Object();
	private boolean shutdownNotifiesSent = false; //  This variable prevents a cascade of 'shutdown' notifies from every other task once shutdown is triggered by one task.
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected Object shutdownLock = new Object();
	protected boolean isFinished = false;
	protected List<Exception> offendingExceptions = new ArrayList<Exception>();
	private Map<Long , Thread> allThreads = new HashMap<Long, Thread>();

	public BlockManagerThreadCollection() throws Exception {

	}

	public void addThread(Thread t){
		t.start();
		this.allThreads.put(t.getId(), t);
	}

	public void addThreads(List<Thread> threads){
		for(Thread t : threads){
			this.addThread(t);
		}
	}

	public List<Exception> getOffendingExceptions(){
		return this.offendingExceptions;
	}

	public void sendShutdownNotifies() throws Exception{
		// Initiates an orderly shutdown in which previously submitted tasks are executed, but
		// no new tasks will be accepted. Invocation has no additional effect if already shut down.
		for(Map.Entry<Long, Thread> e : this.allThreads.entrySet()){
			Long threadId = e.getKey();
			Thread t = e.getValue();
			logger.info(Thread.currentThread().getClass().getName() + " " + Thread.currentThread() + " " + Thread.currentThread().getId() + " " + t.getClass().getName() + " " + t + " " + t.getId() + " == " + Thread.currentThread().equals(t));
			if(t instanceof KeyboardInputReaderTask){
				try{
					//  Close stdin to trigger exit of 'read' call for keyboard input task:
					logger.info("Calling System.in.close() for thread " + t.getClass().getName() + "...");
					System.in.close();
				}catch (IOException ex){
					this.offendingExceptions.add(ex);
				}
			}else{
				if(Thread.currentThread().equals(t)){
					logger.info("Don't interrupt the current thread because it's the one interrupting the others: " + t.getClass().getName() + "...");
				}else{
					logger.info("t.isInterrupted()=" + t.isInterrupted() + ". Calling t.interrupt() on thread " + t.getClass().getName() + "...");
					t.interrupt();
					logger.info("t.isInterrupted()=" + t.isInterrupted() + " now on thread " + t.getClass().getName() + "...");
				}
			}
		}
	}

	public void setIsFinished(boolean isFinished, Exception e){
		synchronized(lock){
			this.isFinished = isFinished;
			if(e != null){
				this.offendingExceptions.add(e);
			}
			if(this.isFinished){
				try{
					if(!this.shutdownNotifiesSent){
						this.sendShutdownNotifies();
						this.shutdownNotifiesSent = true;
					}
				}catch(Exception exception){
					exception.printStackTrace();
				}
			}
		}
	}

	public void blockUntilAllTasksHaveTerminated(){
		//  Wait for all threads to finish:
		for(Map.Entry<Long, Thread> e : this.allThreads.entrySet()){
			try{
				e.getValue().join();
			}catch(InterruptedException ex){
			}
		}
	}

	public boolean getIsFinished(){
		return this.isFinished;
	}
}
