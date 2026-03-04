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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class InMemoryChunksMockClient implements InMemoryChunksClient {

	private Set<CuboidAddress> previousRequiredChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> previousRequiredRegions = new TreeSet<CuboidAddress>();

	private Set<CuboidAddress> chunksExpectedToBecomePending = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> pendingChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> subscribedChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> requestedNotYetReceivedChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> receivedAndSubscribedChunks = new TreeSet<CuboidAddress>();

	private CuboidAddress chunkSize;

	public InMemoryChunksMockClient(CuboidAddress chunkSize){
		this.chunkSize = chunkSize;
	}

	public Set<CuboidAddress> getChunksExpectedToBecomePending(){
		return this.chunksExpectedToBecomePending;
	}

	public void updateRequiredRegions(Set<CuboidAddress> currentRequiredRegions) throws Exception{
		Set<CuboidAddress> currentRequiredChunks = new TreeSet<CuboidAddress>();
		for(CuboidAddress ca : currentRequiredRegions){
			currentRequiredChunks.addAll(ca.getIntersectingChunkSet(this.chunkSize));
		}
		this.chunksExpectedToBecomePending.addAll(currentRequiredChunks);
		this.chunksExpectedToBecomePending.removeAll(this.previousRequiredChunks);
		this.previousRequiredChunks = currentRequiredChunks;
		this.previousRequiredRegions = currentRequiredRegions;
	}

	public void inMemoryChunksCallbackOnChunkBecomesPending(CuboidAddress ca) throws Exception{
		if(this.chunksExpectedToBecomePending.contains(ca)){
			this.chunksExpectedToBecomePending.remove(ca);
			this.pendingChunks.add(ca);
		}else{
			throw new Exception("Did not expect chunk " + ca);
		}
	}

	public void inMemoryChunksCallbackOnEnqueueChunkUnsubscriptionForServer(List<CuboidAddress> cuboidAddresses) throws Exception{
		for(CuboidAddress ca : cuboidAddresses){
			subscribedChunks.remove(ca); //  No longer subscribed to this chunk
			chunksExpectedToBecomePending.remove(ca); //  No need to request
			pendingChunks.remove(ca); //  No need to request
			requestedNotYetReceivedChunks.remove(ca);
			receivedAndSubscribedChunks.remove(ca);
		}
	}

	public void inMemoryChunksCallbackOnEnqueueChunkRequestToServer(List<CuboidAddress> cuboidAddresses) throws Exception{
		for(CuboidAddress ca : cuboidAddresses){
			pendingChunks.remove(ca); //  No longer waiting to request
			requestedNotYetReceivedChunks.add(ca); //  Subscribe to this chunk
		}
	}

	public void inMemoryChunksCallbackOnChunkBecomesAvailable(CuboidAddress ca) throws Exception{
		requestedNotYetReceivedChunks.remove(ca);
		receivedAndSubscribedChunks.add(ca);
	}

	public Long getAuthorizedClientId() throws Exception{
		return 0L;
	}
}
