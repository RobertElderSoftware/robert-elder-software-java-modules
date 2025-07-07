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

import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import java.util.Objects;

public class FrameDimensions {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Long frameCharacterWidth;
	private CuboidAddress frame = new CuboidAddress(
		Coordinate.makeDiagonalCoordinate(0L, 2L),
		Coordinate.makeDiagonalCoordinate(0L, 2L)
	);
	private CuboidAddress terminal = new CuboidAddress(
		Coordinate.makeDiagonalCoordinate(0L, 2L),
		Coordinate.makeDiagonalCoordinate(0L, 2L)
	);

	public FrameDimensions() throws Exception {

	}

	public FrameDimensions(Long frameCharacterWidth, CuboidAddress frame, CuboidAddress terminal) throws Exception {
		this.frameCharacterWidth = frameCharacterWidth;
		this.frame = frame;
		this.terminal = terminal;
		this.sanityCheck();
	}

	public FrameDimensions(FrameDimensions f) throws Exception {
		this.frameCharacterWidth = f.getFrameCharacterWidth();
		this.frame = f.getFrame();
		this.terminal = f.getTerminal();
		this.sanityCheck();
	}

	public final void sanityCheck() throws Exception{
		if(this.frameCharacterWidth < 0L){
			throw new Exception("this.frameCharacterWidth < 0L");
		}
		if(this.frame.getWidth() < 0L){
			throw new Exception("this..getWidth() < 0L");
		}
		if(this.frame.getHeight() < 0L){
			throw new Exception("this.getHeight() < 0L");
		}
		if(this.frame.getCanonicalLowerCoordinate().getX() < 0L){
			throw new Exception("this.getCanonicalLowerCoordinate().getX() < 0L");
		}
		if(this.frame.getCanonicalLowerCoordinate().getY() < 0L){
			throw new Exception("this.getCanonicalLowerCoordinate().getY() < 0L");
		}
		if(this.terminal.getWidth() < 0L){
			throw new Exception("this.terminal.getWidth() < 0L");
		}
		if(this.terminal.getHeight() < 0L){
			throw new Exception("this.terminal.getHeight() < 0L");
		}
	}

	public Long getFrameCharacterWidth(){
		return this.frameCharacterWidth;
	}

	public CuboidAddress getFrame(){
		return this.frame;
	}

	public CuboidAddress getTerminal(){
		return this.terminal;
	}

	public Long getFrameWidth(){
		return this.frame.getWidth();
	}

	public Long getFrameHeight(){
		return this.frame.getHeight();
	}

	public Long getFrameOffsetX() throws Exception{
		return this.frame.getCanonicalLowerCoordinate().getX();
	}

	public Long getFrameOffsetY() throws Exception{
		return this.frame.getCanonicalLowerCoordinate().getY();
	}

	public Long getTerminalWidth(){
		return this.terminal.getWidth();
	}

	public Long getTerminalHeight(){
		return this.terminal.getHeight();
	}

	@Override
	public final int hashCode(){
		return 0;
	}

	@Override
	public String toString(){
		return "frameCharacterWidth=" + String.valueOf(this.frameCharacterWidth) + "frame=" + String.valueOf(this.frame) + "terminal=" + String.valueOf(this.terminal);
	}

	@Override
	public boolean equals(Object a){
		FrameDimensions o = (FrameDimensions)a;
		if(o == null){
			return false;
		}else{
			return (
				Objects.equals(o.getFrameCharacterWidth(), this.frameCharacterWidth) &&
				Objects.equals(o.getFrame(), this.frame) &&
				Objects.equals(o.getTerminal(), this.terminal)
			);
		}
	}
}
