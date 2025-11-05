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

public class CraftingInterfaceThreadState extends UserInterfaceFrameThreadState implements RenderableListContainer{

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private RenderableList<CraftingRecipeRenderableListItem> recipeList;
	private ClientBlockModelContext clientBlockModelContext;

	public CraftingInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT}, new ScreenLayerMergeType [] {ScreenLayerMergeType.PREFER_BOTTOM_LAYER});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;

	}

	public void init() throws Exception{
		this.recipeList = new RenderableList<CraftingRecipeRenderableListItem>(this, 1L, 1L, 10L, 2L, "There are no crafting recipes.");
		this.addRecipeItems();

		/*
		GetCurrentCraftingRecipeSelectionWorkItemResult result = (GetCurrentCraftingRecipeSelectionWorkItemResult)this.clientBlockModelContext.putBlockingWorkItem(
			new GetCurrentCraftingRecipeSelectionWorkItem(this.clientBlockModelContext),
			WorkItemPriority.PRIORITY_LOW
		);

		CraftingRecipe currentlySelectedRecipe = result.getCraftingRecipe();
		*/
	}

	public void onSelectionChange(Long selectedIndex) throws Exception{
		CraftingRecipeRenderableListItem recipe = this.recipeList.getListItems().get(selectedIndex.intValue());
		this.clientBlockModelContext.putWorkItem(new CraftingRecipeChangeWorkItem(this.clientBlockModelContext, recipe.getCraftingRecipe()), WorkItemPriority.PRIORITY_LOW);
	}

	private void addRecipeItems() throws Exception {
		this.recipeList.addItem(
			new CraftingRecipeRenderableListItem(
				new CraftingRecipe(
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(WoodenBlock.class), 5L)
					}),
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(WoodenPick.class), 1L)

					})
				)
			)
		);

		this.recipeList.addItem(
			new CraftingRecipeRenderableListItem(
				new CraftingRecipe(
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(Rock.class), 3L),
						new PlayerInventoryItemStack(gbd(WoodenBlock.class), 2L)
					}),
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(StonePick.class), 1L)

					})
				)
			)
		);

		this.recipeList.addItem(
			new CraftingRecipeRenderableListItem(
				new CraftingRecipe(
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(IronOxide.class), 5L),
						new PlayerInventoryItemStack(gbd(WoodenBlock.class), 5L)
					}),
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(MetallicIron.class), 1L)

					})
				)
			)
		);

		this.recipeList.addItem(
			new CraftingRecipeRenderableListItem(
				new CraftingRecipe(
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(MetallicIron.class), 3L),
						new PlayerInventoryItemStack(gbd(WoodenBlock.class), 2L)
					}),
					Arrays.asList(new PlayerInventoryItemStack [] {
						new PlayerInventoryItemStack(gbd(IronPick.class), 1L)

					})
				)
			)
		);
	}

	private byte [] gbd(Class<?> c) throws Exception {
		return clientBlockModelContext.getBlockDataForClass(c);
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		UserInteractionConfig ki = this.blockManagerThreadCollection.getUserInteractionConfig();
		for(byte b : characters){
			String actionString = new String(new byte [] {b}, "UTF-8");
			UserInterfaceActionType action = ki.getKeyboardActionFromString(actionString);

			if(action == null){
				logger.info("Ignoring " + b);
			}else{
				switch(action){
					case ACTION_CRAFTING:{
						this.clientBlockModelContext.putWorkItem(new TryCraftingWorkItem(this.clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
						break;
					}case ACTION_ENTER:{
						//  The clientBlockModelContext should already know what the currently selected recipe is:
						this.clientBlockModelContext.putWorkItem(new TryCraftingWorkItem(this.clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
						break;
					}default:{
						logger.info("Crafting frame, discarding keyboard input: " + new String(characters, "UTF-8"));
					}
				}
			}
		}
	}

	public void onAnsiEscapeSequence(AnsiEscapeSequence ansiEscapeSequence) throws Exception{
		ScreenLayer bottomLayer = this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT];
		if(ansiEscapeSequence instanceof AnsiEscapeSequenceUpArrowKey){
			this.recipeList.onUpArrowPressed(this, bottomLayer);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceRightArrowKey){
			this.recipeList.onRightArrowPressed(this, bottomLayer);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceDownArrowKey){
			this.recipeList.onDownArrowPressed(this, bottomLayer);
		}else if(ansiEscapeSequence instanceof AnsiEscapeSequenceLeftArrowKey){
			this.recipeList.onLeftArrowPressed(this, bottomLayer);
		}else{
			logger.info("CraftingInterfaceThreadState, discarding unknown ansi escape sequence of type: " + ansiEscapeSequence.getClass().getName());
		}
		this.onFinalizeFrame();
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public void onRenderFrame(boolean hasThisFrameDimensionsChanged, boolean hasOtherFrameDimensionsChanged) throws Exception{
		int [] titleAnsiCodes = UserInterfaceFrameThreadState.getHelpDetailsTitleColors();

		ColouredTextFragmentList topTitlePart = new ColouredTextFragmentList();
		topTitlePart.add(new ColouredTextFragment("Crafting Recipes", titleAnsiCodes));

		List<LinePrintingInstruction> titleInstructions = this.getLinePrintingInstructions(topTitlePart, 1L, 1L, false, false, this.getInnerFrameWidth());

		this.executeLinePrintingInstructionsAtYOffset(titleInstructions, 2L);

		Long linesOnTop = 3L;
		Long sidePadding = 0L;
		Long fchw = this.getFrameCharacterWidth();
		Long x1 = fchw + sidePadding;
		Long y1 = 1L + linesOnTop;
		Long x2 = x1 + this.getInnerFrameWidth() - 2L * sidePadding;
		Long y2 = y1 + this.getInnerFrameHeight() - linesOnTop;
		Coordinate topLeftCorner = new Coordinate(Arrays.asList(x1, y1));
		Coordinate bottomRightCorner = new Coordinate(Arrays.asList(x2, y2));

		this.recipeList.updateRenderableArea(
			this,
			new CuboidAddress(
				topLeftCorner,
				bottomRightCorner
			)
		);

		this.recipeList.render(this, this.bufferedScreenLayers[ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT]);
		this.drawBorders();
	}

	public UIWorkItem takeWorkItem() throws Exception {
		UIWorkItem workItem = this.workItemQueue.takeWorkItem();
		return workItem;
	}

	public void putWorkItem(UIWorkItem workItem, WorkItemPriority priority) throws Exception{
		this.workItemQueue.putWorkItem(workItem, priority);
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}
}
