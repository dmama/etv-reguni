package ch.vd.unireg.listes;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.cache.ServiceCivilCacheWarmer;
import ch.vd.unireg.common.AbstractSpringTest;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.listes.listesnominatives.ListesNominativesProcessor;
import ch.vd.unireg.listes.listesnominatives.ListesNominativesResults;
import ch.vd.unireg.listes.listesnominatives.TypeAdresse;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.transaction.TransactionManager;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.PeriodeDecompte;
import ch.vd.unireg.type.PeriodiciteDecompte;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ListesNominativesProcessorTest extends BusinessTest {

	private TiersService tiersService;

	private HibernateTemplate hibernateTemplate;

	private AdresseService adresseService;

	private TiersDAO tiersDAO;

	private TransactionManager transactionManager;

	private ServiceCivilCacheWarmer serviceCivilCacheWarmer;

	private ListesNominativesProcessor processor;

	@Before
	public void setUp() throws Exception {
		super.onSetUp();
		this.hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		this.tiersService = getBean(TiersService.class, "tiersService");
		this.adresseService = getBean(AdresseService.class, "adresseService");
		this.tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		this.transactionManager = getBean(TransactionManager.class, "transactionManager");
		this.serviceCivilCacheWarmer = getBean(ServiceCivilCacheWarmer.class, "serviceCivilCacheWarmer");
		processor = new ListesNominativesProcessor(hibernateTemplate,
		                                           tiersService,
		                                           adresseService,
		                                           transactionManager,
		                                           tiersDAO,
		                                           serviceCivilCacheWarmer);
	}


	@Test
	public void testListesNominativesProcessorOK() throws Exception {
		final long dpiId = doInNewTransaction(new AbstractSpringTest.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});
		final ListesNominativesResults result = processor.run(1, TypeAdresse.FORMATTEE, true, true, Collections.singleton(dpiId), RegDate.get(), true, null);
		assertNotNull(result);
		assertEquals(result.getNombreTiersTraites(), 1);
	}

	@Test
	public void testListesNominativesProcessorMoreThan1000Ctb() throws Exception {
		final int NUMBER_IDS_CTB = 1500;
		final List<Long> tiers = doInNewTransaction(new AbstractSpringTest.TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				List<Long> ids = new ArrayList<>();
				for (int i = 0; i < NUMBER_IDS_CTB; i++) {
					DebiteurPrestationImposable dpi = addDebiteur();
					tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2008, 1, 1), date(2008, 12, 31));
					tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
					tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2010, 1, 1), date(2010, 12, 31));
					tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
					addForDebiteur(dpi, date(2008, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
					ids.add(dpi.getNumero());
				}
				return ids;
			}
		});
		final ListesNominativesResults result = processor.run(1, TypeAdresse.FORMATTEE, true, true, new HashSet<>(tiers), RegDate.get(), true, null);
		assertNotNull(result);
		assertEquals(result.getNombreTiersTraites(), NUMBER_IDS_CTB);
	}

	@Test
	public void testListesNominativesProcessorSansFichierCtbOK() throws Exception {
		final long dpiId = doInNewTransaction(new AbstractSpringTest.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				DebiteurPrestationImposable dpi = addDebiteur();
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2008, 1, 1), date(2008, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.TRIMESTRIEL, null, date(2009, 1, 1), date(2009, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.UNIQUE, PeriodeDecompte.M01, date(2010, 1, 1), date(2010, 12, 31));
				tiersService.addPeriodicite(dpi, PeriodiciteDecompte.MENSUEL, null, date(2011, 1, 1), null);
				addForDebiteur(dpi, date(2008, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Bex);
				return dpi.getNumero();
			}
		});
		final ListesNominativesResults result = processor.run(1, TypeAdresse.FORMATTEE, true, true, new HashSet<>(), RegDate.get(), true, null);
		assertNotNull(result);
		assertEquals(result.getNombreTiersTraites(), 1);
	}
}