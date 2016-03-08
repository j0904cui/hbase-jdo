# upload big file an to hbase #

I used hbase directly.

```
package com.apache.hadoop.hbase.test;

import java.io.BufferedInputStream;

/**
 * Hadoop File upload test
 * (If you want to upload big file, don't use hbase. just use hadoop directly.)
 * @author ncanis@gmail.com
 *
 */
public class HadoopTest {
	private Configuration conf = null;
	public HadoopTest(){
		conf = new Configuration();
	}
	
	private FileSystem getDFS() throws IOException {
		return FileSystem.get(conf);
	}
	
	public void close(OutputStream os){
		if(os==null) return;
		try { os.close(); } catch (IOException e) {}
	}
	public void close(InputStream is){
		if(is==null) return;
		try { is.close(); } catch (IOException e) {}
	}
	
	public boolean uploadFile(Path directory,String filename, InputStream is, boolean overwrite){
		boolean isSuccess=false;
		FSDataOutputStream fos=null;		
		try {
			Path p  = new Path(directory, new Path(filename));
			FileSystem fs = getDFS();
			if(fs.getFileStatus(directory).isDir()==false) {
				throw new IOException(directory+" isn't directory.");				
			}else if(fs.exists(p)){
				if(overwrite) {
					delete(p,true);
				}else{
					throw new IOException(p+" already exist.");
				}
			}
			
			fos = fs.create(p);
			BufferedInputStream bis = new BufferedInputStream(is);
//			IOUtils.copyBytes(bis,fos,8192,true);
			copyBytes(bis,fos,8192,true);
						
			isSuccess= true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			close(fos);
		}
		return isSuccess;
	}
	
	/**
	 * copy bytes.
	 * you should use IOUtils.copyBytes(bis,fos,8192,true)
	 * @param is
	 * @param os
	 * @param bufferSize
	 * @param isLog
	 * @throws IOException
	 */
	private void copyBytes(InputStream is, OutputStream os, int bufferSize, boolean isLog) throws IOException{
		int length=0;
		long current = 0;		
		long total = is.available();
		long progress = 0;
		byte[] buf = new byte[bufferSize];
		while((length=is.read(buf))!=(-1)){
			os.write(buf,0, length);
			current+=length;
			long now = current*100/total;
			if(progress!=now) {			
				log("progress=",now,"%");
				progress = now;
			}
		}
	}
	
	public void delete(Path p, boolean recursive) throws IOException{
		FileSystem fs = getDFS();
		fs.delete(p, recursive);
	}
	
	public static final void log(Object ... os){
		StringBuilder sb = new StringBuilder();
		for(Object s:os){
			sb.append(s);
		}
		System.out.println(sb.toString());
	}
	
	/**
	 * after use, must close inputstream.
	 * @param p
	 * @param bufferSize
	 * @return
	 * @throws IOException
	 */
	public InputStream path2Stream(Path p, int bufferSize) throws IOException{
		FileSystem fs = getDFS();
		FSDataInputStream fis = fs.open(p, bufferSize);
		return fis;
	}
	
	public boolean copyFile2Local(Path path, File f){
		boolean isSuccess=false;
		try {
			InputStream is = path2Stream(path,8192);
			write2File(is,f);
			isSuccess= true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return isSuccess;
		
	}
	/**
	 * load file stream in hbase, write to file
	 * @param is
	 * @param f
	 * @return
	 */
	public boolean write2File(InputStream is, File f){
		boolean isSuccess=false;
		FileOutputStream fos=null;
		try {
			
			BufferedInputStream bis = new BufferedInputStream(is);
			fos = new FileOutputStream(f);
//			IOUtils.copyBytes(is,fos,8192,true);	
			copyBytes(bis,fos,8192,true);
			isSuccess = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			close(is);
			close(fos);
		}
		return isSuccess;
	}
	
	long start = 0L;
	private void start(){
		start = System.currentTimeMillis();
	}
	
	private long end(){
		return System.currentTimeMillis()-start;
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		HadoopTest ht = new HadoopTest();
		
		File file = new File("doc/movie.avi");
		FileInputStream fis = new FileInputStream(file);
		Path rootPath = new Path("/files/");
		String filename = "movie.avi";
		
		ht.start();
		boolean isUploaded = ht.uploadFile(rootPath,filename,fis,true);
		log("uploaded=",isUploaded," size=",file.length()," bytes. time=",ht.end()," ms");
		
		ht.start();
		ht.copyFile2Local(new Path(rootPath,filename), new File("test/"+filename));
		log("saved file time=",ht.end());
	}

}
```