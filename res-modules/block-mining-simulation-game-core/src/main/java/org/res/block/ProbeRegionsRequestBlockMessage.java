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

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.ArrayList;

public class ProbeRegionsRequestBlockMessage extends BlockMessage {

	private static final Long READ_FLAG_MASK = 0x0001L;
	private static final Long SUBSCRIBE_FLAG_MASK = 0x0002L;
	private Long flags = 0x0L;
	private Long numDimensions = null;
	private List<CuboidAddress> cuboidAddresses = new ArrayList<CuboidAddress>();

	public ProbeRegionsRequestBlockMessage(BlockModelContext blockModelContext, Long numDimensions, List<CuboidAddress> cuboidAddresses, boolean doRead, boolean doSubscribe, Long conversationId){
		super(blockModelContext, conversationId);
		this.numDimensions = numDimensions;
		this.cuboidAddresses = cuboidAddresses;
		if(doRead){
			this.flags |= ProbeRegionsRequestBlockMessage.READ_FLAG_MASK;
		}
		if(doSubscribe){
			this.flags |= ProbeRegionsRequestBlockMessage.SUBSCRIBE_FLAG_MASK;
		}
	}

	public byte [] asByteArray() throws Exception{
		BlockMessageBinaryBuffer buffer = new BlockMessageBinaryBuffer();
		BlockMessage.writeBlockMessageType(buffer, BlockMessageType.BLOCK_MESSAGE_TYPE_PROBE_REGIONS);
		BlockMessage.writeConversationId(buffer, this.conversationId);

		buffer.writeOneLongValue(this.flags);
		buffer.writeOneLongValue(this.numDimensions);
		buffer.writeOneLongValue(this.cuboidAddresses.size());

		for(int i = 0; i < this.cuboidAddresses.size(); i++){
			CuboidAddress.writeCuboidAddress(buffer, this.cuboidAddresses.get(i));
		}

		return buffer.getUsedBuffer();
	}

	public ProbeRegionsRequestBlockMessage(BlockModelContext blockModelContext, BlockMessageBinaryBuffer buffer, Long conversationId) throws Exception {
		super(blockModelContext, conversationId);
		this.flags = buffer.readOneLongValue();
		this.numDimensions = buffer.readOneLongValue();
		Long numCuboidAddresses = buffer.readOneLongValue();
		blockModelContext.logMessage("numDimensions is: " + this.numDimensions);
		blockModelContext.logMessage("numCuboidAddresses is: " + numCuboidAddresses);

		String allMessage = "";
		for(long i = 0; i < numCuboidAddresses; i++){
			this.cuboidAddresses.add(CuboidAddress.readCuboidAddress(buffer, this.numDimensions));
		}
	}

	public Long getNumDimensions(){
		return this.numDimensions;
	}

	public List<CuboidAddress> getCuboidAddresses(){
		return this.cuboidAddresses;
	}

	public void doWork(BlockSession blockSession) throws Exception{
		if((this.flags & ProbeRegionsRequestBlockMessage.READ_FLAG_MASK) != 0L){
			List<Cuboid> cuboids = blockModelContext.getBlockModelInterface().getBlocksInRegions(this.cuboidAddresses);

			DescribeRegionsBlockMessage notifyMessage = new DescribeRegionsBlockMessage(this.blockModelContext, this.numDimensions, cuboids, this.getConversationId());
			SendBlockMessageToSessionWorkItem notifyWorkItem = new SendBlockMessageToSessionWorkItem(this.blockModelContext, blockSession, notifyMessage);

			blockModelContext.putWorkItem(notifyWorkItem, WorkItemPriority.PRIORITY_LOW);
		}

		if((this.flags & ProbeRegionsRequestBlockMessage.SUBSCRIBE_FLAG_MASK) == 0L){
			blockSession.unsubscribeFromRegions(this.cuboidAddresses);
		}else{
			blockSession.subscribeToRegions(blockModelContext, this.cuboidAddresses, this.getConversationId());
		}
	}
}
