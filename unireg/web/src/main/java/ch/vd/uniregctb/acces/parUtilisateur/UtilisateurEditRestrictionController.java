package ch.vd.uniregctb.acces.parUtilisateur;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.acces.parUtilisateur.manager.UtilisateurEditRestrictionManager;
import ch.vd.uniregctb.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.Flash;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.DroitAccesException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.security.SecurityProviderInterface;

@Controller
public class UtilisateurEditRestrictionController {

	private static final String ACCESS_DENIED_MESSAGE = "Vous ne possédez pas les droits IfoSec nécessaires à cette opération de gestion des droits.";
	protected final Logger LOGGER = Logger.getLogger(UtilisateurEditRestrictionController.class);

	private UtilisateurEditRestrictionManager utilisateurEditRestrictionManager;
	private SecurityProviderInterface securityProvider;

	public void setUtilisateurEditRestrictionManager(UtilisateurEditRestrictionManager utilisateurEditRestrictionManager) {
		this.utilisateurEditRestrictionManager = utilisateurEditRestrictionManager;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@RequestMapping(value = "/acces/restrictions-utilisateur.do", method = RequestMethod.GET)
	public ModelAndView getRestrictionsUtilisateur(HttpServletRequest request, @RequestParam("noIndividuOperateur") Long noIndividuOperateur) throws Exception {

		if (!SecurityHelper.isAnyGranted(securityProvider, Role.SEC_DOS_ECR, Role.SEC_DOS_LEC)) {
			throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
		}

		final WebParamPagination pagination = new WebParamPagination(request, "restriction", 25, "id", true);
		UtilisateurEditRestrictionView utilisateurEditRestrictionView = utilisateurEditRestrictionManager.get(noIndividuOperateur, pagination);
		return new ModelAndView("acces/par-utilisateur/restrictions-utilisateur", "command", utilisateurEditRestrictionView);
	}

	@RequestMapping(value = "/acces/restrictions-utilisateur/annuler.do", method = RequestMethod.POST)
	public String onPostAnnulerRestriction(
			@RequestParam("noIndividuOperateur") Long noIndividuOperateur,
			@RequestParam(value = "aAnnuler", required = false) List<Long> restrictionsAAnnuler,
			@RequestParam("annuleTout") Boolean annuleTout) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SEC_DOS_ECR)) {
			throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
		}

		try {
			if (annuleTout) {
				utilisateurEditRestrictionManager.annulerToutesLesRestrictions(noIndividuOperateur);
			}
			else {
				utilisateurEditRestrictionManager.annulerRestrictions(restrictionsAAnnuler);
			}
		}
		catch (DroitAccesException e) {
			throw new ActionException(e.getMessage(), e);
		}
		return "redirect:/acces/restrictions-utilisateur.do?noIndividuOperateur=" + noIndividuOperateur;
	}

	@RequestMapping(value = "/acces/restrictions-utilisateur/exporter.do", method = RequestMethod.POST)
	public String onPostExporter(@RequestParam("noIndividuOperateur") Long noIndividuOperateur) {

		if (!SecurityHelper.isGranted(securityProvider, Role.SEC_DOS_LEC)) {
			throw new AccessDeniedException(ACCESS_DENIED_MESSAGE);
		}

		final ExtractionJob job = utilisateurEditRestrictionManager.exportListeDroitsAcces(noIndividuOperateur);
		Flash.message(String.format("Demande d'export enregistrée (%s)", job.getDescription()));
		return "redirect:/acces/restrictions-utilisateur.do?noIndividuOperateur=" + noIndividuOperateur;
	}
}
