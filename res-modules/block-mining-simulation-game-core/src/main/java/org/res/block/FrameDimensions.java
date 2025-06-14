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
	private Long frameWidth;
	private Long frameHeight;
	private Long frameOffsetX;
	private Long frameOffsetY;
	private Long terminalWidth;
	private Long terminalHeight;

	public FrameDimensions(Long frameCharacterWidth, Long frameWidth, Long frameHeight, Long frameOffsetX, Long frameOffsetY, Long terminalWidth, Long terminalHeight) throws Exception {
		this.frameCharacterWidth = frameCharacterWidth;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.frameOffsetX = frameOffsetX;
		this.frameOffsetY = frameOffsetY;
		this.terminalWidth = terminalWidth;
		this.terminalHeight = terminalHeight;
		this.sanityCheck();
	}

	public FrameDimensions(FrameDimensions f) throws Exception {
		this.frameCharacterWidth = f.getFrameCharacterWidth();
		this.frameWidth = f.getFrameWidth();
		this.frameHeight = f.getFrameHeight();
		this.frameOffsetX = f.getFrameOffsetX();
		this.frameOffsetY = f.getFrameOffsetY();
		this.terminalWidth = f.getTerminalWidth();
		this.terminalHeight = f.getTerminalHeight();
		this.sanityCheck();
	}

	public final void sanityCheck() throws Exception{
		if(this.frameCharacterWidth < 0L){
			throw new Exception("this.frameCharacterWidth < 0L");
		}
		if(this.frameWidth < 0L){
			throw new Exception("this.frameWidth < 0L");
		}
		if(this.frameHeight < 0L){
			throw new Exception("this.frameHeight < 0L");
		}
		if(this.frameOffsetX < 0L){
			throw new Exception("this.frameOffsetX < 0L");
		}
		if(this.frameOffsetY < 0L){
			throw new Exception("this.frameOffsetY < 0L");
		}
		if(this.terminalWidth < 0L){
			throw new Exception("this.terminalWidth < 0L");
		}
		if(this.terminalHeight < 0L){
			throw new Exception("this.terminalHeight < 0L");
		}
	}

	public Long getFrameCharacterWidth(){
		return this.frameCharacterWidth;
	}

	public Long getFrameWidth(){
		return this.frameWidth;
	}

	public Long getFrameHeight(){
		return this.frameHeight;
	}

	public Long getFrameOffsetX(){
		return this.frameOffsetX;
	}

	public Long getFrameOffsetY(){
		return this.frameOffsetY;
	}

	public Long getTerminalWidth(){
		return this.terminalWidth;
	}

	public Long getTerminalHeight(){
		return this.terminalHeight;
	}

	@Override
	public final int hashCode(){
		return 0;
	}

	@Override
	public String toString(){
		return "frameCharacterWidth=" + String.valueOf(this.frameCharacterWidth) + "frameWidth=" + String.valueOf(this.frameWidth) + "frameHeight=" + String.valueOf(this.frameHeight) + "frameOffsetX=" + String.valueOf(this.frameOffsetX) + "frameOffsetY=" + String.valueOf(this.frameOffsetY) + "terminalWidth=" + String.valueOf(this.terminalWidth) + "terminalHeight=" + String.valueOf(this.terminalHeight);
	}

	@Override
	public boolean equals(Object a){
		FrameDimensions o = (FrameDimensions)a;
		if(o == null){
			return false;
		}else{
			return (
				Objects.equals(o.getFrameCharacterWidth(), this.frameCharacterWidth) &&
				Objects.equals(o.getFrameWidth(), this.frameWidth) &&
				Objects.equals(o.getFrameHeight(), this.frameHeight) &&
				Objects.equals(o.getFrameOffsetX(), this.frameOffsetX) &&
				Objects.equals(o.getFrameOffsetY(), this.frameOffsetY) &&
				Objects.equals(o.getTerminalWidth(), this.terminalWidth) &&
				Objects.equals(o.getTerminalHeight(), this.terminalHeight)
			);
		}
	}
}
