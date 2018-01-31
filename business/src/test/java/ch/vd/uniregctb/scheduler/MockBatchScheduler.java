package ch.vd.uniregctb.scheduler;


import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.quartz.SchedulerException;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockBatchScheduler implements BatchScheduler {

	public static class JobData {
		private final String name;
		private final Map<String, Object> params;

		public JobData(String name, Map<String, Object> params) {
			this.name = name;
			this.params = params;
		}

		public String getName() {
			return name;
		}

		public Map<String, Object> getParams() {
			return params;
		}
	}

	private List<JobData> startedJobs = new ArrayList<>();

	@Override
	public boolean isStarted() throws org.quartz.SchedulerException {
		throw new NotImplementedException();
	}

	@Override
	public void register(JobDefinition jobDef) throws SchedulerException {
		throw new NotImplementedException();
	}

	@Override
	public void registerCron(JobDefinition job, String cronExpression) throws SchedulerException, ParseException {
		throw new NotImplementedException();
	}

	@Override
	public void registerCron(JobDefinition job, @Nullable Map<String, Object> params, String cronExpression) throws SchedulerException, ParseException {
		throw new NotImplementedException();
	}

	@Override
	public JobDefinition startJob(String jobName, @Nullable Map<String, Object> params) throws JobAlreadyStartedException, SchedulerException {
		startedJobs.add(new JobData(jobName, params));
		return null;
	}

	@Override
	public Map<String, JobDefinition> getJobs() {
		throw new NotImplementedException();
	}

	@Override
	public JobDefinition getJob(String name) {
		throw new NotImplementedException();
	}

	@Override
	public List<JobDefinition> getSortedJobs() {
		throw new NotImplementedException();
	}

	@Override
	public void stopJob(String name, @Nullable Duration timeout) throws SchedulerException {
		throw new NotImplementedException();
	}

	@Override
	public boolean stopAllRunningJobs() {
		throw new NotImplementedException();
	}

	public List<JobData> getStartedJobs() {
		return startedJobs;
	}
}
