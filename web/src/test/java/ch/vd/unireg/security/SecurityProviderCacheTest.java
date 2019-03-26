package ch.vd.unireg.security;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeDroitAcces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Teste que le cache du service provider se comporte bien comme il faut.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class SecurityProviderCacheTest extends SecurityTest {

	private SecurityProviderCache cache;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		cache = getBean(SecurityProviderCache.class, "securityProviderCache");
		cache.reset();
	}

	/**
	 * [UNIREG-1191] Vérifie que la modification d'un droit d'accès met bien à jour le cache.
	 */
	@Test
	public void testModificationDroitAcces() throws Exception {

		setupDefaultTestOperateur();

		// Etat inital : une personne physique et opérateur sans aucune restriction

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique marcel = addNonHabitant("Marcel", "Bidon", date(1970, 4, 19), Sexe.MASCULIN);
				return marcel.getNumero();
			}
		});

		final Niveau accesInitial = cache.getDroitAcces(TEST_OP_NAME, id);
		assertEquals(Niveau.ECRITURE, accesInitial);

		// Ajout de la restriction

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique marcel = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, id);
				addDroitAcces(TEST_OP_NAME, marcel, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, date(1950, 1, 1), null);
				return null;
			}
		});

		// Etat final : l'opérateur est restreint de toute consultation sur la personne physique

		final Niveau accesFinal = cache.getDroitAcces(TEST_OP_NAME, id);
		assertNull(accesFinal);
	}

	/**
	 * [UNIREG-1191] Vérifie que l'ajout d'un droit d'accès sur composant d'un ménage met bien à jour le cache sur le ménage lui-même.
	 */
	@Test
	public void testModificationDroitAccesMenageCommun() throws Exception {

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur("X", 111, Role.VISU_ALL);
				addOperateur("Z", 333, Role.VISU_ALL);
			}
		});

		// Etat inital : un ménage commun composé de deux personnes physiques A et B; et deux opérateurs X et Z. L'opérateur X possède une
		// autorisation sur la personne physique A, et l'opérateur Z possède une autorisation sur la personne physique B. Conséquence =>
		// aucun des deux opérateurs ne doit pouvoir accéder au ménage commun.

		class Ids {
			long a;
			long b;
			long mc;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique a = addNonHabitant("A", "Bidon", date(1970, 4, 19), Sexe.MASCULIN);
				ids.a = a.getNumero();
				final PersonnePhysique b = addNonHabitant("B", "Bidon", date(1970, 4, 19), Sexe.FEMININ);
				ids.b = b.getNumero();
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(a, b, date(2000, 1, 1), null);
				ids.mc = ensemble.getMenage().getNumero();

				addDroitAcces("X", a, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(1990, 1, 1), null);
				addDroitAcces("Z", b, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(1990, 1, 1), null);
				return null;
			}
		});

		assertEquals(Niveau.ECRITURE, cache.getDroitAcces("X", ids.a));
		assertNull(cache.getDroitAcces("X", ids.b));
		assertNull(cache.getDroitAcces("X", ids.mc));

		assertNull(cache.getDroitAcces("Z", ids.a));
		assertEquals(Niveau.ECRITURE, cache.getDroitAcces("Z", ids.b));
		assertNull(cache.getDroitAcces("Z", ids.mc));

		// Ajout d'une autorisation en lecture de l'opérateur X sur la personne physique B => l'opérateur X doit pouvoir accéder en lecture
		// sur le couple aussi. Aucun changement pour l'opérateur Z.

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique a = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.b);
				addDroitAcces("X", a, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(1990, 1, 1), null);
				return null;
			}
		});

		assertEquals(Niveau.ECRITURE, cache.getDroitAcces("X", ids.a));
		assertEquals(Niveau.LECTURE, cache.getDroitAcces("X", ids.b));
		assertEquals(Niveau.LECTURE, cache.getDroitAcces("X", ids.mc));

		assertNull(cache.getDroitAcces("Z", ids.a));
		assertEquals(Niveau.ECRITURE, cache.getDroitAcces("Z", ids.b));
		assertNull(cache.getDroitAcces("Z", ids.mc));
	}

	@Test
	public void testGetDroitAccessWithNullId() throws Exception {

		class Ids {
			long a;
			long b;
			long mc;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique a = addNonHabitant("A", "Bidon", date(1970, 4, 19), Sexe.MASCULIN);
				ids.a = a.getNumero();
				final PersonnePhysique b = addNonHabitant("B", "Bidon", date(1970, 4, 19), Sexe.FEMININ);
				ids.b = b.getNumero();
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(a, b, date(2000, 1, 1), null);
				ids.mc = ensemble.getMenage().getNumero();

				return null;
			}
		});

		final List<Niveau> acces = cache.getDroitsAcces("broubrou", Arrays.asList(ids.a, ids.b, null));
		assertNotNull(acces);
		assertEquals(3, acces.size());
		assertEquals(Niveau.ECRITURE, acces.get(0));
		assertEquals(Niveau.ECRITURE, acces.get(1));
		assertNull(acces.get(2)); // id null -> accès null
	}

	/**
	 * [SIFISC-9341] après son mariage, le ménage commun d'une collaboratrice de l'ACI n'était pas protégé tant que le cache du security-provider n'a pas été nettoyé...
	 */
	@Test
	public void testCreationMenageSurPersonnePhysiqueProtegee() throws Exception {

		final long noIndProtege = 37854L;
		final long noIndNonProtege = 48745672L;
		final RegDate dateMariage = RegDate.get().addDays(-5);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceProt = date(1990, 1, 1);
				final RegDate dateNaissanceNonProt = date(1991, 5, 4);

				final MockIndividu prot = addIndividu(noIndProtege, dateNaissanceProt, "Huile", "Grosse", Sexe.MASCULIN);
				final MockIndividu nonProt = addIndividu(noIndNonProtege, dateNaissanceNonProt, "Huile", "Epouse", Sexe.FEMININ);
				addNationalite(prot, MockPays.Suisse, dateNaissanceProt, null);
				addNationalite(nonProt, MockPays.Suisse, dateNaissanceNonProt, null);
				marieIndividus(prot, nonProt, dateMariage);
			}
		});

		// mise en place fiscale avant mariage avec ajout d'un droit d'accès sur le contribuable protégé
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique prot = addHabitant(noIndProtege);
				addForPrincipal(prot, date(2012, 5, 12), MotifFor.INDETERMINE, MockCommune.Lausanne);
				addDroitAcces("zai1", prot, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(2013, 1, 1), null);

				final PersonnePhysique nonProt = addHabitant(noIndNonProtege);
				addForPrincipal(nonProt, date(2012, 3, 1), MotifFor.INDETERMINE, MockCommune.Bex);

				return prot.getNumero();
			}
		});

		// vérification que le contribuable est bien protégé
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Niveau droits = cache.getDroitAcces("TOTO", ppId);
				assertNull(droits);
				return null;
			}
		});

		// création du ménage commun
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique prot = (PersonnePhysique) tiersDAO.get(ppId);
				final PersonnePhysique nonProt = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndNonProtege);
				assertNotNull(prot);
				assertNotNull(nonProt);

				final ForFiscalPrincipal ffpProt = prot.getDernierForFiscalPrincipal();
				ffpProt.setDateFin(dateMariage.getOneDayBefore());
				ffpProt.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

				final ForFiscalPrincipal ffpNonProt = nonProt.getDernierForFiscalPrincipal();
				ffpNonProt.setDateFin(dateMariage.getOneDayBefore());
				ffpNonProt.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(prot, nonProt, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				return mc.getNumero();
			}
		});

		// vérification que le contribuable ménage est bien protégé également
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final Niveau droits = cache.getDroitAcces("TOTO", mcId);
				assertNull(droits);
				return null;
			}
		});

	}

	/**
	 * [FISCPROJ-1177] Ce test vérifie que le cache du security-provider est bien mis-à-jour après la création d'un nouvel établissement et son rattachement à une entreprise protégée : l'établissement doit lui-même être protégé.
	 */
	@Test
	public void testCreationEtablissementSurEntrepriseProtegee() throws Exception {

		final RegDate dateFondation = RegDate.get(2000, 1, 1);

		class Ids {
			Long entreprise;
			Long etablissementPrincipal;
			Long etablissementSecondaire;
		}
		final Ids ids = new Ids();

		// on crée une entreprise et son établissement principal
		doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = addEntrepriseInconnueAuCivil("Ma petite entreprise", dateFondation);
			final Etablissement etablissementPrincipal = addEtablissement();
			addActiviteEconomique(entreprise, etablissementPrincipal, dateFondation, null, true);

			// on restreint l'accès sur l'entreprise
			addDroitAcces("zai1", entreprise, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(2013, 1, 1), null);

			ids.entreprise = entreprise.getNumero();
			ids.etablissementPrincipal = etablissementPrincipal.getNumero();
			return null;
		});

		// on vérifie que l'entreprise et l'établissement principal sont bien protégés
		doInNewTransactionAndSession(status -> {
			assertNull(cache.getDroitAcces("TOTO", ids.entreprise));
			assertNull(cache.getDroitAcces("TOTO", ids.etablissementPrincipal));
			return null;
		});

		// on crée un nouvel établissement secondaire
		doInNewTransactionAndSession(status -> {

			final Entreprise entreprise = (Entreprise) tiersDAO.get(ids.entreprise);
			final Etablissement etablissementSecondaire = addEtablissement();
			addActiviteEconomique(entreprise, etablissementSecondaire, dateFondation, null, false);

			ids.etablissementSecondaire = etablissementSecondaire.getNumero();
			return null;
		});

		// on vérifie que l'établissement secondaire est également protégé
		doInNewTransactionAndSession(status -> {
			assertNull(cache.getDroitAcces("TOTO", ids.etablissementSecondaire));
			return null;
		});
	}
}
