package ch.vd.moscow.controller.environment;

import java.util.ArrayList;
import java.util.List;

import ch.vd.moscow.controller.directory.DirectoryView;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.moscow.data.Environment;
import ch.vd.moscow.data.LogDirectory;
import ch.vd.moscow.database.DAO;

@SuppressWarnings({"UnusedDeclaration"})
@Controller
@RequestMapping(value = "/environment")
public class EnvironmentController {

	private DAO dao;

	public void setDao(DAO dao) {
		this.dao = dao;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/list.do", method = RequestMethod.GET)
	public String list(Model model) throws Exception {

		final List<EnvironmentView> environments = new ArrayList<EnvironmentView>();
		for (Environment environment : dao.getEnvironments()) {
			environments.add(new EnvironmentView(environment));
		}
		model.addAttribute("environments", environments);

	    return "environment/list";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/add.do", method = RequestMethod.POST)
	public String add(@RequestParam(value = "name", required = true) String name) throws Exception {
		dao.addEnvironment(new Environment(name));
	    return "redirect:/environment/list.do";
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.GET)
	public String edit(@RequestParam(value = "id", required = true) Long dirId, Model model) {
		final Environment env = dao.getEnvironment(dirId);
		if (env == null) {
			throw new RuntimeException("L'environnement n'existe pas !");
		}

		model.addAttribute("environment", new EnvironmentView(env));

		return "environment/edit";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/edit.do", method = RequestMethod.POST)
	public String edit(@ModelAttribute("environment") final EnvironmentView view) {
		final Environment env = dao.getEnvironment(view.getId());
		if (env == null) {
			throw new RuntimeException("L'environnement n'existe pas !");
		}

		env.setName(view.getName());
		return "redirect:/environment/list.do";
	}

	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "/del.do", method = RequestMethod.POST)
	public String del(@RequestParam(value = "id", required = true) Long id) throws Exception {
		final Environment env = dao.getEnvironment(id);
		if (env == null) {
			throw new RuntimeException("L'environnement n'existe pas !");
		}
		dao.delEnvironment(env);
	    return "redirect:/environment/list.do";
	}
}
