package ch.vd.unireg.evenement.organisation.interne.transformation;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.organisation.interne.HandleStatus;
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
