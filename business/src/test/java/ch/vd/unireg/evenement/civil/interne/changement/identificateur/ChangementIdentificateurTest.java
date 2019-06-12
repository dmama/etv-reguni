package ch.vd.unireg.evenement.civil.interne.changement.identificateur;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.unireg.evenement.civil.interne.MessageCollector;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;

public class ChangementIdentificateurTest extends AbstractEvenementCivilInterneTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChangementIdentificateurTest.class);

	public ChangementIdentificateurTest() {
		setWantIndexationTiers(true);
	}

	@Test
	public void testHandleSurHabitant() throws Exception {

		final long noIndividu = 467844532L;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Popovitch", "Alexandre", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2009, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero();
		});

		// attente d'indexation
		globalTiersIndexer.sync();

		// Rech du tiers avant modif
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(ppId);
		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		Assert.assertEquals("Le tiers n'a pas été indexé", 1, list.size());
		TiersIndexedData tiers = list.get(0);
		Assert.assertEquals("Le numéro du tiers est incorrect", (Long) ppId, tiers.getNumero());

		// changement du NAVS13 dans le registre civil
		doModificationIndividu(noIndividu, individu -> {
			individu.setNouveauNoAVS("7561261400563");
			individu.setNomOfficielMere(new NomPrenom("Popova", "Célestine"));
			individu.setNomOfficielPere(new NomPrenom("Popov", "Martin"));
		});

		// traitement de l'événement civil
		doInNewTransactionAndSession(status -> {
			// déclenchement de l'événement
			final Individu individu = serviceCivil.getIndividu(noIndividu, null);
			final ChangementIdentificateur chgtIdentificateur = new ChangementIdentificateur(individu, null, RegDate.get(), 4848, context);

			final MessageCollector collector = buildMessageCollector();
			chgtIdentificateur.validate(collector, collector);// Valider la conformite sexe et numavs
			chgtIdentificateur.handle(collector);
			Assert.assertTrue("Une erreur est survenue lors du traitement du changement d'identificateur", collector.getErreurs().isEmpty());
			return null;
		});

		// on laisse un peu de temps pour que l'indexation soit faite
		globalTiersIndexer.sync();

		// on cherche de nouveau
		final List<TiersIndexedData> l = globalTiersSearcher.search(criteria);
		Assert.assertEquals("L'indexation n'a pas fonctionné", 1, l.size());
		LOGGER.debug("numero : " + l.get(0).getNumero());
		LOGGER.debug ("nom : " + l.get(0).getNom1());

		// on verifie que le changement a bien été effectué
		Assert.assertEquals("le nouveau NAVS13 n'a pas été indexé", "7561261400563", l.get(0).getNavs13_1());
	}

	@Test
	public void testHandleSurNonHabitant() throws Exception {

		final long noIndividu = 467844532L;

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Popovitch", "Alexandre", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, date(2009, 1, 1), date(2012, 4, 12));
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndividu);
			return pp.getNumero();
		});

		// attente d'indexation
		globalTiersIndexer.sync();

		// Rech du tiers avant modif
		final TiersCriteria criteria = new TiersCriteria();
		criteria.setNumero(ppId);
		final List<TiersIndexedData> list = globalTiersSearcher.search(criteria);
		Assert.assertEquals("Le tiers n'a pas été indexé", 1, list.size());
		TiersIndexedData tiers = list.get(0);
		Assert.assertEquals("Le numéro du tiers est incorrect", (Long) ppId, tiers.getNumero());

		// vérification que les données en base sont vides
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.isHabitantVD());
			Assert.assertNull(pp.getNumeroAssureSocial());
			Assert.assertNull(pp.getNomMere());
			Assert.assertNull(pp.getPrenomsMere());
			Assert.assertNull(pp.getNomPere());
			Assert.assertNull(pp.getPrenomsPere());
			return null;
		});

		doModificationIndividu(noIndividu, individu -> {
			individu.setNouveauNoAVS("7561261400563");
			individu.setNomOfficielMere(new NomPrenom("Popova", "Célestine"));
			individu.setNomOfficielPere(new NomPrenom("Popov", "Martin"));
		});

		// traitement de l'événement civil
		doInNewTransactionAndSession(status -> {
			// déclenchement de l'événement
			final Individu individu = serviceCivil.getIndividu(noIndividu, null);
			final ChangementIdentificateur chgtIdentificateur = new ChangementIdentificateur(individu, null, RegDate.get(), 4848, context);

			final MessageCollector collector = buildMessageCollector();
			chgtIdentificateur.validate(collector, collector);// Valider la conformite sexe et numavs
			chgtIdentificateur.handle(collector);
			Assert.assertTrue("Une erreur est survenue lors du traitement du changement d'identificateur", collector.getErreurs().isEmpty());
			return null;
		});

		// on laisse un peu de temps pour que l'indexation soit faite
		globalTiersIndexer.sync();

		// on cherche de nouveau
		final List<TiersIndexedData> l = globalTiersSearcher.search(criteria);
		Assert.assertEquals("L'indexation n'a pas fonctionné", 1, l.size());
		LOGGER.debug("numero : " + l.get(0).getNumero());
		LOGGER.debug ("nom : " + l.get(0).getNom1());

		// on verifie que le changement a bien été effectué
		Assert.assertEquals("le nouveau NAVS13 n'a pas été indexé", "7561261400563", l.get(0).getNavs13_1());

		// .. et en base aussi
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
			Assert.assertFalse(pp.isHabitantVD());
			Assert.assertEquals("7561261400563", pp.getNumeroAssureSocial());
			Assert.assertEquals("Popova", pp.getNomMere());
			Assert.assertEquals("Célestine", pp.getPrenomsMere());
			Assert.assertEquals("Popov", pp.getNomPere());
			Assert.assertEquals("Martin", pp.getPrenomsPere());
			return null;
		});
	}
}
