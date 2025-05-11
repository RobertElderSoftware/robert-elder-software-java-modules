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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class BlockMessageBinaryBuffer {

	private byte [] data = new byte [1];
	private int usedCapacity = 0;
	private int readPosition = 0;
	private int availableCapacity = 1;

	public long [] readNLongValues(int numberOfLongValues){
		ByteBuffer messageTypeBuffer = ByteBuffer.wrap(this.data, this.readPosition, Long.BYTES * numberOfLongValues);
		LongBuffer longBuffer = messageTypeBuffer.asLongBuffer();
		long[] l = new long[longBuffer.capacity()];
		longBuffer.get(l);
		this.readPosition += Long.BYTES * numberOfLongValues;
		return l;
	}

	public void writeLongValues(long [] values){
		ByteBuffer bb = ByteBuffer.allocate(values.length * Long.BYTES);
		bb.asLongBuffer().put(values);
		this.writeBytes(bb.array());
	}

	public byte [] getUsedBuffer(){
		byte [] newData = new byte [this.usedCapacity];
		System.arraycopy(this.data, 0, newData, 0, this.usedCapacity);
		return newData;
	}

	public void increaseBufferCapacity(int requiredCapacity){
		int newCapacity = this.availableCapacity;
		while(newCapacity < requiredCapacity) {
			newCapacity *= 2;
		}
		byte [] newData = new byte [newCapacity];
		System.arraycopy(data, 0, newData, 0, this.usedCapacity);
		this.data = newData;
		this.availableCapacity = newCapacity;
	}

	public void writeBytes(byte [] dataToWrite){
		this.increaseBufferCapacity(this.usedCapacity + dataToWrite.length);
		System.arraycopy(dataToWrite, 0, this.data, this.usedCapacity, dataToWrite.length);
		this.usedCapacity += dataToWrite.length;
	}

	public byte [] readNBytes(int numBytes){
		byte [] rtn = new byte[numBytes];
		System.arraycopy(this.data, this.readPosition, rtn, 0, numBytes);
		this.readPosition += numBytes;
		return rtn;
	}

	public long readOneLongValue(){
		long [] values = this.readNLongValues(1);
		return values[0];
	}

	public void writeOneLongValue(long v){
		long [] values = new long[1];
		values[0] = v;
		writeLongValues(values);
	}

	public BlockMessageBinaryBuffer(byte [] data, int usedCapacity) {
		this.data = data;
		this.usedCapacity = usedCapacity;
		this.availableCapacity = data.length;
	}

	public BlockMessageBinaryBuffer() {
	}
}
