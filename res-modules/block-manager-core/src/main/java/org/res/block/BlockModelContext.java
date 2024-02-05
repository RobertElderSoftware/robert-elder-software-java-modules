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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public abstract class BlockModelContext extends WorkItemQueueOwner<BlockModelContextWorkItem> {

	public abstract BlockModelInterface getBlockModelInterface();
	public abstract boolean isServer();
	public abstract void postCuboidsWrite(Long numDimensions, List<CuboidAddress> cuboidAddresses) throws Exception;
	public abstract void sendBlockMessage(BlockMessage m, BlockSession session) throws Exception;

	public abstract void inMemoryChunksCallbackOnChunkWasWritten(CuboidAddress cuboidAddress) throws Exception;
	public abstract void inMemoryChunksCallbackOnChunkBecomesPending(CuboidAddress cuboidAddress) throws Exception;

	protected BlockManagerThreadCollection blockManagerThreadCollection = null;
	private ConcurrentHashMap<String, BlockSession> sessionMap = new ConcurrentHashMap<String, BlockSession>();

	private ByteArrayOutputStream partialBinaryMessage = new ByteArrayOutputStream();

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private BlockSchema blockSchema = null;

	protected ClientServerInterface clientServerInterface = null;

	public BlockModelContext(BlockManagerThreadCollection blockManagerThreadCollection, ClientServerInterface clientServerInterface) throws Exception{
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.clientServerInterface = clientServerInterface;
		String catalinaBase = System.getProperty("catalina.base");
		String userDirectory = System.getProperty("user.dir");
		this.logMessage("catalinaBase=" + catalinaBase + ", userDirectory=" + userDirectory);
		String currentWorkingDirectory = catalinaBase == null ? userDirectory : catalinaBase + "/webapps/ROOT/WEB-INF/classes";
		String blockSchemaLocation = currentWorkingDirectory + "/v1_block_schema.json";
		this.logMessage("About to look for block schema at location " + blockSchemaLocation + ".");
		this.blockSchema = new BlockSchema(this, new String(Files.readAllBytes(Paths.get(blockSchemaLocation)), "UTF-8"));
	}

	public BlockManagerThreadCollection getBlockManagerThreadCollection(){
		return this.blockManagerThreadCollection;
	}

	public BlockSchema getBlockSchema(){
		return this.blockSchema;
	}

	public ClientServerInterface getClientServerInterface(){
		return this.clientServerInterface;
	}

	public void logMessage(String s){
		try{
			logger.info(s);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void logException(Exception e){
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		this.logMessage(errors.toString());
	}

	public void onMessage(String txt, BlockSession session) throws Exception {
		throw new Exception("Not expected.");
	}

	public void onBinaryMessage(byte[] inputBytes, boolean last, BlockSession session) throws Exception {
		partialBinaryMessage.write(inputBytes);
		this.logMessage("Partial message received of length " + inputBytes.length + " last=" + last);
		if(last){
			BlockSession blockSession = this.getBlockSession(session.getId());
			ProcessBinaryMessageWorkItem workItem = new ProcessBinaryMessageWorkItem(this, blockSession, partialBinaryMessage.toByteArray());
			this.partialBinaryMessage = new ByteArrayOutputStream();  //  Clear the buffer. 
			this.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
		}
	}

	public void onOpen(BlockSession blockSession) throws Exception {
		this.logMessage("Opened a session id=" + blockSession.getId());
		if(sessionMap.containsKey(blockSession.getId())){
			throw new Exception("Duplicate openning of a session for session id = " + blockSession.getId());
		}else{
			sessionMap.put(blockSession.getId(), blockSession);
		}
		SessionOpenWorkItem workItem = new SessionOpenWorkItem(this, blockSession);
		this.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
	}

	public void onClose(String closeReason, BlockSession session, boolean doClose) throws Exception {
		this.logMessage("In onClose for session id = '" + session.getId() + "' reason = " + closeReason);
		SessionCloseWorkItem workItem = new SessionCloseWorkItem(this, closeReason, session, doClose);
		this.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
		this.removeBlockSession(session.getId());
	}

	public void onError(BlockSession session, Throwable t) throws Throwable {
		if(session == null){
			this.logMessage("Error in a null session?");
			throw t;
		}else{
			BlockSession blockSession = getBlockSession(session.getId());
			SessionErrorWorkItem workItem = new SessionErrorWorkItem(this, blockSession, t);
			putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
			removeBlockSession(blockSession.getId());
		}
	}

	public ConcurrentHashMap<String, BlockSession> getSessionMap(){
		return this.sessionMap;
	}

	public BlockSession getBlockSession(String id) throws Exception{
		if(sessionMap.containsKey(id)){
			return sessionMap.get(id);
		}else{
			throw new Exception("Session not found id = " + id);
		}
	}

	public void removeBlockSession(String sessionId) throws Exception{
		if(sessionMap.containsKey(sessionId)){
			sessionMap.remove(sessionId);
			this.logMessage("Removed block session '" + sessionId + "'");
		}else{
			this.logMessage("Trying to remove session id = '" + sessionId + "', but it was not in the session map.  Must have already been removed.");
		}
	}

	public BlockModelContextWorkItem takeWorkItem() throws Exception {
		//this.logMessage("Before this.workItemQueue.takeWorkItem() " + this.getClass().getName() + " size=" + this.workItemQueue.size());
		BlockModelContextWorkItem w = this.workItemQueue.takeWorkItem();
		//this.logMessage("After this.workItemQueue.takeWorkItem() " + this.getClass().getName() + " size=" + this.workItemQueue.size() + ", WorkItem=" + w.getClass().getName());
		return w;
	}

	public void putWorkItem(BlockModelContextWorkItem workItem, WorkItemPriority priority) throws Exception{
		//this.logMessage("Before this.workItemQueue.putWorkItem(workItem) " + this.getClass().getName() + " size=" + this.workItemQueue.size() + ", WorkItem=" + workItem.getClass().getName());
		this.workItemQueue.putWorkItem(workItem, priority);
		//this.logMessage("After this.workItemQueue.putWorkItem(workItem) " + this.getClass().getName() + " size=" + this.workItemQueue.size());
	}

	public IndividualBlock deserializeBlockData(byte [] blockData) throws Exception{
		String blockClassName = this.getBlockSchema().getFirstBlockMatchDescriptionForByteArray(blockData);
		if(blockClassName == null){
			throw new Exception("Unable to determine block class for data '" + BlockModelContext.convertToHex(blockData) + "'");
		}else{
			return IndividualBlock.makeBlockInstanceFromClassName(blockClassName, blockData);
		}
	}

	public static String convertToHex(byte[] data) { 
		HexFormat hf = HexFormat.of();
		return hf.formatHex(data);
	}

	public boolean doBackgroundProcessing() throws Exception{
		return false;
	}
}
