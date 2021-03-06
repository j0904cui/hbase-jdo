package com.apache.hadoop.hbase.tool.view.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NumberUtils;
import org.apache.hadoop.hbase.util.Bytes;

import com.apache.hadoop.hbase.client.jdo.util.HUtil;
import com.apache.hadoop.hbase.tool.view.comp.table.AbstractHTableModel;
import com.apache.hadoop.hbase.tool.view.comp.table.HTableColumn;
import com.apache.hadoop.hbase.tool.view.processor.TableDataBean;
import com.apache.hadoop.hbase.tool.view.table.TableColumnInfoColumn;

public class TableDataModel extends AbstractHTableModel<TableDataBean>{
	public TableDataModel() {
		super(new ArrayList<TableDataBean>(), TableColumnInfoColumn.getColumns());
	}

	
	private List<String> makeColumnNames(){
		List<String> cols = new ArrayList<String>();
		cols.add("row_key");
		for(TableDataBean bean:values){
			for(String name:bean.getColumns().keySet()){
				if(cols.contains(name)==false) cols.add(name);
			}
		}	
		return cols;
	}
	@Override
	protected void resetColumns(List<TableDataBean> values) {
		this.columns.clear();
		
		int i=0; 
		for(String name:makeColumnNames()) {
			this.columns.add(new TableColumnInfoColumn(i,name,name,150));
			i++;
		}
	}
	
	@Override
	public long getValueLength(int row, int column) {
		byte[] value =getValue(row,column);
		return value==null? 0:value.length;
	}

	
	private byte[] getValue(int r,int c){
		TableDataBean info = this.values.get(r);
		
		HTableColumn columnInfo = columns.get(c);
		
		if(c==0){
			return info.getRow();
		}else{
			for(String name:info.getColumns().keySet()) {
				if(name.equals(columnInfo.getColumnName())) {
					byte[] value = info.getColumns().get(name);
					return value;
				}
			}
		}
		return null;
	}

	@Override
	public Object getValueAt(int r, int c) {
		return HUtil.toBytesString(getValue(r,c));
	}

	@Override
	public void loadTestData(int count) {
		for(int i=0;i<count;i++){
			TableDataBean info = new TableDataBean();
			info.setFamily("family");
			info.setTable("user");
			Map<String, byte[]> cols= new HashMap<String, byte[]>();
			for(int j=0;j<5;j++){
				cols.put("col"+i, ("value-"+j).getBytes());
			}
			info.setColumns(cols);
			values.add(info);
		}
		fireTableDataChanged();
	}
}
