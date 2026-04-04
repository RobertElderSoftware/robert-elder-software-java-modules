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

public class MemoryChunkStateMachine extends StateMachine<CuboidAddress, ChunkMemoryState> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private InMemoryChunks inMemoryChunks;

	public static Set<ChunkMemoryState> getAllStatesSet() throws Exception{
		Set<ChunkMemoryState> rtn = new TreeSet<ChunkMemoryState>();
		for(Map.Entry<Long, MemoryChunkStateType> e : MemoryChunkStateType.memoryChunkStateTypesByValue.entrySet()){
			rtn.add(new ChunkMemoryState(e.getValue()));
		}
		return rtn;
	}

	public MemoryChunkStateMachine(InMemoryChunks inMemoryChunks) throws Exception{
		super(MemoryChunkStateMachine.getAllStatesSet());
		this.inMemoryChunks = inMemoryChunks;
	}

	public void requireChunk(CuboidAddress ca, InMemoryChunksClient clientThatRequired) throws Exception{
		ChunkMemoryState chunkState = this.getStateOfObject(ca);
		if(chunkState == null){
			//  Chunk was not in state machine; Transition the chunk to become pending:
			this.putObjectIntoState(ca, new ChunkMemoryState(MemoryChunkStateType.PENDING));
			this.inMemoryChunks.onMemoryChunkNewlyBecamePending(ca, clientThatRequired);
		}else{
			switch(chunkState.getMemoryChunkStateType()){
				case PENDING:{
					//  Chunk is already pending, do nothing.
					break;
				}case REQUESTED:{
					//  Chunk is requested, do nothing.
					break;
				}case AVAILABLE:{
					//  Chunk is already available, do nothing.
					break;
				}default:{
					throw new Exception("Cannot require chunk in state " + chunkState.getMemoryChunkStateType());
				}
			}
		}
	}

	public void makeChunkAvailable(CuboidAddress ca, InMemoryChunksClient client) throws Exception{
		ChunkMemoryState chunkState = this.getStateOfObject(ca);
		if(chunkState == null){
			throw new Exception("Trying to make chunk " + ca + " available, but it's not present in state machine.");
		}else{
			switch(chunkState.getMemoryChunkStateType()){
				case PENDING:{
					//  TODO: This is not supposed to be possible:
					this.putObjectIntoState(ca, new ChunkMemoryState(MemoryChunkStateType.AVAILABLE));
					break;
				}case REQUESTED:{
					this.putObjectIntoState(ca, new ChunkMemoryState(MemoryChunkStateType.AVAILABLE));
					break;
				}case AVAILABLE:{
					logger.info("Chunk " + ca + " was already available and was updated.");
					break;
				}default:{
					throw new Exception("Should not be possible.");
				}
			}
		}
	}

	public void requestChunk(CuboidAddress ca, InMemoryChunksClient clientThatRequested) throws Exception{
		ChunkMemoryState chunkState = this.getStateOfObject(ca);
		if(chunkState == null){
			throw new Exception("Should not happen.");
		}else{
			switch(chunkState.getMemoryChunkStateType()){
				case PENDING:{
					this.putObjectIntoState(ca, new ChunkMemoryState(MemoryChunkStateType.REQUESTED));
					this.inMemoryChunks.onMemoryChunkNewlyRequested(ca, clientThatRequested);
					break;
				}case REQUESTED:{
					throw new Exception("Should not happen.");
				}case AVAILABLE:{
					throw new Exception("Should not happen.");
				}default:{
					throw new Exception("Should not happen.");
				}
			}
		}
	}

	public void discardUnusedChunks(Set<CuboidAddress> usedChunks, InMemoryChunksClient clientThatDiscarded) throws Exception{
		Set<CuboidAddress> knownChunks = this.getObjectSet();
		knownChunks.removeAll(usedChunks); //  Modify set to only include chunks to discard.
		for(CuboidAddress chunkToDiscard : knownChunks){
			ChunkMemoryState chunkState = this.getStateOfObject(chunkToDiscard);
			this.inMemoryChunks.onMemoryChunkWasDiscardedFromState(chunkToDiscard, chunkState, clientThatDiscarded);
		}
		this.removeAllObjects(knownChunks);
	}

	public Set<CuboidAddress> getChunksInState(ChunkMemoryState state){
		return this.getObjectsInState(state);
	}
}
