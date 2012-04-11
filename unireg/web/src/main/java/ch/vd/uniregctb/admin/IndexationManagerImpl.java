package ch.vd.uniregctb.admin;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class IndexationManagerImpl implements IndexationManager {

	private final Logger LOGGER = Logger.getLogger(IndexationManagerImpl.class);

	private GlobalTiersIndexer tiersIndexer;
	private DataEventService dataEventService;
	private TiersDAO tiersDAO;

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void reindexTiers(long id) {

		final Long indNo = getNumeroIndividu(id);
		if (indNo != null) {
			LOGGER.info("Demande de réindexation manuelle du tiers n°" + id + " (avec éviction du cache des données de l'individu n°" + indNo + ')');
			// on en profite pour forcer l'éviction des données cachées pour l'individu
			dataEventService.onIndividuChange(indNo);
		}
		else {
			LOGGER.info("Demande de réindexation manuelle du tiers n°" + id);
		}
		dataEventService.onTiersChange(id); // on force l'éviction des donées cachées pour tous les types de tiers (pas seulement pour les habitants)

		// on demande la réindexation du tiers
		tiersIndexer.schedule(id);
	}

	/**
	 * Retourne le numéro d'individu d'un tiers à partir de son numéro.
	 *
	 * @param id un numéro de tiers
	 * @return le numéro d'individu du tiers spécifié; ou <b>null</b> si cette information n'est pas disponible.
	 */
	private Long getNumeroIndividu(long id) {
		final Tiers tiers = tiersDAO.get(id);
		final Long indNo;
		if (tiers instanceof PersonnePhysique) {
			final PersonnePhysique pp = (PersonnePhysique) tiers;
			indNo = pp.getNumeroIndividu();
		}
		else {
			indNo = null;
		}
		return indNo;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersIndexer(GlobalTiersIndexer tiersIndexer) {
		this.tiersIndexer = tiersIndexer;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}
}
