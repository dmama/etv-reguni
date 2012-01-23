package ch.vd.moscow.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.moscow.controller.graph.BreakdownCriterion;
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
public class DAOImpl implements DAO {

	private static final Logger LOGGER = Logger.getLogger(DAOImpl.class);

	private HibernateTemplate hibernateTemplate;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	//	public void migrateDb(JdbcTemplate template) {
//		final Integer version = getDbVersion(template);
//		if (version == null) {
//			createDb(template);
//		}
//		else {
//			LOGGER.debug("DB schema is at version " + version);
//			switch (version) {
//			case 1:
//				LOGGER.info("DB schema is up-to-date.");
//				break;
//			default:
//				throw new RuntimeException("Unknown DB schema version = " + version);
//			}
//		}
//	}
//
//	private void createDb(JdbcTemplate template) {
//		createDb_v1(template);
//	}
//
//	private void createDb_v1(JdbcTemplate template) {
//		LOGGER.warn("Creating DB schema version 1...");
//
//		// the version table
//		template.execute("create table db_version(id serial primary key, version integer not null, script varchar(50), ts timestamp not null);");
//		template.execute("insert into db_version(version, script, ts) values (1, 'creation', now());");
//
//		// table to store calls data
//		template.execute("create table calls(id serial primary key, environment varchar(20) not null, service varchar(20) not null, caller varchar(20) not null, method varchar(30) not null, time timestamp not null, latency integer not null, params text);");
//		template.execute("create index calls_env_idx on calls (environment);");
//		template.execute("create index calls_serv_idx on calls (service);");
//		template.execute("create index calls_caller_idx on calls (caller);");
//		template.execute("create index calls_meth_idx on calls (method);");
//
//		// meta-data about logfiles
//		template.execute("create table logfiles(id serial primary key, environment varchar(20) not null, filename varchar(2000) not null);");
//		template.execute("create index logfiles_env_idx on logfiles (environment);");
//		template.execute("create index logfiles_filename_idx on logfiles (filename);");
//
//		// status about imported data
//		template.execute("create table status(id serial primary key, environment varchar(20) not null, up_to timestamp not null);");
//		template.execute("create index status_env_idx on logfiles (environment);");
//
//	}
//
//	public Integer getDbVersion(JdbcTemplate template) {
//		try {
//			return template.queryForInt("select max(version) from db_version");
//		}
//		catch (DataAccessException e) {
//			LOGGER.warn("No table DB_VERSION found : will try to create schema from scratch.");
//			return null;
//		}
//	}

	@Override
	public void clearEnvironments() {
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				session.createSQLQuery("delete from environments").executeUpdate();
				return null;
			}
		});
	}

	@Override
	public Long addEnvironment(Environment env) {
		return (Long) hibernateTemplate.save(env);
	}

	@Override
	public Environment saveEnvironment(Environment env) {
		return hibernateTemplate.merge(env);
	}

	@Override
	public Environment getEnvironment(final String name) {
		return hibernateTemplate.execute(new HibernateCallback<Environment>() {
			@Override
			public Environment doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("from Environment e where e.name = :name");
				query.setParameter("name", name);
				return (Environment) query.uniqueResult();
			}
		});
	}

	@Override
	public Environment getEnvironment(Long id) {
		return hibernateTemplate.get(Environment.class, id);
	}

	@Override
	public List<Environment> getEnvironments() {
		//noinspection unchecked
		return hibernateTemplate.find("from Environment");
	}

	@Override
	public void delEnvironment(Environment env) {
		hibernateTemplate.delete(env);
	}

	@Override
	public void clearCalls() {
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				session.createSQLQuery("delete from calls").executeUpdate();
				return null;
			}
		});
	}

	@Override
	public void clearImportedFiles() {
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				session.createSQLQuery("delete from log_files").executeUpdate();
				return null;
			}
		});
	}

	@Override
	public void clearUpToStatus() {
		hibernateTemplate.execute(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				session.createSQLQuery("delete from completion_statuses").executeUpdate();
				return null;
			}
		});
	}

	@Override
	public Long addCall(final Call call) {
		return (Long) hibernateTemplate.save(call);
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public List<Call> getCalls(final Environment environment) {
		return hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Query query = session.createQuery("from Call c where c.environment = :env");
				query.setParameter("env", environment);
				return query.list();
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public List<Call> getCalls(final Environment environment, final Date from, final Date to) {
		return hibernateTemplate.executeFind(new HibernateCallback<Object>() {
			@Override
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query;
				if (from != null && to != null) {
					query = session.createQuery("from Call c where c.environment = :env and c.timestamp >= :from and c.timestamp <= :to");
					query.setParameter("from", from);
					query.setParameter("to", to);
				}
				else if (from != null) {
					query = session.createQuery("from Call c where c.environment = :env and c.timestamp >= :from");
					query.setParameter("from", from);
				}
				else if (to != null) {
					query = session.createQuery("from Call c where c.environment = :env and c.timestamp <= :to");
					query.setParameter("to", to);
				}
				else {
					query = session.createQuery("from Call c where c.environment = :env");
				}
				query.setParameter("env", environment);
				return query.list();
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public Iterator<Call> iterateCalls(Environment environment, Date from, Date to) {

		final Iterator<Call> iter;

		if (from != null && to != null) {
			iter = hibernateTemplate.iterate("from Call c where c.environment = ? and c.timestamp >= ? and c.timestamp <= ?", environment, from, to);
		}
		else if (from != null) {
			iter = hibernateTemplate.iterate("from Call c where c.environment = ? and c.timestamp >= ?", environment, from);
		}
		else if (to != null) {
			iter = hibernateTemplate.iterate("from Call c where c.environment = ? and c.timestamp <= ?", environment, to);
		}
		else {
			iter = hibernateTemplate.iterate("from Call c where c.environment = ?", environment);
		}

		return iter;
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public Collection<CallStats> getLoadStatsFor(final Environment environment, final Date from, final Date to, final BreakdownCriterion[] criteria, final TimeResolution resolution) {
		return hibernateTemplate.execute(new HibernateCallback<Collection<CallStats>>() {
			@Override
			public Collection<CallStats> doInHibernate(Session session) throws HibernateException, SQLException {

				final String s = buildLoadStatsForQueryString(criteria, resolution, from, to);

				final Query query = session.createSQLQuery(s);
				query.setParameter("env_id", environment.getId());
				if (from != null ){
					query.setParameter("from", from);
				}
				if (to != null) {
					query.setParameter("to", to);
				}

				// run the query
				final List<?> list = query.list();

				// parse the results
				final List<CallStats> results = new ArrayList<CallStats>(list.size());
				for (Object a : list) {
					final Object[] array = (Object[]) a;
					final Number calls = (Number) array[0];
					final List<Object> coord = new ArrayList<Object>(criteria.length);
					for (int i = 0, criteriaLength = criteria.length; i < criteriaLength; i++) {
						coord.add(array[i + 1]);
					}
					final Date date = (Date) array[criteria.length + 1];
					results.add(new CallStats(calls, coord, date));
				}

				if (resolution == TimeResolution.FIVE_MINUTES || resolution == TimeResolution.FIFTEEN_MINUTES) {
					// we need to round manually the results here
					final Map<CallStatsKey, CallStats> map = new HashMap<CallStatsKey, CallStats>();
					for (CallStats c : results) {
						// round the date
						final Date date = round(c.getDate(), resolution);
						c.setDate(date);

						// merge the call if needed
						CallStats old = map.put(new CallStatsKey(c.getCoord(), date), c);
						if (old != null) {
							c.merge(old);
						}
					}
					return map.values();
				}
				else {
					return results;
				}
			}
		});
	}

	protected static String buildLoadStatsForQueryString(@org.jetbrains.annotations.Nullable BreakdownCriterion[] criteria, @Nullable TimeResolution resolution, @Nullable Date from, @Nullable Date to) {

		final String projection = buildProjection(criteria);
		final String timeGrouping = buildTimeGrouping(resolution);

		// build the mighty query
		final StringBuilder s = new StringBuilder();
		s.append("select sum(1)");
		if (projection != null) {
			s.append(", ").append(projection);
		}
		if (timeGrouping != null) {
			s.append(", ").append(timeGrouping);
		}
		s.append(" from calls where env_id = :env_id");
		if (from != null) {
			s.append(" and date >= :from");
		}
		if (to != null) {
			s.append(" and date <= :to");
		}
		if (projection != null || timeGrouping != null) {
			s.append(" group by");
			if (projection != null) {
				s.append(" ").append(projection);
			}
			if (projection != null && timeGrouping != null) {
				s.append(",");
			}
			if (timeGrouping != null) {
				s.append(" ").append(timeGrouping);
			}
		}
		return s.toString();
	}

	private static Date round(Date timestamp, TimeResolution resolution) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTime(timestamp);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		final int minutes = (cal.get(Calendar.MINUTE) / resolution.getMinutes()) * resolution.getMinutes();
		cal.set(Calendar.MINUTE, minutes);
		return cal.getTime();
	}

	private static String buildTimeGrouping(TimeResolution resolution) {
		if (resolution == null) {
			return null;
		}
		switch (resolution) {
		case MINUTE:
		case FIVE_MINUTES:
		case FIFTEEN_MINUTES:
			return "date_trunc('minute', date)";
		case HOUR:
			return "date_trunc('hour', date)";
		case DAY:
			return "date_trunc('day', date)";
		default:
			throw new IllegalArgumentException("Unknown time resolution = [" + resolution + "]");
		}
	}

	private static String buildProjection(BreakdownCriterion[] criteria) {
		if (criteria == null || criteria.length == 0) {
			return null;
		}
		StringBuilder projection = new StringBuilder();
		boolean first = true;
		for (BreakdownCriterion criterion : criteria) {
			if (first) {
				first = false;
			}
			else {
				projection.append(", ");
			}
			switch (criterion) {
			case CALLER:
				projection.append("caller");
				break;
			case ENVIRONMENT:
				projection.append("env");
				break;
			case METHOD:
				projection.append("method");
				break;
			case SERVICE:
				projection.append("service");
				break;
			default:
				throw new IllegalArgumentException("Unknown criterion = [" + criterion + "]");
			}
		}
		return projection.toString();
	}

	@Override
	public Long addImportedFile(final LogFile file) {
		return (Long) hibernateTemplate.save(file);
	}

	@Override
	public boolean isFileAlreadyImported(final Environment environment, final String filepath) {
		final Number count = hibernateTemplate.execute(new HibernateCallback<Number>() {
			@Override
			public Number doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("select count(*) from LogFile lf where lf.environment = :env and lf.filepath = :path");
				query.setParameter("env", environment);
				query.setParameter("path", filepath);
				return (Number) query.uniqueResult();
			}
		});
		return count.intValue() > 0;
	}

	@Override
	public CompletionStatus getCompletionStatus(final Environment environment) {
		return hibernateTemplate.execute(new HibernateCallback<CompletionStatus>() {
			@Override
			public CompletionStatus doInHibernate(Session session) throws HibernateException, SQLException {
				final Query query = session.createQuery("from CompletionStatus cs where cs.environment = :env");
				query.setParameter("env", environment);
				return (CompletionStatus) query.uniqueResult();
			}
		});
	}

	@Override
	public void setCompletionStatus(Environment environment, Date newUpTo) {
		CompletionStatus status = getCompletionStatus(environment);
		if (status == null) {
			status = new CompletionStatus(environment, newUpTo);
		}
		else {
			status.setUpTo(newUpTo);
		}
		hibernateTemplate.merge(status);
	}

	@Override
	public List<LogDirectory> getLogDirectories() {
		//noinspection unchecked
		return hibernateTemplate.find("from LogDirectory");
	}

	@Override
	public LogDirectory addLogDirectory(LogDirectory logDirectory) {
		return hibernateTemplate.merge(logDirectory);
	}

	@Override
	public LogDirectory getLogDirectory(Long id) {
		return hibernateTemplate.get(LogDirectory.class, id);
	}

	@Override
	public void delLogDirectory(LogDirectory dir) {
		hibernateTemplate.delete(dir);
	}

	@Override
	public void flush() {
		hibernateTemplate.flush();
	}

	@Override
	public void clear() {
		hibernateTemplate.clear();
	}

	@Override
	public List<JobDefinition> getJobs() {
		//noinspection unchecked
		return hibernateTemplate.find("from JobDefinition");
	}

	@Override
	public JobDefinition addJob(JobDefinition job) {
		return hibernateTemplate.merge(job);
	}

	@Override
	public JobDefinition getJob(Long id) {
		return hibernateTemplate.get(JobDefinition.class, id);
	}

	@Override
	public void delJob(JobDefinition job) {
		hibernateTemplate.delete(job);
	}
}
