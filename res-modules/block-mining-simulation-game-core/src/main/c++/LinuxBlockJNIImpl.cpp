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
#include "LinuxBlockJNIImpl.h"

#include <memory>
#include <functional>
#include <iostream>
#include <csignal>

#include <jni.h>
#include <semaphore.h>
#include "org_res_block_LinuxBlockJNIInterface.h"   // auto-generated by `javac -d . -h . LinuxBlockJNIInterface.java`
#include <mutex>
#include <condition_variable>
#include <thread>

using std::string;
using std::function;
using std::unique_ptr;
using std::shared_ptr;
using std::cout;
using std::endl;


class semaphore {
	std::mutex mutex_;
	std::condition_variable condition_;
	unsigned long count_ = 0; // Initialize as locked.

public:
	void release() {
		std::lock_guard<decltype(mutex_)> lock(mutex_);
		++count_;
		condition_.notify_one();
	}

	void acquire() {
		std::unique_lock<decltype(mutex_)> lock(mutex_);
		while(!count_)
			condition_.wait(lock);
		--count_;
	}

	bool try_acquire() {
		std::lock_guard<decltype(mutex_)> lock(mutex_);
		if(count_) {
			--count_;
			return true;
		}
		return false;
	}
};

semaphore SIGWINCH_semaphore;
semaphore getSIGWINCH_BLOCKING_semaphore;

class jstring_deleter {
	JNIEnv *m_env;
	jstring m_jstr;

public:

	jstring_deleter(JNIEnv *env, jstring jstr) : m_env(env), m_jstr(jstr) {

	}

	void operator()(const char *cstr) {
		//cout << "[DEBUG] Releasing " << cstr << endl;
		m_env->ReleaseStringUTFChars(m_jstr, cstr);
	}
};

const string ToString(JNIEnv *env, jstring jstr){
    jstring_deleter deleter(env, jstr);
    unique_ptr<const char, jstring_deleter> pcstr(env->GetStringUTFChars(jstr, JNI_FALSE), deleter );

    return string( pcstr.get() );
}

shared_ptr<const char> ToStringPtr(JNIEnv *env, jstring jstr){
	function<void(const char*)> deleter = [env, jstr](const char *cstr) -> void {
		//cout << "[DEBUG] Releasing " << cstr << endl;
		env->ReleaseStringUTFChars(jstr, cstr);
	};

	return shared_ptr<const char>(env->GetStringUTFChars(jstr, JNI_FALSE), deleter);
}

JNIEXPORT void JNICALL Java_org_res_block_LinuxBlockJNIInterface_setupSIGWINCHSignalHandler (JNIEnv *env, jobject thisObj){
	setupSIGWINCHSignalHandler();
}

JNIEXPORT void JNICALL Java_org_res_block_LinuxBlockJNIInterface_shutdownInXMilliseconds (JNIEnv *env, jobject thisObj, jint arg){
	shutdownInXMilliseconds((int)arg);
}

JNIEXPORT jstring JNICALL Java_org_res_block_LinuxBlockJNIInterface_getSIGWINCH (JNIEnv *env, jobject thisObj){
	string rtn = getSIGWINCH();
	return env->NewStringUTF(rtn.c_str());
}

JNIEXPORT jstring JNICALL Java_org_res_block_LinuxBlockJNIInterface_nativePrint (JNIEnv *env, jobject thisObj, jstring arg){
	string rtn = nativePrint(ToString(env, arg));
	return env->NewStringUTF(rtn.c_str());
}

void handle_sigwinch(int sig) {
	SIGWINCH_semaphore.release();             //  Make one SIGWINCH signal available to consume.
	getSIGWINCH_BLOCKING_semaphore.release(); //  Wake up thread
}

void quit_in_x_milliseconds(int sleep_milliseconds) {
	std::this_thread::sleep_for(std::chrono::milliseconds(sleep_milliseconds));
	getSIGWINCH_BLOCKING_semaphore.release(); //  Wake up thread and allow it to quit.
}

void setupSIGWINCHSignalHandler(void) {
	std::signal(SIGWINCH, &handle_sigwinch);
}

void shutdownInXMilliseconds(int sleep_milliseconds) {
	std::thread t1(quit_in_x_milliseconds, sleep_milliseconds);
	t1.detach();
}

const string nativePrint(const string &text) {
	cout << text;
	return text;
}

const string getSIGWINCH() {
	getSIGWINCH_BLOCKING_semaphore.acquire(); // Block on getting signal, or shutting down.
	if(SIGWINCH_semaphore.try_acquire()){ //  Try to consume one signal if available.
		return "{\"SIGNAL\":\"SIGWINCH\"}";
	}else{ //  If wokeup and no signal, must need to shut down
		return "{\"EVENT\":\"QUIT\"}";
	}
}
