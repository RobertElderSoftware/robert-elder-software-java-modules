//  Copyright (c) 2026 Robert Elder Software Inc.
//   
//  Robert Elder Software Proprietary License (Version 2026-04-09)
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
//  either unmodified, modified, or incorporated into another software product, 
//  except as described in the document "REDISTRIBUTION.md" (a file with SHA256 
//  hash value 'c39a6c8200a22caf30eac97095b78def80c9cab1b6f7ddd3fca7fdae71df43da').
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
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

public class MultiClientChunkSet {

	private Map<InMemoryChunksClient, Set<CuboidAddress>> chunkSet = new TreeMap<InMemoryChunksClient, Set<CuboidAddress>>();

	public MultiClientChunkSet() {

	}

	public boolean contains(InMemoryChunksClient inMemoryChunksClient, CuboidAddress ca){
		if(chunkSet.containsKey(inMemoryChunksClient)){
			if(chunkSet.get(inMemoryChunksClient).contains(ca)){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}

	public void add(InMemoryChunksClient inMemoryChunksClient, CuboidAddress ca){
		if(!chunkSet.containsKey(inMemoryChunksClient)){
			chunkSet.put(inMemoryChunksClient, new TreeSet<CuboidAddress>());
		}
		chunkSet.get(inMemoryChunksClient).add(ca);
	}

	public void remove(InMemoryChunksClient inMemoryChunksClient, CuboidAddress ca) throws Exception{
		if(!chunkSet.containsKey(inMemoryChunksClient)){
			throw new Exception("No entry for this client.");
		}
		chunkSet.get(inMemoryChunksClient).remove(ca);
	}

	public List<Map.Entry<InMemoryChunksClient, CuboidAddress>> getEntireChunkList(){
		List<Map.Entry<InMemoryChunksClient, CuboidAddress>> rtn = new ArrayList<Map.Entry<InMemoryChunksClient, CuboidAddress>>();
		for(Map.Entry<InMemoryChunksClient, Set<CuboidAddress>> e : this.chunkSet.entrySet()){
			for(CuboidAddress ca : e.getValue()){
				rtn.add(Map.entry(e.getKey(), ca));
			}
		}
		return rtn;
	}

	public int getEntireChunkListSize(){
		return getEntireChunkList().size();
	}

	public Map.Entry<InMemoryChunksClient, CuboidAddress> removeOne() throws Exception{
		List<Map.Entry<InMemoryChunksClient, CuboidAddress>> entireChunkList = this.getEntireChunkList();
		if(entireChunkList.size() > 0){
			Map.Entry<InMemoryChunksClient, CuboidAddress> first = entireChunkList.get(0);
			InMemoryChunksClient inMemoryChunksClient = first.getKey();
			CuboidAddress firstAddress = first.getValue();
			this.remove(inMemoryChunksClient, firstAddress);
			return Map.entry(inMemoryChunksClient, firstAddress);
		}else{
			throw new Exception("Cannot remove one: No entires.");
		}
	}
}
