package ch.vd.unireg.fourreNeutre;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.RetourEditiqueControllerHelper;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatErreur;
import ch.vd.unireg.editique.EditiqueResultatReroutageInbox;
import ch.vd.unireg.fourreNeutre.manager.FourreNeutreManager;
import ch.vd.unireg.fourreNeutre.view.FourreNeutreView;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.utils.WebContextUtils;

@Controller
@RequestMapping(value = "/fourre-neutre")
public class FourreNeutreController {
	private SecurityProviderInterface securityProvider;
	private FourreNeutreManager fourreNeutreManager;
	private Validator validator;
	private MessageSource messageSource;
	private RetourEditiqueControllerHelper retourEditiqueControllerHelper;
	private static final String PERIODES_DISPONIBLES = "periodes";

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setFourreNeutreManager(FourreNeutreManager fourreNeutreManager) {
		this.fourreNeutreManager = fourreNeutreManager;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	public void setRetourEditiqueControllerHelper(RetourEditiqueControllerHelper retourEditiqueControllerHelper) {
		this.retourEditiqueControllerHelper = retourEditiqueControllerHelper;
	}



	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(validator);
	}


	private static void checkDroitFourreNeutre(SecurityProviderInterface securityProvider) {
		if (!SecurityHelper.isAnyGranted(securityProvider, Role.GEST_FOURRE_NEUTRE)) {
			throw new AccessDeniedException("Vous ne possédez aucun droit IfoSec pour gérer les fourres neutres");
		}
	}

	private  void checkTypePopulation(long tiersId){
		if (!fourreNeutreManager.isAutorisePourFourreNeutre(tiersId)) {
			throw new AccessDeniedException("Le tiers "+tiersId+" n'est pas autorisé pour la génération de fourre neutre");
		}
	}

	@RequestMapping(value = "/imprimer.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String showImprimerForm(@RequestParam(value = "numero", required = true) Long tiersId, Model model) throws Exception{
		checkDroitFourreNeutre(securityProvider);
		checkTypePopulation(tiersId);
		final FourreNeutreView view = new FourreNeutreView();
		view.setTiersId(tiersId);

		view.setPeriodeFiscale(null);
		model.addAttribute("command",view);
		return showFourreView(tiersId, model);
	}

	@NotNull
	private String showFourreView(@RequestParam(value = "numero", required = true) Long tiersId, Model model) {
		initialiserPeriodesDisponibles(tiersId, model);
		return "fourre-neutre/imprimer";
	}

	private void initialiserPeriodesDisponibles(@RequestParam(value = "numero", required = true) Long tiersId, Model model) {
		final List<Integer> periodes = fourreNeutreManager.getPeriodesAutoriseesPourImpression(tiersId);
		model.addAttribute(PERIODES_DISPONIBLES,periodes);
	}


	@RequestMapping(value = "/imprimer.do", method = RequestMethod.POST)
	@Transactional(rollbackFor = Throwable.class)
	public String doImprimerForm(@Valid @ModelAttribute("command") final FourreNeutreView view, BindingResult result,HttpServletResponse response, Model model) throws Exception{
		checkDroitFourreNeutre(securityProvider);
		checkTypePopulation(view.getTiersId());
		if (result.hasErrors()) {
			return showFourreView(view.getTiersId(), model);
		}
		// On imprime le document
		final long tiersId = view.getTiersId();
		final EditiqueResultat resultat =fourreNeutreManager.envoieImpressionLocaleFourreNeutre(tiersId, view.getPeriodeFiscale());
		final FourreNeutreController.RedirectImpressionFourreNeutre inbox = new FourreNeutreController.RedirectImpressionFourreNeutre(tiersId);
		final FourreNeutreController.RedirectImpressionFourreNeutreApresErreur erreur = new FourreNeutreController.RedirectImpressionFourreNeutreApresErreur(tiersId, messageSource);
		return retourEditiqueControllerHelper.traiteRetourEditiqueAfterRedirect(resultat,
		                                                                        "fourreNeutre",
		                                                                        "redirect:/tiers/visu.do?id=" + tiersId,
		                                                                        true,
		                                                                        inbox,
		                                                                        null,
		                                                                        erreur);

	}

	private static class RedirectImpressionFourreNeutre implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatReroutageInbox> {
		private final long id;

		public RedirectImpressionFourreNeutre(long id) {
			this.id = id;
		}

		@Override
		public String doJob(EditiqueResultatReroutageInbox resultat) {
			return "redirect:/tiers/visu.do?id=" + id;
		}

	}


	private static class RedirectImpressionFourreNeutreApresErreur implements RetourEditiqueControllerHelper.TraitementRetourEditique<EditiqueResultatErreur> {
		private final long id;
		private final MessageSource messageSource;

		public RedirectImpressionFourreNeutreApresErreur(long id, MessageSource messageSource) {
			this.id = id;
			this.messageSource = messageSource;
		}

		@Override
		public String doJob(EditiqueResultatErreur resultat) {
			final String message = messageSource.getMessage("global.error.communication.editique", null, WebContextUtils.getDefaultLocale());
			Flash.error(message);
			return "fourre-neutre/imprimer";
		}
	}
}
