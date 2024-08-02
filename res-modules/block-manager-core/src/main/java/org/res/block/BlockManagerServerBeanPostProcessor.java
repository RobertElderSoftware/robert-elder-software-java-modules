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

import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.res.block.dao.impl.BlockDAOImpl;

import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;

import org.springframework.beans.FatalBeanException;


public class BlockManagerServerBeanPostProcessor implements DestructionAwareBeanPostProcessor {

	private SQLiteDataSource sqliteDataSource = null;
	private DriverManagerDataSource postgresDataSource = null;
	private DataSourceTransactionManager dataSourceTransactionManager = null;

	private BlockManagerServerApplicationContextParameters params;

	public BlockManagerServerBeanPostProcessor(BlockManagerServerApplicationContextParameters params){
		this.params = params;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		this.params.getBlockModelContext().logMessage("In postProcessBeforeDestruction for " + bean.getClass().getName() + " " + beanName);
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		this.params.getBlockModelContext().logMessage("In requiresDestruction for " + bean.getClass().getName());
		return true;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		this.params.getBlockModelContext().logMessage("In postProcessBeforeInitialization for " + bean.getClass().getName() + " " + beanName);
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		this.params.getBlockModelContext().logMessage("In postProcessAfterInitialization for " + bean.getClass().getName() + " " + beanName);
		try{
			if(beanName.equals("dataSource")){
				return this.dataSource();
			}else if(beanName.equals("blockDAO")){
				return this.blockDAO();
			}else if(beanName.equals("namedParameterJdbcTemplate")){
				return this.namedParameterJdbcTemplate();
			}else if(beanName.equals("transactionManager")){
				return this.transactionManager();
			}else if(beanName.equals("blockManagerServerApplicationContextParameters")){
				return this.blockManagerServerApplicationContextParameters();

			}else{
				return bean;
			}
		}catch(Exception e){
			throw new FatalBeanException("Failed to create bean " + beanName, e);
		}
	}

	public BlockManagerServerApplicationContextParameters blockManagerServerApplicationContextParameters(){
		return this.params;
	}

	public DataSource dataSource() throws Exception{
		if(this.params.getDatabaseConnectionParameters().getSubprotocol().equals("sqlite")){
			if(this.sqliteDataSource == null){
				String subprotocol = this.params.getDatabaseConnectionParameters().getSubprotocol();
				String filename = this.params.getDatabaseConnectionParameters().getFilename();
				this.sqliteDataSource = new SQLiteDataSource();
				this.sqliteDataSource.setUrl("jdbc:" + subprotocol + ":" + filename);
				SQLiteConfig config = new SQLiteConfig();
				config.setDateClass("TEXT");
				config.setJournalMode(JournalMode.MEMORY);
				this.sqliteDataSource.setConfig(config);
				this.sqliteDataSource.setEnforceForeignKeys(true);
			}
			return this.sqliteDataSource;
		}else if(this.params.getDatabaseConnectionParameters().getSubprotocol().equals("postgresql")){
			if(this.postgresDataSource == null){
				String subprotocol = this.params.getDatabaseConnectionParameters().getSubprotocol();
				String hostname = this.params.getDatabaseConnectionParameters().getHostname();
				String port = this.params.getDatabaseConnectionParameters().getPort();
				String databaseName = this.params.getDatabaseConnectionParameters().getDatabaseName();
				String username = this.params.getDatabaseConnectionParameters().getUsername();
				String password = this.params.getDatabaseConnectionParameters().getPassword();
				this.postgresDataSource = new DriverManagerDataSource("jdbc:" + subprotocol + "://" + hostname + ":" + port + "/" + databaseName, username, password);
				this.postgresDataSource.setDriverClassName("org.postgresql.Driver");
			}
			return this.postgresDataSource;
		}else{
			throw new Exception("Unknown subprotocol: '" + this.params.getDatabaseConnectionParameters().getSubprotocol() + "'");
		}
	}

	public DataSourceTransactionManager transactionManager() throws Exception{
		if(this.dataSourceTransactionManager == null){
			this.dataSourceTransactionManager = new DataSourceTransactionManager(this.dataSource());
		}
		return this.dataSourceTransactionManager;
	}

	public NamedParameterJdbcTemplate namedParameterJdbcTemplate() throws Exception{
		return new NamedParameterJdbcTemplate(this.dataSource());
	}

	public BlockDAOImpl blockDAO() throws Exception{
		BlockDAOImpl blockDAOImpl = new BlockDAOImpl();
		blockDAOImpl.setBlockModelContext(this.params.getBlockModelContext());
		blockDAOImpl.setDataSource(this.dataSource());
		blockDAOImpl.setTransactionManager(this.transactionManager());
        	blockDAOImpl.setBlockManagerServerApplicationContextParameters(this.params);
		return blockDAOImpl; 
	}
}
