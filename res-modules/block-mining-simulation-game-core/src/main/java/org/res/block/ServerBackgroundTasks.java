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

import org.springframework.boot.web.servlet.support.SpringBootServletInitializer; 
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.lang.Runnable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ServerBackgroundTasks extends Thread {

	private BlockModelContext blockModelContext;

	public ServerBackgroundTasks(BlockModelContext blockModelContext) {
		this.blockModelContext = blockModelContext;
	}

	@Override
	public void run() {
		Long tickNumber = 0L;
		boolean isFinished = false;
		blockModelContext.logMessage("Begin running ServerBackgroundTasks.");
		while (!isFinished) {
			try {
				try{
					/*
						Move a single rock back and forth once in a while:
					*/
					blockModelContext.logMessage("Hello world, tickNumber=" + tickNumber);

					Long numDimensions = 4L;
					List<Cuboid> cuboids = new ArrayList<Cuboid>();

					List<Long> p1 = new ArrayList<Long>();
					p1.add(10L);
					p1.add(0L);
					p1.add(10L);
					p1.add(0L);

					List<Long> p2 = new ArrayList<Long>();
					p2.add(10L);
					p2.add(0L);
					p2.add(11L);
					p2.add(0L);
					
					CuboidAddress blockAddressCuboid = new CuboidAddress(new Coordinate(p1), new Coordinate(p2));
					String rockEmoji = String.valueOf(Character.toChars(0x1FAA8));
					byte [] blockData1 = String.valueOf(tickNumber % 2L == 0L ? rockEmoji : "").getBytes("UTF-8");
					byte [] blockData2 = String.valueOf(tickNumber % 2L == 0L ? "" : rockEmoji).getBytes("UTF-8");

					byte [] allBlockData = new byte[blockData1.length + blockData2.length];

					System.arraycopy(blockData1, 0, allBlockData, 0                , blockData1.length);
					System.arraycopy(blockData2, 0, allBlockData, blockData1.length, blockData2.length);

					cuboids.add(
						new Cuboid(
							blockAddressCuboid,
							new CuboidDataLengths(blockAddressCuboid, new long [] {blockData1.length, blockData2.length}),
							new CuboidData(allBlockData)
						)
					);

					WriteCuboidsWorkItem workItem = new WriteCuboidsWorkItem(this.blockModelContext, numDimensions, cuboids);
					blockModelContext.putWorkItem(workItem, WorkItemPriority.PRIORITY_LOW);
					tickNumber++;

					Thread.sleep(1000000);
				}catch(InterruptedException e){
					blockModelContext.logMessage("Caught a InterruptedException in ServerBackgroundTasks.  Set isFinished = true and gracefully exit.");
					isFinished = true;
				}
			} catch (Exception e) {
				blockModelContext.logMessage("Exception in ServerBackgroundTasks:");
				blockModelContext.logException(e);
			}
		}
	}
}
