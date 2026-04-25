//  Copyright (c) 2026 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License (Version 2026-04-09)
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
//  either unmodified, modified, or incorporated into another software product, 
//  except as described in the document "REDISTRIBUTION.md" (a file with SHA256 
//  hash value 'c39a6c8200a22caf30eac97095b78def80c9cab1b6f7ddd3fca7fdae71df43da').
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

	private Long initialSelection = null;
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
	protected RenderableListScreenLayer<T> listAreaLayer;

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

	public void init() throws Exception{
		this.listAreaLayer = new RenderableListScreenLayer<T>(this);
	}

	public List<T> getListItems(){
		return this.list;
	}

	public void notifySelectionChanged() throws Exception{
		this.container.onSelectionChange(this.gridPositionToListIndex(this.selectedIndexX, this.selectedIndexY));
	}

	public boolean hasVerticalOrientation() throws Exception{
		//  The 'orientation' of the list will flip depending on the height/width ratio
		//  of the list's draw area.  I measured the pixels on one individual character
		//  column in my terminal and it was 22px wide by 10px tall:
		double columnSizeRatio = 22.0 / 10.0;
		return (listAreaLayer.getHeight() * columnSizeRatio) > listAreaLayer.getWidth();
	}

	public Long spw() throws Exception{
		return this.container.getBlockManagerThreadCollection().textWidth(CharacterConstants.SPACE);
	}

	public Long lnh(){
		return ScreenLayer.LINE_HEIGHT;
	}

	public Long getRightScrollBarCrossSection() throws Exception{
		return ScrollableScreenLayer.getRightScrollBarCrossSection(this.container);
	}

	public Long getBottomScrollBarCrossSection() throws Exception{
		return ScrollableScreenLayer.getBottomScrollBarCrossSection();
	}

	private Long calculateListItemWidth(boolean includeScrollBar) throws Exception{
		if(hasVerticalOrientation()){
			Long scrollBar = includeScrollBar ? getRightScrollBarCrossSection() : 0L;
			return Math.max(0L, (listAreaLayer.getWidth() - scrollBar - (this.maxVisibleAdjacentLists - spw())) / this.maxVisibleAdjacentLists);
		}else{
			return Math.max(0L, (long)Math.ceil(this.defaultHeight * aspectRatio));
		}
	}

	private Long calculateListItemHeight(boolean includeScrollBar) throws Exception{
		if(hasVerticalOrientation()){
			return Math.max(0L, (long)Math.ceil(this.defaultHeight / aspectRatio));
		}else{
			Long scrollBar = includeScrollBar ? getBottomScrollBarCrossSection() : 0L;
			return Math.max(0L, (listAreaLayer.getHeight() - scrollBar - (this.maxVisibleAdjacentLists - lnh())) / this.maxVisibleAdjacentLists);
		}
	}

	public void onUpArrowPressed(ScreenLayer bottomLayer) throws Exception{
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
		this.notifySelectionChanged();
		render(bottomLayer);
	}

	public void onRightArrowPressed(ScreenLayer bottomLayer) throws Exception{
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
				Long listItemAndSpace = itemCross + spw();
				Long actualRightScrollBarWidth = this.hasRightScrollBar ? getRightScrollBarCrossSection() : 0L;
				Long requiredColumnOffset = this.selectedIndexX * listItemAndSpace + itemCross + actualRightScrollBarWidth;
				if(getUpperVisibleAreaColumnX() < requiredColumnOffset){
					xColumnOffset = requiredColumnOffset - listAreaLayer.getWidth();
				}
			}else{
				//  Item in middle with more items after:
				Long listItemAndSpace = itemCross + spw();
				Long requiredColumnOffset = listItemAndSpace * numItemsDown;

				if(getUpperVisibleAreaColumnX() < requiredColumnOffset){
					xColumnOffset += listItemAndSpace;
				}
			}
		}
		this.notifySelectionChanged();
		render(bottomLayer);
	}


	public void onDownArrowPressed(ScreenLayer bottomLayer) throws Exception{
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
				Long actualBottomScrollBarHeight = ScrollableScreenLayer.getActualBottomScrollBarHeight(this.container, this.hasBottomScrollBar);
				Long requiredColumnOffset = this.selectedIndexY * listItemAndSpace + itemCross + actualBottomScrollBarHeight;
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
		this.notifySelectionChanged();
		render(bottomLayer);
	}

	public void onLeftArrowPressed(ScreenLayer bottomLayer) throws Exception{
		if(this.selectedIndexX.equals(0L)){
			//  Do nothing, left edge of list.
		}else{
			this.selectedIndexX--;
		}

		Long listItemAndSpace = listItemWidth + spw();
		Long selectedItemOffset = this.selectedIndexX * listItemAndSpace;
		if(selectedItemOffset < xColumnOffset){
			xColumnOffset = selectedItemOffset;
		}
		this.notifySelectionChanged();
		render(bottomLayer);
	}

	public void renderEmptyItem(Long x, Long y, Long itemWidth, Long itemHeight) throws Exception{

		for(long l = 0; l < itemHeight; l++){
			ScreenLayer.printTextAtScreenXY(container, new ColouredTextFragment(" ".repeat(itemWidth.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + l, PrintDirection.LEFT_TO_RIGHT, this.listAreaLayer);
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

	public Long getStartingOffsetForGridItemAtX(Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemWidth, spw(), (long)this.gridWidth, false);
	}

	public Long getStartingOffsetForGridItemAtY(Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemHeight, lnh(), (long)this.gridHeight, false);
	}

	public Long getEndingOffsetForGridItemAtX(Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemWidth, spw(), (long)this.gridWidth, true);
	}

	public Long getEndingOffsetForGridItemAtY(Long n) throws Exception{
		return getOffsetForGridItem(n, this.listItemHeight, lnh(), (long)this.gridHeight, true);
	}


	public void renderList(ScreenLayer bottomLayer) throws Exception{
		GraphicsMode mode = container.getBlockManagerThreadCollection().getGraphicsMode();
		boolean useAscii = mode.equals(GraphicsMode.ASCII);
		for(int i = 0; i < this.gridWidth; i++){
			for(int j = 0; j < this.gridHeight; j++){
				Long x = getStartingOffsetForGridItemAtX((long)i) - xColumnOffset;
				Long y = (j * listItemHeight) + (j * lnh()) - yColumnOffset;

				RenderableListItem listItem = this.grid[i][j];
				if(listItem == null){
					this.renderEmptyItem(
						x,
						y,
						listItemWidth,
						listItemHeight
					);
				}else{
					boolean isSelected = selectedIndexX.equals((long)i) && selectedIndexY.equals((long)j);
					Coordinate placementOffset = new Coordinate(Arrays.asList(x,y));
					listItem.render(isSelected, placementOffset, this.listAreaLayer);
				}
				//  Vertical space between list items
				if(i < (this.gridWidth - 1)){
					ScreenLayer.printTextAtScreenXY(container, new ColouredTextFragment(" ".repeat(listItemHeight.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x + listItemWidth, y, PrintDirection.TOP_TO_BOTTOM, this.listAreaLayer);
				}
				//  Horizontal space under list item
				if(j < (this.gridHeight - 1)){
					ScreenLayer.printTextAtScreenXY(container, new ColouredTextFragment(" ".repeat(listItemWidth.intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x, y + listItemHeight, PrintDirection.LEFT_TO_RIGHT, this.listAreaLayer);
				}

				//  Uninitialized square at bottom right hand corner of each item:
				if(i < (this.gridWidth - 1) && j < (this.gridHeight - 1)){
					ScreenLayer.printTextAtScreenXY(container, new ColouredTextFragment(" ".repeat(spw().intValue()), UserInterfaceFrameThreadState.getDefaultBGColors()), x + listItemWidth, y + listItemHeight, PrintDirection.LEFT_TO_RIGHT, this.listAreaLayer);
				}
			}
		}

		Long entireListColumnWidthRight = getEndingOffsetForGridItemAtY((long)Math.max(0, this.gridHeight -1));
		Long entireListColumnWidthBottom = getEndingOffsetForGridItemAtX((long)Math.max(0, this.gridWidth -1));

		Long listRightEdgeX = entireListColumnWidthBottom - xColumnOffset;
		Long listBottomEdgeY = entireListColumnWidthRight - yColumnOffset;

		ScrollableScreenLayer.drawRightScrollBar(this.container, this.hasRightScrollBar, this.hasBottomScrollBar, listAreaLayer, entireListColumnWidthRight, (double)yColumnOffset);

		ScrollableScreenLayer.drawBottomScrollBar(this.container, this.hasRightScrollBar, this.hasBottomScrollBar, listAreaLayer, entireListColumnWidthBottom, (double)xColumnOffset);
		//  Initialize any empty area to right of list any before any scroll bar/right edge of screen layer:
		ScrollableScreenLayer.fillUninitializedAreaOnRight(this.container, this.hasRightScrollBar, this.hasBottomScrollBar, listAreaLayer, listRightEdgeX);

		//  Initialize empty uninitialized area under list up to the area before the right of the list:
		ScrollableScreenLayer.fillUninitializedAreaOnBottom(this.container, this.hasRightScrollBar, this.hasBottomScrollBar, listAreaLayer, listRightEdgeX, listBottomEdgeY);

		ScrollableScreenLayer.fillUninitializedBottomRightCorner(this.container, this.hasRightScrollBar, this.hasBottomScrollBar, listAreaLayer);

		if(list.size() == 0){
			String msg = this.emptyMessage;
			Long len = container.getBlockManagerThreadCollection().getConsoleWriterThreadState().measureTextLengthOnTerminal(msg).getDeltaX();
			Long x = (listAreaLayer.getWidth() / 2L) - (((long)len) / 2L);
			Long y = (listAreaLayer.getHeight() / 2L);
			ScreenLayer.printTextAtScreenXY(container, new ColouredTextFragment(msg, UserInterfaceFrameThreadState.getDefaultTextColors()), x, y, PrintDirection.LEFT_TO_RIGHT, this.listAreaLayer);
		}
	}

	public boolean getHasRightScrollBar(){
		return this.hasRightScrollBar;
	}

	public boolean getHasBottomScrollBar(){
		return this.hasBottomScrollBar;
	}

	public RenderableListContainer getContainer(){
		return this.container;
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

	public void render(ScreenLayer bottomLayer) throws Exception{
		renderList(bottomLayer);
		bottomLayer.mergeDown(this.listAreaLayer, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public void updateRenderableArea(CuboidAddress ca) throws Exception{
		this.updateRenderableArea(ca, false);
	}

	public void updateRenderableArea(CuboidAddress ca, boolean adjustAdjacentColumns) throws Exception{

		//  Get index of selected list item from before:
		Long selectedIndexBefore = gridPositionToListIndex(this.selectedIndexX, this.selectedIndexY);

		Long xOffset = ca.getCanonicalLowerCoordinate().getX();
		Long yOffset = ca.getCanonicalLowerCoordinate().getY();
		Coordinate placementOffset = new Coordinate(Arrays.asList(xOffset, yOffset));

		this.listAreaLayer = new RenderableListScreenLayer<T>(this, placementOffset, ScreenLayer.makeDimensionsCA(0, 0, (int)ca.getWidth(), (int)ca.getHeight()));
		//  Initialize to an obvious pattern for testing.  
		this.listAreaLayer.initializeInRegion(1, "M", new int [] {UserInterfaceFrameThreadState.GREEN_FG_COLOR, UserInterfaceFrameThreadState.YELLOW_BG_COLOR}, null, new ScreenRegion(ScreenRegion.makeScreenRegionCA(0, 0, (int)ca.getWidth(), (int)ca.getHeight())), true, true);

		//  If this flag is set, automatically figure out # of adjacent columns based on available space:
		if(adjustAdjacentColumns){
			Long listCrossToUse = hasVerticalOrientation() ? (long)this.listAreaLayer.getWidth() : (long)this.listAreaLayer.getHeight();
			Long itemCrossToUse = hasVerticalOrientation() ? this.defaultWidth : this.defaultHeight;
			Long space = hasVerticalOrientation() ? spw() : lnh();
			Long scrollBar = hasVerticalOrientation() ? getRightScrollBarCrossSection() : getBottomScrollBarCrossSection();
			Long calcMaxAdj = (listCrossToUse - scrollBar + space) / (space + itemCrossToUse);
			Long defaultMaxAdj = Math.max(1L, calcMaxAdj);

			this.maxVisibleAdjacentLists = defaultMaxAdj;
			this.maxAdjacentLists = defaultMaxAdj;
		}

		this.recalculateConstants(selectedIndexBefore); //  Relies on current state of this.listAreaLayer

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

	public boolean calculateHasBottomScrollBar(Long listItemWidth, Long gridWidth) throws Exception{
		if(list.size() == 0){
			return false;
		}else{
			Long entireListColumnWidth = getEndingOffsetForGridItemAtX(gridWidth -1L);
			if(entireListColumnWidth > listAreaLayer.getWidth()){
				return true;
			}else{
				return false;
			}
		}
	}

	public boolean calculateHasRightScrollBar(Long listItemHeight, Long gridHeight) throws Exception{
		if(list.size() == 0){
			return false;
		}else{
			Long entireListColumnLength = getEndingOffsetForGridItemAtY(gridHeight -1L);
			if(entireListColumnLength > listAreaLayer.getHeight()){
				return true;
			}else{
				return false;
			}
		}
	}

	public Long gridPositionToListIndex(Long x, Long y) throws Exception{
		if(x == null || y == null){
			return null;
		}else{
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
	}

	public Long getCurrentlySelectedListIndex() throws Exception{
		return this.gridPositionToListIndex(this.selectedIndexX, this.selectedIndexY);
	}

	public void setSelectedListIndex(Long listIndex) throws Exception{
		if(list.size() > 0 && this.gridHeight > 0L && this.gridWidth > 0L){
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
			this.initialSelection = listIndex;
		}
	}

	private void recalculateConstants(Long selectedIndexBefore) throws Exception{

		//  For cases where initial selection index is lost due to empty list/zero width etc.
		if(this.initialSelection != null){
			selectedIndexBefore = this.initialSelection;
			this.initialSelection = null;
		}

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
		this.listItemHeight = this.calculateListItemHeight(false);
		this.listItemWidth = this.calculateListItemWidth(false);
		this.hasRightScrollBar = this.calculateHasRightScrollBar(listItemHeight, (long)gridHeight);
		this.hasBottomScrollBar = this.calculateHasBottomScrollBar(listItemWidth, (long)gridWidth);
		if(this.hasRightScrollBar || this.hasBottomScrollBar){
			//  If there is a scroll bar, re-calulate item widths/heights based on that assumption:
			this.listItemHeight = this.calculateListItemHeight(true);
			this.listItemWidth = this.calculateListItemWidth(true);
			this.hasRightScrollBar = this.calculateHasRightScrollBar(listItemHeight, (long)gridHeight);
			this.hasBottomScrollBar = this.calculateHasBottomScrollBar(listItemWidth, (long)gridWidth);
		}

		for(int i = 0; i < this.gridWidth; i++){
			for(int j = 0; j < this.gridHeight; j++){
				int listIndex = this.gridPositionToListIndex((long)i, (long)j).intValue();
				if(listIndex < list.size()){
					this.grid[i][j] = list.get(listIndex);
				}else{
					this.grid[i][j] = null;
				}
				if(selectedIndexBefore != null && selectedIndexBefore.equals((long)listIndex)){
					//  Set new index which might have changed if orientation changed:
					this.selectedIndexX = (long)i;
					this.selectedIndexY = (long)j;
				}
			}
		}

		Long currentlySelectedItemStartX = getStartingOffsetForGridItemAtX(this.selectedIndexX);
		Long currentlySelectedItemEndX = getEndingOffsetForGridItemAtX(this.selectedIndexX);
		Long currentlySelectedItemStartY = getStartingOffsetForGridItemAtY(this.selectedIndexY);
		Long currentlySelectedItemEndY = getEndingOffsetForGridItemAtY(this.selectedIndexY);

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

		Long entireListColumnsX = getEndingOffsetForGridItemAtX(this.gridWidth-1L);
		Long entireListColumnsY = getEndingOffsetForGridItemAtY(this.gridHeight-1L);
		Long actualBottomScrollBarHeight = ScrollableScreenLayer.getActualBottomScrollBarHeight(this.container, this.hasBottomScrollBar);
		Long actualRightScrollBarWidth = ScrollableScreenLayer.getActualRightScrollBarWidth(this.container, this.hasRightScrollBar);
		//  If there is area after the list showing, move the offset back to show more of the list:
		if((this.xColumnOffset + listAreaLayer.getWidth() - actualRightScrollBarWidth) > entireListColumnsX){
			Long newXOffset = entireListColumnsX - (listAreaLayer.getWidth() - actualRightScrollBarWidth);
			this.xColumnOffset = Math.max(0L, newXOffset);
		}
		if((this.yColumnOffset + listAreaLayer.getHeight() - actualBottomScrollBarHeight) > entireListColumnsY){
			Long newYOffset = entireListColumnsY - (listAreaLayer.getHeight() - actualBottomScrollBarHeight);
			this.yColumnOffset = Math.max(0L, newYOffset);
		}
	}

	public void replaceList(List<T> renderers){
		this.list.clear();
		for(T r : renderers){
			this.addItem(r);
		}
	}

	public void clear(){
		this.list.clear();
	}

	public void addItem(T item){
		this.list.add(item);
	}

	public int size(){
		return list.size();
	}
}
