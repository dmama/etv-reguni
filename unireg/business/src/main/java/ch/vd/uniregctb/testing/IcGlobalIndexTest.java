package ch.vd.uniregctb.testing;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class IcGlobalIndexTest extends InContainerTest {

	private static final Logger LOGGER = Logger.getLogger(IcGlobalIndexTest.class);

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

		executeInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				{
					PersonnePhysique nh = new PersonnePhysique(false);
					nh.setNom(nom);
					nh.setPrenom(prenom1);
					getTiersDAO().save(nh);
				}

				{
					PersonnePhysique nh = new PersonnePhysique(false);
					nh.setNom(nom);
					nh.setPrenom(prenom2);
					getTiersDAO().save(nh);
				}
				return null;
			}
		});

		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(nom);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			Assert.isEqual(2, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(prenom1);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			Assert.isEqual(1, list.size());
		}
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(prenom2);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			Assert.isEqual(1, list.size());
		}
	}

	@Test
	public void testInTransaction() throws Exception {

		String nom = "Bolomey";
		String prenom1 = "Claude";

		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom(nom);
			nh.setPrenom(prenom1);
			nh = (PersonnePhysique)getTiersDAO().save(nh);

			@SuppressWarnings("unused")
			long id = nh.getNumero();
			id = 0;
		}

		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison(nom);
			List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
			Assert.isEqual(0, list.size());
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
			nh.setPrenom(prenom1);
			save(nh);
		}
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNumero(num);
			nh.setNom(nom);
			nh.setPrenom(prenom1);
			try {
				LOGGER.warn("L'exception générée ci-dessous est normale!");
				save(nh);
				Assert.fail();
			}
			catch (Exception ex) {
				LOGGER.warn("L'exception générée ci-dessus est normale!");
				// noop
			}
		}

		Assert.isEqual(1, globalTiersSearcher.getApproxDocCount());
	}


	public void setGlobalTiersSearcher(GlobalTiersSearcher globalTiersSearcher) {
		this.globalTiersSearcher = globalTiersSearcher;
	}

	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}

	/**
	 * remplace le save du dao Tiers car il merge l'object au lieu de save.
	 * @param tiers le tiers à sauver
	 */
	private Long save(final Tiers tiers) {
		Long id = (Long)executeInTransaction(new TransactionCallback() {

			public Object doInTransaction(TransactionStatus status) {
				Long id = (Long)getTiersDAO().getHibernateTemplate().save(tiers);
				return id;
			}

		});
		return id;
	}

}
