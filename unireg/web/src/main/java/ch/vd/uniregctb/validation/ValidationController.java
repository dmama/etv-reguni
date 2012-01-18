package ch.vd.uniregctb.validation;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

@Controller
@RequestMapping(value = "/validation")
public class ValidationController {

	private TiersDAO tiersDAO;
	private AdresseService adresseService;
	private ServiceCivilService serviceCivil;
	private ValidationService validationService;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	@RequestMapping(value = "/message.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String message(@RequestParam("tiers") long tiersId, Model mav) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers != null) {
			final ValidationResults results = validationService.validate(tiers);
			checkEtatsCivils(tiers, results);
			checkAdresses(tiers, results);
			mav.addAttribute("results", results);
		}

		return "validation/message";
	}

	/**
	 * Met à jour les erreurs autour des états civils
	 *
	 * @param tiers             un tiers
	 * @param validationResults le résultat de validation à augmenter
	 */
	private void checkEtatsCivils(Tiers tiers, ValidationResults validationResults) {
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isConnuAuCivil()) {
				final Individu ind = serviceCivil.getIndividu(pp.getNumeroIndividu(), RegDate.get());
				for (EtatCivil etatCivil : ind.getEtatsCivils()) {
					if (etatCivil.getDateDebut() == null) {
						final String message = String.format("Le contribuable possède un état civil (%s) sans date de début. Dans la mesure du possible, cette date a été estimée.",
								etatCivil.getTypeEtatCivil().asCore());
						validationResults.addWarning(message);
					}
				}
			}
		}
	}

	/**
	 * Calcul les adresses historiques de manière stricte, et reporte toutes les erreurs trouvées.
	 *
	 * @param tiers             le tiers dont on veut vérifier les adresses
	 * @param validationResults le résultat de la validation à compléter avec les éventuelles erreurs trouvées.
	 */
	private void checkAdresses(Tiers tiers, ValidationResults validationResults) {
		try {
			adresseService.getAdressesFiscalHisto(tiers, true /* strict */);
		}
		catch (Exception e) {
			validationResults.addWarning("Des incohérences ont été détectées dans les adresses du tiers : " + e.getMessage() +
					". Dans la mesure du possible, ces incohérences ont été corrigées à la volée (mais pas sauvées en base).");
		}
	}
}
