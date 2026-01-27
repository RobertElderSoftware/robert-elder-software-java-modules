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


import java.util.UUID;
import java.util.List;
import java.util.Arrays;
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
	private ServerBlockModelInterface blockModelInterface = new ServerBlockModelInterface();
	private DatabaseBlockWorldConnection databaseBlockWorldConnection;
	private ServerInterface serverInterface;
	private SessionOperationInterface sessionOperationInterface;

	public BlockModelInterface getBlockModelInterface(){
		return blockModelInterface;
	}

	public SessionOperationInterface getSessionOperationInterface(){
		return this.sessionOperationInterface;
	}

	public ServerBlockModelContext(BlockManagerThreadCollection blockManagerThreadCollection, ServerInterface serverInterface, SessionOperationInterface sessionOperationInterface, DatabaseBlockWorldConnection databaseBlockWorldConnection) throws Exception {
		super(blockManagerThreadCollection);
		this.serverInterface = serverInterface;
		this.sessionOperationInterface = sessionOperationInterface;
		this.databaseBlockWorldConnection = databaseBlockWorldConnection;
	}

	public void init(Object o) throws Exception{
		this.blockModelInterface.setServerBlockModelInterface(this);
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(BlockManagerServerApplicationContext.class);
		this.context = annotationConfigApplicationContext;

		BlockManagerServerApplicationContextParameters params = new BlockManagerServerApplicationContextParameters(this, databaseBlockWorldConnection.getDatabaseConnectionParameters());
		this.context.addBeanFactoryPostProcessor(new BlockManagerServerBeanPostProcessorConfigurer(params));
		this.context.refresh();

		this.blockDAO = (BlockDAO)context.getBean("blockDAO");

		this.logMessage("Ran constructor of ServerBlockModelContext.");

        	this.blockDAO.ensureBlockTableExists();
		this.blockDAO.turnOffAutoCommit();
	}

	public void destroy(Object o) throws Exception{

	}

	public BlockDAO getBlockDAO(){
		return this.blockDAO;
	}

	public boolean isServer(){
		return true;
	}

	public void sendBlockMessage(BlockMessage m, BlockSession session) throws Exception{
		this.serverInterface.sendBlockMessage(m, session);
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

	public Coordinate getPlayerModelBlockForAuthorizedClientId(PlayerDataModelBlockType blockType, Long authorizedClientId){
		//  This function is where the server can determine how it actually lays out
		//  the root dictionary blocks for each player based on the individual player's
		//  authorized client id.  
		return new Coordinate(Arrays.asList(100000000L - blockType.toLong(), 99999999L, 99999999L - authorizedClientId, 99999999L));
	}

	public boolean isRootDictionaryInitialized(Cuboid c) throws Exception{
		CuboidAddress cuboidAddress = c.getCuboidAddress();
		CuboidDataLengths dataLengths = c.getCuboidDataLengths();
		CuboidData data = c.getCuboidData();

		long sizeOfBlock = dataLengths.getLengths()[0];
		if(sizeOfBlock < 0L){ //  Block not initialized.
			return false;
		}else{
			byte [] blockData = data.getDataAtOffset(0, sizeOfBlock);

			String blockClassName = this.getBlockSchema().getFirstBlockMatchDescriptionForByteArray(blockData);
			if(blockClassName == null){
				throw new Exception("This should never happen.  Initialized, but invalid?");
			}else if(!(IndividualBlock.makeBlockInstanceFromClassName(blockClassName, blockData) instanceof BlockDictionary)){
				throw new Exception("This should never happen.  Initialized, but wrong class type?");
			}else{
				return true;
			}
		}
	}

	public void onErrorNotificationBlockMessage(BlockSession blockSession, Long conversationId, BlockMessageErrorType blockMessageErrorType) throws Exception{
		switch(blockMessageErrorType){
			default:{
				throw new Exception("Message type not expected: " + blockMessageErrorType);
			}
		}
	}

	public void provisionNewPlayer(Long authorizedClientId) throws Exception{
		UUID playerUUID = UUID.randomUUID();
		String playerUUIDString = playerUUID.toString();

		Coordinate rootDictionaryAddress = getPlayerModelBlockForAuthorizedClientId(PlayerDataModelBlockType.ROOT_DICTIONARY, authorizedClientId);
		Coordinate spawnCoordinate = Coordinate.makeOriginCoordinate(4L);

		PlayerObject newPlayerObject = new PlayerObject(
			playerUUIDString,
			PlayerObjectSkinType.HAPPY_FACE
		);

		this.writeSingleBlockAtPosition(
			newPlayerObject.getBlockData(),
			spawnCoordinate
		);

		BlockDictionary newRootDictionary = new BlockDictionary();
		newRootDictionary.put(
			"player_position",
			getPlayerModelBlockForAuthorizedClientId(PlayerDataModelBlockType.PLAYER_POSITION, authorizedClientId)
		);
		newRootDictionary.put(
			"player_inventory",
			getPlayerModelBlockForAuthorizedClientId(PlayerDataModelBlockType.PLAYER_INVENTORY, authorizedClientId)
		);

		this.writeSingleBlockAtPosition(newRootDictionary.getBlockData(), rootDictionaryAddress);

		this.writeSingleBlockAtPosition(
			(new PlayerPositionXYZ("player:" + playerUUIDString, Coordinate.makeOriginCoordinate(4L))).getBlockData(),
			getPlayerModelBlockForAuthorizedClientId(PlayerDataModelBlockType.PLAYER_POSITION, authorizedClientId)
		);

		this.writeSingleBlockAtPosition(
			(new PlayerInventory()).getBlockData(),
			getPlayerModelBlockForAuthorizedClientId(PlayerDataModelBlockType.PLAYER_INVENTORY, authorizedClientId)
		);
	}

	public void writeSingleBlockAtPosition(byte [] data, Coordinate position) throws Exception{
		Long numDimensions = position.getNumDimensions();

		CuboidAddress blocksToChangeAddress = new CuboidAddress(position, position.add(Coordinate.makeUnitCoordinate(4L)));
		BlockMessageBinaryBuffer dataForOneCuboid = new BlockMessageBinaryBuffer();

		Long numBlocks = blocksToChangeAddress.getVolume();
		long [] dataLengths = new long [numBlocks.intValue()];
		for(int i = 0; i < numBlocks; i++){
			dataLengths[i] = data.length;
			dataForOneCuboid.writeBytes(data);
		}

		CuboidDataLengths currentCuboidDataLengths = new CuboidDataLengths(blocksToChangeAddress, dataLengths);
		CuboidData currentCuboidData = new CuboidData(dataForOneCuboid.getUsedBuffer());

		Cuboid c = new Cuboid(blocksToChangeAddress, currentCuboidDataLengths, currentCuboidData);
		this.getBlockModelInterface().writeBlocksInRegion(c);
	}

	public void sendRootDictionaryAddress(BlockSession blockSession, Long conversationId, Long authorizedClientId, boolean allowError) throws Exception{
		Coordinate rootDictionaryAddress = getPlayerModelBlockForAuthorizedClientId(PlayerDataModelBlockType.ROOT_DICTIONARY, authorizedClientId);
		//  Read the single block where the root dictionary is supposed to be stored:
		List<CuboidAddress> addresses = new ArrayList<CuboidAddress>();
		addresses.add(new CuboidAddress(rootDictionaryAddress, rootDictionaryAddress.add(Coordinate.makeUnitCoordinate(4L))));
		List<Cuboid> cuboids = this.getBlockModelInterface().getBlocksInRegions(addresses);
		if(isRootDictionaryInitialized(cuboids.get(0))){
			//  Send address back to client:
			AuthorizedCommandBlockMessage response = new AuthorizedCommandBlockMessage(this, conversationId, authorizedClientId, AuthorizedCommandType.COMMAND_TYPE_RESPOND_ROOT_DICTIONARY_ADDRESS, rootDictionaryAddress);
			this.sendBlockMessage(response, blockSession);
		}else{
			if(allowError){
				//  Send error notification
				ErrorNotificationBlockMessage response = new ErrorNotificationBlockMessage(this, BlockMessageErrorType.ROOT_BLOCK_DICTIONARY_UNINITIALIZED, conversationId);
				this.sendBlockMessage(response, blockSession);
			}else{
				throw new Exception("Case not expected.");
			}
		}
	}

	public void onAuthorizedCommandBlockMessage(BlockSession blockSession, Long conversationId, Long authorizedClientId, AuthorizedCommandType authorizedCommandType, Coordinate coordinate) throws Exception{
		switch(authorizedCommandType){
			case COMMAND_TYPE_REQUEST_ROOT_DICTIONARY_ADDRESS:{
				this.sendRootDictionaryAddress(blockSession, conversationId, authorizedClientId, true);
				break;
			}case COMMAND_TYPE_PROVISION_PLAYER:{
				this.provisionNewPlayer(authorizedClientId);
				this.sendRootDictionaryAddress(blockSession, conversationId, authorizedClientId, false);
				break;
			}default:{
				throw new Exception("Message type not expected: " + authorizedCommandType);
			}
		}
	}
}
