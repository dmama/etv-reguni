package ch.vd.uniregctb.parentes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.MultipleSwitch;
import ch.vd.uniregctb.tiers.Parente;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.type.Sexe;

public class InitialisationParentesProcessorTest extends BusinessTest {

	private InitialisationParentesProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final RapportEntreTiersDAO rapportDAO = getBean(RapportEntreTiersDAO.class, "rapportEntreTiersDAO");

		// contrairement au vrai code, je ne coupe pas ici le validationInterceptor pour que si quelque chose
		// pête à ce niveau, on le voit au moins ici...
		final MultipleSwitch interceptorSwitch = new MultipleSwitch(parentesSynchronizer, tacheSynchronizer);
		processor = new InitialisationParentesProcessor(rapportDAO, tiersDAO, transactionManager, hibernateTemplate, interceptorSwitch, tiersService);
	}

	@Test
	public void testAucunConnuAuCivil() throws Exception {
		serviceCivil.setUp(new DefaultMockServiceCivil());

		// quelques non-habitants quand-même
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addNonHabitant("Pierre", "Kiroul", null, Sexe.MASCULIN);
				addNonHabitant("Namass", "Pamouss", null, Sexe.FEMININ);
				return null;
			}
		});

		final InitialisationParentesResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getParentes());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getParentes().size());
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
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				tiersService.createNonHabitantFromIndividu(noIndividuEnfant);
				tiersService.createNonHabitantFromIndividu(noIndividuParent);
				addNonHabitant("Pierre", "Kiroul", null, Sexe.MASCULIN);
				addNonHabitant("Namass", "Pamouss", null, Sexe.FEMININ);
				return null;
			}
		});

		final InitialisationParentesResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getParentes());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getParentes().size());
	}

	@Test
	public void testDestructionParentesPresentes() throws Exception {
		serviceCivil.setUp(new DefaultMockServiceCivil());

		final class Ids {
			long idParent;
			long idEnfant;
			long isParente;
		}

		// quelques non-habitants quand-même (avec une relation de filiation entre eux...)
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pierre = addNonHabitant("Pierre", "Kiroul", null, Sexe.MASCULIN);
				final PersonnePhysique namass = addNonHabitant("Namass", "Pamouss", null, Sexe.FEMININ);
				final Parente parente = addParente(namass, pierre, date(2010, 6, 23), null);
				final Ids ids = new Ids();
				ids.idParent = pierre.getNumero();
				ids.idEnfant = namass.getNumero();
				ids.isParente = parente.getId();
				return ids;
			}
		});

		// on vérifie que la relation de filiation est bien là...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
			}
		});

		final InitialisationParentesResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getParentes());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getParentes().size());

		// on vérifie que la relation de parenté a bien été enlevée
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
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
			}
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
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique papa = addHabitant(noIndPapa);
				final PersonnePhysique maman = addHabitant(noIndMaman);
				final PersonnePhysique belle = addHabitant(noIndBelle);
				final PersonnePhysique dur = addHabitant(noIndDur);
				final PersonnePhysique henri = addHabitant(noIndHenri);

				final Ids ids = new Ids();
				ids.idPapa = papa.getNumero();
				ids.idMaman = maman.getNumero();
				ids.idBelle = belle.getNumero();
				ids.idDur = dur.getNumero();
				ids.idHenri = henri.getNumero();
				return ids;
			}
		});

		// génération des relations
		final InitialisationParentesResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getParentes());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(4, results.getParentes().size());

		// vérification du contenu du rapport d'exécution
		final List<InitialisationParentesResults.InfoParente> parentes = new ArrayList<>(results.getParentes());
		Collections.sort(parentes, new Comparator<InitialisationParentesResults.InfoParente>() {
			@Override
			public int compare(InitialisationParentesResults.InfoParente o1, InitialisationParentesResults.InfoParente o2) {
				final int compParents = Long.compare(o1.noCtbParent, o2.noCtbParent);
				if (compParents == 0) {
					return Long.compare(o1.noCtbEnfant, o2.noCtbEnfant);
				}
				return compParents;
			}
		});

		{
			final InitialisationParentesResults.InfoParente parente = parentes.get(0);
			Assert.assertNotNull(parente);
			Assert.assertEquals(ids.idPapa, parente.noCtbParent);
			Assert.assertEquals(ids.idBelle, parente.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 22), parente.dateDebut);
			Assert.assertNull(parente.dateFin);
		}
		{
			final InitialisationParentesResults.InfoParente parente = parentes.get(1);
			Assert.assertNotNull(parente);
			Assert.assertEquals(ids.idPapa, parente.noCtbParent);
			Assert.assertEquals(ids.idDur, parente.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 21), parente.dateDebut);
			Assert.assertNull(parente.dateFin);
		}
		{
			final InitialisationParentesResults.InfoParente parente = parentes.get(2);
			Assert.assertNotNull(parente);
			Assert.assertEquals(ids.idMaman, parente.noCtbParent);
			Assert.assertEquals(ids.idBelle, parente.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 22), parente.dateDebut);
			Assert.assertNull(parente.dateFin);
		}
		{
			final InitialisationParentesResults.InfoParente parente = parentes.get(3);
			Assert.assertNotNull(parente);
			Assert.assertEquals(ids.idMaman, parente.noCtbParent);
			Assert.assertEquals(ids.idDur, parente.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 21), parente.dateDebut);
			Assert.assertNull(parente.dateFin);
		}

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Parente> parentes = hibernateTemplate.find("from Parente", null, FlushMode.AUTO);
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
					Assert.assertEquals((Long) ids.idPapa, parente.getObjetId());
					Assert.assertEquals((Long) ids.idBelle, parente.getSujetId());
					Assert.assertEquals(date(2005, 12, 22), parente.getDateDebut());
					Assert.assertNull(parente.getDateFin());
				}
				{
					final Parente parente = copyToSort.get(1);
					Assert.assertNotNull(parente);
					Assert.assertEquals((Long) ids.idPapa, parente.getObjetId());
					Assert.assertEquals((Long) ids.idDur, parente.getSujetId());
					Assert.assertEquals(date(2005, 12, 21), parente.getDateDebut());
					Assert.assertNull(parente.getDateFin());
				}
				{
					final Parente parente = copyToSort.get(2);
					Assert.assertNotNull(parente);
					Assert.assertEquals((Long) ids.idMaman, parente.getObjetId());
					Assert.assertEquals((Long) ids.idBelle, parente.getSujetId());
					Assert.assertEquals(date(2005, 12, 22), parente.getDateDebut());
					Assert.assertNull(parente.getDateFin());
				}
				{
					final Parente parente = copyToSort.get(3);
					Assert.assertNotNull(parente);
					Assert.assertEquals((Long) ids.idMaman, parente.getObjetId());
					Assert.assertEquals((Long) ids.idDur, parente.getSujetId());
					Assert.assertEquals(date(2005, 12, 21), parente.getDateDebut());
					Assert.assertNull(parente.getDateFin());
				}
				return null;
			}
		});
	}
}
