package ch.vd.uniregctb.admin;

import static junit.framework.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.junit.Test;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobDefinition.JobStatut;

public class GestionJobTest extends WithoutSpringTest {

	public class MyJobDefinition extends JobDefinition {

		public MyJobDefinition() {
			super("bla", "cat", 1,"descr");
		}

		@Override
		protected void doExecute(Map<String, Object> params) throws Exception {
		}

		@Override
		public boolean isVisible() {
			return isTesting();
		}

	}

	@Test
	public void testLastStartBeforeToday() {

		JobDefinition def = new MyJobDefinition();
		def.setRunningMessage("Ouahou!");
		def.setStatut(JobStatut.JOB_OK);

		{
			Date dateStart = DateHelper.getCalendar(2005, 2, 17, 15, 2, 52).getTime();
			def.setLastStart(dateStart);

			GestionJob job = new GestionJob(def);
			assertEquals("17.02.2005 15:02:52", job.getLastStart());
		}
	}

	@Test
	public void testLastStartNearToday() {

		JobDefinition def = new MyJobDefinition();
		def.setRunningMessage("Ouahou!");
		def.setStatut(JobStatut.JOB_OK);

		{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)-1);
			Date dateStart = cal.getTime();
			def.setLastStart(dateStart);

			GestionJob job = new GestionJob(def);
			assertEquals(DateHelper.timeToString(dateStart), job.getLastStart());
		}
	}

	@Test
	public void testLastEndSameDay() {

		JobDefinition def = new MyJobDefinition();
		def.setRunningMessage("Ouahou!");
		def.setStatut(JobStatut.JOB_OK);

		{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)-1);
			Date dateEnd = cal.getTime();
			def.setLastEnd(dateEnd);
			cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)-1);
			Date dateStart = cal.getTime();
			def.setLastStart(dateStart);

			GestionJob job = new GestionJob(def);
			assertEquals(DateHelper.timeToString(dateStart), job.getLastStart());
			assertEquals("1h 00m 00s", job.getDuration());
		}
	}

	@Test
	public void testLastEndPastDates() {

		JobDefinition def = new MyJobDefinition();
		def.setRunningMessage("Ouahou!");
		def.setStatut(JobStatut.JOB_OK);

		{
			Date dateStart = DateHelper.getCalendar(2005, 2, 18, 15, 2, 52).getTime();
			def.setLastStart(dateStart);
			Date dateEnd = DateHelper.getCalendar(2005, 2, 18, 16, 23, 12).getTime();
			def.setLastEnd(dateEnd);

			GestionJob job = new GestionJob(def);
			assertEquals("18.02.2005 15:02:52", job.getLastStart());
			assertEquals("1h 20m 20s", job.getDuration());
		}
	}

	@Test
	public void testLastEndPreviousDay() {

		JobDefinition def = new MyJobDefinition();
		def.setRunningMessage("Ouahou!");
		def.setStatut(JobStatut.JOB_OK);

		{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)-1);
			Date dateEnd = cal.getTime();
			def.setLastEnd(dateEnd);

			cal.setTimeInMillis(cal.getTimeInMillis() - 100000*1000/*Around one day*/);
			Date dateStart = cal.getTime();
			def.setLastStart(dateStart);

			GestionJob job = new GestionJob(def);
			assertEquals(DateHelper.dateTimeToDisplayString(dateStart), job.getLastStart());
			assertEquals("1j 03h 46m 40s", job.getDuration());
		}
	}

	@Test
	public void testStillRunning() {

		JobDefinition def = new MyJobDefinition();
		def.setRunningMessage("Ouahou!");
		def.setStatut(JobStatut.JOB_RUNNING);

		{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.SECOND, cal.get(Calendar.SECOND)-32);
			Date date = cal.getTime();
			def.setLastStart(date);

			GestionJob job = new GestionJob(def);
			assertEquals(DateHelper.timeToString(date), job.getLastStart());
			assertEquals("32s", job.getDuration());
		}
		{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE)-45);
			cal.set(Calendar.SECOND, cal.get(Calendar.SECOND)+4);
			Date date = cal.getTime();
			def.setLastStart(date);

			GestionJob job = new GestionJob(def);
			assertEquals(DateHelper.timeToString(date), job.getLastStart());
			assertEquals("44m 56s", job.getDuration());
		}
		{
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)-1);
			cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE)-12);
			cal.set(Calendar.SECOND, cal.get(Calendar.SECOND)+4);
			Date date = cal.getTime();
			def.setLastStart(date);

			GestionJob job = new GestionJob(def);
			assertEquals(DateHelper.timeToString(date), job.getLastStart());
			assertEquals("1h 11m 56s", job.getDuration());
		}
	}

}
