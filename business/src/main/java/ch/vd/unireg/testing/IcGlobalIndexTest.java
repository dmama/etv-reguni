package ch.vd.unireg.testing;

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersCriteria;

public class IcGlobalIndexTest extends InContainerTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(IcGlobalIndexTest.class);

	private GlobalTiersSearcher globalTiersSearcher;
	private GlobalTiersIndexer globalTiersIndexer;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		globalTiersIndexer.overwriteIndex();
	}

	@Test
	@NotTransactional
	public void testBase() throws Exception {

		final String nom = "Bolomey";
		final String prenom1 = "Claude";
		final String prenom2 = "Alain";

		executeInTransaction(status -> {
			{
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom(nom);
				nh.setPrenomUsuel(prenom1);
				getTiersDAO().save(nh);
			}

			{
				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom(nom);
				nh.setPrenomUsuel(prenom2);
				getTiersDAO().save(nh);
			}
			return null;
		});

		globalTiersIndexer.sync();
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(nom);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			if (list.size() != 2) {
				throw new IllegalArgumentException();
			}
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(prenom1);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			if (list.size() != 1) {
				throw new IllegalArgumentException();
			}
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(prenom2);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			if (list.size() != 1) {
				throw new IllegalArgumentException();
			}
		}
	}

	@Test
	public void testInTransaction() throws Exception {

		String nom = "Bolomey";
		String prenom1 = "Claude";

		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom(nom);
			nh.setPrenomUsuel(prenom1);
			nh = (PersonnePhysique)getTiersDAO().save(nh);

			@SuppressWarnings("unused")
			long id = nh.getNumero();
			id = 0;
		}

		globalTiersIndexer.sync();
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(nom);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			if (!list.isEmpty()) {
				throw new IllegalArgumentException();
			}
		}

	}

	@Test
	@NotTransactional
	public void testInError() throws Exception {
		final String nom = "Bolomey";
		final String prenom1 = "Claude";
		final Long num = 12345678L;

		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNumero(num);
			nh.setNom(nom);
			nh.setPrenomUsuel(prenom1);
			save(nh);
		}
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNumero(num);
			nh.setNom(nom);
			nh.setPrenomUsuel(prenom1);
			try {
				LOGGER.warn("L'exception générée ci-dessous est normale!");
				save(nh);
				throw new IllegalArgumentException();
			}
			catch (Exception ex) {
				LOGGER.warn("L'exception générée ci-dessus est normale!");
				// noop
			}
		}

		globalTiersIndexer.sync();
		if (globalTiersSearcher.getApproxDocCount() != 1) {
			throw new IllegalArgumentException();
		}
	}


	public void setGlobalTiersSearcher(GlobalTiersSearcher globalTiersSearcher) {
		this.globalTiersSearcher = globalTiersSearcher;
	}

	@Override
	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}

	/**
	 * remplace le save du dao Tiers car il merge l'object au lieu de save.
	 * @param tiers le tiers à sauver
	 */
	private Long save(final Tiers tiers) {
		return executeInTransaction(status -> {
			final Session session = getSessionFactory().getCurrentSession();
			return (Long) session.save(tiers);
		});
	}

}
