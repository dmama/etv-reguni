package ch.vd.unireg.evenement.iam;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.validation.ValidationService;

public class EvenementIAMServiceImpl implements EvenementIAMService, EvenementIAMHandler {

	private TiersDAO tiersDAO;
	private ValidationService validationService;
	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementIAMServiceImpl.class);

	@Override
	public void onEvent(EvenementIAM event) throws EvenementIAMException {

		if (event instanceof EnregistrementEmployeur) {
			onEnregistrementEmployeur((EnregistrementEmployeur) event);
		}
		else {
			throw new IllegalArgumentException("Type d'événement inconnu = " + event.getClass());
		}
	}

	protected void onEnregistrementEmployeur(EnregistrementEmployeur enregistrement) throws EvenementIAMException {
		//on recupere la liste des employeurs à modifier
		final List<InfoEmployeur> infoEmployeurs = enregistrement.getEmployeursAMettreAJour();
		if (infoEmployeurs != null) {
			for (InfoEmployeur infoEmployeur : infoEmployeurs) {
				final long debiteurId = infoEmployeur.getNoEmployeur();
				final DebiteurPrestationImposable debiteur = tiersDAO.getDebiteurPrestationImposableByNumero(debiteurId);
				if (debiteur == null) {
					throw new EvenementIAMException(EsbBusinessCode.DPI_INEXISTANT, "L'employeur n°" + debiteurId + " est inconnu.");
				}

				final ValidationResults results = validationService.validate(debiteur);
				if (results.hasErrors()) {
					throw new EvenementIAMException(EsbBusinessCode.TIERS_INVALIDE, "L'employeur  n°" + debiteurId + " ne valide pas (" + results.toString() + ").");
				}

				updateInformationsDebiteur(infoEmployeur, debiteur);
			}
		}
		else {
			LOGGER.info(String.format("Informations employeurs absentes pour une action create ou update," +
					" le message avec business id %s est ignoré", enregistrement.getBusinessId()));
		}
	}

	/**
	 * Mets a jour le mode de communication et l'id du logicile pour le débiteur
	 * @param infoEmployeur
	 * @param debiteur
	 */
	private void updateInformationsDebiteur(InfoEmployeur infoEmployeur, DebiteurPrestationImposable debiteur) {
		debiteur.setModeCommunication(infoEmployeur.getModeCommunication());
		debiteur.setLogicielId(infoEmployeur.getLogicielId());
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}
}
