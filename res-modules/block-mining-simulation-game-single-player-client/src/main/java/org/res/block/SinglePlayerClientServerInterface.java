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

import org.res.block.ClientServerInterface;

import java.util.Set;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class SinglePlayerClientServerInterface extends ClientServerInterface{

	private ServerBlockModelContext serverBlockModelContext;
	private LocalBlockSession clientBlockSession;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Object lock = new Object();

	public void setServerBlockModelContext(ServerBlockModelContext serverBlockModelContext){
		this.serverBlockModelContext = serverBlockModelContext;
	}

	public void setClientBlockModelContext(ClientBlockModelContext clientBlockModelContext){
		this.clientBlockModelContext = clientBlockModelContext;
	}

	public void setClientToServerSession(LocalBlockSession clientBlockSession){
		synchronized(lock){
			this.clientBlockSession = clientBlockSession;
		}
	}

	public LocalBlockSession getClientToServerSession(){
		synchronized(lock){
			return this.clientBlockSession;
		}
	}

	public void Connect() throws Exception{
		//  Do nothing
	}

	public void Disconnect() throws Exception{
		//  Do nothing
	}

	public String getClientSessionId() throws Exception{
		//  There is a case where another thread starts up a bit too fast
		//  and tries to send some data over the socket before the client session
		//  has been set.  For now, let the thread spin until this variable is initailized.
		//  TODO: Improve this to use a better design:
		while(this.getClientToServerSession() == null){};
		return this.getClientToServerSession().getId();
	}

	public void onBlockSessionOpen(BlockSession blockSession) throws Exception{
		if(blockSession instanceof LocalBlockSession){
			logger.info(String.format("Opened local session for id '%s'.", blockSession.getId()));
		}else{
			throw new Exception("Expected sesion to be of type LocalBlockSession but it had type " + blockSession.getClass().getName());
		}
	}


	public void onBlockSessionClose(BlockSession blockSession, String closeReason, boolean doClose) throws Exception{
		if(blockSession instanceof LocalBlockSession){
			logger.info(String.format("Closed websocket session for id '%s' for reason '%s'.", blockSession.getId(), closeReason));	
		}else{
			throw new Exception("Expected sesion to be of type LocalBlockSession but it had type " + blockSession.getClass().getName());
		}
	}

	public BlockModelContext getRemoteBlockModelContext(BlockMessage m) throws Exception{
		if(m.getBlockModelContext() instanceof ClientBlockModelContext){
			return this.serverBlockModelContext;
		}else if(m.getBlockModelContext() instanceof ServerBlockModelContext){
			return this.clientBlockModelContext;
		}else{
			throw new Exception("Unexpect class type " + m.getBlockModelContext().getClass().getName());
		}
	}

	public void sendBlockMessage(BlockMessage m, BlockSession session) throws Exception{
		//  Swap all the block model context references from server to client and vice versa:

		if(session instanceof LocalBlockSession){
			LocalBlockSession remoteSession = ((LocalBlockSession)session).getRemoteSession();
			BlockModelContext remoteBlockModelContext = getRemoteBlockModelContext(m);

			m.setBlockModelContext(remoteBlockModelContext);

			ProcessBlockMessageWorkItem w = new ProcessBlockMessageWorkItem(remoteBlockModelContext, remoteSession, m);
			remoteBlockModelContext.putWorkItem(w, WorkItemPriority.PRIORITY_LOW);
		}else{
			throw new Exception("Expected session to be local type, but it was " + session.getClass().getName());
		}
	}
}
