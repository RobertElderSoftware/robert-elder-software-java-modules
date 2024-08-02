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

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.res.block.dao.impl.BlockDAOImpl;

import javax.sql.DataSource;

import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig;

/*
	The spring framework seems to be designed in such a way that the 'application context' is
	always assumed to be stored in .xml file bean definitions, however, this makes the 
	application extremely unmaintainable due to the need to maintain all the applicationContext.xml
	.xml files and keep then in the right locaiton(s) upon deployment.  The spring framework
	makes it very difficult to control the bean definitions purely from code, and the only way
	that I was able to achieve this is to effectively use this class as a placeholder for the
	@Autowired variables, and then actually produce the real values in the
	BlockManagerServerBeanPostProcessor class.
*/

@Configuration
public class BlockManagerServerApplicationContext {

	@Bean
	public Object blockManagerServerApplicationContextParameters(){
		return new Object();
	}

	@Bean
	public Object dataSource(){
		return new Object();
	}

	@Bean
	public Object transactionManager(){
		return new Object();
	}

	@Bean
	public Object namedParameterJdbcTemplate(){
		return new Object();
	}

	@Bean
	public Object blockDAO(){
		return new Object();
	}
}
