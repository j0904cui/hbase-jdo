# What is HBase-util? #
HBase-util is open source module that enables it to store bean class directly into HBase tables (http://hbase.org/) running
on the Hadoop Distributed FileSystem (http://hadoop.apache.org/core/)

this project contributed apache hbase(http://wiki.apache.org/hadoop/Hbase)

**This is not JDO (persistence api). just simple module for hbase**

hbase-util can make to handle the hbase more easily

this project can help you for executing java program simply.
http://code.google.com/p/simple-java-executor/

## Release Notes ##
  * 2011-01-21 ver0.1 release for hbase 0.90
  * 2011-01-25 ver0.11 release for hbase 0.20.6
  * **2011-03-14 ver0.2.1 release**

![http://blogfile.paran.com/BLOG_1164657/201103/1300089626_hbasetool_conv.PNG.jpg](http://blogfile.paran.com/BLOG_1164657/201103/1300089626_hbasetool_conv.PNG.jpg)

## Features ##
  * can handle like java data objects. (no persistence api)
  * sequence generator
  * search with index and paramter
  * querys to insert,update,select,delete
  * general util(upload big file)
  * HTable Pool by using apache pool.
  * Bean class with anotation

## Dependency ##
- hbase libraries. (http://hbase.apache.org)

- commons-beanutils.jar (http://commons.apache.org/)

- commons-pool-1.5.5.jar (http://commons.apache.org/)

- hbase-transactionl.jar

> (https://github.com/hbase-trx/hbase-transactional-tableindexed)


## Example source ##

  * HBaseExample.java
```
package com.apache.hadoop.hbase.client.jdo.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.tableindexed.IndexedTable;

import com.apache.hadoop.hbase.client.jdo.AbstractHBaseDBO;
import com.apache.hadoop.hbase.client.jdo.HBaseBigFile;
import com.apache.hadoop.hbase.client.jdo.HBaseDBOImpl;
import com.apache.hadoop.hbase.client.jdo.query.DeleteQuery;
import com.apache.hadoop.hbase.client.jdo.query.HBaseOrder;
import com.apache.hadoop.hbase.client.jdo.query.HBaseParam;
import com.apache.hadoop.hbase.client.jdo.query.InsertQuery;
import com.apache.hadoop.hbase.client.jdo.query.QSearch;
import com.apache.hadoop.hbase.client.jdo.query.SelectQuery;
import com.apache.hadoop.hbase.client.jdo.query.UpdateQuery;

/**
 * Hbase JDO Example.
 * 
 * dependency library.
 * - commons-beanutils.jar
 * - commons-pool-1.5.5.jar
 * - hbase0.90.0-transactionl.jar
 * 
 * you can expand Delete,Select,Update,Insert Query classes.
 * @author ncanis
 *
 */
public class HBaseExample {
	public static void main(String[] args) throws Exception {
		AbstractHBaseDBO dbo = new HBaseDBOImpl();
		
		//*drop if table is already exist.*
		if(dbo.isTableExist("user")){
			dbo.deleteTable("user");
		}
		
		//*create table*
		dbo.createTableIfNotExist("user",HBaseOrder.DESC,"account");
		//dbo.createTableIfNotExist("user",HBaseOrder.ASC,"account");
		
		//create index.
		String[] cols={"id","name"};
		dbo.addIndexExistingTable("user","account",cols);
		
		//insert
		InsertQuery insert = dbo.createInsertQuery("user");
		UserBean bean = new UserBean();
		bean.setFamily("account");
		bean.setAge(20);
		bean.setEmail("ncanis@gmail.com");
		bean.setId("ncanis");
		bean.setName("ncanis");
		bean.setPassword("1111");
		insert.insert(bean);
		
		//select 1 row
		SelectQuery select = dbo.createSelectQuery("user");
		UserBean resultBean = (UserBean)select.select(bean.getRow(),UserBean.class);
		
		// select column value.
		String value = (String)select.selectColumn(bean.getRow(),"account","id",String.class);
		
		// search with option (QSearch has EQUAL, NOT_EQUAL, LIKE)
		// select id,password,name,email from account where id='ncanis' limit startRow,20
		HBaseParam param = new HBaseParam();
		param.setPage(bean.getRow(),20);
		param.addColumn("id","password","name","email");
		param.addSearchOption("id","ncanis",QSearch.EQUAL);
		select.search("account", param, UserBean.class);
		
		// search column value is existing.
		boolean isExist = select.existColumnValue("account","id","ncanis".getBytes());
		
		// update password.
		UpdateQuery update = dbo.createUpdateQuery("user");
		Hashtable<String, byte[]> colsTable = new Hashtable<String, byte[]>();
		colsTable.put("password","2222".getBytes());
		update.update(bean.getRow(),"account",colsTable);
		
		//delete
		DeleteQuery delete = dbo.createDeleteQuery("user");
		delete.deleteRow(resultBean.getRow());
	
		////////////////////////////////////
		// etc
		
		// HTable pool with apache commons pool
		// borrow and release. HBasePoolManager(maxActive, minIdle etc..)
		IndexedTable table = dbo.getPool().borrow("user");
		dbo.getPool().release(table);
		
		// upload bigFile by hadoop directly.
		HBaseBigFile bigFile = new HBaseBigFile();
		File file = new File("doc/movie.avi");
		FileInputStream fis = new FileInputStream(file);
		Path rootPath = new Path("/files/");
		String filename = "movie.avi";
		bigFile.uploadFile(rootPath,filename,fis,true);
		
		// receive file stream from hadoop.
		Path p = new Path(rootPath,filename);
		InputStream is = bigFile.path2Stream(p,4096);
		
		
	}
}
```

  * UserBean .java

```
package com.apache.hadoop.hbase.client.jdo.examples;
import com.apache.hadoop.hbase.client.jdo.AbstractHBaseBean;


/**
 * Test UserBean
 * @author ncanis
 *
 */
public class UserBean extends AbstractHBaseBean{
	@Column@Index
	private String id;
	@Column
	private String password;
	@Column@Index
	private String name;
	@Column
	private String email;
	@Column
	private int age;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	
	
}
```