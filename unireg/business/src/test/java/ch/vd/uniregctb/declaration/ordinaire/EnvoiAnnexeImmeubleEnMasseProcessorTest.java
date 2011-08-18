package ch.vd.uniregctb.declaration.ordinaire;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.cache.ServiceCivilCacheWarmer;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class EnvoiAnnexeImmeubleEnMasseProcessorTest extends BusinessTest {

	private EnvoiAnnexeImmeubleEnMasseProcessor processor;
	private HibernateTemplate hibernateTemplate;
	private ParametreAppService parametreAppService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		final ModeleDocumentDAO modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		final DelaisService delaisService = getBean(DelaisService.class, "delaisService");
		final DeclarationImpotService diService = getBean(DeclarationImpotService.class, "diService");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");
		final PlatformTransactionManager transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		final ServiceCivilCacheWarmer serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");

		serviceCivil.setUp(new DefaultMockServiceCivil());

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new EnvoiAnnexeImmeubleEnMasseProcessor(tiersService, hibernateTemplate, modeleDAO, periodeDAO, diService, 100, transactionManager, serviceCivilCacheWarmer);
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


		// 1 immeuble => 1 annexe
		assertEquals(1, processor.getNombreAnnexeAEnvoyer(1));
		//2 immeubles => 1annexe
		assertEquals(1, processor.getNombreAnnexeAEnvoyer(2));
		//3 immeubles => 2 annexes
		assertEquals(2, processor.getNombreAnnexeAEnvoyer(3));
		//4 immeubles => 2 annexes
		assertEquals(2, processor.getNombreAnnexeAEnvoyer(4));
		//5 immeubles => 3 annexes
		assertEquals(3, processor.getNombreAnnexeAEnvoyer(5));

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsAssujettiEnFinPeriode() {

		// Contribuable sans for fiscal
		Contribuable erich = addNonHabitant("Erich", "Honekker", date(1934, 1, 1), Sexe.MASCULIN);
		assertFalse(processor.isAssujettiEnFinDePeriode(erich, 2011));

		// Contribuable avec un for fiscal principal à Neuchâtel
		Contribuable maxwell = addNonHabitant("Maxwell", "Dupuis", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(maxwell, date(1980, 1, 1), null, MockCommune.Neuchatel);
		assertFalse(processor.isAssujettiEnFinDePeriode(maxwell, 2011));

		// Contribuable avec un for fiscal principal ouvert à Lausanne
		Contribuable felicien = addNonHabitant("Félicien", "Bolomey", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(felicien, date(1980, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
		assertTrue(processor.isAssujettiEnFinDePeriode(felicien, 2011));

		// Contribuable avec un for fiscal principal fermé à Lausanne
		Contribuable bernard = addNonHabitant("Bernard", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(bernard, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2011, 8, 20), MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
		assertFalse(processor.isAssujettiEnFinDePeriode(bernard, 2011));

		// Contribuable avec un for fiscal principal fermé à Lausanne
		Contribuable lamda = addNonHabitant("Lamda", "Bidon", date(1955, 1, 1), Sexe.MASCULIN);
		addForPrincipal(lamda, date(1980, 1, 1), MotifFor.ARRIVEE_HS, date(2011, 8, 20), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
		assertFalse(processor.isAssujettiEnFinDePeriode(lamda, 2011));
	}
}
