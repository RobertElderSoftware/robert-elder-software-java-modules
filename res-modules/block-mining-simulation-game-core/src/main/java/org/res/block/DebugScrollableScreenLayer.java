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

public class DebugScrollableScreenLayer extends ScrollableScreenLayer implements BlockManagerThreadCollectionProvider {
	private BlockManagerThreadCollection blockManagerThreadCollection;
	private Long contentColumnWidth = 0L;
	private Long contentColumnHeight = 0L;

	public Long getContentColumnHeight() throws Exception{
		return contentColumnHeight;
	}

	public Long getContentColumnWidth() throws Exception{
		return contentColumnWidth;
	}

	public BlockManagerThreadCollectionProvider getProvider(){
		return this;
	}

	public boolean getHasRightScrollBar(){
		return true;
	}

	public boolean getHasBottomScrollBar(){
		return true;
	}

	public void renderDebugArea() throws Exception{
		for(long x = 0L; x < this.getWidth(); x++){
			for(long y = 0L; y < this.getHeight(); y++){
				boolean isOutOfBounds = (x < 0 || x > contentColumnWidth || y < 0 || y > contentColumnHeight);
				long xO = x - this.getScrollColumnOffsetX();
				long yO = y - this.getScrollColumnOffsetY();
				int [] bgColors = isOutOfBounds ? UserInterfaceFrameThreadState.getRGBBackgroundColor(0, 0, 255) : UserInterfaceFrameThreadState.getRGBBackgroundColor((int)xO * 5, (int)yO * 5, 0);
				int [] fgColors = UserInterfaceFrameThreadState.getDefaultTextFGColors();

				int [] colors = UserInterfaceFrameThreadState.concatIntArrays(bgColors, fgColors);
				String cellText = isOutOfBounds ? "." : String.valueOf((xO+yO) % 10L);
				ScreenLayer.printTextAtScreenXY(this, new ColouredTextFragment(cellText, colors), x, y, PrintDirection.LEFT_TO_RIGHT, this);
			}
		}

		this.drawScrollBars(this);
	}

	public void render(ScreenLayer bottomLayer) throws Exception{
		renderDebugArea();
		bottomLayer.mergeDown(this, true, ScreenLayerMergeType.PREFER_BOTTOM_LAYER);
	}

	public void updateLayerBoundary(Coordinate upper) throws Exception{
		this.resizeLayer(
			new CuboidAddress(
				new Coordinate(Arrays.asList(0L, 0L)),
				upper
			)
		);
	}

	public void setVisibleWidth(Long w) throws Exception{
		this.updateLayerBoundary(new Coordinate(Arrays.asList(w, (long)this.getHeight())));
	}

	public void setVisibleHeight(Long h) throws Exception{
		this.updateLayerBoundary(new Coordinate(Arrays.asList((long)this.getWidth(), h)));
	}

	public void setContentColumnWidth(Long w) throws Exception{
		this.contentColumnWidth = w;
	}

	public void setContentColumnHeight(Long h) throws Exception{
		this.contentColumnHeight = h;
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public DebugScrollableScreenLayer(BlockManagerThreadCollection blockManagerThreadCollection) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
	}
}
