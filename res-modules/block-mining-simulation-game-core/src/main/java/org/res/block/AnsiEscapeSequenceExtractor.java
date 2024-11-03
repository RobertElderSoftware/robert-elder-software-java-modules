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

public class AnsiEscapeSequenceExtractor {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private ParserBuffer buffer = new ParserBuffer();

	public Byte acceptByteRange(byte low, byte high) throws Exception{
		if(!buffer.atEnd()){
			if(buffer.head() >= low && buffer.head() <= high){
				Byte r = buffer.head();
				buffer.advance();
				return r;
			}
		}
		return null;
	}

	public Byte acceptByte(byte c) throws Exception{
		return acceptByteRange(c, c);
	}

	public Long acceptIntegerConstant() throws Exception {
		List<Byte> numberBytes = new ArrayList<Byte>();
		while(!buffer.atEnd()){
			Byte c = acceptByteRange((byte)'0', (byte)'9');
			if(c != null){
				numberBytes.add(c);
			}else{
				break;
			}
		}
		if(numberBytes.size() > 0){
			try{
				byte[] byteArray = new byte[numberBytes.size()];
				for (int index = 0; index < numberBytes.size(); index++) {
					byteArray[index] = numberBytes.get(index);
				}
				return Long.valueOf(new String(byteArray, "UTF-8"));
			}catch (NumberFormatException e){
				throw new Exception("NumberFormatException for " + String.valueOf(numberBytes) + ": ", e);
			}
		}else{
			return null;
		}
	}

	public AnsiEscapeSequence acceptEscapeSequence() throws Exception{
		int initialPosition = this.buffer.getCurrentPosition();
		if(acceptByte((byte)'\033') != null){
			if(acceptByte((byte)'[') != null){
				Long y = acceptIntegerConstant();
				if(y != null){
					if(acceptByte((byte)';') != null){
						Long x = acceptIntegerConstant();
						if(x != null){
							if(acceptByte((byte)'R') != null){
								return new CursorPositionReport(x, y);
							}else{
								this.buffer.setPosition(initialPosition);
								return null;
							}
						}else{
							this.buffer.setPosition(initialPosition);
							return null;
						}
					}else if(acceptByte((byte)'~') != null){
						if(y.equals(5L)){
							return new AnsiEscapeSequencePageUpKey();
						}else if(y.equals(6L)){
							return new AnsiEscapeSequencePageDownKey();
						}else{
							this.buffer.setPosition(initialPosition);
							return null;
						}
					}else{
						this.buffer.setPosition(initialPosition);
						return null;
					}
				}else if(acceptByte((byte)'A') != null){
					return new AnsiEscapeSequenceUpArrowKey();
				}else if(acceptByte((byte)'B') != null){
					return new AnsiEscapeSequenceDownArrowKey();
				}else if(acceptByte((byte)'C') != null){
					return new AnsiEscapeSequenceRightArrowKey();
				}else if(acceptByte((byte)'D') != null){
					return new AnsiEscapeSequenceLeftArrowKey();
				}else{
					this.buffer.setPosition(initialPosition);
					return null;
				}
			}else{
				this.buffer.setPosition(initialPosition);
				return null;
			}
		}else{
			return null;
		}
	}

	public AnsiEscapeSequence tryToParseBuffer() throws Exception{
		this.buffer.resetParsingPositions();
		return acceptEscapeSequence();
	}

	public void addToBuffer(byte [] bytes) throws Exception{
		this.buffer.addBytes(bytes);
	}

	public byte [] extractParsedBytes(){
		return this.buffer.extractParsedBytes();
	}

	public byte [] extractNextBytes(int numBytes) throws Exception{
		return this.buffer.extractNextBytes(numBytes);
	}

	public List<Byte> getBuffer(){
		return this.buffer.getBuffer();
	}

	public boolean containsPartiallyParsedSequence() throws Exception{
		return this.buffer.containsPartiallyParsedSequence();
	}

	public boolean wasParsingIncomplete() throws Exception{
		return this.buffer.wasParsingIncomplete();
	}

	public AnsiEscapeSequenceExtractor() throws Exception{

	}
}
