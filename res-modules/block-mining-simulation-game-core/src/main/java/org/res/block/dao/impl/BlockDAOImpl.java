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
package org.res.block.dao.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashMap;
import java.util.Arrays;
import java.sql.Types;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import org.res.block.dao.BlockDAO;
import org.res.block.dao.BlockRecordRowMapper;
import org.res.block.dao.BlockRecord;
import org.res.block.Cuboid;
import org.res.block.CuboidAddress;
import org.res.block.CuboidDataLengths;
import org.res.block.CuboidData;
import org.res.block.Coordinate;
import org.res.block.BlockMessageBinaryBuffer;
import org.res.block.BlockUpdateRecord;

import org.res.block.BlockModelContext;
import org.res.block.RegionIteration;
import org.res.block.BlockManagerServerApplicationContextParameters;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.sql.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class BlockDAOImpl implements BlockDAO {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
        private DataSource dataSource = null;

	private BlockManagerServerApplicationContextParameters blockManagerServerApplicationContextParameters = null;

        private NamedParameterJdbcTemplate namedParameterJdbcTemplate = null;
        private JdbcTemplate jdbcTemplate = null;

	private static final Long NUM_DIMENSION_IN_DATABASE = 5L;

	private BlockModelContext blockModelContext;

	private TransactionTemplate transactionTemplate;

	public void setBlockModelContext(BlockModelContext blockModelContext){
		this.blockModelContext = blockModelContext;
                blockModelContext.logMessage("Set blockModelContext in BlockDAOImpl.");
	}

        public void setBlockManagerServerApplicationContextParameters(BlockManagerServerApplicationContextParameters blockManagerServerApplicationContextParameters) {
                blockModelContext.logMessage("Set blockManagerServerApplicationContextParameters in BlockDAOImpl.");
                this.blockManagerServerApplicationContextParameters = blockManagerServerApplicationContextParameters;
        }

        public void setDataSource(DataSource dataSource) {
                blockModelContext.logMessage("Set dataSource in BlockDAOImpl.");
                this.dataSource = dataSource;
                this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
                this.jdbcTemplate = new JdbcTemplate(dataSource);
        }

	public void setTransactionManager(DataSourceTransactionManager transactionManager){
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

        public void createBlockTable() throws Exception{
		String createTableSQL =
		"CREATE TABLE IF NOT EXISTS block (" + 
		"	x0 bigint NOT NULL," + 
		"	x1 bigint NOT NULL," + 
		"	x2 bigint NOT NULL," + 
		"	x3 bigint NOT NULL," + 
		"	x4 bigint NOT NULL," + 
		"	data bytea NOT NULL," + 
		"	last_modified bigint NOT NULL," + 
		"	PRIMARY KEY(x0, x1, x2, x3, x4)" + 
		");";
		Map<String, Object> queryParams = new HashMap<String, Object>();
                logger.info("About to run: " + createTableSQL);
                this.namedParameterJdbcTemplate.update(createTableSQL, queryParams);

		String createIndexSQL =
		"CREATE UNIQUE INDEX block_idx ON block (x0, x1, x2, x3, x4, data, last_modified);";
                logger.info("About to run: " + createIndexSQL);
                this.namedParameterJdbcTemplate.update(createIndexSQL, queryParams);
        }

	public void turnOffAutoCommit() throws Exception{
                this.dataSource.getConnection().setAutoCommit(false);
	}

	public String getDatabaseHexLiteral(byte [] data) throws Exception {
		if(blockManagerServerApplicationContextParameters.getDatabaseConnectionParameters().getSubprotocol().equals("sqlite")){
			return "x'" + BlockModelContext.convertToHex(data) + "'";
		}else if(blockManagerServerApplicationContextParameters.getDatabaseConnectionParameters().getSubprotocol().equals("postgresql")){
			return "decode('" + BlockModelContext.convertToHex(data) + "', 'hex')";
		}else{
			throw new Exception("Unknown db: " + blockManagerServerApplicationContextParameters.getDatabaseConnectionParameters().getSubprotocol());
		}
	}

        public void ensureBlockTableExistsInTransaction(TransactionStatus status) {
		try{
			if(blockManagerServerApplicationContextParameters.getDatabaseConnectionParameters().getSubprotocol().equals("sqlite")){
				String SQL = "SELECT CASE WHEN (SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'block') > 0 THEN true ELSE false END;";
				Map<String, Object> queryParams = new HashMap<String, Object>();
				Boolean hasBlockTable = this.namedParameterJdbcTemplate.queryForObject(SQL, queryParams, Boolean.class);
				if(hasBlockTable){
					logger.info("Verified that table 'block' exists for sqlite.  No need to create.");
				}else{
					logger.info("Verified that table 'block' DOES NOT exist for sqlite.  Need to create...");
					this.createBlockTable();
				}
			}else if(blockManagerServerApplicationContextParameters.getDatabaseConnectionParameters().getSubprotocol().equals("postgresql")){
				String SQL = "SELECT EXISTS (" + 
				"	SELECT FROM information_schema.tables" + 
				"	WHERE  table_schema = 'public'" + 
				"	AND    table_name   = 'block'" + 
				");";
				Map<String, Object> queryParams = new HashMap<String, Object>();
				Boolean hasBlockTable = this.namedParameterJdbcTemplate.queryForObject(SQL, queryParams, Boolean.class);
				if(!hasBlockTable){
					this.createBlockTable();
				}
			}else{
				status.setRollbackOnly();
				this.blockModelContext.getBlockManagerThreadCollection().setIsFinished(true, new Exception("Unknown database subprotocol."));
			}
		}catch(Exception e){
			this.blockModelContext.getBlockManagerThreadCollection().setIsFinished(true, e);
		}
        }

        public void ensureBlockTableExists() throws Exception{
		this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) {
			        ensureBlockTableExistsInTransaction(status);
			}
		});
        }

        public Cuboid getBlocksInRegion(CuboidAddress cuboidAddress) throws Exception{

        	List<String> selectCoordinatesString = new ArrayList<String>();
        	for(long i = 0; i < cuboidAddress.getNumDimensions(); i++){
        		selectCoordinatesString.add("x" + String.valueOf(i));
        	}

        	List<String> coordinateFiltersString = new ArrayList<String>();
        	for(long i = 0; i < cuboidAddress.getNumDimensions(); i++){
        		coordinateFiltersString.add("(x" + String.valueOf(i) + " >= :x" + String.valueOf(i) + "_min AND x" + String.valueOf(i) + " <= :x" + String.valueOf(i) + "_max)");
        	}

        	List<String> orderBysString = new ArrayList<String>();
        	for(long i = cuboidAddress.getNumDimensions() -1L; i >= 0L; i--){
        		orderBysString.add("x" + String.valueOf(i) + " ASC");
        	}

        	String sql = "" + 
			"SELECT\n" +
			String.join(",", selectCoordinatesString) + ",\n" +
			"	data\n" +
			"FROM\n" +
			"	block\n" +
			"WHERE\n" +
			String.join(" AND ", coordinateFiltersString) + "\n" +
			"ORDER BY\n" +
			String.join(",\n", orderBysString) + "\n" +
			";";
		Map<String, Object> queryParams = new HashMap<String, Object>();
        	for(long i = 0; i < cuboidAddress.getNumDimensions(); i++){
			queryParams.put("x" + String.valueOf(i) + "_min", cuboidAddress.getCanonicalLowerCoordinate().getValueAtIndex(i));
			queryParams.put("x" + String.valueOf(i) + "_max", cuboidAddress.getCanonicalUpperCoordinate().getValueAtIndex(i));
        	}
		List<BlockRecord> blocks = this.namedParameterJdbcTemplate.query(sql, queryParams, new BlockRecordRowMapper());
		blockModelContext.logMessage("Did a read request for region: " + cuboidAddress.toString() + "'");
		for(BlockRecord b : blocks){
			blockModelContext.logMessage("Got a block from database: " + b.getCoordinate().getValueAtIndex(0L) + ", " + b.getCoordinate().getValueAtIndex(1L) + ", " + b.getCoordinate().getValueAtIndex(2L) + ": '" + new String(b.getData(), "UTF-8") + "'");
		}

		long cuboidVolumeInBlocks = cuboidAddress.getVolume();
		BlockMessageBinaryBuffer dataForOneCuboid = new BlockMessageBinaryBuffer();
		long [] dataLengths = new long [(int)cuboidVolumeInBlocks];
		for(int j = 0; j < cuboidVolumeInBlocks; j++){
			dataLengths[j] = -1L; /*  Default to block not present. */
		}

		for(int j = 0; j < blocks.size(); j++){
			BlockRecord r = blocks.get(j);
			long blockOffsetInArray = cuboidAddress.getLinearArrayIndexForCoordinate(r.getCoordinate());
			blockModelContext.logMessage("OFFSET IS " + blockOffsetInArray);
			dataLengths[(int)blockOffsetInArray] = (long)r.getData().length;
			dataForOneCuboid.writeBytes(r.getData());
		}

		CuboidDataLengths currentCuboidDataLengths = new CuboidDataLengths(cuboidAddress, dataLengths);
		CuboidData currentCuboidData = new CuboidData(dataForOneCuboid.getUsedBuffer());

		return new Cuboid(cuboidAddress, currentCuboidDataLengths, currentCuboidData);
	}

        public List<Cuboid> getBlocksInRegionsInTransaction(List<CuboidAddress> cuboidAddresses, TransactionStatus status) {
		try{
			List<Cuboid> cuboids = new ArrayList<Cuboid>();

			for(int i = 0; i < cuboidAddresses.size(); i++){
				cuboids.add(this.getBlocksInRegion(cuboidAddresses.get(i)));
			}

			return cuboids;
		}catch(Exception e){
			status.setRollbackOnly();
			this.blockModelContext.getBlockManagerThreadCollection().setIsFinished(true, e);
			return null;
		}
        }

        @Override
        public List<Cuboid> getBlocksInRegions(List<CuboidAddress> cuboidAddresses) throws Exception{
		return transactionTemplate.execute(new TransactionCallback<List<Cuboid>>() {
			public List<Cuboid> doInTransaction(TransactionStatus status) {
				return getBlocksInRegionsInTransaction(cuboidAddresses, status);
			}
		});
        }

        public void writeBlocksInRegionInTransactionWithoutBatch(Cuboid c, TransactionStatus status) {
		try{
			CuboidAddress cuboidAddress = c.getCuboidAddress();
			CuboidDataLengths dataLengths = c.getCuboidDataLengths();
			CuboidData data = c.getCuboidData();

			Coordinate lower = cuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = cuboidAddress.getCanonicalUpperCoordinate();

			Coordinate currentCoordinate = cuboidAddress.getCanonicalLowerCoordinate();

			List<String> rowStrings = new ArrayList<String>();

			RegionIteration regionIteration = new RegionIteration(cuboidAddress.getCanonicalLowerCoordinate(), cuboidAddress);
			do{
				currentCoordinate = regionIteration.getCurrentCoordinate();
				blockModelContext.logMessage("Here is currentCoordinate: " + currentCoordinate);


				long blockOffsetInArray = cuboidAddress.getLinearArrayIndexForCoordinate(currentCoordinate);
				long sizeOfBlock = dataLengths.getLengths()[(int)blockOffsetInArray];
				long offsetOfBlock = dataLengths.getOffsets()[(int)blockOffsetInArray];
				blockModelContext.logMessage("OFFSET index is " + blockOffsetInArray + " offset of block in data is " + offsetOfBlock + " size of data is " + sizeOfBlock);
				byte [] blockData = data.getDataAtOffset(offsetOfBlock, sizeOfBlock);

				String blockClassName = this.blockModelContext.getBlockSchema().getFirstBlockMatchDescriptionForByteArray(blockData);
				blockModelContext.logMessage("Write block of class '" + blockClassName + "' at coordinate:" + currentCoordinate.toString() + " with data '" + new String(blockData, "UTF-8") + "'.");
				if(blockClassName == null){
					throw new Exception("Refusing to allow block of unrecogned type into database.");
				}

				List<String> coordinateParts = new ArrayList<String>();
				for(int index = 0; index < NUM_DIMENSION_IN_DATABASE; index++){
					if(index < cuboidAddress.getNumDimensions()){
						coordinateParts.add(String.valueOf(currentCoordinate.getValueAtIndex(Long.valueOf(index))));
					}else{
						coordinateParts.add(String.valueOf(0L)); //  Coordinate was not specified.
					}
				}

				rowStrings.add("(" + String.join(",", coordinateParts) + ", " + getDatabaseHexLiteral(blockData) + ", 0)");
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());

			List<String> coordinateStrings = new ArrayList<String>();
			for(long i = 0; i < NUM_DIMENSION_IN_DATABASE; i++){
				coordinateStrings.add("x" + String.valueOf(i));
			}

			String sql = 
				"INSERT INTO\n" +
				"	block\n" +
				"	(\n" +
				"		" + String.join(",", coordinateStrings) + ",\n" +
				"		data,\n" +
				"		last_modified\n" +
				"	)\n" +
				"VALUES\n" + String.join(",\n", rowStrings) +
				"ON CONFLICT (" + String.join(",", coordinateStrings) + ") DO UPDATE\n" +
				"SET\n" +
				"	data = EXCLUDED.data,\n" +
				"	last_modified = EXCLUDED.last_modified\n";

			blockModelContext.logMessage("Here is the query: " + sql);

			this.jdbcTemplate.update(sql);
        	}catch(Exception e){
			status.setRollbackOnly();
			this.blockModelContext.getBlockManagerThreadCollection().setIsFinished(true, e);
        	}
        }

        public void writeBlocksInRegionInTransaction(Cuboid c, TransactionStatus status) {
		try{
			CuboidAddress cuboidAddress = c.getCuboidAddress();
			CuboidDataLengths dataLengths = c.getCuboidDataLengths();
			CuboidData data = c.getCuboidData();

			Coordinate lower = cuboidAddress.getCanonicalLowerCoordinate();
			Coordinate upper = cuboidAddress.getCanonicalUpperCoordinate();

			Coordinate currentCoordinate = cuboidAddress.getCanonicalLowerCoordinate();


			List<BlockUpdateRecord> blockUpdateRecords = new ArrayList<BlockUpdateRecord>();

			RegionIteration regionIteration = new RegionIteration(cuboidAddress.getCanonicalLowerCoordinate(), cuboidAddress);
			do{
				currentCoordinate = regionIteration.getCurrentCoordinate();
				blockModelContext.logMessage("Here is currentCoordinate: " + currentCoordinate);


				long blockOffsetInArray = cuboidAddress.getLinearArrayIndexForCoordinate(currentCoordinate);
				long sizeOfBlock = dataLengths.getLengths()[(int)blockOffsetInArray];
				long offsetOfBlock = dataLengths.getOffsets()[(int)blockOffsetInArray];
				blockModelContext.logMessage("OFFSET index is " + blockOffsetInArray + " offset of block in data is " + offsetOfBlock + " size of data is " + sizeOfBlock);
				byte [] blockData = data.getDataAtOffset(offsetOfBlock, sizeOfBlock);

				String blockClassName = this.blockModelContext.getBlockSchema().getFirstBlockMatchDescriptionForByteArray(blockData);
				blockModelContext.logMessage("Write block of class '" + blockClassName + "' at coordinate:" + currentCoordinate.toString() + " with data '" + new String(blockData, "UTF-8") + "'.");
				if(blockClassName == null){
					throw new Exception("Refusing to allow block of unrecogned type into database.");
				}

				blockUpdateRecords.add(new BlockUpdateRecord(currentCoordinate, blockData));
			}while (regionIteration.incrementCoordinateWithinCuboidAddress());

			List<String> coordinateStrings = new ArrayList<String>();
			for(long i = 0; i < NUM_DIMENSION_IN_DATABASE; i++){
				coordinateStrings.add("x" + String.valueOf(i));
			}

			List<String> coordinateParamQuestionMarks = new ArrayList<String>();
			for(long i = 0; i < NUM_DIMENSION_IN_DATABASE; i++){
				coordinateParamQuestionMarks.add("?");
			}

			this.jdbcTemplate.batchUpdate(
				"INSERT INTO\n" +
				"	block\n" +
				"	(\n" +
				"		" + String.join(",", coordinateStrings) + ",\n" +
				"		data,\n" +
				"		last_modified\n" +
				"	)\n" +
				"VALUES\n" +
				"	(\n" +
				"		" + String.join(",", coordinateParamQuestionMarks) + ",\n" +
				"		?,\n" +
				"		?\n" +
				"	)\n" +
				"ON CONFLICT (" + String.join(",", coordinateStrings) + ") DO UPDATE\n" +
				"SET\n" +
				"	data = EXCLUDED.data,\n" +
				"	last_modified = EXCLUDED.last_modified\n",
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						BlockUpdateRecord blockUpdateRecord = blockUpdateRecords.get(i);

						for(int index = 0; index < NUM_DIMENSION_IN_DATABASE; index++){
							if(index < cuboidAddress.getNumDimensions()){
								ps.setLong(index + 1, blockUpdateRecord.getCoordinate().getValueAtIndex(Long.valueOf(index)));
							}else{
								ps.setLong(index + 1, 0L);
							}
						}

						ps.setBytes(NUM_DIMENSION_IN_DATABASE.intValue() + 1, blockUpdateRecord.getData());

						ps.setLong(NUM_DIMENSION_IN_DATABASE.intValue() + 2, 0L); // Last modified
					}

					public int getBatchSize() {
						return blockUpdateRecords.size();
					}
				}
			);
        	}catch(Exception e){
			status.setRollbackOnly();
			this.blockModelContext.getBlockManagerThreadCollection().setIsFinished(true, e);
        	}
        }

        @Override
        public void writeBlocksInRegion(Cuboid c) throws Exception{
		this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) {
			        //writeBlocksInRegionInTransaction(c, status);
			        writeBlocksInRegionInTransactionWithoutBatch(c, status);
			}
		});
	}
}
