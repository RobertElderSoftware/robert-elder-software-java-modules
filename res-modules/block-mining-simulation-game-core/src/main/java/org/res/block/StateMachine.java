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

public abstract class StateMachine<T extends Comparable<T>, U extends StateMachineState>{

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	protected Map<U, Set<T>> stateToObjectsMap = new TreeMap<U, Set<T>>();
	protected Map<T, U> objectToStateMap = new TreeMap<T, U>();
	protected Set<U> allStatesSet;

	public StateMachine(Set<U> allStatesSet) throws Exception{
		this.allStatesSet = allStatesSet;
		for(U state : this.allStatesSet){
			stateToObjectsMap.put(state, new TreeSet<T>());
		}
	}

	public void addObjectIntoStateNoCheck(T object, U state) throws Exception{
		objectToStateMap.put(object, state);
		if(state != null){
			if(stateToObjectsMap.containsKey(state)){
				stateToObjectsMap.get(state).add(object);
			}else{
				throw new Exception("state=" + state);
			}
		}
	}

	public void addObjectIntoState(T object, U state) throws Exception{
		if(objectToStateMap.containsKey(object)){
			throw new Exception("Object " + object + " is already in the map.");
		}else{
			this.addObjectIntoStateNoCheck(object, state);
		}
	}

	public U getStateOfObject(T obj){
		return objectToStateMap.get(obj);
	}

	public void putObjectIntoState(T obj, U state) throws Exception{
		removeObject(obj);
		this.addObjectIntoStateNoCheck(obj, state);
	}

	public Set<T> getObjectSet(){
		return new TreeSet<T>(objectToStateMap.keySet());
	}

	public void removeObject(T obj){
		U currentState = objectToStateMap.get(obj);
		if(currentState != null){
			stateToObjectsMap.get(currentState).remove(obj);
		}
		objectToStateMap.remove(obj);
	}

	public void removeAllObjects(Set<T> objs) throws Exception{
		objectToStateMap.keySet().removeAll(objs);
		for(U state : this.allStatesSet){
			stateToObjectsMap.get(state).removeAll(objs);
		}
	}

	public Set<T> getObjectsInState(U state){
		return new TreeSet<T>(stateToObjectsMap.get(state));
	}
}
