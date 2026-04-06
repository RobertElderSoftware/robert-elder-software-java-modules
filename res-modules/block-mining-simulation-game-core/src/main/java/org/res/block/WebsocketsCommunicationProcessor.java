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

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

import java.net.URI;
import java.net.URISyntaxException;

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

@ClientEndpoint
public class WebsocketsCommunicationProcessor{

	protected WebSocketContainer container;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private ServerBlockModelContext serverBlockModelContext;
	private BlockManagerThreadCollection blockManagerThreadCollection;
	private WebsocketBlockWorldConnectionParameters websocketBlockWorldConnectionParameters;
	private Session userSession = null;
	private Map<Long, String> localClientSessionIds = new TreeMap<Long, String>();

	public WebsocketsCommunicationProcessor(BlockManagerThreadCollection blockManagerThreadCollection, ServerBlockModelContext serverBlockModelContext){
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.serverBlockModelContext = serverBlockModelContext;
	}

	public WebsocketsCommunicationProcessor(BlockManagerThreadCollection blockManagerThreadCollection, WebsocketBlockWorldConnectionParameters websocketBlockWorldConnectionParameters){
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.websocketBlockWorldConnectionParameters = websocketBlockWorldConnectionParameters;
	}

	public String getSessionIdPrefixString(ClientBlockModelContext clientBlockModelContext) throws Exception{
		AuthorizedBlockWorldConnection abwc = clientBlockModelContext.getAuthorizedBlockWorldConnection();
		String worldAddress = abwc.getBlockWorldConnection().getBlockWorldConnectionParameters().getBlockWorldAddressString();
		String idString = String.valueOf(abwc.getAuthorizedClientId());
		String prefix = worldAddress + ":" + idString;
		return prefix;
	}

	public BlockWorldConnection getBlockWorldConnection() throws Exception{
		if(this.serverBlockModelContext == null){
			return this.blockManagerThreadCollection.getBlockWorldConnectionByParams(websocketBlockWorldConnectionParameters);
		}else{
			return this.serverBlockModelContext.getBlockWorldConnection();
		}
	}

	public String getServerToClientSessionIdString(ClientBlockModelContext clientBlockModelContext) throws Exception{
		return getSessionIdPrefixString(clientBlockModelContext) + "local_server_to_client_connection";
	}

	public String getClientToServerSessionIdString(ClientBlockModelContext clientBlockModelContext) throws Exception{
		return getSessionIdPrefixString(clientBlockModelContext) + "local_client_to_server_connection";
	}

	public void worldConnect() throws Exception{
		if(this.websocketBlockWorldConnectionParameters == null){
			//  Nothing to do.
		}else{
			String websocketsServerURL = this.websocketBlockWorldConnectionParameters.getBlockWorldAddressString();
			this.userSession = ContainerProvider.getWebSocketContainer().connectToServer(this, new URI(websocketsServerURL));
			logger.info("Did websocket connect to url '" + websocketsServerURL + "'");
		}
	}

	public void authorizedClientConnect(ClientBlockModelContext clientBlockModelContext) throws Exception{
		if(this.websocketBlockWorldConnectionParameters == null){
			//  Local in-memory connection to server, manually set up connection:
			AuthorizedBlockWorldConnection abwc = clientBlockModelContext.getAuthorizedBlockWorldConnection();
			String worldAddress = abwc.getBlockWorldConnection().getBlockWorldConnectionParameters().getBlockWorldAddressString();

			Long authorizedClientId = clientBlockModelContext.getAuthorizedClientId();
			String localClientSessionId = getClientToServerSessionIdString(clientBlockModelContext);
			this.localClientSessionIds.put(authorizedClientId, localClientSessionId);

			LocalBlockSession serverToClientSession = new LocalBlockSession(serverBlockModelContext, getServerToClientSessionIdString(clientBlockModelContext));
			LocalBlockSession clientToServerSession = new LocalBlockSession(clientBlockModelContext, localClientSessionId);

			//  Add the sessions to manually set up the connection
			this.blockManagerThreadCollection.onOpen(serverBlockModelContext.getBlockWorldConnection(), serverToClientSession);
			this.blockManagerThreadCollection.onOpen(clientBlockModelContext.getBlockWorldConnection(), clientToServerSession);

			//  Connection the sessions to each others' remote endpoint:
			serverToClientSession.setRemoteSession(clientToServerSession);
			clientToServerSession.setRemoteSession(serverToClientSession);
		}else{
			//  The 'onOpen' will be called later automatically by the websockets open event...
		}
	}

	public void disconnect() throws Exception{
		if(this.websocketBlockWorldConnectionParameters == null){
			// TODO:  Figure out how to clear id for this client:
			//this.localClientSessionId = null; // Clear id.
		}else{
			this.userSession.close();
		}
	}

	public String getClientSessionId(ClientBlockModelContext clientBlockModelContext) throws Exception{
		if(this.websocketBlockWorldConnectionParameters == null){
			Long authorizedClientId = clientBlockModelContext.getAuthorizedClientId();
			return this.localClientSessionIds.get(authorizedClientId);
		}else{
			return this.userSession.getId();
		}
	}

	@OnOpen
	public void onOpen(Session session) throws Exception {
		logger.info("In onOpen...");
		this.blockManagerThreadCollection.onOpen(getBlockWorldConnection(), new WebsocketBlockSession(session));
	}

	@OnMessage
	public void onMessage(String txt, Session session) throws Exception {
		throw new Exception("Not expected.");
	}

	@OnMessage
	public void onBinaryMessage(byte[] inputBytes, boolean last, Session session) throws Exception {
		BlockSession blockSession = getBlockWorldConnection().getBlockSession(session.getId());
		this.blockManagerThreadCollection.onBinaryMessage(getBlockWorldConnection(), inputBytes, last, blockSession);
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) throws Exception {
		logger.info("In onClose...");
		this.blockManagerThreadCollection.onClose(getBlockWorldConnection(), String.valueOf(reason), session.getId());
	}

	@OnError
	public void onError(Session session, Throwable t) throws Throwable {
		logger.info("In onError...");
		this.blockManagerThreadCollection.onError(getBlockWorldConnection(), session.getId(), t);
	}
}
