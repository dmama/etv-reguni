package ch.vd.unireg.evenement.entreprise.interne.raisonsociale;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterneDeTraitement;
import ch.vd.unireg.evenement.entreprise.interne.HandleStatus;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

/**
 * Cette classe n'a de sens que pour expliciter l'existance de mutations portant sur la raison sociale de l'entreprise.
 *
 * Elle ne fait que stocker les messages à l'utilisateur concernant chaque établissement. Mais le faire ici permet d'avoir
 * une liste complète des mutations identifiées via les {@link EvenementEntrepriseInterne}.
 *
 * @author Raphaël Marmier, 2016-05-18
 */
public class RaisonSociale extends EvenementEntrepriseInterneDeTraitement {

	/**
	 * Chaque paire représente un message de changement de raison sociale de l'établissement associé à l'état de warning désiré.
	 */
	private final Map<Etablissement, List<Pair<String, Boolean>>> changementsRaison;

	public RaisonSociale(EvenementEntreprise evenement, EntrepriseCivile entrepriseCivile, Entreprise entreprise, EvenementEntrepriseContext context,
	                     EvenementEntrepriseOptions options, Map<Etablissement, List<Pair<String, Boolean>>> changementsRaison) throws EvenementEntrepriseException {
		super(evenement, entrepriseCivile, entreprise, context, options);
		this.changementsRaison = changementsRaison;
	}

	@Override
	public String describe() {
		return "Changement de raison sociale";
	}

	@Override
	public void doHandle(EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
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
	protected void validateSpecific(EvenementEntrepriseErreurCollector erreurs, EvenementEntrepriseWarningCollector warnings, EvenementEntrepriseSuiviCollector suivis) throws EvenementEntrepriseException {
	}
}
