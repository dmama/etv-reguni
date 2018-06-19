package ch.vd.unireg.evenement.organisation.interne;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.organisation.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;

/**
 * Evénement de réindexation silencieuse, à créer lorsqu'on veut être sûr que l'entité sera
 * réindexée, mais sans que cette réindexation soit rapportée dans le cadre du traitement de l'événement,
 * ni que le status ne soit modifié.
 *
 * @author Raphaël Marmier, 2015-09-04
 */
public class Indexation extends EvenementEntrepriseInterneInformationPure {

	private static final Logger LOGGER = LoggerFactory.getLogger(Indexation.class);

	final EvenementEntreprise event;

	public Indexation(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise,
	                  EvenementEntrepriseContext context, EvenementEntrepriseOptions options) throws
			EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
		event = evenement;
	}

	@Override
	public String describe() {
		return "Indexation";
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		final Entreprise pm = getEntreprise();
		if (pm != null) {
			programmeReindexation(pm, suivis);

		}
		// Cet événement reste en status REDONDANT. Utiliser la classe dérivée IndexationPure pour obtenir un statut TRAITE.
	}

	@Override
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
		// rien à valider
	}
}
