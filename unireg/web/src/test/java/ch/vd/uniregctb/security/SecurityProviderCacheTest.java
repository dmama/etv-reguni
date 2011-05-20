package ch.vd.uniregctb.security;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeDroitAcces;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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
				addDroitAcces(TEST_OP_NO_IND, marcel, TypeDroitAcces.INTERDICTION, Niveau.LECTURE, date(1950, 1, 1), null);
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
				addOperateur("X", 111, Role.VISU_ALL.getIfosecCode());
				addOperateur("Z", 333, Role.VISU_ALL.getIfosecCode());
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

				addDroitAcces(111, a, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(1990, 1, 1), null);
				addDroitAcces(333, b, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(1990, 1, 1), null);
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
				addDroitAcces(111, a, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(1990, 1, 1), null);
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

		final List<Niveau> acces = cache.getDroitAcces("broubrou", Arrays.asList(ids.a, ids.b, null));
		assertNotNull(acces);
		assertEquals(3, acces.size());
		assertEquals(Niveau.ECRITURE, acces.get(0));
		assertEquals(Niveau.ECRITURE, acces.get(1));
		assertNull(acces.get(2)); // id null -> accès null
	}
}
