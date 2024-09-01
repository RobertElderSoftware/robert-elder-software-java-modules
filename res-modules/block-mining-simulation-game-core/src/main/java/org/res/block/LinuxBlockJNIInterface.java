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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

public class LinuxBlockJNIInterface {
	static {
		try{
			boolean loadInsideJar = true;
			if(loadInsideJar){ //  If loading the library from inside a jar file
				String libraryResource = "/liblinux_block_jni.so";
				InputStream inputStream = LinuxBlockJNIInterface.class.getResourceAsStream(libraryResource);
				if(inputStream == null){
					throw new Exception("InputStream was null when trying to load libraryResource='" + libraryResource + "' from jar.");
				}else{
					File file = File.createTempFile("lib", ".so");
					OutputStream outputStream = new FileOutputStream(file);
					byte[] buffer = new byte[4096];
					int length;
					while ((length = inputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, length);
					}
					inputStream.close();
					outputStream.close();

					System.load(file.getAbsolutePath());
					file.deleteOnExit();
				}


			}else{ //  If loading the library as 'liblinux_block_jni.so' using -Djava.library.path=...
				System.loadLibrary("linux_block_jni");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public native void shutdownInXMilliseconds(int millisecondsTimeout);
	public native String getSIGWINCH();
	public native String nativePrint(String text);
	public native void setupSIGWINCHSignalHandler();
}
