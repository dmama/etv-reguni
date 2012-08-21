package ch.vd.uniregctb.evenement.civil.interne.depart;

import java.util.EnumSet;
import java.util.Set;

import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;

public class DepartPrincipalEch extends DepartPrincipal {

	private static final String IGNORE = "Ignoré car départ vaudois";

	private static final Set<EtatEvenementCivil> ETATS_AVEC_MESSAGE_IGNORE_POSSIBLE = EnumSet.of(EtatEvenementCivil.A_VERIFIER, EtatEvenementCivil.REDONDANT, EtatEvenementCivil.TRAITE);

	public DepartPrincipalEch(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options, Adresse ancienneAdresse) throws EvenementCivilException {
		super(event, context, options, ancienneAdresse);

		//SIFISC-4230 Pour les evenements ech,les départs vaudois sont ignorés
		if (isDepartVaudois()) {
			final String message = String.format("%s : la nouvelle commune de résidence %s est toujours dans le canton.", IGNORE, nouvelleCommune.getNomMinuscule());
			event.setCommentaireTraitement(message);
		}
	}

	@Override
	public boolean shouldResetCommentaireTraitement(EtatEvenementCivil etat, String commentaireTraitement) {
		// [SIFISC-6008] On ne dit pas qu'on ignore un événement civil qui est parti en erreur...
		return super.shouldResetCommentaireTraitement(etat, commentaireTraitement) || (!ETATS_AVEC_MESSAGE_IGNORE_POSSIBLE.contains(etat) && commentaireTraitement.startsWith(IGNORE));
	}
}
