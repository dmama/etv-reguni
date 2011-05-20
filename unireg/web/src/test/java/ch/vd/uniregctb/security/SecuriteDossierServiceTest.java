package ch.vd.uniregctb.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeDroitAcces;

public class SecuriteDossierServiceTest extends SecurityTest {

	private SecuriteDossierService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(SecuriteDossierService.class, "securiteDossierService");
	}

	@Override
	protected void setAuthentication() {
		// on ne fait rien : on va gérer ça au cas par cas
	}

	@Test
	public void testGetAccesOperateurInconnu() throws Exception {

		setupDefaultTestOperateur();
		setAuthentication("inconnu");

		class Ids {
			Long jojo;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Contribuable jojo = addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.jojo = jojo.getId();
				return null;
			}
		});

		final Tiers jojo = (Tiers) hibernateTemplate.get(Tiers.class, ids.jojo);
		assertNull(service.getAcces(jojo)); // opérateur inconnu -> aucun accès
	}

	/**
	 * Cas simple : l'opérateur est connu et aucune autorisation ni restriction n'existe.
	 */
	@Test
	public void testGetAccesCasSimple() throws Exception {

		setupDefaultTestOperateur();
		setAuthentication(TEST_OP_NAME);

		class Ids {
			Long jojo;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				Contribuable jojo = addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.jojo = jojo.getId();
				return null;
			}
		});

		final Tiers jojo = (Tiers) hibernateTemplate.get(Tiers.class, ids.jojo);
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo));
	}

	/**
	 * L'opérateur est connu et il possède une restriction à l'écriture (= lecture seule)
	 */
	@Test
	public void testGetAccesRestrictionEcriture() throws Exception {

		setupDefaultTestOperateur();
		setAuthentication(TEST_OP_NAME);

		class Ids {
			Long jojo;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique jojo = addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.jojo = jojo.getId();

				addDroitAcces(TEST_OP_NO_IND, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				return null;
			}
		});

		final Tiers jojo = (Tiers) hibernateTemplate.get(Tiers.class, ids.jojo);
		assertEquals(Niveau.LECTURE, service.getAcces(jojo));
	}

	/**
	 * Teste les diverses restrictions possibles. Teste aussi que lorsqu'un opérateur possède une restriction sur un dossier, les autres
	 * opérateurs ne sont pas impactés.
	 */
	@Test
	public void testGetAccesRestrictions() throws Exception {

		setAuthentication(TEST_OP_NAME);

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(TEST_OP_NAME, TEST_OP_NO_IND, Role.VISU_ALL.getIfosecCode());
				addOperateur("op2", 2222, Role.VISU_ALL.getIfosecCode());
				addOperateur("op3", 3333, Role.VISU_ALL.getIfosecCode());
				addOperateur("op4", 4444, Role.VISU_ALL.getIfosecCode());
				addOperateur("op5", 5555, Role.VISU_ALL.getIfosecCode());
				addOperateur("op6", 6666, Role.VISU_ALL.getIfosecCode());
				addOperateur("op7", 7777, Role.VISU_ALL.getIfosecCode());
				addOperateur("op8", 8888, Role.VISU_ALL.getIfosecCode());
				addOperateur("op9", 9999, Role.VISU_ALL.getIfosecCode());
			}
		});

		class Ids {
			Long jojo;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique jojo = addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.jojo = jojo.getId();

				addDroitAcces(2222, jojo, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(3333, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(4444, jojo, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, RegDate.get(1980, 1, 1), RegDate.get(2112, 1, 1));
				addDroitAcces(5555, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(1980, 1, 1), RegDate.get(2112, 1, 1));
				addDroitAcces(6666, jojo, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, RegDate.get(1980, 1, 1), RegDate.get(1984, 1, 1));
				addDroitAcces(7777, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(1980, 1, 1), RegDate.get(1984, 1, 1));
				DroitAcces da = addDroitAcces(8888, jojo, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, RegDate.get(1980, 1, 1), null); // annulée
				da.setAnnule(true);
				da = addDroitAcces(9999, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(1980, 1, 1), null); // annulée
				da.setAnnule(true);
				return null;
			}
		});

		final Tiers jojo = (Tiers) hibernateTemplate.get(Tiers.class, ids.jojo);
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo)); // pas d'interdiction particulière

		resetAuthentication();
		setAuthentication("op2");
		assertNull(service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op3");
		assertEquals(Niveau.LECTURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op4");
		assertNull(service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op5");
		assertEquals(Niveau.LECTURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op6");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo)); // interdiction échue

		resetAuthentication();
		setAuthentication("op7");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo)); // interdiction échue

		resetAuthentication();
		setAuthentication("op8");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo)); // interdiction annulée

		resetAuthentication();
		setAuthentication("op9");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo)); // interdiction annulée
	}

	/**
	 * Teste que lorsqu'un opérateur possède une autorisation sur un dossier, les autres opérateurs n'ayant pas d'autorisation particulière
	 * ne doivent plus pouvoir accèder au dossier.
	 */
	@Test
	public void testGetAccesAutorisations() throws Exception {

		setAuthentication(TEST_OP_NAME);

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(TEST_OP_NAME, TEST_OP_NO_IND, Role.VISU_ALL.getIfosecCode());
				addOperateur("op2", 2222, Role.VISU_ALL.getIfosecCode());
				addOperateur("op3", 3333, Role.VISU_ALL.getIfosecCode());
				addOperateur("op4", 4444, Role.VISU_ALL.getIfosecCode());
				addOperateur("op5", 5555, Role.VISU_ALL.getIfosecCode());
				addOperateur("op6", 6666, Role.VISU_ALL.getIfosecCode());
			}
		});

		class Ids {
			Long jojo;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique jojo = addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.jojo = jojo.getId();

				addDroitAcces(TEST_OP_NO_IND, jojo, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(2222, jojo, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(3333, jojo, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(5555, jojo, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(1974, 1, 1), RegDate.get(1975, 1, 1)); // échue
				DroitAcces da = addDroitAcces(6666, jojo, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(1974, 1, 1), null); // annulée
				da.setAnnule(true);
				return null;
			}
		});

		final Tiers jojo = (Tiers) hibernateTemplate.get(Tiers.class, ids.jojo);
		assertEquals(Niveau.LECTURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op2");
		assertEquals(Niveau.LECTURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op3");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op4");
		assertNull(service.getAcces(jojo)); // pas d'autorisation particulière

		resetAuthentication();
		setAuthentication("op5");
		assertNull(service.getAcces(jojo)); // autorisation échue

		resetAuthentication();
		setAuthentication("op6");
		assertNull(service.getAcces(jojo)); // autorisation annulée
	}

	/**
	 * Teste que le mélange des restrictions et autorisations donne bien le résultat voulu.
	 */
	@Test
	public void testGetAccesRestrictionsEtAutorisations() throws Exception {

		setAuthentication(TEST_OP_NAME);

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(TEST_OP_NAME, TEST_OP_NO_IND, Role.VISU_ALL.getIfosecCode());
				addOperateur("op2", 2222, Role.VISU_ALL.getIfosecCode());
				addOperateur("op3", 3333, Role.VISU_ALL.getIfosecCode());
				addOperateur("op4", 4444, Role.VISU_ALL.getIfosecCode());
			}
		});

		class Ids {
			Long jojo;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique jojo = addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.jojo = jojo.getId();

				addDroitAcces(TEST_OP_NO_IND, jojo, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(2222, jojo, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(3333, jojo, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(4444, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				return null;
			}
		});

		final Tiers jojo = (Tiers) hibernateTemplate.get(Tiers.class, ids.jojo);
		assertEquals(Niveau.LECTURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op2");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op3");
		assertNull(service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("op4");
		assertNull(service.getAcces(jojo)); // l'interdiction de niveau ECRITURE est masquée par l'interdiction totale d'accès posée
		// implicitement par les autorisations des autres opérateurs
	}

	/**
	 * Teste que les membres de la direction ACI (= plus précisement les opérateurs avec les profiles IZPOUDP et IZPOUDM) peuvent toujours
	 * accéder aux dossiers.
	 */
	@Test
	public void testGetAccesBypassDirectionACI() throws Exception {

		setAuthentication(TEST_OP_NAME);

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(TEST_OP_NAME, TEST_OP_NO_IND, Role.VISU_ALL.getIfosecCode());
				addOperateur("op2", 2222, Role.VISU_ALL.getIfosecCode());
				addOperateur("op3", 3333, Role.VISU_ALL.getIfosecCode());
				addOperateur("op4", 4444, Role.VISU_ALL.getIfosecCode());
				addOperateur("dirAci1", 10000, Role.VISU_ALL.getIfosecCode(), Role.LECTURE_DOSSIER_PROTEGE.getIfosecCode());
				addOperateur("dirAci2", 10001, Role.VISU_ALL.getIfosecCode(), Role.LECTURE_DOSSIER_PROTEGE.getIfosecCode(),
						Role.ECRITURE_DOSSIER_PROTEGE.getIfosecCode());
				addOperateur("dirAci3", 10002, Role.VISU_ALL.getIfosecCode(), Role.LECTURE_DOSSIER_PROTEGE.getIfosecCode());
				addOperateur("dirAci4", 10003, Role.VISU_ALL.getIfosecCode(), Role.LECTURE_DOSSIER_PROTEGE.getIfosecCode(),
						Role.ECRITURE_DOSSIER_PROTEGE.getIfosecCode());
			}
		});

		class Ids {
			Long jojo;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique jojo = addNonHabitant("Jojo", "Leproux", RegDate.get(1954, 3, 31), Sexe.MASCULIN);
				ids.jojo = jojo.getId();

				addDroitAcces(TEST_OP_NO_IND, jojo, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(2222, jojo, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(3333, jojo, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(4444, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);

				// direction ACI
				addDroitAcces(10002, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(10003, jojo, TypeDroitAcces.INTERDICTION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				return null;
			}
		});

		final Tiers jojo = (Tiers) hibernateTemplate.get(Tiers.class, ids.jojo);

		resetAuthentication();
		setAuthentication("dirAci1");
		assertEquals(Niveau.LECTURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("dirAci2");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo));

		resetAuthentication();
		setAuthentication("dirAci3");
		assertEquals(Niveau.LECTURE, service.getAcces(jojo)); // l'interdiction spécifique est ignorée

		resetAuthentication();
		setAuthentication("dirAci4");
		assertEquals(Niveau.ECRITURE, service.getAcces(jojo)); // l'interdiction spécifique est ignorée
	}

	/**
	 * [UNIREG-962] Teste qu'un opérateur avec un autorisation LECTURE sur son compte en parallèle avec d'autres opérateurs avec
	 * autorisation ECRITURE possède bien un accès en lecture seul.
	 */
	@Test
	public void testGetAccesAutorisationLectureEtEcriture() throws Exception {

		setAuthentication("zaiamx");

		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur("zaiamx", 442451, Role.VISU_ALL.getIfosecCode()); // Anouchka Rossier
				addOperateur("zaiduj", 50824, Role.VISU_ALL.getIfosecCode()); // Jean-François Durgnat
				addOperateur("zaipwy", 52038, Role.VISU_ALL.getIfosecCode()); // Pierre Wicky
				addOperateur("zaiadu", 47017, Role.VISU_ALL.getIfosecCode()); // Jean-Claude Durgnat
				addOperateur("zaiyrz", 52771, Role.VISU_ALL.getIfosecCode()); // Yvon Rudaz
			}
		});

		class Ids {
			Long anouchka;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique anouchka = addNonHabitant("Anouchka", "Rossier", RegDate.get(1969, 10, 20), Sexe.FEMININ);
				ids.anouchka = anouchka.getId();

				addDroitAcces(442451, anouchka, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(50824, anouchka, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(52038, anouchka, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(47017, anouchka, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				addDroitAcces(52771, anouchka, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, RegDate.get(2000, 1, 1), null);
				return null;
			}
		});

		final Tiers anouchka = (Tiers) hibernateTemplate.get(Tiers.class, ids.anouchka);
		assertEquals(Niveau.LECTURE, service.getAcces(anouchka));

		resetAuthentication();
		setAuthentication("zaiduj");
		assertEquals(Niveau.ECRITURE, service.getAcces(anouchka));

		resetAuthentication();
		setAuthentication("zaipwy");
		assertEquals(Niveau.ECRITURE, service.getAcces(anouchka));

		resetAuthentication();
		setAuthentication("zaiadu");
		assertEquals(Niveau.ECRITURE, service.getAcces(anouchka));

		resetAuthentication();
		setAuthentication("zaiyrz");
		assertEquals(Niveau.ECRITURE, service.getAcces(anouchka));
	}
}
