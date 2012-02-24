package ch.vd.moscow.database;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ch.vd.moscow.data.Call;
import ch.vd.moscow.data.Caller;
import ch.vd.moscow.data.CompletionStatus;
import ch.vd.moscow.data.Environment;
import ch.vd.moscow.data.LogFile;
import ch.vd.moscow.data.Method;
import ch.vd.moscow.data.Service;
import ch.vd.moscow.job.JobStatus;
import ch.vd.moscow.job.LoggingJobStatus;
import ch.vd.registre.base.date.RegDate;

/**
 * @author msi
 */
public class DatabaseServiceImpl implements DatabaseService {

	private static final Logger LOGGER = Logger.getLogger(DatabaseServiceImpl.class);
	
	private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		}
	};

	private DAO dao;
	
	private static class Line {

		private final Environment environment;
		private final String service;
		private final String user;
		private final String method;
		private final long milliseconds;
		private final Date timestamp;
		private final String params;

		public Line(Environment environment, String service, String user, String method, long milliseconds, Date timestamp, String params) {
			this.environment = environment;
			this.service = service;
			this.user = user;
			this.method = method;
			this.milliseconds = milliseconds;
			this.timestamp = timestamp;
			this.params = params;
		}

		public Environment getEnvironment() {
			return environment;
		}

		public String getService() {
			return service;
		}

		public String getUser() {
			return user;
		}

		public String getMethod() {
			return method;
		}

		public long getMilliseconds() {
			return milliseconds;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public String getParams() {
			return params;
		}

		// exemple de ligne de log : [tiers2.read] INFO  [2010-11-11 10:48:38.464] [web-it] (15 ms) GetTiersHisto{login=UserLogin{userId='zsimsn', oid=22}, tiersNumber=10010169, parts=[ADRESSES]} charge=1
		private static Line parse(Environment environment, String line) throws ParseException {
			if (StringUtils.isBlank(line)) {
				return null;
			}
	
			int next;
	
			// on récupère le nom du service
			final String service;
			{
				int left = line.indexOf('[');
				int right = line.indexOf(']');
				service = line.substring(left + 1, right);
				next = right;
			}
	
			// on récupère le timestamp
			final String timestampAsString;
			{
				int left = line.indexOf('[', next + 1);
				int right = line.indexOf(']', next + 1);
				timestampAsString = line.substring(left + 1, right);
				next = right;
			}
	
			// on récupère le user
			String user;
			{
				int left = line.indexOf('[', next + 1);
				int right = line.indexOf(']', next + 1);
				user = line.substring(left + 1, right);
				next = right;
			}
			if (user.equals("aci-com")) {
				user = "acicom";
			}
			if (user.equals("emp-aci")) {
				user = "empaci";
			}
	
			// on récupère les millisecondes
			final String milliAsString;
			{
				int left = line.indexOf('(', next + 1);
				int right = line.indexOf(')', next + 1);
				milliAsString = line.substring(left + 1, right - 3);
				next = right;
			}
	
			// on récupère le nom de la méthode
			final String method;
			{
				int left = line.indexOf(' ', next + 1);
				int right = line.indexOf('{', next + 1);
				next = right;
				method = line.substring(left + 1, right);
			}
	
			// on récupère les paramètres
			final String params;
			{
				int left = next;
				int right = line.indexOf(" load=", next + 1);
				params = line.substring(left, right);
			}
	
			final Date timestamp = parseTimestamp(timestampAsString);
			final long milliseconds = Long.parseLong(milliAsString);
	
			return new Line(environment, service, user, method, milliseconds, timestamp, params);
		}
	}

	public static Date parseTimestamp(String timestampAsString) throws ParseException {
		try {
			return TIMESTAMP_FORMAT.get().parse(timestampAsString);
		}
		catch (Exception e) {
			throw new RuntimeException("Error when parsing timestamp = [" + timestampAsString + "]", e);
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDao(DAO dao) {
		this.dao = dao;
	}

	@Override
	public void clearDb() {
		dao.clearCalls();
		dao.clearImportedFiles();
		dao.clearUpToStatus();
		dao.clearEnvironments();
	}

	@Override
	public void importLog(final Environment environment, final String filename, JobStatus status) {

		if (status == null) {
			status = new LoggingJobStatus();
		}

		if (dao.isFileAlreadyImported(environment, filename)) {
			// nothing to do
			return;
		}

		final boolean isTodayLog = importFile(environment, filename, status);
		if (!isTodayLog) {
			dao.addImportedFile(new LogFile(environment, filename));
		}

		status.setMessage("finishing...");
	}

	/**
	 * Import the specified log file and store all calls information into the database.
	 *
	 * @param environment the target environnement
	 * @param filename    the log file to import
	 * @param status      the job status
	 * @return <b>true</b> if the file is the today log (= still in construction); <b>false</b> otherwise.
	 */
	private boolean importFile(Environment environment, String filename, JobStatus status) {

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			Boolean isTodayLog = importStream(environment, reader, filename, status);
			return isTodayLog == null || isTodayLog;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			safeClose(reader);
		}
	}
	
	private static class ImportContext {

		private DAO dao;
		private Map<String, Caller> callers = new HashMap<String, Caller>();
		private Map<String, Service> services = new HashMap<String, Service>();
		private Map<String, Method> methods = new HashMap<String, Method>();

		private ImportContext(DAO dao, List<Caller> callers, List<Service> services, List<Method> methods) {
			this.dao = dao;
			for (Caller caller : callers) {
				this.callers.put(caller.getName(), caller);
			}
			for (Service service : services) {
				this.services.put(service.getName(), service);
			}
			for (Method method : methods) {
				this.methods.put(method.getName(), method);
			}
		}
		
		public Caller getOrCreateCaller(String name) {
			Caller caller = callers.get(name);
			if (caller == null) {
				caller = new Caller();
				caller.setName(name);
				caller = dao.saveCaller(caller);
				callers.put(caller.getName(), caller);
			}
			return caller;
		}

		public Service getOrCreateService(String name) {
			Service service = services.get(name);
			if (service == null) {
				service = new Service();
				service.setName(name);
				service = dao.saveService(service);
				services.put(service.getName(), service);
			}
			return service;
		}

		public Method getOrCreateMethod(String name) {
			Method method = methods.get(name);
			if (method == null) {
				method = new Method();
				method.setName(name);
				method = dao.saveMethod(method);
				methods.put(method.getName(), method);
			}
			return method;
		}
	}

	private Boolean importStream(Environment environment, BufferedReader reader, String filename, JobStatus status) throws IOException, ParseException {

		LOGGER.debug("Importing calls from file [" + filename + "]");

		final CompletionStatus completionStatus = dao.getCompletionStatus(environment);
		final Date upToStatus = (completionStatus == null ? null : completionStatus.getUpTo());

		Boolean isTodayLog = null;
		Date newUpTo = upToStatus;
		int lineCount = 0;
		int lineImported = 0;

		final ImportContext context = new ImportContext(dao, dao.getCallers(), dao.getServices(), dao.getMethods());

		String line = reader.readLine();
		while (line != null) {
			if (++lineCount % 17 == 0) {
				status.setMessage(String.format("importing line #%d", lineCount));
				// execute sql inserts and clear the session
				dao.flush();
				dao.clear();
			}
			final Date t;
			try {
				t = processLine(environment, line, upToStatus, context);
				if (t != null) {
					++lineImported;
				}
			}
			catch (ParseException e) {
				LOGGER.error("Parsing line #" + lineCount + " of file [" + filename + "]");
				throw e;
			}
			catch (RuntimeException e) {
				LOGGER.error("Processing line #" + lineCount + " of file [" + filename + "]");
				throw e;
			}
			if (t != null) {
				if (newUpTo == null || t.after(newUpTo)) {
					newUpTo = t;
				}
				if (isTodayLog == null) {
					isTodayLog = (RegDate.get() == RegDate.get(t));
				}
			}

			line = reader.readLine();
		}

		LOGGER.debug(lineCount + " lines read, " + lineImported + " calls imported");

		status.setMessage("saving completion status...");
		dao.setCompletionStatus(environment, newUpTo);
		return isTodayLog;
	}

	private Date processLine(Environment environment, String line, Date upToStatus, ImportContext context) throws ParseException {

		final Line l = Line.parse(environment, line);
		if (l == null) {
			return null;
		}

		final Date timestamp = l.getTimestamp();
		if (upToStatus != null && (timestamp.before(upToStatus) || timestamp.equals(upToStatus))) {
			// call has already been imported, we ignore it
			return null;
		}
		
		final Call call = resolveLine(l, context);
		dao.addCall(call);
		return timestamp;
	}

	private static Call resolveLine(Line line, ImportContext context) {
		Call call = new Call();
		call.setEnvironment(line.getEnvironment());
		call.setLatency(line.getMilliseconds());
		call.setTimestamp(line.getTimestamp());
		call.setParams(line.getParams());
		call.setCaller(context.getOrCreateCaller(line.getUser()));
		call.setService(context.getOrCreateService(line.getService()));
		call.setMethod(context.getOrCreateMethod(line.getMethod()));
		return call;
	}

	private static void safeClose(BufferedReader reader) {
		if (reader != null) {
			try {
				reader.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
