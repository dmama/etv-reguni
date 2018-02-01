package ch.vd.unireg.delai;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ControllerUtils;
import ch.vd.unireg.common.Flash;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.security.AccessDeniedException;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.utils.WebContextUtils;

@Controller
@RequestMapping(value = "/declaration/delai")
public class DelaiController {

	private HibernateTemplate hibernateTemplate;
	private SecurityProviderInterface securityProvider;
	private MessageSource messageSource;
	private ControllerUtils controllerUtils;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public void setControllerUtils(ControllerUtils controllerUtils) {
		this.controllerUtils = controllerUtils;
	}

	/**
	 * Annuler le délai spécifié.
	 */
	@Transactional(rollbackFor = Throwable.class)
	@RequestMapping(value = "annuler.do", method = RequestMethod.POST)
	public String annulerDelai(@RequestParam(value = "id", required = true) final long id) throws Exception {

		// Vérifie les paramètres
		final DelaiDeclaration delai = hibernateTemplate.get(DelaiDeclaration.class, id);
		if (delai == null) {
			throw new ObjectNotFoundException(messageSource.getMessage("error.delai.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		final Declaration declaration = delai.getDeclaration();
		if (declaration instanceof DeclarationImpotOrdinairePP) {
			if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PP)) {
				throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des délais sur les déclarations d'impôt des personnes physiques.");
			}
		}
		else if (declaration instanceof DeclarationImpotOrdinairePM) {
			if (!SecurityHelper.isGranted(securityProvider, Role.DI_DELAI_PM)) {
				throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des délais sur les déclarations d'impôt des personnes morales.");
			}
		}
		else if (declaration instanceof DeclarationImpotSource) {
			if (!SecurityHelper.isGranted(securityProvider, Role.LR)) {
				throw new AccessDeniedException("Vous ne possédez pas le droit IfoSec d'édition des listes récapitulatives.");
			}
		}
		else if (declaration == null) {
			throw new ObjectNotFoundException("Délai " + id + " non rattaché à une déclaration.");
		}
		else {
			throw new IllegalArgumentException("Type de déclaration inattendu : " + declaration.getClass().getName());
		}

		// si le délai est déjà annulé, on ne fait rien
		if (!delai.isAnnule()) {

			// on refuse l'annulation du premier délai accordé
			if (delai.getEtat() == EtatDelaiDocumentFiscal.ACCORDE) {
				final RegDate premier = declaration.getPremierDelai();
				if (declaration.getDelaiAccordeAu() == premier) {
					throw new IllegalArgumentException("Le premier délai accordé ne peut pas être annulé.");
				}
			}

			final Tiers tiers = declaration.getTiers();
			controllerUtils.checkAccesDossierEnEcriture(tiers.getId());

			// On annule le délai
			delai.setAnnule(true);

			Flash.message("Le délai a été annulé.");
		}
		else {
			Flash.message("Le délai était déjà annulé.");
		}

		return (declaration instanceof DeclarationImpotOrdinaire ? "redirect:/di/editer.do?id=" : "redirect:/lr/edit-lr.do?id=") + declaration.getId();
	}
}
