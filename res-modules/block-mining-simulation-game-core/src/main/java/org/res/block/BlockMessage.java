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

public abstract class BlockMessage {

	protected BlockModelContext blockModelContext;
	protected Long conversationId;

	public BlockMessage(BlockModelContext blockModelContext, Long conversationId){
		this.blockModelContext = blockModelContext;
		this.conversationId = conversationId;
	}

	public Long getConversationId(){
		return this.conversationId;
	}

	public void setBlockModelContext(BlockModelContext blockModelContext){
		this.blockModelContext = blockModelContext;
	}

	public BlockModelContext getBlockModelContext(){
		return this.blockModelContext;
	}

	public static BlockMessage consumeBlockMessage(BlockModelContext blockModelContext, BlockMessageBinaryBuffer buffer) throws Exception {
		int byteOffsetIntoMessage = 0;

		BlockMessageType blockMessageType = BlockMessage.readBlockMessageType(buffer);
		Long conversationId = buffer.readOneLongValue();
		switch(blockMessageType){
			case BLOCK_MESSAGE_TYPE_PROBE_REGIONS:{
				return new ProbeRegionsRequestBlockMessage(blockModelContext, buffer, conversationId);
			}case BLOCK_MESSAGE_TYPE_DESCRIBE_REGIONS:{
				return new DescribeRegionsBlockMessage(blockModelContext, buffer, conversationId);
			}case BLOCK_MESSAGE_TYPE_ERROR_NOTIFICATION:{
				return new ErrorNotificationBlockMessage(blockModelContext, buffer, conversationId);
			}case BLOCK_MESSAGE_TYPE_ACKNOWLEDGEMENT:{
				return new AcknowledgementBlockMessage(blockModelContext, buffer, conversationId);
			}case BLOCK_MESSAGE_TYPE_AUTHORIZED_COMMAND:{
				return new AuthorizedCommandBlockMessage(blockModelContext, buffer, conversationId);
			}default:{
				throw new Exception("Unknown message type: " + blockMessageType.toString());
			}
		}
	}

	public static BlockMessageType readBlockMessageType(BlockMessageBinaryBuffer buffer) throws Exception{
		long messageType = buffer.readOneLongValue();
		BlockMessageType blockMessageType = BlockMessageType.forValue(messageType);
		if(blockMessageType == null){
			throw new Exception("Did not find a message type for message id=" + messageType);
		}else{
			return blockMessageType;
		}
	}

	public static void writeBlockMessageType(BlockMessageBinaryBuffer buffer, BlockMessageType blockMessageType){
		long messageType = blockMessageType.toLong();
		buffer.writeOneLongValue(messageType);
	}

	public static void writeConversationId(BlockMessageBinaryBuffer buffer, Long conversationId){
		buffer.writeOneLongValue(conversationId);
	}

	public BlockMessage() {

	}

	public abstract void doWork(BlockSession blockSession) throws Exception;
	public abstract byte [] asByteArray() throws Exception;
}
