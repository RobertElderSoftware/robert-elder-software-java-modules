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

public class CraftingInterfaceThreadState extends UserInterfaceFrameThreadState {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected BlockManagerThreadCollection blockManagerThreadCollection = null;

	private RenderableList<CraftingRecipeRenderableListItem> recipeList = new RenderableList<CraftingRecipeRenderableListItem>(1L, 10L, 2L);
	private ClientBlockModelContext clientBlockModelContext;

	public CraftingInterfaceThreadState(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext) throws Exception {
		super(blockManagerThreadCollection, clientBlockModelContext, new int [] {ConsoleWriterThreadState.BUFFER_INDEX_DEFAULT}, new ScreenLayerMergeType [] {ScreenLayerMergeType.PREFER_BOTTOM_LAYER});
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientBlockModelContext = clientBlockModelContext;

		/*
		byte [] rockData = this.getBlockDataForClass(Rock.class);
		byte [] ironPickData = this.getBlockDataForClass(IronPick.class);
		byte [] stonePickData = this.getBlockDataForClass(StonePick.class);
		byte [] woodenPickData = this.getBlockDataForClass(WoodenPick.class);
		byte [] ironOxideData = this.getBlockDataForClass(IronOxide.class);
		byte [] woodenBlockData = this.getBlockDataForClass(WoodenBlock.class);
		byte [] metallicIronBlockData = this.getBlockDataForClass(MetallicIron.class);
		*/

		this.recipeList.addItem(
			new CraftingRecipeRenderableListItem(
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(MetallicIron.class), 3L),
					new PlayerInventoryItemStack(gbd(WoodenBlock.class), 2L)
				}),
				Arrays.asList(new PlayerInventoryItemStack [] {
					new PlayerInventoryItemStack(gbd(IronPick.class), 1L)

				})
			)
		);
	}

	private byte [] gbd(Class<?> c) throws Exception {
		return clientBlockModelContext.getBlockDataForClass(c);
	}

	public void onKeyboardInput(byte [] characters) throws Exception {
		logger.info("Crafting frame, discarding keyboard input: " + new String(characters, "UTF-8"));
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
		

		this.recipeList.updateRenderableArea(
			this,
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				new Coordinate(Arrays.asList(this.getInnerFrameWidth(), this.getInnerFrameHeight()))
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
