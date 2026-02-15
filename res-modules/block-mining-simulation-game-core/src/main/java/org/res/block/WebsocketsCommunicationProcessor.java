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
	private ClientBlockModelContext clientBlockModelContext;
	private BlockManagerThreadCollection blockManagerThreadCollection;
	private WebsocketBlockWorldConnectionParameters websocketBlockWorldConnectionParameters;
	private Session userSession = null;

	public WebsocketsCommunicationProcessor(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, ServerBlockModelContext serverBlockModelContext){
		this.clientBlockModelContext = clientBlockModelContext;
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.serverBlockModelContext = serverBlockModelContext;
	}

	public WebsocketsCommunicationProcessor(BlockManagerThreadCollection blockManagerThreadCollection, ClientBlockModelContext clientBlockModelContext, WebsocketBlockWorldConnectionParameters websocketBlockWorldConnectionParameters){
		this.clientBlockModelContext = clientBlockModelContext;
		this.blockManagerThreadCollection = blockManagerThreadCollection;
		this.websocketBlockWorldConnectionParameters = websocketBlockWorldConnectionParameters;
	}

	public String getSessionIdPrefixString() throws Exception{
		AuthorizedBlockWorldConnection abwc = this.clientBlockModelContext.getAuthorizedBlockWorldConnection();
		String worldAddress = abwc.getBlockWorldConnection().getBlockWorldConnectionParameters().getBlockWorldAddressString();
		String idString = String.valueOf(abwc.getAuthorizedClientId());
		String prefix = worldAddress + ":" + idString;
		return prefix;
	}

	public String getServerToClientSessionIdString() throws Exception{
		return getSessionIdPrefixString() + "local_server_to_client_connection";
	}

	//public String getClientToServerSessionIdString() throws Exception{
	//	return getSessionIdPrefixString() + "local_client_to_server_connection";
	//}

	public void connect() throws Exception{
		if(this.websocketBlockWorldConnectionParameters == null){
			AuthorizedBlockWorldConnection abwc = this.clientBlockModelContext.getAuthorizedBlockWorldConnection();
			String worldAddress = abwc.getBlockWorldConnection().getBlockWorldConnectionParameters().getBlockWorldAddressString();
			String idString = String.valueOf(abwc.getAuthorizedClientId());
			String prefix = worldAddress + ":" + idString;

			LocalBlockSession serverToClientSession = new LocalBlockSession(serverBlockModelContext, getServerToClientSessionIdString());
			LocalBlockSession clientToServerSession = new LocalBlockSession(clientBlockModelContext, getServerToClientSessionIdString());

			//  Add the sessions to manually set up the connection
			serverBlockModelContext.onOpen(serverToClientSession);
			clientBlockModelContext.onOpen(clientToServerSession);

			//  Connection the sessions to each others' remote endpoint:
			serverToClientSession.setRemoteSession(clientToServerSession);
			clientToServerSession.setRemoteSession(serverToClientSession);
		}else{
			String websocketsServerURL = this.websocketBlockWorldConnectionParameters.getBlockWorldAddressString();
			this.userSession = ContainerProvider.getWebSocketContainer().connectToServer(this, new URI(websocketsServerURL));
			logger.info("Did websocket connect to url '" + websocketsServerURL + "'");
		}
	}

	public void disconnect() throws Exception{
		if(this.websocketBlockWorldConnectionParameters == null){
			// Do nothing
		}else{
			this.userSession.close();
		}
	}

	public String getClientSessionId() throws Exception{
		if(this.websocketBlockWorldConnectionParameters == null){
			return getServerToClientSessionIdString();
		}else{
			return this.userSession.getId();
		}
	}

	@OnOpen
	public void onOpen(Session session) throws Exception {
		logger.info("In onOpen...");
		this.blockManagerThreadCollection.onOpen(clientBlockModelContext, session);
	}

	@OnMessage
	public void onMessage(String txt, Session session) throws Exception {
		this.blockManagerThreadCollection.onMessage(clientBlockModelContext, txt, session);
	}

	@OnMessage
	public void onBinaryMessage(byte[] inputBytes, boolean last, Session session) throws Exception {
		this.blockManagerThreadCollection.onBinaryMessage(clientBlockModelContext, inputBytes, last, session);
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) throws Exception {
		logger.info("In onClose...");
		this.blockManagerThreadCollection.onClose(clientBlockModelContext, reason, session);
	}

	@OnError
	public void onError(Session session, Throwable t) throws Throwable {
		logger.info("In onError...");
		this.blockManagerThreadCollection.onError(clientBlockModelContext, session, t);
	}
}
