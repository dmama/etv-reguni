package ch.vd.moscow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class ErrorController {

	@RequestMapping(value = "/404.do")
	public String list() throws Exception {
	    return "404";
	}
}
