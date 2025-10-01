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

public class RenderableList<T extends RenderableListItem> {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private List<T> list = new ArrayList<T>();
	private CuboidAddress renderableArea = new CuboidAddress(
		new Coordinate(Arrays.asList(0L, 0L)),
		new Coordinate(Arrays.asList(0L, 0L))
	);

	private Long selectedIndex = 0L;
	private Long firstVisibleItemIndex = 0L;
	// The max possible number of whole list items that can be on screen in one column/row
	private Long maxNumVisibleListItems = 0L;
	// The max number of items that can appear in one of the adjacent lists:
	private Long maxItemsInAdjacentListColumn = 0L;

	private Long maxAdjacentLists;
	protected ScreenLayer listAreaLayer = new ScreenLayer();

	public boolean hasVerticalOrientation(UserInterfaceFrameThreadState frame) throws Exception{
		return (frame.getInnerFrameHeight() * 2L) > frame.getInnerFrameWidth();
	}

	public Long getListItemWidth(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			return frame.getInnerFrameWidth() / this.maxAdjacentLists;
		}else{
			return 10L;
		}
	}

	public Long getListItemHeight(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			return 7L;
		}else{
			return frame.getInnerFrameHeight() / this.maxAdjacentLists;
		}
	}

	public void onUpArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
			this.selectedIndex--;
			if(this.selectedIndex < 0L){
				this.selectedIndex = 0L;
			}
			if(this.selectedIndex < firstVisibleItemIndex){
				firstVisibleItemIndex = this.selectedIndex;
			}
		}else{
		}
		render(frame, bottomLayer);
	}

	public void onRightArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
		}else{
			this.selectedIndex++;
			if(this.selectedIndex >= list.size()){
				this.selectedIndex = (long)(list.size() -1);
			}else{
				if(this.selectedIndex == (firstVisibleItemIndex + maxNumVisibleListItems)){
					firstVisibleItemIndex++;
				}
			}
		}
		render(frame, bottomLayer);
	}

	public void onDownArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
			this.selectedIndex++;
			if(this.selectedIndex >= list.size()){
				this.selectedIndex = (long)(list.size() -1);
			}else{
				if(this.selectedIndex == (firstVisibleItemIndex + maxNumVisibleListItems)){
					firstVisibleItemIndex++;
				}
			}
		}else{
		}
		render(frame, bottomLayer);
	}

	public void onLeftArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
		}else{
			this.selectedIndex--;
			if(this.selectedIndex < 0L){
				this.selectedIndex = 0L;
			}
			if(this.selectedIndex < firstVisibleItemIndex){
				firstVisibleItemIndex = this.selectedIndex;
			}
		}
		render(frame, bottomLayer);
	}

	public RenderableList(Long maxAdjacentLists) throws Exception{
		this.maxAdjacentLists = maxAdjacentLists;
	}

	public void renderInVerticalOrientation(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer, int lowerBound, int upperBound, boolean isLastItem) throws Exception{
		Long fchw = frame.getFrameCharacterWidth();

		//  Space taken up by max number of whole list items that can fit on screen.
		Long onePaddedItemMeasurement = (getListItemHeight(frame) + 1);
		Long visibleItemsArea = maxNumVisibleListItems * onePaddedItemMeasurement;
		Long totalSpace = frame.getInnerFrameHeight();
		Long offset = (totalSpace - visibleItemsArea) + 1;
		Long adjustmentXOffset = 0L;
		Long adjustmentYOffset = (isLastItem ? offset : 0L);

		for(int adjacentListNumber = 0; adjacentListNumber < this.maxAdjacentLists; adjacentListNumber++){
			for(int i = lowerBound; i < upperBound; i++){
				int currentListItemIndex = i + (int)(adjacentListNumber * maxItemsInAdjacentListColumn);
				if(currentListItemIndex >= 0 && currentListItemIndex < this.list.size()){
					boolean isSelected = currentListItemIndex == this.selectedIndex;
					RenderableListItem listItem = list.get(currentListItemIndex);
					int visibleOffset = i - firstVisibleItemIndex.intValue();
					Long x = adjustmentXOffset + (adjacentListNumber * (getListItemWidth(frame) + 1));
					Long y = adjustmentYOffset + visibleOffset * (getListItemHeight(frame) + fchw);
					Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
					listItem.render(frame, isSelected, placementOffset, this.listAreaLayer);
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(getListItemWidth(frame).intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + getListItemHeight(frame), true, this.listAreaLayer);
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(getListItemHeight(frame).intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), x + getListItemWidth(frame), y, false, this.listAreaLayer);
				}
			}
		}
	}

	public void renderInHorizontalOrientation(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer, int lowerBound, int upperBound, boolean isLastItem) throws Exception{
		Long fchw = frame.getFrameCharacterWidth();

		//  Space taken up by max number of whole list items that can fit on screen.
		Long onePaddedItemMeasurement = (getListItemWidth(frame) + fchw);
		Long visibleItemsArea = maxNumVisibleListItems * onePaddedItemMeasurement;
		Long totalSpace = frame.getInnerFrameWidth();
		Long offset = (totalSpace - visibleItemsArea) + 1;
		Long adjustmentXOffset = (isLastItem ? offset : 0L);
		Long adjustmentYOffset = 0L;

		for(int i = lowerBound; i < upperBound; i++){
			if(i >= 0){
				boolean isSelected = i == this.selectedIndex;
				RenderableListItem listItem = list.get(i);
				int visibleOffset = i - firstVisibleItemIndex.intValue();
				Long x = adjustmentXOffset + (visibleOffset * (getListItemWidth(frame) + fchw));
				Long y = adjustmentYOffset + 0L;
				Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
				listItem.render(frame, isSelected, placementOffset, this.listAreaLayer);
				frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(getListItemHeight(frame).intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x + getListItemWidth(frame), y, false, this.listAreaLayer);
			}
		}
	}

	public void render(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		boolean hasVerticalOrientation = hasVerticalOrientation(frame);

		boolean hasFullItemList = list.size() > maxNumVisibleListItems.intValue();
		boolean isLastItem = (this.firstVisibleItemIndex.intValue() == (list.size() -maxNumVisibleListItems.intValue())) && hasFullItemList;
		int lowerBound = firstVisibleItemIndex.intValue() + (isLastItem ? -1 : 0);
		int upperBound = Math.min(list.size(), (int)(firstVisibleItemIndex + maxNumVisibleListItems + 1L));
		if(hasVerticalOrientation){
			renderInVerticalOrientation(frame, bottomLayer, lowerBound, upperBound, isLastItem);
		}else{
			renderInHorizontalOrientation(frame, bottomLayer, lowerBound, upperBound, isLastItem);
		}

		bottomLayer.mergeDown(this.listAreaLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public void updateRenderableArea(UserInterfaceFrameThreadState frame, CuboidAddress ca) throws Exception{

		boolean hasVerticalOrientation = hasVerticalOrientation(frame);
		Long fchw = frame.getFrameCharacterWidth();
		Long xOffset = fchw;
		Long yOffset = 1L;
		Coordinate placementOffset = new Coordinate(Arrays.asList(xOffset, yOffset));


		Long width = ca.getWidth();
		Long height = ca.getHeight();
		this.listAreaLayer = new ScreenLayer(placementOffset, ScreenLayer.makeDimensionsCA(0, 0, width.intValue(), height.intValue()));
		//  Initialize to an obvious pattern for testing.  
		this.listAreaLayer.initializeInRegion(1, "M", new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR, UserInterfaceFrameThreadState.YELLOW_BG_COLOR}, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, width.intValue(), height.intValue())), true, true);

		this.renderableArea = ca;
		for(int i = 0; i < list.size(); i++){
			RenderableListItem listItem = list.get(i);
			listItem.updateRenderableArea(
				new CuboidAddress(
					new Coordinate(Arrays.asList(0L, 0L)),
					new Coordinate(Arrays.asList(getListItemWidth(frame), getListItemHeight(frame)))
				)
			);
		}

		if(hasVerticalOrientation){
			maxNumVisibleListItems = height / (getListItemHeight(frame) + 1);
		}else{
			maxNumVisibleListItems = width / (getListItemWidth(frame) + fchw);
		}

		this.maxItemsInAdjacentListColumn = (long)Math.ceil((double)list.size() / (double)maxAdjacentLists);
	}

	public void addItem(T item){
		list.add(item);
	}
}
