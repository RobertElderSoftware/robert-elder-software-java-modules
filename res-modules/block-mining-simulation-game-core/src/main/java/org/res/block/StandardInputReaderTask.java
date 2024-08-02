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

import java.lang.Runnable;
import java.lang.Thread;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.res.block.Coordinate;
import java.io.IOException;
import java.io.FileDescriptor;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.util.List;
import java.util.ArrayList;


public class StandardInputReaderTask extends Thread {

	private BlockModelContext blockModelContext;
	private ClientBlockModelContext clientBlockModelContext;
	private CharacterWidthMeasurementThreadState characterWidthMeasurementThreadState;
	private AnsiEscapeSequenceExtractor ansiEscapeSequenceExtractor = new AnsiEscapeSequenceExtractor();

	public StandardInputReaderTask(ClientBlockModelContext clientBlockModelContext, CharacterWidthMeasurementThreadState characterWidthMeasurementThreadState) throws Exception{
		this.clientBlockModelContext = clientBlockModelContext;
		this.characterWidthMeasurementThreadState = characterWidthMeasurementThreadState;
	}

	public void setTTYMode(List<String> commandParts) throws Exception{
		ShellProcessRunner r = new ShellProcessRunner(commandParts);
		ShellProcessFinalResult result = r.getFinalResult();
		if(result.getReturnValue() == 0){
			this.clientBlockModelContext.logMessage("Command '" + commandParts + "' ran with success:");
		}else{
			this.clientBlockModelContext.logMessage(new String(result.getOutput().getStdoutOutput(), "UTF-8"));
			this.clientBlockModelContext.logMessage(new String(result.getOutput().getStderrOutput(), "UTF-8"));
		}
	}

	public void setToRawMode() throws Exception{
		this.setTTYMode(Arrays.asList("stty", "raw", "-echo", "-F", "/dev/tty"));
	}

	public void setToCookedMode() throws Exception{
		this.setTTYMode(Arrays.asList("stty", "cooked", "echo", "-F", "/dev/tty"));
	}

	public boolean onInputByte(byte b) throws Exception{
		clientBlockModelContext.logMessage("onInputByte: " + String.valueOf(b));
		this.ansiEscapeSequenceExtractor.addToBuffer(b);
		while(this.ansiEscapeSequenceExtractor.getBuffer().size() > 0){ //  While there is still something to parse.
			CursorPositionReport cpr = (CursorPositionReport)this.ansiEscapeSequenceExtractor.tryToParseBuffer();
			if(cpr == null){
				if(this.ansiEscapeSequenceExtractor.containsPartiallyParsedSequence()){
					clientBlockModelContext.logMessage("->Input buffer contains partially parsed ansi sequence=" + String.valueOf(this.ansiEscapeSequenceExtractor.getBuffer()));
					return false;  // Nothing more to do.
				}else{
					byte [] extracted = new byte [1];
					extracted[0] = this.ansiEscapeSequenceExtractor.extractNextByte();
					clientBlockModelContext.logMessage("->There is just regular text in the buffer: " + String.valueOf(extracted[0]));
					boolean inputReadingFinished = this.clientBlockModelContext.onKeyboardInput(extracted);
					if(inputReadingFinished){
						return inputReadingFinished;
					}
				}
			}else{
				//  If we successfully parsed an ANSI escape sequence, extract those characters from the stream:
				List<Byte> extracted = this.ansiEscapeSequenceExtractor.extractParsedBytes();
				clientBlockModelContext.logMessage("Got Character Position Report: x=" + cpr.getX() + ", y=" + cpr.getY() + ", extracted=" + String.valueOf(extracted));

				CursorPositionReportWorkItem wm = new CursorPositionReportWorkItem(this.characterWidthMeasurementThreadState, cpr.getX(), cpr.getY());
				this.characterWidthMeasurementThreadState.putWorkItem(wm, WorkItemPriority.PRIORITY_LOW);
			}
		}
		return false;
	}

	@Override
	public void run() {
		clientBlockModelContext.logMessage("Begin running StandardInputReaderTask (" + Thread.currentThread() + ") for " + this.clientBlockModelContext.getClass().getName());
		try{
			try{
				this.setToRawMode();
				boolean inputReadingFinished = false;

				//  Using 'System.in' directly results in blocking reads that can't be interrupted (for graceful shutdown),
				//  so use the 'FileChannel' approach instead which is interruptable.
				FileInputStream stdin = new FileInputStream(FileDescriptor.in);
				FileChannel stdinChannel = stdin.getChannel();
				do{
					int buffer_len = 1024;
					ByteBuffer buffer = ByteBuffer.allocate(buffer_len);
					clientBlockModelContext.logMessage("Before input read, stdinChannel.size()=" + stdinChannel.size() + "... ");
					int read_rtn = stdinChannel.read(buffer);
					clientBlockModelContext.logMessage("After input read... " + read_rtn);
					if(read_rtn == -1){ //  When System.in has been closed
						inputReadingFinished = true;
					}else{
						byte[] bytes_read = new byte[read_rtn];
						buffer.rewind();
						buffer.get(bytes_read);
						clientBlockModelContext.logMessage("Bytes read are " + BlockModelContext.convertToHex(bytes_read));
						for(int i = 0; i < read_rtn; i++){
							inputReadingFinished = this.onInputByte(bytes_read[i]);
							if(inputReadingFinished){
								break;
							}
						}
					}
				}while (!inputReadingFinished && !clientBlockModelContext.getBlockManagerThreadCollection().getIsFinished() && !Thread.currentThread().isInterrupted());
			}catch(ClosedByInterruptException e){
				//  This is expected during normal shutdown.
			}
			this.setToCookedMode();
		}catch(Exception e){
			try{
				this.setToCookedMode();
			}catch(Exception ee){
				clientBlockModelContext.logMessage("Unable to restore cooked mode.");
			}

			clientBlockModelContext.logMessage("Exception:");
			e.printStackTrace();
		}

		//  Put cursor position to top of screen:
		System.out.println("\033[1;1H\033[0m");

		clientBlockModelContext.logMessage("Finished running StandardInputReaderTask, exiting thread for " + this.clientBlockModelContext.getClass().getName());
	}
}
