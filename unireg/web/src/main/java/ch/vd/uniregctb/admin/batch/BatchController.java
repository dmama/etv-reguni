package ch.vd.uniregctb.admin.batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;

@Controller
@RequestMapping(value = "/admin/batch/")
public class BatchController {

	private BatchScheduler batchScheduler;

	@SuppressWarnings("UnusedDeclaration")
	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	@RequestMapping(value = "/running.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@ResponseBody
	public List<BatchView> running() {

		final List<BatchView> list = new ArrayList<BatchView>();

		final Date limit = DateUtils.addMinutes(DateHelper.getCurrentDate(), -10);

		final Collection<JobDefinition> jobs = batchScheduler.getJobs().values();
		for (JobDefinition job : jobs) {
			final Date lastStart = job.getLastStart();
			if (job.isRunning() || (lastStart != null && limit.before(lastStart))) {
				list.add(new BatchView(job));
			}
		}

		return list;
	}
}
