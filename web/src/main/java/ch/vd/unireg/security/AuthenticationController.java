package ch.vd.unireg.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AuthenticationController {

	@RequestMapping(value = "/authenticationFailed.do")
	public String index() {
		return "security/authenticationFailed";
	}
}
