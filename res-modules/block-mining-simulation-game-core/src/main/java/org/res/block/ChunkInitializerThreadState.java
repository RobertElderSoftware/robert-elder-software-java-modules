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
import java.util.Random;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;

import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.security.MessageDigest;

public class ChunkInitializerThreadState extends UIEventReceiverThreadState<ChunkInitializerWorkItem> {

	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private Set<Long> outstandingChunkWriteConversations = new HashSet<Long>();
	private MultiDimensionalNoiseGenerator noiseGenerator = new MultiDimensionalNoiseGenerator(0L, MessageDigest.getInstance("SHA-512"));
	private Coordinate playerPosition = null;
	private CuboidAddress reachableMapArea = null;
	private Map<CuboidAddress, Cuboid> cuboidsToInitialize = new HashMap<CuboidAddress, Cuboid>();
	private ClientBlockModelContext clientBlockModelContext;
	private InMemoryChunks inMemoryChunks;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public void receiveEventNotification(UINotificationType notificationType, Object o, WorkItemPriority priority) throws Exception{
		this.putWorkItem(new ChunkInitializerWorkItemEventNotificationWorkItem(this, new EventNotificationWorkItem<ChunkInitializerWorkItem>(this, o, notificationType)), priority);
	}

	private static final Object [] interestingBlocks = new Object [] {
		Kaolin.class,
		CalcinedAnthracite.class,
		MetallicTitanium.class,
		SiliconDioxide.class,
		Chrysoberyl.class,
		MetallicSilver.class,
		TitaniumDioxide.class,
		Bauxite.class,
		Malachite.class,
		Pyrite.class,
		Ilmenite.class,
		Limonite.class,
		Wuestite.class,
		Siderite.class,
		Magnetite.class,
		Goethite.class,
		Taconite.class,
		MetallicCopper.class,
		IronOxide.class,
		Hematite.class
	};

	public ChunkInitializerThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, InMemoryChunks inMemoryChunks) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		this.inMemoryChunks = inMemoryChunks;
	}

	protected void init(Object o) throws Exception{
		UIModelProbeWorkItemResult result = (UIModelProbeWorkItemResult)this.clientBlockModelContext.putBlockingWorkItem(
			new UIModelProbeWorkItem(
				this.clientBlockModelContext,
				UINotificationType.PLAYER_POSITION,
				UINotificationSubscriptionType.SUBSCRIBE,
				this
			),
			WorkItemPriority.PRIORITY_LOW
		);

		//  Set initial player position:
		this.onPlayerPositionChange((PlayerPositionXYZ)result.getObject());
	}

	public void destroy(Object o) throws Exception{

	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public ChunkInitializerWorkItem takeWorkItem() throws Exception {
		ChunkInitializerWorkItem w = this.workItemQueue.takeWorkItem();
		return w;
	}

	public void putWorkItem(ChunkInitializerWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public void onPlayerPositionChange(PlayerPositionXYZ newPosition) throws Exception{
		this.playerPosition = newPosition == null ? null : newPosition.getPosition();
	}

	public void onMapAreaChange(CuboidAddress reachableMapArea) throws Exception{
		this.reachableMapArea = reachableMapArea;
	}

	public void onNewCuboidToInitialize(Cuboid cuboid) throws Exception{
		cuboidsToInitialize.put(cuboid.getCuboidAddress(), cuboid);
		this.doChunkInitializationActivity();
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

	public void initializeOneCuboid(Cuboid cuboid) throws Exception{
		CuboidAddress cuboidAddress = cuboid.getCuboidAddress();
		long cuboidVolumeInBlocks = cuboidAddress.getVolume();
		BlockMessageBinaryBuffer newDataForOneCuboid = new BlockMessageBinaryBuffer();
		long [] newDataLengths = new long [(int)cuboidVolumeInBlocks];
		for(int j = 0; j < cuboidVolumeInBlocks; j++){
			newDataLengths[j] = -1L; /*  Default to block not present. */
		}

		CuboidDataLengths oldDataLengths = cuboid.getCuboidDataLengths();
		CuboidData oldData = cuboid.getCuboidData();
		Long numBlocksThatWereInitialized = 0L;

		//this.logMessage("In initializeUninitializedBlocksOnServer for cuboidAddress=" + cuboidAddress);
		RegionIteration regionIteration = new RegionIteration(cuboidAddress.getCanonicalLowerCoordinate(), cuboidAddress);
		do{
			Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
			long blockOffsetInArray = cuboidAddress.getLinearArrayIndexForCoordinate(currentCoordinate);
			long sizeOfBlock = oldDataLengths.getLengths()[(int)blockOffsetInArray];
			long offsetOfBlock = oldDataLengths.getOffsets()[(int)blockOffsetInArray];
			IndividualBlock blockWritten = null;
			if(sizeOfBlock < 0L){
				//  This block is uninitialized, initialize it.
				byte [] blockData = getBlockDataAtCoordinate(currentCoordinate);
				if(blockData == null){
					newDataLengths[(int)blockOffsetInArray] = -1; // Keep it uninitialized.
				}else{
					newDataLengths[(int)blockOffsetInArray] = blockData.length;
					newDataForOneCuboid.writeBytes(blockData);
					numBlocksThatWereInitialized++;
				}
			}else{
				//  Block already initialized.
				newDataLengths[(int)blockOffsetInArray] = sizeOfBlock;
				newDataForOneCuboid.writeBytes(oldData.getDataAtOffset(offsetOfBlock, sizeOfBlock));
			}
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());

		CuboidDataLengths newCuboidDataLengths = new CuboidDataLengths(cuboidAddress, newDataLengths);
		CuboidData newCuboidData = new CuboidData(newDataForOneCuboid.getUsedBuffer());
		Cuboid newGeneratedCuboid = new Cuboid(cuboidAddress, newCuboidDataLengths, newCuboidData);

		if(numBlocksThatWereInitialized > 0L){ //  Only send the update back when there is actually something to initialize (otherwise, we get stuck in a loop)
			Long conversationId = this.getUnusedOustandingChunkWriteConversationId();
			clientBlockModelContext.submitChunkToServer(cuboidAddress.getNumDimensions(), Arrays.asList(newGeneratedCuboid), WorkItemPriority.PRIORITY_LOW, conversationId);
			outstandingChunkWriteConversations.add(conversationId);
		}
	}

	public byte [] getBlockDataAtCoordinate(Coordinate c) throws Exception{
		//  Higher dimensional data like player inventory, player position etc.
		if(!c.getValueAtIndex(3L).equals(0L)){
			return null;
		}

		long [] coordinate = new long [3];
		coordinate[0] = c.getX();
		coordinate[1] = c.getY();
		coordinate[2] = c.getZ();
		double smallWaveNoise = noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, new double [] {0.08}, new double [] {Math.pow(0.08, 1.2)}) * 100;
		double largeWaveNoise = (noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, new double [] {0.01}, new double [] {Math.pow(0.01, 1.2)}) * 10000) - 3.0;
		double caveNoise = (noiseGenerator.multiOctaveNoiseAtCoordinate(coordinate, new double [] {0.005}, new double [] {Math.pow(0.005, 1.2)}) * 10000);
		double positiveLargeWaveNoise = largeWaveNoise < 0.0 ? 0.0 : largeWaveNoise; //  The large wave noise is used to 'switch on/off' the smaller wave noise.
		double noiseAtPixel = smallWaveNoise * positiveLargeWaveNoise;
		noiseAtPixel = noiseAtPixel > 1.0 ? 1.0 : noiseAtPixel;
		noiseAtPixel = noiseAtPixel < -1.0 ? -1.0 : noiseAtPixel;

		/*  Blank sphere around origin: */
		if(((c.getX() * c.getX()) + (c.getY() * c.getY()) + (c.getZ() * c.getZ())) < (8L * 8L * 8L)){
			return "".getBytes("UTF-8");
		}

		/*  Caves */
		if(caveNoise >= -0.2 && caveNoise <= 0.2){
			return "".getBytes("UTF-8");
		}

		/*  Below Ground */
		if(c.getY() < 0L){
			if(noiseAtPixel <= 0.60){
				return clientBlockModelContext.getBlockDataForClass(Rock.class);
			}else if(noiseAtPixel <= 1.0){
				//  Fill up this range of gradient with whatever interesting blocks are currently in the game:
				double maxDifference = 1.0 - 0.60;
				double observedDifference = noiseAtPixel - 0.60;
				double fraction = observedDifference / maxDifference;
				int index = Math.min(interestingBlocks.length -1, (int)(fraction * interestingBlocks.length));
				return clientBlockModelContext.getBlockDataForClass((Class)interestingBlocks[index]);
			}else{
				return clientBlockModelContext.getBlockDataForClass(IronPick.class);
			}
		/*  Ground Level */
		}else if(c.getY().equals(0L)){
			if(smallWaveNoise <= -0.6){
				return clientBlockModelContext.getBlockDataForClass(WoodenBlock.class);
			}else if(smallWaveNoise <= 0.5){
				return "".getBytes("UTF-8");
			}else if(smallWaveNoise <= 0.90){
				return clientBlockModelContext.getBlockDataForClass(Rock.class);
			}else if(smallWaveNoise <= 1.0){
				return clientBlockModelContext.getBlockDataForClass(IronOxide.class);
			}else{
				return "".getBytes("UTF-8");
			}
		/*  Above Ground */
		}else{
			if(smallWaveNoise <= -0.7){
				return clientBlockModelContext.getBlockDataForClass(WoodenBlock.class);
			}else if(smallWaveNoise <= 1.0){
				return "".getBytes("UTF-8");
			}else{
				return "".getBytes("UTF-8");
			}
		}
	}
	public Long getUnusedOustandingChunkWriteConversationId(){
		while (true){
			Long l = new Random().nextLong();
			if(!outstandingChunkWriteConversations.contains(l)){
				return l;
			}
		}
	}

	public void doChunkInitializationActivity() throws Exception{
		Long maxOutstandingInitializingChunkWrites = 2L;
		while(cuboidsToInitialize.size() > 0 && outstandingChunkWriteConversations.size() < maxOutstandingInitializingChunkWrites){
			List<CuboidAddress> closestCuboidAddressList = this.getClosestCuboidAddressList(cuboidsToInitialize.keySet(), this.playerPosition);
			if(closestCuboidAddressList.size() > 0){
				CuboidAddress cuboidAddressToInitialize = closestCuboidAddressList.get(0);
				//  Don't bother initializing chunks that are not visible in the game area, and also not loaded/loading in memory:
				//  This could still discard a few chunks from the initialization queue if they are outside the visible game area, but in the
				//  initially loaded area before inMemoryChunks has had a chance to issue the pending request.  You can just move around to correctly load the area though.
				if(
					(!this.inMemoryChunks.isChunkLoadedOrPending(cuboidAddressToInitialize)) &&
					this.reachableMapArea != null && this.reachableMapArea.getIntersectionCuboidAddress(cuboidAddressToInitialize) == null
				){
					//  If the area we're trying to initialize is off screen, then just ignore this chunk and move on.
					logger.info("Chunk initializer thread discarding cuboid " + cuboidAddressToInitialize + " because it's not in a loaded memory area anymore.");
				}else{
					logger.info("Initializing this cuboid: " + cuboidAddressToInitialize + " because it was in the loaded memory area.");
					this.initializeOneCuboid(cuboidsToInitialize.get(cuboidAddressToInitialize));
				}
				cuboidsToInitialize.remove(cuboidAddressToInitialize);
			}else{
				logger.info("Chunk initializer thread: no more cuboids left to initialize.");
			}
		}
	}

	public void onNewAcknowledgement(Long conversationId) throws Exception{
		if(outstandingChunkWriteConversations.contains(conversationId)){
			outstandingChunkWriteConversations.remove(conversationId);  // Server acknowledged this chunk write
			this.doChunkInitializationActivity();
		}else{
			logger.info("Chunk initializer thread: conversationId=" + conversationId + " was not part of any initialied block write.  Most likely an unrelated write.");
		}
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}

	public void onUIEventNotification(Object o, UINotificationType notificationType) throws Exception{
		switch(notificationType){
			case PLAYER_POSITION:{
				this.onPlayerPositionChange((PlayerPositionXYZ)o);
				break;
			}default:{
				throw new Exception("Unknown event notification type: " + notificationType);
			}
		}
	}
}
