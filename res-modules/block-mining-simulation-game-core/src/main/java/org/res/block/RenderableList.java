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
	private RenderableListItem grid[][];
	private int gridWidth = 0;
	private int gridHeight = 0;
	private static final Long LINE_HEIGHT = 1L;
	private static final String VERTICAL_SCROLL_BAR_CHARACTER = CharacterConstants.VERTICAL_LINE;

	private Long defaultWidth;
	private Long defaultHeight;
	private Long selectedIndexX = 0L;
	private Long selectedIndexY = 0L;
	private Long xColumnOffset = 0L;
	private Long yColumnOffset = 0L;
	private boolean hasRightScrollBar = false;
	private boolean hasBottomScrollBar = false;

	private Long maxVisibleAdjacentLists;
	private Long maxAdjacentLists;
	protected ScreenLayer listAreaLayer = new ScreenLayer();

	private double aspectRatio;
	private Long listItemHeight;
	private Long listItemWidth;
	private String emptyMessage;
	private RenderableListContainer container;

	public RenderableList(RenderableListContainer container, Long maxVisibleAdjacentLists, Long maxAdjacentLists, Long defaultWidth, Long defaultHeight, String emptyMessage) throws Exception{
		this.container = container;
		this.maxVisibleAdjacentLists = maxVisibleAdjacentLists;
		this.maxAdjacentLists = maxAdjacentLists;
		this.defaultWidth = defaultWidth;
		this.defaultHeight = defaultHeight;
		this.aspectRatio = (double)this.defaultWidth / (double)this.defaultHeight;
		this.emptyMessage = emptyMessage;
	}

	public List<T> getListItems(){
		return this.list;
	}

	public void notifySelectionChanged(UserInterfaceFrameThreadState frame) throws Exception{
		this.container.onSelectionChange(this.gridPositionToListIndex(this.selectedIndexX, this.selectedIndexY));
	}

	public boolean hasVerticalOrientation() throws Exception{
		//  The 'orientation' of the list will flip depending on the height/width ratio
		//  of the list's draw area.  I measured the pixels on one individual character
		//  column in my terminal and it was 22px wide by 10px tall:
		double columnSizeRatio = 22.0 / 10.0;
		return (listAreaLayer.getHeight() * columnSizeRatio) > listAreaLayer.getWidth();
	}

	public Long spw(UserInterfaceFrameThreadState frame) throws Exception{
		return frame.textWidth(CharacterConstants.SPACE);
	}

	public Long lnh(){
		return RenderableList.LINE_HEIGHT;
	}

	public Long getRightScrollBarCrossSection(UserInterfaceFrameThreadState frame) throws Exception{
		return 2L * frame.textWidth(RenderableList.VERTICAL_SCROLL_BAR_CHARACTER);
	}

	public Long getBottomScrollBarCrossSection(UserInterfaceFrameThreadState frame) throws Exception{
		return lnh();
	}

	private Long calculateListItemWidth(UserInterfaceFrameThreadState frame, boolean includeScrollBar) throws Exception{
		if(hasVerticalOrientation()){
			Long scrollBar = includeScrollBar ? getRightScrollBarCrossSection(frame) : 0L;
			return Math.max(0L, (listAreaLayer.getWidth() - scrollBar - (this.maxVisibleAdjacentLists - spw(frame))) / this.maxVisibleAdjacentLists);
		}else{
			return Math.max(0L, (long)Math.ceil(this.defaultWidth * aspectRatio));
		}
	}

	private Long calculateListItemHeight(UserInterfaceFrameThreadState frame, boolean includeScrollBar) throws Exception{
		if(hasVerticalOrientation()){
			return Math.max(0L, (long)Math.ceil(this.defaultHeight * aspectRatio));
		}else{
			Long scrollBar = includeScrollBar ? getBottomScrollBarCrossSection(frame) : 0L;
			return Math.max(0L, (listAreaLayer.getHeight() - scrollBar - (this.maxVisibleAdjacentLists - lnh())) / this.maxVisibleAdjacentLists);
		}
	}

	public void onUpArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(this.selectedIndexY.equals(0L)){
			//  Do nothing, at top of list area.
		}else{
			this.selectedIndexY--;
		}
		Long listItemAndSpace = listItemHeight + lnh();
		Long selectedItemOffset = this.selectedIndexY * listItemAndSpace;
		if(selectedItemOffset < yColumnOffset){
			yColumnOffset = selectedItemOffset;
		}
		this.notifySelectionChanged(frame);
		render(frame, bottomLayer);
	}

	public void onRightArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(
			//  Right boundary for list area
			(selectedIndexX.equals(this.gridWidth-1L)) ||
			//  Or, last element in list which may not be against bottom boundary
			gridPositionToListIndex(selectedIndexX + 1L, selectedIndexY) >= (list.size())
		){
			//  Do nothing.
		}else{
			this.selectedIndexX++;
			Long numItemsDown = this.selectedIndexX + 1L;
			Long itemCross = listItemWidth;
			if(list.size() == 0){
				//  Do nothing.
			}else if(numItemsDown.equals((long)this.gridWidth)){
				//  Item near end of column in list:
				Long listItemAndSpace = itemCross + spw(frame);
				Long actualRightScrollBarWidth = this.hasRightScrollBar ? getRightScrollBarCrossSection(frame) : 0L;
				Long requiredColumnOffset = this.selectedIndexX * listItemAndSpace + itemCross + actualRightScrollBarWidth;
				if(getUpperVisibleAreaColumnX() < requiredColumnOffset){
					xColumnOffset = requiredColumnOffset - listAreaLayer.getWidth();
				}
			}else{
				//  Item in middle with more items after:
				Long listItemAndSpace = itemCross + spw(frame);
				Long requiredColumnOffset = listItemAndSpace * numItemsDown;

				if(getUpperVisibleAreaColumnX() < requiredColumnOffset){
					xColumnOffset += listItemAndSpace;
				}
			}
		}
		this.notifySelectionChanged(frame);
		render(frame, bottomLayer);
	}

	public void onDownArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(
			//  Bottom boundary for list area
			(this.selectedIndexY.equals(this.gridHeight-1L)) ||
			//  Or, last element in list which may not be against bottom boundary
			gridPositionToListIndex(selectedIndexX, selectedIndexY + 1L) >= (list.size())
		){
			//  Do nothing.
		}else{
			this.selectedIndexY++;
			Long numItemsDown = this.selectedIndexY + 1L;
			Long itemCross = listItemHeight;
			if(list.size() == 0){
				//  Do nothing.
			}else if(numItemsDown.equals((long)this.gridHeight)){
				//  Item near end of column in list:
				Long listItemAndSpace = itemCross + lnh();
				Long actualBottomScrollBarWidth = this.hasBottomScrollBar ? getBottomScrollBarCrossSection(frame) : 0L;
				Long requiredColumnOffset = this.selectedIndexY * listItemAndSpace + itemCross + actualBottomScrollBarWidth;
				if(getUpperVisibleAreaColumnY() < requiredColumnOffset){
					yColumnOffset = requiredColumnOffset - listAreaLayer.getHeight();
				}
			}else{
				//  Item in middle with more items after:
				Long listItemAndSpace = itemCross + lnh();
				Long requiredColumnOffset = listItemAndSpace * numItemsDown;

				if(getUpperVisibleAreaColumnY() < requiredColumnOffset){
					yColumnOffset += listItemAndSpace;
				}
			}
		}
		this.notifySelectionChanged(frame);
		render(frame, bottomLayer);
	}

	public void onLeftArrowPressed(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		if(this.selectedIndexX.equals(0L)){
			//  Do nothing, left edge of list.
		}else{
			this.selectedIndexX--;
		}

		Long listItemAndSpace = listItemWidth + spw(frame);
		Long selectedItemOffset = this.selectedIndexX * listItemAndSpace;
		if(selectedItemOffset < xColumnOffset){
			xColumnOffset = selectedItemOffset;
		}
		this.notifySelectionChanged(frame);
		render(frame, bottomLayer);
	}

	public void renderEmptyItem(UserInterfaceFrameThreadState frame, Long x, Long y, Long itemWidth, Long itemHeight) throws Exception{

		for(long l = 0; l < itemHeight; l++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(itemWidth.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + l, true, this.listAreaLayer);
		}
	}

	public Long getOffsetForGridItem(Long index, Long listItemCrossSection, Long space, Long gridCrossSection, boolean isEndingOffset) throws Exception{
		if(index < 0L){
			return 0L;
		}else{
			Long itemsDown = gridCrossSection.equals(0L) ? 0L : (Math.max(0L, index) % gridCrossSection);
			Long endingAdj = isEndingOffset ? 1L : 0L;
			return (((itemsDown + endingAdj) * listItemCrossSection) + (itemsDown  * space));
		}
	}

	public Long getStartingOffsetForGridItemAtX(UserInterfaceFrameThreadState frame, Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemWidth, spw(frame), (long)this.gridWidth, false);
	}

	public Long getStartingOffsetForGridItemAtY(UserInterfaceFrameThreadState frame, Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemHeight, lnh(), (long)this.gridHeight, false);
	}

	public Long getEndingOffsetForGridItemAtX(UserInterfaceFrameThreadState frame, Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemWidth, spw(frame), (long)this.gridWidth, true);
	}

	public Long getEndingOffsetForGridItemAtY(UserInterfaceFrameThreadState frame, Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemHeight, lnh(), (long)this.gridHeight, true);
	}

	public ColouredTextFragmentList makeScrollTextFragmentList(UserInterfaceFrameThreadState frame, Long textColumnHeight, boolean isVertical) throws Exception{
		ColouredTextFragmentList rtn = new ColouredTextFragmentList();

		double visibleListAreaCrossSection = isVertical ? (double)listAreaLayer.getHeight() : (double)listAreaLayer.getWidth();

		double entireListColumnWidth = isVertical ? (double)getEndingOffsetForGridItemAtY(frame, (long)Math.max(0, this.gridHeight -1)) : (double)getEndingOffsetForGridItemAtX(frame, (long)Math.max(0, this.gridWidth -1));
		double offsetColumns = isVertical ? (double)yColumnOffset : (double)xColumnOffset;
		double percentOffset = offsetColumns / entireListColumnWidth;
		double percentVisible = visibleListAreaCrossSection / entireListColumnWidth;

		int firstColumnIndex = 0;
		int firstVisibleColumnIndex = (int)Math.floor(percentOffset * (double)textColumnHeight);
		int lastVisibleColumnIndex = Math.min((int)Math.ceil((percentOffset + percentVisible) * (double)textColumnHeight), textColumnHeight.intValue());
		int endColumnIndex = textColumnHeight.intValue();

		//  Calculate coloured Areas:
		int beforeScrollBar = Math.max(firstVisibleColumnIndex - firstColumnIndex, 0);
		int inScrollBar = Math.max(lastVisibleColumnIndex - firstVisibleColumnIndex, 0);
		int afterScrollBar = Math.max(endColumnIndex - lastVisibleColumnIndex, 0);

		if(beforeScrollBar < 0 || inScrollBar < 0 || afterScrollBar < 0){
			throw new Exception("One of the scroll bar components was negative: beforeScrollBar=" + beforeScrollBar + " inScrollBar=" + inScrollBar + " afterScrollBar=" + afterScrollBar + "");
		}

		GraphicsMode mode = frame.getBlockManagerThreadCollection().getGraphicsMode();
		boolean useAscii = mode.equals(GraphicsMode.ASCII);

		String dc = isVertical ? RenderableList.VERTICAL_SCROLL_BAR_CHARACTER : CharacterConstants.EQUALS_SIGN;

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

	public void renderList(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		for(int i = 0; i < this.gridWidth; i++){
			for(int j = 0; j < this.gridHeight; j++){
				Long x = getStartingOffsetForGridItemAtX(frame, (long)i) - xColumnOffset;
				Long y = (j * listItemHeight) + (j * lnh()) - yColumnOffset;

				RenderableListItem listItem = this.grid[i][j];
				if(listItem == null){
					this.renderEmptyItem(
						frame,
						x,
						y,
						listItemWidth,
						listItemHeight
					);
				}else{
					boolean isSelected = selectedIndexX.equals((long)i) && selectedIndexY.equals((long)j);
					Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
					listItem.render(frame, isSelected, placementOffset, this.listAreaLayer);
				}
				//  Vertical space between list items
				if(i < (this.gridWidth - 1)){
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(listItemHeight.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x + listItemWidth, y, false, this.listAreaLayer);
				}
				//  Horizontal space under list item
				if(j < (this.gridHeight - 1)){
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(listItemWidth.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + listItemHeight, true, this.listAreaLayer);
				}

				//  Uninitialized square at bottom right hand corner of each item:
				if(i < (this.gridWidth - 1) && j < (this.gridHeight - 1)){
					frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(spw(frame).intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x + listItemWidth, y + listItemHeight, true, this.listAreaLayer);
				}
			}
		}

		Long actualRightScrollBarWidth = this.hasRightScrollBar ? getRightScrollBarCrossSection(frame) : 0L;
		Long rightScrollBarOffset = listAreaLayer.getWidth() - actualRightScrollBarWidth;
		Long rightScrollBarCharacterHeight = this.hasBottomScrollBar ? (long)listAreaLayer.getHeight() - getBottomScrollBarCrossSection(frame): (long)listAreaLayer.getHeight();
	
		//  Right Scroll bar
		for(long i = 0; i < actualRightScrollBarWidth; i++){
			frame.printTextAtScreenXY(makeScrollTextFragmentList(frame, rightScrollBarCharacterHeight, true), rightScrollBarOffset + i, 0L, false, this.listAreaLayer);
		}


		Long actualBottomScrollBarHeight = this.hasBottomScrollBar ? getBottomScrollBarCrossSection(frame) : 0L;
		Long bottomScrollBarCharacterWidth = this.hasRightScrollBar ? (long)listAreaLayer.getWidth() - getRightScrollBarCrossSection(frame): (long)listAreaLayer.getWidth();
		//  Bottom Scroll bar
		Long bottomScrollBarOffset = listAreaLayer.getHeight() - actualBottomScrollBarHeight;
		for(long i = 0; i < getBottomScrollBarCrossSection(frame); i++){
			frame.printTextAtScreenXY(makeScrollTextFragmentList(frame, bottomScrollBarCharacterWidth, false), 0L, bottomScrollBarOffset + i, true, this.listAreaLayer);
		}

		//  Initialize any empty area to right of list any before any scroll bar/right edge of frame:
		Long listRightEdgeX = getEndingOffsetForGridItemAtX(frame, this.gridWidth -1L) - xColumnOffset;
		Long rightFrameEdgeX = listAreaLayer.getWidth() - actualRightScrollBarWidth;
		int nSpacesOnRight = (int)Math.max(rightFrameEdgeX - listRightEdgeX, 0L);
		Long rightAreaHeight = listAreaLayer.getHeight() - actualBottomScrollBarHeight;
		for(long i = 0L; i < rightAreaHeight; i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(nSpacesOnRight), UserInterfaceFrameThreadState.getDefaultBGColors()), listRightEdgeX, i, true, this.listAreaLayer);
		}

		//  Initialize empty uninitialized area under list up to the area before the right of the list:
		Long listBottomEdgeY = getEndingOffsetForGridItemAtY(frame, this.gridHeight -1L) - yColumnOffset;
		Long listBottomLeftEdgeX = 0L;
		int nSpacesUnderOnLeft = (int)Math.max(listRightEdgeX - listBottomLeftEdgeX, 0L);
		Long leftUnderAreaHeight = listAreaLayer.getHeight() - actualBottomScrollBarHeight - listBottomEdgeY;
		for(long i = 0L; i < leftUnderAreaHeight; i++){
			frame.printTextAtScreenXY(new ColouredTextFragment(" ".repeat(nSpacesUnderOnLeft), UserInterfaceFrameThreadState.getDefaultBGColors()), listBottomLeftEdgeX, listBottomEdgeY + i, true, this.listAreaLayer);
		}

		if(list.size() == 0){
			String msg = this.emptyMessage;
			int len = msg.length();
			Long x = (listAreaLayer.getWidth() / 2L) - (((long)len) / 2L);
			Long y = (listAreaLayer.getHeight() / 2L);
			frame.printTextAtScreenXY(new ColouredTextFragment(msg, UserInterfaceFrameThreadState.getDefaultTextColors()), x, y, true, this.listAreaLayer);
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

	public void render(UserInterfaceFrameThreadState frame, ScreenLayer bottomLayer) throws Exception{
		renderList(frame, bottomLayer);
		bottomLayer.mergeDown(this.listAreaLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public void updateRenderableArea(UserInterfaceFrameThreadState frame, CuboidAddress ca) throws Exception{

		//  Get index of selected list item from before:
		Long selectedIndexBefore = gridPositionToListIndex(this.selectedIndexX, this.selectedIndexY);

		Long xOffset = ca.getCanonicalLowerCoordinate().getX();
		Long yOffset = ca.getCanonicalLowerCoordinate().getY();
		Coordinate placementOffset = new Coordinate(Arrays.asList(xOffset, yOffset));

		this.listAreaLayer = new ScreenLayer(placementOffset, ScreenLayer.makeDimensionsCA(0, 0, (int)ca.getWidth(), (int)ca.getHeight()));
		//  Initialize to an obvious pattern for testing.  
		this.listAreaLayer.initializeInRegion(1, "M", new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR, UserInterfaceFrameThreadState.YELLOW_BG_COLOR}, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, (int)ca.getWidth(), (int)ca.getHeight())), true, true);

		this.recalculateConstants(frame, selectedIndexBefore); //  Relies on current state of this.listAreaLayer

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

	public boolean calculateHasBottomScrollBar(UserInterfaceFrameThreadState frame, Long listItemWidth, Long gridWidth) throws Exception{
		if(list.size() == 0){
			return false;
		}else{
			Long entireListColumnWidth = getEndingOffsetForGridItemAtX(frame, gridWidth -1L);
			if(entireListColumnWidth > listAreaLayer.getWidth()){
				return true;
			}else{
				return false;
			}
		}
	}

	public boolean calculateHasRightScrollBar(UserInterfaceFrameThreadState frame, Long listItemHeight, Long gridHeight) throws Exception{
		if(list.size() == 0){
			return false;
		}else{
			Long entireListColumnLength = getEndingOffsetForGridItemAtY(frame, gridHeight -1L);
			if(entireListColumnLength > listAreaLayer.getHeight()){
				return true;
			}else{
				return false;
			}
		}
	}

	public Long gridPositionToListIndex(Long x, Long y) throws Exception{
		if(hasVerticalOrientation()){
			/* Vertical orientation list rendering order is
			   0 3 6
			   1 4 7 
			   2 5 8 */
			return this.gridHeight * x + y;
		}else{
			/* Horizontal orientation list rendering order is
			   0 1 2 
			   3 4 5 
			   6 7 8 */
			return this.gridWidth * y + x;
		}
	}

	public void setSelectedListIndex(UserInterfaceFrameThreadState frame, Long listIndex) throws Exception{
		if(list.size() > 0){
			if(hasVerticalOrientation()){
				this.selectedIndexX = listIndex / this.gridHeight;
				this.selectedIndexY = listIndex % this.gridHeight;
			}else{
				this.selectedIndexX = listIndex % this.gridWidth;
				this.selectedIndexY = listIndex / this.gridWidth;
			}
		}else{
			this.selectedIndexX = 0L;
			this.selectedIndexY = 0L;
		}
	}

	private void recalculateConstants(UserInterfaceFrameThreadState frame, Long selectedIndexBefore) throws Exception{

		Long minRows = (long)Math.ceil((double)list.size() / (double)maxAdjacentLists);
		Long minCols = (long)Math.ceil((double)list.size() / (double)Math.max(minRows, 1L));

		if(hasVerticalOrientation()){
			this.gridWidth = minCols.intValue();
			this.gridHeight = minRows.intValue();
		}else{
			this.gridWidth = minRows.intValue();
			this.gridHeight = minCols.intValue(); 
		}

		this.grid = new RenderableListItem [this.gridWidth][this.gridHeight];

		//  Start by assuming there are no scroll bars when calculating list item widths/heights:
		this.listItemHeight = this.calculateListItemHeight(frame, false);
		this.listItemWidth = this.calculateListItemWidth(frame, false);
		this.hasRightScrollBar = this.calculateHasRightScrollBar(frame, listItemHeight, (long)gridHeight);
		this.hasBottomScrollBar = this.calculateHasBottomScrollBar(frame, listItemWidth, (long)gridWidth);
		if(this.hasRightScrollBar || this.hasBottomScrollBar){
			//  If there is a scroll bar, re-calulate item widths/heights based on that assumption:
			this.listItemHeight = this.calculateListItemHeight(frame, true);
			this.listItemWidth = this.calculateListItemWidth(frame, true);
			this.hasRightScrollBar = this.calculateHasRightScrollBar(frame, listItemHeight, (long)gridHeight);
			this.hasBottomScrollBar = this.calculateHasBottomScrollBar(frame, listItemWidth, (long)gridWidth);
		}

		for(int i = 0; i < this.gridWidth; i++){
			for(int j = 0; j < this.gridHeight; j++){
				int listIndex = this.gridPositionToListIndex((long)i, (long)j).intValue();
				if(listIndex < list.size()){
					this.grid[i][j] = list.get(listIndex);
				}else{
					this.grid[i][j] = null;
				}
				if(selectedIndexBefore.equals((long)listIndex)){
					//  Set new index which might have changed if orientation changed:
					this.selectedIndexX = (long)i;
					this.selectedIndexY = (long)j;
				}
			}
		}

		Long currentlySelectedItemStartX = getStartingOffsetForGridItemAtX(frame, this.selectedIndexX);
		Long currentlySelectedItemEndX = getEndingOffsetForGridItemAtX(frame, this.selectedIndexX);
		Long currentlySelectedItemStartY = getStartingOffsetForGridItemAtY(frame, this.selectedIndexY);
		Long currentlySelectedItemEndY = getEndingOffsetForGridItemAtY(frame, this.selectedIndexY);

		//  Selected item appears before start of on screen area:
		if(currentlySelectedItemStartX < this.xColumnOffset){
			this.xColumnOffset = currentlySelectedItemStartX;
		}
		if(currentlySelectedItemStartY < this.yColumnOffset){
			this.yColumnOffset = currentlySelectedItemStartY;
		}

		//  Selected item has endpoint off of visible area:
		if(currentlySelectedItemEndX > (this.xColumnOffset + listAreaLayer.getWidth())){
			this.xColumnOffset = currentlySelectedItemStartX;
		}
		if(currentlySelectedItemEndY > (this.yColumnOffset + listAreaLayer.getHeight())){
			this.yColumnOffset = currentlySelectedItemStartY;
		}

		Long entireListColumnsX = getEndingOffsetForGridItemAtX(frame, this.gridWidth-1L);
		Long entireListColumnsY = getEndingOffsetForGridItemAtY(frame, this.gridHeight-1L);
		Long actualBottomScrollBarWidth = this.hasBottomScrollBar ? getBottomScrollBarCrossSection(frame) : 0L;
		Long actualRightScrollBarWidth = this.hasRightScrollBar ? getRightScrollBarCrossSection(frame) : 0L;
		//  If there is area after the list showing, move the offset back to show more of the list:
		if((this.xColumnOffset + listAreaLayer.getWidth() - actualRightScrollBarWidth) > entireListColumnsX){
			Long newXOffset = entireListColumnsX - (listAreaLayer.getWidth() - actualRightScrollBarWidth);
			this.xColumnOffset = Math.max(0L, newXOffset);
		}
		if((this.yColumnOffset + listAreaLayer.getHeight() - actualBottomScrollBarWidth) > entireListColumnsY){
			Long newYOffset = entireListColumnsY - (listAreaLayer.getHeight() - actualBottomScrollBarWidth);
			this.yColumnOffset = Math.max(0L, newYOffset);
		}
	}

	public void clearList(){
		this.list = new ArrayList<T>();
	}

	public void addItem(T item){
		this.list.add(item);
	}

	public int size(){
		return list.size();
	}
}
