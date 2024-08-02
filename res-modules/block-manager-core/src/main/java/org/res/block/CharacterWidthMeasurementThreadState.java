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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;

import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class CharacterWidthMeasurementThreadState extends WorkItemQueueOwner<CharacterWidthMeasurementWorkItem> {
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private ClientBlockModelContext clientBlockModelContext;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private TextWidthMeasurementWorkItem currentTextWidthMeasurement = null;

	private BlockingQueue<TextWidthMeasurementWorkItem> pendingTextWidthRequests = new LinkedBlockingDeque<TextWidthMeasurementWorkItem>();

	public CharacterWidthMeasurementThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void addPendingTextWidthRequest(TextWidthMeasurementWorkItem w){
		this.pendingTextWidthRequests.add(w);
	}

	public void notifyOfCurrentCursorPosition(Long x, Long y){
		if(this.currentTextWidthMeasurement == null){
			logger.info("WARNING:  Discarding cursor position report (" + x + "," + y + ") since there appears to be no active text width request?");
		}else{
			this.currentTextWidthMeasurement.notifyOfCurrentCursorPosition(x, y);
		}
	}

	public CharacterWidthMeasurementWorkItem takeWorkItem() throws Exception {
		CharacterWidthMeasurementWorkItem w = this.workItemQueue.takeWorkItem();
		return w;
	}

	public void putWorkItem(CharacterWidthMeasurementWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public boolean doBackgroundProcessing() throws Exception{
		//  If there is a character width measurement request available, start working on it:
		if(currentTextWidthMeasurement == null && this.pendingTextWidthRequests.size() > 0){
			this.currentTextWidthMeasurement = this.pendingTextWidthRequests.take();
		}
		if(this.currentTextWidthMeasurement != null){
			TextWidthMeasurementWorkItemResult r = this.currentTextWidthMeasurement.getResult();
			if(r != null){ //  Did we actually get a result yet?
				this.addResultForThreadId(r, this.currentTextWidthMeasurement.getThreadId()); //  Unblock whatever thread was waiting.
				logger.info("In CharacterWidthMeasurementThreadState, Added a text width result for thread_id=" + this.currentTextWidthMeasurement.getThreadId());
				this.currentTextWidthMeasurement = null; //  Allow for processing of next character width request.
			}
		}
		return false; // There is no additional work we can do until we get another work item.
	}

	public WorkItemResult putBlockingWorkItem(CharacterWidthMeasurementWorkItem workItem, WorkItemPriority priority) throws Exception {
		return this.workItemQueue.putBlockingWorkItem(workItem, priority);
	}

	public void addResultForThreadId(WorkItemResult workItemResult, Long threadId) throws Exception {
		this.workItemQueue.addResultForThreadId(workItemResult, threadId);
	}
}
