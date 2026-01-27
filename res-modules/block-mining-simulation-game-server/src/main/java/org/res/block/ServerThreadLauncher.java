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


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import org.res.block.dao.BlockDAO;
import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

@EnableAutoConfiguration(
	exclude={
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
	}
)
@Configuration
@ComponentScan
@Component
public class ServerThreadLauncher {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private BlockManagerThreadCollection blockManagerThreadCollection = null;
	private ServerBlockModelContext serverBlockModelContext = null;

	public ServerThreadLauncher() throws Exception {
		//  Just create some fake arguments to simulate passing them in from the command-line for now:
		Map<String, List<String>> defaultArgumentValues = new HashMap<String, List<String>>();
		defaultArgumentValues.put("--disable-jni", Arrays.asList());
		String [] args = new String[0];
		CommandLineArgumentCollection commandLineArgumentCollection = ArgumentParser.parseArguments(args, defaultArgumentValues);
		this.blockManagerThreadCollection = new BlockManagerThreadCollection(commandLineArgumentCollection, false);

		logger.info("In ServerThreadLauncher constructor.");

		DatabaseConnectionParameters dbParams = new DatabaseConnectionParameters(
			"postgresql", //String subprotocol,
			"127.0.0.1", //String hostname,
			"5432", //String port,
			"block_manager", //String databaseName,
			"block_manager_user", //String username,
			"block_manager_password", //String password,
			null //String filename
		);

		ServerInterface serverInterface = new WebserverServerInterface();
		this.serverBlockModelContext = new ServerBlockModelContext(blockManagerThreadCollection, serverInterface, new WebsocketsSessionOperationInterface(), new DatabaseBlockWorldConnection(dbParams));
		this.serverBlockModelContext.putWorkItem(new InitializeYourselfServerBlockModelContextWorkItem(this.serverBlockModelContext, new ArrayList<ClientBlockModelContext>()), WorkItemPriority.PRIORITY_LOW);
		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<BlockModelContextWorkItem>(serverBlockModelContext, BlockModelContextWorkItem.class, ServerBlockModelContext.class));
	}

	@PostConstruct
	public void init() throws Exception{
	}

	@PreDestroy
	public void shutdown() throws Exception {
		this.blockManagerThreadCollection.setIsProcessFinished(true, null);
	}

	public boolean getIsProcessFinished(){
		return this.blockManagerThreadCollection.getIsProcessFinished();
	}

	public void onOpen(Session session) throws Exception {
		WebsocketBlockSession bs = new WebsocketBlockSession(session);
		bs.setMaxBinaryMessageBufferSize(1024*4);
		this.serverBlockModelContext.onOpen(bs);
	}

	public void onMessage(String txt, Session session) throws Exception {
		this.serverBlockModelContext.onMessage(txt, new WebsocketBlockSession(session));
	}

	public void onBinaryMessage(byte[] inputBytes, boolean last, Session session) throws Exception {
		this.serverBlockModelContext.onBinaryMessage(inputBytes, last, new WebsocketBlockSession(session));
	}

	public void onClose(String reason, Session session, boolean doClose) throws Exception {

		this.serverBlockModelContext.onClose(reason, new WebsocketBlockSession(session), doClose);
	}

	public void onError(Session session, Throwable t) throws Throwable {
		this.serverBlockModelContext.onError(new WebsocketBlockSession(session), t);
	}
}
