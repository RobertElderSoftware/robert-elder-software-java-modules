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

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;

import java.io.IOException;

public abstract class BlockSession {

	private static final Long MAX_REGION_SUBSCRIPTIONS = 5000L;

	public abstract String getId();
	public abstract void close(String reason) throws Exception;

	protected Object monitor = new Object();
	protected Map<CuboidAddress, Long> subscribedRegions = new TreeMap<CuboidAddress, Long>();
	protected BlockModelContext blockModelContext;

	public BlockSession(BlockModelContext blockModelContext) throws Exception {
		this.blockModelContext = blockModelContext;
	}

	public Map<CuboidAddress, Long> getSubscriptionIntersections(List<CuboidAddress> addresses) throws Exception {
		Map<CuboidAddress, Long> intersections = new TreeMap<CuboidAddress, Long>();
		for(CuboidAddress address : addresses){
			for(Map.Entry<CuboidAddress, Long> existingRegionSubscription : this.subscribedRegions.entrySet()){
				CuboidAddress intersection = existingRegionSubscription.getKey().getIntersectionCuboidAddress(address);
				if(intersection != null){
					intersections.put(intersection, existingRegionSubscription.getValue());
				}
			}
		}
		return intersections;
	}

	public void unsubscribeFromRegions(List<CuboidAddress> regionsToUnsubscribeFrom) throws Exception {
		Long proposedRegionSubscriptionCount = Long.valueOf(this.subscribedRegions.size() - regionsToUnsubscribeFrom.size());
		blockModelContext.logMessage("-proposedRegionSubscriptionCount=" + proposedRegionSubscriptionCount + ", MAX_REGION_SUBSCRIPTIONS=" + BlockSession.MAX_REGION_SUBSCRIPTIONS);
		for(CuboidAddress regionToUnsubscribe : regionsToUnsubscribeFrom){
			this.subscribedRegions.remove(regionToUnsubscribe);
		}
	}

	public void subscribeToRegions(List<CuboidAddress> regionsToSubscribeTo, Long conversationId) throws Exception {
		List<CuboidAddress> newSubscriptionsToAdd = new ArrayList<CuboidAddress>();
		List<CuboidAddress> preExistingSubscription = new ArrayList<CuboidAddress>();
		for(CuboidAddress regionToSubscribeTo : regionsToSubscribeTo){
			boolean addThisRegion = true;
			if(this.subscribedRegions.containsKey(regionToSubscribeTo)){
				//blockModelContext.logMessage("Region " + regionToSubscribeTo + " is already subscribed to. Don't add it.");
				addThisRegion = false;
				preExistingSubscription.add(regionToSubscribeTo);
			}else{
				for(Map.Entry<CuboidAddress, Long> existingRegionSubscription : this.subscribedRegions.entrySet()){
					CuboidAddress intersection = existingRegionSubscription.getKey().getIntersectionCuboidAddress(regionToSubscribeTo);
					if(intersection == null){
						//blockModelContext.logMessage("Region " + regionToSubscribeTo + " has no intersection with " + existingRegionSubscription + ". Add it.");
					}else{
						blockModelContext.logMessage("Region " + regionToSubscribeTo + " has an intersection (" + intersection + ") with " + existingRegionSubscription + " and is not the same. TODO: Consider this case.");
						throw new Exception("Refusing to add a subscription that overlaps with another subscription.");
					}
				}
			}

			if(addThisRegion){
				newSubscriptionsToAdd.add(regionToSubscribeTo);
			}
		}

		Long proposedRegionSubscriptionCount = Long.valueOf(newSubscriptionsToAdd.size() + this.subscribedRegions.size());
		blockModelContext.logMessage("+proposedRegionSubscriptionCount=" + proposedRegionSubscriptionCount + ", MAX_REGION_SUBSCRIPTIONS=" + BlockSession.MAX_REGION_SUBSCRIPTIONS);

		if(preExistingSubscription.size() > 0){
			ErrorNotificationBlockMessage response = new ErrorNotificationBlockMessage(this.blockModelContext, BlockMessageErrorType.IDENTICAL_SUBSCRIPTION, conversationId);
			this.blockModelContext.sendBlockMessage(response, this);
		}else if(proposedRegionSubscriptionCount < BlockSession.MAX_REGION_SUBSCRIPTIONS){
			for(CuboidAddress subscriptionToAdd : newSubscriptionsToAdd){
				//  Track the subscription and the correspond conversation id that goes with it:
				this.subscribedRegions.put(subscriptionToAdd, conversationId);
			}
		}else{
			ErrorNotificationBlockMessage response = new ErrorNotificationBlockMessage(this.blockModelContext, BlockMessageErrorType.MAX_REGION_SUBSCRPTIONS_EXCEEDED, conversationId);
			this.blockModelContext.sendBlockMessage(response, this);
		}
	}
}
