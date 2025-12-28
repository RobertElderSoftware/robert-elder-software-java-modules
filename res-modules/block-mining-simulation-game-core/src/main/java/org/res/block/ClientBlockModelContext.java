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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientBlockModelContext extends BlockModelContext implements BlockModelInterface {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private InMemoryChunks inMemoryChunks;
	private ChunkInitializerThreadState chunkInitializerThreadState;
	private Coordinate rootBlockDictionaryAddress = null;
	private BlockDictionary rootBlockDictionary = null;

	/*  This defines the dimensions of the 'chunks' that are loaded into memory as we move around */
	private final CuboidAddress chunkSizeCuboidAddress = new CuboidAddress(new Coordinate(Arrays.asList(0L, 0L, 0L, 0L)), new Coordinate(Arrays.asList(3L, 3L, 5L, 1L)));
	private PlayerPositionXYZ playerPositionXYZ = null;
	private PlayerInventory playerInventory = new PlayerInventory();
	private PlayerObject playerObject = null;

	private CuboidAddress mapAreaCuboidAddress;
	private Integer selectedInventoryItemIndex = null;
	private Integer selectedCraftingRecipeIndex = 0;
	private Map<UINotificationType, Set<UIEventReceiverThreadState<?>>> uiEventSubscriptions = new HashMap<UINotificationType, Set<UIEventReceiverThreadState<?>>>();
	private Long authorizedClientId = 0L; //  TODO:  change this for each player

	public ClientBlockModelContext(BlockManagerThreadCollection blockManagerThreadCollection, ClientServerInterface clientServerInterface) throws Exception {
		super(blockManagerThreadCollection, clientServerInterface);
	}

	public Class<?> getSelectedInventoryItemClass() throws Exception{
		if(playerInventory == null){
			return null;
		}else{
			if(playerInventory.getInventoryItemStackList().size() == 0 || this.selectedInventoryItemIndex == null){
				return null;
			}else{
				PlayerInventoryItemStack currentStack = playerInventory.getInventoryItemStackList().get(this.selectedInventoryItemIndex);
				IndividualBlock blockFromStack = currentStack.getBlock(this.blockManagerThreadCollection.getBlockSchema());
				return blockFromStack.getClass();
			}
		}
	}

	public void init(Object o) throws Exception{
		//  This is important and only used by multi-player client.  TODO:  Figure out how to simplify this:
		this.clientServerInterface.setClientBlockModelContext(this);

		//TODO: Actually assign this and delete it from client server interface:
		//this.serverBlockModelContext = (ServerBlockModelContext)o;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();

		this.logMessage("Ran init of ClientBlockModelContext.");

		this.inMemoryChunks = new InMemoryChunks(blockManagerThreadCollection, this, chunkSizeCuboidAddress);
		this.inMemoryChunks.putWorkItem(new InitializeYourselfInMemoryChunksWorkItem(this.inMemoryChunks), WorkItemPriority.PRIORITY_LOW);
		this.chunkInitializerThreadState = new ChunkInitializerThreadState(blockManagerThreadCollection, this, this.inMemoryChunks);
		this.chunkInitializerThreadState.putWorkItem(new InitializeYourselfChunkInitializerWorkItem(this.chunkInitializerThreadState), WorkItemPriority.PRIORITY_LOW);


		this.clientServerInterface.Connect();

		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<InMemoryChunksWorkItem>(this.inMemoryChunks, InMemoryChunksWorkItem.class, this.inMemoryChunks.getClass()));
		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<ChunkInitializerWorkItem>(this.chunkInitializerThreadState, ChunkInitializerWorkItem.class, this.chunkInitializerThreadState.getClass()));

		this.requestRootBlockDictionary();
	}

	public CraftingRecipe getCurrentCraftingRecipe() throws Exception{
		return getCraftingRecipesList().get(this.selectedCraftingRecipeIndex);
	}

	public void onClientModelNotification(Object o, ClientModelNotificationType notificationType) throws Exception{
		switch(notificationType){
			case DO_TRY_CRAFTING:{
				this.onTryCrafting();
				break;
			}case DO_TRY_POSITION_CHANGE:{
				this.onTryPositionChange((Vector)o);
				break;
			}case CRAFTING_RECIPE_SELECTION_CHANGE:{
				this.onCraftingRecipeChange((Integer)o);
				break;
			}case INVENTORY_ITEM_SELECTION_CHANGE:{
				this.onInventoryItemSelectionChange((Integer)o);
				break;
			}default:{
				throw new Exception("Unknown event notification in onClientModelNotification: " + notificationType);
			}
		}
	}

	private byte [] gbd(Class<?> c) throws Exception {
		return this.getBlockDataForClass(c);
	}

	public List<CraftingRecipe> getCraftingRecipesList() throws Exception{
		List<CraftingRecipe> rtn = new ArrayList<CraftingRecipe>();
		rtn.add(
			new CraftingRecipe(
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(WoodenBlock.class), 5L)
				}),
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(WoodenPick.class), 1L)

				})
			)
		);

		rtn.add(
			new CraftingRecipe(
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(Rock.class), 3L),
					new PlayerInventoryItemStack(gbd(WoodenBlock.class), 2L)
				}),
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(StonePick.class), 1L)

				})
			)
		);

		rtn.add(
			new CraftingRecipe(
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(IronOxide.class), 5L),
					new PlayerInventoryItemStack(gbd(WoodenBlock.class), 5L)
				}),
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(MetallicIron.class), 1L)

				})
			)
		);

		rtn.add(
			new CraftingRecipe(
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(MetallicIron.class), 3L),
					new PlayerInventoryItemStack(gbd(WoodenBlock.class), 2L)
				}),
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(IronPick.class), 1L)

				})
			)
		);
		return rtn;
	}

	public void onTryPositionChange(Vector delta) throws Exception{
		Coordinate currentPosition = this.getPlayerPosition();
		if(currentPosition != null){
			Coordinate newCandiatePosition = currentPosition.copy().add(delta);

			IndividualBlock blockAtCandidatePlayerPosition = this.readBlockAtCoordinate(newCandiatePosition);
			if(blockAtCandidatePlayerPosition == null){
				logger.info("Not moving to " + newCandiatePosition + " because block at that location is not loaded by client yet and we don't know what's there.");
			}else{
				boolean disableCollisionDetection = false;
				if(
					disableCollisionDetection ||
					blockAtCandidatePlayerPosition instanceof EmptyBlock
				){
					this.onPlayerPositionMove(new PlayerPositionXYZ(this.playerPositionXYZ.getPlayerUUID(), newCandiatePosition));
				}else{
					//  Don't move, there is something in the way.
					logger.info("Not moving to " + newCandiatePosition + " because block at that location has class '" + blockAtCandidatePlayerPosition.getClass().getName() + "'");
				}
			}
		}
	}

	public ConsoleWriterThreadState getConsoleWriterThreadState(){
		return this.blockManagerThreadCollection.getConsoleWriterThreadState();
	}

	public Coordinate getPlayerPosition(){
		return this.playerPositionXYZ == null ? null : this.playerPositionXYZ.getPosition();
	}

	public Set<CuboidAddress> getRequiredRegionsSet() throws Exception{
		Set<CuboidAddress> requiredRegions = new HashSet<CuboidAddress>();
		CuboidAddress reachableMapArea = this.getReachableMapAreaCuboidAddress();
		if(reachableMapArea != null){
			requiredRegions.add(reachableMapArea.copy());
		}
		if(rootBlockDictionaryAddress != null){
			requiredRegions.add(new CuboidAddress(rootBlockDictionaryAddress.copy(), rootBlockDictionaryAddress.add(Coordinate.makeUnitCoordinate(4L)).copy()));
		}
		if(rootBlockDictionary != null){
			for(Map.Entry<String, Coordinate> e : this.rootBlockDictionary.entrySet()){
				Coordinate c = e.getValue();
				requiredRegions.add(new CuboidAddress(c.copy(), c.copy().add(Coordinate.makeUnitCoordinate(4L))));
			}
		}
		return requiredRegions;
	}

	public void notifyLoadedRegionsChanged() throws Exception{
		this.inMemoryChunks.putWorkItem(new UpdateRequiredRegionsWorkItem(this.inMemoryChunks, getRequiredRegionsSet()), WorkItemPriority.PRIORITY_LOW);
		this.logMessage("Just sent an update required regions work item request with " + String.valueOf(getRequiredRegionsSet()));
	}

	public void doMineBlocksAtPosition(Coordinate centerPosition) throws Exception {
		byte [] woodenPickData = this.getBlockDataForClass(WoodenPick.class);
		byte [] stonePickData = this.getBlockDataForClass(StonePick.class);
		byte [] ironPickData = this.getBlockDataForClass(IronPick.class);
		/*  If they have at least one mining pick, mine all the blocks. */
		boolean hasWoodenPick = this.playerInventory.containsBlockCount(woodenPickData, 1L);
		boolean hasStonePick = this.playerInventory.containsBlockCount(stonePickData, 1L);
		boolean hasIronPick = this.playerInventory.containsBlockCount(ironPickData, 1L);

		boolean hasSelectedWoodenPick = this.getSelectedInventoryItemClass() != null && this.getSelectedInventoryItemClass() == WoodenPick.class;
		boolean hasSelectedStonePick = this.getSelectedInventoryItemClass() != null && this.getSelectedInventoryItemClass() == StonePick.class;
		boolean hasSelectedIronPick = this.getSelectedInventoryItemClass() != null && this.getSelectedInventoryItemClass() == IronPick.class;

		byte [] pickDataToUse = null;
		Long miningDistance = null;
		if(hasIronPick && hasSelectedIronPick){
			miningDistance = 3L;
			pickDataToUse = ironPickData;
		}else{
			if(hasStonePick && hasSelectedStonePick){
				miningDistance = 2L;
				pickDataToUse = stonePickData;
			}else{
				if(hasWoodenPick && hasSelectedWoodenPick){
					miningDistance = 1L;
					pickDataToUse = woodenPickData;
				}else{
					miningDistance = 1L; //  Distance of 1, but only mines one at a time.
				}
			}
		}

		//  Top left hand corner of area to mine:
		Coordinate startingPosition = centerPosition.changeByDeltaXYZ(-miningDistance, 0L, -miningDistance);
		CuboidAddress regionToMine = new CuboidAddress(startingPosition, startingPosition.changeByDeltaXYZ(2L * miningDistance, 0L, 2L * miningDistance).add(Coordinate.makeUnitCoordinate(4L)));
		
		List<Coordinate> coordinatesToCheck = new ArrayList<Coordinate>();
		RegionIteration regionIteration = new RegionIteration(startingPosition, regionToMine);
		do{
			coordinatesToCheck.add(regionIteration.getCurrentCoordinate());
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());
		//  Last, try block below:
		Coordinate underBlock = centerPosition.changeByDeltaY(-1L);
		coordinatesToCheck.add(underBlock);

		Long numBlocksMined = 0L;

		for(Coordinate c : coordinatesToCheck){
			IndividualBlock block = readBlockAtCoordinate(c);
			this.logMessage("block at coordinate " + c + " (block instanceof Rock)=" + (block instanceof Rock));
			if(block != null && block.isMineable()){
				this.playerInventory.addItemCountToInventory(block.getBlockData(), 1L);
				//  TODO:  Group these into a single transaction:
				this.logMessage("Writing inventory as " + this.playerInventory.asJsonString());
				numBlocksMined++;
				if(pickDataToUse == null){ //  If they don't have a pick, only mine a single block around the player:
					this.doBlockWriteAtPosition("".getBytes("UTF-8"), c, 0L);
					break;
				}
			}
		}
		if(pickDataToUse != null){
			if(numBlocksMined.equals(1L)){
				this.doBlockWriteAtPosition("".getBytes("UTF-8"), underBlock, 0L);
			}else{
				this.doBlockWriteAtPosition("".getBytes("UTF-8"), centerPosition, miningDistance);
			}
		}
		if(numBlocksMined > 1L){
			this.playerInventory.addItemCountToInventory(pickDataToUse, -1L);
		}
		if(numBlocksMined > 0L){
			this.onPlayerInventoryChangeNotify();
		}
		this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), getPlayerInventoryBlockAddress());
	}

	public void doPlaceItemAtPlayerPosition() throws Exception{
		if(this.getSelectedInventoryItemClass() != null){
			byte [] blockDataToPlace = this.getBlockDataForClass(this.getSelectedInventoryItemClass());
			if(this.playerInventory.containsBlockCount(blockDataToPlace, 1L)){
				this.playerInventory.addItemCountToInventory(blockDataToPlace, -1L);
				this.onPlayerInventoryChangeNotify();
				this.doBlockWriteAtPosition(blockDataToPlace, this.getPlayerPosition(), 0L);
				this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), getPlayerInventoryBlockAddress());
			}else{
				//  Does not have any more of these blocks.
			}
		}
	}

	public boolean hasEnoughForCraftingRecipe(CraftingRecipe recipe) throws Exception{
		for(PlayerInventoryItemStack itemStack : recipe.getConsumedItems()){
			if(!this.playerInventory.containsBlockCount(itemStack.getBlockData(), itemStack.getQuantity())){
				return false;
			}
		}
		return true;
	}

	public void applyItemChangeForCraftingRecipe(CraftingRecipe recipe) throws Exception{
		//  Subtract consumed items:
		for(PlayerInventoryItemStack itemStack : recipe.getConsumedItems()){
			this.playerInventory.addItemCountToInventory(itemStack.getBlockData(), -itemStack.getQuantity());
		}
		//  Add produced items:
		for(PlayerInventoryItemStack itemStack : recipe.getProducedItems()){
			int indexOfItem = this.playerInventory.addItemCountToInventory(itemStack.getBlockData(), itemStack.getQuantity());
			onInventoryItemSelectionChange(indexOfItem);
		}
	}

	public void onTryCrafting() throws Exception{
		if(hasEnoughForCraftingRecipe(getCurrentCraftingRecipe())){
			this.applyItemChangeForCraftingRecipe(getCurrentCraftingRecipe());
			this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), getPlayerInventoryBlockAddress());
		}else{
			this.logMessage("Did not have enough reagents to craft anything.");
		}
		this.onPlayerInventoryChangeNotify();
	}



	public void onUserInterfaceAction(UserInterfaceActionType action) throws Exception {
		switch(action){
			case ACTION_PLACE_BLOCK:{
				this.doPlaceItemAtPlayerPosition();
				break;
			}case ACTION_CRAFTING:{
				this.onTryCrafting();
				break;
			}case ACTION_MINING:{
				this.doMineBlocksAtPosition(this.getPlayerPosition());
				break;
			}default:{
				throw new Exception("Unexpected action=" + action.toString());
			}
		}
	}

	public Cuboid getCuboidForSingleBlock(byte [] data, Coordinate position) throws Exception{
		Long numDimensions = position.getNumDimensions();

		CuboidAddress blocksToChangeAddress = new CuboidAddress(position, position.add(Coordinate.makeUnitCoordinate(4L)));
		BlockMessageBinaryBuffer dataForOneCuboid = new BlockMessageBinaryBuffer();

		Long numBlocks = blocksToChangeAddress.getVolume();
		long [] dataLengths = new long [numBlocks.intValue()];
		for(int i = 0; i < numBlocks; i++){
			dataLengths[i] = data.length;
			dataForOneCuboid.writeBytes(data);
		}

		CuboidDataLengths currentCuboidDataLengths = new CuboidDataLengths(blocksToChangeAddress, dataLengths);
		CuboidData currentCuboidData = new CuboidData(dataForOneCuboid.getUsedBuffer());

		return new Cuboid(blocksToChangeAddress, currentCuboidDataLengths, currentCuboidData);
	}

	public void writeSingleBlockAtPosition(byte [] data, Coordinate position) throws Exception{
		List<Cuboid> cuboids = new ArrayList<Cuboid>();
		cuboids.add(getCuboidForSingleBlock(data, position));
		this.submitChunkToServer(position.getNumDimensions(), cuboids, WorkItemPriority.PRIORITY_LOW, 12345L);
	}

	public void doBlockWriteAtPosition(byte [] data, Coordinate position, Long radius) throws Exception{
		Long numDimensions = position.getNumDimensions();

		Coordinate lower = position.changeX(position.getX() - radius).changeZ(position.getZ() - radius);
		Coordinate upper = position.changeX(position.getX() + radius).changeZ(position.getZ() + radius);
		CuboidAddress blocksToChangeAddress = new CuboidAddress(lower, upper.add(Coordinate.makeUnitCoordinate(4L)));

		List<Cuboid> cuboids = new ArrayList<Cuboid>();
		RegionIteration regionIteration = new RegionIteration(blocksToChangeAddress.getCanonicalLowerCoordinate(), blocksToChangeAddress);
		do{
			Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
			if(!currentCoordinate.equals(this.playerPositionXYZ.getPosition())){
				cuboids.add(getCuboidForSingleBlock(data, currentCoordinate));
			}
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());
		this.submitChunkToServer(numDimensions, cuboids, WorkItemPriority.PRIORITY_LOW, 12345L);
	}

	public boolean isServer(){
		return false;
	}

	public BlockModelInterface getBlockModelInterface(){
		return this;
	}

	public void shutdown() throws Exception {
		for(Map.Entry<String, BlockSession> e : this.getSessionMap().entrySet()){
			//  Gracefully close all sessions:
			this.logMessage("Closing session '" + e.getKey() + "' TODO:  Send some kind of notify to the server here.");
			e.getValue().close("Gracefully closing due to shutdown sequence...");
		}
		try{
			this.clientServerInterface.Disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}
		this.logMessage("Ran shutdown of ClientBlockModelContext.");
	}

	public Coordinate getPlayerInventoryBlockAddress() throws Exception {
		if(this.rootBlockDictionary == null){
			throw new Exception("Not expected.");
		}else{
			return this.rootBlockDictionary.get("player_inventory");
		}
	}

	public Coordinate getPlayerPositionBlockAddress() throws Exception {
		if(this.rootBlockDictionary == null){
			throw new Exception("Not expected.");
		}else{
			return this.rootBlockDictionary.get("player_position");
		}
	}

	public void checkForSpecialCoordinateUpdates(Coordinate currentCoordinate, byte [] blockData) throws Exception{
		if(currentCoordinate.equals(this.rootBlockDictionaryAddress)){
			BlockDictionary dictionaryBlock = (BlockDictionary)this.deserializeBlockData(blockData);
			if(this.rootBlockDictionary == null || !this.rootBlockDictionary.equals(dictionaryBlock)){
				this.rootBlockDictionary = dictionaryBlock;
				//  This is just a hack to unload all chunks that are currently loaded
				//  in order to make sure that all client events are triggered for
				//  the newly considered player model blocks.
				//  There is currently a bug in the 'InMemoryChunks' class where if you
				//  specify a new set of 'required regions' that includes a block that's
				//  inside an existing already-loaded chunk, then the post loading events
				//  that would otherwise have been triggered for that block never happen
				//  (since the block is already loaded, and the update signal was missed).
				//  TODO:  Fix this event issue in the general case.
				this.inMemoryChunks.putWorkItem(new UpdateRequiredRegionsWorkItem(this.inMemoryChunks, new HashSet<CuboidAddress>()), WorkItemPriority.PRIORITY_LOW);
				this.notifyLoadedRegionsChanged();
			}
		}
		if(this.rootBlockDictionary != null){
			if(this.playerPositionXYZ == null && currentCoordinate.equals(getPlayerPositionBlockAddress())){
				this.playerPositionXYZ = (PlayerPositionXYZ)this.deserializeBlockData(blockData);
				this.sendUIEventsToSubscribedThreads(UINotificationType.PLAYER_POSITION, this.playerPositionXYZ.copy(), WorkItemPriority.PRIORITY_LOW);
				getConsoleWriterThreadState().putWorkItem(new TellClientTerminalChangedWorkItem(getConsoleWriterThreadState()), WorkItemPriority.PRIORITY_LOW);
			}else if(currentCoordinate.equals(getPlayerInventoryBlockAddress()) && this.playerInventory.getInventoryItemStackList().size() == 0){

				PlayerInventory playerInventory = (PlayerInventory)this.deserializeBlockData(blockData);
				this.onPlayerInventoryChange(new PlayerInventory(new String(playerInventory.getBlockData(), "UTF-8")));
			}else if(this.playerPositionXYZ != null && currentCoordinate.equals(this.playerPositionXYZ.getPosition())){
				this.playerObject = (PlayerObject)this.deserializeBlockData(blockData);
			}
		}
	}

	public void writeTestBlocks() throws Exception{
		/*
			Write a few block in a specific known pattern.  Used for 
			debugging issues with read/write ordering of cuboid data.
		*/
		Long numDimensions = 4L;
		Coordinate upper = new Coordinate(Arrays.asList(2L, 1L, 2L, 1L));
		Coordinate lower = new Coordinate(Arrays.asList(0L, 0L, 0L, 0L));

		CuboidAddress ca = new CuboidAddress(lower, upper);
		BlockMessageBinaryBuffer cuboidData = new BlockMessageBinaryBuffer();

		byte [] rockData = this.getBlockDataForClass(Rock.class);
		byte [] ironOxideData = this.getBlockDataForClass(IronOxide.class);
		byte [] metallicIronData = this.getBlockDataForClass(MetallicIron.class);
		byte [] ironPickData = this.getBlockDataForClass(IronPick.class);

		long [] dataLengths = new long [4];
		dataLengths[0] = rockData.length;
		cuboidData.writeBytes(rockData);
		dataLengths[1] = ironOxideData.length;
		cuboidData.writeBytes(ironOxideData);
		dataLengths[2] = metallicIronData.length;
		cuboidData.writeBytes(metallicIronData);
		dataLengths[3] = ironPickData.length;
		cuboidData.writeBytes(ironPickData);

		CuboidDataLengths currentCuboidDataLengths = new CuboidDataLengths(ca, dataLengths);
		CuboidData currentCuboidData = new CuboidData(cuboidData.getUsedBuffer());

		List<Cuboid> cuboids = new ArrayList<Cuboid>();
		cuboids.add(new Cuboid(ca, currentCuboidDataLengths, currentCuboidData));
		this.submitChunkToServer(numDimensions, cuboids, WorkItemPriority.PRIORITY_LOW, 12345L);
	}

	public void writeBlocksInRegion(Cuboid cuboid) throws Exception{
		CuboidAddress cuboidAddress = cuboid.getCuboidAddress();
		CuboidDataLengths dataLengths = cuboid.getCuboidDataLengths();
		CuboidData data = cuboid.getCuboidData();

		this.inMemoryChunks.putWorkItem(new HandlePendingChunkWriteWorkItem(this.inMemoryChunks, cuboid.copy()), WorkItemPriority.PRIORITY_LOW);

		long numUninitializedBlocks = 0L;
		RegionIteration regionIteration = new RegionIteration(cuboidAddress.getCanonicalLowerCoordinate(), cuboidAddress);
		do{
			Coordinate currentCoordinate = regionIteration.getCurrentCoordinate();
			long blockOffsetInArray = cuboidAddress.getLinearArrayIndexForCoordinate(currentCoordinate);
			long sizeOfBlock = dataLengths.getLengths()[(int)blockOffsetInArray];
			long offsetOfBlock = dataLengths.getOffsets()[(int)blockOffsetInArray];
			if(sizeOfBlock < 0L){
				//this.logMessage("Size of block was negative.  Uninitialized block.");
				numUninitializedBlocks++;
				this.checkForSpecialCoordinateUpdates(currentCoordinate, null);
			}else{
				//this.logMessage("OFFSET index is " + blockOffsetInArray + " offset of block in data is " + offsetOfBlock + " size of data is " + sizeOfBlock);
				byte [] blockData = data.getDataAtOffset(offsetOfBlock, sizeOfBlock);
				this.checkForSpecialCoordinateUpdates(currentCoordinate, blockData);
			}
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());

		if(numUninitializedBlocks > 0L){
			InitializeChunkWorkItem initializeChunkWorkItem = new InitializeChunkWorkItem(this.chunkInitializerThreadState, cuboid.copy());
			this.chunkInitializerThreadState.putWorkItem(initializeChunkWorkItem, WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void sendBlockMessage(BlockMessage m, BlockSession session) throws Exception{
		this.clientServerInterface.sendBlockMessage(m, session);
	}

	public IndividualBlock readBlockAtCoordinate(Coordinate coordinate) throws Exception{
		return this.inMemoryChunks.readBlockAtCoordinate(coordinate);
	}

	public void loadBlocksFromMemory(ThreeDimensionalCircularBuffer<IndividualBlock> blockBuffer, CuboidAddress areaToInclude, CuboidAddress areaToExclude) throws Exception {
		this.inMemoryChunks.loadBlocksFromMemory(blockBuffer, areaToInclude, areaToExclude);
	}

	public List<Cuboid> getBlocksInRegions(List<CuboidAddress> cuboidAddresses) throws Exception{
		throw new Exception("Not Implemented.");
	}

	public void inMemoryChunksCallbackOnChunkWasWritten(CuboidAddress ca) throws Exception{
		this.sendUIEventsToSubscribedThreads(UINotificationType.UPDATE_MAP_AREA_FLAGS, ca.copy(), WorkItemPriority.PRIORITY_LOW);
	}

	public void inMemoryChunksCallbackOnChunkBecomesPending(CuboidAddress ca) throws Exception{
		this.sendUIEventsToSubscribedThreads(UINotificationType.UPDATE_MAP_AREA_FLAGS, ca.copy(), WorkItemPriority.PRIORITY_LOW);
	}

	public void postCuboidsWrite(Long numDimensions, List<CuboidAddress> cuboidAddresses) throws Exception{

	}

	public void onInventoryItemSelectionChange(int index) throws Exception{
		this.selectedInventoryItemIndex = index;
		this.sendUIEventsToSubscribedThreads(UINotificationType.CURRENTLY_SELECTED_INVENTORY_ITEM, this.selectedInventoryItemIndex, WorkItemPriority.PRIORITY_LOW);
	}

	public void onCraftingRecipeChange(Integer recipeIndex) throws Exception{
		this.selectedCraftingRecipeIndex = recipeIndex;
		this.sendUIEventsToSubscribedThreads(UINotificationType.CURRENTLY_SELECTED_CRAFTING_RECIPE, this.selectedCraftingRecipeIndex, WorkItemPriority.PRIORITY_LOW);
	}

	public void onMapAreaChange(CuboidAddress newMapArea) throws Exception{
		this.mapAreaCuboidAddress = newMapArea;
		//  The chunk initializer needs to know about the reachable area which extends beyond the viewport:
		this.chunkInitializerThreadState.putWorkItem(new ChunkInitializerNotifyMapAreaChangeWorkItem(this.chunkInitializerThreadState, getReachableMapAreaCuboidAddress().copy()), WorkItemPriority.PRIORITY_HIGH);
		this.notifyLoadedRegionsChanged();
	}

	public void onPlayerPositionMove(PlayerPositionXYZ newPosition) throws Exception{
		List<Cuboid> cuboids = new ArrayList<Cuboid>();
		cuboids.add(getCuboidForSingleBlock("".getBytes("UTF-8"), this.playerPositionXYZ.getPosition()));
		this.playerPositionXYZ = newPosition;
		cuboids.add(getCuboidForSingleBlock(this.playerObject.getBlockData(), this.playerPositionXYZ.getPosition()));
		cuboids.add(getCuboidForSingleBlock(this.playerPositionXYZ.asJsonString().getBytes("UTF-8"), getPlayerPositionBlockAddress()));

		this.submitChunkToServer(this.playerPositionXYZ.getPosition().getNumDimensions(), cuboids, WorkItemPriority.PRIORITY_LOW, 12345L);

		this.sendUIEventsToSubscribedThreads(UINotificationType.PLAYER_POSITION, this.playerPositionXYZ.copy(), WorkItemPriority.PRIORITY_LOW);
	}

	public CuboidAddress getReachableMapAreaCuboidAddress() throws Exception{
		if(this.mapAreaCuboidAddress == null){
			return null;
		}else{
			Coordinate lower = this.mapAreaCuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = this.mapAreaCuboidAddress.getCanonicalUpperCoordinate();

			//  Load some area outside the viewport to make moving around more pleasant.
			Long paddingAroundMapArea = 20L;
			Coordinate reachableLower = lower.add(new Coordinate(Arrays.asList(-paddingAroundMapArea, - 1L, -paddingAroundMapArea, -1L)));
			Coordinate reachableUpper = upper.add(new Coordinate(Arrays.asList(paddingAroundMapArea, 1L, paddingAroundMapArea, 1L)));
			//  We need to be able to load the entire 'reachable' area so that it's possible to 
			//  check what the adjacent block is, so we can check if it's possible to move there.
			return new CuboidAddress(reachableLower, reachableUpper);
		}
	}

	public void onPlayerInventoryChangeNotify() throws Exception{
		this.sendUIEventsToSubscribedThreads(UINotificationType.CURRENT_INVENTORY, new PlayerInventory(this.playerInventory.getBlockData()), WorkItemPriority.PRIORITY_LOW);
	}

	public void onPlayerInventoryChange(PlayerInventory newInventory) throws Exception{
		this.playerInventory = newInventory;
		this.onPlayerInventoryChangeNotify();
	}

	public void enqueueChunkUnsubscriptionForServer(List<CuboidAddress> cuboidAddresses, WorkItemPriority priority) throws Exception{
		BlockSession bs = this.getSessionMap().get(this.clientServerInterface.getClientSessionId());
		Long conversationId = 12345L;// TODO
		ProbeRegionsRequestBlockMessage m = new ProbeRegionsRequestBlockMessage(this, cuboidAddresses.get(0).getNumDimensions(), cuboidAddresses, false, false, conversationId);
		SendBlockMessageToSessionWorkItem workItem = new SendBlockMessageToSessionWorkItem(this, bs, m);
		this.putWorkItem(workItem, priority);
	}

	public void enqueueChunkRequestToServer(CuboidAddress cuboidAddress, WorkItemPriority priority) throws Exception{
		this.logMessage("Doing request to server for chunk=" + cuboidAddress);
		List<CuboidAddress> l = new ArrayList<CuboidAddress>();
		l.add(cuboidAddress);
		BlockSession bs = this.getSessionMap().get(this.clientServerInterface.getClientSessionId());
		Long conversationId = 12345L;// TODO
		ProbeRegionsRequestBlockMessage m = new ProbeRegionsRequestBlockMessage(this, cuboidAddress.getNumDimensions(), l, true, true, conversationId);
		SendBlockMessageToSessionWorkItem workItem = new SendBlockMessageToSessionWorkItem(this, bs, m);
		this.putWorkItem(workItem, priority);
	}

	public void submitChunkToServer(Long numDimensions, List<Cuboid> cuboids, WorkItemPriority priority, Long conversationId) throws Exception{
		BlockSession bs = this.getSessionMap().get(this.clientServerInterface.getClientSessionId());
		DescribeRegionsBlockMessage response = new DescribeRegionsBlockMessage(this, numDimensions, cuboids, conversationId);
		SendBlockMessageToSessionWorkItem workItem = new SendBlockMessageToSessionWorkItem(this, bs, response);
		this.putWorkItem(workItem, priority);
	}

	public void requestPlayerProvisioning() throws Exception{
		BlockSession bs = this.getSessionMap().get(this.clientServerInterface.getClientSessionId());

		AuthorizedCommandBlockMessage getRootMessage = new AuthorizedCommandBlockMessage(this, 12345L, authorizedClientId, AuthorizedCommandType.COMMAND_TYPE_PROVISION_PLAYER);

		SendBlockMessageToSessionWorkItem workItem = new SendBlockMessageToSessionWorkItem(this, bs, getRootMessage);
		this.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
	}

	public void requestRootBlockDictionary() throws Exception{
		BlockSession bs = this.getSessionMap().get(this.clientServerInterface.getClientSessionId());

		AuthorizedCommandBlockMessage getRootMessage = new AuthorizedCommandBlockMessage(this, 12345L, authorizedClientId, AuthorizedCommandType.COMMAND_TYPE_REQUEST_ROOT_DICTIONARY_ADDRESS);

		SendBlockMessageToSessionWorkItem workItem = new SendBlockMessageToSessionWorkItem(this, bs, getRootMessage);
		this.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
	}

	public void onAcknowledgementMessage(Long conversationId) throws Exception{
		ChunkInitializerWorkItem workItem = new ChunkInitializerNotifyAcknowledgementWorkItem(this.chunkInitializerThreadState, conversationId);
		this.chunkInitializerThreadState.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
	}

	public void onErrorNotificationBlockMessage(BlockSession blockSession, Long conversationId, BlockMessageErrorType blockMessageErrorType) throws Exception{
		switch(blockMessageErrorType){
			case ROOT_BLOCK_DICTIONARY_UNINITIALIZED:{
				this.requestPlayerProvisioning();
				break;
			}default:{
				throw new Exception("Message type not expected: " + blockMessageErrorType);
			}
		}
	}

	public void onAuthorizedCommandBlockMessage(BlockSession blockSession, Long conversationId, Long authorizedClientId, AuthorizedCommandType authorizedCommandType, Coordinate coordinate) throws Exception{
		switch(authorizedCommandType){
			case COMMAND_TYPE_RESPOND_ROOT_DICTIONARY_ADDRESS:{
				this.rootBlockDictionaryAddress = coordinate;
				this.notifyLoadedRegionsChanged();
				break;
			}default:{
				throw new Exception("Message type not expected: " + authorizedCommandType);
			}
		}
	}

	public WorkItemResult putBlockingWorkItem(BlockModelContextWorkItem workItem, WorkItemPriority priority) throws Exception {
		// TODO:  Push this into a base class
		BlockManagerThread t = this.blockManagerThreadCollection.getThreadById(Thread.currentThread().threadId());
		if(t instanceof WorkItemProcessorTask){
			Class<?> ct = ((WorkItemProcessorTask<?>)t).getWorkItemClass();
			if(ct == BlockModelContextWorkItem.class && workItem.getIsBlocking()){
				throw new Exception("Current thread is instanceof WorkItemProcessorTask<BlockModelContextWorkItem>.  Attempting to block here will cause a deadlock.");
			}else{
				return this.workItemQueue.putBlockingWorkItem(workItem, priority);
			}
		}else{
			return this.workItemQueue.putBlockingWorkItem(workItem, priority);
		}
	}

	public void sendUIEventsToSubscribedThreads(UINotificationType notificationType, Object o, WorkItemPriority priority) throws Exception{
		if(uiEventSubscriptions.containsKey(notificationType)){
			Set<UIEventReceiverThreadState<?>> subscribedThreads = uiEventSubscriptions.get(notificationType);
			for(UIEventReceiverThreadState<?> subscribedThread : subscribedThreads){
				subscribedThread.receiveEventNotification(notificationType, o, priority);
			}
		}
	}

	public void addUIEventSubscription(UINotificationType notificationType, UIEventReceiverThreadState<?> receiverThread) throws Exception{
		if(!uiEventSubscriptions.containsKey(notificationType)){
			this.uiEventSubscriptions.put(notificationType, new HashSet<UIEventReceiverThreadState<?>>());
		}
		Set<UIEventReceiverThreadState<?>> subscribedThreadStates = uiEventSubscriptions.get(notificationType);
		subscribedThreadStates.add(receiverThread);
	}

	public void removeUIEventSubscription(UINotificationType notificationType, UIEventReceiverThreadState<?> receiverThread) throws Exception{
		if(uiEventSubscriptions.containsKey(notificationType)){
			uiEventSubscriptions.get(notificationType).remove(receiverThread);
		}
	}

	public void doUIModelProbeWorkItem(WorkItem workItem, UINotificationType notificationType, UINotificationSubscriptionType subscriptionType, UIEventReceiverThreadState<?> receiverThread) throws Exception{
		if(subscriptionType.equals(UINotificationSubscriptionType.UNSUBSCRIBE)){
			this.removeUIEventSubscription(notificationType, receiverThread);
			this.workItemQueue.addResultForThreadId(new EmptyWorkItemResult(), workItem.getThreadId());
		}else{
			switch(notificationType){
				case CURRENTLY_SELECTED_CRAFTING_RECIPE:{
					this.workItemQueue.addResultForThreadId(new UIModelProbeWorkItemResult(this.selectedCraftingRecipeIndex), workItem.getThreadId());
					break;
				}case CURRENT_INVENTORY:{
					this.workItemQueue.addResultForThreadId(new UIModelProbeWorkItemResult(this.playerInventory), workItem.getThreadId());
					break;
				}case PLAYER_POSITION:{
					PlayerPositionXYZ p = this.playerPositionXYZ == null ? null : this.playerPositionXYZ.copy();
					this.workItemQueue.addResultForThreadId(new UIModelProbeWorkItemResult(p), workItem.getThreadId());
					break;
				}case UPDATE_MAP_AREA_FLAGS:{
					this.workItemQueue.addResultForThreadId(new UIModelProbeWorkItemResult(new Object()), workItem.getThreadId());
					break;
				}case CURRENTLY_SELECTED_INVENTORY_ITEM:{
					this.workItemQueue.addResultForThreadId(new UIModelProbeWorkItemResult(selectedInventoryItemIndex), workItem.getThreadId());
					break;
				}case CURRENT_RECIPE_LIST:{
					this.workItemQueue.addResultForThreadId(new UIModelProbeWorkItemResult(getCraftingRecipesList()), workItem.getThreadId());
					break;
				}default:{
					throw new Exception("Unexpected notificationType=" + notificationType.toString());
				}
			}
			if(subscriptionType.equals(UINotificationSubscriptionType.SUBSCRIBE)){
				this.addUIEventSubscription(notificationType, receiverThread);
			}
		}
	}

	public void destroy(Object o) throws Exception{

	}
}
