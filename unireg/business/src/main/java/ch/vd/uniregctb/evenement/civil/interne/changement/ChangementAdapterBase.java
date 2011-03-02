package ch.vd.uniregctb.evenement.civil.interne.changement;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public abstract class ChangementAdapterBase extends EvenementCivilInterneBase {

	/**
	 * Construit un événement civil interne sur la base d'un événement civil externe.
	 *
	 *
	 * @param evenement un événement civil externe
	 * @param context   le context d'exécution de l'événement
	 * @throws ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException
	 *          si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */
	protected ChangementAdapterBase(EvenementCivilExterne evenement, EvenementCivilContext context, AbstractChangementHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
	}

	public Pair<PersonnePhysique,PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

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
