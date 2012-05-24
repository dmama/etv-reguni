package ch.vd.moscow.job.processor;

import ch.vd.moscow.data.ImportLogsJob;
import ch.vd.moscow.database.DatabaseService;
import ch.vd.moscow.job.JobStatus;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ImportLogsProcessor implements JobProcessor<ImportLogsJob> {

	private static final Logger LOGGER = Logger.getLogger(ImportLogsProcessor.class);

	private DatabaseService databaseService;
	private PlatformTransactionManager transactionManager;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDatabaseService(DatabaseService databaseService) {
		this.databaseService = databaseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void execute(final ImportLogsJob job, final JobStatus status) {

		LOGGER.info("Starting logs import from directory " + job.getDirectory().getDirectoryPath());

		final File dir = new File(job.getDirectory().getDirectoryPath());
		if (!dir.exists()) {
			LOGGER.error("File directory [" + job.getDirectory().getDirectoryPath() + "] doesn't exist.");
			return;
		}

		// multiple filters can be specified separated by spaces
		final String filter = job.getDirectory().getPattern();
		final String[] filters = filter.split(" ");

		// convert command-line expression (*.log) into regular expression (.*\.log)
		final List<Pattern> patterns = new ArrayList<Pattern>(filters.length);
		for (String s : filters) {
			final String regexp = s.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replaceAll("\\?", ".");
			final Pattern p = Pattern.compile(regexp);
			patterns.add(p);
		}

		final String[] logFiles = dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (Pattern pattern : patterns) {
					if (pattern.matcher(name).matches()) {
						return true;
					}
				}
				return false;
			}
		});

		if (logFiles.length == 0) {
			LOGGER.warn("There is no file matching pattern [" + filter + "] in directory [" + job.getDirectory().getDirectoryPath() + "]");
			return;
		}

		// sort them older first (expected filename format is 'blablaba.log.YYYY-MM-DD' or 'blablaba.log' for today log)
		final List<String> sortedLogFiles = Arrays.asList(logFiles);
		Collections.sort(sortedLogFiles, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				RegDate d1 = parseLogFilenameDate(o1);
				RegDate d2 = parseLogFilenameDate(o2);
				return d1.compareTo(d2);
			}
		});

		final TransactionTemplate template = new TransactionTemplate(transactionManager);

		for (final String logFile : logFiles) {
			if (status.isInterrupted()) {
				break;
			}

			template.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus s) {
					databaseService.importLog(job.getDirectory().getEnvironment(), job.getDirectory().getDirectoryPath() + "/" + logFile, status);
				}
			});
		}

		LOGGER.info("Logs import from directory " + job.getDirectory().getDirectoryPath() + " done.");
	}

	private static RegDate parseLogFilenameDate(String logFilename) {
		if (logFilename.endsWith(".log")) { // today log
			return RegDate.get();
		}
		int pos = logFilename.indexOf(".log");
		final String dateAsString = logFilename.substring(pos + 5);
		final RegDate date = RegDateHelper.dashStringToDate(dateAsString);
		if (date == null) {
			throw new IllegalArgumentException("Unable to determine date from log filename = [" + logFilename + "]");
		}
		return date;
	}
}
