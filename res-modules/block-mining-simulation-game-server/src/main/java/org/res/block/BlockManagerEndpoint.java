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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.Object;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.lang.Throwable;
import java.io.PrintWriter;

import java.util.Enumeration;
import java.util.ResourceBundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.nio.file.Files;

import org.springframework.web.util.HtmlUtils;

import java.text.SimpleDateFormat;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Locale;
import java.util.HashMap;
import java.util.Collections;
import java.lang.Object;
import java.lang.Math;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import java.sql.Timestamp;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonNull;
import com.google.gson.reflect.TypeToken;

import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;

import java.lang.ClassNotFoundException;
import java.lang.NoSuchMethodException;
import java.lang.InstantiationException;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.http.HttpServletRequest;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

import javax.websocket.server.ServerEndpoint;
import org.springframework.web.socket.server.standard.SpringConfigurator;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.res.block.dao.BlockDAO;
import org.res.block.dao.impl.BlockDAOImpl;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.springframework.context.ConfigurableApplicationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

@ServerEndpoint(
	value = "/block-manager",
	encoders = WebSocketMessageEncoder.class, 
	decoders = WebSocketMessageDecoder.class,
	configurator = CustomSpringConfigurator.class
)
public class BlockManagerEndpoint {

	/*  Due to limitations of spring and tomcat, you can't use @autowired with the @serverendpoint annotation. */

	private ServerThreadLauncher serverThreadLauncher = null;

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private ServerThreadLauncher getServerThreadLauncher() throws Exception{
		if(serverThreadLauncher == null){
			serverThreadLauncher = CustomSpringConfigurator.getApplicationContext().getBean(ServerThreadLauncher.class);
		}

		if(serverThreadLauncher.getIsProcessFinished()){
			//((ConfigurableApplicationContext)CustomSpringConfigurator.getApplicationContext()).close();
			throw new Exception("Refusing to do any further work after isFinished flag has been set.");
		}

		return serverThreadLauncher;
	}

	@OnOpen
	public void onOpen(Session session) throws Exception {
		this.getServerThreadLauncher().onOpen(session);
	}

	@OnMessage
	public void onMessage(String txt, Session session) throws Exception {
		this.getServerThreadLauncher().onMessage(txt, session);
	}

	@OnMessage
	public void onBinaryMessage(byte[] inputBytes, boolean last, Session session) throws Exception {
		this.getServerThreadLauncher().onBinaryMessage(inputBytes, last, session);
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) throws Exception {
		boolean doClose = true;
		if(reason != null && CloseReason.CloseCodes.SERVICE_RESTART.equals(reason.getCloseCode())){
			doClose = false;
			/*
			This check exists to avoid this error:
			SEVERE [main] org.apache.tomcat.websocket.pojo.PojoEndpointBase.onClose Failed to call onClose method of POJO end point for POJO of type [org.res.block.BlockManagerEndpoint]
				java.lang.reflect.InvocationTargetException
					at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
					at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
					...
					at org.apache.tomcat.websocket.WsSession.close(WsSession.java:625)
					at org.res.block.BlockSession.close(BlockSession.java:48)
					at org.res.block.BlockModelContext.shutdown(BlockModelContext.java:94)

			*/
			logger.info("Close code was SERVICE_RESTART.  Don't try to close sessions to avoid stack traces.");
			return;
		}else if(reason != null && CloseReason.CloseCodes.GOING_AWAY.equals(reason.getCloseCode())){
			logger.info("Close code was GOING_AWAY.  Allow closing of session.");
		}else{
			logger.info("Close code was something else.");
		}
		this.getServerThreadLauncher().onClose(String.valueOf(reason), session, doClose);
	}

	@OnError
	public void onError(Session session, Throwable t) throws Throwable {
		this.getServerThreadLauncher().onError(session, t);
	}
}
