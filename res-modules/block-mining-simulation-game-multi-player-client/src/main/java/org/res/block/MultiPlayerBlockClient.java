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

import java.io.BufferedInputStream;
import java.io.IOException;

import org.res.block.Coordinate;
import java.io.IOException;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.Thread;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.util.StatusPrinter;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

class MultiPlayerBlockClient {

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

	public static void startMultiPlayerClient(CommandLineArgumentCollection commandLineArgumentCollection) throws Exception {
		MultiPlayerBlockClient.setupLogging(commandLineArgumentCollection.getUsedSingleValue("--log-file"));

		String hostName = "127.0.0.1";
		Integer port = 8888;
		String url = "/block-manager";

		BlockManagerThreadCollection blockManagerThreadCollection = new BlockManagerThreadCollection(commandLineArgumentCollection, true);
		blockManagerThreadCollection.init();
		MultiPlayerClientInterface clientInterface = new MultiPlayerClientInterface(hostName, port, url);
		ClientBlockModelContext clientBlockModelContext = new ClientBlockModelContext(blockManagerThreadCollection, clientInterface, new WebsocketsSessionOperationInterface());
		blockManagerThreadCollection.addClientBlockModelContext(clientBlockModelContext);
		clientBlockModelContext.putWorkItem(new InitializeYourselfClientBlockModelContextWorkItem(clientBlockModelContext), WorkItemPriority.PRIORITY_LOW);
		blockManagerThreadCollection.addThread(new WorkItemProcessorTask<BlockModelContextWorkItem>(clientBlockModelContext, BlockModelContextWorkItem.class, ClientBlockModelContext.class));
		blockManagerThreadCollection.setupDefaultUIForClient(clientBlockModelContext);
		//  Start the game loading process
		blockManagerThreadCollection.blockUntilAllTasksHaveTerminated();
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

	public static void main(String[] args) throws Exception {
		Map<String, List<String>> defaultArgumentValues = new HashMap<String, List<String>>();
		CommandLineArgumentCollection commandLineArgumentCollection = ArgumentParser.parseArguments(args, defaultArgumentValues);

		if(commandLineArgumentCollection.hasUsedKey("--help")){
			//  Just print help menu an exit:
			if(commandLineArgumentCollection.hasUsedKey("--debug-arguments")){
				commandLineArgumentCollection.printHelpMenu(true);
			}else{
				commandLineArgumentCollection.printHelpMenu(false);
			}
		}else{
			MultiPlayerBlockClient.startMultiPlayerClient(commandLineArgumentCollection);
		}
	}
}
