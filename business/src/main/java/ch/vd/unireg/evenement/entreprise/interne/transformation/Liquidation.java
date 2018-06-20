package ch.vd.unireg.evenement.entreprise.interne.transformation;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.entreprise.interne.HandleStatus;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * @author Raphaël Marmier, 2016-02-19
 */
public class Liquidation extends EvenementEntrepriseInterneDeTraitement {

	protected Liquidation(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile,
	                      Entreprise entreprise, EvenementEntrepriseContext context,
	                      EvenementEntrepriseOptions options) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
	}

	@Override
	public String describe() {
		return "Liquidation";
	}


	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		warnings.addWarning("Une vérification, pouvant aboutir à un traitement manuel (processus complexe), est requise pour cause de Liquidation de l'entreprise.");
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
	}
}
