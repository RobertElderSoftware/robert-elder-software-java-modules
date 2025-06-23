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

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.Collectors;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.ByteArrayOutputStream;

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

public class InventoryInterfaceThreadState extends UserInterfaceFrameThreadState {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private PlayerInventory playerInventory = null;
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private Long selectedInventoryItemIndex = null;

	private ClientBlockModelContext clientBlockModelContext;

	public InventoryInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}
	
	public void onSelectionChange(Long newSelection) throws Exception{
		this.selectedInventoryItemIndex = newSelection;

		PlayerInventory inventory = this.getPlayerInventory();
		List<PlayerInventoryItemStack> itemStacks = inventory.getInventoryItemStackList();
		PlayerInventoryItemStack currentStack = itemStacks.get(this.selectedInventoryItemIndex.intValue());
		IndividualBlock blockFromStack = currentStack.getBlock(this.blockManagerThreadCollection.getBlockSchema());
		this.clientBlockModelContext.putWorkItem(new InventoryItemSelectionChangeWorkItem(this.clientBlockModelContext, blockFromStack.getClass()), WorkItemPriority.PRIORITY_LOW);
	}

	public Long getNumInventoryItems() throws Exception{
		PlayerInventory inventory = this.getPlayerInventory();
		if(inventory == null){
			return 0L;
		}else{
			List<PlayerInventoryItemStack> itemStacks = inventory.getInventoryItemStackList();
			if(itemStacks.size() > 0){
				return (long)itemStacks.size();
			}else{
				return 0L;
			}
		}
	}

	public Long getSelectedInventoryItemIndex() throws Exception{
		if(getNumInventoryItems() != null && getNumInventoryItems() > 0){
			if(this.selectedInventoryItemIndex == null){
				this.onSelectionChange(0L);
			}
			return this.selectedInventoryItemIndex;
		}else{
			return null;
		}
	}

	public void onInventoryItemSelectionChange(Long selectionDiff) throws Exception{
		Long numInventoryItems = this.getNumInventoryItems();
		if(numInventoryItems != null && numInventoryItems > 0){
			Long initialIndex = this.selectedInventoryItemIndex == null ? 0L : this.selectedInventoryItemIndex;
			Long newIndex = (((initialIndex + selectionDiff) % numInventoryItems) + numInventoryItems) % numInventoryItems;
			this.onSelectionChange(newIndex);
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.onInventoryItemSelectionChange(-1L);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRightArrowKey){
			this.onInventoryItemSelectionChange(this.getMaxItemsInColumn());
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			this.onInventoryItemSelectionChange(1L);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceLeftArrowKey){
			this.onInventoryItemSelectionChange(-this.getMaxItemsInColumn());
		}else{
			logger.info("InventoryInterfaceThreadState, discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
		this.onRenderFrame(true, true);
		this.onFinalizeFrame();
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		logger.info("Discarding keyboard input: " + new String(characters, "UTF-8"));
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{
		this.clearFrame();
		this.render();
	}

	public void render() throws Exception{
		this.reprintFrame();
	}

	public void onPlayerInventoryChange(PlayerInventory playerInventory) throws Exception{
		this.playerInventory = playerInventory;
		if(this.getFrameWidth() != null && this.getFrameHeight() != null){
			this.reprintFrame();
		}
		this.onFinalizeFrame();
	}

	public ColouredTextFragmentList makeInventoryItemText(PlayerInventoryItemStack stack, int itemIndex) throws Exception{
		IndividualBlock blockFromStack = stack.getBlock(this.blockManagerThreadCollection.getBlockSchema());

		GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
		String blockPresentation = BlockSkins.getPresentation(blockFromStack.getClass(), mode.equals(GraphicsMode.ASCII));
		String prefixCharacter = mode.equals(GraphicsMode.ASCII) ? CharacterConstants.INVENTORY_ARROW_ASCII : CharacterConstants.INVENTORY_ARROW_EMOJI;

		int selectedIndex = this.getSelectedInventoryItemIndex().intValue();
		String prefix = itemIndex == selectedIndex ? prefixCharacter : "";
		String paddedPrefix = this.whitespacePad(prefix, 3L);

		String inventoryItemText = this.whitespacePad(blockPresentation, this.getMapAreaCellWidth()) + "  (" + stack.getQuantity().toString() + ") " + blockFromStack.getClass().getSimpleName() + " ";
		return new ColouredTextFragmentList(Arrays.asList(
			new ColouredTextFragment(paddedPrefix, new int[] {DEFAULT_TEXT_BG_COLOR, GREEN_FG_COLOR}),
			new ColouredTextFragment(inventoryItemText, new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR})
		), true);
	}

	public List<List<ColouredTextFragmentList>> divideIntoColumns(List<ColouredTextFragmentList> inventoryItemTextLists, Long maxItemsInColumn){
		List<List<ColouredTextFragmentList>> rtn = new ArrayList<List<ColouredTextFragmentList>>();
		int i = 0; 
		List<ColouredTextFragmentList> currentList = new ArrayList<ColouredTextFragmentList>();
		for(ColouredTextFragmentList l : inventoryItemTextLists){
			if(i % maxItemsInColumn == 0){
				if(currentList.size() > 0){
					rtn.add(currentList);
				}
				currentList = new ArrayList<ColouredTextFragmentList>();
			}
			currentList.add(l);
			i++;

		}
		if(currentList.size() > 0){
			rtn.add(currentList);
		}
		return rtn;
	}

	public void printInventoryColumn(List<ColouredTextFragmentList> column, Long offset) throws Exception{

		Long yPaddingTop = this.getInnerFrameHeight() < 2L ? 1L : 2L;
		
		if(this.getInnerFrameWidth() > 0L){
			List<LinePrintingInstructionAtOffset> instructionsAtOffset = new ArrayList<LinePrintingInstructionAtOffset>();
			for(int i = 0; i < column.size(); i++){
				ColouredTextFragmentList text = column.get(i);
				Long yOffset = yPaddingTop + (i * 2L);
				//this.printTextAtScreenXY(text, 2L + offset, yOffset, true);

				Long xOffset = 2L + offset;
				Long paddingLeft = xOffset;
				Long paddingRight = this.getInnerFrameWidth() - (xOffset + getInventoryColumnWidth()) + 2L;
				paddingRight = Math.max(2L, paddingRight);
				if((paddingLeft + 1L) < this.getInnerFrameWidth()){ // Don't print beyond the edge of the frame.
					List<LinePrintingInstruction> instructions = this.getLinePrintingInstructions(text, paddingLeft, paddingRight, true, false, this.getInnerFrameWidth());
					instructionsAtOffset.addAll(this.wrapLinePrintingInstructionsAtOffset(instructions, yOffset, 1L));
				}
			}
			for(LinePrintingInstructionAtOffset instruction : instructionsAtOffset){
				Long lineOffset = instruction.getOffsetY();
				if((lineOffset >= 1L) && (lineOffset <= this.getFrameHeight() -2L)){
					this.executeLinePrintingInstructionsAtYOffset(Arrays.asList(instruction.getLinePrintingInstruction()), lineOffset);
				}
			}
		}
	}

	public Long getInventoryColumnWidth(){
		return 30L;
	}

	public void reprintFrame() throws Exception {
		this.drawBorders();

		this.printTextAtScreenXY(new ColouredTextFragment("- Inventory -", new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR}), 5L, 0L, true);

		List<ColouredTextFragmentList> inventoryItemTextLists = new ArrayList<ColouredTextFragmentList>();
		PlayerInventory inventory = this.getPlayerInventory();
		if(inventory != null){
			List<PlayerInventoryItemStack> itemStacks = inventory.getInventoryItemStackList();
			if(itemStacks.size() == 0){
				inventoryItemTextLists.add(new ColouredTextFragmentList(new ColouredTextFragment("Empty.", new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR})));
			}else{
				for(int i = 0; i < itemStacks.size(); i++){
					PlayerInventoryItemStack stack = itemStacks.get(i);
					inventoryItemTextLists.add(makeInventoryItemText(stack, i));
				}
			}
		}

		List<List<ColouredTextFragmentList>> columns = this.divideIntoColumns(inventoryItemTextLists, getMaxItemsInColumn());
		for(int i = 0; i < columns.size(); i++){
			this.printInventoryColumn(columns.get(i), getInventoryColumnWidth() * i);
		}
	}

	private Long getMaxItemsInColumn()throws Exception{
		Long m = this.getUsableFrameHeight() / 2L; // Two rows per item
		return m < 1L ? 1L : m;
	}

	private Long getUsableFrameHeight() throws Exception{
		return this.getInnerFrameHeight();
	}

	private PlayerInventory getPlayerInventory()throws Exception{
		return this.playerInventory;
	}

	public UIWorkItem takeWorkItem() throws Exception {
		UIWorkItem workItem = this.workItemQueue.takeWorkItem();
		return workItem;
	}

	public void putWorkItem(InventoryInterfaceWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public void putWorkItem(UIWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}
}
