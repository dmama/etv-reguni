
package ch.vd.uniregctb.tiers.jobs;

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

import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.Filiation;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.type.Sexe;

public class InitialisationFiliationsProcessorTest extends BusinessTest {

	private InitialisationFiliationsProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final RapportEntreTiersDAO rapportDAO = getBean(RapportEntreTiersDAO.class, "rapportEntreTiersDAO");
		processor = new InitialisationFiliationsProcessor(rapportDAO, tiersDAO, transactionManager, hibernateTemplate, serviceCivil, tiersService);
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

		final InitialisationFiliationsResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getFiliations());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getFiliations().size());
	}

	@Test
	public void testDestructionFiliationsPresentes() throws Exception {
		serviceCivil.setUp(new DefaultMockServiceCivil());

		final class Ids {
			long idParent;
			long idEnfant;
			long idFilation;
		}

		// quelques non-habitants quand-même (avec une relation de filiation entre eux...)
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pierre = addNonHabitant("Pierre", "Kiroul", null, Sexe.MASCULIN);
				final PersonnePhysique namass = addNonHabitant("Namass", "Pamouss", null, Sexe.FEMININ);
				final Filiation filiation = addFiliation(namass, pierre, date(2010, 6, 23), null);
				final Ids ids = new Ids();
				ids.idParent = pierre.getNumero();
				ids.idEnfant = namass.getNumero();
				ids.idFilation = filiation.getId();
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
					Assert.assertEquals(Filiation.class, rapport.getClass());
					Assert.assertEquals((Long) ids.idFilation, rapport.getId());
				}
				{
					final PersonnePhysique enfant = (PersonnePhysique) tiersDAO.get(ids.idEnfant);
					final Set<RapportEntreTiers> sujetRapports = enfant.getRapportsSujet();
					Assert.assertEquals(1, sujetRapports.size());
					final Set<RapportEntreTiers> objectRapports = enfant.getRapportsObjet();
					Assert.assertEquals(0, objectRapports.size());

					final RapportEntreTiers rapport = sujetRapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertEquals(Filiation.class, rapport.getClass());
					Assert.assertEquals((Long) ids.idFilation, rapport.getId());
				}

				return null;
			}
		});

		final InitialisationFiliationsResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getFiliations());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(0, results.getFiliations().size());

		// on vérifie que la relation de filiation a bien été enlevée
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
					final Filiation filiation = hibernateTemplate.get(Filiation.class, ids.idFilation);
					Assert.assertNull(filiation);
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
		final InitialisationFiliationsResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertNotNull(results.getErreurs());
		Assert.assertNotNull(results.getFiliations());
		Assert.assertEquals(0, results.getErreurs().size());
		Assert.assertEquals(4, results.getFiliations().size());

		// vérification du contenu du rapport d'exécution
		final List<InitialisationFiliationsResults.InfoFiliation> filiations = new ArrayList<>(results.getFiliations());
		Collections.sort(filiations, new Comparator<InitialisationFiliationsResults.InfoFiliation>() {
			@Override
			public int compare(InitialisationFiliationsResults.InfoFiliation o1, InitialisationFiliationsResults.InfoFiliation o2) {
				final int compParents = Long.compare(o1.noCtbParent, o2.noCtbParent);
				if (compParents == 0) {
					return Long.compare(o1.noCtbEnfant, o2.noCtbEnfant);
				}
				return compParents;
			}
		});

		{
			final InitialisationFiliationsResults.InfoFiliation filiation = filiations.get(0);
			Assert.assertNotNull(filiation);
			Assert.assertEquals(ids.idPapa, filiation.noCtbParent);
			Assert.assertEquals(ids.idBelle, filiation.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 22), filiation.dateDebut);
			Assert.assertNull(filiation.dateFin);
		}
		{
			final InitialisationFiliationsResults.InfoFiliation filiation = filiations.get(1);
			Assert.assertNotNull(filiation);
			Assert.assertEquals(ids.idPapa, filiation.noCtbParent);
			Assert.assertEquals(ids.idDur, filiation.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 21), filiation.dateDebut);
			Assert.assertNull(filiation.dateFin);
		}
		{
			final InitialisationFiliationsResults.InfoFiliation filiation = filiations.get(2);
			Assert.assertNotNull(filiation);
			Assert.assertEquals(ids.idMaman, filiation.noCtbParent);
			Assert.assertEquals(ids.idBelle, filiation.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 22), filiation.dateDebut);
			Assert.assertNull(filiation.dateFin);
		}
		{
			final InitialisationFiliationsResults.InfoFiliation filiation = filiations.get(3);
			Assert.assertNotNull(filiation);
			Assert.assertEquals(ids.idMaman, filiation.noCtbParent);
			Assert.assertEquals(ids.idDur, filiation.noCtbEnfant);
			Assert.assertEquals(date(2005, 12, 21), filiation.dateDebut);
			Assert.assertNull(filiation.dateFin);
		}

		// vérification en base
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final List<Filiation> filiations = hibernateTemplate.find("from Filiation", null, FlushMode.AUTO);
				Assert.assertNotNull(filiations);
				Assert.assertEquals(4, filiations.size());
				final List<Filiation> copyToSort = new ArrayList<>(filiations);
				Collections.sort(copyToSort, new Comparator<Filiation>() {
					@Override
					public int compare(Filiation o1, Filiation o2) {
						int comp = Long.compare(o1.getObjetId(), o2.getObjetId());
						if (comp == 0) {
							comp = Long.compare(o1.getSujetId(), o2.getSujetId());
						}
						return comp;
					}
				});

				{
					final Filiation filiation = copyToSort.get(0);
					Assert.assertNotNull(filiation);
					Assert.assertEquals((Long) ids.idPapa, filiation.getObjetId());
					Assert.assertEquals((Long) ids.idBelle, filiation.getSujetId());
					Assert.assertEquals(date(2005, 12, 22), filiation.getDateDebut());
					Assert.assertNull(filiation.getDateFin());
				}
				{
					final Filiation filiation = copyToSort.get(1);
					Assert.assertNotNull(filiation);
					Assert.assertEquals((Long) ids.idPapa, filiation.getObjetId());
					Assert.assertEquals((Long) ids.idDur, filiation.getSujetId());
					Assert.assertEquals(date(2005, 12, 21), filiation.getDateDebut());
					Assert.assertNull(filiation.getDateFin());
				}
				{
					final Filiation filiation = copyToSort.get(2);
					Assert.assertNotNull(filiation);
					Assert.assertEquals((Long) ids.idMaman, filiation.getObjetId());
					Assert.assertEquals((Long) ids.idBelle, filiation.getSujetId());
					Assert.assertEquals(date(2005, 12, 22), filiation.getDateDebut());
					Assert.assertNull(filiation.getDateFin());
				}
				{
					final Filiation filiation = copyToSort.get(3);
					Assert.assertNotNull(filiation);
					Assert.assertEquals((Long) ids.idMaman, filiation.getObjetId());
					Assert.assertEquals((Long) ids.idDur, filiation.getSujetId());
					Assert.assertEquals(date(2005, 12, 21), filiation.getDateDebut());
					Assert.assertNull(filiation.getDateFin());
				}
				return null;
			}
		});
	}
}
