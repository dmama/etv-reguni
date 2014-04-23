package ch.vd.uniregctb.about;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AboutController {

	@RequestMapping(value = "/about.do", method = RequestMethod.GET)
	public String about() {
		return "about/about";
	}
}
