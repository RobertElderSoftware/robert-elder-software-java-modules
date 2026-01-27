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

import javax.websocket.Session;
import javax.websocket.RemoteEndpoint;
import javax.websocket.CloseReason;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class WebsocketBlockSession extends BlockSession {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private Session session; /* Don't return any references to session to ensure thread safety. */
	private Object monitor = new Object();

	public WebsocketBlockSession(Session session) throws Exception {
		super();
		this.session = session;
	}

	public void close(String reason) throws Exception {
		synchronized (monitor){
			this.session.close();
		}
	}

	public void sendBytes(byte [] bytes) throws Exception{
		if(this.session.isOpen()){
			this.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
		}else{
			logger.info("Session '" + this.session.getId() + "' was closed.  Discarding the " + bytes.length + " bytes that were scheduled to be sent to this session.");
		}
	}

	private RemoteEndpoint.Basic getBasicRemote(){
		synchronized (monitor){
			return this.session.getBasicRemote();
		}
	}

	public String getId(){
		synchronized (monitor){
			return this.session.getId();
		}
	}

	public void setMaxBinaryMessageBufferSize(int length){
		synchronized (monitor){
			this.session.setMaxBinaryMessageBufferSize(length);
		}
	}
}
