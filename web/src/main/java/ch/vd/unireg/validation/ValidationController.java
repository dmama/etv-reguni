package ch.vd.unireg.validation;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.shared.validation.ValidationService;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersDAO;

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

	/**
	 * Valide le tiers spécifié et retourne les éventuels messages d'erreur et de warning.
	 *
	 * @param tiersId le numéro de tiers à vérifier
	 * @return les éventuels messages d'erreur et de warning sous format JSON.
	 */
	@ResponseBody
	@RequestMapping(value = "/tiers.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public ValidationResultsView tiers(@RequestParam("id") long tiersId) {

		final Tiers tiers = tiersDAO.get(tiersId);
		if (tiers == null) {
			throw new TiersNotFoundException(tiersId);
		}

		final ValidationResults results = validationService.validate(tiers);
		checkEtatsCivils(tiers, results);
		checkAdresses(tiers, results);

		return new ValidationResultsView(results);
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
				if (ind == null) {
					throw new IndividuNotFoundException(pp);
				}
				if (ind.getEtatsCivils() != null) {
					for (EtatCivil etatCivil : ind.getEtatsCivils().asList()) {
						if (etatCivil.getDateDebut() == null) {
							final String message = String.format("Le contribuable possède un état civil (%s) sans date de début. Dans la mesure du possible, cette date a été estimée.",
									EtatCivilHelper.civil2core(etatCivil.getTypeEtatCivil()));
							validationResults.addWarning(message);
						}
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
