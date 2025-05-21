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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.Comparator;

import java.lang.Thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class ScreenOutputBuffer {

	public int [][] characterWidths = null;
	public int [][][] colourCodes = null;
	public String [][] characters = null;
	public boolean [][] changedFlags = null;

	public ScreenOutputBuffer(){

	}

	public void initialize(Long terminalWidth, Long terminalHeight){
		// By default, make assumptions that minimize screen prints
		this.initialize(terminalWidth, terminalHeight, 0, null, false, null);

	}

	public void initialize(Long terminalWidth, Long terminalHeight, int chrWidth, String s, boolean changed, String msg){
		this.characterWidths = new int [terminalWidth.intValue()][terminalHeight.intValue()];
		for(int [] a : this.characterWidths){
			Arrays.fill(a, chrWidth);
		}
		this.colourCodes = new int [terminalWidth.intValue()][terminalHeight.intValue()][0];
		this.characters = new String [terminalWidth.intValue()][terminalHeight.intValue()];
		for(String [] a : this.characters){
			Arrays.fill(a, s);
		}
		this.changedFlags = new boolean [terminalWidth.intValue()][terminalHeight.intValue()];
		for(boolean [] a : this.changedFlags){
			Arrays.fill(a, changed);
		}
		if(msg != null){
			Long messageLength = Long.valueOf(msg.length());
			Long xOffset = messageLength > terminalWidth ? 0 : ((terminalWidth - messageLength) / 2);
			Long yOffset = terminalHeight / 2L;

			for(Long i = 0L; i < msg.length(); i++){
				if(((int)(xOffset + i)) < terminalWidth.intValue()){
					this.characters[xOffset.intValue() + i.intValue()][yOffset.intValue()] = String.valueOf(msg.charAt(i.intValue()));
				}
			}

		}
	}
}
