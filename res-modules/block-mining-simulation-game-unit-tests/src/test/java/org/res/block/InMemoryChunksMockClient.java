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

	protected Object lock = new Object();

	private Set<CuboidAddress> previousRequiredChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> previousRequiredRegions = new TreeSet<CuboidAddress>();

	private Map<CuboidAddress, Long> expectedPendingChunkSignals = new HashMap<CuboidAddress, Long>();
	private Map<CuboidAddress, Long> expectedAvailableChunkSignals = new HashMap<CuboidAddress, Long>();

	private Set<CuboidAddress> subscribedChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> requestedNotYetReceivedChunks = new TreeSet<CuboidAddress>();

	private CuboidAddress chunkSize;

	public InMemoryChunksMockClient(CuboidAddress chunkSize){
		this.chunkSize = chunkSize;
	}

	public void updateRequiredRegions(Set<CuboidAddress> currentRequiredRegions) throws Exception{
		synchronized(lock){
			System.out.println("MockClient START updateRequiredRegions, saw currentRequiredRegions=" + currentRequiredRegions);
			Set<CuboidAddress> currentRequiredChunks = new TreeSet<CuboidAddress>();
			for(CuboidAddress ca : currentRequiredRegions){
				currentRequiredChunks.addAll(ca.getIntersectingChunkSet(this.chunkSize));
			}
			System.out.println("MockClient currentRequiredChunks=" + currentRequiredChunks);
			System.out.println("MockClient this.previousRequiredChunks=" + this.previousRequiredChunks);

			for(CuboidAddress ca : currentRequiredChunks){
				//  We should have already gotten a signal from the previously pending chunks:
				//  TODO:  Maybe don't give signals for already loaded chunks?
				if(!(this.previousRequiredChunks.contains(ca))){
					this.incrementExpectedPendingSignalCount(ca);
				}
			}

			System.out.println("MockClient this.expectedPendingChunkSignals=" + this.expectedPendingChunkSignals);
			this.previousRequiredChunks = currentRequiredChunks;
			this.previousRequiredRegions = currentRequiredRegions;
			System.out.println("MockClient END updateRequiredRegions");
		}
	}

	public void incrementExpectedPendingSignalCount(CuboidAddress ca){
		if(expectedPendingChunkSignals.containsKey(ca)){
			Long newCount = expectedPendingChunkSignals.get(ca) + 1L;
			expectedPendingChunkSignals.put(ca, newCount);
			System.out.println("Incremented expected becomes pending singal count for ca=" + ca + " to " + newCount);
		}else{
			expectedPendingChunkSignals.put(ca, 1L);
			System.out.println("Incremented expected becomes pending singal count for ca=" + ca + " to " + 1L);
		}
	}

	public void decrementExpectedPendingSignalCount(CuboidAddress ca) throws Exception{
		if(expectedPendingChunkSignals.containsKey(ca)){
			Long currentValue = expectedPendingChunkSignals.get(ca);
			if(currentValue > 0L){
				Long newValue = currentValue - 1L;
				expectedPendingChunkSignals.put(ca, newValue);
				if(newValue.equals(0L)){ //  Don't consider in map anymore:
					expectedPendingChunkSignals.remove(ca);
				}
				System.out.println("Decremented expected becomes pending singal count for ca=" + ca + " to " + newValue);
			}else{
				throw new Exception("This should not happen, signal not expected.");
			}
		}else{
			throw new Exception("This should not happen, becomes pending signal not expected from chunk " + ca);
		}
	}

	public void incrementExpectedAvailableSignalCount(CuboidAddress ca){
		if(expectedAvailableChunkSignals.containsKey(ca)){
			Long newCount = expectedAvailableChunkSignals.get(ca) + 1L;
			expectedAvailableChunkSignals.put(ca, newCount);
			System.out.println("Incremented expected becomes available singal count for ca=" + ca + " to " + newCount);
		}else{
			expectedAvailableChunkSignals.put(ca, 1L);
			System.out.println("Incremented expected becomes available singal count for ca=" + ca + " to " + 1L);
		}
	}

	public void decrementExpectedAvailableSignalCount(CuboidAddress ca) throws Exception{
		if(expectedAvailableChunkSignals.containsKey(ca)){
			Long currentValue = expectedAvailableChunkSignals.get(ca);
			if(currentValue > 0L){
				Long newValue = currentValue - 1L;
				expectedAvailableChunkSignals.put(ca, newValue);
				if(newValue.equals(0L)){ //  Don't consider in map anymore:
					expectedAvailableChunkSignals.remove(ca);
				}
				System.out.println("Decremented expected becomes available singal count for ca=" + ca + " to " + newValue);
			}else{
				throw new Exception("This should not happen, signal not expected.");
			}
		}else{
			throw new Exception("This should not happen, becomes available signal not expected from chunk " + ca);
		}
	}

	public void inMemoryChunksCallbackOnChunkBecomesPending(CuboidAddress ca) throws Exception{
		synchronized(lock){
			System.out.println("MockClient inMemoryChunksCallbackOnChunkBecomesPending, saw ca=" + ca);
			decrementExpectedPendingSignalCount(ca);
		}
	}

	public void inMemoryChunksCallbackOnEnqueueChunkUnsubscriptionForServer(List<CuboidAddress> cuboidAddresses) throws Exception{
		synchronized(lock){
			System.out.println("MockClient inMemoryChunksCallbackOnEnqueueChunkUnsubscriptionForServer, saw cuboidAddresses=" + cuboidAddresses);
			for(CuboidAddress ca : cuboidAddresses){
				subscribedChunks.remove(ca); //  No longer subscribed to this chunk
				requestedNotYetReceivedChunks.remove(ca);
				expectedAvailableChunkSignals.remove(ca);
			}
		}
	}

	public void inMemoryChunksCallbackOnEnqueueChunkRequestToServer(List<CuboidAddress> cuboidAddresses) throws Exception{
		synchronized(lock){
			System.out.println("MockClient inMemoryChunksCallbackOnEnqueueChunkRequestToServer, saw cuboidAddresses=" + cuboidAddresses);
			for(CuboidAddress ca : cuboidAddresses){
				requestedNotYetReceivedChunks.add(ca); //  Request
				subscribedChunks.add(ca); // Subscribe
			}
		}
	}

	public void inMemoryChunksCallbackOnChunkBecomesAvailable(CuboidAddress ca) throws Exception{
		synchronized(lock){
			System.out.println("MockClient inMemoryChunksCallbackOnChunkBecomesAvailable, saw ca=" + ca);
			requestedNotYetReceivedChunks.remove(ca);
			decrementExpectedAvailableSignalCount(ca);
		}
	}

	public void onChunkBecameAvailable(Cuboid cuboid){
		synchronized(lock){
			incrementExpectedAvailableSignalCount(cuboid.getCuboidAddress());
		}
	}

	public Long getAuthorizedClientId() throws Exception{
		return 0L;
	}

	public CuboidAddress takeRequestedNotYetReceivedChunk(){
		synchronized(lock){
			if(requestedNotYetReceivedChunks.size() > 0){
				CuboidAddress rtn = new ArrayList<CuboidAddress>(requestedNotYetReceivedChunks).get(0);
				requestedNotYetReceivedChunks.remove(rtn);
				return rtn;
			}else{
				return null;
			}
		}
	}

	public Set<CuboidAddress> getExpectedPendingChunks(){
		synchronized(lock){
			return new TreeSet<CuboidAddress>(this.expectedPendingChunkSignals.keySet());
		}
	}

	public Set<CuboidAddress> getSubscribedChunks(){
		synchronized(lock){
			return new TreeSet<CuboidAddress>(this.subscribedChunks);
		}
	}

	public Set<CuboidAddress> getExpectedAvailableChunks(){
		synchronized(lock){
			return new TreeSet<CuboidAddress>(this.expectedAvailableChunkSignals.keySet());
		}
	}
}
