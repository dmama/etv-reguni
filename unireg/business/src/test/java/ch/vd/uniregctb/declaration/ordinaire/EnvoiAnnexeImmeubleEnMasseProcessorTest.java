package ch.vd.uniregctb.declaration.ordinaire;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class EnvoiAnnexeImmeubleEnMasseProcessorTest extends BusinessTest {

	private EnvoiAnnexeImmeubleEnMasseProcessor processor;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		final HibernateTemplate hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		final PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		final PeriodeImpositionService periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");

		serviceCivil.setUp(new DefaultMockServiceCivil());

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EnvoiAnnexeImmeubleEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO, diService, 100, transactionManager, serviceCivilCacheWarmer,
				periodeImpositionService);
		// évite de logger plein d'erreurs pendant qu'on teste le comportement du processor
		Logger serviceLogger = Logger.getLogger(DeclarationImpotServiceImpl.class);
		serviceLogger.setLevel(Level.FATAL);
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();

		// réactive le log normal
		Logger serviceLogger = Logger.getLogger(DeclarationImpotServiceImpl.class);
		serviceLogger.setLevel(Level.ERROR);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
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
	@Transactional(rollbackFor = Throwable.class)
	public void testPeriodeImpositionEnFinPeriode() {

		// Contribuable sans for fiscal
		final Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(erich, 2011));

		// Contribuable avec un for fiscal principal à Neuchâtel
		final Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(maxwell, date(1980, 1, 1), null, MockCommune.Neuchatel);
		assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(maxwell, 2011));

		// Contribuable avec un for fiscal principal ouvert à Lausanne
		final Contribuable felicien = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(felicien, date(1980, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		final PeriodeImposition piFelicien = processor.getPeriodeImpositionEnFinDePeriodeFiscale(felicien, 2011);
		assertNotNull(piFelicien);
		assertEquals(date(2011, 1, 1), piFelicien.getDateDebut());
		assertEquals(date(2011, 12, 31), piFelicien.getDateFin());

		// Contribuable avec un for fiscal principal fermé à Lausanne
		final Contribuable bernard = addNonHabitant("Bernard", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(bernard, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2011, 8, 20), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(bernard, 2011));

		// Contribuable avec un for fiscal principal fermé à Lausanne
		final Contribuable lamda = addNonHabitant("Lamda", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(lamda, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2011, 8, 20), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
		assertNull(processor.getPeriodeImpositionEnFinDePeriodeFiscale(lamda, 2011));
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testNumeroSequenceUtilise() {

		final int annee = 2011;
		final DateRange anneeComplete = new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 12, 31));
		final DateRange moitieAnnee1 = new DateRangeHelper.Range(date(annee, 1, 1), date(annee, 6, 30));
		final DateRange moitieAnnee2 = new DateRangeHelper.Range(date(annee, 7, 1), date(annee, 12, 31));
		final PeriodeFiscale pf = addPeriodeFiscale(annee);
		final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
		addCedi();

		// Contribuable sans di 2011 -> 1
		final Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertEquals(1, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(erich, anneeComplete));

		// Contribuable avec une di 2011 annulée -> 2
		{
			final Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinaire di = addDeclarationImpot(maxwell, pf, anneeComplete.getDateDebut(), anneeComplete.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di.setAnnule(true);
			assertEquals(1, (int) di.getNumero());
			assertEquals(2, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(maxwell, anneeComplete));
		}

		// Contribuable avec deux di 2011 annulées -> 3
		{
			final Contribuable arthur = addNonHabitant("Arthur", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinaire di1 = addDeclarationImpot(arthur, pf, moitieAnnee1.getDateDebut(), moitieAnnee1.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di1.setAnnule(true);
			assertEquals(1, (int) di1.getNumero());
			final DeclarationImpotOrdinaire di2 = addDeclarationImpot(arthur, pf, moitieAnnee2.getDateDebut(), moitieAnnee2.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di2.setAnnule(true);
			assertEquals(2, (int) di2.getNumero());
			assertEquals(3, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(arthur, anneeComplete));
		}

		// Contribuable avec une di 2011 non-annulée qui ne touche pas la fin de l'année -> 2
		{
			final Contribuable felicien = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinaire di = addDeclarationImpot(felicien, pf, moitieAnnee1.getDateDebut(), moitieAnnee1.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			assertEquals(1, (int) di.getNumero());
			assertEquals(2, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(felicien, anneeComplete));
		}

		// Contribuable avec une di 2011 non-annulée qui touche la fin de l'année -> 1 (reprise du numéro)
		{
			final Contribuable bernard = addNonHabitant("Bernard", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinaire di = addDeclarationImpot(bernard, pf, moitieAnnee2.getDateDebut(), moitieAnnee2.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			assertEquals(1, (int) di.getNumero());
			assertEquals(1, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(bernard, anneeComplete));
		}

		// Contribuable avec une di 2011 non-annulée qui touche la fin de l'année -> 12 (reprise du numéro)
		{
			final Contribuable albert = addNonHabitant("Albert", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
			final DeclarationImpotOrdinaire di = addDeclarationImpot(albert, pf, moitieAnnee2.getDateDebut(), moitieAnnee2.getDateFin(), TypeContribuable.VAUDOIS_ORDINAIRE, md);
			di.setNumero(12);
			assertEquals(12, (int) di.getNumero());
			assertEquals(12, EnvoiAnnexeImmeubleEnMasseProcessor.getNoSequenceAnnexeImmeuble(albert, anneeComplete));
		}
	}
}
