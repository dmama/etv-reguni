package ch.vd.uniregctb.admin.evenementExterne;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jmx.export.MBeanExporter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.EncodingFixHelper;
import ch.vd.uniregctb.jms.JmxAwareEsbMessageEndpointManager;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;

@Controller
@RequestMapping(value = "/admin/jms")
public class QueuesJmsController {

	private SecurityProviderInterface securityProvider;
	private Map<String,JmxAwareEsbMessageEndpointManager> jmxManager;

	private MBeanExporter exporter;



	@SuppressWarnings("UnusedDeclaration")
	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}


	public void setJmxManager(Map<String, JmxAwareEsbMessageEndpointManager> jmxManager) {
		this.jmxManager = jmxManager;
	}

	@RequestMapping(value = "/show.do", method = RequestMethod.GET)
	public String statusQueues(Model model) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		final List<QueuesJmsView> list = new ArrayList<>();
		for (String key : jmxManager.keySet()) {
			JmxAwareEsbMessageEndpointManager messageEndpointManager = jmxManager.get(key);
			final QueuesJmsView queuesJmsView =
					new QueuesJmsView(key,messageEndpointManager.getDestinationName(),messageEndpointManager.getDescription(),
							messageEndpointManager.getReceivedMessages(),
							messageEndpointManager.getMaxConcurrentConsumers(),messageEndpointManager.isRunning());
			list.add(queuesJmsView);
		}

		model.addAttribute("queues", list);
		return "admin/jms";

	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public ModelAndView startQueue(@RequestParam(value = "id", required = true) String identifiant) {

		try {
			if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
				throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
			}


			jmxManager.get(identifiant).start();
			return new ModelAndView("redirect:/admin/jms/show.do");
		}
		catch (Exception e) {
			return new ModelAndView(EncodingFixHelper.breakToIso(e.getMessage()));
		}


	}

	@RequestMapping(value = "/stop.do", method = RequestMethod.POST)
	public ModelAndView stopQueue(@RequestParam(value = "id", required = true) String identifiant) {

		try {
			if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
				throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
			}

			jmxManager.get(identifiant).stop();
			return new ModelAndView("redirect:/admin/jms/show.do");

		}
		catch (Exception e) {
			return new ModelAndView(EncodingFixHelper.breakToIso(e.getMessage()));
		}

	}

}
