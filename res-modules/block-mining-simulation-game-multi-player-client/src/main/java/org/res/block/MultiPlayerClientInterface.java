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

import org.res.block.ClientInterface;
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
public class MultiPlayerClientInterface extends ClientInterface{

	private String hostName;
	private Session userSession = null;
	private Integer port;
	private String url;
	protected WebSocketContainer container;
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public MultiPlayerClientInterface(String hostName, Integer port, String url){
		this.hostName = hostName;
		this.port = port;
		this.url = url;
		this.container = ContainerProvider.getWebSocketContainer();
	}

	public String getClientSessionId() throws Exception{
		return this.userSession.getId();
	}


	private BlockModelContext getBlockModelContext() throws Exception{
		if(clientBlockModelContext == null){
			throw new Exception("Forgot to initialize clientBlockModelContext?");
		}else{
			return clientBlockModelContext;
		}
	}

	public void Connect() throws Exception{
		String websocketsServerURL = "ws://" + this.hostName + ":" + this.port + this.url;
		this.userSession = this.container.connectToServer(this, new URI(websocketsServerURL));
	}

	public void Disconnect() throws Exception{
		this.userSession.close();
	}

	public void sendBlockMessage(BlockMessage m, BlockSession session) throws Exception{
		if(session instanceof WebsocketBlockSession){
			byte [] byteEncodedResponse = m.asByteArray();
			((WebsocketBlockSession)session).sendBytes(byteEncodedResponse);
		}else{
			throw new Exception("Expected sesion to be of type WebsocketBlockSession but it had type " + session.getClass().getName());
		}
	}

	@OnOpen
	public void onOpen(Session session) throws Exception {
		this.getBlockModelContext().onOpen(new WebsocketBlockSession(session));
	}

	@OnMessage
	public void onMessage(String txt, Session session) throws Exception {
		this.getBlockModelContext().onMessage(txt, new WebsocketBlockSession(session));
	}

	@OnMessage
	public void onBinaryMessage(byte[] inputBytes, boolean last, Session session) throws Exception {
		this.getBlockModelContext().onBinaryMessage(inputBytes, last, new WebsocketBlockSession(session));
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) throws Exception {
		boolean doClose = true;

		this.getBlockModelContext().onClose(String.valueOf(reason), new WebsocketBlockSession(session), doClose);
	}

	@OnError
	public void onError(Session session, Throwable t) throws Throwable {
		this.getBlockModelContext().onError(new WebsocketBlockSession(session), t);
	}
}
