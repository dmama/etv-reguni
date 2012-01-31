package ch.vd.uniregctb.evenement.civil.interne.changement;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public abstract class ChangementBase extends EvenementCivilInterne {

	/**
	 * Construit un événement civil interne sur la base d'un événement civil externe (Reg-PP).
	 *
	 * @param evenement un événement civil externe
	 * @param context   le context d'exécution de l'événement
	 * @param options les options de l'evt
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException
	 *          si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */
	protected ChangementBase(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Construit un événement civil interne sur la base d'un événement civil externe (eCH).
	 *
	 * @param event un événement civil externe
	 * @param context   le context d'exécution de l'événement
	 * @param options les options de l'evt
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException
	 *          si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */

	public ChangementBase(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ChangementBase(Individu individu, Individu conjoint, RegDate dateEvenement,
	                         Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, context);
	}



	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// si on n'a pas de contribuable correspondant à l'événement, on ne pourra pas l'indexer, mais c'est tout, ce n'est
		// pas ici que l'on va déterminer si c'est un problème ou pas
		final PersonnePhysique principal = getPrincipalPP();
		if (principal != null) {
			// on demande la réindexation du tiers
			context.getIndexer().schedule(principal.getNumero());
		}
		else {
			Audit.info(getNumeroEvenement(), String.format("L'individu %d ne correspond à aucun contribuable connu, pas d'indexation", getNoIndividu()));
		}

		return HandleStatus.TRAITE;
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
