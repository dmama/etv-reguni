package ch.vd.moscow.database;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import ch.vd.moscow.controller.graph.CallDimension;
import ch.vd.moscow.controller.graph.Filter;
import ch.vd.moscow.controller.graph.TimeResolution;
import ch.vd.moscow.data.Call;
import ch.vd.moscow.data.CompletionStatus;
import ch.vd.moscow.data.Environment;
import ch.vd.moscow.data.JobDefinition;
import ch.vd.moscow.data.LogDirectory;
import ch.vd.moscow.data.LogFile;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface DAO {

//	void migrateDb(JdbcTemplate template);

	void clearEnvironments();

	Long addEnvironment(Environment env);

	Environment saveEnvironment(Environment env);

	Environment getEnvironment(String name);

	Environment getEnvironment(Long id);

	List<Environment> getEnvironments();

	void delEnvironment(Environment env);

	/**
	 * Clear all calls data from the database.
	 */
	void clearCalls();

	Long addCall(final Call call);

	List<Call> getCalls(Environment environment);

	List<Call> getCalls(Environment environment, Date from, Date to);

	Iterator<Call> iterateCalls(Environment environment, Date from, Date to);

	Collection<CallStats> getLoadStatsFor(Filter[] filters, Date from, Date to, CallDimension[] criteria, TimeResolution resolution);

	List<Object> getDimensionValues(CallDimension dimension);

	void clearImportedFiles();

	Long addImportedFile(final LogFile file);

	boolean isFileAlreadyImported(Environment environment, String filepath);

	void clearUpToStatus();

	CompletionStatus getCompletionStatus(Environment environment);

	void setCompletionStatus(Environment environment, Date newUpTo);

	List<LogDirectory> getLogDirectories();

	LogDirectory addLogDirectory(LogDirectory logDirectory);

	LogDirectory getLogDirectory(Long id);

	void delLogDirectory(LogDirectory dir);

	void flush();

	void clear();

	List<JobDefinition> getJobs();

	JobDefinition addJob(JobDefinition job);

	JobDefinition getJob(Long id);

	void delJob(JobDefinition job);
}
