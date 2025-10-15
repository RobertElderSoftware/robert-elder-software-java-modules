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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.res.block.WorkItem;
import org.res.block.BlockSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class CraftingRecipeRenderableListItem extends RenderableListItem{

	private List<PlayerInventoryItemStack> inputItems;
	private List<PlayerInventoryItemStack> outputItems;

	public void render(UserInterfaceFrameThreadState frame, boolean isSelected, Coordinate placementOffset, ScreenLayer bottomLayer) throws Exception{
		int [] titleAnsiCodes = UserInterfaceFrameThreadState.getHelpDetailsTitleColors();

		GraphicsMode mode = frame.getBlockManagerThreadCollection().getGraphicsMode();
		boolean useAscii = mode.equals(GraphicsMode.ASCII);

		int [] bgColours = UserInterfaceFrameThreadState.getDefaultListItemBGColor(useAscii);
		int [] initialColours = UserInterfaceFrameThreadState.concatIntArrays(new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR}, bgColours);

		this.displayLayer.initializeInRegion(1, " ", initialColours, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, this.displayLayer.getWidth(), this.displayLayer.getHeight())), true, true);


		String text = getStackListDescription(inputItems, frame) + " " + getStackListDescription(outputItems, frame);

		Long currentLine = 1L;
		Long rightPadding = 1L;
		Long leftPadding = 1L;
		List<LinePrintingInstructionAtOffset> instructions = new ArrayList<LinePrintingInstructionAtOffset>();

		ColouredTextFragmentList recipeFragments = new ColouredTextFragmentList();
		recipeFragments.add(new ColouredTextFragment("RECIPE:", titleAnsiCodes));
		recipeFragments.add(new ColouredTextFragment(" " + text, initialColours));
		List<LinePrintingInstruction> keyDescriptionInstructions = frame.getLinePrintingInstructions(recipeFragments, leftPadding, rightPadding, true, false, (long)this.displayLayer.getWidth());
		instructions.addAll(frame.wrapLinePrintingInstructionsAtOffset(keyDescriptionInstructions, currentLine, 1L));
		currentLine += keyDescriptionInstructions.size() + 1;

		Long offsetToPrintAt = 1L;
		frame.executeLinePrintingInstructionsAtYOffsett(instructions, offsetToPrintAt, this.displayLayer);

		this.displayLayer.setPlacementOffset(placementOffset);
		bottomLayer.mergeDown(this.displayLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public String getStackListDescription(List<PlayerInventoryItemStack> itemStacks, UserInterfaceFrameThreadState frame) throws Exception{
		BlockSchema blockSchema = frame.getClientBlockModelContext().getBlockSchema();
		List<String> parts = new ArrayList<String>();
		for(PlayerInventoryItemStack itemStack : itemStacks){
			IndividualBlock block = itemStack.getBlock(blockSchema);
			parts.add(block.getClass().getSimpleName() + " (" + itemStack.getQuantity() + ")");
		}
		return String.join(", ", parts);
	}

	public CraftingRecipeRenderableListItem(List<PlayerInventoryItemStack> inputItems, List<PlayerInventoryItemStack> outputItems) throws Exception{
		this.inputItems = inputItems;
		this.outputItems = outputItems;
	}
}
