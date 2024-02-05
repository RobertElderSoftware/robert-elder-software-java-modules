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

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;

import java.io.IOException;

public abstract class BlockSession {

	public abstract String getId();
	public abstract void close(String reason) throws Exception;

	protected Object monitor = new Object();
	protected Set<CuboidAddress> subscribedRegions = new TreeSet<CuboidAddress>();
	protected BlockModelContext blockModelContext;

	public BlockSession(BlockModelContext blockModelContext) throws Exception {
		this.blockModelContext = blockModelContext;
	}

	public List<CuboidAddress> getCuboidAddressesWithSubscriptionIntersections(List<CuboidAddress> addresses) throws Exception {
		/* TODO:  Re-consider how this will work with partial region intersections, and duplicate/split regions. */
		List<CuboidAddress> rtn = new ArrayList<CuboidAddress>();
		for(CuboidAddress address : addresses){
			for(CuboidAddress existingRegionSubscription : this.subscribedRegions){
				CuboidAddress intersection = existingRegionSubscription.getIntersectionCuboidAddress(address);
				if(intersection != null){
					rtn.add(address);
				}
			}
		}
		return rtn;
	}

	public void subscribeToRegions(List<CuboidAddress> regionsToSubscribeTo) throws Exception {
		List<CuboidAddress> newSubscriptionsToAdd = new ArrayList<CuboidAddress>();
		for(CuboidAddress regionToSubscribeTo : regionsToSubscribeTo){
			boolean addThisRegion = true;
			if(this.subscribedRegions.contains(regionToSubscribeTo)){
				//blockModelContext.logMessage("Region " + regionToSubscribeTo + " is already subscribed to. Don't add it.");
				addThisRegion = false;
			}else{
				for(CuboidAddress existingRegionSubscription : this.subscribedRegions){
					CuboidAddress intersection = existingRegionSubscription.getIntersectionCuboidAddress(regionToSubscribeTo);
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
		this.subscribedRegions.addAll(newSubscriptionsToAdd);
	}

}
