package ch.vd.moscow.database;

import ch.vd.moscow.data.Call;
import ch.vd.moscow.data.CompletionStatus;
import ch.vd.moscow.data.Environment;
import ch.vd.moscow.data.LogFile;
import ch.vd.moscow.job.JobStatus;
import ch.vd.moscow.job.LoggingJobStatus;
import ch.vd.registre.base.date.RegDate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * @author msi
 */
public class DatabaseServiceImpl implements DatabaseService {

	private static final Logger LOGGER = Logger.getLogger(DatabaseServiceImpl.class);

	private DAO dao;

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

	private Boolean importStream(Environment environment, BufferedReader reader, String filename, JobStatus status) throws IOException, ParseException {

		LOGGER.debug("Importing calls from file [" + filename + "]");

		int lineCount = 0;
		final CompletionStatus completionStatus = dao.getCompletionStatus(environment);
		final Date upToStatus = (completionStatus == null ? null : completionStatus.getUpTo());
		Boolean isTodayLog = null;
		Date newUpTo = upToStatus;

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
				t = processLine(environment, line, upToStatus);
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

		LOGGER.debug(lineCount + " calls imported");

		status.setMessage("saving completion status...");
		dao.setCompletionStatus(environment, newUpTo);
		return isTodayLog;
	}

	private Date processLine(Environment environment, String line, Date upToStatus) throws ParseException {

		final Call call = Call.parse(environment, line);
		if (call == null) {
			return null;
		}

		final Date timestamp = call.getTimestamp();
		if (upToStatus != null && (timestamp.before(upToStatus) || timestamp.equals(upToStatus))) {
			// call has already been imported, we ignore it
			return null;
		}

		dao.addCall(call);
		return timestamp;
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
