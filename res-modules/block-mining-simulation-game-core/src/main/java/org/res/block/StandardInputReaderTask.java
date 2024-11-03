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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;

import org.res.block.Coordinate;
import java.io.IOException;
import java.io.FileDescriptor;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class StandardInputReaderTask extends BlockManagerThread {

	private BlockModelContext blockModelContext;
	private ClientBlockModelContext clientBlockModelContext;
	private ConsoleWriterThreadState consoleWriterThreadState;
	private AnsiEscapeSequenceExtractor ansiEscapeSequenceExtractor = new AnsiEscapeSequenceExtractor();
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public StandardInputReaderTask(ClientBlockModelContext clientBlockModelContext, ConsoleWriterThreadState consoleWriterThreadState) throws Exception{
		this.clientBlockModelContext = clientBlockModelContext;
		this.consoleWriterThreadState = consoleWriterThreadState;
	}

	@Override
	public String getBetterClassName() throws Exception{
		return this.getClass().getName();
	}

	public void setTTYMode(List<String> commandParts) throws Exception{
		ShellProcessRunner r = new ShellProcessRunner(commandParts);
		ShellProcessFinalResult result = r.getFinalResult();
		if(result.getReturnValue() == 0){
			logger.info("Command '" + commandParts + "' ran with success:");
		}else{
			logger.info(new String(result.getOutput().getStdoutOutput(), "UTF-8"));
			logger.info(new String(result.getOutput().getStderrOutput(), "UTF-8"));
		}
	}

	public void setToRawMode() throws Exception{
		this.setTTYMode(Arrays.asList("stty", "raw", "-echo", "-F", "/dev/tty"));
	}

	public void setToCookedMode() throws Exception{
		this.setTTYMode(Arrays.asList("stty", "cooked", "echo", "-F", "/dev/tty"));
	}

	public boolean onInputBytes(byte [] bytes) throws Exception{
		logger.info("onInputByte: " + BlockModelContext.convertToHex(bytes));
		this.ansiEscapeSequenceExtractor.addToBuffer(bytes);
		while(this.ansiEscapeSequenceExtractor.getBuffer().size() > 0){ //  While there is still something to parse.
			AnsiEscapeSequence escapeSequence = this.ansiEscapeSequenceExtractor.tryToParseBuffer();
			if(escapeSequence == null){
				if(this.ansiEscapeSequenceExtractor.wasParsingIncomplete()){
					//  This case happens when we encounter a control sequence we haven't considered yet.
					//  Just extract the portion of the control sequence that we were able to parse,
					//  and then handle any remaining characters like normal input.  This isn't ideal,
					//  but it's better than letting the input get blocked forever due to the parser
					//  repeatedly failing to match the expected ANSI escape sequence patterns:
					byte [] extracted = this.ansiEscapeSequenceExtractor.extractParsedBytes();
					logger.info("->Parsing was incomplete!  Discarding these bytes: " + BlockModelContext.convertToHex(extracted));
				}if(this.ansiEscapeSequenceExtractor.containsPartiallyParsedSequence()){
					//  Single escape character?
					if(this.ansiEscapeSequenceExtractor.getBuffer().size() == 1 && this.ansiEscapeSequenceExtractor.getBuffer().get(0) == 0x1b){
						byte [] extracted = this.ansiEscapeSequenceExtractor.extractNextBytes(1);
						ProcessStandardInputBytesWorkItem workItem = new ProcessStandardInputBytesWorkItem(this.consoleWriterThreadState, extracted);
						this.consoleWriterThreadState.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
					}else{
						logger.info("->Input buffer contains partially parsed ansi sequence=" + String.valueOf(this.ansiEscapeSequenceExtractor.getBuffer()));
						return false;  // Nothing more to do.
					}
				}else{
					byte [] extracted = this.ansiEscapeSequenceExtractor.extractNextBytes(1);
					logger.info("->There is just regular text in the buffer: " + BlockModelContext.convertToHex(extracted));
					ProcessStandardInputBytesWorkItem workItem = new ProcessStandardInputBytesWorkItem(this.consoleWriterThreadState, extracted);
					this.consoleWriterThreadState.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
				}
			}else{
				//  If we successfully parsed an ANSI escape sequence, extract those characters from the stream:
				byte [] extracted = this.ansiEscapeSequenceExtractor.extractParsedBytes();
				logger.info("Got escape sequence, extracted=" + BlockModelContext.convertToHex(extracted));
				ProcessStandardInputAnsiEscapeSequenceWorkItem wm = new ProcessStandardInputAnsiEscapeSequenceWorkItem(this.consoleWriterThreadState, escapeSequence);
				this.consoleWriterThreadState.putWorkItem(wm, WorkItemPriority.PRIORITY_LOW);
			}
		}
		return false;
	}

	@Override
	public void run() {
		logger.info("Begin running StandardInputReaderTask (" + Thread.currentThread() + ") for " + this.clientBlockModelContext.getClass().getName());
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
					logger.info("Before input read, stdinChannel.size()=" + stdinChannel.size() + "... ");
					int read_rtn = stdinChannel.read(buffer);
					logger.info("After input read... " + read_rtn);
					if(read_rtn == -1){ //  When System.in has been closed
						inputReadingFinished = true;
					}else{
						byte[] bytes_read = new byte[read_rtn];
						buffer.rewind();
						buffer.get(bytes_read);
						logger.info("Bytes read are " + BlockModelContext.convertToHex(bytes_read));
						inputReadingFinished = this.onInputBytes(bytes_read);
					}
				}while (!inputReadingFinished && !clientBlockModelContext.getBlockManagerThreadCollection().getIsProcessFinished() && !Thread.currentThread().isInterrupted());
			}catch(AsynchronousCloseException e){
				//  This is expected during normal shutdown.
			}
			this.setToCookedMode();
		}catch(Exception e){
			try{
				this.setToCookedMode();
			}catch(Exception ee){
				logger.info("Unable to restore cooked mode.");
			}

			logger.info("run method terminated with an exception.");
			clientBlockModelContext.getBlockManagerThreadCollection().setIsProcessFinished(true, e);
		}

		//  Put cursor position to top of screen:
		System.out.println("\033[1;1H\033[0m");

		logger.info("Finished running StandardInputReaderTask, exiting thread for " + this.clientBlockModelContext.getClass().getName());
	}
}
