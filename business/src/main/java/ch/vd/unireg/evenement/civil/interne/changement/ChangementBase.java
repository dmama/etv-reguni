package ch.vd.unireg.evenement.civil.interne.changement;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.tiers.PersonnePhysique;

public abstract class ChangementBase extends EvenementCivilInterne {

	/**
	 * Construit un événement civil interne sur la base d'un événement civil externe (Reg-PP).
	 *
	 * @param evenement un événement civil externe
	 * @param context   le context d'exécution de l'événement
	 * @param options les options de l'evt
	 * @throws ch.vd.unireg.evenement.civil.common.EvenementCivilException
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
	 * @throws ch.vd.unireg.evenement.civil.common.EvenementCivilException
	 *          si l'événement est suffisemment incohérent pour que tout traitement soit impossible.
	 */

	public ChangementBase(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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
