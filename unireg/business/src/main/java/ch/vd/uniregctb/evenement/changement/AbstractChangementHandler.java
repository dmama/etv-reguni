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

		final Long numero = evenement.getPrincipalPPId();
		Assert.notNull(numero, "Pas de personne physique correspondant au numéro individu " + evenement.getNoIndividu() + " (le cas aurait dû être vu plus tôt)");

		// on demande la réindexation du tiers
		indexer.schedule(numero);
		return null;
	}

	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}
}
