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

	private ClientBlockModelContext clientBlockModelContext;

	public InventoryInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext);
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;
	}
	
	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		logger.info("Inventory frame, discarding ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		logger.info("Discarding keyboard input: " + new String(characters, "UTF-8"));
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void onFrameDimensionsChanged() throws Exception{
		this.render();
	}

	public void onFrameFocusChanged() throws Exception{
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
	}

	public void reprintFrame() throws Exception {
		this.drawBorders(true);

		this.printTextAtScreenXY(new ColouredTextFragment("- Inventory -", new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR}), 10L, 0L, true);
		PlayerInventory inventory = this.getPlayerInventory();
		if(inventory != null){
			List<PlayerInventoryItemStack> itemStacks = inventory.getInventoryItemStackList();
			logger.info("Here is the inventory: " + inventory.asJsonString() + ".");
			Long inventoryItemsXOffset = 2L;
			if(itemStacks.size() == 0){
				this.printTextAtScreenXY(new ColouredTextFragment("Empty.", new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR}), inventoryItemsXOffset, 2L, true);
			}else{
				for(int i = 0; i < itemStacks.size(); i++){
					PlayerInventoryItemStack stack = itemStacks.get(i);
					IndividualBlock blockFromStack = stack.getBlock(this.blockManagerThreadCollection.getBlockSchema());
					int maxItemsInColumn = 4;
					int xOffset = (i / maxItemsInColumn) * 30;
					int yOffset = (i % maxItemsInColumn) * 2;

					GraphicsMode mode = blockManagerThreadCollection.getGraphicsMode();
					String blockPresentation = BlockSkins.getPresentation(blockFromStack.getClass(), mode.equals(GraphicsMode.ASCII));

					Long printTextX = inventoryItemsXOffset + xOffset;
					Long printTextY = (long)(2 + yOffset);
					String inventoryItemText = this.whitespacePadMapAreaCell(blockPresentation) + "  (" + stack.getQuantity().toString() + ") " + blockFromStack.getClass().getSimpleName() + " ";
					this.printTextAtScreenXY(new ColouredTextFragment(inventoryItemText, new int[] {DEFAULT_TEXT_BG_COLOR, DEFAULT_TEXT_FG_COLOR}), printTextX, printTextY, true);
				}
			}
		}
	}

	private PlayerInventory getPlayerInventory(){
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
