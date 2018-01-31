package ch.vd.uniregctb;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

	@RequestMapping(value = "/index.do")
	public String index() throws IOException {
		return "redirect:/tiers/list.do";
	}
}
