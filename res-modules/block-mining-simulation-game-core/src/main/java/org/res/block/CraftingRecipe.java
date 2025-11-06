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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.res.block.WorkItem;
import org.res.block.BlockSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class CraftingRecipe {

	private List<PlayerInventoryItemStack> consumedItems;
	private List<PlayerInventoryItemStack> producedItems;

	public List<PlayerInventoryItemStack> getConsumedItems(){
		return this.consumedItems;
	}

	public List<PlayerInventoryItemStack> getProducedItems(){
		return this.producedItems;
	}

	public CraftingRecipe(List<PlayerInventoryItemStack> consumedItems, List<PlayerInventoryItemStack> producedItems) throws Exception{
		this.consumedItems = consumedItems;
		this.producedItems = producedItems;
	}

	@Override
	public final int hashCode(){
		return 1;
	}

	@Override
	public final boolean equals(Object o){
		CraftingRecipe c = (CraftingRecipe)o;
		if(c == null){
			return false;
		}else{
			if(this.getConsumedItems().size() == c.getConsumedItems().size() && this.getProducedItems().size() == c.getProducedItems().size()){
				for(int i = 0; i < this.getConsumedItems().size(); i++){
					if(!this.getConsumedItems().get(i).equals(c.getConsumedItems().get(i))){
						return false;
					}
				}
				for(int i = 0; i < this.getProducedItems().size(); i++){
					if(!this.getProducedItems().get(i).equals(c.getProducedItems().get(i))){
						return false;
					}
				}
				return true;
			}else{
				return false;
			}
		}
	}
}
