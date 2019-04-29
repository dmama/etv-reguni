package ch.vd.unireg.scheduler;


import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.SchedulerException;

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
		throw new NotImplementedException("");
	}

	@Override
	public void register(JobDefinition jobDef) throws SchedulerException {
		throw new NotImplementedException("");
	}

	@Override
	public void registerCron(JobDefinition job, String cronExpression) throws SchedulerException, ParseException {
		throw new NotImplementedException("");
	}

	@Override
	public void registerCron(JobDefinition job, @Nullable Map<String, Object> params, String cronExpression) throws SchedulerException, ParseException {
		throw new NotImplementedException("");
	}

	@Override
	public JobDefinition startJob(String jobName, @Nullable Map<String, Object> params) throws JobAlreadyStartedException, SchedulerException {
		startedJobs.add(new JobData(jobName, params));
		return null;
	}

	@Override
	public JobDefinition queueJob(@NotNull String jobName, @Nullable Map<String, Object> params) throws SchedulerException {
		throw new NotImplementedException("");
	}

	@Override
	public Map<String, JobDefinition> getJobs() {
		throw new NotImplementedException("");
	}

	@Override
	public JobDefinition getJob(String name) {
		throw new NotImplementedException("");
	}

	@Override
	public List<JobDefinition> getSortedJobs() {
		throw new NotImplementedException("");
	}

	@Override
	public void stopJob(String name, @Nullable Duration timeout) throws SchedulerException {
		throw new NotImplementedException("");
	}

	@Override
	public boolean stopAllRunningJobs() {
		throw new NotImplementedException("");
	}

	public List<JobData> getStartedJobs() {
		return startedJobs;
	}
}
