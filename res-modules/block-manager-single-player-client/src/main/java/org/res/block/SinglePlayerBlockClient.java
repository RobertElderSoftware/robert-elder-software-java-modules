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

import java.io.BufferedInputStream;
import java.io.IOException;

import org.res.block.Coordinate;
import java.io.IOException;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.Thread;

import java.util.concurrent.TimeUnit;

import org.res.block.ServerBlockModelContext;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.slf4j.LoggerFactory;

class SinglePlayerBlockClient {

	public static void setupLogging(String logfileName) throws Exception {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logbackLogger = loggerContext.getLogger("ROOT");
		//  Stop output from being printed to console:
		logbackLogger.detachAppender("console");

		if(logfileName != null){
			FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender = new FileAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
			fileAppender.setContext(loggerContext);
			fileAppender.setFile(logfileName);

			PatternLayoutEncoder encoder = new PatternLayoutEncoder();
			encoder.setContext(loggerContext);
			encoder.setPattern("LOGBACK: %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
			encoder.start();

			fileAppender.setEncoder(encoder);
			fileAppender.start();

			logbackLogger.addAppender(fileAppender);
		}
		//StatusPrinter.print(loggerContext);
	}

	public static Map<String, String> parseArguments(String[] args) throws Exception {
		String blockWorldFile = null;
		//  Default to no logging:
		String logFile = null;
		for (int i = 0; i < args.length; i+=2){
			if (args[i].equals("--block-world-file") && args.length > i + 1){
				blockWorldFile = args[i+1];
			} else if (args[i].equals("--log-file")){
				logFile = args[i+1];
			}
		}
		Map<String, String> params = new HashMap<String, String>();
		params.put("--block-world-file", blockWorldFile);
		params.put("--log-file", logFile);
		return params;
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> commandlineArguments = parseArguments(args);

		SinglePlayerBlockClient.setupLogging(commandlineArguments.get("--log-file"));

		BlockManagerThreadCollection blockManagerThreadCollection = new BlockManagerThreadCollection();

		SinglePlayerClientServerInterface clientServerInterface = new SinglePlayerClientServerInterface();

		String databaseFile = commandlineArguments.get("--block-world-file") == null ? "world-1.sqlite" : commandlineArguments.get("--block-world-file");

		DatabaseConnectionParameters dbParams = new DatabaseConnectionParameters(
			"sqlite", //String subprotocol,
			null, //String hostname,
			null, //String port,
			null, //String databaseName,
			null, //String username,
			null, //String password,
			databaseFile //String filename
		);

		ServerBlockModelContext serverBlockModelContext = new ServerBlockModelContext(blockManagerThreadCollection, clientServerInterface, dbParams);
		ClientBlockModelContext clientBlockModelContext = new ClientBlockModelContext(blockManagerThreadCollection, clientServerInterface);

		clientServerInterface.setServerBlockModelContext(serverBlockModelContext);
		clientServerInterface.setClientBlockModelContext(clientBlockModelContext);

		blockManagerThreadCollection.addThreads(serverBlockModelContext.getThreads());
		blockManagerThreadCollection.addThreads(clientBlockModelContext.getThreads());

		LocalBlockSession serverToClientSession = new LocalBlockSession(serverBlockModelContext, "local_server_to_client_connection");
		LocalBlockSession clientToServerSession = new LocalBlockSession(clientBlockModelContext, "local_server_to_client_connection");

		//  Add the sessions to manually set up the connection
		serverBlockModelContext.onOpen(serverToClientSession);
		clientBlockModelContext.onOpen(clientToServerSession);

		//  Lets the client know where to send requests:
		clientServerInterface.setClientToServerSession(clientToServerSession);

		//  Connection the sessions to each others' remote endpoint:
		serverToClientSession.setRemoteSession(clientToServerSession);
		clientToServerSession.setRemoteSession(serverToClientSession);

		//  Start the game loading process
		clientBlockModelContext.init();
		blockManagerThreadCollection.blockUntilAllTasksHaveTerminated();
		serverBlockModelContext.shutdown();
		clientBlockModelContext.shutdown();

		List<Exception> offendingExceptions = blockManagerThreadCollection.getOffendingExceptions();
		if(offendingExceptions.size() == 0){
			System.out.println("Exited gracefully without any exceptions.");
		}else{
			System.out.println("This game contains a programming bug that caused it to crash.  Here is the stack trace:");
			System.out.println("");
			for(Exception e : offendingExceptions){
				e.printStackTrace();
			}
		}
	}
}
