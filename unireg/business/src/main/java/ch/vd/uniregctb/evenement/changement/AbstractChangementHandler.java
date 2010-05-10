package ch.vd.uniregctb.evenement.changement;

import java.util.List;

import org.springframework.util.Assert;

import ch.vd.registre.base.utils.Pair;
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

		// [UNIREG-1321] on évite de flusher la session parce que cela provoque des optimistic exceptions sur les corrections de date de naissance.
		// [UNIREG-1691] on accepte que l'événement ne concerne pas un habitant (il suffit que la personne ait été habitante un jour pour que
		// le registre civil puisse envoyer de tels événements)
		final Long numero = getTiersDAO().getNumeroPPByNumeroIndividu(evenement.getIndividu().getNoTechnique(), true);
		Assert.notNull(numero, "Pas de personne physique correspondant au numéro individu " + evenement.getIndividu().getNoTechnique() + " (le cas aurait dû être vu plus tôt)");

		// Force une réindexation du Tiers
		// le tiers est modifié, donc l'interceptor d'indexation le réindexe et remet indexDirty à false
		// pp.setIndexDirty(true);
		// [UNIREG-757] Ca ne marche pas si le tiers est déjà dirty : à ce moment-là Hibernate ne détecte aucun changement (= il n'y en a
		// effectivement pas !) et l'intercepteur d'indexation n'est pas déclenché. Il veut mieux simplement indexer le tiers sur-le-champs.

		// on demande la réindexation du tiers
		indexer.schedule(numero);
		return null;
	}

	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}
}
