package com.rmd.lygp.transdata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

/**
 * Hello world!
 *
 */
public class App {
	private static Logger log = Logger.getLogger(App.class);

	public static void main(String[] args) {
		try {
			String jdbcUrlF = "jdbc:mysql://192.168.0.18:3306/rmd?characterEncoding=UTF-8";
			String jdbcUsernameF = "root";
			String jdbcPasswordF = "admin";
			Connection ConnF = null;
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			ConnF = DriverManager.getConnection(jdbcUrlF, jdbcUsernameF, jdbcPasswordF);
			String sqlF = "select count(*) as total from t_order_goodslist";
			Statement stmt = ConnF.createStatement();
			ResultSet rs = stmt.executeQuery(sqlF);
			while (rs.next()) {
				log.info("t_order_goodslist 总数:" + rs.getLong(1));
			}
			rs.close();
			stmt.close();
			int indexF = 0;
			String sqlF1 = "select * from t_order_goodslist order by id asc";
			Statement stmtF1 = ConnF.createStatement();
			ResultSet rsF1 = stmtF1.executeQuery(sqlF1);
			Statement stmtF2 = ConnF.createStatement();
			ResultSet rsF2 = null;
			String content = "use rmd;\r\n";
			content += "set character set utf8;\r\n";
			while (rsF1.next()) {
				indexF++;
				log.info("取第" + indexF + "条t_order_goodslist.id=" + rsF1.getLong("id"));
				String sqlF2 = "select tgs.baseid, tgs.status, tgs.id,tgs.code, tgs.weightnum,tgs.weightval,tgb.`name` as goodsname, tbd.brandname,tgs.packnum,CONCAT(tut1.unitname,\"/\",tut2.unitname) AS packname,tsu.unitname, tgb.subname, thumbnailurl, saleunitid,packnumunitid,tgb.brandId, spec, prefprice,marketprice,  CASE WHEN (UNIX_TIMESTAMP(NOW()) > UNIX_TIMESTAMP(promotionstartdate)) AND (UNIX_TIMESTAMP(NOW()) < UNIX_TIMESTAMP(promotionenddate)) THEN promotionprice ELSE NULL END AS promotionprice,promotionstartdate, promotionenddate  from t_goods_saleinfo tgs LEFT JOIN t_sys_unit tsu ON tgs.saleunitid = tsu.id LEFT JOIN t_sys_unit_pack tsyspk ON tgs.packnumunitid = tsyspk.id LEFT JOIN t_sys_unit tut1 ON tsyspk.unitid1 = tut1.id LEFT JOIN t_sys_unit tut2 ON tsyspk.unitid2 = tut2.id LEFT JOIN t_goods_base tgb ON tgs.baseid = tgb.id LEFT JOIN t_goods_brand tbd ON tbd.id = tgb.brandid where tgs.id=" + rsF1.getLong("goodsid");
				rsF2 = stmtF2.executeQuery(sqlF2);
				while (rsF2.next()) {
					// tagIds+=","+rsF2.getLong("id");
					String sqlT = String.format(
							"update t_order_goodslist set goodsname='%s',brandname='%s',brandId=%d,spec='%s',packinfo='%s',packnumunitid=%d,unitprice=%f,saleunitid=%d,saleunit='%s',packnum=%d,goodsimg='%s',goodsbaseid=%d,goods_code='%s' where id=%d;",
							rsF2.getString("goodsname"), rsF2.getString("brandname"), rsF2.getInt("brandId"),
							rsF2.getString("spec"), rsF2.getString("packname"), rsF2.getInt("packnumunitid"),
							rsF2.getBigDecimal("promotionprice")==null?rsF2.getBigDecimal("prefprice"):rsF2.getBigDecimal("promotionprice"), rsF2.getInt("saleunitid"),
									rsF2.getString("unitname"), rsF2.getInt("packnum"),
									rsF2.getString("thumbnailurl"), rsF2.getInt("baseid"),rsF2.getString("code"),rsF1.getLong("id"));
					content += sqlT+"\r\n";
				}
				
			}
			sqlF1="select * from t_order_base";
			stmtF1 = ConnF.createStatement();
			rsF1 = stmtF1.executeQuery(sqlF1);
			Integer need=null;
			String ordernum=null;
			Integer memId=null;
			Long count=0l;
			Integer receiver=null;
			while (rsF1.next()) {
				need=rsF1.getInt("needinvoice");
				ordernum=rsF1.getString("ordernumber");
				memId=rsF1.getInt("userid");
				receiver=rsF1.getInt("receiver");
				//发票
				if(need!=null&&need.intValue()==1201){
					String sqlF2="select count(*) as count from t_order_receiveaddress where (addressstatus=2 or addressstatus=3) and ordernum='"+ordernum+"'";
					rsF2 = stmtF2.executeQuery(sqlF2);
					while (rsF2.next()) {
						count=rsF2.getLong("count");
					}
					//添加发票地址
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					if(count.longValue()==0){
						String sqlF3="select * from t_member_receiveaddress where (addressstatus=2 or addressstatus=3) and memeberid="+memId+" limit 1";
						rsF2 = stmtF2.executeQuery(sqlF3);
						while (rsF2.next()) {
							String sqlT = String.format("insert into t_order_receiveaddress(ordernum,receivername,receivemobile,receivetel,provid,cityid,county,street,postcode,createtime,memeberid,addressemail,addressstatus) "
																					+ "values('%s','%s','%s','%s','%d','%d','%s','%s','%s','%s','%d','%s','%d');",
																					ordernum,rsF2.getString("receivername"),rsF2.getString("receivemobile"),rsF2.getString("receivetel"),rsF2.getInt("provid"),rsF2.getInt("cityid"),rsF2.getString("county"),rsF2.getString("street"),rsF2.getString("postcode"),rsF2.getTimestamp("createtime")==null?null:sdf.format(rsF2.getTimestamp("createtime")),memId,rsF2.getString("addressemail"),2);
							content += sqlT+"\r\n";
						}
					}
				}
				//收货
				String sqlF2="select count(*) as count from t_order_receiveaddress where addressstatus=1 and ordernum='"+ordernum+"'";
				rsF2 = stmtF2.executeQuery(sqlF2);
				while (rsF2.next()) {
					count=rsF2.getLong("count");
				}
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				if(count.longValue()==0){
					String sqlF3="select * from t_member_receiveaddress where id="+receiver;
					rsF2 = stmtF2.executeQuery(sqlF3);
					while (rsF2.next()) {
						String sqlT = String.format("insert into t_order_receiveaddress(ordernum,receivername,receivemobile,receivetel,provid,cityid,county,street,postcode,createtime,memeberid,addressemail,addressstatus) "
																				+ "values('%s','%s','%s','%s','%d','%d','%s','%s','%s','%s','%d','%s','%d');",
																				ordernum,rsF2.getString("receivername"),rsF2.getString("receivemobile"),rsF2.getString("receivetel"),rsF2.getInt("provid"),rsF2.getInt("cityid"),rsF2.getString("county"),rsF2.getString("street"),rsF2.getString("postcode"),rsF2.getTimestamp("createtime")==null?null:sdf.format(rsF2.getTimestamp("createtime")),memId,rsF2.getString("addressemail"),1);
						content += sqlT+"\r\n";
					}
				}
			}
			writeContent(content, "d:/", "transorder.sql", false);
			log.info("迁移成功");
			if (rsF2 != null) {
				rsF2.close();
				stmtF2.close();
			}

			rsF1.close();
			stmtF1.close();
			ConnF.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 写入内容到文件
	 * 
	 * @param number
	 * @param filename
	 * @return
	 */
	public static boolean writeContent(String c, String dirname, String filename, boolean isAppend) {
		File f = new File(dirname);
		if (!f.exists()) {
			f.mkdirs();
		}
		try {
			FileOutputStream fos = new FileOutputStream(dirname + File.separator + filename, isAppend);
			OutputStreamWriter writer = new OutputStreamWriter(fos);
			writer.write(c);
			writer.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
