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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ParserBuffer {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private List<Byte> buffer = new ArrayList<Byte>();
	private int position = 0;
	private int highestObservedPosition = 0;

	public ParserBuffer(){
	}

	public int getCurrentPosition() throws Exception{
		return this.position;
	}

	public void setPosition(int p) throws Exception{
		this.position = p;
	}

	public void advance() throws Exception{
		this.position++;
		if(this.position > highestObservedPosition){
			this.highestObservedPosition = this.position;
		}
	}

	public List<Byte> extractParsedBytes(){
		//  Extract only the portion of characters that were parsed:
		List<Byte> rtn = new ArrayList<Byte>();
		for(int i = 0; i < this.highestObservedPosition; i++){
			rtn.add(this.buffer.remove(0));
		}
		return rtn;
	}

	public byte extractNextByte() throws Exception{
		if(this.buffer.size() == 0){
			throw new Exception("Parser buffer was empty?");
		}else{
			return (byte)this.buffer.remove(0);
		}
	}

	public void addByte(Byte c) throws Exception{
		this.buffer.add(c);
	}

	public int size() throws Exception{
		return this.buffer.size();
	}

	public boolean atEnd() throws Exception{
		return !(this.position < this.buffer.size());
	}

	public void resetParsingPositions() throws Exception{
		this.position = 0;
		this.highestObservedPosition = 0;
	}

	public boolean containsPartiallyParsedSequence() throws Exception{
		return this.highestObservedPosition > 0;
	}

	public boolean wasParsingIncomplete() throws Exception{
		boolean rtn = this.highestObservedPosition > 0 && (this.highestObservedPosition != this.buffer.size());
		logger.info("wasParsingIncomplete=" + rtn + " this.highestObservedPosition=" + this.highestObservedPosition + " this.buffer.size()=" + this.buffer.size());
		return rtn;
	}

	public List<Byte> getBuffer(){
		return this.buffer;
	}


	public Byte head() throws Exception{
		if(position < buffer.size()){
			return buffer.get(position);
		}else{
			throw new Exception("Position " + position + " past end of buffer of size " + buffer.size());
		}
	}
}
