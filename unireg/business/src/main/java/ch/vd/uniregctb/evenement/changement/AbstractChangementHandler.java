package ch.vd.uniregctb.evenement.changement;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerBase;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Handler avec comportement par défaut la réindexation de l'individu à l'origine de l'événement.
 *
 * @author Pavel BLANCO
 *
 */
public abstract class AbstractChangementHandler extends EvenementCivilHandlerBase {

	private GlobalTiersIndexer indexer;

	@Override
	public Pair<PersonnePhysique,PersonnePhysique> handle(EvenementCivil evenement, List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {

		// si on n'a pas de contribuable correspondant à l'événement, on ne pourra pas l'indexer, mais c'est tout, ce n'est
		// pas ici que l'on va déterminer si c'est un problème ou pas
		final Long numero = evenement.getPrincipalPPId();
		if (numero != null) {
			// on demande la réindexation du tiers
			indexer.schedule(numero);
		}
		else {
			Audit.info(evenement.getNumeroEvenement(), String.format("L'individu %d ne correspond à aucun contribuable connu, pas d'indexation", evenement.getNoIndividu()));
		}

		return null;
	}

	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	@Override
	protected boolean isContribuableObligatoirementConnuAvantTraitement(EvenementCivil evenement) {
		return super.isContribuableObligatoirementConnuAvantTraitement(evenement) && !autoriseIndividuInconnuFiscalement();
	}

	/**
	 * @return <code>true</code> si le traitement supporte le fait que l'individu soit inconnu, <code>false</code> dans le cas contraire
	 */
	protected boolean autoriseIndividuInconnuFiscalement() {
		return true;
	}
}
