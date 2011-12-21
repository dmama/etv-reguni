package ch.vd.moscow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@Controller
public class ErrorController {

	@RequestMapping(value = "/404.do")
	public String _404() throws Exception {
	    return "404";
	}

	@RequestMapping(value = "/error.do")
	public String error() throws Exception {
	    return "error";
	}
}
