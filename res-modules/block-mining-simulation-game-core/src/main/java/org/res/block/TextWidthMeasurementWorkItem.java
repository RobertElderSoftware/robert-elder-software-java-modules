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

public class TextWidthMeasurementWorkItem extends ConsoleWriterWorkItem {

	private String text;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Long x1 = 1L;
	private Long y1 = 1L;
	private Long x2 = null; //  Reported second cursor position after printing text.
	private Long y2 = null;

	public TextWidthMeasurementWorkItem(ConsoleWriterThreadState consoleWriterThreadState, String text){
		super(consoleWriterThreadState, true);
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
		this.consoleWriterThreadState.addPendingTextWidthRequest(this);
	}

	public Long getX1(){
		return this.x1;
	}

	public Long getY1(){
		return this.y1;
	}

	public TextWidthMeasurementWorkItemResult getResult(){
		if(x2 != null && y2 != null){
			Long deltaX = x2 - x1;
			Long deltaY = y2 - y1;
			if(deltaX >= 0L && deltaY >= 0L){
				return new TextWidthMeasurementWorkItemResult(deltaX, deltaY);
			}else{
				logger.info("WARNING:  Negative text movement? : deltaX=" + deltaX + ", deltaY=" + deltaY + "?");
				return new TextWidthMeasurementWorkItemResult(0L, 0L);
			}
		}else{
			return null;
		}
	}
}
