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

public abstract class ScrollableScreenLayer extends ScreenLayer {
	public abstract void render(ScreenLayer bottomLayer) throws Exception;
	public abstract Long getContentColumnHeight() throws Exception;
	public abstract Long getContentColumnWidth() throws Exception;
	public abstract BlockManagerThreadCollectionProvider getProvider();
	public abstract boolean getHasRightScrollBar();
	public abstract boolean getHasBottomScrollBar();

	private static final String VERTICAL_SCROLL_BAR_CHARACTER = CharacterConstants.VERTICAL_LINE;
	public Long scrollColumnOffsetX = 0L;
	public Long scrollColumnOffsetY = 0L;

	public Long getScrollColumnOffsetX(){
		return scrollColumnOffsetX;
	}

	public Long getScrollColumnOffsetY(){
		return scrollColumnOffsetY;
	}

	public void setScrollColumnOffsetX(Long x){
		this.scrollColumnOffsetX = x;
	}

	public void setScrollColumnOffsetY(Long y){
		this.scrollColumnOffsetY = y;
	}

	public ScrollableScreenLayer() throws Exception{
		super();
	}

	public ScrollableScreenLayer(Coordinate placementOffset, CuboidAddress dimensions) throws Exception{
		super(placementOffset, dimensions);
	}

	public static Long getRightScrollBarCrossSection(BlockManagerThreadCollectionProvider provider) throws Exception{
		return 2L * provider.getBlockManagerThreadCollection().textWidth(ScrollableScreenLayer.VERTICAL_SCROLL_BAR_CHARACTER);
	}

	public static Long getBottomScrollBarCrossSection() throws Exception{
		return ScreenLayer.LINE_HEIGHT;
	}

	public static Long getActualRightScrollBarWidth(BlockManagerThreadCollectionProvider provider, boolean hasRightScrollBar) throws Exception{
		return hasRightScrollBar ? ScrollableScreenLayer.getRightScrollBarCrossSection(provider) : 0L;
	}

	public static Long getActualBottomScrollBarHeight(BlockManagerThreadCollectionProvider provider, boolean hasBottomScrollBar) throws Exception{
		return hasBottomScrollBar ? ScrollableScreenLayer.getBottomScrollBarCrossSection() : 0L;
	}

	public void drawScrollBars(BlockManagerThreadCollectionProvider provider) throws Exception{
		this.drawRightScrollBar(provider);
		this.drawBottomScrollBar(provider);
		//  Initialize any empty area to right of list any before any scroll bar/right edge of screen layer:
		this.fillUninitializedAreaOnRight(provider);

		//  Initialize empty uninitialized area under list up to the area before the right of the list:
		this.fillUninitializedAreaOnBottom(provider);
		this.fillUninitializedBottomRightCorner(provider);
	}

	public void drawRightScrollBar(BlockManagerThreadCollectionProvider provider) throws Exception{
		GraphicsMode mode = provider.getBlockManagerThreadCollection().getGraphicsMode();
		boolean useAscii = mode.equals(GraphicsMode.ASCII);

		Long actualRightScrollBarWidth = ScrollableScreenLayer.getActualRightScrollBarWidth(provider, getHasRightScrollBar());
		Long rightScrollBarOffset = getWidth() - actualRightScrollBarWidth;
		Long rightScrollBarCharacterHeight = getHasBottomScrollBar() ? (long)getHeight() - ScrollableScreenLayer.getBottomScrollBarCrossSection(): (long)getHeight();
	
		//  Right Scroll bar
		for(long i = 0; i < actualRightScrollBarWidth; i++){
			ScreenLayer.printTextAtScreenXY(provider, ScrollableScreenLayer.makeScrollTextFragmentList(rightScrollBarCharacterHeight, getContentColumnHeight(), getScrollColumnOffsetY(), true, useAscii, this), rightScrollBarOffset + i, 0L, PrintDirection.TOP_TO_BOTTOM, this);
		}
	}

	public Long drawBottomScrollBar(BlockManagerThreadCollectionProvider provider) throws Exception{

		GraphicsMode mode = provider.getBlockManagerThreadCollection().getGraphicsMode();
		boolean useAscii = mode.equals(GraphicsMode.ASCII);
		Long actualBottomScrollBarHeight = ScrollableScreenLayer.getActualBottomScrollBarHeight(provider, getHasBottomScrollBar());
		Long bottomScrollBarOffset = getHeight() - actualBottomScrollBarHeight;
		Long bottomScrollBarCharacterWidth = getHasRightScrollBar() ? (long)getWidth() - getRightScrollBarCrossSection(provider): (long)getWidth();
		//  Bottom Scroll bar
		for(long i = 0; i < getBottomScrollBarCrossSection(); i++){
			ScreenLayer.printTextAtScreenXY(provider, ScrollableScreenLayer.makeScrollTextFragmentList(bottomScrollBarCharacterWidth, getContentColumnWidth(), getScrollColumnOffsetX(), false, useAscii, this), 0L, bottomScrollBarOffset + i, PrintDirection.LEFT_TO_RIGHT, this);
		}

		return actualBottomScrollBarHeight;
	}

	public void fillUninitializedAreaOnRight(BlockManagerThreadCollectionProvider provider) throws Exception{

		Long contentEdgeX = getContentColumnWidth() - getScrollColumnOffsetX();

		Long actualRightScrollBarWidth = ScrollableScreenLayer.getActualRightScrollBarWidth(provider, getHasRightScrollBar());
		Long actualBottomScrollBarHeight = ScrollableScreenLayer.getActualBottomScrollBarHeight(provider, getHasBottomScrollBar());
		Long rightFrameEdgeX = getWidth() - actualRightScrollBarWidth;
		int nSpacesOnRight = (int)Math.max(rightFrameEdgeX - contentEdgeX, 0L);
		Long rightAreaHeight = getHeight() - actualBottomScrollBarHeight;
		for(long i = 0L; i < rightAreaHeight; i++){
			ScreenLayer.printTextAtScreenXY(provider, new ColouredTextFragment("R".repeat(nSpacesOnRight), UserInterfaceFrameThreadState.getDefaultBGColors()), contentEdgeX, i, PrintDirection.LEFT_TO_RIGHT, this);
		}
	}

	public void fillUninitializedAreaOnBottom(BlockManagerThreadCollectionProvider provider) throws Exception{
		Long contentRightEdgeX = getContentColumnWidth() - getScrollColumnOffsetX();
		Long contentBottomEdgeY = getContentColumnHeight() - getScrollColumnOffsetY();
		Long actualRightScrollBarWidth = ScrollableScreenLayer.getActualRightScrollBarWidth(provider, getHasRightScrollBar());
		Long actualBottomScrollBarHeight = ScrollableScreenLayer.getActualBottomScrollBarHeight(provider, getHasBottomScrollBar());
		Long contentBottomLeftEdgeX = 0L;
		int nSpacesUnderOnLeft = (int)Math.max(contentRightEdgeX - contentBottomLeftEdgeX, 0L);
		Long leftUnderAreaHeight = getHeight() - actualBottomScrollBarHeight - contentBottomEdgeY;
		for(long i = 0L; i < leftUnderAreaHeight; i++){
			ScreenLayer.printTextAtScreenXY(provider, new ColouredTextFragment("B".repeat(nSpacesUnderOnLeft), UserInterfaceFrameThreadState.getDefaultBGColors()), contentBottomLeftEdgeX, contentBottomEdgeY + i, PrintDirection.LEFT_TO_RIGHT, this);
		}
	}

	public void fillUninitializedBottomRightCorner(BlockManagerThreadCollectionProvider provider) throws Exception{
		Long actualRightScrollBarWidth = ScrollableScreenLayer.getActualRightScrollBarWidth(provider, getHasRightScrollBar());
		Long actualBottomScrollBarHeight = ScrollableScreenLayer.getActualBottomScrollBarHeight(provider, getHasBottomScrollBar());
		GraphicsMode mode = provider.getBlockManagerThreadCollection().getGraphicsMode();
		boolean useAscii = mode.equals(GraphicsMode.ASCII);
		if(getHasRightScrollBar() && getHasBottomScrollBar()){
			//  When both scroll bars are active there is
			//  an unitialized area in the bottom right hand corner:
			Long xDrawOffset = getWidth() - actualRightScrollBarWidth;
			Long yDrawOffset = getHeight() - actualBottomScrollBarHeight;
			for(long i = 0L; i < actualBottomScrollBarHeight; i++){
				ScreenLayer.printTextAtScreenXY(provider, new ColouredTextFragment(CharacterConstants.ASTERISK.repeat(actualRightScrollBarWidth.intValue()), UserInterfaceFrameThreadState.getScrollBarDefaultColors(useAscii)), xDrawOffset, yDrawOffset + i, PrintDirection.LEFT_TO_RIGHT, this);
			}
		}
	}

	public static ColouredTextFragmentList makeScrollTextFragmentList(Long textColumnHeight, double entireListColumnWidth, double offsetColumns, boolean isVertical, boolean useAscii, ScrollableScreenLayer layer) throws Exception{
		ColouredTextFragmentList rtn = new ColouredTextFragmentList();

		double visibleListAreaCrossSection = isVertical ? (double)layer.getHeight() : (double)layer.getWidth();

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


		String dc = isVertical ? ScrollableScreenLayer.VERTICAL_SCROLL_BAR_CHARACTER : CharacterConstants.EQUALS_SIGN;

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
}
