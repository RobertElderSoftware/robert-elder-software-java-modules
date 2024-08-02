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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.ArrayList;

public class DescribeRegionsBlockMessage extends BlockMessage {

	private Long numDimensions = null;
	private List<Cuboid> cuboids = null;

	public DescribeRegionsBlockMessage(BlockModelContext blockModelContext, Long numDimensions, List<Cuboid> cuboids, Long conversationId) throws Exception {
		super(blockModelContext, conversationId);
		this.numDimensions = numDimensions;
		this.cuboids = cuboids;
	}

	public byte [] asByteArray() throws Exception{
		BlockMessageBinaryBuffer buffer = new BlockMessageBinaryBuffer();
		BlockMessage.writeBlockMessageType(buffer, BlockMessageType.BLOCK_MESSAGE_TYPE_DESCRIBE_REGIONS);
		BlockMessage.writeConversationId(buffer, this.conversationId);

		buffer.writeOneLongValue(this.numDimensions);
		buffer.writeOneLongValue(this.cuboids.size());

		for(int i = 0; i < this.cuboids.size(); i++){
			CuboidAddress.writeCuboidAddress(buffer, this.cuboids.get(i).getCuboidAddress());
		}

		for(int i = 0; i < this.cuboids.size(); i++){
			CuboidDataLengths.writeCuboidDataLengths(buffer, this.cuboids.get(i).getCuboidDataLengths());
		}

		for(int i = 0; i < this.cuboids.size(); i++){
			CuboidData.writeCuboidData(buffer, this.cuboids.get(i).getCuboidData());
		}

		return buffer.getUsedBuffer();
	}

	public DescribeRegionsBlockMessage(BlockModelContext blockModelContext, BlockMessageBinaryBuffer buffer, Long conversationId) throws Exception {
		super(blockModelContext, conversationId);
		this.numDimensions = buffer.readOneLongValue();
		Long numCuboids = buffer.readOneLongValue();

		List<CuboidAddress> cuboidAddresses = new ArrayList<CuboidAddress>();
		List<CuboidDataLengths> cuboidDataLengths = new ArrayList<CuboidDataLengths>();
		List<CuboidData> cuboidData = new ArrayList<CuboidData>();

		for(int i = 0; i < numCuboids; i++){
			cuboidAddresses.add(CuboidAddress.readCuboidAddress(buffer, numDimensions));
		}

		for(int i = 0; i < numCuboids; i++){
			cuboidDataLengths.add(CuboidDataLengths.readCuboidDataLengths(buffer, cuboidAddresses.get(i)));
		}

		for(int i = 0; i < numCuboids; i++){
			cuboidData.add(CuboidData.readCuboidData(buffer, cuboidDataLengths.get(i)));
		}

		this.cuboids = new ArrayList<Cuboid>();
		for(int i = 0; i < numCuboids; i++){
			this.cuboids.add(new Cuboid(cuboidAddresses.get(i), cuboidDataLengths.get(i), cuboidData.get(i)));
		}
	}

	public Long getNumDimensions(){
		return this.numDimensions;
	}

	public Long getNumCuboids(){
		return Long.valueOf(this.cuboids.size());
	}

	public void doWork(BlockSession blockSession) throws Exception{
		blockModelContext.logMessage("in doWork for DescribeRegionsBlockMessage numDimensions=" + this.numDimensions + " getNumCuboids()=" + this.cuboids.size());
		for(Cuboid c : this.cuboids){
			blockModelContext.logMessage("cuboid.getCuboidAddress()=" + c.getCuboidAddress());
		}
		WriteCuboidsWorkItem workItem = new WriteCuboidsWorkItem(this.blockModelContext, this.numDimensions, this.cuboids);
		blockModelContext.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);

		AcknowledgementBlockMessage acknowledgementBlockMessage = new AcknowledgementBlockMessage(this.blockModelContext, this.conversationId);
		SendBlockMessageToSessionWorkItem notifyWorkItem = new SendBlockMessageToSessionWorkItem(this.blockModelContext, blockSession, acknowledgementBlockMessage);
		blockModelContext.putWorkItem(notifyWorkItem, WorkItemPriority.PRIORITY_LOW);
	}
}
