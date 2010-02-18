package ch.vd.uniregctb.indexer.tiers;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.IndexerBatchException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public interface GlobalTiersIndexer {

	/**
	 * Efface l'index.
	 */
	public void overwriteIndex();

	/**
	 * Indexe ou réindexe un tiers à partir de son numéro. Le tiers est chargé de la base dans une transaction spécifique.
	 */
	public void indexTiers(long id) throws IndexerException;

	public void indexTiers(Tiers tiers) throws IndexerException;

	public void indexTiers(Tiers tiers, boolean removeBefore) throws IndexerException;

	public void indexTiers(Tiers tiers, boolean removeBefore, boolean followDependents) throws IndexerException;

	/**
	 * Index les tiers spécifié.
	 *
	 * @param tiers
	 *            les tiers à indexer
	 * @param removeBefore
	 *            si <b>vrai</b> les données du tiers seront supprimée de l'index avant d'être réinsérée; si <b>false</b> les données seront
	 *            simplement ajoutées.
	 * @param followDependents
	 *            si <b>vrai</b> les tiers liés (ménage commun, ...) seront aussi indexées.
	 * @throws IndexerBatchException
	 *             en cas d'exception lors de l'indexation d'un ou plusieurs tiers. La méthode essaie d'indexer tous les tiers dans tous les
	 *             cas, ce qui veut dire que si le premier tiers lève une exception, les tiers suivants seront quand même indexés.
	 */
	public void indexTiers(List<Tiers> tiers, boolean removeBefore, boolean followDependents) throws IndexerBatchException;

	public int indexAllDatabase() throws IndexerException;

	public int indexAllDatabase(boolean assertSameNumber, StatusManager statusManager) throws IndexerException;


	public int getApproxDocCount();
	public int getExactDocCount();

	public enum Mode {
		FULL,
		INCREMENTAL,
		DIRTY_ONLY
	}

	/**
	 * Indexe ou réindexe tout ou partie de la base de données.
	 *
	 * @param nbThreads
	 *            le nombre de threads simultanés utilisés pour indexer la base
	 * @param mode
	 *            le mode d'indexation voulu.
	 * @param prefetchIndividus
	 *            détermine si les individus doivent être préchargés en vrac
	 *
	 * @return le nombre de tiers indexés
	 */
	public int indexAllDatabaseAsync(StatusManager statusManager, int nbThreads, Mode mode, boolean prefetchIndividus)
			throws IndexerException;


	/**
	 * Supprime un tiers de l'indexer
	 *
	 * @param id
	 * @param type
	 */
	public void removeEntity(Long id, String type);

	/**
	 * Flag qui indique si l'indexation doit se faire a la volée ou si elle sera faite a posteriori.
	 * <p>
	 * Note: cette valeur est valable <b>pour le thread courant</b>.
	 */
	public boolean isOnTheFlyIndexation();

	/**
	 * Active ou désactive l'indexation <b>pour le thread courant</b>.
	 */
	public void setOnTheFlyIndexation(boolean onTheFlyIndexation);

	/**
	 * @param globalIndex
	 *            the globalIndex to set
	 */
	public abstract void setGlobalIndex(GlobalIndexInterface globalIndex);

	/**
	 * @param tiersDAO
	 *            the tiersDAO to set
	 */
	public abstract void setTiersDAO(TiersDAO tiersDAO);

	/**
	 * @param hostCivilService
	 *            the hostCivilService to set
	 */
	public abstract void setServiceCivilService(ServiceCivilService civilService);

	public abstract void setTiersService(TiersService tiersService);

	public abstract AdresseService getAdresseService();

	public abstract void setAdresseService(AdresseService adresseService);

}
