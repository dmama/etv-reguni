package ch.vd.uniregctb.indexer.tiers;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.IndexerFormatHelper;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
public class MenageCommunIndexableTest extends BusinessTest {

	private TiersDAO dao;
	private TiersService tiersService;

	public MenageCommunIndexableTest() {
		setWantIndexation(true);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TiersDAO.class, "tiersDAO");
		tiersService = getBean(TiersService.class, "tiersService");
	}

	/**
	 * UNIREG-601: Vérifie qu'un ménage-commun avec rapport-entre-tiers annulés est quand même indexé avec les noms des personnes physiques.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMenageCommunRapportsAnnules() throws Exception {

		final RegDate dateN1 = RegDate.get(1956, 1, 21);
		final RegDate dateN2 = RegDate.get(1967, 12, 3);
		final String noAVS1 = "123.45.678";
		final String noAVS2 = "987.65.432";

		class Ids {
			Long idHab1;
			Long idHab2;
			Long idMc;
		}
		final Ids ids = new Ids();

		// Crée un ménage commun dont les rapports-entre-tiers sont annulés
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nhab1 = new PersonnePhysique(false);
				nhab1.setNom("Maillard");
				nhab1.setPrenom("Philippe");
				nhab1.setNumeroAssureSocial(noAVS1);
				nhab1.setDateNaissance(dateN1);
				nhab1.setSexe(Sexe.MASCULIN);
				nhab1 = (PersonnePhysique) dao.save(nhab1);
				ids.idHab1 = nhab1.getNumero();

				PersonnePhysique nhab2 = new PersonnePhysique(false);
				nhab2.setNom("Maillard-Gallet");
				nhab2.setPrenom("Gladys");
				nhab2.setNumeroAssureSocial(noAVS2);
				nhab2.setDateNaissance(dateN2);
				nhab2.setSexe(Sexe.FEMININ);
				nhab2 = (PersonnePhysique) dao.save(nhab2);
				ids.idHab2 = nhab2.getNumero();

				MenageCommun mc = new MenageCommun();
				mc.setNumero(12345678L);
				mc = (MenageCommun) dao.save(mc);
				ids.idMc = mc.getNumero();

				RapportEntreTiers rapport = tiersService.addTiersToCouple(mc, nhab1, RegDate.get(2001, 2, 23), null);
				rapport.setAnnule(true);
				rapport = tiersService.addTiersToCouple(mc, nhab2, RegDate.get(2001, 2, 23), null);
				rapport.setAnnule(true);

				return null;
			}
		});

		globalTiersIndexer.sync();

		assertIndexData("Philippe Maillard", "", dateN1, ids.idHab1);
		assertIndexData("Gladys Maillard-Gallet", "", dateN2, ids.idHab2);
		assertIndexData("Philippe Maillard", "Gladys Maillard-Gallet", dateN1.index() + " " + dateN2.index(), ids.idMc);
	}

	/**
	 * UNIREG-1619: Vérifie un ménage-commun avec rapport-entre-tiers annulés et non-annulés
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMenageCommunRapportsAnnulesEtNonAnnules() throws Exception {

		final RegDate dateN1 = RegDate.get(1956, 1, 21);
		final RegDate dateN3 = RegDate.get(1960, 1, 1);
		final String noAVS1 = "123.45.678";
		final String noAVS3 = "111.11.113";

		class Ids {
			Long idHab1;
			Long idHab3;
			Long idMc;
		}
		final Ids ids = new Ids();

		// Crée un ménage commun dont les rapports-entre-tiers sont annulés
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nhab1 = new PersonnePhysique(false);
				nhab1.setNom("Maillard");
				nhab1.setPrenom("Philippe");
				nhab1.setNumeroAssureSocial(noAVS1);
				nhab1.setDateNaissance(dateN1);
				nhab1.setSexe(Sexe.MASCULIN);
				nhab1 = (PersonnePhysique) dao.save(nhab1);
				ids.idHab1 = nhab1.getNumero();

				PersonnePhysique nhab3 = new PersonnePhysique(false);
				nhab3.setNom("Casanova");
				nhab3.setPrenom("Giacomo");
				nhab3.setNumeroAssureSocial(noAVS3);
				nhab3.setDateNaissance(dateN3);
				nhab3.setSexe(Sexe.MASCULIN);
				nhab3 = (PersonnePhysique) dao.save(nhab3);
				ids.idHab3 = nhab3.getNumero();

				MenageCommun mc = new MenageCommun();
				mc.setNumero(12345678L);
				mc = (MenageCommun) dao.save(mc);
				ids.idMc = mc.getNumero();

				tiersService.addTiersToCouple(mc, nhab1, RegDate.get(2001, 2, 23), null);
				RapportEntreTiers rapportAnnule = tiersService.addTiersToCouple(mc, nhab3, RegDate.get(2000, 1, 1), null);
				rapportAnnule.setAnnule(true);

				return null;
			}
		});

		globalTiersIndexer.sync();

		assertIndexData("Philippe Maillard", "", dateN1, ids.idHab1);
		assertIndexData("Giacomo Casanova", "", dateN3, ids.idHab3);
		assertIndexData("Philippe Maillard", "", dateN1, ids.idMc);
	}

	/**
	 * UNIREG-1619: Vérifie un ménage-commun dont tous les rapports sont annulés (seuls deux sont pris)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMenageCommunTroisRapportsAnnulesDifferentesDatesAnnulation() throws Exception {

		final RegDate dateN1 = RegDate.get(1956, 1, 21);
		final RegDate dateN2 = RegDate.get(1967, 12, 3);
		final RegDate dateN3 = RegDate.get(1960, 1, 1);
		final String noAVS1 = "123.45.678";
		final String noAVS2 = "987.65.432";
		final String noAVS3 = "111.11.113";

		class Ids {
			Long idHab1;
			Long idHab3;
			Long idHab2;
			Long idMc;
		}
		final Ids ids = new Ids();

		// Crée un ménage commun dont les rapports-entre-tiers sont annulés
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nhab1 = new PersonnePhysique(false);
				nhab1.setNom("Maillard");
				nhab1.setPrenom("Philippe");
				nhab1.setNumeroAssureSocial(noAVS1);
				nhab1.setDateNaissance(dateN1);
				nhab1.setSexe(Sexe.MASCULIN);
				nhab1 = (PersonnePhysique) dao.save(nhab1);
				ids.idHab1 = nhab1.getNumero();

				PersonnePhysique nhab2 = new PersonnePhysique(false);
				nhab2.setNom("Maillard-Gallet");
				nhab2.setPrenom("Gladys");
				nhab2.setNumeroAssureSocial(noAVS2);
				nhab2.setDateNaissance(dateN2);
				nhab2.setSexe(Sexe.FEMININ);
				nhab2 = (PersonnePhysique) dao.save(nhab2);
				ids.idHab2 = nhab2.getNumero();

				PersonnePhysique nhab3 = new PersonnePhysique(false);
				nhab3.setNom("Casanova");
				nhab3.setPrenom("Giacomo");
				nhab3.setNumeroAssureSocial(noAVS3);
				nhab3.setDateNaissance(dateN3);
				nhab3.setSexe(Sexe.MASCULIN);
				nhab3 = (PersonnePhysique) dao.save(nhab3);
				ids.idHab3 = nhab3.getNumero();

				MenageCommun mc = new MenageCommun();
				mc.setNumero(12345678L);
				mc = (MenageCommun) dao.save(mc);
				ids.idMc = mc.getNumero();

				final RapportEntreTiers rapport1 = tiersService.addTiersToCouple(mc, nhab1, RegDate.get(2001, 2, 23), null);
				final RapportEntreTiers rapport2 = tiersService.addTiersToCouple(mc, nhab2, RegDate.get(2001, 2, 23), null);
				final RapportEntreTiers rapport3 = tiersService.addTiersToCouple(mc, nhab3, RegDate.get(2000, 1, 1), null);

				// annulation des rapports dans un ordre précis : seuls les deux plus récents seront pris
				// en compte dans l'indexation
				rapport3.setAnnule(true);
				Thread.sleep(1000);
				rapport2.setAnnule(true);
				Thread.sleep(1000);
				rapport1.setAnnule(true);

				return null;
			}
		});

		globalTiersIndexer.sync();

		assertIndexData("Philippe Maillard", "", dateN1, ids.idHab1);
		assertIndexData("Gladys Maillard-Gallet", "", dateN2, ids.idHab2);
		assertIndexData("Giacomo Casanova", "", dateN3, ids.idHab3);
		assertIndexData("Philippe Maillard", "Gladys Maillard-Gallet", dateN1.index() + " " + dateN2.index(), ids.idMc);
	}

	/**
	 * UNIREG-1619: Vérifie un ménage-commun dont tous les rapports sont annulés (seuls deux sont pris)
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testMenageCommunTroisRapportsAnnulesMemesDatesAnnulation() throws Exception {

		final RegDate dateN1 = RegDate.get(1956, 1, 21);
		final RegDate dateN2 = RegDate.get(1967, 12, 3);
		final RegDate dateN3 = RegDate.get(1960, 1, 1);
		final String noAVS1 = "123.45.678";
		final String noAVS2 = "987.65.432";
		final String noAVS3 = "111.11.113";

		class Ids {
			Long idHab1;
			Long idHab3;
			Long idHab2;
			Long idMc;
		}
		final Ids ids = new Ids();

		// Crée un ménage commun dont les rapports-entre-tiers sont annulés
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PersonnePhysique nhab1 = new PersonnePhysique(false);
				nhab1.setNom("Maillard");
				nhab1.setPrenom("Philippe");
				nhab1.setNumeroAssureSocial(noAVS1);
				nhab1.setDateNaissance(dateN1);
				nhab1.setSexe(Sexe.MASCULIN);
				nhab1 = (PersonnePhysique) dao.save(nhab1);
				ids.idHab1 = nhab1.getNumero();

				PersonnePhysique nhab2 = new PersonnePhysique(false);
				nhab2.setNom("Maillard-Gallet");
				nhab2.setPrenom("Gladys");
				nhab2.setNumeroAssureSocial(noAVS2);
				nhab2.setDateNaissance(dateN2);
				nhab2.setSexe(Sexe.FEMININ);
				nhab2 = (PersonnePhysique) dao.save(nhab2);
				ids.idHab2 = nhab2.getNumero();

				PersonnePhysique nhab3 = new PersonnePhysique(false);
				nhab3.setNom("Casanova");
				nhab3.setPrenom("Giacomo");
				nhab3.setNumeroAssureSocial(noAVS3);
				nhab3.setDateNaissance(dateN3);
				nhab3.setSexe(Sexe.MASCULIN);
				nhab3 = (PersonnePhysique) dao.save(nhab3);
				ids.idHab3 = nhab3.getNumero();

				MenageCommun mc = new MenageCommun();
				mc.setNumero(12345678L);
				mc = (MenageCommun) dao.save(mc);
				ids.idMc = mc.getNumero();

				final RapportEntreTiers rapport1 = tiersService.addTiersToCouple(mc, nhab1, RegDate.get(2001, 2, 23), RegDate.get(2008, 1, 31));
				final RapportEntreTiers rapport2 = tiersService.addTiersToCouple(mc, nhab2, RegDate.get(2001, 2, 23), RegDate.get(2008, 12, 31));
				final RapportEntreTiers rapport3 = tiersService.addTiersToCouple(mc, nhab3, RegDate.get(2000, 1, 1), RegDate.get(2008, 12, 31));

				// annulation des rapports dans un ordre précis (tous au même moment) : c'est la date de fin du rapport
				// qui détermine l'ordre ou, à défaut, le log de création
				// en compte dans l'indexation
				final Date now = DateHelper.getCurrentDate();
				rapport1.setAnnulationDate(now);
				rapport2.setAnnulationDate(now);
				rapport3.setAnnulationDate(now);
				rapport1.setAnnulationUser("toto");
				rapport2.setAnnulationUser("toto");
				rapport3.setAnnulationUser("toto");

				return null;
			}
		});

		globalTiersIndexer.sync();

		assertIndexData("Philippe Maillard", "", dateN1, ids.idHab1);
		assertIndexData("Gladys Maillard-Gallet", "", dateN2, ids.idHab2);
		assertIndexData("Giacomo Casanova", "", dateN3, ids.idHab3);
		assertIndexData("Giacomo Casanova", "Gladys Maillard-Gallet", dateN3.index() + " " + dateN2.index(), ids.idMc);
	}

	private void assertIndexData(String nom1, String nom2, RegDate dateNaissance, long ctbId) {
		assertIndexData(nom1, nom2, IndexerFormatHelper.objectToString(dateNaissance), ctbId);
	}

	private void assertIndexData(String nom1, String nom2, String dateNaissance, long ctbId) {
		TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(ctbId);
		final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
		assertNotNull(resultats);
		assertEquals(1, resultats.size());

		final TiersIndexedData data = resultats.get(0);
		assertEquals(nom1, data.getNom1());
		assertEquals(nom2, data.getNom2());
		assertEquals(dateNaissance, data.getDateNaissance());
	}
}
