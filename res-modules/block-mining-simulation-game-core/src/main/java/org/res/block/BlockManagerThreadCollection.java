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

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.reflect.ParameterizedType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Date;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.lang.Thread;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.lang.InterruptedException;

public class BlockManagerThreadCollection {

	private boolean shutdownNotifiesSent = false; //  This variable prevents a cascade of 'shutdown' notifies from every other task once shutdown is triggered by one task.
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected Object lock = new Object();
	protected boolean enableJNI;
	protected boolean isProcessFinished = false;
	protected List<Exception> offendingExceptions = new ArrayList<Exception>();
	private Map<Long, BlockManagerThread> allThreads = new HashMap<Long, BlockManagerThread>();
	private List<Long> activeThreadIds = new ArrayList<Long>();
	private CommandLineArgumentCollection commandLineArgumentCollection;

	protected ClientServerInterface clientServerInterface = null;
	private LinuxBlockJNIInterface linuxBlockJNIInterface = null;

	private BlockSchema blockSchema = null;
	private UserInteractionConfig userInteractionConfig = null;
	private Boolean assumeEmojisAreSupported = null;

	public BlockManagerThreadCollection(CommandLineArgumentCollection commandLineArgumentCollection, boolean ensureStdinIsATTY) throws Exception {
		//  This is not very portable, but I actually don't know how many terminals
		//  support advanced emoji characters out there.  Possibly make this guess better in the future:
		logger.info("Observed TERM variable with value '" + String.valueOf(System.getenv("TERM")) + "'.");
		String termVariableContents = System.getenv("TERM") == null ? "" : System.getenv("TERM");
		this.assumeEmojisAreSupported = termVariableContents.contains("xterm");

		this.commandLineArgumentCollection = commandLineArgumentCollection;
		if(this.getIsJNIEnabled()){
			this.linuxBlockJNIInterface = new LinuxBlockJNIInterface();
		}

		String explicitBlockSchemaFile = this.getBlockSchemaFile();
		String blockSchemaFileString = null;
		if(explicitBlockSchemaFile == null){
			blockSchemaFileString = this.loadJarResourceIntoString("/v4_block_schema.json");
		}else{
			blockSchemaFileString = new String(Files.readAllBytes(Paths.get(explicitBlockSchemaFile)), "UTF-8");
		}
		this.blockSchema = new BlockSchema(blockSchemaFileString, this.getAllowUnrecognizedBlockTypes());


		String explicitUserInteractionConfigFile = this.getUserInteractionConfigFile();
		String userInteractionJsonString = null;
		if(explicitUserInteractionConfigFile == null){
			userInteractionJsonString = this.loadJarResourceIntoString("/user_interaction.json");
		}else{
			userInteractionJsonString = new String(Files.readAllBytes(Paths.get(explicitUserInteractionConfigFile)), "UTF-8");
		}
		
		this.userInteractionConfig = new UserInteractionConfig(userInteractionJsonString);
		if(ensureStdinIsATTY){
			this.ensureStdinIsATTY();
		}
	}

	public final void ensureStdinIsATTY() throws Exception {
		if(System.console() == null){
			String message = "System.Console() == null, stdin is probably not a tty!";
			logger.info(message);
			throw new Exception(message);
		}else{
			logger.info("System.Console() != null, stdin is probably a tty.");
		}
	}

	public final String loadJarResourceIntoString(String filename) throws Exception{
		InputStream in = getClass().getResourceAsStream(filename);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[1024*4];

		while ((nRead = in.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		byte [] fileBytes = buffer.toByteArray();
		return new String(fileBytes, "UTF-8");
	}

	public UserInteractionConfig getUserInteractionConfig(){
		return this.userInteractionConfig;
	}

	public void printBlockSchema(){
		//  Print block schema json to standard out:
		System.out.print(this.blockSchema.getInputJsonBlockSchema());
	}

	public void printUserInteractionConfig(){
		//  Print block schema json to standard out:
		System.out.print(this.userInteractionConfig.getInputJson());
	}

	public BlockSchema getBlockSchema() {
		return this.blockSchema;
	}

	public boolean getPrintBlockSchema() {
		return this.commandLineArgumentCollection.hasUsedKey("--print-block-schema");
	}

	public boolean getPrintUserInteractionConfig() {
		return this.commandLineArgumentCollection.hasUsedKey("--print-user-interaction-config");
	}

	public boolean getIsUseASCII() {
		return this.commandLineArgumentCollection.hasUsedKey("--use-ascii");
	}

	public boolean getIsUseEmojis() {
		return this.commandLineArgumentCollection.hasUsedKey("--use-emojis");
	}

	public boolean getRightToLeftPrint() {
		return this.commandLineArgumentCollection.hasUsedKey("--right-to-left-print");
	}

	public GraphicsMode getGraphicsMode() throws Exception{
		if(!this.getIsUseASCII() && !this.getIsUseEmojis()){
			//  Default to whatever support is implied by TERM variable:
			if(this.assumeEmojisAreSupported){
				return GraphicsMode.EMOJI;
			}else{
				return GraphicsMode.ASCII;
			}
		}else if(this.getIsUseASCII() && !this.getIsUseEmojis()){
			//  User requested to only show ASCII
			return GraphicsMode.ASCII;
		}else if(!this.getIsUseASCII() && this.getIsUseEmojis()){
			//  User explicitly requested to show emojis
			return GraphicsMode.EMOJI;
		}else if(this.getIsUseASCII() && this.getIsUseEmojis()){
			throw new Exception("User requested to show emojis and ASCII graphics at the same time.  This doesn't make sense.");
		}else{
			throw new Exception("Unexpected case with command line arguments.");
		}
	}

	public final String getUserInteractionConfigFile() throws Exception {
		return this.commandLineArgumentCollection.getUsedSingleValue("--user-interaction-config-file");
	}

	public final String getBlockSchemaFile() throws Exception {
		return this.commandLineArgumentCollection.getUsedSingleValue("--block-schema-file");
	}

	public final boolean getAllowUnrecognizedBlockTypes() {
		return this.commandLineArgumentCollection.hasUsedKey("--allow-unrecognized-block-types");
	}

	public final boolean getIsJNIEnabled() {
		return !this.commandLineArgumentCollection.hasUsedKey("--disable-jni");
	}

	public LinuxBlockJNIInterface getLinuxBlockJNIInterface(){
		return this.linuxBlockJNIInterface;
	}

	public void addThread(BlockManagerThread t) throws Exception{
		t.start();
		synchronized(lock){
			Long threadId = t.getId();
			if(this.allThreads.containsKey(threadId)){
				throw new Exception("threadId=" + threadId + " already exists.");
			}else{
				this.allThreads.put(threadId, t);
				this.activeThreadIds.add(threadId);
			}
		}
	}

	public void removeThread(BlockManagerThread t) throws Exception{
		synchronized(lock){
			Long threadId = t.getId();
			if(this.allThreads.containsKey(threadId)){
				this.allThreads.remove(threadId);
				int offset = this.activeThreadIds.indexOf(threadId);
				if(offset == -1){
					throw new Exception("Did not find threadId=" + threadId + " in list.");
				}else{
					//logger.info("Before removing threadId=" + threadId + ": this.activeThreadIds=" + this.activeThreadIds);
					this.activeThreadIds.remove(offset);
					//logger.info("After removing threadId=" + threadId + ": this.activeThreadIds=" + this.activeThreadIds);
				}
			}else{
				throw new Exception("Did not find threadId=" + threadId + " in map.");
			}
		}
	}

	public void addThreads(List<BlockManagerThread> threads) throws Exception{
		for(BlockManagerThread t : threads){
			this.addThread(t);
		}
	}

	public List<Exception> getOffendingExceptions(){
		return this.offendingExceptions;
	}

	public BlockManagerThread getThreadById(Long id){
		return this.allThreads.get(id);
	}

	public void sendShutdownNotifies() throws Exception{
		// Initiates an orderly shutdown in which previously submitted tasks are executed, but
		// no new tasks will be accepted. Invocation has no additional effect if already shut down.
		for(int i = 0; i < this.activeThreadIds.size(); i++){
			Long threadId = this.activeThreadIds.get(i);
			BlockManagerThread t = this.allThreads.get(threadId);
			logger.info(Thread.currentThread().getClass().getName() + " " + Thread.currentThread() + " " + Thread.currentThread().getId() + " " + t.getBetterClassName() + " " + t + " " + t.getId() + " == " + Thread.currentThread().equals(t));
			if(t instanceof StandardInputReaderTask){
				try{
					//  Close stdin to trigger exit of 'read' call for keyboard input task:
					logger.info("Calling System.in.close() for thread " + t.getBetterClassName() + "...");
					System.in.close();
				}catch (IOException ex){
					this.offendingExceptions.add(ex);
				}
			}else{
				if(Thread.currentThread().equals(t)){
					logger.info("Don't interrupt the current thread because it's the one interrupting the others: " + t.getBetterClassName() + "...");
				}else{
					logger.info("t.isInterrupted()=" + t.isInterrupted() + ". Calling t.interrupt() on thread " + t.getBetterClassName() + "...");
					t.interrupt();
					logger.info("t.isInterrupted()=" + t.isInterrupted() + " now on thread " + t.getBetterClassName() + "...");
				}
			}
		}
	}

	public void setIsProcessFinished(boolean isProcessFinished, Exception e){
		synchronized(lock){
			this.isProcessFinished = isProcessFinished;
			if(e != null){
				this.offendingExceptions.add(e);
			}
			if(this.isProcessFinished){
				try{
					if(!this.shutdownNotifiesSent){
						this.sendShutdownNotifies();
						this.shutdownNotifiesSent = true;
						//  Wake up threads that are blocked getting signals so they can shut down
						if(this.getIsJNIEnabled()){
							this.getLinuxBlockJNIInterface().shutdownInXMilliseconds(0);
						}
					}
				}catch(Exception exception){
					exception.printStackTrace();
				}
			}
		}
	}

	public void blockUntilAllTasksHaveTerminated() throws Exception{
		//  Wait for all threads to finish:
		int numActiveThreads = this.activeThreadIds.size();
		while(numActiveThreads > 0){
			try{
				BlockManagerThread t = null;
				Long threadId = null;
				synchronized(lock){
					threadId = this.activeThreadIds.get(0);
					t = this.allThreads.get(threadId);
				}
				t.join();
				//  Only remove it from the list after it's finished:
				synchronized(lock){
					this.activeThreadIds.remove(0);
					//logger.info("blockUntilAllTasksHaveTerminated, Before removing threadId=" + threadId + ": this.activeThreadIds=" + this.activeThreadIds);
					this.allThreads.remove(threadId);
					//logger.info("blockUntilAllTasksHaveTerminated, After removing threadId=" + threadId + ": this.activeThreadIds=" + this.activeThreadIds);
					if(!(t.getId() == threadId)){
						throw new Exception("t.getId()=" + t.getId() + " != threadId=" + threadId);
					}
					logger.info("After thread join, removed threadId=" + threadId);
					numActiveThreads = this.activeThreadIds.size();
				}
			}catch(InterruptedException ex){
			}
		}
	}

	public boolean getIsProcessFinished(){
		return this.isProcessFinished;
	}
}
