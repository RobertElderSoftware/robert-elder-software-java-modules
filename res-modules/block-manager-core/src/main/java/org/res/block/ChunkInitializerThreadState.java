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

public class ChunkInitializerThreadState extends WorkItemQueueOwner<ChunkInitializerWorkItem> {

	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private MultiDimensionalNoiseGenerator noiseGenerator = new MultiDimensionalNoiseGenerator(0L);
	private Coordinate playerPosition = null;
	private CuboidAddress reachableGameArea = null;
	private Map<CuboidAddress, Cuboid> cuboidsToInitialize = new HashMap<CuboidAddress, Cuboid>();
	private ClientBlockModelContext clientBlockModelContext;
	private InMemoryChunks inMemoryChunks;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public ChunkInitializerThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, InMemoryChunks inMemoryChunks) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		this.inMemoryChunks = inMemoryChunks;
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

	public void onPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		this.playerPosition = newPosition;
	}

	public void onGameAreaChange(CuboidAddress reachableGameArea) throws Exception{
		this.reachableGameArea = reachableGameArea;
	}

	public void onNewCuboidToInitialize(Cuboid cuboid) throws Exception{
		cuboidsToInitialize.put(cuboid.getCuboidAddress(), cuboid);
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

		// TODO:  Don't use sleep to slow down this thread, use blocking and rate limiting instead:
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

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
			clientBlockModelContext.submitChunkToServer(newGeneratedCuboid, WorkItemPriority.PRIORITY_LOW);
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
			if(noiseAtPixel <= 0.75){
				return Rock.blockDataString.getBytes("UTF-8");
			}else if(noiseAtPixel <= 0.80){
				return TitaniumDioxide.blockDataString.getBytes("UTF-8");
			}else if(noiseAtPixel <= 0.85){
				return Bauxite.blockDataString.getBytes("UTF-8");
			}else if(noiseAtPixel <= 0.90){
				return Pyrite.blockDataString.getBytes("UTF-8");
			}else if(noiseAtPixel <= 0.95){
				return Ilmenite.blockDataString.getBytes("UTF-8");
			}else if(noiseAtPixel <= 0.96){
				return MetallicCopper.blockDataString.getBytes("UTF-8");
			}else if(noiseAtPixel <= 1.0){
				return IronOxide.blockDataString.getBytes("UTF-8");
			}else{
				return IronPick.blockDataString.getBytes("UTF-8");
			}
		/*  Ground Level */
		}else if(c.getY().equals(0L)){
			if(smallWaveNoise <= -0.6){
				return WoodenBlock.blockDataString.getBytes("UTF-8");
			}else if(smallWaveNoise <= 0.5){
				return "".getBytes("UTF-8");
			}else if(smallWaveNoise <= 0.90){
				return Rock.blockDataString.getBytes("UTF-8");
			}else if(smallWaveNoise <= 1.0){
				return IronOxide.blockDataString.getBytes("UTF-8");
			}else{
				return "".getBytes("UTF-8");
			}
		/*  Above Ground */
		}else{
			if(smallWaveNoise <= -0.7){
				return WoodenBlock.blockDataString.getBytes("UTF-8");
			}else if(smallWaveNoise <= 1.0){
				return "".getBytes("UTF-8");
			}else{
				return "".getBytes("UTF-8");
			}
		}
	}

	public boolean doBackgroundProcessing() throws Exception{
		List<CuboidAddress> closestCuboidAddressList = this.getClosestCuboidAddressList(cuboidsToInitialize.keySet(), this.playerPosition);
		if(closestCuboidAddressList.size() > 0){
			CuboidAddress cuboidAddressToInitialize = closestCuboidAddressList.get(0);
			//  Don't bother initializing chunks that are not visible in the game area, and also not loaded/loading in memory:
			//  This could still discard a few chunks from the initialization queue if they are outside the visible game area, but in the
			//  initially loaded area before inMemoryChunks has had a chance to issue the pending request.  You can just move around to correctly load the area though.
			if(
				(!this.inMemoryChunks.isChunkLoadedOrPending(cuboidAddressToInitialize)) &&
				this.reachableGameArea != null && this.reachableGameArea.getIntersectionCuboidAddress(cuboidAddressToInitialize) == null
			){
				//  If the area we're trying to initialize is off screen, then just ignore this chunk and move on.
				logger.info("Chunk initializer thread discarding cuboid " + cuboidAddressToInitialize + " because it's not in a loaded memory area anymore.");
			}else{
				logger.info("Initializing this cuboid: " + cuboidAddressToInitialize + " because it was in the loaded memory area.");
				this.initializeOneCuboid(cuboidsToInitialize.get(cuboidAddressToInitialize));
			}
			cuboidsToInitialize.remove(cuboidAddressToInitialize);
			//  There is only more work if there are still cuboids to initialize
			//  Also, allow prioritization of new work items:
			return cuboidsToInitialize.size() > 0 && this.workItemQueue.size() == 0;
		}else{
			return false;
		}
	}
}
