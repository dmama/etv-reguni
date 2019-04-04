package ch.vd.unireg.admin.evenementExterne;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.unireg.common.EncodingFixHelper;
import ch.vd.unireg.jms.MessageListenerContainerJmxInterface;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;

@Controller
@RequestMapping(value = "/admin/jms")
public class QueuesJmsController {

	private SecurityProviderInterface securityProvider;
	private Map<String, MessageListenerContainerJmxInterface> jmxManager;

	@SuppressWarnings("UnusedDeclaration")
	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setJmxManager(Map<String, MessageListenerContainerJmxInterface> jmxManager) {
		this.jmxManager = jmxManager;
	}

	@RequestMapping(value = "/show.do", method = RequestMethod.GET)
	public String statusQueues(Model model) {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
		}

		final List<Map.Entry<String, MessageListenerContainerJmxInterface>> sortedEntries = new ArrayList<>(jmxManager.entrySet());
		sortedEntries.sort(Comparator.comparing(entry -> entry.getValue().getDestinationName()));

		final List<QueuesJmsView> list = new ArrayList<>(sortedEntries.size());
		for (Map.Entry<String, MessageListenerContainerJmxInterface> entry : sortedEntries) {
			final MessageListenerContainerJmxInterface messageEndpointManager = entry.getValue();
			final QueuesJmsView queuesJmsView = new QueuesJmsView(entry.getKey(),
			                                                      messageEndpointManager.getDestinationName(),
			                                                      messageEndpointManager.getDescription(),
			                                                      messageEndpointManager.getReceivedMessages(),
			                                                      messageEndpointManager.getMaxConcurrentConsumers(),
			                                                      messageEndpointManager.isRunning());
			list.add(queuesJmsView);
		}

		model.addAttribute("queues", list);
		return "admin/jms";

	}

	@RequestMapping(value = "/start.do", method = RequestMethod.POST)
	public ModelAndView startQueue(@RequestParam(value = "id", required = true) String identifiant) {

		try {
			if (!SecurityHelper.isAnyGranted(securityProvider, Role.ADMIN, Role.TESTER)) {
				throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
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
				throw new AccessDeniedException("vous ne possédez pas les droits d'administration pour l'application Unireg");
			}

			jmxManager.get(identifiant).stop();
			return new ModelAndView("redirect:/admin/jms/show.do");

		}
		catch (Exception e) {
			return new ModelAndView(EncodingFixHelper.breakToIso(e.getMessage()));
		}
	}
}