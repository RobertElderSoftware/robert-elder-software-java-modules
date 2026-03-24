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

public class InMemoryChunksMockClient implements InMemoryChunksClient, Comparable<InMemoryChunksClient>{

	protected Object lock = new Object();

	private Set<CuboidAddress> pendingChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> requestedChunks = new TreeSet<CuboidAddress>();
	private Set<CuboidAddress> availableChunks = new TreeSet<CuboidAddress>();

	public InMemoryChunksMockClient(CuboidAddress chunkSize){

	}

	public Long getAuthorizedClientId() {
		return 0L;
	}

	@Override
	public int compareTo(InMemoryChunksClient other) {
		return this.getAuthorizedClientId().compareTo(other.getAuthorizedClientId());
	}

	public void assertDoesContain(Set<CuboidAddress> s, CuboidAddress c, String msg) throws Exception{
		if(!s.contains(c)){
			throw new Exception(msg + " does NOT contains " + c + " but it should!" + "\npending chunks=" + pendingChunks + "\navailable chunks=" + availableChunks + "\nrequested chunks=" + requestedChunks);
		}
	}

	public void assertDoesNotContain(Set<CuboidAddress> s, CuboidAddress c, String msg) throws Exception{
		if(s.contains(c)){
			throw new Exception(msg + " contains " + c + " but it should not!  " + "\npending chunks=" + pendingChunks + "\navailable chunks=" + availableChunks + "\nrequested chunks=" + requestedChunks);
		}
	}

	public void doStateTransition(MemoryChunkStateType signalType, CuboidAddress cuboidAddress) throws Exception{
		switch(signalType){
			case PENDING:{
				assertDoesNotContain(requestedChunks, cuboidAddress, "Requested Chunks");
				assertDoesNotContain(availableChunks, cuboidAddress, "Available Chunks");
				assertDoesNotContain(pendingChunks, cuboidAddress, "Pending Chunks");
				pendingChunks.add(cuboidAddress);
				System.out.println("Got signal " + signalType + ": Chunk " + cuboidAddress + " transitioned from nothing to state PENDING.");
				break;
			}case REQUESTED:{
				assertDoesContain(pendingChunks, cuboidAddress, "Pending Chunks");
				assertDoesNotContain(requestedChunks, cuboidAddress, "Requested Chunks");
				assertDoesNotContain(availableChunks, cuboidAddress, "Available Chunks");

				pendingChunks.remove(cuboidAddress);
				requestedChunks.add(cuboidAddress);
				System.out.println("Got signal " + signalType + ": Chunk " + cuboidAddress + " transitioned from PENDING to state REQUESTED.");
				break;
			}case DISCARDED:{
				if(pendingChunks.contains(cuboidAddress)){
					pendingChunks.remove(cuboidAddress);
					assertDoesNotContain(requestedChunks, cuboidAddress, "Requested Chunks");
					assertDoesNotContain(availableChunks, cuboidAddress, "Available Chunks");
					System.out.println("Got signal " + signalType + ": Chunk " + cuboidAddress + " transitioned from PENDING to state nothing.");
				}else if(availableChunks.contains(cuboidAddress)){
					availableChunks.remove(cuboidAddress);
					assertDoesNotContain(requestedChunks, cuboidAddress, "Requested Chunks");
					assertDoesNotContain(pendingChunks, cuboidAddress, "Pending Chunks");
					System.out.println("Got signal " + signalType + ": Chunk " + cuboidAddress + " transitioned from AVAILABLE to state nothing.");
				}else if(requestedChunks.contains(cuboidAddress)){
					assertDoesNotContain(availableChunks, cuboidAddress, "Available Chunks");
					assertDoesNotContain(pendingChunks, cuboidAddress, "Pending Chunks");
					System.out.println("Got signal " + signalType + ": Signal to dicard chunk " + cuboidAddress + ", but it's in requested state. Ignore.");
				}else{
					throw new Exception("Unexpected state transition.");
				}
				break;
			}case AVAILABLE:{
				assertDoesContain(requestedChunks, cuboidAddress, "Requested Chunks");
				assertDoesNotContain(pendingChunks, cuboidAddress, "Pending Chunks");
				assertDoesNotContain(availableChunks, cuboidAddress, "Available Chunks");

				requestedChunks.remove(cuboidAddress);
				availableChunks.add(cuboidAddress);
				System.out.println("Got signal " + signalType + ": Chunk " + cuboidAddress + " transitioned from REQUESTED to state AVAILABLE.");
				break;
			}default:{
				throw new Exception("Unknown signal type.");
			}
		}
	}

	public CuboidAddress giveRandomRequestedChunk() throws Exception{
		synchronized(lock){
			if(requestedChunks.size() > 0){
				List<CuboidAddress> l = new ArrayList<CuboidAddress>(requestedChunks);
				return l.get(0);
			}else{
				return null;
			}
		}
	}

	public void onChunkSignal(ChunkSignal signal) throws Exception{
		synchronized(lock){
			for(CuboidAddress ca : signal.getCuboidAddresses()){
				this.doStateTransition(signal.getMemoryChunkStateType(), ca);
			}
		}
	}
}
