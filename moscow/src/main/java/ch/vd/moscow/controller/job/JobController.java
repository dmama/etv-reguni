package ch.vd.moscow.controller.job;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.quartz.SchedulerException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.moscow.controller.directory.DirectoryView;
import ch.vd.moscow.data.ImportLogsJob;
import ch.vd.moscow.data.JobDefinition;
import ch.vd.moscow.data.LogDirectory;
import ch.vd.moscow.database.DAO;
import ch.vd.moscow.job.JobManager;
import ch.vd.moscow.job.JobScheduler;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"UnusedDeclaration"})
@Controller
@RequestMapping(value = "/job")
public class JobController {

	private DAO dao;
	private JobScheduler scheduler;
	private JobManager manager;

	public void setDao(DAO dao) {
		this.dao = dao;
	}

	public void setScheduler(JobScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void setManager(JobManager manager) {
		this.manager = manager;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String list(Model model) throws Exception {
		model.addAttribute("jobs", getAllJobViews());
		model.addAttribute("directories", getAllDirectoryViews());
	    return "job/list";
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.GET)
	public String add(Model model) {
		model.addAttribute("job", new JobView());
		model.addAttribute("directories", getAllDirectoryViews());
		return "job/add";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	public String add(@ModelAttribute("job") final JobView view) throws SchedulerException, ParseException {

		ImportLogsJob job = new ImportLogsJob();
		job.setName(view.getName());
		job.setDirectory(dao.getLogDirectory(view.getDirId()));
		job.setCronExpression(view.getCronExpression());

		job = (ImportLogsJob) dao.addJob(job);
		dao.flush();

		scheduler.registerJob(job);

		return "redirect:/job/list.do";
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	public String edit(@RequestParam(value = "id", required = true) Long jobId, Model model) {
		final JobDefinition job = dao.getJob(jobId);
		if (job == null) {
			throw new RuntimeException("Le job n'existe pas !");
		}

		model.addAttribute("job", new JobView(job, null));
		model.addAttribute("directories", getAllDirectoryViews());

		return "job/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	public String edit(@ModelAttribute("directory") final JobView view) throws SchedulerException, ParseException {
		final JobDefinition job = dao.getJob(view.getId());
		if (job == null) {
			throw new RuntimeException("Le job n'existe pas !");
		}

		job.setName(view.getName());
		job.setCronExpression(view.getCronExpression());
		if (job instanceof ImportLogsJob) {
			ImportLogsJob ij =(ImportLogsJob) job;
			ij.setDirectory(dao.getLogDirectory(view.getDirId()));
		}

		dao.flush();
		scheduler.updateJob(job);

		return "redirect:/job/list.do";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/del.do", method = RequestMethod.POST)
	public String del(@RequestParam(value = "id", required = true) Long id) throws Exception {
		final JobDefinition job = dao.getJob(id);
		if (job == null) {
			throw new RuntimeException("Le job n'existe pas !");
		}
		dao.delJob(job);
		scheduler.unregisterJob(job);
	    return "redirect:/job/list.do";
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/run.do", method = RequestMethod.POST)
	public String run(@RequestParam(value = "id", required = true) Long jobId) throws SchedulerException {
		final JobDefinition job = dao.getJob(jobId);
		if (job == null) {
			throw new RuntimeException("Le job n'existe pas !");
		}

		scheduler.executeImmediately(job);
		return "redirect:/job/list.do";
	}

	private List<JobView> getAllJobViews() {
		final List<JobView> jobs = new ArrayList<JobView>();
		for (JobDefinition job : dao.getJobs()) {
			jobs.add(new JobView(job, manager.getStatus(job)));
		}
		return jobs;
	}

	private List<DirectoryView> getAllDirectoryViews() {
		final List<DirectoryView> directories = new ArrayList<DirectoryView>();
		for (LogDirectory directory : dao.getLogDirectories()) {
			directories.add(new DirectoryView(directory));
		}
		return directories;
	}

}
