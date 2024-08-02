//  Copyright (c) 2024 Robert Elder Software Inc.
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

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class TextWidthMeasurementWorkItem extends CharacterWidthMeasurementWorkItem {

	private String text;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Long x1 = 1L;
	private Long y1 = 1L;
	private Long x2 = null; //  Reported second cursor position after printing text.
	private Long y2 = null;

	public TextWidthMeasurementWorkItem(CharacterWidthMeasurementThreadState characterWidthMeasurementThreadState, String text){
		super(characterWidthMeasurementThreadState, true);
		this.text = text;
	}

	public void notifyOfCurrentCursorPosition(Long x, Long y){
		if(this.x2 == null){
			this.x2 = x;
			this.y2 = y;
		}else{
			logger.info("WARNING:  Discarding cursor position report (" + x + "," + y + ") because the class TextWidthMeasurementWorkItem has already seen a cursor position report?");
		}
	}

	public String getText(){
		return text;
	}

	public void doWork() throws Exception{
		this.characterWidthMeasurementThreadState.addPendingTextWidthRequest(this);
		System.out.print("\033[" + this.y1 + ";" + this.x1 + "H"); //  Move cursor known reference point to calculate offset of text.
		System.out.flush();
		System.out.print(text); //  Print the text for which we want to measure width.
		System.out.flush();
		System.out.println("\033[6n");  //  Request cursor position measurement.
		System.out.flush();
		//  Move cursor back to 0,0 to and re-draw some of the corner of the frame
		//  to overwrite the test character:
		System.out.print("\033[0;0H\u2554\u2550\u2550\u2550");
	}

	public TextWidthMeasurementWorkItemResult getResult(){
		if(x2 != null){
			Long textWidth = x2 - x1;
			if(textWidth >= 0L){
				return new TextWidthMeasurementWorkItemResult(textWidth);
			}else{
				logger.info("WARNING:  Negative text width: " + textWidth + "?");
				return new TextWidthMeasurementWorkItemResult(0L);
			}
		}else{
			return null;
		}
	}
}
