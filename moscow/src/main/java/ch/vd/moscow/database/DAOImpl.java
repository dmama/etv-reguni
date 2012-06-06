package ch.vd.moscow.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.moscow.controller.graph.CallDimension;
import ch.vd.moscow.controller.graph.Filter;
import ch.vd.moscow.controller.graph.TimeResolution;
import ch.vd.moscow.data.Call;
import ch.vd.moscow.data.Caller;
import ch.vd.moscow.data.CompletionStatus;
import ch.vd.moscow.data.Environment;
import ch.vd.moscow.data.JobDefinition;
import ch.vd.moscow.data.LogDirectory;
import ch.vd.moscow.data.LogFile;
import ch.vd.moscow.data.Method;
import ch.vd.moscow.data.Service;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DAOImpl implements DAO {

//	private static final Logger LOGGER = Logger.getLogger(DAOImpl.class);

	private HibernateTemplate hibernateTemplate;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

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
	public Caller saveCaller(Caller caller) {
		return hibernateTemplate.merge(caller);
	}

	@Override
	public List<Caller> getCallers() {
		//noinspection unchecked
		return hibernateTemplate.find("from Caller");
	}

	@Override
	public Service saveService(Service service) {
		return hibernateTemplate.merge(service);
	}

	@Override
	public List<Service> getServices() {
		//noinspection unchecked
		return hibernateTemplate.find("from Service");
	}

	@Override
	public Method saveMethod(Method method) {
		return hibernateTemplate.merge(method);
	}

	@Override
	public List<Method> getMethods() {
		//noinspection unchecked
		return hibernateTemplate.find("from Method");
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

	private static class DimensionsValues {

		protected final Map<Long, Environment> environnements = new HashMap<Long, Environment>();
		protected final Map<Long, Service> services = new HashMap<Long, Service>();
		protected final Map<Long, Caller> callers = new HashMap<Long, Caller>();
		protected final Map<Long, Method> methods = new HashMap<Long, Method>();

		private DimensionsValues(List<Environment> environnements, List<Service> services, List<Caller> callers, List<Method> methods) {
			for (Environment env : environnements) {
				this.environnements.put(env.getId(), env);
			}
			for (Service service : services) {
				this.services.put(service.getId(), service);
			}
			for (Caller caller : callers) {
				this.callers.put(caller.getId(), caller);
			}
			for (Method method : methods) {
				this.methods.put(method.getId(), method);
			}
		}
	}
	
	@SuppressWarnings({"unchecked"})
	@Override
	public Collection<CallStats> getLoadStatsFor(final Filter[] filters, final Date from, final Date to, final CallDimension[] criteria, final TimeResolution resolution) {
		return hibernateTemplate.execute(new HibernateCallback<Collection<CallStats>>() {
			@Override
			public Collection<CallStats> doInHibernate(Session session) throws HibernateException, SQLException {

				final String s = buildLoadStatsForQueryString(filters, criteria, resolution, from, to);

				final Query query = session.createSQLQuery(s);
				if (filters != null) {
					for (int i = 0, filtersLength = filters.length; i < filtersLength; i++) {
						final Filter filter = filters[i];
						query.setParameter("filterValue" + i, dimValueToDb(filter.getDimension(), filter.getValue()));
					}
				}
				if (from != null ){
					query.setParameter("from", from);
				}
				if (to != null) {
					query.setParameter("to", to);
				}

				// run the query
				final List<?> list = query.list();

				// load some satellite data (dimensions only at the moment)
				final DimensionsValues dimValues = new DimensionsValues(session.createQuery("from Environment").list(), session.createQuery("from Service").list(),
						session.createQuery("from Caller").list(), session.createQuery("from Method").list());

				// parse the results
				final List<CallStats> results = new ArrayList<CallStats>(list.size());
				for (Object a : list) {
					final Object[] array = (Object[]) a;
					final Number calls = (Number) array[0];
					final Number latency = (Number) array[1];
					final Number maxPing = (Number) array[2];
					final List<Object> coord = new ArrayList<Object>(criteria.length);
					for (int i = 0, criteriaLength = criteria.length; i < criteriaLength; i++) {
						coord.add(getDimensionValueName(criteria[i], (Number) array[i + 3], dimValues));
					}
					final Date date = (Date) array[criteria.length + 3];
					results.add(new CallStats(calls, latency, maxPing, coord, date));
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

			private String getDimensionValueName(CallDimension dimension, Number id, DimensionsValues dimValues) {
				switch (dimension) {
				case ENVIRONMENT:
					return dimValues.environnements.get(id.longValue()).getName();
				case SERVICE:
					return dimValues.services.get(id.longValue()).getName();
				case CALLER:
					return dimValues.callers.get(id.longValue()).getName();
				case METHOD:
					return dimValues.methods.get(id.longValue()).getName();
				default:
					throw new IllegalArgumentException("Unknown dimension = [" + dimension + "]");
				}
			}
		});
	}

	protected static String buildLoadStatsForQueryString(@Nullable Filter[] filters, @Nullable CallDimension[] criteria, @Nullable TimeResolution resolution, @Nullable Date from,
	                                                     @Nullable Date to) {

		final String projection = buildProjection(criteria);
		final String timeGrouping = buildTimeGrouping(resolution);

		// build the mighty query
		final StringBuilder s = new StringBuilder();
		s.append("select sum(1) as calls, sum(latency) as latency, max(latency) as maxPing");
		if (projection != null) {
			s.append(", ").append(projection);
		}
		if (timeGrouping != null) {
			s.append(", ").append(timeGrouping);
		}
		s.append(" from calls");

		// where clause
		final StringBuilder where = new StringBuilder();

		if (filters != null) {
			for (int i = 0, filtersLength = filters.length; i < filtersLength; i++) {
				final Filter filter = filters[i];
				where.append(" and ").append(dimToColumn(filter.getDimension())).append(" = :filterValue").append(i);
			}
		}

		if (from != null) {
			where.append(" and date >= :from");
		}
		if (to != null) {
			where.append(" and date <= :to");
		}

		if (where.length() != 0) {
			s.append(" where ").append(where.substring(5)); // remove first " and "
		}

		// grouping
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

	private static String dimToColumn(CallDimension dimension) {
		switch (dimension) {
		case CALLER:
			return "caller_id";
		case ENVIRONMENT:
			return "env_id";
		case METHOD:
			return "method_id";
		case SERVICE:
			return "service_id";
		default:
			throw new IllegalArgumentException("Unknown dimension = [" + dimension + "]");
		}
	}

	private static Object dimValueToDb(CallDimension dimension, String value) {
		switch (dimension) {
		case CALLER:
			return Integer.valueOf(value);
		case ENVIRONMENT:
			return Integer.valueOf(value);
		case METHOD:
			return Integer.valueOf(value);
		case SERVICE:
			return Integer.valueOf(value);
		default:
			throw new IllegalArgumentException("Unknown dimension = [" + dimension + "]");
		}
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

	private static String buildProjection(CallDimension[] criteria) {
		if (criteria == null || criteria.length == 0) {
			return null;
		}
		StringBuilder projection = new StringBuilder();
		boolean first = true;
		for (CallDimension criterion : criteria) {
			if (first) {
				first = false;
			}
			else {
				projection.append(", ");
			}
			projection.append(dimToColumn(criterion));
		}
		return projection.toString();
	}

	@Override
	public List<Object> getDimensionValues(final CallDimension dimension) {
		return hibernateTemplate.execute(new HibernateCallback<List<Object>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<Object> doInHibernate(Session session) throws HibernateException, SQLException {
				final SQLQuery query;
				switch(dimension) {
				case ENVIRONMENT:
					query = session.createSQLQuery("select id from environments order by id");
					break;
				case CALLER:
					query = session.createSQLQuery("select id from callers order by id");
					break;
				case METHOD:
					query = session.createSQLQuery("select id from methods order by id");
					break;
				case SERVICE:
					query = session.createSQLQuery("select id from services order by id");
					break;
				default:
					throw new IllegalArgumentException("Unkown dimension = [" + dimension + "]");
				}
				return query.list();
			}
		});
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
