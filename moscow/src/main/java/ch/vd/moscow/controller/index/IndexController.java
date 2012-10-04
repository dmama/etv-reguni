package ch.vd.moscow.controller.index;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.moscow.controller.environment.EnvironmentView;
import ch.vd.moscow.data.Environment;
import ch.vd.moscow.database.DAO;

@Controller
public class IndexController {

	private DAO dao;

	public void setDao(DAO dao) {
		this.dao = dao;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@RequestMapping(value = "/index.do", method = RequestMethod.GET)
    public String index(Model model) throws Exception {

		final List<EnvironmentView> environments = new ArrayList<EnvironmentView>();
		for (Environment environment : dao.getEnvironments()) {
			environments.add(new EnvironmentView(environment));
		}
		model.addAttribute("environments", environments);

		return "index";
    }
}
