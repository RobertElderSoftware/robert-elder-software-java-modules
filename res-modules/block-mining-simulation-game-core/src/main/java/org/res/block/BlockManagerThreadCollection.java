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

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.reflect.ParameterizedType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.StringWriter;
import java.io.PrintWriter;

import java.io.EOFException;
import java.io.IOException;
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
import javax.websocket.Session;
import javax.websocket.CloseReason;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.OnError;
import javax.websocket.Session;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.lang.InterruptedException;

public class BlockManagerThreadCollection {

	private boolean shutdownNotifiesSent = false; //  This variable prevents a cascade of 'shutdown' notifies from every other task once shutdown is triggered by one task.
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected Object lock = new Object();
	protected Object messageLock = new Object();
	protected boolean enableJNI;
	protected boolean isProcessFinished = false;
	protected List<Exception> offendingExceptions = new ArrayList<Exception>();
	private Map<Long, BlockManagerThread> allThreads = new HashMap<Long, BlockManagerThread>();
	private List<Long> activeThreadIds = new ArrayList<Long>();
	private CommandLineArgumentCollection commandLineArgumentCollection;

	private LinuxBlockJNIInterface linuxBlockJNIInterface = null;

	private ConsoleWriterThreadState consoleWriterThreadState;
	private SIGWINCHListenerThreadState sigwinchListenerThreadState;
	private BlockSchema blockSchema = null;
	private UserInteractionConfig userInteractionConfig = null;
	private Boolean assumeEmojisAreSupported = null;
	private Map<BlockWorldConnectionParameters, BlockWorldConnection> blockWorldConnections = new HashMap<BlockWorldConnectionParameters, BlockWorldConnection>();
	private Map<BlockWorldConnectionParameters, Map<Long, AuthorizedBlockWorldConnection>> authorizedBlockWorldConnections = new HashMap<BlockWorldConnectionParameters, Map<Long, AuthorizedBlockWorldConnection>>();

	private Map<BlockWorldConnectionParameters, InMemoryChunks> loadedWorldChunks = new HashMap<BlockWorldConnectionParameters, InMemoryChunks>();
	private ByteArrayOutputStream partialBinaryMessage = new ByteArrayOutputStream();

	/*  This defines the dimensions of the 'chunks' that are loaded into memory as we move around */
	private final CuboidAddress chunkSizeCuboidAddress = new CuboidAddress(new Coordinate(Arrays.asList(0L, 0L, 0L, 0L)), new Coordinate(Arrays.asList(3L, 3L, 5L, 1L)));

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
			blockSchemaFileString = this.loadJarResourceIntoString("/v6_block_schema.json");
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

	public final void init() throws Exception {
		//  Console Writer Thread
		this.consoleWriterThreadState = new ConsoleWriterThreadState(this);
		this.consoleWriterThreadState.putWorkItem(new InitializeYourselfConsoleWriterWorkItem(this.consoleWriterThreadState), WorkItemPriority.PRIORITY_LOW);
		this.addThread(new WorkItemProcessorTask<ConsoleWriterWorkItem>(this.consoleWriterThreadState, ConsoleWriterWorkItem.class, this.consoleWriterThreadState.getClass()));


		//  Console Reader Thread
		this.addThread(new StandardInputReaderTask(getConsoleWriterThreadState()));

		//  SIGWINCHListener Thread
		if(this.getIsJNIEnabled()){
			this.sigwinchListenerThreadState = new SIGWINCHListenerThreadState(this);
			this.addThread(new WorkItemProcessorTask<SIGWINCHListenerWorkItem>(this.sigwinchListenerThreadState, SIGWINCHListenerWorkItem.class, this.sigwinchListenerThreadState.getClass()));
		}
	}

	public void connectAndStart() throws Exception{

		if(authorizedBlockWorldConnections.entrySet().size() == 0){
			//  Need to set something to get UI to show up:
			getConsoleWriterThreadState().putWorkItem(new TellClientTerminalChangedWorkItem(getConsoleWriterThreadState()), WorkItemPriority.PRIORITY_LOW);
		}else{
			for(Map.Entry<BlockWorldConnectionParameters, Map<Long, AuthorizedBlockWorldConnection>> worldConnectionEntry : authorizedBlockWorldConnections.entrySet()){
				BlockWorldConnectionParameters params = worldConnectionEntry.getKey();
				Map<Long, AuthorizedBlockWorldConnection> authorizedClients = worldConnectionEntry.getValue();
				BlockWorldConnection worldConnection = getBlockWorldConnectionByParams(params);
				//  Set up the connection to the world for all authorized
				//  clients in that world:
				worldConnection.getCommunicationProcessor().worldConnect();
				//  Now set up a connection for each individual authorized client:
				for(Map.Entry<Long, AuthorizedBlockWorldConnection> playerConnectionEntry : authorizedClients.entrySet()){
					//  Connect every client:
					ClientBlockModelContext clientBlockModelContext = playerConnectionEntry.getValue().getClientBlockModelContext();
					clientBlockModelContext.authorizedClientConnect();
					clientBlockModelContext.startRunningClient();
				}
			}
		}
	}

	public ConsoleWriterThreadState getConsoleWriterThreadState(){
		return this.consoleWriterThreadState;
	}

	public List<Map.Entry<BlockWorldConnectionParameters, BlockWorldConnection>> getAllBlockWorldConnectionEntries(){
		return new ArrayList<Map.Entry<BlockWorldConnectionParameters, BlockWorldConnection>>(blockWorldConnections.entrySet());
	}

	public final BlockWorldConnection getBlockWorldConnectionByParams(BlockWorldConnectionParameters params) throws Exception{
		return this.blockWorldConnections.get(params);
	}

	public final BlockWorldConnection makeOrGetBlockWorldConnection(BlockWorldConnectionParameters params, SessionOperationInterface sessionOperationInterface) throws Exception{
		
		if(!this.blockWorldConnections.containsKey(params)){
			if(params instanceof DatabaseBlockWorldConnectionParameters){
				DatabaseBlockWorldConnection bwc = new DatabaseBlockWorldConnection(this, sessionOperationInterface, (DatabaseBlockWorldConnectionParameters)params);
				bwc.init();
				this.blockWorldConnections.put(params, bwc);
			}else if(params instanceof WebsocketBlockWorldConnectionParameters){
				WebsocketBlockWorldConnection bwc = new WebsocketBlockWorldConnection(this, sessionOperationInterface, (WebsocketBlockWorldConnectionParameters)params);
				bwc.init();
				this.blockWorldConnections.put(params, bwc);
			}else{
				throw new Exception("Unexpected params type.");
			}

			//  Start a thread for loading/unloading chunks for this world:
			Long maxPendingChunks = 2L;
			InMemoryChunks imc = new InMemoryChunks(this, chunkSizeCuboidAddress, maxPendingChunks);
			loadedWorldChunks.put(params, imc);
			imc.putWorkItem(new InitializeYourselfInMemoryChunksWorkItem(imc), WorkItemPriority.PRIORITY_LOW);
			addThread(new WorkItemProcessorTask<InMemoryChunksWorkItem>(imc, InMemoryChunksWorkItem.class, imc.getClass()));
		}
		return this.blockWorldConnections.get(params);
	}

	public byte [] getBlockDataForClass(Class<?> c) throws Exception{
		if(this.getBlockSchema() == null){
			throw new Exception("Cannot lookup byte pattern for  '" + c.getName() + "' because block schema has not been initialized yet.");
		}else{
			return this.getBlockSchema().getBinaryDataForByteComparisonBlockForClass(c);
		}
	}

	public InMemoryChunks getInMemoryChunksForWorld(BlockWorldConnectionParameters params){
		return loadedWorldChunks.get(params);
	}

	public final AuthorizedBlockWorldConnection getAuthorizedBlockWorldConnection(Long authorizedClientId, BlockWorldConnectionParameters params) throws Exception{
		if(this.authorizedBlockWorldConnections.get(params).containsKey(authorizedClientId)){
			return this.authorizedBlockWorldConnections.get(params).get(authorizedClientId);
		}else{
			throw new Exception("Unknown authorizedClientId=" + authorizedClientId);
		}
	}

	public final AuthorizedBlockWorldConnection makeOrGetAuthorizedBlockWorldConnection(Long authorizedClientId, BlockWorldConnectionParameters params) throws Exception{
		
		if(!this.authorizedBlockWorldConnections.containsKey(params)){
			this.authorizedBlockWorldConnections.put(params, new HashMap<Long, AuthorizedBlockWorldConnection>());
		}
		if(!this.authorizedBlockWorldConnections.get(params).containsKey(authorizedClientId)){
			AuthorizedBlockWorldConnection abwc = new AuthorizedBlockWorldConnection(this, authorizedClientId, blockWorldConnections.get(params));
			abwc.init();
			this.authorizedBlockWorldConnections.get(params).put(authorizedClientId, abwc);
		}
		return this.authorizedBlockWorldConnections.get(params).get(authorizedClientId);
	}

	public final List<ClientBlockModelContext> getClientBlockModelContexts(){
		for(Map.Entry<BlockWorldConnectionParameters, Map<Long, AuthorizedBlockWorldConnection>> worldConnectionEntry : authorizedBlockWorldConnections.entrySet()){
			for(Map.Entry<Long, AuthorizedBlockWorldConnection> playerConnectionEntry : worldConnectionEntry.getValue().entrySet()){
				ClientBlockModelContext clientBlockModelContext = playerConnectionEntry.getValue().getClientBlockModelContext();
				return Arrays.asList(clientBlockModelContext);
			}
		}
		return new ArrayList<ClientBlockModelContext>();
	}

	public final void ensureStdinIsATTY() throws Exception {
		/*if(System.console() == null){
			String message = "System.Console() == null, stdin is probably not a tty!";
			logger.info(message);
			throw new Exception(message);
		}else{
			logger.info("System.Console() != null, stdin is probably a tty.");
		}*/
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

	public Integer getFixedWidth() throws Exception {
		if(this.commandLineArgumentCollection.hasUsedKey("--fixed-width")){
			return Integer.valueOf(this.commandLineArgumentCollection.getUsedSingleValue("--fixed-width"));
			
		}else{
			return null;
		}
	}

	public Integer getFixedHeight() throws Exception {
		if(this.commandLineArgumentCollection.hasUsedKey("--fixed-height")){
			return Integer.valueOf(this.commandLineArgumentCollection.getUsedSingleValue("--fixed-height"));
			
		}else{
			return null;
		}
	}

	public Integer getCompatibilityWidth() throws Exception {
		if(this.commandLineArgumentCollection.hasUsedKey("--compatibility-width")){
			return Integer.valueOf(this.commandLineArgumentCollection.getUsedSingleValue("--compatibility-width"));
			
		}else{
			return null;
		}
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
			Long threadId = t.threadId();
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
			Long threadId = t.threadId();
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
			logger.info(Thread.currentThread().getClass().getName() + " " + Thread.currentThread() + " " + Thread.currentThread().threadId() + " " + t.getBetterClassName() + " " + t + " " + t.threadId() + " == " + Thread.currentThread().equals(t));
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
					if(!(t.threadId() == threadId)){
						throw new Exception("t.threadId()=" + t.threadId() + " != threadId=" + threadId);
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

	public void setupDefaultUIForClient(ClientBlockModelContext client) throws Exception{

		boolean useMultiSplitDemo = false;
		ConsoleWriterThreadState cwts = getConsoleWriterThreadState();
		Long existingRootSplitId = cwts.getRootSplitId();
		if(useMultiSplitDemo){
			List<Long> splits1 = new ArrayList<Long>();
			splits1.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(MapAreaInterfaceThreadState.class, client)));
			splits1.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(InventoryInterfaceThreadState.class, client)));
			splits1.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(EmptyFrameThreadState.class, client)));

			List<Long> splits2 = new ArrayList<Long>();
			splits2.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(EmptyFrameThreadState.class, client)));
			splits2.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(EmptyFrameThreadState.class, client)));
			splits2.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(EmptyFrameThreadState.class, client)));

			List<Long> splits3 = new ArrayList<Long>();
			splits3.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(EmptyFrameThreadState.class, client)));
			splits3.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(EmptyFrameThreadState.class, client)));
			splits3.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(EmptyFrameThreadState.class, client)));

			List<Long> topSplits = new ArrayList<Long>();
			Long h1 = cwts.makeHorizontalSplit();
			cwts.addSplitPartsByIds(h1, splits1);
			topSplits.add(h1);

			Long h2 = cwts.makeHorizontalSplit();
			cwts.addSplitPartsByIds(h2, splits2);
			topSplits.add(h2);

			Long h3 = cwts.makeHorizontalSplit();
			cwts.addSplitPartsByIds(h3, splits3);
			topSplits.add(h3);

			Long top = cwts.makeVerticalSplit();
			cwts.addSplitPartsByIds(top, topSplits);
			cwts.setRootSplit(top);
		}else{
			List<Double> framePercents = new ArrayList<Double>();
			List<Long> splits = new ArrayList<Long>();
			splits.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(MapAreaInterfaceThreadState.class, client)));
			splits.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(InventoryInterfaceThreadState.class, client)));
			if(existingRootSplitId == null){
				framePercents.add(0.75);
				framePercents.add(0.25);
			}else{
				splits.add(existingRootSplitId);
				framePercents.add(0.60);
				framePercents.add(0.20);
				framePercents.add(0.20);
			}


			Long subSplit = cwts.makeHorizontalSplit();
			cwts.addSplitPartsByIds(subSplit, splits);
			((UserInterfaceSplitMulti)cwts.getUserInterfaceSplitById(subSplit)).setSplitPercentages(framePercents);

			Long root = cwts.makeVerticalSplit();
			List<Long> topSplits = new ArrayList<Long>();
			topSplits.add(subSplit);
			topSplits.add(cwts.makeLeafNodeSplit(cwts.createFrameAndThread(CraftingInterfaceThreadState.class, client)));
			List<Double> topSplitPercents = new ArrayList<Double>();
			topSplitPercents.add(0.75);
			topSplitPercents.add(0.25);
			cwts.addSplitPartsByIds(root, topSplits);
			((UserInterfaceSplitMulti)cwts.getUserInterfaceSplitById(root)).setSplitPercentages(topSplitPercents);

			cwts.setRootSplit(root);
		}
	}

	public void shutdownAllClientsAndServers() throws Exception{
		for(Map.Entry<BlockWorldConnectionParameters, BlockWorldConnection> worldConnectionEntry : blockWorldConnections.entrySet()){
			worldConnectionEntry.getValue().destroy();
		}

		for(Map.Entry<BlockWorldConnectionParameters, Map<Long, AuthorizedBlockWorldConnection>> worldConnectionEntry : authorizedBlockWorldConnections.entrySet()){
			for(Map.Entry<Long, AuthorizedBlockWorldConnection> playerConnectionEntry : worldConnectionEntry.getValue().entrySet()){
				playerConnectionEntry.getValue().destroy();
			}
		}
	}

	public void onOpen(BlockWorldConnection blockWorldConnection, BlockSession blockSession) throws Exception {
		logger.info("Opened a session id=" + blockSession.getId());
		blockWorldConnection.addBlockSession(blockSession);
		if(blockSession instanceof WebsocketBlockSession){
			logger.info(String.format("Opened websocket session for id '%s'.", blockSession.getId()));
			((WebsocketBlockSession)blockSession).setMaxBinaryMessageBufferSize(1024*4);
		}else if(blockSession instanceof LocalBlockSession){
			// Do nothing.
		}else{
			throw new Exception("Expected sesion to be of type WebsocketBlockSession but it had type " + blockSession.getClass().getName());
		}
	}

	public void onBlockModelMessage(BlockModelContext blockModelContext, BlockMessage blockMessage, BlockSession blockSession) throws Exception {
		ProcessBlockMessageWorkItem w = new ProcessBlockMessageWorkItem(blockModelContext, blockSession, blockMessage);
		blockModelContext.putWorkItem(w, WorkItemPriority.PRIORITY_LOW);
	}

	public void onBinaryMessage(BlockWorldConnection bwc, byte[] inputBytes, boolean last, BlockSession blockSession) throws Exception {
		synchronized(messageLock){
			partialBinaryMessage.write(inputBytes);
			logger.info("Partial message received of length " + inputBytes.length + " last=" + last);
			if(last){
				byte [] messageByteArray = partialBinaryMessage.toByteArray();
				long authorizedClientId = BlockMessage.extractAuthorizedClientId(new BlockMessageBinaryBuffer(messageByteArray, 0));
				AuthorizedBlockWorldConnection abwc = this.getAuthorizedBlockWorldConnection(authorizedClientId, bwc.getBlockWorldConnectionParameters());
				BlockModelContext blockModelContext = abwc.getClientBlockModelContext();
				BlockMessage blockMessage = BlockMessage.consumeBlockMessage(blockModelContext, new BlockMessageBinaryBuffer(messageByteArray, 0));

				this.onBlockModelMessage(blockModelContext, blockMessage, blockSession);
				this.partialBinaryMessage = new ByteArrayOutputStream();  //  Clear the buffer.
			}
		}
	}

	public void onBinaryMessage(BlockModelContext blockModelContext, byte[] inputBytes, boolean last, BlockSession blockSession) throws Exception {
		synchronized(messageLock){
			partialBinaryMessage.write(inputBytes);
			logger.info("Partial message received of length " + inputBytes.length + " last=" + last);
			if(last){
				byte [] messageByteArray = partialBinaryMessage.toByteArray();
				BlockMessage blockMessage = BlockMessage.consumeBlockMessage(blockModelContext, new BlockMessageBinaryBuffer(messageByteArray, 0));

				this.onBlockModelMessage(blockModelContext, blockMessage, blockSession);
				this.partialBinaryMessage = new ByteArrayOutputStream();  //  Clear the buffer. 
			}
		}
	}

	public void onClose(BlockWorldConnection blockWorldConnection, String reason, String sessionId) throws Exception {
		logger.info("In onClose for blockSession id = '" + String.valueOf(sessionId) + "' reason = " + reason);
		blockWorldConnection.removeBlockSession(sessionId);
		logger.info(String.format("Closed websocket session for id '%s' for reason '%s'.", sessionId, reason));	
	}

	public void onError(BlockWorldConnection blockWorldConnection, String sessionId, Throwable throwable) throws Throwable {
		blockWorldConnection.removeBlockSession(sessionId);
		if(throwable instanceof EOFException){
			logger.info(String.format("Observed an '%s' error in webSocket session '%s'.  Continuing...", throwable.getClass().getName(), sessionId), throwable);
		}else if(throwable instanceof IOException){
			logger.info(String.format("Observed an '%s' error in webSocket session '%s'.  Continuing...", throwable.getClass().getName(), sessionId), throwable);
		}else{
			logger.info(String.format("Observed unexpeced '%s' error in webSocket session '%s'.  Throwing exception...", throwable.getClass().getName(), sessionId));
			throw new Exception("Session experienced this unhandled throwable:", throwable);
		}
	}

	public static String exceptionToString(Throwable t){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
}
