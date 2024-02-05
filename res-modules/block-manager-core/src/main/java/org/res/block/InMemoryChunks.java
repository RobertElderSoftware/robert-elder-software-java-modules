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
import java.util.TreeSet;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class InMemoryChunks extends WorkItemQueueOwner<InMemoryChunksWorkItem> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected Object lock = new Object();

	private Coordinate playerPosition = null;

	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private BlockModelContext blockModelContext;

	/*  A description of how many blocks the 'chunk' should be on each side: */
	private CuboidAddress chunkSize = null;

	/*  A list of regions of space that we need to maintain in memory.  These regions are not necessarily aligned to chunk boundaries.  */
	private Set<CuboidAddress> lastRequiredRegions = new HashSet<CuboidAddress>();

	/*  A list of the required chunks that are needed to satisfy the cuboids described in 'requiredRegions' */
	private Set<CuboidAddress> lastRequiredChunks = new TreeSet<CuboidAddress>();

	/*  Chunks for which a request needs to be made to the remote server to load the block data. */
	private Set<CuboidAddress> pendingNotYetRequestedChunks = new TreeSet<CuboidAddress>();

	/*  Chunks for which for which an outstanding request to the server has been made, but a response has not been returned yet. */
	private Set<CuboidAddress> pendingAlreadyRequestedChunks = new TreeSet<CuboidAddress>();

	/*  The map of chunks that currently resides in memory. */
	private Map<CuboidAddress, IndividualBlock[]> blockChunks = new TreeMap<CuboidAddress, IndividualBlock[]>();

	public BlockModelContext getBlockModelContext(){
		return this.blockModelContext;
	}

	public void updateRequiredRegions(Set<CuboidAddress> requiredRegions) throws Exception{
		synchronized(lock){
			if(!this.lastRequiredRegions.equals(requiredRegions)){ /*  Only do this work if something has changed. */
				this.lastRequiredRegions = requiredRegions;

				/*  Calculate the entire set of required chunks that are needed at this moment: */
				Set<CuboidAddress> currentRequiredChunks = new TreeSet<CuboidAddress>();
				for(CuboidAddress ca : requiredRegions){
					currentRequiredChunks.addAll(ca.getIntersectingChunkSet(this.chunkSize));
				}

				//  Chunks that are required now that weren't required last time:
				Set<CuboidAddress> newlyRequiredChunks = new TreeSet<CuboidAddress>(currentRequiredChunks);
				newlyRequiredChunks.removeAll(this.lastRequiredChunks);
				//  Chunks that are not needed right now, but were needed last time:
				Set<CuboidAddress> newlyObsoleteChunks = new TreeSet<CuboidAddress>(this.lastRequiredChunks);
				newlyObsoleteChunks.removeAll(currentRequiredChunks);

				this.lastRequiredChunks = currentRequiredChunks;

				//  Set up a pending request for all chunks that need to be loaded:
				for(CuboidAddress c : newlyRequiredChunks){
					if(this.pendingNotYetRequestedChunks.contains(c)){
						//  Has not been requested yet.
					}else if(this.pendingAlreadyRequestedChunks.contains(c)){
						//  Still waiting for the chunk to come back.
					}else if(this.blockChunks.containsKey(c)){
						//  This chunk is already loaded.
					}else{
						blockModelContext.logMessage("Adding a blank entry for chunk " + c + ".  Will be reset with data that comes from server.");
						this.blockChunks.put(c, new IndividualBlock [(int)this.chunkSize.getVolume()]);
						this.pendingNotYetRequestedChunks.add(c);
						this.blockModelContext.inMemoryChunksCallbackOnChunkBecomesPending(c.copy());
					}
				}

				//  Remove old chunks that we don't need anymore:
				for(CuboidAddress c : newlyObsoleteChunks){
					if(this.pendingNotYetRequestedChunks.contains(c)){
						//  Just let the chunk get requested and then discard it when it comes back.
					}else if(this.pendingAlreadyRequestedChunks.contains(c)){
						//  Just let the chunk request come back and then discard it when it comes back.
					}else if(this.blockChunks.containsKey(c)){
						//  Remove the chunk to free up memory.
						this.blockChunks.remove(c);
					}else{
						throw new Exception("Obsolete chunk is not pending or found in blockChunk map: " + c + ".  This should not happen.");
					}
				}
			}
		}

		Set<CuboidAddress> pendingChunksToRequest = this.collectPendingChunksToRequest(this.playerPosition, 1L);

		if(pendingChunksToRequest.size() > 0){
			Set<Long> uniqueDimensions = new HashSet<Long>();
			for(CuboidAddress ca : pendingChunksToRequest){
				uniqueDimensions.add(ca.getNumDimensions());
			}

			if(uniqueDimensions.size() == 1){
				for(CuboidAddress cuboidAddress : pendingChunksToRequest){
					RequestChunkFromServerWorkItem m = new RequestChunkFromServerWorkItem((ClientBlockModelContext)this.getBlockModelContext(), cuboidAddress.copy());
					this.getBlockModelContext().putWorkItem(m, WorkItemPriority.PRIORITY_LOW);
					logger.info("Inside updateRequiredRegions just put a work item into block client for cuboidAddress=" + String.valueOf(cuboidAddress));
				}
			}else{
				throw new Exception("Trying to read blocks from cuboids with multiple different dimensions: " + uniqueDimensions);
			}
		}
	}

	public void onPlayerPositionChange(Coordinate newPosition) throws Exception{
		this.playerPosition = newPosition;
	}

	private List<CuboidAddress> getClosestCuboidAddressList(Set<CuboidAddress> cuboidAddresses, Coordinate currentCoordinate) throws Exception {

		if(currentCoordinate == null){
			currentCoordinate = new Coordinate(Arrays.asList(0L, 0L, 0L, 0L));
		}

		Map<Double, CuboidAddress> distanceSortedTreemap = new TreeMap<Double, CuboidAddress>();
		for(CuboidAddress ca : cuboidAddresses){
			Double distance = ca.getCentroidDistanceFromCoordinate(currentCoordinate);
			//blockModelContext.logMessage("Distance was " + distance + " for point " + currentCoordinate + " to cuboid " + ca + ".");
			distanceSortedTreemap.put(distance, ca);
		}

		List<CuboidAddress> sortedCuboidAddresses = new ArrayList<CuboidAddress>();
		for(Map.Entry<Double, CuboidAddress> e : distanceSortedTreemap.entrySet()){
			sortedCuboidAddresses.add(e.getValue());
		}

		return sortedCuboidAddresses;
	}

	public Set<CuboidAddress> collectPendingChunksToRequest(Coordinate currentCoordinate, Long maxPendingChunks) throws Exception {
		synchronized(lock){
			Long numChunksSelected = 0L;
			Set<CuboidAddress> chunksToRequest = new HashSet<CuboidAddress>();

			List<CuboidAddress> closestFirstCuboidAddressList = this.getClosestCuboidAddressList(this.pendingNotYetRequestedChunks, currentCoordinate);

			for(CuboidAddress chunkToRequest : closestFirstCuboidAddressList){
				if(this.pendingAlreadyRequestedChunks.contains(chunkToRequest)){
					throw new Exception("Chunk request already sent.  This is not supposed to happen.");
				}else{
					if(numChunksSelected < maxPendingChunks){
						numChunksSelected++;
						chunksToRequest.add(chunkToRequest);
					}else{
						break;
					}
				}
			}

			for(CuboidAddress chunk : chunksToRequest){
				this.pendingAlreadyRequestedChunks.add(chunk);
				this.pendingNotYetRequestedChunks.remove(chunk);
			}
			return chunksToRequest;
		}
	}

	public void handlePendingChunkWrite(Cuboid cuboid) throws Exception{
		synchronized(lock){
			CuboidAddress cuboidAddress = cuboid.getCuboidAddress();
			CuboidDataLengths dataLengths = cuboid.getCuboidDataLengths();
			CuboidData data = cuboid.getCuboidData();

			RegionIteration regionIteration = new RegionIteration(cuboidAddress.getCanonicalLowerCoordinate(), cuboidAddress);
			do{
				Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
				long blockOffsetInArray = cuboidAddress.getLinearArrayIndexForCoordinate(currentCoordinate);
				long sizeOfBlock = dataLengths.getLengths()[(int)blockOffsetInArray];
				long offsetOfBlock = dataLengths.getOffsets()[(int)blockOffsetInArray];
				IndividualBlock blockToWrite = null;
				if(sizeOfBlock < 0L){
					blockToWrite = new UninitializedBlock();
				}else{
					byte [] blockData = data.getDataAtOffset(offsetOfBlock, sizeOfBlock);
					String blockClassName = this.blockModelContext.getBlockSchema().getFirstBlockMatchDescriptionForByteArray(blockData);
					blockToWrite = IndividualBlock.makeBlockInstanceFromClassName(blockClassName, blockData);
				}

				//  Figure out which 'chunk' this coordinate belongs to
				CuboidAddress chunkCuboidAddress = CuboidAddress.blockCoordinateToChunkCuboidAddress(currentCoordinate, this.chunkSize);
				long blockOffsetInChunkArray = chunkCuboidAddress.getLinearArrayIndexForCoordinate(currentCoordinate);
				if(this.blockChunks.get(chunkCuboidAddress) == null){
					blockModelContext.logMessage("WARNING:  Discarding update to block at " + currentCoordinate + " in an unloaded region.  TODO: handle this better in the future.");
				}else{
					this.blockChunks.get(chunkCuboidAddress)[(int)blockOffsetInChunkArray] = blockToWrite;
				}
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());

			//  Acknowledge that the chunk has been received and processed:
			//  This could also be an update too:
			this.pendingAlreadyRequestedChunks.remove(cuboidAddress);
			this.blockModelContext.inMemoryChunksCallbackOnChunkWasWritten(cuboidAddress.copy());
		}
	}

	public IndividualBlock readBlockAtCoordinate(Coordinate coordinate) throws Exception{
		synchronized(lock){
			//  Figure out which 'chunk' this coordinate belongs to:
			CuboidAddress chunkCuboidAddress = CuboidAddress.blockCoordinateToChunkCuboidAddress(coordinate, this.chunkSize);
			long blockOffsetInArray = chunkCuboidAddress.getLinearArrayIndexForCoordinate(coordinate);
			IndividualBlock [] blocksInChunk = this.blockChunks.get(chunkCuboidAddress);
			if(blocksInChunk == null){ //  Chunk not loaded, and not even in a pending request to server.
				return null;
			}else{
				return blocksInChunk[(int)blockOffsetInArray];
			}
		}
	}

	public InMemoryChunks(BlockManagerThreadCollection blockManagerThreadCollection, BlockModelContext blockModelContext, CuboidAddress chunkSize) throws Exception{
		synchronized(lock){
			this.blockManagerThreadCollection = blockManagerThreadCollection;
			this.blockModelContext = blockModelContext;
			this.chunkSize = chunkSize;
			for(long i = 0; i < chunkSize.getNumDimensions(); i++){
				if(chunkSize.getWidthForIndex(i) < 1L){
					throw new Exception("Invalid chunk dimension size: " + chunkSize.getWidthForIndex(i));
				}
			}
		}
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public InMemoryChunksWorkItem takeWorkItem() throws Exception {
		InMemoryChunksWorkItem w = this.workItemQueue.takeWorkItem();
		return w;
	}

	public void putWorkItem(InMemoryChunksWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}
}
