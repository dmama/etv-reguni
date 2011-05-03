package ch.vd.uniregctb.evenement.civil.interne.changement;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public abstract class ChangementBase extends EvenementCivilInterne {

	/**
	 * Construit un événement civil interne sur la base d'un événement civil externe.
	 *
	 * @param evenement un événement civil externe
	 * @param context   le context d'exécution de l'événement
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException
	 *          si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */
	protected ChangementBase(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ChangementBase(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, TypeEvenementCivil typeEvenementCivil, RegDate dateEvenement,
	                         Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, typeEvenementCivil, dateEvenement, numeroOfsCommuneAnnonce, context);
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {

		// si on n'a pas de contribuable correspondant à l'événement, on ne pourra pas l'indexer, mais c'est tout, ce n'est
		// pas ici que l'on va déterminer si c'est un problème ou pas
		final Long numero = getPrincipalPPId();
		if (numero != null) {
			// on demande la réindexation du tiers
			context.getIndexer().schedule(numero);
		}
		else {
			Audit.info(getNumeroEvenement(), String.format("L'individu %d ne correspond à aucun contribuable connu, pas d'indexation", getNoIndividu()));
		}

		return null;
	}

	@Override
	protected boolean isContribuableObligatoirementConnuAvantTraitement() {
		return super.isContribuableObligatoirementConnuAvantTraitement() && !autoriseIndividuInconnuFiscalement();
	}

	/**
	 * @return <code>true</code> si le traitement supporte le fait que l'individu soit inconnu, <code>false</code> dans le cas contraire
	 */
	protected boolean autoriseIndividuInconnuFiscalement() {
		return true;
	}
}
