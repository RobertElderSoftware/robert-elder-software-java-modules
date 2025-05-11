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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ShellProcessReaderThread implements Callable<byte []> {
	private String streamName;
	private InputStream inputStream;
	private byte [] bufferedOutput = new byte [0];

	public ShellProcessReaderThread(String streamName, InputStream inputStream){
		this.streamName = streamName;
		this.inputStream = inputStream;
	}

	public byte [] readPartialResult(){
		synchronized (this.bufferedOutput){
			byte [] rtn = this.bufferedOutput.clone();
			this.bufferedOutput = new byte[0];
			return rtn;
		}
	}

	@Override
	public byte [] call() throws Exception{
		try{
			int bufferSize = 4096;
			byte [] buffer = new byte [bufferSize];
			boolean done = false;
			while (!done) {
				int numBytesRead = inputStream.read(buffer, 0, bufferSize);
				if(numBytesRead == -1){
					done = true;
				}else{
					synchronized (this.bufferedOutput){
						byte[] tmp = new byte[this.bufferedOutput.length + numBytesRead];
						System.arraycopy(this.bufferedOutput, 0, tmp, 0, this.bufferedOutput.length);
						System.arraycopy(buffer, 0, tmp, this.bufferedOutput.length, numBytesRead);
						this.bufferedOutput = tmp;
					}
				}
			}
			inputStream.close();	
		}catch (Exception e){
			throw new Exception("Caught an exception in reading input stream name " + this.streamName);
		}
		return this.bufferedOutput;
	}
}
