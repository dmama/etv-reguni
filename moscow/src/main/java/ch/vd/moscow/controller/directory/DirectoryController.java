package ch.vd.moscow.controller.directory;

import ch.vd.moscow.controller.environment.EnvironmentView;
import ch.vd.moscow.data.Environment;
import ch.vd.moscow.data.LogDirectory;
import ch.vd.moscow.database.DAO;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration"})
@Controller
@RequestMapping(value = "/directory")
public class DirectoryController {

	private DAO dao;

	public void setDao(DAO dao) {
		this.dao = dao;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String list(Model model) throws Exception {

		final List<DirectoryView> directories = new ArrayList<DirectoryView>();
		for (LogDirectory directory : dao.getLogDirectories()) {
			directories.add(new DirectoryView(directory));
		}
		model.addAttribute("directories", directories);
		model.addAttribute("environments", getAllEnvironments());

	    return "directory/list";
	}

	private List<EnvironmentView> getAllEnvironments() {
		final List<EnvironmentView> environments = new ArrayList<EnvironmentView>();
		for (Environment environment : dao.getEnvironments()) {
			environments.add(new EnvironmentView(environment));
		}
		return environments;
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	public String add(@RequestParam(value = "env_id", required = true) Long envId,
	                  @RequestParam(value = "path", required = true) String path,
	                  @RequestParam(value = "pattern", required = true) String pattern) throws Exception {
		final Environment env = dao.getEnvironment(envId);
		if (env == null) {
			throw new RuntimeException("L'environnement n'existe pas !");
		}
		if (StringUtils.isBlank(path)) {
			throw new RuntimeException("Le chemin est vide !");
		}
		dao.addLogDirectory(new LogDirectory(env, path, pattern));

	    return "redirect:/directory/list.do";
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	public String edit(@RequestParam(value = "id", required = true) Long dirId, Model model) {
		final LogDirectory dir = dao.getLogDirectory(dirId);
		if (dir == null) {
			throw new RuntimeException("Le répertoire n'existe pas !");
		}

		model.addAttribute("directory", new DirectoryView(dir));
		model.addAttribute("environments", getAllEnvironments());

		return "directory/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	public String edit(@ModelAttribute("directory") final DirectoryView view) {
		final LogDirectory dir = dao.getLogDirectory(view.getId());
		if (dir == null) {
			throw new RuntimeException("Le répertoire n'existe pas !");
		}

		dir.setEnvironment(dao.getEnvironment(view.getEnvId()));
		dir.setDirectoryPath(view.getDirectoryPath());
		dir.setPattern(view.getPattern());

		return "redirect:/directory/list.do";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/del.do", method = RequestMethod.POST)
	public String del(@RequestParam(value = "id", required = true) Long id) throws Exception {
		final LogDirectory dir = dao.getLogDirectory(id);
		if (dir == null) {
			throw new RuntimeException("Le répertoire n'existe pas !");
		}
		dao.delLogDirectory(dir);
	    return "redirect:/directory/list.do";
	}
}
