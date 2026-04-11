//  Copyright (c) 2026 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License (Version 2026-04-09)
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
//  either unmodified, modified, or incorporated into another software product, 
//  except as described in the document "REDISTRIBUTION.md" (a file with SHA256 
//  hash value 'c39a6c8200a22caf30eac97095b78def80c9cab1b6f7ddd3fca7fdae71df43da').
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public abstract class BlockWorldConnection {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected WebsocketsCommunicationProcessor websocketsCommunicationProcessor;
	private BlockWorldConnectionParameters blockWorldConnectionParameters;
	private SessionOperationInterface sessionOperationInterface;
	private ConcurrentHashMap<String, BlockSession> sessionMap = new ConcurrentHashMap<String, BlockSession>();
	public ConcurrentHashMap<String, BlockSession> getSessionMap(){
		return this.sessionMap;
	}

	public void removeBlockSession(String sessionId) throws Exception{
		if(sessionMap.containsKey(sessionId)){
			sessionMap.remove(sessionId);
			logger.info("Removed block session '" + sessionId + "'");
		}else{
			logger.info("Trying to remove session id = '" + sessionId + "', but it was not in the session map.  Must have already been removed.");
		}
	}

	public void addBlockSession(BlockSession blockSession) throws Exception{
		logger.info("Opened a session id=" + blockSession.getId());
		if(sessionMap.containsKey(blockSession.getId())){
			throw new Exception("Duplicate openning of a session for session id = " + blockSession.getId());
		}else{
			sessionMap.put(blockSession.getId(), blockSession);
		}
	}

	public BlockSession getBlockSession(String id) throws Exception{
		if(sessionMap.containsKey(id)){
			return sessionMap.get(id);
		}else{
			throw new Exception("Session not found id = " + id);
		}
	}

	public BlockWorldConnection(BlockWorldConnectionParameters blockWorldConnectionParameters, SessionOperationInterface sessionOperationInterface) throws Exception {
		this.blockWorldConnectionParameters = blockWorldConnectionParameters;
		this.sessionOperationInterface = sessionOperationInterface;
	}

	public String getBlockWorldAddressString(){
		return blockWorldConnectionParameters.getBlockWorldAddressString();
	}

	public SessionOperationInterface getSessionOperationInterface(){
		return this.sessionOperationInterface;
	}

	public BlockWorldConnectionParameters getBlockWorldConnectionParameters() throws Exception{
		return this.blockWorldConnectionParameters;
	}

	public abstract void init() throws Exception;
	public abstract void destroy() throws Exception;
	public abstract WebsocketsCommunicationProcessor getCommunicationProcessor() throws Exception;
}
