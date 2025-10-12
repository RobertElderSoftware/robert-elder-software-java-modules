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

	private Long verticalScrollBarWidth = 2L;
	private Long horizontalScrollBarHeight = 1L;

	public boolean hasVerticalOrientation(UserInterfaceFrameThreadState frame) throws Exception{
		return (frame.getInnerFrameHeight() * 2L) > frame.getInnerFrameWidth();
	}

	public Long getListItemWidth(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			return (frame.getInnerFrameWidth() - verticalScrollBarWidth - (this.maxAdjacentLists - 1)) / this.maxAdjacentLists;
		}else{
			return 10L;
		}
	}

	public Long getListItemHeight(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			return 7L;
		}else{
			return (frame.getInnerFrameHeight() - horizontalScrollBarHeight - (this.maxAdjacentLists - 1)) / this.maxAdjacentLists;
		}
	}

	public void onUpArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
			Long moduloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
			if(moduloOffset == 0L){
				//  Do nothing, at top of list area.
			}else{
				this.selectedIndex--;
			}
			Long newModuloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
			if(newModuloOffset < firstVisibleItemIndex){
				firstVisibleItemIndex = newModuloOffset;
			}
		}else{
			if(this.selectedIndex - maxItemsInAdjacentListColumn >= 0){
				this.selectedIndex -= this.maxItemsInAdjacentListColumn;
			}
		}
		render(frame, bottomLayer);
	}

	public void onRightArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
			if(this.selectedIndex + maxItemsInAdjacentListColumn < list.size()){
				this.selectedIndex += this.maxItemsInAdjacentListColumn;
			}
		}else{
			Long moduloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
			if(
				//  Bottom boundary for list area
				(moduloOffset == (this.maxItemsInAdjacentListColumn-1)) ||
				//  Or, last element in list which may not be against bottom boundary
				this.selectedIndex >= (list.size() -1)
			){
				//  Do nothing.
			}else{
				this.selectedIndex++;
				Long newModuloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
				if(newModuloOffset == (firstVisibleItemIndex + maxNumVisibleListItems)){
					firstVisibleItemIndex++;
				}
			}
		}
		render(frame, bottomLayer);
	}

	public void onDownArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
			Long moduloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
			if(
				//  Bottom boundary for list area
				(moduloOffset == (this.maxItemsInAdjacentListColumn-1)) ||
				//  Or, last element in list which may not be against bottom boundary
				this.selectedIndex >= (list.size() -1)
			){
				//  Do nothing.
			}else{
				this.selectedIndex++;
				Long newModuloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
				if(newModuloOffset == (firstVisibleItemIndex + maxNumVisibleListItems)){
					firstVisibleItemIndex++;
				}
			}
		}else{
			if(this.selectedIndex + maxItemsInAdjacentListColumn < list.size()){
				this.selectedIndex += this.maxItemsInAdjacentListColumn;
			}
		}
		render(frame, bottomLayer);
	}

	public void onLeftArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(hasVerticalOrientation(frame)){
			if(this.selectedIndex - maxItemsInAdjacentListColumn >= 0){
				this.selectedIndex -= this.maxItemsInAdjacentListColumn;
			}
		}else{
			Long moduloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
			if(moduloOffset == 0L){
				//  Do nothing, at top of list area.
			}else{
				this.selectedIndex--;
			}
			Long newModuloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
			if(newModuloOffset < firstVisibleItemIndex){
				firstVisibleItemIndex = newModuloOffset;
			}
		}
		render(frame, bottomLayer);
	}

	public RenderableList(Long maxAdjacentLists) throws Exception{
		this.maxAdjacentLists = maxAdjacentLists;
	}

	public void renderEmptyItem(UserInterfaceFrameThreadState frame, Long x, Long y, Long itemWidth, Long itemHeight) throws Exception{

		for(long l = 0; l < itemHeight; l++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(itemWidth.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + l, true, this.listAreaLayer);
		}
	}

	public ColouredTextFragmentList makeScrollTextFragmentList(Long textColumnHeight) throws Exception{
		ColouredTextFragmentList rtn = new ColouredTextFragmentList();

		Long limitIndex = Math.max(0L, Math.min(((long)(list.size()-1)), maxItemsInAdjacentListColumn -1L));

		Long firstListIndex = 0L;
		Long firstVisibleListIndex = firstVisibleItemIndex;
		Long lastVisibleListIndex = Math.min(firstVisibleItemIndex + maxNumVisibleListItems - 1L, limitIndex);
		Long endListIndex = limitIndex;

		Long firstColumnIndex = 0L;
		Long firstVisibleColumnIndex = (long)Math.floor(((double)firstVisibleListIndex / (double)limitIndex) * (double)textColumnHeight);
		Long lastVisibleColumnIndex = (long)Math.ceil(((double)lastVisibleListIndex / (double)limitIndex) * (double)textColumnHeight);
		Long endColumnIndex = (long)Math.ceil(((double)endListIndex / (double)limitIndex) * (double)textColumnHeight);

		//  Calculate coloured Areas:
		Long beforeScrollBar = firstVisibleColumnIndex - firstColumnIndex;
		Long inScrollBar = lastVisibleColumnIndex - firstVisibleColumnIndex;
		Long afterScrollBar = endColumnIndex - lastVisibleColumnIndex;

		rtn.add(
			new ColouredTextFragment(" ".repeat(beforeScrollBar.intValue()), new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR})
		);

		rtn.add(
			new ColouredTextFragment(" ".repeat(inScrollBar.intValue()), new int [] {UserInterfaceFrameThreadState.RED_BG_COLOR})
		);

		rtn.add(
			new ColouredTextFragment(" ".repeat(afterScrollBar.intValue()), new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR})
		);

		return rtn;
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

				boolean isSelected = currentListItemIndex == this.selectedIndex;
				int visibleOffset = i - firstVisibleItemIndex.intValue();
				Long x = adjustmentXOffset + (adjacentListNumber * (getListItemWidth(frame) + 1));
				Long y = adjustmentYOffset + visibleOffset * (getListItemHeight(frame) + 1);
				Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
				if(currentListItemIndex >= 0 && currentListItemIndex < this.list.size()){
					RenderableListItem listItem = list.get(currentListItemIndex);
					listItem.render(frame, isSelected, placementOffset, this.listAreaLayer);
				}else{
					this.renderEmptyItem(
						frame,
						x,
						y,
						getListItemWidth(frame),
						getListItemHeight(frame)
					);
				}
				frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(getListItemWidth(frame).intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + getListItemHeight(frame), true, this.listAreaLayer);
				if(adjacentListNumber < (this.maxAdjacentLists-1)){
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(getListItemHeight(frame).intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), x + getListItemWidth(frame), y, false, this.listAreaLayer);
				}
			}
		}
		Long areaUsedByList = (getListItemWidth(frame) * this.maxAdjacentLists) + (this.maxAdjacentLists -1L);
		Long initializedArea = areaUsedByList + verticalScrollBarWidth;
		Long uninitializedAreaWidth = frame.getInnerFrameWidth() - initializedArea;

	
		//  Scroll bar
		for(long i = 0; i < verticalScrollBarWidth; i++){
			frame.printTextAtScreenXY(makeScrollTextFragmentList(frame.getInnerFrameHeight() + 1L), areaUsedByList + i, 0L, false, this.listAreaLayer);
		}

		//  Initialize any empty area on right edge:
		for(long i = 0; i < uninitializedAreaWidth; i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(frame.getInnerFrameHeight().intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), initializedArea + i, 0L, false, this.listAreaLayer);
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

		for(int adjacentListNumber = 0; adjacentListNumber < this.maxAdjacentLists; adjacentListNumber++){
			for(int i = lowerBound; i < upperBound; i++){
				int currentListItemIndex = i + (int)(adjacentListNumber * maxItemsInAdjacentListColumn);
				boolean isSelected = currentListItemIndex == this.selectedIndex;
				int visibleOffset = i - firstVisibleItemIndex.intValue();
				Long x = adjustmentXOffset + (visibleOffset * (getListItemWidth(frame) + fchw));
				Long y = adjustmentYOffset + (adjacentListNumber * (getListItemHeight(frame) + 1));
				Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
				if(
					//  Outside lower bound of entire list
					currentListItemIndex < 0 ||
					//  Outside lower bound of entire list
					currentListItemIndex >= this.list.size()
				){
					this.renderEmptyItem(
						frame,
						x,
						y,
						getListItemWidth(frame),
						getListItemHeight(frame)
					);
				}else{
					RenderableListItem listItem = list.get(currentListItemIndex);
					listItem.render(frame, isSelected, placementOffset, this.listAreaLayer);
				}
				frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(getListItemHeight(frame).intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x + getListItemWidth(frame), y, false, this.listAreaLayer);
				if(adjacentListNumber < (this.maxAdjacentLists-1)){
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(getListItemWidth(frame).intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + getListItemHeight(frame), true, this.listAreaLayer);
				}
			}
		}
		Long areaUsedByList = (getListItemHeight(frame) * this.maxAdjacentLists) + (this.maxAdjacentLists -1L);
		Long initializedArea = areaUsedByList + horizontalScrollBarHeight;
		Long uninitializedAreaHeight = frame.getInnerFrameHeight() - initializedArea;

		//  Scroll bar
		for(long i = 0; i < horizontalScrollBarHeight; i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(frame.getInnerFrameWidth().intValue() + 1), new int [] {UserInterfaceFrameThreadState.BLUE_BG_COLOR}), 0L, areaUsedByList + i, true, this.listAreaLayer);
		}

		//  Initialize any empty area on bottom edge:
		for(long i = 0; i < uninitializedAreaHeight; i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(frame.getInnerFrameWidth().intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), 0L, initializedArea + i, true, this.listAreaLayer);
		}
	}

	public void render(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		boolean hasVerticalOrientation = hasVerticalOrientation(frame);

		boolean hasFullItemList = maxItemsInAdjacentListColumn > maxNumVisibleListItems.intValue();
		boolean isLastItem = (this.firstVisibleItemIndex.intValue() == (maxItemsInAdjacentListColumn -maxNumVisibleListItems.intValue())) && hasFullItemList;
		//  If there are a small number of items in list, don't show the partially
		//  obscured item near the end of the list.  If there are many items, do show it:
		Long nextItemPeekAdjustment = maxItemsInAdjacentListColumn > maxNumVisibleListItems ? 1L : 0L;
		int lowerBound = firstVisibleItemIndex.intValue() + (isLastItem ? -1 : 0);
		int upperBound = Math.min(list.size(), (int)(firstVisibleItemIndex + maxNumVisibleListItems + nextItemPeekAdjustment));
		if(hasVerticalOrientation){
			renderInVerticalOrientation(frame, bottomLayer, lowerBound, upperBound, isLastItem);
		}else{
			renderInHorizontalOrientation(frame, bottomLayer, lowerBound, upperBound, isLastItem);
		}

		bottomLayer.mergeDown(this.listAreaLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public void updateRenderableArea(UserInterfaceFrameThreadState frame, CuboidAddress ca) throws Exception{

		this.renderableArea = ca;
		this.recalculateConstants(frame);

		Long fchw = frame.getFrameCharacterWidth();
		Long xOffset = fchw;
		Long yOffset = 1L;
		Coordinate placementOffset = new Coordinate(Arrays.asList(xOffset, yOffset));

		this.listAreaLayer = new ScreenLayer(placementOffset, ScreenLayer.makeDimensionsCA(0, 0, (int)ca.getWidth(), (int)ca.getHeight()));
		//  Initialize to an obvious pattern for testing.  
		this.listAreaLayer.initializeInRegion(1, "M", new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR, UserInterfaceFrameThreadState.YELLOW_BG_COLOR}, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, (int)ca.getWidth(), (int)ca.getHeight())), true, true);

		for(int i = 0; i < list.size(); i++){
			RenderableListItem listItem = list.get(i);
			listItem.updateRenderableArea(
				new CuboidAddress(
					new Coordinate(Arrays.asList(0L, 0L)),
					new Coordinate(Arrays.asList(getListItemWidth(frame), getListItemHeight(frame)))
				)
			);
		}
	}

	public void recalculateConstants(UserInterfaceFrameThreadState frame) throws Exception{
		Long fchw = frame.getFrameCharacterWidth();
		Long maxVisibleFloor;
		Long maxVisibleCeil;
		if(hasVerticalOrientation(frame)){
			maxVisibleFloor = (long)Math.floor((double)this.renderableArea.getHeight() / (double)(getListItemHeight(frame) + 1));
			maxVisibleCeil = (long)Math.ceil((double)this.renderableArea.getHeight() / (double)(getListItemHeight(frame) + 1));
		}else{
			maxVisibleFloor = (long)Math.floor((double)this.renderableArea.getWidth() / (double)(getListItemWidth(frame) + fchw));
			maxVisibleCeil = (long)Math.ceil((double)this.renderableArea.getWidth() / (double)(getListItemWidth(frame) + fchw));
		}

		this.maxNumVisibleListItems = maxVisibleFloor;

		Long dividedListColumnLength = (long)Math.ceil((double)list.size() / (double)maxAdjacentLists);
		this.maxItemsInAdjacentListColumn = Math.max(maxVisibleFloor, dividedListColumnLength);

		//  Handle situations where frame size changes and visible area moves beyond
		//  end of list:
		Long moduloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
		Long lowerScreenIndex = moduloOffset - this.firstVisibleItemIndex;
		Long upperScreenIndex = this.firstVisibleItemIndex + this.maxNumVisibleListItems;
		Long upperVisibleModuloIndex = this.firstVisibleItemIndex + this.maxNumVisibleListItems;
		if(
			//  Selected item is outside lower end of what's on screen
			(lowerScreenIndex < 0L) ||
			//  Selected item is outside upper end of what's on screen
			(moduloOffset >= upperScreenIndex) ||
			//  Showing items beyond end of list column:
			(upperVisibleModuloIndex > this.maxItemsInAdjacentListColumn)
		){
			Long restrictedOffset = (moduloOffset - (this.maxNumVisibleListItems - 1L));
			this.firstVisibleItemIndex = Math.max(0L, restrictedOffset);
		}
	}

	public void addItem(T item){
		list.add(item);
	}
}
