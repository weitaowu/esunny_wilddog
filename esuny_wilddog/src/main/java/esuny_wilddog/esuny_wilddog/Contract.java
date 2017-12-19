package esuny_wilddog.esuny_wilddog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jtap.TapAPIQuoteWhole;

public class Contract {
	static Logger logger = LoggerFactory.getLogger(Contract.class.getName());
	
	// 合约名称 NYMEX.CL.1801
	String contractUID;

	// 全部品种交易时间信息
	static final HashMap<String, ArrayList<LocalTime>> commodityTradeTime = new HashMap<String, ArrayList<LocalTime>>();

	static void InitTradeTime() {
		ArrayList<LocalTime> array = new ArrayList<LocalTime>();
		Boolean isDST = false;
		// 美原油 天然气 黄金 白银
		// 夏令电子盘 06:00-05:00
		// 冬令电子盘 07:00-06:00
		if (isDST) {
			array.add(LocalTime.of(06, 00, 00));
			array.add(LocalTime.MAX);
			array.add(LocalTime.MIDNIGHT);
			array.add(LocalTime.of(05, 00, 00));
		} else {
			array.add(LocalTime.of(07, 00, 00));
			array.add(LocalTime.MAX);
			array.add(LocalTime.MIDNIGHT);
			array.add(LocalTime.of(06, 00, 00));
		}
		commodityTradeTime.put("NYMEX.CL", array);
		commodityTradeTime.put("NYMEX.NG", array);
		commodityTradeTime.put("NYMEX.GC", array);
		commodityTradeTime.put("NYMEX.SI", array);

		// 德指
		// 夏令电子盘 13:50-04:00*
		// 冬令电子盘 14:50-05:00
		array.clear();
		if (isDST) {
			array.add(LocalTime.of(13, 50, 00));
			array.add(LocalTime.MAX);
			array.add(LocalTime.MIDNIGHT);
			array.add(LocalTime.of(04, 00, 00));
		} else {
			array.add(LocalTime.of(14, 50, 00));
			array.add(LocalTime.MAX);
			array.add(LocalTime.MIDNIGHT);
			array.add(LocalTime.of(05, 00, 00));
		}
		commodityTradeTime.put("EUREX.DAX", array);

		// 大恒指 小恒指
		// 09:15-12:00; 13:00-16:30
		// (T+1)17:15-01:00
		array.clear();
		array.add(LocalTime.of(17, 15, 00));
		array.add(LocalTime.MAX);
		array.add(LocalTime.MIDNIGHT);
		array.add(LocalTime.of(01, 00, 00));
		array.add(LocalTime.of(9, 15, 00));
		array.add(LocalTime.of(16, 30, 00));
		commodityTradeTime.put("HKEX.HSI", array);
		commodityTradeTime.put("HKEX.MHI", array);

		// A50
		// 09:00-16:35;
		// (T+1)17:00-04:45
		array.clear();
		array.add(LocalTime.of(17, 00, 00));
		array.add(LocalTime.MAX);
		array.add(LocalTime.MIDNIGHT);
		array.add(LocalTime.of(04, 45, 00));
		array.add(LocalTime.of(9, 00, 00));
		array.add(LocalTime.of(16, 35, 00));
		commodityTradeTime.put("SGX.CN", array);
	}

	// 从文件恢复当天K线
	void LoadKLineFile(File dataDir, String filename) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(dataDir, filename)), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null) {
				logger.debug(line);
				String s[] = line.split(" ");
				KLine k = new KLine();
//				k.index = Long.parseLong(s[0]);
//				k.OpenPx = Double.parseDouble(s[1]);
//				k.HighPx = Double.parseDouble(s[2]);
//				k.LowPx = Double.parseDouble(s[3]);
//				k.LastPx = Double.parseDouble(s[4]);
//				k.Volume = Long.parseLong(s[5]);
//				k.TotalQty = Long.parseLong(s[6]);
			}
			reader.close();
		} catch (UnsupportedEncodingException e) {
			logger.error(dataDir + filename + ":" + e.getMessage());
		} catch (FileNotFoundException e) {
			logger.error(dataDir + filename + ":" + e.getMessage());
		}
	}
	
	static final int MAX_CONTRACT_NUM = 50;
	
	// 保存上一笔行情,计算逐笔成交
	TapAPIQuoteWhole last_quote = new TapAPIQuoteWhole();
	public KLine last_kline = new KLine();
	
	// 全天分钟K线 分时线 
	private LinkedHashMap<Long, KLine> minklines = new LinkedHashMap<Long, KLine>(KLine.KLINECAPACITY);


	Contract(String contractUID){
		this.contractUID = contractUID;
	}
	
	/**
	 * @param strDateTimestamp
	 */
	public static long getMinKLineIndex(String contractUID, String strDateTimestamp) {
		return getMinKLineIndex(contractUID, strDateTimestamp, 0);
	}

	/**
	 * 取分钟K下标,偏移n分钟,负数向前,零当前分钟,正数向后
	 */
	public static long getMinKLineIndex(String contractUID, String strDateTimestamp, Integer n) {
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		long l = 0;
		Date d = new Date();
		Date dd = new Date();
		try {
			d = df.parse(strDateTimestamp);
			// TODO 需完善交易时间段判断
			dd = new Date(d.getTime() + n * 60 * 1000);	
		} catch (ParseException e) {
			e.printStackTrace();
		}
		l = DateUtils.toLong(dd) / 100 * 100;
		return l;
	}
	
	/**
	 * 根据时间戳判断品种所属交易日 
	 * 
	 */
	public static Long getTradeDay(String commodityUID, String strDateTimestamp) {
		// TODO
		LocalDate ld = LocalDate.now();

		return new Long(ld.getYear() * 10000 + ld.getMonthValue() * 100 + ld.getDayOfMonth());
	}
	
	/**
	 * 根据时间戳判断品种交易日第一根分钟K线下标
	 * 
	 */
	public static Long getFirstMinKLineIndex(String commodityUID,  String strDateTimestamp) {
		//TODO T
		Long nTradeDay = getTradeDay(commodityUID, strDateTimestamp);
		LocalTime lt = LocalTime.now();
		return new Long(0);
	}
	
	public void UpdateQuote(TapAPIQuoteWhole quote) {
		// 品种ID
		String commodityUID = quote.Contract.Commodity.ExchangeNo 
				+ "." + quote.Contract.Commodity.CommodityNo;
		
		// 第一根K线下标
		long firstminklineIndex = getFirstMinKLineIndex(commodityUID, quote.DateTimeStamp);
		// 当前分钟K下标
		long minklineindex = getMinKLineIndex(commodityUID, quote.DateTimeStamp, 0);
		
		KLine minkline = minklines.get(new Long(minklineindex));
		if (minkline == null) {
			minkline = new KLine(new Long(minklineindex));
		}

		// 分钟K线首次更新 openpx需特别处理
		if (minklineindex == firstminklineIndex) {
			if (minkline.getOpenPx() == 0) {
				minkline.setOpenPx( quote.QOpeningPrice > 0 ? quote.QOpeningPrice : quote.QPreClosingPrice);			
			}		
		} else if (minklineindex > firstminklineIndex) {
			if (minkline.getOpenPx() == 0) {
				long preminklineindex = getMinKLineIndex(commodityUID, quote.DateTimeStamp, -1);
				KLine preminkline = minklines.get(preminklineindex);
				// TODO 处理行情丢失问题
				if (preminkline == null || preminkline.getLastPx() == 0) {
					minkline.setOpenPx(quote.QOpeningPrice);
					
					last_quote = quote; // trick 避免巨量成交
					
					// 补全缺失K线
					// firstminklineIndex --> preminklineindex
					// minklines.put(new Long(minklineindex), minkline);
				} else
					minkline.setOpenPx(preminkline.getLastPx() > 0 ? preminkline.getLastPx() : quote.QOpeningPrice);
			}
		} else if (minklineindex < firstminklineIndex) {
			logger.error(
					"ERROR: minklineindex[" + minklineindex + "] < firstminklineIndex[" + firstminklineIndex + "]");
			return;
		}

		if (minkline.getHighPx() < quote.QLastPrice)
			minkline.setHighPx(quote.QLastPrice);

		if (minkline.getLowPx() == 0 || minkline.getLowPx() > quote.QLastPrice)
			minkline.setLowPx (quote.QLastPrice);

		minkline.setLastPx(quote.QLastPrice);

		minkline.setVolume(minkline.getVolume() + quote.QTotalQty - last_quote.QTotalQty);
		
		minkline.setTotalQty(quote.QTotalQty);
		
		minklines.put(new Long(minklineindex), minkline);

		StringBuilder line = new StringBuilder(512);
		line.append(this.toString());
		
		if(quote.QTotalQty - last_quote.QTotalQty > 0) {
			line.append(" deal:").append(quote.QTotalQty - last_quote.QTotalQty);
		}
		
		logger.debug(line.toString());

		last_quote = quote;
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LocalDate today = LocalDate.now();
		logger.debug("Today's Local date : " + today);

		// 判断是否夏令时
		logger.debug("inDaylightTime:" + TimeZone.getDefault().inDaylightTime(new Date()));

		// JVM -Duser.timezone=GMT+08
		TimeZone.setDefault(TimeZone.getTimeZone("GMT-05"));
		TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
		boolean inDs = tz.inDaylightTime(new Date());
		logger.debug("inDaylightTime:" + inDs);
	}

}
