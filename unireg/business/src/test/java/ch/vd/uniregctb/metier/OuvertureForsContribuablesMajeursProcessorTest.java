package ch.vd.uniregctb.metier;

import ch.vd.uniregctb.interfaces.model.Nationalite;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.metier.OuvertureForsResults.Erreur;
import ch.vd.uniregctb.metier.OuvertureForsResults.Traite;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.Tiers.ForsParType;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;

public class OuvertureForsContribuablesMajeursProcessorTest extends BusinessTest {

	private OuvertureForsContribuablesMajeursProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		final GlobalTiersSearcher searcher = getBean(GlobalTiersSearcher.class, "globalTiersSearcher");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new OuvertureForsContribuablesMajeursProcessor(transactionManager, hibernateTemplate, tiersService, adresseService,
				serviceInfra, searcher);
	}

	/**
	 * Vérifie que le batch n'ouvre pas de for principal sur un habitant suisse qui n'est pas encore majeur.
	 */
	@Test
	public void testTraiteHabitantSuisseNonMajeur() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(2000, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null, 1);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement);
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);
		assertEmpty(rapport.habitantTraites);

		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.principaux);
		assertEmpty(fors.secondaires);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
	}

	/**
	 * Vérifie que le batch ouvre bien un for principal ordinaire sur un habitant suisse majeur qui n'en possède pas.
	 */
	@Test
	public void testTraiteHabitantSuisseMajeurSansFor() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null, 1);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement);
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);

		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());

		final Traite traite = traites.get(0);
		assertEquals(h.getNumero().longValue(), traite.noCtb);
		assertEquals(ModeImposition.ORDINAIRE, traite.modeImposition);

		// Vérification qu'un for a bien été ouvert sur le contribuable
		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
		assertEmpty(fors.secondaires);

		final List<ForFiscalPrincipal> principaux = fors.principaux;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipal fp = principaux.get(0);
		assertNotNull(fp);
		assertEquals(dateNaissance.addYears(18), fp.getDateDebut());
		assertNull(fp.getDateFin());
		assertEquals(ModeImposition.ORDINAIRE, fp.getModeImposition());
		assertEquals(MotifRattachement.DOMICILE, fp.getMotifRattachement());
		assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), fp.getNumeroOfsAutoriteFiscale().intValue());
	}

	/**
	 * Vérifie que le batch ouvre bien un for principal ordinaire sur un habitant suisse majeur qui n'en possède pas.
	 */
	@Test
	public void testTraiteHabitantEtrangerMajeurSansFor() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Tetram", "Ducik", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateNaissance, null);
				addNationalite(individu, MockPays.Albanie, dateNaissance, null, 1);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement);
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantEnErrors);

		final List<Traite> traites = rapport.habitantTraites;
		assertEquals(1, traites.size());

		final Traite traite = traites.get(0);
		assertEquals(h.getNumero().longValue(), traite.noCtb);
		assertEquals(ModeImposition.SOURCE, traite.modeImposition);

		// Vérification qu'un for a bien été ouvert sur le contribuable
		final ForsParType fors = h.getForsParType(true);
		assertNotNull(fors);
		assertEmpty(fors.autreElementImpot);
		assertEmpty(fors.dpis);
		assertEmpty(fors.secondaires);

		final List<ForFiscalPrincipal> principaux = fors.principaux;
		assertNotNull(principaux);
		assertEquals(1, principaux.size());

		final ForFiscalPrincipal fp = principaux.get(0);
		assertNotNull(fp);
		assertEquals(dateNaissance.addYears(18), fp.getDateDebut());
		assertNull(fp.getDateFin());
		assertEquals(MotifRattachement.DOMICILE, fp.getMotifRattachement());
		assertEquals(ModeImposition.SOURCE, fp.getModeImposition());
		assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), fp.getNumeroOfsAutoriteFiscale().intValue());
	}

	/**
	 * Vérifie que le batch n'ouvre pas de for supplémentaire sur un habitant majeur qui en possède déjà.
	 * <p>
	 * Note: dans le cas réel de l'exécution du batch, le contribuable ci-dessous aurait été exclu du traitement au niveau de la requête
	 * SQL. Il est donc normal que le traitement provoque une erreur.
	 */
	@Test
	public void testTraiteHabitantSuisseMajeurAvecForPreexistant() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null, 1);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);
		addForPrincipal(h, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Lausanne);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement);
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(1, erreurs.size());

		final Erreur e = erreurs.get(0);
		assertNotNull(e);
		// le for existe déjà (voir remarque dans javadoc du test)
		assertEquals(OuvertureForsResults.ErreurType.INCOHERENCE_FOR_FISCAUX, e.raison);
	}

	/**
	 * Vérifie que le batch génère une erreur lorsqu'un habitant ne possède pas d'adresse de domicile (les valeurs par défaut ne doivent pas
	 * être utilisées).
	 */
	@Test
	public void testTraiteHabitantSuisseMajeurSansAdresseDomicile() {

		final int noIndividu = 1234;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Duschmole", "Jean", true);
				// adresse courrier seulement -> l'adresse de domicile sera une adresse par défaut
				addAdresse(individu, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne,
						dateNaissance, null);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null, 1);
			}
		});

		final PersonnePhysique h = addHabitant(noIndividu);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement);
		processor.rapport = rapport;
		processor.traiteHabitant(h.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(1, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(1, erreurs.size());

		final Erreur e = erreurs.get(0);
		assertNotNull(e);
		assertEquals(OuvertureForsResults.ErreurType.DOMICILE_INCONNU, e.raison);
	}

	/**
	 * [UNIREG-1588] Vérifie que le batch génère une erreur lorsqu'un habitant ne possède pas de nationalité.
	 */
	@Test
	public void testTraiteHabitantSuisseMajeurSansNationalite() {

		final int noIndividu1 = 1234;
		final int noIndividu2 = 4321;
		final RegDate dateTraitement = date(2009, 1, 1);
		final RegDate dateNaissance = date(1990, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// un individu sans nationalité
				MockIndividu individu1 = addIndividu(noIndividu1, dateNaissance, "Duschmole", "Jean", true);
				addAdresse(individu1, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, MockLocalite.Lausanne, dateNaissance, null);
				individu1.setNationalites(null);

				// un autre individu sans nationalité (variante)
				MockIndividu individu2 = addIndividu(noIndividu2, dateNaissance, "Schmoledu", "Jean", true);
				addAdresse(individu2, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, MockLocalite.Lausanne, dateNaissance, null);
				individu2.setNationalites(Collections.<Nationalite>emptyList());
			}
		});

		final PersonnePhysique h1 = addHabitant(noIndividu1);
		final PersonnePhysique h2 = addHabitant(noIndividu2);

		// Lancement du batch
		final OuvertureForsResults rapport = new OuvertureForsResults(dateTraitement);
		processor.rapport = rapport;
		processor.traiteHabitant(h1.getNumero(), dateTraitement);
		processor.traiteHabitant(h2.getNumero(), dateTraitement);

		// Vérification du rapport
		assertEquals(2, rapport.nbHabitantsTotal);
		assertEmpty(rapport.habitantTraites);
		final List<Erreur> erreurs = rapport.habitantEnErrors;
		assertEquals(2, erreurs.size());

		final Erreur e1 = erreurs.get(0);
		assertNotNull(e1);
		assertEquals(OuvertureForsResults.ErreurType.CIVIL_EXCEPTION, e1.raison);
		assertEquals("ch.vd.uniregctb.tiers.TiersException: Impossible de déterminer la nationalité de l'individu n°" + noIndividu1, e1.details);

		final Erreur e2 = erreurs.get(1);
		assertNotNull(e2);
		assertEquals(OuvertureForsResults.ErreurType.CIVIL_EXCEPTION, e2.raison);
		assertEquals("ch.vd.uniregctb.tiers.TiersException: Impossible de déterminer la nationalité de l'individu n°" + noIndividu2, e2.details);
	}
}
