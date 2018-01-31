package ch.vd.uniregctb.scheduler;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

public class OverrideSchedulerFactoryBean  extends SchedulerFactoryBean {


	@Override
	protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) throws SchedulerException {
		String name = schedulerName;
		if( name == null) {
			name  = "schedulerQuartz" + System.currentTimeMillis();
		} else {
			name += System.currentTimeMillis();
		}
		setSchedulerName(name);
		return super.createScheduler(schedulerFactory, name);
	}
}
