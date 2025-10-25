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

	private static final Long LINE_HEIGHT = 1L;
	private static final String VERTICAL_SCROLL_BAR_CHARACTER = CharacterConstants.VERTICAL_LINE;

	private Long defaultWidth;
	private Long defaultHeight;
	private Long selectedIndex = 0L;
	private Long xColumnOffset = 0L;
	private Long yColumnOffset = 0L;
	//private Long firstVisibleItemIndex = 0L;
	// The max possible number of whole list items that can be on screen in one column/row
	private Long maxNumVisibleListItems = 0L;
	// The max number of items that can appear in one of the adjacent lists:
	private Long maxItemsInAdjacentListColumn = 0L;
	private boolean hasScrollBar = false;

	private Long maxAdjacentLists;
	protected ScreenLayer listAreaLayer = new ScreenLayer();

	private double aspectRatio;
	private Long listItemHeight;
	private Long listItemWidth;

	public RenderableList(Long maxAdjacentLists, Long defaultWidth, Long defaultHeight) throws Exception{
		this.maxAdjacentLists = maxAdjacentLists;
		this.defaultWidth = defaultWidth;
		this.defaultHeight = defaultHeight;
		this.aspectRatio = (double)this.defaultWidth / (double)this.defaultHeight;
	}

	public boolean hasVerticalOrientation(UserInterfaceFrameThreadState frame) throws Exception{
		return (frame.getInnerFrameHeight() * 2L) > frame.getInnerFrameWidth();
	}

	public boolean hasScrollBar(UserInterfaceFrameThreadState frame) throws Exception{
		return this.hasScrollBar;
	}

	public Long spw(UserInterfaceFrameThreadState frame) throws Exception{
		return frame.textWidth(CharacterConstants.SPACE);
	}

	public Long lnh(){
		return RenderableList.LINE_HEIGHT;
	}

	public Long getScrollBarCrossSection(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			if(hasScrollBar(frame)){
				return 2L * frame.textWidth(RenderableList.VERTICAL_SCROLL_BAR_CHARACTER);
			}else{
				return 0L;
			}
		}else{
			if(hasScrollBar(frame)){
				return lnh();
			}else{
				return 0L;
			}
		}
	}

	private Long calculateListItemWidth(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			return Math.max(0L, (frame.getInnerFrameWidth() - getScrollBarCrossSection(frame) - (this.maxAdjacentLists - spw(frame))) / this.maxAdjacentLists);
		}else{
			return Math.max(0L, (long)Math.ceil(this.defaultWidth * aspectRatio));
		}
	}

	private Long calculateListItemHeight(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			return Math.max(0L, (long)Math.ceil(this.defaultHeight * aspectRatio));
		}else{
			return Math.max(0L, (frame.getInnerFrameHeight() - getScrollBarCrossSection(frame) - (this.maxAdjacentLists - lnh())) / this.maxAdjacentLists);
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
			Long listItemAndSpace = listItemHeight + 1;
			Long selectedItemOffset = newModuloOffset * listItemAndSpace;
			if(selectedItemOffset < yColumnOffset){
				yColumnOffset = selectedItemOffset;
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
				Long numItemsDown = newModuloOffset + 1L;
				Long itemCross = listItemWidth;
				if(list.size() == 0){
					//  Do nothing.
				}else if(numItemsDown.equals(this.maxItemsInAdjacentListColumn)){
					//  Item near end of column in list:
					Long listItemAndSpace = itemCross + 1;
					Long requiredColumnOffset = newModuloOffset * listItemAndSpace + itemCross;
					if(getUpperVisibleAreaColumnX() < requiredColumnOffset){
						xColumnOffset = requiredColumnOffset - listAreaLayer.getWidth();
					}
				}else{
					//  Item in middle with more items after:
					Long listItemAndSpace = itemCross + 1;
					Long requiredColumnOffset = listItemAndSpace * numItemsDown;

					if(getUpperVisibleAreaColumnX() < requiredColumnOffset){
						xColumnOffset += listItemAndSpace;
					}
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
				Long numItemsDown = newModuloOffset + 1L;
				Long itemCross = listItemHeight;
				if(list.size() == 0){
					//  Do nothing.
				}else if(numItemsDown.equals(this.maxItemsInAdjacentListColumn)){
					//  Item near end of column in list:
					Long listItemAndSpace = itemCross + 1;
					Long requiredColumnOffset = newModuloOffset * listItemAndSpace + itemCross;
					if(getUpperVisibleAreaColumnY() < requiredColumnOffset){
						yColumnOffset = requiredColumnOffset - listAreaLayer.getHeight();
					}
				}else{
					//  Item in middle with more items after:
					Long listItemAndSpace = itemCross + 1;
					Long requiredColumnOffset = listItemAndSpace * numItemsDown;

					if(getUpperVisibleAreaColumnY() < requiredColumnOffset){
						yColumnOffset += listItemAndSpace;
					}
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
			Long listItemAndSpace = listItemWidth + 1;
			Long selectedItemOffset = newModuloOffset * listItemAndSpace;
			if(selectedItemOffset < xColumnOffset){
				xColumnOffset = selectedItemOffset;
			}
		}
		render(frame, bottomLayer);
	}


	public void renderEmptyItem(UserInterfaceFrameThreadState frame, Long x, Long y, Long itemWidth, Long itemHeight) throws Exception{

		for(long l = 0; l < itemHeight; l++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(itemWidth.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + l, true, this.listAreaLayer);
		}
	}

	public Long getStartingOffsetForItemIndex(UserInterfaceFrameThreadState frame, Long n) throws Exception{
		Long listItemCrossSection = hasVerticalOrientation(frame) ? listItemHeight : listItemWidth;
		Long itemsBefore = (n % this.maxItemsInAdjacentListColumn);
		//  Width/height of each list item, plus the spaces in between all those items:
		return ((itemsBefore * listItemCrossSection) + (itemsBefore * 1L));
	}

	public Long getEndingOffsetForItemIndex(UserInterfaceFrameThreadState frame, Long n) throws Exception{
		Long listItemCrossSection = hasVerticalOrientation(frame) ? listItemHeight : listItemWidth;
		Long itemsDown = (n % this.maxItemsInAdjacentListColumn) + 1L;
		//  Width/height of each list item, plus the spaces in between all those items:
		return ((itemsDown * listItemCrossSection) + (Math.max(itemsDown - 1L, 0L) * 1L));
	}

	public Long getCrossSectionForFirstNListItems(UserInterfaceFrameThreadState frame, Long n, boolean getStartingBoundary) throws Exception{
		Long listItemCrossSection = hasVerticalOrientation(frame) ? listItemHeight : listItemWidth;
		Long moduloOffset = this.selectedIndex % this.maxItemsInAdjacentListColumn;
		Long previousItemPadding = (n.equals(0L) || n.equals(maxItemsInAdjacentListColumn -1L)) ? 1L : 0L;
		Long countedPadding = getStartingBoundary ? previousItemPadding : 0L;
		//  Width/height of each list item, plus the spaces in between all those items:
		Long ni = getStartingBoundary ? n : n + 1L;
		return ((ni * listItemCrossSection) + (Math.max(ni - 1L, 0L) * 1L)) + countedPadding;
	}

	public ColouredTextFragmentList makeScrollTextFragmentList(UserInterfaceFrameThreadState frame, Long textColumnHeight, Long offsetAdjustment) throws Exception{
		ColouredTextFragmentList rtn = new ColouredTextFragmentList();

		Long maxPossibleItemsInColumn = Math.min(Math.max(0L, list.size() -1), maxItemsInAdjacentListColumn);
		double visibleListAreaCrossSection = hasVerticalOrientation(frame) ? (double)frame.getInnerFrameHeight() : (double)frame.getInnerFrameWidth();

		double entireListColumnWidth = (double)getEndingOffsetForItemIndex(frame, (long)Math.max(0, list.size() -1));
		double offsetColumns = hasVerticalOrientation(frame) ? (double)yColumnOffset : (double)xColumnOffset;
		double percentOffset = offsetColumns / entireListColumnWidth;
		double percentVisible = visibleListAreaCrossSection / entireListColumnWidth;

		int firstColumnIndex = 0;
		int firstVisibleColumnIndex = (int)Math.floor(percentOffset * (double)textColumnHeight);
		int lastVisibleColumnIndex = Math.min((int)Math.ceil((percentOffset + percentVisible) * (double)textColumnHeight), textColumnHeight.intValue());
		int endColumnIndex = textColumnHeight.intValue();

		//  Calculate coloured Areas:
		int beforeScrollBar = firstVisibleColumnIndex - firstColumnIndex;
		int inScrollBar = lastVisibleColumnIndex - firstVisibleColumnIndex;
		int afterScrollBar = endColumnIndex - lastVisibleColumnIndex;

		GraphicsMode mode = frame.getBlockManagerThreadCollection().getGraphicsMode();
		boolean useAscii = mode.equals(GraphicsMode.ASCII);

		String dc = hasVerticalOrientation(frame) ? RenderableList.VERTICAL_SCROLL_BAR_CHARACTER : CharacterConstants.EQUALS_SIGN;

		rtn.add(
			new ColouredTextFragment(dc.repeat(beforeScrollBar), UserInterfaceFrameThreadState.getScrollBarDefaultColors(useAscii))
		);

		rtn.add(
			new ColouredTextFragment(" ".repeat(inScrollBar), UserInterfaceFrameThreadState.getVisibleAreaScrollBarBGColor())
		);

		rtn.add(
			new ColouredTextFragment(dc.repeat(afterScrollBar), UserInterfaceFrameThreadState.getScrollBarDefaultColors(useAscii))
		);

		return rtn;
	}

	public void renderInVerticalOrientation(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer, int lowerBound, int upperBound) throws Exception{
		Long fchw = frame.getFrameCharacterWidth();

		//  Space taken up by max number of whole list items that can fit on screen.
		Long onePaddedItemMeasurement = (listItemHeight + 1);
		Long visibleItemsArea = maxNumVisibleListItems * onePaddedItemMeasurement;
		Long totalSpace = frame.getInnerFrameHeight();
		Long offset = (totalSpace - visibleItemsArea) + 1;

		for(int adjacentListNumber = 0; adjacentListNumber < this.maxAdjacentLists; adjacentListNumber++){
			for(int i = lowerBound; i < upperBound; i++){
				int currentListItemIndex = i + (int)(adjacentListNumber * maxItemsInAdjacentListColumn);

				boolean isSelected = currentListItemIndex == this.selectedIndex;
				Long x = (adjacentListNumber * (listItemWidth + 1)) - xColumnOffset;
				Long y = i * (listItemHeight + 1) - yColumnOffset;
				Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
				if(
					currentListItemIndex >= 0 &&
					i < maxItemsInAdjacentListColumn &&
					currentListItemIndex < this.list.size()
				){
					RenderableListItem listItem = list.get(currentListItemIndex);
					listItem.render(frame, isSelected, placementOffset, this.listAreaLayer);
				}else{
					this.renderEmptyItem(
						frame,
						x,
						y,
						listItemWidth,
						listItemHeight
					);
				}
				frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(listItemWidth.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + listItemHeight, true, this.listAreaLayer);
				if(adjacentListNumber < (this.maxAdjacentLists-1)){
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(listItemHeight.intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), x + listItemWidth, y, false, this.listAreaLayer);
				}
			}
		}
		Long areaUsedByList = (listItemWidth * this.maxAdjacentLists) + (this.maxAdjacentLists -1L);
		Long initializedArea = areaUsedByList + getScrollBarCrossSection(frame);
		Long uninitializedAreaWidth = frame.getInnerFrameWidth() - initializedArea;

	
		//  Scroll bar
		for(long i = 0; i < getScrollBarCrossSection(frame); i++){
			frame.printTextAtScreenXY(makeScrollTextFragmentList(frame, frame.getInnerFrameHeight(), yColumnOffset), areaUsedByList + i, 0L, false, this.listAreaLayer);
		}

		//  Initialize any empty area on right edge after scroll bar:
		for(long i = 0; i < uninitializedAreaWidth; i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(frame.getInnerFrameHeight().intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), initializedArea + i, 0L, false, this.listAreaLayer);
		}

		//  Initialize empty uninitialized area under list when the list has very few items:
		Long areaUnderListYOffset = (upperBound - lowerBound) * (onePaddedItemMeasurement);
		for(long i = areaUnderListYOffset; i < frame.getInnerFrameHeight(); i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(areaUsedByList.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), 0L, i, true, this.listAreaLayer);
		}

		if(list.size() == 0){
			String msg = "List is empty.";
			int len = msg.length();
			Long x = (frame.getInnerFrameWidth() / 2L) - (((long)len) / 2L);
			Long y = (frame.getInnerFrameHeight() / 2L);
			frame.printTextAtScreenXY(new ColouredTextFragment(msg, UserInterfaceFrameThreadState.getDefaultTextColors()), x, y, true, this.listAreaLayer);
		}
	}

	public void renderInHorizontalOrientation(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer, int lowerBound, int upperBound) throws Exception{
		Long fchw = frame.getFrameCharacterWidth();

		//  Space taken up by max number of whole list items that can fit on screen.
		Long onePaddedItemMeasurement = (listItemWidth + fchw);
		Long visibleItemsArea = maxNumVisibleListItems * onePaddedItemMeasurement;
		Long totalSpace = frame.getInnerFrameWidth();
		Long offset = (totalSpace - visibleItemsArea) + 1;

		for(int adjacentListNumber = 0; adjacentListNumber < this.maxAdjacentLists; adjacentListNumber++){
			for(int i = lowerBound; i < upperBound; i++){
				int currentListItemIndex = i + (int)(adjacentListNumber * maxItemsInAdjacentListColumn);
				boolean isSelected = currentListItemIndex == this.selectedIndex;
				Long x = (i * (listItemWidth + fchw)) - xColumnOffset;
				Long y = (adjacentListNumber * (listItemHeight + 1)) - yColumnOffset ;

				Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
				if(
					//  Outside lower bound of entire list
					currentListItemIndex >= 0 &&
					//  Beyond end of column:
					i < maxItemsInAdjacentListColumn &&
					//  Outside upper bound of entire list
					currentListItemIndex < this.list.size()
				){
					RenderableListItem listItem = list.get(currentListItemIndex);
					listItem.render(frame, isSelected, placementOffset, this.listAreaLayer);
				}else{
					this.renderEmptyItem(
						frame,
						x,
						y,
						listItemWidth,
						listItemHeight
					);
				}
				frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(listItemHeight.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x + listItemWidth, y, false, this.listAreaLayer);
				if(adjacentListNumber < (this.maxAdjacentLists-1)){
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(listItemWidth.intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + listItemHeight, true, this.listAreaLayer);
				}
			}
		}
		Long areaUsedByList = (listItemHeight * this.maxAdjacentLists) + (this.maxAdjacentLists -1L);
		Long initializedArea = areaUsedByList + getScrollBarCrossSection(frame);
		Long uninitializedAreaHeight = frame.getInnerFrameHeight() - initializedArea;

		//  Scroll bar
		for(long i = 0; i < getScrollBarCrossSection(frame); i++){
			frame.printTextAtScreenXY(makeScrollTextFragmentList(frame, frame.getInnerFrameWidth(), xColumnOffset), 0L, areaUsedByList + i, true, this.listAreaLayer);
		}

		//  Initialize any empty area on bottom edge under scroll bar:
		for(long i = 0; i < uninitializedAreaHeight; i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(frame.getInnerFrameWidth().intValue() + 1), UserInterfaceFrameThreadState.getDefaultBGColors()), 0L, initializedArea + i, true, this.listAreaLayer);
		}

		//  Initialize empty uninitialized area after list when the list has very few items:
		Long areaUnderListXOffset = (upperBound - lowerBound) * (onePaddedItemMeasurement);
		for(long i = areaUnderListXOffset; i < frame.getInnerFrameWidth(); i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(areaUsedByList.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), i, 0L, false, this.listAreaLayer);
		}

		if(list.size() == 0){
			String msg = "List is empty.";
			int len = msg.length();
			Long x = (frame.getInnerFrameWidth() / 2L) - (((long)len) / 2L);
			Long y = (frame.getInnerFrameHeight() / 2L);
			frame.printTextAtScreenXY(new ColouredTextFragment(msg, UserInterfaceFrameThreadState.getDefaultBGColors()), x, y, true, this.listAreaLayer);
		}
	}

	public int getLowerVisibleAreaColumnX(){
		return xColumnOffset.intValue();
	}

	public int getUpperVisibleAreaColumnX(){
		return (int)(xColumnOffset + listAreaLayer.getWidth());
	}

	public int getLowerVisibleAreaColumnY(){
		return yColumnOffset.intValue();
	}

	public Long getUpperVisibleAreaColumnY(){
		return yColumnOffset + listAreaLayer.getHeight();
	}

	public int getLowerItemIndex(UserInterfaceFrameThreadState frame) throws Exception{
		if(hasVerticalOrientation(frame)){
			return (int)(getLowerVisibleAreaColumnY() / (listItemHeight + 1));
		}else{
			return (int)(getLowerVisibleAreaColumnX() / (listItemWidth + 1));
		}
	}

	public int getUpperItemIndex(UserInterfaceFrameThreadState frame)throws Exception{
		if(hasVerticalOrientation(frame)){
			return (int)Math.ceil((double)getUpperVisibleAreaColumnY() / (double)(listItemHeight + 1));
		}else{
			return (int)Math.ceil((double)getUpperVisibleAreaColumnX() / (double)(listItemWidth + 1));
		}
	}

	public void render(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		boolean hasVerticalOrientation = hasVerticalOrientation(frame);

		int lowerBound = getLowerItemIndex(frame);
		int upperBound = getUpperItemIndex(frame);
		if(hasVerticalOrientation){
			renderInVerticalOrientation(frame, bottomLayer, lowerBound, upperBound);
		}else{
			renderInHorizontalOrientation(frame, bottomLayer, lowerBound, upperBound);
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
					new Coordinate(Arrays.asList(listItemWidth, listItemHeight))
				)
			);
		}
	}

	public boolean getScrollBarState(UserInterfaceFrameThreadState frame, Long dividedListColumnLength) throws Exception{
		//  Calculate in advance whether a scroll bar will be required
		//  because actual width depends on whethere there is a scroll bar.
		Long testListItemHeight = calculateListItemHeight(frame);
		Long testListItemWidth = calculateListItemWidth(frame);
		if(list.size() == 0){
			return false;
		}else{
			if(hasVerticalOrientation(frame)){
				Long entireListColumnLength = (dividedListColumnLength * testListItemHeight) + ((dividedListColumnLength -1L) * 1);
				if(entireListColumnLength > listAreaLayer.getHeight()){
					return true;
				}else{
					return false;
				}
			}else{
				Long entireListColumnWidth = (dividedListColumnLength * testListItemWidth) + ((dividedListColumnLength -1L) * 1);
				if(entireListColumnWidth > listAreaLayer.getWidth()){
					return true;
				}else{
					return false;
				}
			}
		}
	}

	public void recalculateConstants(UserInterfaceFrameThreadState frame) throws Exception{
		Long dividedListColumnLength = (long)Math.ceil((double)list.size() / (double)maxAdjacentLists);

		this.hasScrollBar = this.getScrollBarState(frame, dividedListColumnLength);
		//  Calculate of exact width/height depends on whether there is a scroll bar:
		this.listItemHeight = calculateListItemHeight(frame);
		this.listItemWidth = calculateListItemWidth(frame);

		Long fchw = frame.getFrameCharacterWidth();
		Long maxVisibleFloor;
		Long maxVisibleCeil;
		if(hasVerticalOrientation(frame)){
			maxVisibleFloor = (long)Math.floor((double)this.renderableArea.getHeight() / (double)(listItemHeight + 1));
			maxVisibleCeil = (long)Math.ceil((double)this.renderableArea.getHeight() / (double)(listItemHeight + 1));
		}else{
			maxVisibleFloor = (long)Math.floor((double)this.renderableArea.getWidth() / (double)(listItemWidth + fchw));
			maxVisibleCeil = (long)Math.ceil((double)this.renderableArea.getWidth() / (double)(listItemWidth + fchw));
		}

		this.maxNumVisibleListItems = maxVisibleFloor;

		this.maxItemsInAdjacentListColumn = Math.max(maxVisibleFloor, dividedListColumnLength);

		if(list.size() == 0){
			//  Do nothing.
		}else{
			//  If the split orientation changes, keep the visible area focused on the selected item:
			if(hasVerticalOrientation(frame)){
				this.xColumnOffset = 0L;

				Long endOfList = getEndingOffsetForItemIndex(frame, (long)Math.max(0, list.size()-1));
				Long lowerSelectedItemBoundary = getStartingOffsetForItemIndex(frame, this.selectedIndex);
				Long upperSelectedItemBoundary = getEndingOffsetForItemIndex(frame, this.selectedIndex);
				Long currentScrollAreaLowerBoundary = this.yColumnOffset;
				Long currentScrollAreaUpperBoundary = this.yColumnOffset + listAreaLayer.getHeight();
				if(!(
					lowerSelectedItemBoundary >= currentScrollAreaLowerBoundary &&
					upperSelectedItemBoundary <= currentScrollAreaUpperBoundary
				)){
					if(!(lowerSelectedItemBoundary >= currentScrollAreaLowerBoundary)){
						this.yColumnOffset = lowerSelectedItemBoundary;
					}
					if(!(upperSelectedItemBoundary <= currentScrollAreaUpperBoundary)){
						this.yColumnOffset = upperSelectedItemBoundary - listAreaLayer.getHeight();
					}
				}
				if(this.hasScrollBar){
					//  Don't display beyond end of list:
					if((this.yColumnOffset + listAreaLayer.getHeight()) > endOfList){
						this.yColumnOffset = endOfList - listAreaLayer.getHeight();
					}
				}
			}else{
				this.yColumnOffset = 0L;

				Long endOfList = getEndingOffsetForItemIndex(frame, (long)Math.max(0, list.size()-1));
				Long lowerSelectedItemBoundary = getStartingOffsetForItemIndex(frame, this.selectedIndex);
				Long upperSelectedItemBoundary = getEndingOffsetForItemIndex(frame, this.selectedIndex);
				Long currentScrollAreaLowerBoundary = this.xColumnOffset;
				Long currentScrollAreaUpperBoundary = this.xColumnOffset + listAreaLayer.getWidth();

				if(!(
					lowerSelectedItemBoundary >= currentScrollAreaLowerBoundary &&
					upperSelectedItemBoundary <= currentScrollAreaUpperBoundary
				)){
					if(!(lowerSelectedItemBoundary >= currentScrollAreaLowerBoundary)){
						this.xColumnOffset = lowerSelectedItemBoundary;
					}
					if(!(upperSelectedItemBoundary <= currentScrollAreaUpperBoundary)){
						this.xColumnOffset = upperSelectedItemBoundary - listAreaLayer.getWidth();
					}
				}

				if(this.hasScrollBar){
					//  Don't display beyond end of list:
					if((this.xColumnOffset + listAreaLayer.getWidth()) > endOfList){
						this.xColumnOffset = endOfList - listAreaLayer.getWidth();
					}
				}
			}
		}
	}

	public void addItem(T item){
		list.add(item);
	}

	public int size(){
		return list.size();
	}
}
