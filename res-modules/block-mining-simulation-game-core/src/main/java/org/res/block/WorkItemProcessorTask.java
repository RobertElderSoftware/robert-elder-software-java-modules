//  Copyright (c) 2025 Robert Elder Software Inc.
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

import java.lang.Runnable;
import java.lang.Thread;
import java.lang.reflect.ParameterizedType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class WorkItemProcessorTask<T extends WorkItem> extends BlockManagerThread {

	protected Class<T> workItemClass;
	protected Class<?> descriptiveClass;
	private WorkItemQueueOwner<T> workItemQueueOwner;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private boolean isThreadFinished = false;

	public WorkItemProcessorTask(WorkItemQueueOwner<T> workItemQueueOwner, Class<T> workItemClass, Class<?> descriptiveClass){
		this.workItemQueueOwner = workItemQueueOwner;
		this.workItemClass = workItemClass;
		this.descriptiveClass = descriptiveClass;
	}

	public void setIsThreadFinished(boolean isThreadFinished){
		this.isThreadFinished = isThreadFinished;
	}

	public Class<?> getDescriptiveClass(){
		return this.descriptiveClass;
	}

	public Class<T> getWorkItemClass(){
		return this.workItemClass;
	}

	public boolean getIsThreadFinished(){
		return this.isThreadFinished;
	}

	@Override
	public String getBetterClassName() throws Exception{
		return this.getClass().getName() + "<" + this.workItemClass.getName() + ", " + this.descriptiveClass.getName() + ">, thread.threadId()=" + threadId();
	}

	@Override
	public void run() {
		logger.info("Begin running WorkItemProcessorTask (id=" + Thread.currentThread().threadId() + ") for " + this.workItemQueueOwner.getClass().getName());
		currentThread().setName(this.descriptiveClass.asSubclass(this.descriptiveClass).getSimpleName() + " for " + this.getClass().getSimpleName());
		while (
			!this.workItemQueueOwner.getBlockManagerThreadCollection().getIsProcessFinished() &&
			!this.getIsThreadFinished() &&
			!Thread.currentThread().isInterrupted()
		) {
			try {
				try{
					//  If this thread is about to block on an empty work item queue, spend a bit of time
					//  on background tasks:
					while(
						!this.workItemQueueOwner.getBlockManagerThreadCollection().getIsProcessFinished() &&
						!this.getIsThreadFinished() &&
						!Thread.currentThread().isInterrupted() && 
						this.workItemQueueOwner.getWorkItemQueueSize() == 0 &&
						this.workItemQueueOwner.doBackgroundProcessing()
					){ }
					logger.info("before take " + this.workItemQueueOwner.getClass().getName());
					WorkItem workItem = this.workItemQueueOwner.takeWorkItem();
					logger.info("after take " + this.workItemQueueOwner.getClass().getName());
					workItem.doWork();
				}catch(InterruptedException e){
					if(this.getIsThreadFinished()){ // If we're only shutting down the current thread, there is no need to shut down all the others:
						logger.info("Caught a InterruptedException from takeWorkItem, and this.getIsThreadFinished()=" + this.getIsThreadFinished() + ".  Gracefully exit for " + this.workItemQueueOwner.getClass().getName());
					}else{
						logger.info("Caught a InterruptedException from takeWorkItem.  Set isFinished = true and gracefully exit for " + this.workItemQueueOwner.getClass().getName());
						this.workItemQueueOwner.getBlockManagerThreadCollection().setIsProcessFinished(true, null);
					}
				}
			} catch (Exception e) {
				/*  Exit immediately if there is any unexpected exception so it can be detected and debugged. */
				logger.error("Exception in WorkItemProcessorTask for " + this.workItemQueueOwner.getClass().getName() + ":", e);
				this.workItemQueueOwner.getBlockManagerThreadCollection().setIsProcessFinished(true, e);
			}
		}
		logger.info("Exiting WorkItemProcessorTask (id=" + Thread.currentThread().threadId() + "), exiting thread getName()=" + this.workItemQueueOwner.getClass().getName() + ", getIsProcessFinished()=" + this.workItemQueueOwner.getBlockManagerThreadCollection().getIsProcessFinished() + ", isInterrupted()=" + Thread.currentThread().isInterrupted() + ", this.getIsThreadFinished()=" + this.getIsThreadFinished());
	}
}
