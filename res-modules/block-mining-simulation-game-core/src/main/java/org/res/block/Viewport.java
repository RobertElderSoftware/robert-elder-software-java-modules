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

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class Viewport extends WorkItemQueueOwner<ViewportWorkItem> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Long terminalWidth;
	private Long terminalHeight;
	private Long viewportWidth;
	private Long viewportHeight;
	private Long frameWidthTop;
	private Long frameCharacterWidth;
	private Long inventoryAreaHeight;
	private Long gameAreaCellWidth;
	private PlayerInventory playerInventory = null;
	private Coordinate playerPosition;
	private CuboidAddress gameAreaCuboidAddress;
	private Set<Coordinate> stalePlayerPositions = new HashSet<Coordinate>();
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private ClientBlockModelContext clientBlockModelContext;
	private BlockSchema blockSchema;

	private ViewportCell[][] viewportCells = null;
	private String[][] screenPrints = null;

	public Viewport(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, BlockSchema blockSchema) throws Exception {
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
		this.blockSchema = blockSchema;
		//  Clear screen at startup
		System.out.println("\033[2J");
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void onViewportDimensionsChange(Long terminalWidth, Long terminalHeight, Long viewportWidth, Long viewportHeight) throws Exception{
		this.terminalWidth = terminalWidth;
		this.terminalHeight = terminalHeight;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;

		this.viewportCells = new ViewportCell[this.viewportWidth.intValue()][this.viewportHeight.intValue()];
		this.screenPrints = new String[this.terminalWidth.intValue()][this.terminalHeight.intValue()];
		for(int i = 0; i < this.viewportWidth.intValue(); i++){
			for(int j = 0; j < this.viewportHeight.intValue(); j++){
				this.viewportCells[i][j] = new ViewportCell();
			}
		}
	}

	public void onPlayerPositionChange(Coordinate previousPosition, Coordinate newPosition) throws Exception{
		if(previousPosition != null){
			this.stalePlayerPositions.add(previousPosition);
		}
		if(newPosition != null){
			this.stalePlayerPositions.add(newPosition);
		}

		this.playerPosition = newPosition;

		if(previousPosition != null){
			this.updateViewportFlags(new CuboidAddress(previousPosition, previousPosition));
		}
		if(newPosition != null){
			this.updateViewportFlags(new CuboidAddress(newPosition, newPosition));
		}

		this.reprintFrame();
	}

	public void onPlayerInventoryChange(PlayerInventory playerInventory) throws Exception{
		this.playerInventory = playerInventory;
		this.reprintFrame();
	}

	public void onGameAreaChange(CuboidAddress newGameArea) throws Exception{
		CuboidAddress previousGameArea = this.gameAreaCuboidAddress;
		this.gameAreaCuboidAddress = newGameArea;

		if(previousGameArea == null){
			this.updateViewportFlags(newGameArea);
		}else{
			Coordinate beforeLower = previousGameArea.getCanonicalLowerCoordinate();
			Coordinate beforeUpper = previousGameArea.getCanonicalUpperCoordinate();
			Coordinate afterLower = newGameArea.getCanonicalLowerCoordinate();
			Coordinate afterUpper = newGameArea.getCanonicalUpperCoordinate();
			
			Coordinate diffLower = afterLower.subtract(beforeLower);
			Coordinate diffUpper = afterUpper.subtract(beforeUpper);
			if(diffLower.equals(diffUpper)){
				Long diffX = diffLower.getX();
				Long diffY = diffLower.getY();
				Long diffZ = diffLower.getZ();

				/* Viewport flags can be changed by just shifting the array around: */
				if(diffX > 0){
					//  Move all cells to the left by diffX
					for(int i = 0; i < (this.viewportWidth.intValue() - diffX.intValue()); i++){
						for(int j = 0; j < this.viewportHeight.intValue(); j++){
							boolean has_change = !this.viewportCells[i][j].equals(this.viewportCells[i + diffX.intValue()][j]);
							this.viewportCells[i][j] = this.viewportCells[i + diffX.intValue()][j];
							if(this.viewportCells[i][j].getCurrentBlock() == null){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.PENDING_LOAD);
							}else if(has_change){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.BLOCK_CHANGE);
							}
						}
					}
					//  Initialize new cells in the empty space
					for(int i = (this.viewportWidth.intValue() - diffX.intValue()); i < this.viewportWidth.intValue(); i++){
						for(int j = 0; j < this.viewportHeight.intValue(); j++){
							this.viewportCells[i][j] = new ViewportCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.gameAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.updateViewportFlags(new CuboidAddress(theCoordinate, theCoordinate));
						}
					}

					this.printViewportUpdates(this.gameAreaCuboidAddress);
				}else if(diffX < 0){
					//  Move all cells to the right by diffX
					for(int i = this.viewportWidth.intValue() -1; i >= (-diffX.intValue()); i--){
						for(int j = 0; j < this.viewportHeight.intValue(); j++){
							boolean has_change = !this.viewportCells[i][j].equals(this.viewportCells[i + diffX.intValue()][j]);
							this.viewportCells[i][j] = this.viewportCells[i + diffX.intValue()][j];
							if(this.viewportCells[i][j].getCurrentBlock() == null){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.PENDING_LOAD);
							}else if(has_change){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.BLOCK_CHANGE);
							}
						}
					}
					//  Initialize new cells in the empty space
					for(int i = (-diffX.intValue()) -1; i >= 0; i--){
						for(int j = 0; j < this.viewportHeight.intValue(); j++){
							this.viewportCells[i][j] = new ViewportCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.gameAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.updateViewportFlags(new CuboidAddress(theCoordinate, theCoordinate));
						}
					}
					this.printViewportUpdates(this.gameAreaCuboidAddress);
				}

				if(!diffY.equals(0L)){
					//  Just update all viewport flags when go to a different level:
					this.updateViewportFlags(this.gameAreaCuboidAddress);
				}

				if(diffZ > 0){
					//  Move all cells down by diffZ (up in viewport cell array)
					for(int i = 0; i < this.viewportWidth.intValue(); i++){
						for(int j = this.viewportHeight.intValue() -1; j >= diffZ.intValue(); j--){
							boolean has_change = !this.viewportCells[i][j].equals(this.viewportCells[i][j - diffZ.intValue()]);
							this.viewportCells[i][j] = this.viewportCells[i][j - diffZ.intValue()];
							if(this.viewportCells[i][j].getCurrentBlock() == null){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.PENDING_LOAD);
							}else if(has_change){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.BLOCK_CHANGE);
							}
						}
					}
					//  Initialize new cells in the empty space
					for(int i = 0; i < this.viewportWidth.intValue(); i++){
						for(int j = 0; j < diffZ.intValue(); j++){
							this.viewportCells[i][j] = new ViewportCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.gameAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.updateViewportFlags(new CuboidAddress(theCoordinate, theCoordinate));
						}
					}
					this.printViewportUpdates(this.gameAreaCuboidAddress);
				}else if(diffZ < 0){
					//  Move all cells up by diffZ (down in viewport cell array)
					for(int i = 0; i < this.viewportWidth.intValue(); i++){
						for(int j = 0; j < this.viewportHeight.intValue() + diffZ.intValue(); j++){
							boolean has_change = !this.viewportCells[i][j].equals(this.viewportCells[i][j - diffZ.intValue()]);
							this.viewportCells[i][j] = this.viewportCells[i][j - diffZ.intValue()];
							if(this.viewportCells[i][j].getCurrentBlock() == null){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.PENDING_LOAD);
							}else if(has_change){
								this.viewportCells[i][j].addViewportCellFlag(ViewportCellFlag.BLOCK_CHANGE);
							}
						}
					}
					//  Initialize new cells in the empty space
					for(int i = 0; i < this.viewportWidth.intValue(); i++){
						for(int j = this.viewportHeight.intValue() + diffZ.intValue(); j < this.viewportHeight.intValue(); j++){
							this.viewportCells[i][j] = new ViewportCell();
							Coordinate theCoordinate = afterLower.changeByDeltaXYZ(Long.valueOf(i), 0L, (this.gameAreaCuboidAddress.getWidthForIndex(2L) - 1L) - Long.valueOf(j));
							this.updateViewportFlags(new CuboidAddress(theCoordinate, theCoordinate));
						}
					}
					this.printViewportUpdates(this.gameAreaCuboidAddress);
				}
			}else{
				throw new Exception("Case not considered: diffLower=" + diffLower + " but diffUpper=" + diffUpper);
			}
		}
	}

	public void onFrameDimensionsChange(Long frameWidthTop, Long frameCharacterWidth, Long inventoryAreaHeight, Long gameAreaCellWidth) throws Exception{
		this.frameWidthTop = frameWidthTop;
		this.frameCharacterWidth = frameCharacterWidth;
		this.inventoryAreaHeight = inventoryAreaHeight;
		this.gameAreaCellWidth = gameAreaCellWidth;
		this.reprintFrame();
	}

	public ViewportCell recalculateViewportCellsAtCoordinate(Coordinate currentViewportCoordinate) throws Exception {
		IndividualBlock b = clientBlockModelContext.readBlockAtCoordinate(currentViewportCoordinate);
		Long xOffsetScreen = currentViewportCoordinate.getX() - this.gameAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
		Long yOffsetScreen = (this.gameAreaCuboidAddress.getWidthForIndex(2L) - 1L) - (currentViewportCoordinate.getZ() - this.gameAreaCuboidAddress.getCanonicalLowerCoordinate().getZ());
		ViewportCell currentViewportCell = this.viewportCells[xOffsetScreen.intValue()][yOffsetScreen.intValue()];

		if(b == null){ /* Chunk not even loaded. */
			currentViewportCell.setCurrentBlock(b);
			currentViewportCell.addViewportCellFlag(ViewportCellFlag.PENDING_LOAD);
		}else{
			currentViewportCell.setCurrentBlock(b);

			if(currentViewportCell.hasPendingLoadFlags()){ //  If this block was pending before, but now it's not, this requires a reprint.
				currentViewportCell.addViewportCellFlag(ViewportCellFlag.BLOCK_CHANGE);
				currentViewportCell.clearPendingLoadFlags();
			}
		}

		/*  Does player position need to be re-rendered? */
		if(this.stalePlayerPositions.contains(currentViewportCoordinate)){
			currentViewportCell.addViewportCellFlag(ViewportCellFlag.PLAYER_MOVEMENT);
			this.stalePlayerPositions.remove(currentViewportCoordinate);
		}
		return currentViewportCell;
	}

	public void updateViewportFlags(CuboidAddress areaToUpdate) throws Exception {
		if(this.gameAreaCuboidAddress != null){
			logger.info("Doing updateViewportFlags with viewport cuboid as " + this.gameAreaCuboidAddress + ".");
		
			RegionIteration regionIteration = new RegionIteration(areaToUpdate.getCanonicalLowerCoordinate(), areaToUpdate);
			do{
				Coordinate currentViewportCoordinate = regionIteration.getCurrentCoordinate();
				if(this.gameAreaCuboidAddress.containsCoordinate(currentViewportCoordinate)){
					this.recalculateViewportCellsAtCoordinate(currentViewportCoordinate);
				}
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());

			this.printViewportUpdates(areaToUpdate);
			return;
		}else{
			logger.info("missing updateViewportFlags because this.gameAreaCuboidAddress was null.");
			return;
		}
	}

	private void printTextInGameAreaXY(String blockDataString, Long x, Long y, boolean isPlayerPosition){
		// A lot of the unicode characters in the game area take up two spaces:
		printTextInAtScreenXY(blockDataString, (x * this.gameAreaCellWidth) + this.frameCharacterWidth, y + this.frameWidthTop, isPlayerPosition);
	}

	private void doScreenPrintIfChanged(String currentPrint, Long x, Long y){
		// Is this text the same as the last text printed at this location?
		if(currentPrint.equals(this.screenPrints[x.intValue()][y.intValue()])){
			// Do nothing.
		}else{
			System.out.print(currentPrint);
		}
		// Cache current print for comparison next time.
		this.screenPrints[x.intValue()][y.intValue()] = currentPrint;
	}

	private void printTextInAtScreenXY(String blockDataString, Long x, Long y, boolean isPlayerPosition){
		String backgroundColour = isPlayerPosition ? "\033[42m" : "\033[0m";
		//  Escape sequence offsets are 1 based
		String s = "\033[" + (y+1) + ";" + (x+1) + "H" + backgroundColour + blockDataString;
		this.doScreenPrintIfChanged(s, x, y);
	}

	public void printWordInAtScreenXY(String s, Long x, Long y, boolean isPlayerPosition){
		for(int i = 0; i < s.length(); i++){
			this.printTextInAtScreenXY(String.valueOf(s.charAt(i)), x + i, y, isPlayerPosition);
		}
	}

	public void reprintFrame() throws Exception {
		if(this.viewportCells != null && this.screenPrints != null){
			Long fchrw = this.frameCharacterWidth;
			//  Top border
			this.printWordInAtScreenXY("                   ", 10L, 0L, false); //  Required in wide character mode otherwise coordinates don't show correctly.
			this.printTextInAtScreenXY("\u2554", 0L, 0L, false);
			for(long l = 1L; l < (this.terminalWidth / fchrw) -1L; l++){
				this.printTextInAtScreenXY("\u2550", l * fchrw, 0L, false);
			}
			this.printTextInAtScreenXY("\u2557", this.terminalWidth - fchrw * 1L, 0L, false);

			//  Player coordinate:
			String playerCoordinateString = "X=" + this.getPlayerPosition().getX() + ", Y=" + this.getPlayerPosition().getY() + ", Z=" + this.getPlayerPosition().getZ();
			this.printWordInAtScreenXY(playerCoordinateString, 10L, 0L, false);

			//  Right border and left border
			for(long l = 1L; l < this.terminalHeight; l++){
				this.printTextInAtScreenXY("\u2551", 0L, l, false);
				this.printTextInAtScreenXY("\u2551", this.terminalWidth - fchrw * 1L, l, false);
			}

			//  Bottom border under game area:
			this.printTextInAtScreenXY("\u2560", 0L, this.terminalHeight - this.inventoryAreaHeight, false);
			for(long x = 1L; x < (this.terminalWidth / fchrw) -1L; x++){
				this.printTextInAtScreenXY("\u2550", x * fchrw, this.terminalHeight - this.inventoryAreaHeight, false);
			}
			this.printTextInAtScreenXY("\u2563", this.terminalWidth - fchrw * 1L, this.terminalHeight - this.inventoryAreaHeight, false);
			this.printWordInAtScreenXY("             ", 10L, this.terminalHeight - this.inventoryAreaHeight, false); //  Required in wide character mode.
			this.printWordInAtScreenXY("- Inventory -", 10L, this.terminalHeight - this.inventoryAreaHeight, false);
			PlayerInventory inventory = this.getPlayerInventory();
			if(inventory != null){
				List<PlayerInventoryItemStack> itemStacks = inventory.getInventoryItemStackList();
				logger.info("Here is the inventory: " + inventory.asJsonString() + ".");
				Long inventoryItemsXOffset = 2L;
				if(itemStacks.size() == 0){
					this.printWordInAtScreenXY("Empty.", inventoryItemsXOffset, this.terminalHeight - this.inventoryAreaHeight + 2, false);
				}else{
					for(int i = 0; i < itemStacks.size(); i++){
						PlayerInventoryItemStack stack = itemStacks.get(i);
						IndividualBlock blockFromStack = stack.getBlock(this.blockSchema);
						//  Need to print any unicode characters together because the 'printWordInAtScreenXY' will split up surrogate pairs and break them:
						int maxItemsInColumn = 4;
						int xOffset = (i / maxItemsInColumn) * 30;
						int yOffset = (i % maxItemsInColumn) * 2;
						this.printTextInAtScreenXY(blockFromStack.getTerminalPresentation(), inventoryItemsXOffset + xOffset, this.terminalHeight - this.inventoryAreaHeight + 2 + yOffset, false);
						this.printWordInAtScreenXY("  (" + stack.getQuantity().toString() + ") " + blockFromStack.getClass().getSimpleName() + " ", inventoryItemsXOffset + 2L + xOffset, this.terminalHeight - this.inventoryAreaHeight + 2 + yOffset, false);
					}
				}
			}

			//  Right border and left border down where inventory is
			for(long y = this.terminalHeight - this.inventoryAreaHeight + 1L; y < this.terminalHeight -1L; y++){
				this.printTextInAtScreenXY("\u2551", 0L, y, false);
				this.printTextInAtScreenXY("\u2551", this.terminalWidth - fchrw * 1L, y, false);
			}

			//  Very bottom border:
			this.printTextInAtScreenXY("\u255A", 0L, this.terminalHeight -1L, false);
			for(long x = 1L; x < (this.terminalWidth / fchrw) -1L; x++){
				this.printTextInAtScreenXY("\u2550", x * fchrw, this.terminalHeight -1L, false);
			}
			this.printTextInAtScreenXY("\u255D", this.terminalWidth - fchrw * 1L, this.terminalHeight -1L, false);
		}
	}

	public String whitespacePadViewportCell(String presentedText) throws Exception{
		//  An empty cell with zero byte length will otherwise render to nothing causing the last cell to not get overprinted.
		//  Adding the extra space after the Rocks because the 'rock' emoji only takes up one space for the background colour, and BG colour won't update correctly otherwise.

		Long presentedTextWidth = this.clientBlockModelContext.measureTextLengthOnTerminal(presentedText);
		Long paddedViewportCellWidth = this.gameAreaCellWidth;
		if(presentedTextWidth > paddedViewportCellWidth){
			throw new Exception("Character has terminal width of " + presentedTextWidth + " which is wider than allowed paddedViewportCellWidth value of " + paddedViewportCellWidth);
		}

		while(presentedTextWidth < paddedViewportCellWidth){
			presentedText += " ";
			presentedTextWidth += 1;
		}

		return presentedText;
	}

	public void printViewportUpdates(CuboidAddress areaToUpdate) throws Exception {
		if(this.gameAreaCuboidAddress != null){
			Long numPrints = 0L;
			logger.info("Doing printViewportUpdates with viewport cuboid as " + this.gameAreaCuboidAddress + ".");
		
			RegionIteration regionIteration = new RegionIteration(areaToUpdate.getCanonicalLowerCoordinate(), areaToUpdate);
			do{
				Coordinate currentViewportCoordinate = regionIteration.getCurrentCoordinate();
				if(this.gameAreaCuboidAddress.containsCoordinate(currentViewportCoordinate)){
					boolean isPlayerPosition = false;
					if(
						this.getPlayerPosition() != null &&
						currentViewportCoordinate.getX().equals(this.getPlayerPosition().getX()) &&
						currentViewportCoordinate.getY().equals(this.getPlayerPosition().getY()) &&
						currentViewportCoordinate.getZ().equals(this.getPlayerPosition().getZ())
					){
						isPlayerPosition = true;
					}
					Long xOffsetScreen = currentViewportCoordinate.getX() - this.gameAreaCuboidAddress.getCanonicalLowerCoordinate().getX();
					Long yOffsetScreen = (this.gameAreaCuboidAddress.getWidthForIndex(2L) -1L) - (currentViewportCoordinate.getZ() - this.gameAreaCuboidAddress.getCanonicalLowerCoordinate().getZ());
					ViewportCell currentViewportCell = this.viewportCells[xOffsetScreen.intValue()][yOffsetScreen.intValue()];
					//logger.info("printViewportUpdates() for " + currentViewportCoordinate + " xOffsetScreen=" + xOffsetScreen + " yOffsetScreen=" + yOffsetScreen);

					if(currentViewportCell.hasBlockChangedFlags()){ /*  Updated cell */
						String padded = this.whitespacePadViewportCell(currentViewportCell.renderBlockCell());
						this.printTextInGameAreaXY(padded, xOffsetScreen, yOffsetScreen, isPlayerPosition);
						//this.printTextInGameAreaXY(this.whitespacePadViewportCell("U"), xOffsetScreen, yOffsetScreen, isPlayerPosition);
						numPrints++;
					}else if(currentViewportCell.hasPlayerMovementFlags()){ /*  Where player is or was */
						String padded = this.whitespacePadViewportCell(currentViewportCell.renderBlockCell());
						this.printTextInGameAreaXY(padded, xOffsetScreen, yOffsetScreen, isPlayerPosition);
						//this.printTextInGameAreaXY(this.whitespacePadViewportCell("P"), xOffsetScreen, yOffsetScreen, isPlayerPosition);
						numPrints++;
					}else if(currentViewportCell.hasPendingLoadFlags()){ /*  Loading cell */
						this.printTextInGameAreaXY(this.whitespacePadViewportCell("?"), xOffsetScreen, yOffsetScreen, isPlayerPosition);
						numPrints++;
					}else {
						//this.printTextInGameAreaXY(this.whitespacePadViewportCell("-"), xOffsetScreen, yOffsetScreen, isPlayerPosition);
					}
					currentViewportCell.clearNonLoadingFlags();
				}
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());
			System.out.print("\033[0;0H"); //  Move cursor to 0,0 after every print.
			logger.info("Just did this many prints: " + numPrints + ".");
			return;  //  Did we exit early because of more work items?
		}else{
			return;
		}
	}

	private Coordinate getPlayerPosition(){
		return this.playerPosition;
	}

	private PlayerInventory getPlayerInventory(){
		return this.playerInventory;
	}

	public ViewportWorkItem takeWorkItem() throws Exception {
		ViewportWorkItem workItem = this.workItemQueue.takeWorkItem();
		return workItem;
	}

	public void putWorkItem(ViewportWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}
}
