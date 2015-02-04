package ch.vd.uniregctb.declaration.ordinaire;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class EnvoiAnnexeImmeubleEnMasseProcessorTest extends BusinessTest {

	private EnvoiAnnexeImmeubleEnMasseProcessor processor;
	private AdresseService adresseService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		adresseService = getBean(AdresseService.class, "adresseService");

		serviceCivil.setUp(new DefaultMockServiceCivil());

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EnvoiAnnexeImmeubleEnMasseProcessor(tiersService, modeleDAO, periodeDAO, diService, 100, transactionManager, serviceCivilCacheWarmer,
				periodeImpositionService, adresseService);
	}

	@Test
	public void testCalculerNombreAnnexeImmeuble() {

		// 1 immeuble => 2 annexes
		assertEquals(2, processor.getNombreAnnexeAEnvoyer(1));
		//2 immeubles => 2 annexes
		assertEquals(2, processor.getNombreAnnexeAEnvoyer(2));
		//3 immeubles => 4 annexes
		assertEquals(4, processor.getNombreAnnexeAEnvoyer(3));
		//4 immeubles => 4 annexes
		assertEquals(4, processor.getNombreAnnexeAEnvoyer(4));
		//5 immeubles => 6 annexes
		assertEquals(6, processor.getNombreAnnexeAEnvoyer(5));
	}

	@Test
	public void testPeriodeImpositionEnFinPeriode() throws Exception {

		final EnvoiAnnexeImmeubleResults r = new EnvoiAnnexeImmeubleResults(2011, RegDate.get(), "tubidu.zip", Integer.MAX_VALUE, tiersService, adresseService);

		// Contribuable sans for fiscal
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
				assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(erich, 2011, r));
			}
		});

		// Contribuable avec un for fiscal principal à Neuchâtel
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
				addForPrincipal(maxwell, date(1980, 1, 1), null, MockCommune.Neuchatel);
				assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(maxwell, 2011, r));			}
		});

		// Contribuable avec un for fiscal principal ouvert à Lausanne
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Contribuable felicien = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
				addForPrincipal(felicien, date(1980, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				final PeriodeImposition piFelicien = processor.getPeriodeImpositionEnFinDePeriodeFiscale(felicien, 2011, r);
				assertNotNull(piFelicien);
				assertEquals(date(2011, 1, 1), piFelicien.getDateDebut());
				assertEquals(date(2011, 12, 31), piFelicien.getDateFin());
			}
		});

		// Contribuable avec un for fiscal principal fermé à Lausanne
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Contribuable bernard = addNonHabitant("Bernard", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
				addForPrincipal(bernard, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2011, 8, 20), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(bernard, 2011, r));
			}
		});

		// Contribuable avec un for fiscal principal fermé à Lausanne
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final Contribuable lamda = addNonHabitant("Lamda", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
				addForPrincipal(lamda, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2011, 8, 20), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(lamda, 2011, r));
			}
		});
	}

	@Test
	public void testNumeroSequenceUtilise() throws Exception {

		final int annee = 2011;
		final DateRange anneeComplete = new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 12, 31));
		final DateRange moitieAnnee1 = new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 6, 30));
		final DateRange moitieAnnee2 = new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 12, 31));

		final class Ids {
			long pfId;
			long mdId;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);

				// Contribuable sans di 2011 -> 1
				final Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
				assertEquals(1, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(erich, anneeComplete));

				final Ids ids = new Ids();
				ids.mdId = md.getId();
				ids.pfId = pf.getId();
				return ids;
			}
		});

		// Contribuable avec une di 2011 annulée -> 2
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PeriodeFiscale pf = hibernateTemplate.get(PeriodeFiscale.class, ids.pfId);
				final ModeleDocument md = hibernateTemplate.get(ModeleDocument.class, ids.mdId);
				final Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(maxwell, pf, anneeComplete.getDateDebut(), anneeComplete.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				di.setAnnule(true);
				assertEquals(1, (int) di.getNumero());
				assertEquals(2, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(maxwell, anneeComplete));
			}
		});

		// Contribuable avec deux di 2011 annulées -> 3
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PeriodeFiscale pf = hibernateTemplate.get(PeriodeFiscale.class, ids.pfId);
				final ModeleDocument md = hibernateTemplate.get(ModeleDocument.class, ids.mdId);
				final Contribuable arthur = addNonHabitant("Arthur", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
				final DeclarationImpotOrdinaire di1 = addDeclarationImpot(arthur, pf, moitieAnnee1.getDateDebut(), moitieAnnee1.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				di1.setAnnule(true);
				assertEquals(1, (int) di1.getNumero());
				final DeclarationImpotOrdinaire di2 = addDeclarationImpot(arthur, pf, moitieAnnee2.getDateDebut(), moitieAnnee2.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				di2.setAnnule(true);
				assertEquals(2, (int) di2.getNumero());
				assertEquals(3, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(arthur, anneeComplete));
			}
		});

		// Contribuable avec une di 2011 non-annulée qui ne touche pas la fin de l'année -> 2
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PeriodeFiscale pf = hibernateTemplate.get(PeriodeFiscale.class, ids.pfId);
				final ModeleDocument md = hibernateTemplate.get(ModeleDocument.class, ids.mdId);
				final Contribuable felicien = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(felicien, pf, moitieAnnee1.getDateDebut(), moitieAnnee1.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				assertEquals(1, (int) di.getNumero());
				assertEquals(2, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(felicien, anneeComplete));
			}
		});

		// Contribuable avec une di 2011 non-annulée qui touche la fin de l'année -> 1 (reprise du numéro)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PeriodeFiscale pf = hibernateTemplate.get(PeriodeFiscale.class, ids.pfId);
				final ModeleDocument md = hibernateTemplate.get(ModeleDocument.class, ids.mdId);
				final Contribuable bernard = addNonHabitant("Bernard", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(bernard, pf, moitieAnnee2.getDateDebut(), moitieAnnee2.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				assertEquals(1, (int) di.getNumero());
				assertEquals(1, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(bernard, anneeComplete));
			}
		});

		// Contribuable avec une di 2011 non-annulée qui touche la fin de l'année -> 12 (reprise du numéro)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PeriodeFiscale pf = hibernateTemplate.get(PeriodeFiscale.class, ids.pfId);
				final ModeleDocument md = hibernateTemplate.get(ModeleDocument.class, ids.mdId);
				final Contribuable albert = addNonHabitant("Albert", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(albert, pf, moitieAnnee2.getDateDebut(), moitieAnnee2.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
				di.setNumero(12);
				assertEquals(12, (int) di.getNumero());
				assertEquals(12, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(albert, anneeComplete));
			}
		});
	}
}
