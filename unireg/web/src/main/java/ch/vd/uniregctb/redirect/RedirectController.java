package ch.vd.uniregctb.redirect;

import java.io.UnsupportedEncodingException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

@Controller
@RequestMapping(value = "/redirect")
public class RedirectController {

	private ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@RequestMapping(value = "/{appname}.do", method = RequestMethod.GET)
	public String redirect(@PathVariable(value = "appname") ApplicationFiscale application,
	                       @RequestParam(value = "id", required = true) Long id) throws UnsupportedEncodingException {

		final String url = infraService.getUrlVers(application, id);
		if (url == null) {
			throw new ServiceInfrastructureException("L'url vers l'application " + application.name() + " n'est pas définie !");
		}
		return "redirect:" + url;
	}
}
