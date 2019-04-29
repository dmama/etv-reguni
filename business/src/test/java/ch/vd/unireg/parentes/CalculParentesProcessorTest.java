package ch.vd.unireg.parentes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.common.MultipleSwitch;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportEntreTiersDAO;
import ch.vd.unireg.type.Sexe;

public class CalculParentesProcessorTest extends BusinessTest {

	private CalculParentesProcessor processor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final RapportEntreTiersDAO rapportDAO = getBean(RapportEntreTiersDAO.class, "rapportEntreTiersDAO");

		// contrairement au vrai code, je ne coupe pas ici le validationInterceptor pour que si quelque chose
		// pête à ce niveau, on le voit au moins ici...
		final MultipleSwitch interceptorSwitch = new MultipleSwitch(parentesSynchronizer, tacheSynchronizer);
		processor = new CalculParentesProcessor(rapportDAO, tiersDAO, transactionManager, interceptorSwitch, tiersService);
	}

	@Test
	public void testAucunConnuAuCivil() throws Exception {
		serviceCivil.setUp(new DefaultMockServiceCivil());

		// quelques non-habitants quand-même
		doInNewTransactionAndSession(status -> {
			addNonHabitant("Pierre", "Kiroul", null, Sexe.MASCULIN);
			addNonHabitant("Namass", "Pamouss", null, Sexe.FEMININ);
			return null;
		});

		final CalculParentesResults results = processor.run(1, CalculParentesMode.FULL, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getUpdates());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getUpdates().size());
	}

	@Test
	public void testConnusAuCivilMaisNonHabitants() throws Exception {

		final long noIndividuParent = 4387436L;
		final long noIndividuEnfant = 4398243L;
		final RegDate dateNaissanceEnfant = date(2005, 5, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent = addIndividu(noIndividuParent, null, "Malfoy", "Lucius", Sexe.MASCULIN);
				final MockIndividu enfant = addIndividu(noIndividuEnfant, dateNaissanceEnfant, "Malfoy", "Draco", Sexe.MASCULIN);
				addLiensFiliation(enfant, parent, null, dateNaissanceEnfant, null);
			}
		});

		// les non-habitants basés sur les individus connus
		doInNewTransactionAndSession(status -> {
			tiersService.createNonHabitantFromIndividu(noIndividuEnfant);
			tiersService.createNonHabitantFromIndividu(noIndividuParent);
			addNonHabitant("Pierre", "Kiroul", null, Sexe.MASCULIN);
			addNonHabitant("Namass", "Pamouss", null, Sexe.FEMININ);
			return null;
		});

		final CalculParentesResults results = processor.run(1, CalculParentesMode.FULL, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getUpdates());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getUpdates().size());
	}

	@Test
	public void testDestructionParentesPresentesModeFull() throws Exception {
		serviceCivil.setUp(new DefaultMockServiceCivil());

		final class Ids {
			long idParent;
			long idEnfant;
			long isParente;
		}

		// quelques non-habitants quand-même (avec une relation de filiation entre eux...)
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pierre = addNonHabitant("Pierre", "Kiroul", null, Sexe.MASCULIN);
			final PersonnePhysique namass = addNonHabitant("Namass", "Pamouss", null, Sexe.FEMININ);
			final Parente parente = addParente(namass, pierre, date(2010, 6, 23), null);
			final Ids ids1 = new Ids();
			ids1.idParent = pierre.getNumero();
			ids1.idEnfant = namass.getNumero();
			ids1.isParente = parente.getId();
			return ids1;
		});

		// on vérifie que la relation de filiation est bien là...
		doInNewTransactionAndSession(status -> {
			{
				final PersonnePhysique parent = (PersonnePhysique) tiersDAO.get(ids.idParent);
				final Set<RapportEntreTiers> sujetRapports = parent.getRapportsSujet();
				Assert.assertEquals(0, sujetRapports.size());
				final Set<RapportEntreTiers> objectRapports = parent.getRapportsObjet();
				Assert.assertEquals(1, objectRapports.size());

				final RapportEntreTiers rapport = objectRapports.iterator().next();
				Assert.assertNotNull(rapport);
				Assert.assertEquals(Parente.class, rapport.getClass());
				Assert.assertEquals((Long) ids.isParente, rapport.getId());
			}
			{
				final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
				final Set<RapportEntreTiers> sujetRapports = enfant.getRapportsSujet();
				Assert.assertEquals(1, sujetRapports.size());
				final Set<RapportEntreTiers> objectRapports = enfant.getRapportsObjet();
				Assert.assertEquals(0, objectRapports.size());

				final RapportEntreTiers rapport = sujetRapports.iterator().next();
				Assert.assertNotNull(rapport);
				Assert.assertEquals(Parente.class, rapport.getClass());
				Assert.assertEquals((Long) ids.isParente, rapport.getId());
			}
			return null;
		});

		final CalculParentesResults results = processor.run(1, CalculParentesMode.FULL, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getUpdates());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getUpdates().size());

		// on vérifie que la relation de parenté a bien été enlevée
		doInNewTransactionAndSession(status -> {
			{
				final PersonnePhysique parent = (PersonnePhysique) tiersDAO.get(ids.idParent);
				final Set<RapportEntreTiers> objetRapports = parent.getRapportsObjet();
				Assert.assertEquals(0, objetRapports.size());
				final Set<RapportEntreTiers> sujetRapports = parent.getRapportsSujet();
				Assert.assertEquals(0, sujetRapports.size());
			}
			{
				final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
				final Set<RapportEntreTiers> objetRapports = enfant.getRapportsObjet();
				Assert.assertEquals(0, objetRapports.size());
				final Set<RapportEntreTiers> sujetRapports = enfant.getRapportsSujet();
				Assert.assertEquals(0, sujetRapports.size());
			}
			{
				final Parente parente = hibernateTemplate.get(Parente.class, ids.isParente);
				Assert.assertNull(parente);
			}
			return null;
		});
	}

	@Test
	public void testFamille() throws Exception {

		final long noIndPapa = 321634246L;
		final long noIndMaman = 326746L;
		final long noIndBelle = 4784523L;
		final long noIndDur = 423782967L;
		final long noIndHenri = 4378935623L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papa = addIndividu(noIndPapa, date(1982, 6, 30), "Barba", "Papa", Sexe.MASCULIN);
				final MockIndividu maman = addIndividu(noIndMaman, date(1982, 3, 12), "Barba", "Mamma", Sexe.FEMININ);
				final MockIndividu belle = addIndividu(noIndBelle, date(2005, 12, 22), "Barba", "Belle", Sexe.FEMININ);
				final MockIndividu dur = addIndividu(noIndDur, date(2005, 12, 21), "Barbi", "Dur", Sexe.MASCULIN);
				final MockIndividu henri = addIndividu(noIndHenri, date(1990, 2, 21), "Cantaunet", "Henri", Sexe.MASCULIN);

				addLienVersParent(belle, papa, date(2005, 12, 22), null);
				addLienVersParent(belle, maman, date(2005, 12, 22), null);
				addLienVersParent(dur, papa, date(2005, 12, 21), null);
				addLienVersParent(dur, maman, date(2005, 12, 21), null);
			}
		});

		final class Ids {
			long idPapa;
			long idMaman;
			long idBelle;
			long idDur;
			long idHenri;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique papa = addHabitant(noIndPapa);
			final PersonnePhysique maman = addHabitant(noIndMaman);
			final PersonnePhysique belle = addHabitant(noIndBelle);
			final PersonnePhysique dur = addHabitant(noIndDur);
			final PersonnePhysique henri = addHabitant(noIndHenri);

			final Ids ids1 = new Ids();
			ids1.idPapa = papa.getNumero();
			ids1.idMaman = maman.getNumero();
			ids1.idBelle = belle.getNumero();
			ids1.idDur = dur.getNumero();
			ids1.idHenri = henri.getNumero();
			return ids1;
		});

		// génération des relations
		final CalculParentesResults results = processor.run(1, CalculParentesMode.FULL, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getUpdates());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(4, results.getUpdates().size());

		final List<ParenteUpdateInfo> updates = results.getUpdates();

		// vérification du contenu du rapport d'exécution
		{
			final ParenteUpdateInfo update = updates.get(0);
			Assert.assertNotNull(update);
			Assert.assertEquals(ids.idPapa, update.noCtbParent);
			Assert.assertEquals(ids.idBelle, update.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 22), update.dateDebut);
			Assert.assertNull(update.dateFin);
		}
		{
			final ParenteUpdateInfo update = updates.get(1);
			Assert.assertNotNull(update);
			Assert.assertEquals(ids.idPapa, update.noCtbParent);
			Assert.assertEquals(ids.idDur, update.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 21), update.dateDebut);
			Assert.assertNull(update.dateFin);
		}
		{
			final ParenteUpdateInfo update = updates.get(2);
			Assert.assertNotNull(update);
			Assert.assertEquals(ids.idMaman, update.noCtbParent);
			Assert.assertEquals(ids.idBelle, update.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 22), update.dateDebut);
			Assert.assertNull(update.dateFin);
		}
		{
			final ParenteUpdateInfo update = updates.get(3);
			Assert.assertNotNull(update);
			Assert.assertEquals(ids.idMaman, update.noCtbParent);
			Assert.assertEquals(ids.idDur, update.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 21), update.dateDebut);
			Assert.assertNull(update.dateFin);
		}

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final List<Parente> parentes = hibernateTemplate.find("from Parente", FlushMode.AUTO);
			Assert.assertNotNull(parentes);
			Assert.assertEquals(4, parentes.size());
			final List<Parente> copyToSort = new ArrayList<>(parentes);
			Collections.sort(copyToSort, new Comparator<Parente>() {
				@Override
				public int compare(Parente o1, Parente o2) {
					int comp = Long.compare(o1.getObjetId(), o2.getObjetId());
					if (comp == 0) {
						comp = Long.compare(o1.getSujetId(), o2.getSujetId());
					}
					return comp;
				}
			});

			{
				final Parente parente = copyToSort.get(0);
				Assert.assertNotNull(parente);
				Assert.assertFalse(parente.isAnnule());
				Assert.assertEquals((Long) ids.idPapa, parente.getObjetId());
				Assert.assertEquals((Long) ids.idBelle, parente.getSujetId());
				Assert.assertEquals(date(2005, 12, 22), parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
			}
			{
				final Parente parente = copyToSort.get(1);
				Assert.assertNotNull(parente);
				Assert.assertFalse(parente.isAnnule());
				Assert.assertEquals((Long) ids.idPapa, parente.getObjetId());
				Assert.assertEquals((Long) ids.idDur, parente.getSujetId());
				Assert.assertEquals(date(2005, 12, 21), parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
			}
			{
				final Parente parente = copyToSort.get(2);
				Assert.assertNotNull(parente);
				Assert.assertFalse(parente.isAnnule());
				Assert.assertEquals((Long) ids.idMaman, parente.getObjetId());
				Assert.assertEquals((Long) ids.idBelle, parente.getSujetId());
				Assert.assertEquals(date(2005, 12, 22), parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
			}
			{
				final Parente parente = copyToSort.get(3);
				Assert.assertNotNull(parente);
				Assert.assertFalse(parente.isAnnule());
				Assert.assertEquals((Long) ids.idMaman, parente.getObjetId());
				Assert.assertEquals((Long) ids.idDur, parente.getSujetId());
				Assert.assertEquals(date(2005, 12, 21), parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
			}
			return null;
		});
	}

	@Test
	public void testRefreshAll() throws Exception {

		final long noIndParent1 = 4367345L;
		final long noIndEnfant1 = 543735L;
		final long noIndParent2 = 43425423L;
		final long noIndEnfant2 = 37834956L;
		final RegDate dateNaissanceEnfant1 = date(2000, 5, 31);
		final RegDate dateNaissanceEnfant2 = date(2002, 1, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent1 = addIndividu(noIndParent1, null, "Bleu", "Papa", Sexe.MASCULIN);
				final MockIndividu enfant1 = addIndividu(noIndEnfant1, dateNaissanceEnfant1, "Bleu", "Puce", Sexe.FEMININ);
				addLiensFiliation(enfant1, parent1, null, dateNaissanceEnfant1, null);

				// pas de filiation ici -> si on rafraîchit et qu'elle existe au civil, elle doit être annulée
				addIndividu(noIndParent2, null, "Rouge", "Maman", Sexe.FEMININ);
				addIndividu(noIndEnfant2, dateNaissanceEnfant2, "Rouge", "Fiston", Sexe.MASCULIN);
			}
		});

		final class Ids {
			long ppParent1;
			long ppParent2;
			long ppEnfant1;
			long ppEnfant2;
		}

		// mise en place fiscale : tout le contraire de ce qui devrait exister
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, false, status -> {
			final PersonnePhysique parent1 = addHabitant(noIndParent1);
			final PersonnePhysique enfant1 = addHabitant(noIndEnfant1);
			final PersonnePhysique parent2 = addHabitant(noIndParent2);
			final PersonnePhysique enfant2 = addHabitant(noIndEnfant2);
			addParente(enfant2, parent2, dateNaissanceEnfant2, null);
			final Ids ids1 = new Ids();
			ids1.ppParent1 = parent1.getNumero();
			ids1.ppEnfant1 = enfant1.getNumero();
			ids1.ppParent2 = parent2.getNumero();
			ids1.ppEnfant2 = enfant2.getNumero();
			return ids1;
		});

		final CalculParentesResults result = processor.run(1, CalculParentesMode.REFRESH_ALL, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getErreurs().size());
		Assert.assertEquals(2, result.getUpdates().size());

		final List<ParenteUpdateInfo> updates = result.getUpdates();
		{
			final ParenteUpdateInfo update = updates.get(0);
			Assert.assertNotNull(update);
			Assert.assertEquals(ids.ppParent1, update.noCtbParent);
			Assert.assertEquals(ids.ppEnfant1, update.noCtbEnfant);
			Assert.assertEquals(dateNaissanceEnfant1, update.dateDebut);
			Assert.assertNull(update.dateFin);
			Assert.assertEquals(ParenteUpdateInfo.Action.CREATION, update.action);
		}
		{
			final ParenteUpdateInfo update = updates.get(1);
			Assert.assertNotNull(update);
			Assert.assertEquals(ids.ppParent2, update.noCtbParent);
			Assert.assertEquals(ids.ppEnfant2, update.noCtbEnfant);
			Assert.assertEquals(dateNaissanceEnfant2, update.dateDebut);
			Assert.assertNull(update.dateFin);
			Assert.assertEquals(ParenteUpdateInfo.Action.ANNULATION, update.action);
		}

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final List<Parente> parentes = hibernateTemplate.find("from Parente", FlushMode.AUTO);
			Assert.assertNotNull(parentes);
			Assert.assertEquals(2, parentes.size());
			final List<Parente> copyToSort = new ArrayList<>(parentes);
			Collections.sort(copyToSort, new Comparator<Parente>() {
				@Override
				public int compare(Parente o1, Parente o2) {
					int comp = Long.compare(o1.getObjetId(), o2.getObjetId());
					if (comp == 0) {
						comp = Long.compare(o1.getSujetId(), o2.getSujetId());
					}
					return comp;
				}
			});

			{
				final Parente parente = copyToSort.get(0);
				Assert.assertNotNull(parente);
				Assert.assertEquals((Long) ids.ppParent1, parente.getObjetId());
				Assert.assertEquals((Long) ids.ppEnfant1, parente.getSujetId());
				Assert.assertEquals(dateNaissanceEnfant1, parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
				Assert.assertFalse(parente.isAnnule());
			}
			{
				final Parente parente = copyToSort.get(1);
				Assert.assertNotNull(parente);
				Assert.assertEquals((Long) ids.ppParent2, parente.getObjetId());
				Assert.assertEquals((Long) ids.ppEnfant2, parente.getSujetId());
				Assert.assertEquals(dateNaissanceEnfant2, parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
				Assert.assertTrue(parente.isAnnule());
			}
			return null;
		});
	}

	@Test
	public void testRefreshDirty() throws Exception {

		final long noIndParent1 = 4367345L;
		final long noIndEnfant1 = 543735L;
		final long noIndParent2 = 43425423L;
		final long noIndEnfant2 = 37834956L;
		final RegDate dateNaissanceEnfant1 = date(2000, 5, 31);
		final RegDate dateNaissanceEnfant2 = date(2002, 1, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent1 = addIndividu(noIndParent1, null, "Bleu", "Papa", Sexe.MASCULIN);
				final MockIndividu enfant1 = addIndividu(noIndEnfant1, dateNaissanceEnfant1, "Bleu", "Puce", Sexe.FEMININ);
				addLiensFiliation(enfant1, parent1, null, dateNaissanceEnfant1, null);

				// pas de filiation ici -> si on rafraîchit et qu'elle existe au civil, elle doit être annulée
				addIndividu(noIndParent2, null, "Rouge", "Maman", Sexe.FEMININ);
				addIndividu(noIndEnfant2, dateNaissanceEnfant2, "Rouge", "Fiston", Sexe.MASCULIN);
			}
		});

		// on crée juste l'enfant1 en tant que contribuable, sans son parent -> dirty
		final long ppEnfant1 = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique enfant1 = addHabitant(noIndEnfant1);
			return enfant1.getNumero();
		});

		// vérifie le flag "dirty"
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique enfant1 = (PersonnePhysique) tiersDAO.get(ppEnfant1);
			Assert.assertNotNull(enfant1);
			Assert.assertTrue(enfant1.isParenteDirty());
			return null;
		});

		final class Ids {
			long ppParent1;
			long ppParent2;
			long ppEnfant2;
		}

		// mise en place fiscale : tout le contraire de ce qui devrait exister
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, false, status -> {
			final PersonnePhysique parent1 = addHabitant(noIndParent1);
			final PersonnePhysique parent2 = addHabitant(noIndParent2);
			final PersonnePhysique enfant2 = addHabitant(noIndEnfant2);
			addParente(enfant2, parent2, dateNaissanceEnfant2, null);
			final Ids ids1 = new Ids();
			ids1.ppParent1 = parent1.getNumero();
			ids1.ppParent2 = parent2.getNumero();
			ids1.ppEnfant2 = enfant2.getNumero();
			return ids1;
		});

		final CalculParentesResults result = processor.run(1, CalculParentesMode.REFRESH_DIRTY, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getErreurs().size());
		Assert.assertEquals(1, result.getUpdates().size());

		final List<ParenteUpdateInfo> updates = result.getUpdates();
		final ParenteUpdateInfo update = updates.get(0);
		Assert.assertNotNull(update);
		Assert.assertEquals(ids.ppParent1, update.noCtbParent);
		Assert.assertEquals(ppEnfant1, update.noCtbEnfant);
		Assert.assertEquals(dateNaissanceEnfant1, update.dateDebut);
		Assert.assertNull(update.dateFin);
		Assert.assertEquals(ParenteUpdateInfo.Action.CREATION, update.action);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final List<Parente> parentes = hibernateTemplate.find("from Parente", FlushMode.AUTO);
			Assert.assertNotNull(parentes);
			Assert.assertEquals(2, parentes.size());
			final List<Parente> copyToSort = new ArrayList<>(parentes);
			Collections.sort(copyToSort, new Comparator<Parente>() {
				@Override
				public int compare(Parente o1, Parente o2) {
					int comp = Long.compare(o1.getObjetId(), o2.getObjetId());
					if (comp == 0) {
						comp = Long.compare(o1.getSujetId(), o2.getSujetId());
					}
					return comp;
				}
			});

			{
				final Parente parente = copyToSort.get(0);
				Assert.assertNotNull(parente);
				Assert.assertEquals((Long) ids.ppParent1, parente.getObjetId());
				Assert.assertEquals((Long) ppEnfant1, parente.getSujetId());
				Assert.assertEquals(dateNaissanceEnfant1, parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
				Assert.assertFalse(parente.isAnnule());
			}
			{
				final Parente parente = copyToSort.get(1);
				Assert.assertNotNull(parente);
				Assert.assertEquals((Long) ids.ppParent2, parente.getObjetId());
				Assert.assertEquals((Long) ids.ppEnfant2, parente.getSujetId());
				Assert.assertEquals(dateNaissanceEnfant2, parente.getDateDebut());
				Assert.assertNull(parente.getDateFin());
				Assert.assertFalse(parente.isAnnule());     // pas annulé car pas rafraîchi !
			}
			return null;
		});
	}

	@Test
	public void testRefreshDirtyToujoursPasBon() throws Exception {

		final long noIndParent1 = 4367345L;
		final long noIndEnfant1 = 543735L;
		final RegDate dateNaissanceEnfant1 = date(2000, 5, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu parent1 = addIndividu(noIndParent1, null, "Bleu", "Papa", Sexe.MASCULIN);
				final MockIndividu enfant1 = addIndividu(noIndEnfant1, dateNaissanceEnfant1, "Bleu", "Puce", Sexe.FEMININ);
				addLiensFiliation(enfant1, parent1, null, dateNaissanceEnfant1, null);
			}
		});

		// on crée juste l'enfant1 en tant que contribuable, sans son parent -> dirty
		final long ppEnfant1 = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique enfant1 = addHabitant(noIndEnfant1);
			return enfant1.getNumero();
		});

		// vérifie le flag "dirty"
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique enfant1 = (PersonnePhysique) tiersDAO.get(ppEnfant1);
			Assert.assertNotNull(enfant1);
			Assert.assertTrue(enfant1.isParenteDirty());
			return null;
		});

		// on lance le retraitement
		final CalculParentesResults result = processor.run(1, CalculParentesMode.REFRESH_DIRTY, null);
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getErreurs().size());     // eh oui, il y a toujours un problème ici !!...
		Assert.assertEquals(0, result.getUpdates().size());

		final CalculParentesResults.InfoErreur erreur = result.getErreurs().get(0);
		Assert.assertNotNull(erreur);
		Assert.assertEquals(ppEnfant1, erreur.noCtbEnfant);
		Assert.assertEquals(String.format("Impossible de créer une parenté depuis l'enfant %d vers son parent car aucun tiers n'existe dans le registre avec le numéro d'individu %d.",
		                                  ppEnfant1, noIndParent1),
		                    erreur.msg);
	}
}
