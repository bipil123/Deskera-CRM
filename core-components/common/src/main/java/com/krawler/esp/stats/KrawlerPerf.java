/*
 * Copyright (C) 2012  Krawler Information Systems Pvt Ltd
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package com.krawler.esp.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import com.krawler.common.stats.StatUtil;
import com.krawler.common.util.Constants;
import com.krawler.common.util.KrawlerLog;
import com.krawler.common.util.Log;
import com.krawler.common.util.LogFactory;
import com.krawler.common.util.StringUtil;
//import com.krawler.database.DbPool;
//import com.krawler.database.DbUtil;
import com.krawler.esp.service.util.ThreadLocalData;
import com.krawler.esp.utils.ConfigReader;
import com.krawler.esp.utils.KrawlerApp;

/**
 * A collection of methods for keeping track of server performance statistics.
 */
public class KrawlerPerf {

	static Log sLog = LogFactory.getLog(KrawlerPerf.class);
	private static final com.krawler.common.util.Log sKrawlerStats = LogFactory
			.getLog("krawler.stats");

	public static final String RTS_DB_POOL_SIZE = "db_pool_size";
	public static final String RTS_INNODB_BP_HIT_RATE = "innodb_bp_hit_rate";

	public static final String RTS_POP_CONN = "pop_conn";
	public static final String RTS_POP_SSL_CONN = "pop_ssl_conn";
	public static final String RTS_IMAP_CONN = "imap_conn";
	public static final String RTS_IMAP_SSL_CONN = "imap_ssl_conn";

	// Accumulators. To add a new accumulator, create a static instance here,
	// add it to the CORE_ACCUMULATORS array and if necessary, set options
	// in the static init code below.
	public static Counter COUNTER_LMTP_RCVD_MSGS = new Counter("lmtp_rcvd_msgs");
	public static Counter COUNTER_LMTP_RCVD_BYTES = new Counter(
			"lmtp_rcvd_bytes");
	public static Counter COUNTER_LMTP_RCVD_RCPT = new Counter("lmtp_rcvd_rcpt");
	public static Counter COUNTER_LMTP_DLVD_MSGS = new Counter("lmtp_dlvd_msgs");
	public static Counter COUNTER_LMTP_DLVD_BYTES = new Counter(
			"lmtp_dlvd_bytes");
	public static StopWatch STOPWATCH_DB_CONN = new StopWatch("db_conn");
	public static StopWatch STOPWATCH_LDAP_DC = new StopWatch("ldap_dc");
	public static StopWatch STOPWATCH_MBOX_ADD_MSG = new StopWatch(
			"mbox_add_msg");
	public static StopWatch STOPWATCH_MBOX_GET = new StopWatch("mbox_get"); // Mailbox
	// accessor
	// response
	// time
	public static Counter COUNTER_MBOX_CACHE = new Counter("mbox_cache"); // Mailbox
	// cache
	// hit
	// rate
	public static Counter COUNTER_MBOX_MSG_CACHE = new Counter("mbox_msg_cache");
	public static Counter COUNTER_MBOX_ITEM_CACHE = new Counter(
			"mbox_item_cache");
	public static StopWatch STOPWATCH_SOAP = new StopWatch("soap");
	public static StopWatch STOPWATCH_IMAP = new StopWatch("imap");
	public static StopWatch STOPWATCH_POP = new StopWatch("pop");
	public static Counter COUNTER_IDX_WRT = new Counter("idx_wrt");
	public static Counter COUNTER_IDX_WRT_OPENED = new Counter("idx_wrt_opened");
	public static Counter COUNTER_IDX_WRT_OPENED_CACHE_HIT = new Counter(
			"idx_wrt_opened_cache_hit");

	private static RealtimeStats sRealtimeStats = new RealtimeStats(
			new String[] { RTS_DB_POOL_SIZE, RTS_INNODB_BP_HIT_RATE,
					RTS_POP_CONN, RTS_POP_SSL_CONN, RTS_IMAP_CONN,
					RTS_IMAP_SSL_CONN });

	private static CopyOnWriteArrayList<Accumulator> sAccumulators = new CopyOnWriteArrayList<Accumulator>(
			new Accumulator[] { COUNTER_LMTP_RCVD_MSGS,
					COUNTER_LMTP_RCVD_BYTES, COUNTER_LMTP_RCVD_RCPT,
					COUNTER_LMTP_DLVD_MSGS, COUNTER_LMTP_DLVD_BYTES,
					STOPWATCH_DB_CONN, STOPWATCH_LDAP_DC,
					STOPWATCH_MBOX_ADD_MSG, STOPWATCH_MBOX_GET,
					COUNTER_MBOX_CACHE, COUNTER_MBOX_MSG_CACHE,
					COUNTER_MBOX_ITEM_CACHE, STOPWATCH_SOAP, STOPWATCH_IMAP,
					STOPWATCH_POP, COUNTER_IDX_WRT, COUNTER_IDX_WRT_OPENED,
					COUNTER_IDX_WRT_OPENED_CACHE_HIT, sRealtimeStats });

	/**
	 * This may only be called BEFORE KrawlerPerf.initialize is called,
	 * otherwise the column names will not be output correctly into the logs
	 */
	public static void addRealtimeStatName(String name) {
		if (sIsInitialized)
			throw new IllegalStateException(
					"Cannot add stat name after KrawlerPerf.initialize() is called");
		sRealtimeStats.addName(name);
	}

	/**
	 * This may only be called BEFORE KrawlerPerf.initialize is called,
	 * otherwise the column names will not be output correctly into the logs
	 */
	public static void addAccumulator(Accumulator toAdd) {
		if (sIsInitialized)
			throw new IllegalStateException(
					"Cannot add stat name after KrawlerPerf.initialize() is called");
		sAccumulators.add(toAdd);
	}

	private static Map<String, StatementStats> sSqlToStats = new HashMap<String, StatementStats>();

	/**
	 * The number of statements that were executed, as reported by
	 * <code>DebugPreparedStatement</code>. This number only gets updated
	 * when debug logging is turned on for the <code>Krawler.sqltrace</code>
	 * or <code>Krawler.perf</code> categories.
	 */
	private static int sStatementCount = 0;

	/**
	 * The number of statements that were prepared, as reported by
	 * {@link DbPool.Connection#prepareStatement}.
	 */
	private static int sPrepareCount = 0;

	private static final long STATS_WRITE_INTERVAL = Constants.MILLIS_PER_MINUTE;
	private static final long SLOW_QUERY_THRESHOLD = 300; // ms
	private static long sLastStatsWrite = System.currentTimeMillis();

	/**
	 * Returns <code>true</code> if we are collecting performance data.
	 * Performance data is collected when the <code>Krawler.perf</code>
	 * logging category is set to <code>DEBUG</code>.
	 */
	public static boolean isPerfEnabled() {
		return KrawlerLog.perf.isDebugEnabled();
	}

	private static class StatementStats {
		String mSql;
		int mCount = 0;
		int mMinTime = Integer.MAX_VALUE;
		int mMaxTime = 0;
		int mTotalTime = 0;

		StatementStats(String sql) {
			mSql = sql;
		}

		int getAvgTime() {
			if (mCount == 0) {
				return 0;
			}
			return mTotalTime / mCount;
		}
	}

	public static int getStatementCount() {
		return sStatementCount;
	}

	public static int getPrepareCount() {
		return sPrepareCount;
	}

	public static void incrementPrepareCount() {
		sPrepareCount++;
	}

	/**
	 * Increments the total SQL statement count. If <code>sqlperf</code> debug
	 * logging is on,
	 * 
	 * <ul>
	 * <li>Updates aggregate timing data for the given statement</li>
	 * <li>Writes timing data to <code>sqlperf.csv</code> once a minute</li>
	 * <li>Writes slow queries to <code>slow_queries.csv</code></li>
	 * </ul>
	 * 
	 * @param normalized
	 *            the SQL statement
	 * @param durationMillis
	 *            execution time
	 * @see #getStatementCount()
	 */
//	public static void updateDbStats(String sql, int durationMillis) {
//		sStatementCount++;
//		if (!KrawlerLog.perf.isDebugEnabled()) {
//			return;
//		}
//
//        String normalized = "";        
////		String normalized = DbUtil.normalizeSql(sql);
//		StatementStats stats = null;
//
//		synchronized (sSqlToStats) {
//			stats = (StatementStats) sSqlToStats.get(normalized);
//			if (stats == null) {
//				stats = new StatementStats(normalized);
//				sSqlToStats.put(normalized, stats);
//			}
//
//			stats.mCount++;
//			stats.mTotalTime += durationMillis;
//			if (durationMillis < stats.mMinTime) {
//				stats.mMinTime = durationMillis;
//			}
//			if (durationMillis > stats.mMaxTime) {
//				stats.mMaxTime = durationMillis;
//			}
//		}
//
//		ThreadLocalData.incrementDbTime(durationMillis);
//
//		if (durationMillis > SLOW_QUERY_THRESHOLD) {
//			writeSlowQuery(sql, normalized, durationMillis);
//		}
//		long now = System.currentTimeMillis();
//		if (now - sLastStatsWrite > STATS_WRITE_INTERVAL) {
//			writeDbStats();
//			sLastStatsWrite = now;
//		}
//	}

	/**
	 * Sorts stats in reverse order by count.
	 */
	private static class StatsComparator implements Comparator<StatementStats> {
		public int compare(StatementStats stats1, StatementStats stats2) {
			if (stats1.mCount < stats2.mCount) {
				return 1;
			} else if (stats1.mCount > stats2.mCount) {
				return -1;
			}
			return 0;
		}
	}

	private static final StatsComparator STATS_COMPARATOR = new StatsComparator();

	/**
	 * Writes all the SQL statements and their latest timing data to
	 * <code>sqlperf.csv</code>, in descending order by count.
	 */
	private static void writeDbStats() {
		String filename = ConfigReader.getinstance()
				.get("log_directory", "log")
				+ "/sqlperf.csv";
		FileWriter writer = null;
		try {
			synchronized (sSqlToStats) {
				// Calculate total of totals
				int totalTotal = 0;
				Iterator i = sSqlToStats.values().iterator();
				while (i.hasNext()) {
					StatementStats stats = (StatementStats) i.next();
					totalTotal += stats.mTotalTime;
				}

				writer = new FileWriter(filename);
				writer
						.write("sql,count,min_time,avg_time,max_time,total_time,percent_total_time\n");
				List<StatementStats> statsList = new ArrayList<StatementStats>(
						sSqlToStats.values());
				Collections.sort(statsList, STATS_COMPARATOR);
				i = statsList.iterator();
				while (i.hasNext()) {
					StatementStats stats = (StatementStats) i.next();
					int percent = 0;
					if (totalTotal != 0) {
						percent = stats.mTotalTime * 100 / totalTotal;
					}
					writer.write("\"" + stats.mSql + "\"," + stats.mCount + ","
							+ stats.mMinTime + "," + stats.getAvgTime() + ","
							+ stats.mMaxTime + "," + stats.mTotalTime + ","
							+ percent + "%,\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			KrawlerLog.perf.warn("Unable to write to " + filename + ": "
					+ e.toString());
		}
	}

	private static void writeSlowQuery(String sql, String normalized,
			int durationMillis) {
		String filename = ConfigReader.getinstance()
				.get("log_directory", "log")
				+ "/slow_queries.csv";
		FileWriter writer = null;
		try {
			File file = new File(filename);
			if (!file.exists()) {
				// There's a remote possibility that two processes will do this
				// at the same time.
				// Worst case, we'll lose one line of data.
				writer = new FileWriter(file);
				writer.write("timestamp,exec_time,sql,normalized\n");
			} else {
				writer = new FileWriter(file, true);
			}
			if (sql.indexOf('"') >= 0) {
				// Escape quotes for CSV.
				// replace() does regexp operations, so only use it when
				// necessary.
				sql.replace("\"", "\"\"");
			}
			writer.write(StatUtil.getTimestampString() + "," + durationMillis
					+ ",\"" + sql + "\",\"" + normalized + "\"\n");
			writer.close();
		} catch (IOException e) {
			KrawlerLog.perf.warn("Unable to write to " + filename + ": "
					+ e.toString());
		}
	}

	private static Map<String, FileWriter> sWriterMap = new HashMap<String, FileWriter>();

	private static FileWriter getWriter(StatsFile statsFile) throws IOException {
		FileWriter writer = null;

		synchronized (sWriterMap) {
			writer = (FileWriter) sWriterMap.get(statsFile.getFilename());
			if (writer != null) {
				return writer;
			}

			File file = statsFile.getFile();
			StringBuffer buf = new StringBuffer();

			// If the file doesn't exist, write the header
			if (!file.exists()) {
				writer = new FileWriter(file);
				buf.append("timestamp");
				String[] statNames = statsFile.getStatNames();
				if (statNames.length > 0) {
					buf.append(',');
					buf.append(StringUtil.join(",", statNames));
				}
				if (statsFile.shouldLogThreadLocal()) {
					buf.append(",exec_time,db_time,sql_count");
				}
				buf.append('\n');
				writer.write(buf.toString());
			} else {
				writer = new FileWriter(file, true);
			}

			sWriterMap.put(statsFile.getFilename(), writer);
		}

		return writer;
	}

	private static void removeWriter(StatsFile statsFile) {
		synchronized (sWriterMap) {
			sWriterMap.remove(statsFile.getFilename());
		}
	}

	/**
	 * Logs statistical information to the specified <code>StatsFile</code>,
	 * which contains one stat definition.
	 * 
	 * @param statsFile
	 *            the <code>StatsFile</code> object, which contains the
	 *            filename and names of the statistics to be logged.
	 * @param stat
	 *            the statistic to be logged
	 */
	public static void writeStats(StatsFile statsFile, Object stat) {
		List<Object> stats = new ArrayList<Object>();
		stats.add(stat);
		writeStats(statsFile, stats);
	}

	/**
	 * Logs statistical information to the specified <code>StatsFile</code>,
	 * which contains two stat definitions. Stats are logged in the same order
	 * as defined in the <code>StatsFile</code>.
	 * 
	 * @param statsFile
	 *            the <code>StatsFile</code> object, which contains the
	 *            filename and any extra columns written for the given event
	 * @param stat1
	 *            the first statistic
	 * @param stat2
	 *            the second statistic
	 * @param eventName
	 *            the event name
	 */
	public static void writeStats(StatsFile statsFile, Object stat1,
			Object stat2) {
		List<Object> stats = new ArrayList<Object>();
		stats.add(stat1);
		stats.add(stat2);
		writeStats(statsFile, stats);
	}

	/**
	 * Logs statistical information to the specified <code>StatsFile</code>,
	 * which contains three stat definitions. Stats are logged in the same order
	 * as defined in the <code>StatsFile</code>.
	 * 
	 * @param statsFile
	 *            the <code>StatsFile</code> object, which contains the
	 *            filename and any extra columns written for the given event
	 * @param stat1
	 *            the first statistic
	 * @param stat2
	 *            the second statistic
	 * @param eventName
	 *            the event name
	 */
	public static void writeStats(StatsFile statsFile, Object stat1,
			Object stat2, Object stat3) {
		List<Object> stats = new ArrayList<Object>();
		stats.add(stat1);
		stats.add(stat2);
		stats.add(stat3);
		writeStats(statsFile, stats);
	}

	/**
	 * Logs statistical information to the specified <code>StatsFile</code>.
	 * Stats are logged in the same order as defined in the
	 * <code>StatsFile</code>.
	 * 
	 * @param statsFile
	 *            the <code>StatsFile</code> object, which contains the
	 *            filename and any extra columns written for the given event
	 * @param stats
	 *            statistical values
	 */
	public static void writeStats(StatsFile statsFile, List<Object> stats) {
		FileWriter writer = null;
		try {
			StringBuffer buf = new StringBuffer();
			writer = getWriter(statsFile);
			// Write timestamp and event name
			buf.append(StatUtil.getTimestampString());

			// Write stats
			for (Object value : stats) {
				if (value == null) {
					value = "";
				} else if (value instanceof String) {
					// Put quotes around strings
					String s = (String) value;
					if (s.indexOf('"') >= 0) {
						// Escape any internal quotes
						s = s.replaceAll("\"", "\"\"");
						value = "\"" + s + "\"";
					} else if (s.indexOf(',') >= 0) {
						value = "\"" + s + "\"";
					}
				}
				buf.append(',');
				buf.append(value);
			}

			// Write ThreadLocalData
			if (statsFile.shouldLogThreadLocal()) {
				buf.append(',').append(ThreadLocalData.getProcessingTime());
				buf.append(',').append(ThreadLocalData.getDbTime());
				buf.append(',').append(ThreadLocalData.getStatementCount());
			}
			buf.append('\n');
			writer.write(buf.toString());
			writer.flush();
		} catch (IOException e) {
			KrawlerLog.perf.warn("Unable to write to "
					+ statsFile.getFile().getPath() + ": " + e.toString());
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e2) {
					KrawlerLog.perf.warn(e2.toString());
				}
				removeWriter(statsFile);
			}
		}
	}

	/**
	 * Adds the given callback to the list of callbacks that are called during
	 * realtime stats collection.
	 */
	public static void addStatsCallback(RealtimeStatsCallback callback) {
		sRealtimeStats.addCallback(callback);
	}

	/**
	 * Returns the names of the columns for Krawlerstats.csv.
	 */
	public static List<String> getKrawlerStatsColumns() {
		List<String> columns = new ArrayList<String>();
		columns.add("timestamp");
		synchronized (sAccumulators) {
			for (Accumulator a : sAccumulators) {
				for (String column : a.getNames()) {
					columns.add(column);
				}
			}
		}
		return columns;
	}

	private static final long DUMP_FREQUENCY = Constants.MILLIS_PER_MINUTE;
	private static boolean sIsInitialized = false;

	public synchronized static void initialize() {
		if (sIsInitialized) {
			sLog.warn("Detected a second call to KrawlerPerf.initialize()",
					new Exception());
			return;
		}

		addStatsCallback(new SystemStats());

		// Only the average is interesting for these counters
		COUNTER_MBOX_CACHE.setShowAverage(true);
		COUNTER_MBOX_CACHE.setAverageName("mbox_cache");
		COUNTER_MBOX_CACHE.setShowCount(false);
		COUNTER_MBOX_CACHE.setShowTotal(false);

		COUNTER_MBOX_MSG_CACHE.setShowAverage(true);
		COUNTER_MBOX_MSG_CACHE.setAverageName("mbox_msg_cache");
		COUNTER_MBOX_MSG_CACHE.setShowCount(false);
		COUNTER_MBOX_MSG_CACHE.setShowTotal(false);

		COUNTER_MBOX_ITEM_CACHE.setShowAverage(true);
		COUNTER_MBOX_ITEM_CACHE.setAverageName("mbox_item_cache");
		COUNTER_MBOX_ITEM_CACHE.setShowCount(false);
		COUNTER_MBOX_ITEM_CACHE.setShowTotal(false);

		COUNTER_IDX_WRT.setShowAverage(true);
		COUNTER_IDX_WRT.setShowCount(false);
		COUNTER_IDX_WRT.setShowTotal(false);

		KrawlerApp.sTimer.scheduleAtFixedRate(new KrawlerStatsDumper(),
				DUMP_FREQUENCY, DUMP_FREQUENCY);
		sIsInitialized = true;
	}

	/**
	 * <code>TimerTask</code> implementation that writes a row to
	 * Krawlerstats.csv once a minute.
	 */
	private static final class KrawlerStatsDumper extends TimerTask {

		public boolean cancel() {
			KrawlerLog.perf.error("StatsDumper canceled");
			return super.cancel();
		}

		public void run() {
			try {
				List<Object> data = new ArrayList<Object>();
				data.add(StatUtil.getTimestampString());
				for (Accumulator a : sAccumulators) {
					synchronized (a) {
						data.addAll(a.getData());
						a.reset();
					}
				}

				// Clean up nulls
				for (int i = 0; i < data.size(); i++) {
					if (data.get(i) == null) {
						data.set(i, "");
					}
				}
				sKrawlerStats.info(StringUtil.join(",", data));
			} catch (Throwable t) {
				if (t instanceof OutOfMemoryError) {
					throw (OutOfMemoryError) t;
				}
				KrawlerLog.perf.error("Accumulator error", t);
			}
		}
	}
}
