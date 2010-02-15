package ch.vd.uniregctb.testing;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.common.ClientConstants;
import ch.vd.uniregctb.common.TestingConstants;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

@ContextConfiguration(locations = {
		ClientConstants.UNIREG_BUSINESS_JOBS4WEBIT, TestingConstants.UNIREG_BUSINESSIT_DATABASE
})
public class InContainerTestingJobTest extends BusinessItTest {

	private BatchScheduler batchScheduler;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		batchScheduler = getBean(BatchScheduler.class, "batchScheduler");
	}

	@Test
	@NotTransactional
	public void testJob() throws Exception {

		JobDefinition job = batchScheduler.startJobWithDefaultParams(InContainerTestingJob.NAME);

		while (job.isRunning()) {
			Thread.sleep(1000);
		}

		Assert.assertEquals(JobDefinition.JobStatut.JOB_OK, job.getStatut());
	}

}
