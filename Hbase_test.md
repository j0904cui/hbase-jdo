## Stress test ##

```
package com.apache.hadoop.hbase.test;

import java.io.BufferedInputStream;

/**
 * Stress Test
 * @author ncanis@gmail.com
 *
 */
public class StressTest extends AbstractHBase{
	private String tableName ="tost_indexed_table";
	private String family[] = {"messages"};
	private String cols[] = {"pid","title","name","content"};
	private volatile int magCount = 0;
	private SecureRandom srand = null;
	
	public StressTest(String tableName, String[] family, String[] cols){
		this.tableName = tableName;
		this.family = family;
		this.cols = cols;
		srand = new SecureRandom(Bytes.toBytes(System.currentTimeMillis()));
	}
	
	public void deleteTable() throws IOException {		
		deleteTable(tableName);		
	}
	public void createTable() throws IOException {
		createIndexTable(tableName, family);
		List<IndexSpecification> list = new ArrayList<IndexSpecification>(cols.length);
		for(String col:cols) {
			list.add(makeSpec(family[0],col));
		}
	}
	
	public void addIndexes() throws IOException{
		List<IndexSpecification> list = new ArrayList<IndexSpecification>(cols.length);
		for(String col:cols) {
			list.add(makeSpec(family[0],col));
		}
		addIndexExistingTable(tableName, list);
	}
	
	public void insertThread(final int mag, final int count) {
		final long start = System.currentTimeMillis();
		ExecutorService ex = Executors.newFixedThreadPool(10);
		for(int i=0;i<mag;i++) {
			ex.execute(new Runnable() {
				@Override
				public void run() {
					try {
						insert(count);
						magCount++;
						if(magCount==mag) {
							long end = System.currentTimeMillis();
							log.debug("### {}ea Insert Finished. time={} ###",count*mag,(end-start));
							getCount();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	public void insert(int count) throws IOException{
		long start = System.currentTimeMillis();
		IndexedTable table = new IndexedTable(config, tableName.getBytes());
		table.setWriteBufferSize(1024*1024*1);
		table.setAutoFlush(false);
		
		// row key 등록
		int i =0;
		for (i = 1; i <= count; i++) {
			String key= "key"+srand.nextLong();
			Put p = new Put(key.getBytes());
			for(String col:cols) {
				p.add(family[0].getBytes(), col.getBytes(), (col+"-" + i).getBytes());				
			}
			table.put(p);
			log.debug(i+" data added. data={}",toRowLog(p));
		}
		table.flushCommits();		
		table.close();
		long end = System.currentTimeMillis();
		log.debug("{}ea insert Finished. time={}ms",count,(end-start));
		getCount();
	}
	
	private String toRowLog(Put p){
		Collection<List<KeyValue>> cols = p.getFamilyMap().values();
		StringBuffer sb = new StringBuffer();
		for(List<KeyValue> list: cols){
			for(KeyValue kv:list){
				sb.append(Bytes.toString(kv.getFamily())).append("=").append(Bytes.toString(kv.getValue()));
				sb.append(" ");
			}
		}
		return sb.toString();
	}
	
	public void getCount() throws IOException {
		long start = System.currentTimeMillis();
		IndexedTable table = new IndexedTable(config, tableName.getBytes());
		table.setScannerCaching(1000);// this is important.
		table.setAutoFlush(false);
		table.setWriteBufferSize(1024*1024*12);
		
		FirstKeyOnlyFilter filter = new FirstKeyOnlyFilter();
		Scan scan = new Scan();
		scan.setFilter(filter);
//		scan.addColumn(COL_MSG.getBytes(),"pid".getBytes()); // pid row count		
		ResultScanner rs = table.getScanner(scan);
		Iterator it = rs.iterator();
		int count = 0;
		while(it.hasNext()){
			it.next();
			count++;
		}
		rs.close();
		long end = System.currentTimeMillis();
		log.debug("### {} total row count={}, time={}ms ###",new Object[]{table,count,(end-start)});
	}
	
//	private void testKey(){
//		IndexKeyGenerator gen = new SimpleIndexKeyGenerator();
//		Map <byte[], byte[]> map = new HashMap<byte[], byte[]>();
//		map.put(Bytes.toBytes(0), COL_MSG.getBytes());
//		byte[] key = gen.createIndexKey(TABLE.getBytes(), map);
//		System.out.println(new String(key));
//	}
	
	public void search() throws IOException {
		long start = System.currentTimeMillis();
		IndexedTable table = new IndexedTable(config, tableName.getBytes());
		table.setScannerCaching(1000);
//		table.setScannerCaching(100);
		
//		Scan scan = new Scan();
//		scan.addColumn(family[0].getBytes(), cols[0].getBytes());
//		scan.addColumn(family[0].getBytes(), cols[1].getBytes());
//		scan.addColumn(family[0].getBytes(), cols[2].getBytes());
//		scan.addColumn(family[0].getBytes(), cols[3].getBytes());

//		scan.setFilter(new PageFilter(4)); // 20ms
		
		// 53sec
		
		RowFilter filter = new RowFilter(CompareOp.GREATER, new BinaryPrefixComparator("999996".getBytes()));
		
		
//		Filter filter = new SingleColumnValueFilter(
//				Bytes.toBytes(family[0]), 
//				Bytes.toBytes(cols[0]),
//				CompareOp.EQUAL, 
//				Bytes.toBytes("pid-999996"));
		
		/*
		 * index key = value + base row key
		 */
		byte[] startRow = Bytes.toBytes("999996");
		byte[] endRow = null;
//		
//		
//		filter = rowFilter;
		/*
		 * String indexId, 
		 * final byte[] indexStartRow,  
		 * final byte[] indexStopRow,
      	   byte[][] indexColumns, 
      	   final Filter indexFilter,
      	   final byte[][] baseColumns
		 */
		ResultScanner scanner = table.getIndexedScanner(
				cols[0], // index id
				startRow, // start row
				endRow,  // end row
		        
		        new byte[][] {  // index columns.
					Bytes.toBytes(family[0]+":"+cols[0])					
				},
				filter, // filter
		        new byte[][] {  //base columns.
					Bytes.toBytes(family[0]+":"+cols[0]),
					Bytes.toBytes(family[0]+":"+cols[1]),
					Bytes.toBytes(family[0]+":"+cols[2]),
					Bytes.toBytes(family[0]+":"+cols[3])
				}
		);
		
		toList("search", scanner);
		
		long end = System.currentTimeMillis();
		log.debug("### searching end time={}ms ###",(end-start));
	}
	
	public void addIndex(String indexID) throws IOException{
		Collection<IndexSpecification> c = getIndexList(tableName);
		for(IndexSpecification is:c){
			log.debug("existing index={}",is.getIndexId());
		}
		
		if(isIndexExist(tableName, indexID)) {
			removeIndex(tableName,indexID);
		}
		
		String indexTable = tableName+"-"+"messages"; 
		if(isTableExist(indexTable)){
			deleteTable(indexTable);
		}
		addIndexExistingTable(tableName,makeSpec(family[0],indexID));
	}
	
	public void insertBigFile() throws IOException {
		long start = System.currentTimeMillis();
		HTable table = new HTable(config, tableName.getBytes());
		table.setWriteBufferSize(1024*1024*1);
		table.setAutoFlush(false);
		
		byte[] data = readFully(new File("doc/movie.avi"));
		
		
		String key= "key"+srand.nextLong();
		Put p = new Put(key.getBytes());		
		p.add(family[0].getBytes(), "file".getBytes(),data);				
		table.put(p);

		table.flushCommits();		
		table.close();
		long end = System.currentTimeMillis();
		log.debug("big file inserting Finished. time={}ms",(end-start));
	}
	
	private byte[] readFully(File file) throws IOException {
		FileInputStream in = null;
		ByteArrayOutputStream baos = null;
		BufferedInputStream bis = null;
		
		try{
			in = new FileInputStream(file);
			baos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];

			bis = new BufferedInputStream(in);
			int read = 0;
			while((read=bis.read(buf))!=-1) {
				baos.write(buf, 0, read);
			}
		}catch(IOException e){
			throw e;
		}finally {
			bis.close();
			baos.close();
			in.close();
		}
		
		return baos.toByteArray();
	}
	
	private void showDesc() throws IOException{
		IndexedTable table = new IndexedTable(config, tableName.getBytes());
		HTableDescriptor desc = table.getTableDescriptor();
		HColumnDescriptor[] hcd = desc.getColumnFamilies();
		for(HColumnDescriptor col:hcd){
			System.out.println(col);
		}
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String table ="tost_indexed_table";
		String[] family = {"messages"};
		String[] cols = {"pid","title","name","content"};
		
		StressTest test = new StressTest(table,family,cols);
		// create & delete
//		test.deleteTable();
//		test.deleteTable("tost_indexed_table-pid", "tost_indexed_table-title",
//				"tost_indexed_table-name","tost_indexed_table-content");
		
//		test.createTable();
//		test.addIndexes();

		test.showDesc();
		// insert data
//		test.insert(1000000);
//		test.insertThread(10,30000);
		
		// searching
//		test.search();
	}
}

```