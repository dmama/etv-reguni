package ch.vd.unireg.errors;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorsController {

	@RequestMapping(value = "/error.do")
	public String error() throws IOException {
		return "errors/error";
	}

	@RequestMapping(value = "/403.do")
	public String forbidden() throws IOException {
		return "errors/403";
	}

	@RequestMapping(value = "/404.do")
	public String notFound() throws IOException {
		return "errors/404";
	}
}
