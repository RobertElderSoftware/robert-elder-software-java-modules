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

public class InMemoryChunks extends UIEventReceiverThreadState<InMemoryChunksWorkItem> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected Object lock = new Object();

	private Map<InMemoryChunksClient, MemoryChunkStateMachine> chunkStateMachines = new TreeMap<InMemoryChunksClient, MemoryChunkStateMachine>();

	private Map<Long, PlayerPositionXYZ> playerPositions = new HashMap<Long, PlayerPositionXYZ>();

	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	/*  A description of how many blocks the 'chunk' should be on each side: */
	private CuboidAddress chunkSize = null;

	/*  A list of regions of space that we need to maintain in memory.  These regions are not necessarily aligned to chunk boundaries.  */
	private Set<CuboidAddress> lastRequiredRegions = new HashSet<CuboidAddress>();

	/*  The map of chunks that currently resides in memory. */
	private Map<CuboidAddress, IndividualBlock[]> blockChunks = new TreeMap<CuboidAddress, IndividualBlock[]>();

	/*  Max chunks we allow to be in pending state at once: */
	private Long maxPendingChunks;

	public void receiveEventNotification(UINotificationType notificationType, Object o, WorkItemPriority priority) throws Exception{
		this.putWorkItem(new InMemoryChunksWorkItemEventNotificationWorkItem(this, new EventNotificationWorkItem<InMemoryChunksWorkItem>(this, o, notificationType)), priority);

	}

	protected void init(Object o) throws Exception{
	}

	public boolean isEmptyAndFinished(){
		synchronized(lock){
			return (
				this.getAllChunksInState(MemoryChunkStateType.PENDING).size() == 0 &&
				this.getAllChunksInState(MemoryChunkStateType.REQUESTED).size() == 0 &&
				this.getWorkItemQueueSize() == 0
			);
		}
	}

	public void printSizes(){
		synchronized(lock){
			logger.info(
				"this.getAllChunksInState(MemoryChunkStateType.PENDING).size()=" + this.getAllChunksInState(MemoryChunkStateType.PENDING).size() +
				", this.getAllChunksInState(MemoryChunkStateType.REQUESTED).size()=" + this.getAllChunksInState(MemoryChunkStateType.REQUESTED).size() +
				", this.getWorkItemQueueSize()=" + this.getWorkItemQueueSize()
			);
		}
	}

	public void registerPlayer(ClientBlockModelContext clientBlockModelContext) throws Exception{
		UIModelProbeWorkItemResult result = (UIModelProbeWorkItemResult)clientBlockModelContext.putBlockingWorkItem(
			new UIModelProbeWorkItem(
				clientBlockModelContext,
				UINotificationType.PLAYER_POSITION,
				UINotificationSubscriptionType.SUBSCRIBE,
				this,
				BlockingType.NO_BLOCK
			),
			WorkItemPriority.PRIORITY_LOW
		);

		//  Set initial player position:
		if(result.getObject() != null){
			this.onPlayerPositionChange((AuthorizedPlayerPositionXYZ)result.getObject());
		}
	}

	public void sendSignal(InMemoryChunksClient inMemoryChunksClient, ChunkSignal signal) throws Exception{
		inMemoryChunksClient.onChunkSignal(signal);
	}

	public void destroy(Object o) throws Exception{

	}

	public boolean isChunkLoading(CuboidAddress c){
		return (
			!this.blockChunks.containsKey(c) &&
			(
				this.getAllChunksInState(MemoryChunkStateType.REQUESTED).size() > 0 ||
				this.getAllChunksInState(MemoryChunkStateType.PENDING).size() > 0
			)
		);
	}

	public boolean isChunkLoadedOrPending(InMemoryChunksClient inMemoryChunksClient, CuboidAddress c) throws Exception{
		synchronized(lock){
			if(
				this.isChunkLoading(c) ||
				this.blockChunks.containsKey(c)
			){
				return true;
			}else{
				return false;
			}
		}
	}

	public void onMemoryChunkNewlyBecamePending(CuboidAddress ca, InMemoryChunksClient clientThatRequired)throws Exception{
		this.sendSignal(clientThatRequired, new ChunkSignal(MemoryChunkStateType.PENDING, Arrays.asList(ca.copy())));
	}

	public void onMemoryChunkNewlyRequested(CuboidAddress ca, InMemoryChunksClient clientThatRequired)throws Exception{
		this.sendSignal(clientThatRequired, new ChunkSignal(MemoryChunkStateType.REQUESTED, Arrays.asList(ca.copy())));
	}

	public void onMemoryChunkWasDiscardedFromState(CuboidAddress ca, ChunkMemoryState fromState, InMemoryChunksClient clientThatDiscarded) throws Exception{
		switch(fromState.getMemoryChunkStateType()){
			case PENDING:{
				//  Don't bother even requesting it if we're just going to dicard it anyway:
				this.sendSignal(clientThatDiscarded, new ChunkSignal(null, Arrays.asList(ca)));
				break;
			}case REQUESTED:{
				//  Discard:
				this.sendSignal(clientThatDiscarded, new ChunkSignal(null, Arrays.asList(ca)));
				break;
			}case AVAILABLE:{
				//  Remove the chunk to free up memory.
				this.blockChunks.remove(ca);
				this.sendSignal(clientThatDiscarded, new ChunkSignal(null, Arrays.asList(ca)));
				break;
			}default:{
				throw new Exception("Unknown state in discard chunk.");
			}
		}
	}

	public void setupClientStateMachine(InMemoryChunksClient inMemoryChunksClient) throws Exception{
		if(!this.chunkStateMachines.containsKey(inMemoryChunksClient)){
			this.chunkStateMachines.put(inMemoryChunksClient, new MemoryChunkStateMachine(this));
		}
	}

	public void updateRequiredRegions(Set<CuboidAddress> requiredRegions, InMemoryChunksClient inMemoryChunksClient) throws Exception{
		synchronized(lock){
			if(!this.lastRequiredRegions.equals(requiredRegions)){ /*  Only do this work if something has changed. */
				this.lastRequiredRegions = requiredRegions;
				this.setupClientStateMachine(inMemoryChunksClient);

				/*  Calculate the entire set of required chunks that are needed at this moment: */
				Set<CuboidAddress> currentRequiredChunks = new TreeSet<CuboidAddress>();
				for(CuboidAddress ca : requiredRegions){
					Set<CuboidAddress> requiredIntersectingChunks = ca.getIntersectingChunkSet(this.chunkSize);
					currentRequiredChunks.addAll(requiredIntersectingChunks);
					for(CuboidAddress requiredCA : requiredIntersectingChunks){
						this.chunkStateMachines.get(inMemoryChunksClient).requireChunk(requiredCA, inMemoryChunksClient);
					}
				}
				this.chunkStateMachines.get(inMemoryChunksClient).discardUnusedChunks(currentRequiredChunks, inMemoryChunksClient);
			}else{
				logger.info("Required regions has not changed.  Do nothing.");
			}
		}
	}

	public List<Map.Entry<InMemoryChunksClient, CuboidAddress>> getAllChunksInState(MemoryChunkStateType stateType){
		ChunkMemoryState state = new ChunkMemoryState(stateType);
		List<Map.Entry<InMemoryChunksClient, CuboidAddress>> rtn = new ArrayList<Map.Entry<InMemoryChunksClient, CuboidAddress>>();
		for(Map.Entry<InMemoryChunksClient, MemoryChunkStateMachine> e : chunkStateMachines.entrySet()){
			MemoryChunkStateMachine sm = e.getValue();
			Set<CuboidAddress> chunksInState = sm.getChunksInState(state);
			for(CuboidAddress ca : chunksInState){
				rtn.add(Map.entry(e.getKey(), ca));
			}
		}
		return rtn;
	}

	public boolean onHasPendingNotYetRequestedChunks() throws Exception{
		synchronized(lock){
			//  Only start a request for more chunks if we've not already exceeded the max outstanding chunks.
			if(this.getAllChunksInState(MemoryChunkStateType.REQUESTED).size() < maxPendingChunks){
				List<Map.Entry<InMemoryChunksClient, CuboidAddress>> closestFirstCuboidAddressList = this.getClosestCuboidAddressList(this.getAllChunksInState(MemoryChunkStateType.PENDING), this.playerPositions);

				Long numChunksSelected = 0L;
				List<Map.Entry<InMemoryChunksClient, CuboidAddress>> pendingChunksToRequest = new ArrayList<Map.Entry<InMemoryChunksClient, CuboidAddress>>();
				for(Map.Entry<InMemoryChunksClient, CuboidAddress> clientChunkToRequest : closestFirstCuboidAddressList){
					if(numChunksSelected < maxPendingChunks){
						numChunksSelected++;
						pendingChunksToRequest.add(clientChunkToRequest);
					}else{
						break;
					}
				}

				if(pendingChunksToRequest.size() > 0){
					Set<Long> uniqueDimensions = new HashSet<Long>();
					for(Map.Entry<InMemoryChunksClient, CuboidAddress> clientChunkToRequest : pendingChunksToRequest){
						uniqueDimensions.add(clientChunkToRequest.getValue().getNumDimensions());
					}

					if(uniqueDimensions.size() == 1){
						for(Map.Entry<InMemoryChunksClient, CuboidAddress> clientChunkToRequest : pendingChunksToRequest){
							logger.info(clientChunkToRequest.getValue() + " is transitioning from pendingNotYetRequestedChunks to pendingAlreadyRequestedChunks.");
							this.chunkStateMachines.get(clientChunkToRequest.getKey()).requestChunk(clientChunkToRequest.getValue(), clientChunkToRequest.getKey());

							logger.info("Inside updateRequiredRegions just put a work item into block client for cuboidAddress=" + String.valueOf(clientChunkToRequest.getValue()));
						}
					}else{
						throw new Exception("Trying to read blocks from cuboids with multiple different dimensions: " + uniqueDimensions);
					}
					return true;
				}else{
					logger.info("There were no pending chunks to request.");
					return false;
				}
			}else{
				logger.info("Did not request any more chunks because: this < maxPendingChunks=" + maxPendingChunks);
				return false;
			}
		}
	}

	public void onPlayerPositionChange(AuthorizedPlayerPositionXYZ newPosition) throws Exception{
		this.playerPositions.put(newPosition.getAuthorizedClientId(), newPosition.getPlayerPositionXYZ());
	}

	private List<Map.Entry<InMemoryChunksClient, CuboidAddress>> getClosestCuboidAddressList(List<Map.Entry<InMemoryChunksClient, CuboidAddress>> cuboidAddresses, Map<Long, PlayerPositionXYZ> playerPositions) throws Exception {

		Map<Double, Map.Entry<InMemoryChunksClient, CuboidAddress>> distanceSortedTreemap = new TreeMap<Double, Map.Entry<InMemoryChunksClient, CuboidAddress>>();
		for(Map.Entry<InMemoryChunksClient, CuboidAddress> oneClientChunk : cuboidAddresses){
			List<Double> distancesFromPlayers = new ArrayList<Double>();
			for(Map.Entry<Long, PlayerPositionXYZ> e : playerPositions.entrySet()){
				PlayerPositionXYZ player = e.getValue();
				Double distance = oneClientChunk.getValue().getCentroidDistanceFromCoordinate(player.getPosition());
				distancesFromPlayers.add(distance);
			}
			Double minDistanceValue = distancesFromPlayers.size() == 0 ? 0.0 : Collections.min(distancesFromPlayers);
			distanceSortedTreemap.put(minDistanceValue, oneClientChunk);
		}

		List<Map.Entry<InMemoryChunksClient, CuboidAddress>> sortedCuboidAddresses = new ArrayList<Map.Entry<InMemoryChunksClient, CuboidAddress>>();
		for(Map.Entry<Double, Map.Entry<InMemoryChunksClient, CuboidAddress>> e : distanceSortedTreemap.entrySet()){
			sortedCuboidAddresses.add(e.getValue());
		}

		return sortedCuboidAddresses;
	}

	public void addInMemoryChunk(Cuboid cuboid, InMemoryChunksClient inMemoryChunksClient) throws Exception{
		synchronized(lock){
			CuboidAddress cuboidAddress = cuboid.getCuboidAddress();
			CuboidDataLengths dataLengths = cuboid.getCuboidDataLengths();
			CuboidData data = cuboid.getCuboidData();
			if(cuboidAddress == null){
				throw new Exception("cuboidAddress null");
			}
			if(inMemoryChunksClient == null){
				throw new Exception("inMemoryChunksClient null");
			}
			if(this.chunkStateMachines.get(inMemoryChunksClient) == null){
				throw new Exception("this.chunkStateMachines.get(inMemoryChunksClient) null");
			}
			ChunkMemoryState chunkState = this.chunkStateMachines.get(inMemoryChunksClient).getStateOfObject(cuboidAddress);

			if(chunkState == null){
				//  This was probably odd-sized non-chunk aligned update. TODO:  Separate these code paths.
			}else{
				//  If it was a pending request, acknowledge that the chunk has been received and processed:
				this.chunkStateMachines.get(inMemoryChunksClient).makeChunkAvailable(cuboidAddress, inMemoryChunksClient);
				this.blockChunks.put(cuboidAddress, new IndividualBlock [(int)this.chunkSize.getVolume()]);
			}

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
					String blockClassName = blockManagerThreadCollection.getBlockSchema().getFirstBlockMatchDescriptionForByteArray(blockData);
					blockToWrite = IndividualBlock.makeBlockInstanceFromClassName(blockClassName, blockData);
				}

				//  Figure out which 'chunk' this coordinate belongs to
				CuboidAddress chunkCuboidAddress = CuboidAddress.blockCoordinateToChunkCuboidAddress(currentCoordinate, this.chunkSize);
				long blockOffsetInChunkArray = chunkCuboidAddress.getLinearArrayIndexForCoordinate(currentCoordinate);

				if(this.blockChunks.get(chunkCuboidAddress) == null){
					//  This case will naturally happen if we unsubscribed just as we're about to get an update from the server about a write that took place before the unsubscribe:
					logger.info("Note:  Discarding update to block at " + currentCoordinate + " (cuboidAddress=" + chunkCuboidAddress + ") in an unloaded region.");
				}else{
					this.blockChunks.get(chunkCuboidAddress)[(int)blockOffsetInChunkArray] = blockToWrite;
				}
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());
			this.sendSignal(inMemoryChunksClient, new ChunkSignal(MemoryChunkStateType.AVAILABLE, Arrays.asList(cuboidAddress.copy())));
		}
	}

	private IndividualBlock readBlockAtCoordinate_Internal(Coordinate coordinate) throws Exception{
		//  Figure out which 'chunk' this coordinate belongs to:
		CuboidAddress chunkCuboidAddress = CuboidAddress.blockCoordinateToChunkCuboidAddress(coordinate, this.chunkSize);
		long blockOffsetInArray = chunkCuboidAddress.getLinearArrayIndexForCoordinate(coordinate);
		if(this.isChunkLoading(chunkCuboidAddress)){
			return null; //  Still waiting on chunk to come back from server.
		}else{
			IndividualBlock [] blocksInChunk = this.blockChunks.get(chunkCuboidAddress);
			if(blocksInChunk == null){ //  Chunk not loaded at all, and not even in a pending request to server.
				return null;
			}else{
				IndividualBlock b = blocksInChunk[(int)blockOffsetInArray];
				if(b instanceof UninitializedBlock && !coordinate.getValueAtIndex(3L).equals(0L)){
					// Special case for Uninitialized block
					// outside the map plane:
					return new EmptyBlock(new byte []{});
				}else{
					return b;
				}
			}
		}
	}

	public IndividualBlock readBlockAtCoordinate(Coordinate coordinate) throws Exception{
		synchronized(lock){
			return readBlockAtCoordinate_Internal(coordinate);
		}
	}

	public void loadBlocksFromMemory(ThreeDimensionalCircularBuffer<IndividualBlock> blockBuffer, CuboidAddress areaToInclude, CuboidAddress areaToExclude) throws Exception {
		synchronized(lock){
			RegionIteration regionIteration = new RegionIteration(areaToInclude.getCanonicalLowerCoordinate(), areaToInclude);
			while(!regionIteration.isDone()){
				Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
				if(areaToExclude == null || !areaToExclude.containsCoordinate(currentCoordinate)){
					IndividualBlock b = this.readBlockAtCoordinate_Internal(currentCoordinate);
					if(b != null){ /* Chunk not even loaded. */
						blockBuffer.setObjectAtCoordinate(currentCoordinate, b);
					}
				}
				regionIteration.incrementCoordinateWithinCuboidAddress();
			}
		}
	}

	public InMemoryChunks(BlockManagerThreadCollection blockManagerThreadCollection, CuboidAddress chunkSize, Long maxPendingChunks) throws Exception{
		synchronized(lock){
			this.blockManagerThreadCollection = blockManagerThreadCollection;
			this.chunkSize = chunkSize;
			this.maxPendingChunks = maxPendingChunks;
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
		if(this.getAllChunksInState(MemoryChunkStateType.PENDING).size() > 0){
			if(onHasPendingNotYetRequestedChunks()){
				return true; // More background work to do.
			}
		}

		return false;
	}

	public void onUIEventNotification(Object o, UINotificationType notificationType) throws Exception{
		switch(notificationType){
			case PLAYER_POSITION:{
				this.onPlayerPositionChange((AuthorizedPlayerPositionXYZ)o);
				break;
			}default:{
				throw new Exception("Unknown event notification type: " + notificationType);
			}
		}
	}
}
