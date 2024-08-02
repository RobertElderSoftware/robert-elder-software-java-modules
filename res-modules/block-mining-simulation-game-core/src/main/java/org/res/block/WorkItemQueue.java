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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.res.block.WorkItem;
import org.res.block.BlockSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class WorkItemQueue<T extends WorkItem> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private int MAX_ITEMS = 10000;
	private PriorityBlockingQueue<PrioritizedWorkItem<T>> priorityBlockingQueue;
	private int numPriorities = WorkItemPriority.size;
	private Map<Long, BlockingQueue<WorkItemResult>> blockingWorkItemResults = new ConcurrentHashMap<Long, BlockingQueue<WorkItemResult>>();

	public WorkItemQueue() {
		this.priorityBlockingQueue = new PriorityBlockingQueue<PrioritizedWorkItem<T>>(MAX_ITEMS, new PrioritizedWorkItemComparator<T>());
	}

	public int size(){
		return this.priorityBlockingQueue.size();
	}

	private void putWorkItemInternal(T workItem, WorkItemPriority priority) throws Exception {
		while (true) {
			try {
				if(this.priorityBlockingQueue.size() == this.MAX_ITEMS){
					throw new Exception("Blocking queue is full, MAX_ITEMS=" + MAX_ITEMS + ".  Expect a deadlock.  TODO: make this work better. ");
				}
				this.priorityBlockingQueue.put(new PrioritizedWorkItem<T>(workItem, priority));
				return;
			} catch (InterruptedException e) {
				logger.info("produceWorkItem InterruptedException");
			}
		}
	}
	public void putWorkItem(T workItem, WorkItemPriority priority) throws Exception {
		if(workItem.getIsBlocking()){
			throw new Exception("Expected a non-blocking work item.");
		}else{
			this.putWorkItemInternal(workItem, priority);
		}
	}

	protected T takeWorkItem() throws Exception {
		T workItem = this.priorityBlockingQueue.take().getWorkItem();
		return workItem;
	}

	public WorkItemResult putBlockingWorkItem(T workItem, WorkItemPriority priority) throws Exception {
		/*
		 * This method should always be called from another thread that wants to block
		 * waiting for a response from this worker thread.
		 */
		if(workItem.getIsBlocking()){
			Long currentThreadId = workItem.getThreadId();
			if(!blockingWorkItemResults.containsKey(currentThreadId)){
				blockingWorkItemResults.put(currentThreadId, new LinkedBlockingDeque<WorkItemResult>());
				logger.info("Added a new result queue for blocking work items that will be returned to thread_id=" + currentThreadId);
			}
			this.putWorkItemInternal(workItem, priority);
			return blockingWorkItemResults.get(currentThreadId).take();
		}else{
			throw new Exception("Expected a blocking work item.");
		}
	}

	public void addResultForThreadId(WorkItemResult workItemResult, Long threadId) throws Exception {
		logger.info("About to add a result queue item for blocking work items that will be returned to thread_id=" + threadId);
		this.blockingWorkItemResults.get(threadId).put(workItemResult);
	}
}
