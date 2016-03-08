## hbase-jdo example sources. ##
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