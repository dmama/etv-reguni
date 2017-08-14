package ch.vd.uniregctb.evenement.organisation.interne.raisonsociale;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterneDeTraitement;
import ch.vd.uniregctb.evenement.organisation.interne.HandleStatus;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;

/**
 * Cette classe n'a de sens que pour expliciter l'existance de mutations portant sur la raison sociale de l'entreprise.
 *
 * Elle ne fait que stocker les messages à l'utilisateur concernant chaque établissement. Mais le faire ici permet d'avoir
 * une liste complète des mutations identifiées via les {@link EvenementOrganisationInterne}.
 *
 * @author Raphaël Marmier, 2016-05-18
 */
public class RaisonSociale extends EvenementOrganisationInterneDeTraitement {

	/**
	 * Chaque paire représente un message de changement de raison sociale de l'établissement associé à l'état de warning désiré.
	 */
	private final Map<Etablissement, List<Pair<String, Boolean>>> changementsRaison;

	public RaisonSociale(EvenementOrganisation evenement, Organisation organisation, Entreprise entreprise, EvenementOrganisationContext context,
	                     EvenementOrganisationOptions options, Map<Etablissement, List<Pair<String, Boolean>>> changementsRaison) throws EvenementOrganisationException {
		super(evenement, organisation, entreprise, context, options);
		this.changementsRaison = changementsRaison;
	}

	@Override
	public String describe() {
		return "Changement de raison sociale";
	}

	@Override
	public void doHandle(EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
		for (List<Pair<String, Boolean>> paires : changementsRaison.values()) {
			for (Pair<String, Boolean> paire : paires) {
				if (paire.getRight()) {
					warnings.addWarning(paire.getLeft());
				} else {
					suivis.addSuivi(paire.getLeft());
				}
			}
		}
		raiseStatusTo(HandleStatus.TRAITE);
	}

	@Override
	protected void validateSpecific(EvenementOrganisationErreurCollector erreurs, EvenementOrganisationWarningCollector warnings, EvenementOrganisationSuiviCollector suivis) throws EvenementOrganisationException {
	}
}
