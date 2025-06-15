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
	private final Map<String, TextWidthMeasurementWorkItemResult> measuredTextLengths = new ConcurrentHashMap<String, TextWidthMeasurementWorkItemResult>();
	private InMemoryChunks inMemoryChunks;
	private ChunkInitializerThreadState chunkInitializerThreadState;
	private ConsoleWriterThreadState consoleWriterThreadState;
	private SIGWINCHListenerThreadState sigwinchListenerThreadState;
	private final Coordinate playerPositionBlockAddress = new Coordinate(Arrays.asList(99999999L, 99999999L, 99999999L, 99999999L)); //  The location of the block where the player's position will be stored.
	private final Coordinate playerInventoryBlockAddress = new Coordinate(Arrays.asList(99999998L, 99999999L, 99999999L, 99999999L)); //  The location of the block where the player's inventory will be stored.
	private PlayerPositionXYZ playerPositionXYZ = null;
	private PlayerInventory playerInventory = null;

	private CuboidAddress mapAreaCuboidAddress;
	private Class<?> selectedBlockClass = null;

	public ClientBlockModelContext(BlockManagerThreadCollection blockManagerThreadCollection, ClientServerInterface clientServerInterface) throws Exception {
		super(blockManagerThreadCollection, clientServerInterface);
	}

	public void init() throws Exception{
		this.clientServerInterface.setClientBlockModelContext(this);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();

		this.logMessage("Ran init of ClientBlockModelContext.");

		/*  This defines the dimensions of the 'chunks' that are loaded into memory as we move around */
		CuboidAddress chunkSizeCuboidAddress = new CuboidAddress(new Coordinate(Arrays.asList(0L, 0L, 0L, 0L)), new Coordinate(Arrays.asList(2L, 2L, 4L, 0L)));
		this.inMemoryChunks = new InMemoryChunks(blockManagerThreadCollection, this, chunkSizeCuboidAddress);
		this.notifyLoadedRegionsChanged();
		this.chunkInitializerThreadState = new ChunkInitializerThreadState(blockManagerThreadCollection, this, this.inMemoryChunks);
		this.consoleWriterThreadState = new ConsoleWriterThreadState(blockManagerThreadCollection, this);
		this.consoleWriterThreadState.init();

		

		if(this.blockManagerThreadCollection.getIsJNIEnabled()){
			this.sigwinchListenerThreadState = new SIGWINCHListenerThreadState(blockManagerThreadCollection, this);
		}

		this.clientServerInterface.Connect();

		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<InMemoryChunksWorkItem>(this.inMemoryChunks, InMemoryChunksWorkItem.class, this.inMemoryChunks.getClass()));
		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<ChunkInitializerWorkItem>(this.chunkInitializerThreadState, ChunkInitializerWorkItem.class, this.chunkInitializerThreadState.getClass()));
		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<ConsoleWriterWorkItem>(this.consoleWriterThreadState, ConsoleWriterWorkItem.class, this.consoleWriterThreadState.getClass()));
		if(this.blockManagerThreadCollection.getIsJNIEnabled()){
			this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<SIGWINCHListenerWorkItem>(this.sigwinchListenerThreadState, SIGWINCHListenerWorkItem.class, this.sigwinchListenerThreadState.getClass()));
		}
		this.blockManagerThreadCollection.addThread(new StandardInputReaderTask(this, this.consoleWriterThreadState));
		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<BlockModelContextWorkItem>(this, BlockModelContextWorkItem.class, this.getClass()));
	}


	public ConsoleWriterThreadState getConsoleWriterThreadState(){
		return consoleWriterThreadState;
	}

	public Coordinate getPlayerPosition(){
		return this.playerPositionXYZ == null ? null : this.playerPositionXYZ.getPosition();
	}

	public PlayerInventory getPlayerInventory(){
		return this.playerInventory;
	}

	public Long getTerminalDimension(int index, Long defaultValue){
		try{
			List<String> commandParts = Arrays.asList("stty", "size", "-F", "/dev/tty");
			ShellProcessRunner r = new ShellProcessRunner(commandParts);
			ShellProcessFinalResult result = r.getFinalResult();
			if(result.getReturnValue() == 0){
				this.logMessage("Command '" + commandParts + "' ran with success:");
				this.logMessage(new String(result.getOutput().getStdoutOutput(), "UTF-8"));
				return Long.valueOf(new String(result.getOutput().getStdoutOutput(), "UTF-8").trim().split(" ")[index]);
			}else{
				this.logMessage(new String(result.getOutput().getStdoutOutput(), "UTF-8"));
				this.logMessage(new String(result.getOutput().getStderrOutput(), "UTF-8"));
				this.logMessage("Command " + commandParts + " returned non zero: " + String.valueOf(result.getReturnValue()) + ". stdout was: " + new String(result.getOutput().getStdoutOutput(), "UTF-8") +  ". stderr was: " + new String(result.getOutput().getStderrOutput(), "UTF-8"));
			}
		}catch(Exception e){
			this.logException(e);
		}
		this.logMessage("Something went wrong trying to get terminal dimension. Return default value of " + defaultValue);
		return defaultValue;
	}

	public Long getTerminalWidth(){
		return this.getTerminalDimension(1, 80L);
	}

	public Long getTerminalHeight(){
		return this.getTerminalDimension(0, 30L);
	}

	public Set<CuboidAddress> getRequiredRegionsSet() throws Exception{
		Set<CuboidAddress> requiredRegions = new HashSet<CuboidAddress>();
		CuboidAddress reachableMapArea = this.getReachableMapAreaCuboidAddress();
		if(reachableMapArea != null){
			requiredRegions.add(reachableMapArea.copy());
		}
		requiredRegions.add(new CuboidAddress(playerPositionBlockAddress.copy(), playerPositionBlockAddress.copy()));
		requiredRegions.add(new CuboidAddress(playerInventoryBlockAddress.copy(), playerInventoryBlockAddress.copy()));
		return requiredRegions;
	}

	public void notifyLoadedRegionsChanged() throws Exception{
		Coordinate playerPosition = this.getPlayerPosition();
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

		boolean hasSelectedWoodenPick = this.selectedBlockClass != null && this.selectedBlockClass == WoodenPick.class;
		boolean hasSelectedStonePick = this.selectedBlockClass != null && this.selectedBlockClass == StonePick.class;
		boolean hasSelectedIronPick = this.selectedBlockClass != null && this.selectedBlockClass == IronPick.class;

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

		Coordinate startingPosition = centerPosition.changeByDeltaXYZ(-miningDistance, 0L, -miningDistance);
		CuboidAddress regionToMine = new CuboidAddress(startingPosition, startingPosition.changeByDeltaXYZ(2L*miningDistance, 0L, 2L*miningDistance));
		
		List<Coordinate> coordinatesToCheck = new ArrayList<Coordinate>();
		RegionIteration regionIteration = new RegionIteration(startingPosition, regionToMine);
		do{
			coordinatesToCheck.add(regionIteration.getCurrentCoordinate());
		}while (regionIteration.incrementCoordinateWithinCuboidAddress());

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
					this.doBlockWriteAtPlayerPosition("".getBytes("UTF-8"), c, 0L);
					break;
				}
			}
		}
		if(pickDataToUse != null){
			this.doBlockWriteAtPlayerPosition("".getBytes("UTF-8"), centerPosition, miningDistance);
		}
		if(numBlocksMined > 1L){
			this.playerInventory.addItemCountToInventory(pickDataToUse, -1L);
		}
		if(numBlocksMined > 0L){
			this.onPlayerInventoryChange();
		}
		this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), playerInventoryBlockAddress);
	}

	public void doPlaceItemAtPlayerPosition() throws Exception{
		if(this.selectedBlockClass != null){
			byte [] blockDataToPlace = this.getBlockDataForClass(this.selectedBlockClass);
			if(this.playerInventory.containsBlockCount(blockDataToPlace, 1L)){
				this.playerInventory.addItemCountToInventory(blockDataToPlace, -1L);
				this.onPlayerInventoryChange();
				this.doBlockWriteAtPlayerPosition(blockDataToPlace, this.getPlayerPosition(), 0L);
				this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), playerInventoryBlockAddress);
			}else{
				//  Does not have any more of these blocks.
			}
		}
	}

	public void onTryCrafting() throws Exception{
		byte [] rockData = this.getBlockDataForClass(Rock.class);
		byte [] ironPickData = this.getBlockDataForClass(IronPick.class);
		byte [] stonePickData = this.getBlockDataForClass(StonePick.class);
		byte [] woodenPickData = this.getBlockDataForClass(WoodenPick.class);
		byte [] ironOxideData = this.getBlockDataForClass(IronOxide.class);
		byte [] woodenBlockData = this.getBlockDataForClass(WoodenBlock.class);
		byte [] metallicIronBlockData = this.getBlockDataForClass(MetallicIron.class);
		/*  Try to craft a pick: */
		Long requiredMetallicIronForIronPick = 3L;
		Long requiredWoodenBlocksForIronPick = 2L;
		Long numIronPicksCreated = 1L;

		Long requiredIronOxide = 5L;
		Long requiredWoodenBlocks = 5L;
		Long numMetallicIronCreated = 1L;

		Long requiredRocksForStonePick = 3L;
		Long requiredWoodenBlocksForStonePick = 2L;
		Long numStonePicksCreated = 1L;

		Long requiredWoodForWoodenPick = 5L;
		Long numWoodenPicksCreated = 1L;

		//  Try crafting iron pick axe
		if(
			this.playerInventory.containsBlockCount(metallicIronBlockData, requiredMetallicIronForIronPick) &&
			this.playerInventory.containsBlockCount(woodenBlockData, requiredWoodenBlocksForIronPick)
		){
			this.playerInventory.addItemCountToInventory(metallicIronBlockData, -requiredMetallicIronForIronPick);
			this.playerInventory.addItemCountToInventory(woodenBlockData, -requiredWoodenBlocksForIronPick);
			this.playerInventory.addItemCountToInventory(ironPickData, numIronPicksCreated);
			this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), playerInventoryBlockAddress);
		}else{
			/*  Try to craft metallic iron: */
			if(
				this.playerInventory.containsBlockCount(ironOxideData, requiredIronOxide) &&
				this.playerInventory.containsBlockCount(woodenBlockData, requiredWoodenBlocks)
			){
				this.playerInventory.addItemCountToInventory(ironOxideData, -requiredIronOxide);
				this.playerInventory.addItemCountToInventory(woodenBlockData, -requiredWoodenBlocks);
				this.playerInventory.addItemCountToInventory(metallicIronBlockData, numMetallicIronCreated);
				this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), playerInventoryBlockAddress);
			}else{
				//  Try crafting stone pick axe
				if(
					this.playerInventory.containsBlockCount(rockData, requiredRocksForStonePick) &&
					this.playerInventory.containsBlockCount(woodenBlockData, requiredWoodenBlocksForStonePick)
				){
					this.playerInventory.addItemCountToInventory(rockData, -requiredRocksForStonePick);
					this.playerInventory.addItemCountToInventory(woodenBlockData, -requiredWoodenBlocksForStonePick);
					this.playerInventory.addItemCountToInventory(stonePickData, numStonePicksCreated);
					this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), playerInventoryBlockAddress);
				}else{
					//  Try crafting wooden pick axe
					if(
						this.playerInventory.containsBlockCount(woodenBlockData, requiredWoodForWoodenPick)
					){
						this.playerInventory.addItemCountToInventory(woodenBlockData, -requiredWoodForWoodenPick);
						this.playerInventory.addItemCountToInventory(woodenPickData, numWoodenPicksCreated);
						this.writeSingleBlockAtPosition(this.playerInventory.asJsonString().getBytes("UTF-8"), playerInventoryBlockAddress);
					}else{
						this.logMessage("Did not have enough reagents to craft anything.");
					}
				}
			}
		}
		this.onPlayerInventoryChange();
	}

	public TextWidthMeasurementWorkItemResult measureTextLengthOnTerminal(String text) throws Exception{
		if(!measuredTextLengths.containsKey(text)){
			TextWidthMeasurementWorkItem wm = new TextWidthMeasurementWorkItem(this.consoleWriterThreadState, text);
			TextWidthMeasurementWorkItemResult m = (TextWidthMeasurementWorkItemResult)this.consoleWriterThreadState.putBlockingWorkItem(wm, WorkItemPriority.PRIORITY_LOW);
			measuredTextLengths.put(text, m);
		}
		return this.measuredTextLengths.get(text);
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

	public void writeSingleBlockAtPosition(byte [] data, Coordinate position) throws Exception{
		Long numDimensions = position.getNumDimensions();

		CuboidAddress blocksToChangeAddress = new CuboidAddress(position, position);
		BlockMessageBinaryBuffer dataForOneCuboid = new BlockMessageBinaryBuffer();

		Long numBlocks = blocksToChangeAddress.getVolume();
		long [] dataLengths = new long [numBlocks.intValue()];
		for(int i = 0; i < numBlocks; i++){
			dataLengths[i] = data.length;
			dataForOneCuboid.writeBytes(data);
		}

		CuboidDataLengths currentCuboidDataLengths = new CuboidDataLengths(blocksToChangeAddress, dataLengths);
		CuboidData currentCuboidData = new CuboidData(dataForOneCuboid.getUsedBuffer());

		Cuboid c = new Cuboid(blocksToChangeAddress, currentCuboidDataLengths, currentCuboidData);
		this.submitChunkToServer(c, WorkItemPriority.PRIORITY_HIGH, 12345L);
	}

	public void doBlockWriteAtPlayerPosition(byte [] data, Coordinate position, Long radius) throws Exception{
		Long numDimensions = position.getNumDimensions();

		Coordinate lower = position.changeX(position.getX() - radius).changeZ(position.getZ() - radius);
		Coordinate upper = position.changeX(position.getX() + radius).changeZ(position.getZ() + radius);
		CuboidAddress blocksToChangeAddress = new CuboidAddress(lower, upper);
		BlockMessageBinaryBuffer dataForOneCuboid = new BlockMessageBinaryBuffer();

		Long numBlocks = blocksToChangeAddress.getVolume();
		long [] dataLengths = new long [numBlocks.intValue()];
		for(int i = 0; i < numBlocks; i++){
			dataLengths[i] = data.length;
			dataForOneCuboid.writeBytes(data);
		}

		CuboidDataLengths currentCuboidDataLengths = new CuboidDataLengths(blocksToChangeAddress, dataLengths);
		CuboidData currentCuboidData = new CuboidData(dataForOneCuboid.getUsedBuffer());

		Cuboid c = new Cuboid(blocksToChangeAddress, currentCuboidDataLengths, currentCuboidData);
		this.submitChunkToServer(c, WorkItemPriority.PRIORITY_HIGH, 12345L);
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

	public void notifyOfSIGWINCH() throws Exception{
		this.onTerminalWindowChanged();
	}

	public void onTerminalWindowChanged() throws Exception{
		Long terminalWidth = this.getTerminalWidth();
		Long terminalHeight = this.getTerminalHeight();
		
		GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
		String exampleFrameCharacter = mode.equals(GraphicsMode.ASCII) ? CharacterConstants.PLUS_SIGN : CharacterConstants.BOX_DRAWINGS_DOUBLE_HORIZONTAL;
		Long frameCharacterWidth = this.measureTextLengthOnTerminal(exampleFrameCharacter).getDeltaX();
		this.consoleWriterThreadState.putWorkItem(new TerminalDimensionsChangedWorkItem(this.consoleWriterThreadState, terminalWidth, terminalHeight, frameCharacterWidth), WorkItemPriority.PRIORITY_LOW);

		if(this.playerInventory != null){
			this.onPlayerInventoryChange();
		}
	}

	public void checkForSpecialCoordinateUpdates(Coordinate currentCoordinate, byte [] blockData) throws Exception{
		//  TODO:  Figure out a better way to have custom event handlers for specific blocks of interest like this:
		if(this.playerPositionXYZ == null && currentCoordinate.equals(playerPositionBlockAddress)){

			IndividualBlock blockWritten = blockData == null ? new UninitializedBlock() : this.deserializeBlockData(blockData);

			if(blockWritten instanceof UninitializedBlock){
				this.playerPositionXYZ = new PlayerPositionXYZ("player:f7828de4-5e6e-4ff7-8b35-752734a2b59d", 0L, 0L, 0L); //  Initial value when world is started.
			}else if(blockWritten instanceof PlayerPositionXYZ){
				// Make a new copy of the position to operate on:
				this.playerPositionXYZ = new PlayerPositionXYZ(new String(blockWritten.getBlockData(), "UTF-8"));
			}else{
				throw new Exception("Expected block to be of type PlayerPositionXYZ, but it was type " + blockWritten.getClass().getName());
			}
			consoleWriterThreadState.putWorkItem(new CNMapAreaNotifyPlayerPositionChangeWorkItem(consoleWriterThreadState, null, this.playerPositionXYZ.getPosition().copy()), WorkItemPriority.PRIORITY_LOW);
			this.onTerminalWindowChanged();
		}else if(this.playerInventory == null && currentCoordinate.equals(playerInventoryBlockAddress)){

			IndividualBlock blockWritten = blockData == null ? new UninitializedBlock() : this.deserializeBlockData(blockData);

			if(blockWritten instanceof UninitializedBlock){
				this.playerInventory = new PlayerInventory();
				this.onPlayerInventoryChange();
				this.logMessage("initialize an empty inventory.");
			}else if(blockWritten instanceof PlayerInventory){
				// Make a new copy of the position to operate on:
				this.playerInventory = new PlayerInventory(new String(blockWritten.getBlockData(), "UTF-8"));

				this.onPlayerInventoryChange();

				this.logMessage("Loading this new inventory: " + new String(blockWritten.getBlockData(), "UTF-8"));
			}else{
				throw new Exception("Expected block to be of type PlayerInventory, but it was type " + blockWritten.getClass().getName());
			}
		}
	}




	public void writeTestBlocks() throws Exception{
		/*
			Write a few block in a specific known pattern.  Used for 
			debugging issues with read/write ordering of cuboid data.
		*/
		Long numDimensions = 4L;
		Coordinate upper = new Coordinate(Arrays.asList(1L, 0L, 1L, 0L));
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

		Cuboid c = new Cuboid(ca, currentCuboidDataLengths, currentCuboidData);
		this.submitChunkToServer(c, WorkItemPriority.PRIORITY_LOW, 12345L);
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

	public List<Cuboid> getBlocksInRegions(List<CuboidAddress> cuboidAddresses) throws Exception{
		throw new Exception("Not Implemented.");
	}

	public void inMemoryChunksCallbackOnChunkWasWritten(CuboidAddress ca) throws Exception{
		this.consoleWriterThreadState.putWorkItem(new CNUpdateMapAreaFlagsWorkItem(consoleWriterThreadState, ca.copy()), WorkItemPriority.PRIORITY_LOW);
	}

	public void inMemoryChunksCallbackOnChunkBecomesPending(CuboidAddress ca) throws Exception{
		this.consoleWriterThreadState.putWorkItem(new CNUpdateMapAreaFlagsWorkItem(consoleWriterThreadState, ca.copy()), WorkItemPriority.PRIORITY_MEDIUM);
	}

	public void postCuboidsWrite(Long numDimensions, List<CuboidAddress> cuboidAddresses) throws Exception{

	}

	public void onInventoryItemSelectionChange(Class<?> blockClass) throws Exception{
		this.selectedBlockClass = blockClass;
	}

	public void onMapAreaChange(CuboidAddress newMapArea) throws Exception{
		this.mapAreaCuboidAddress = newMapArea;
		CuboidAddress ga = newMapArea == null ? null : newMapArea.copy();
		//  The chunk initializer needs to know about the reachable area which extends beyond the viewport:
		this.chunkInitializerThreadState.putWorkItem(new ChunkInitializerNotifyMapAreaChangeWorkItem(this.chunkInitializerThreadState, getReachableMapAreaCuboidAddress().copy()), WorkItemPriority.PRIORITY_HIGH);
		this.notifyLoadedRegionsChanged();
	}

	public void onPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		this.playerPositionXYZ = new PlayerPositionXYZ(this.playerPositionXYZ.getPlayerUUID(), newPosition.getX(), newPosition.getY(), newPosition.getZ());

		this.writeSingleBlockAtPosition(this.playerPositionXYZ.asJsonString().getBytes("UTF-8"), playerPositionBlockAddress);

		Coordinate prev = previousPosition == null ? null : previousPosition.copy();
		this.chunkInitializerThreadState.putWorkItem(new ChunkInitializerNotifyPlayerPositionChangeWorkItem(this.chunkInitializerThreadState, prev, newPosition.copy()), WorkItemPriority.PRIORITY_HIGH);
		this.inMemoryChunks.putWorkItem(new InMemoryChunksNotifyPlayerPositionChangeWorkItem(this.inMemoryChunks, newPosition.copy()), WorkItemPriority.PRIORITY_HIGH);
		this.logMessage("Client acknowledged player position update from " + previousPosition  + " to " + newPosition + ".");
	}

	public CuboidAddress getReachableMapAreaCuboidAddress() throws Exception{
		if(this.mapAreaCuboidAddress == null){
			return null;
		}else{
			Coordinate lower = this.mapAreaCuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = this.mapAreaCuboidAddress.getCanonicalUpperCoordinate();

			//  Load some area outside the viewport to make moving around more pleasant.
			Long paddingAroundMapArea = 20L;
			Coordinate reachableLower = lower.changeByDeltaXYZ(-paddingAroundMapArea, - 1L, -paddingAroundMapArea);
			Coordinate reachableUpper = upper.changeByDeltaXYZ(paddingAroundMapArea, 1L, paddingAroundMapArea);
			//  We need to be able to load the entire 'reachable' area so that it's possible to 
			//  check what the adjacent block is, so we can check if it's possible to move there.
			return new CuboidAddress(reachableLower, reachableUpper);
		}
	}


	public void onPlayerInventoryChange() throws Exception{
		this.consoleWriterThreadState.putWorkItem(new CNPlayerInventoryChangeWorkItem(this.consoleWriterThreadState, new PlayerInventory(this.playerInventory.getBlockData())), WorkItemPriority.PRIORITY_LOW);
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

	public void submitChunkToServer(Cuboid cuboid, WorkItemPriority priority, Long conversationId) throws Exception{
		BlockSession bs = this.getSessionMap().get(this.clientServerInterface.getClientSessionId());
		DescribeRegionsBlockMessage response = new DescribeRegionsBlockMessage(this, cuboid.getNumDimensions(), Arrays.asList(cuboid), conversationId);
		SendBlockMessageToSessionWorkItem workItem = new SendBlockMessageToSessionWorkItem(this, bs, response);
		this.putWorkItem(workItem, priority);
	}

	public void onAcknowledgementMessage(Long conversationId) throws Exception{
		ChunkInitializerWorkItem workItem = new ChunkInitializerNotifyAcknowledgementWorkItem(this.chunkInitializerThreadState, conversationId);
		this.chunkInitializerThreadState.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
	}

}
