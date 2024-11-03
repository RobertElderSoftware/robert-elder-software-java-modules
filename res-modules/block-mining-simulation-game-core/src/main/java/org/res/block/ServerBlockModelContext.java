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


import java.util.List;
import java.util.ArrayList;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

public class ServerBlockModelContext extends BlockModelContext {

	private AbstractApplicationContext context = null;
	private BlockDAO blockDAO = null;
	private ExecutorService executorService = null;
	private ServerBlockModelInterface blockModelInterface = new ServerBlockModelInterface(this);

	public BlockModelInterface getBlockModelInterface(){
		return blockModelInterface;
	}

	public ServerBlockModelContext(BlockManagerThreadCollection blockManagerThreadCollection, ClientServerInterface clientServerInterface, DatabaseConnectionParameters databaseConnectionParameters) throws Exception {
		super(blockManagerThreadCollection, clientServerInterface);
		this.clientServerInterface = clientServerInterface;
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(BlockManagerServerApplicationContext.class);
		this.context = annotationConfigApplicationContext;

		BlockManagerServerApplicationContextParameters params = new BlockManagerServerApplicationContextParameters(this, databaseConnectionParameters);
		this.context.addBeanFactoryPostProcessor(new BlockManagerServerBeanPostProcessorConfigurer(params));
		this.context.refresh();

		this.blockDAO = (BlockDAO)context.getBean("blockDAO");

		this.logMessage("Ran constructor of ServerBlockModelContext.");

        	this.blockDAO.ensureBlockTableExists();
        	this.blockDAO.turnOffAutoCommit();
		this.blockManagerThreadCollection.addThread(new WorkItemProcessorTask<BlockModelContextWorkItem>(this, BlockModelContextWorkItem.class));
	}

	public BlockDAO getBlockDAO(){
		return this.blockDAO;
	}

	public boolean isServer(){
		return true;
	}

	public void sendBlockMessage(BlockMessage m, BlockSession session) throws Exception{
		this.clientServerInterface.sendBlockMessage(m, session);
	}

	public void shutdown() throws Exception {
		for(Map.Entry<String, BlockSession> e : getSessionMap().entrySet()){
			//  Gracefully close all sessions:
			this.logMessage("Closing session '" + e.getKey() + "' TODO:  Send some kind of notify to the client here.");
			e.getValue().close("Gracefully closing due to shutdown sequence...");
		}
		this.logMessage("Ran shutdown of ServerBlockModelContext.");
	}

	public void inMemoryChunksCallbackOnChunkWasWritten(CuboidAddress ca) throws Exception{
	}

	public void inMemoryChunksCallbackOnChunkBecomesPending(CuboidAddress ca) throws Exception{
	}

	public void postCuboidsWrite(Long numDimensions, List<CuboidAddress> cuboidAddresses) throws Exception{
		AfterWriteCuboidsWorkItem afterWriteCuboidsWorkItem = new AfterWriteCuboidsWorkItem(this, numDimensions, cuboidAddresses);
		this.putWorkItem(afterWriteCuboidsWorkItem, WorkItemPriority.PRIORITY_LOW);
	}

	public void onAcknowledgementMessage(Long conversationId) throws Exception{

	}
}
