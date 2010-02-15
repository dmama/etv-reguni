package ch.vd.uniregctb.interfaces.fiscal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import ch.vd.ifosdi.metier.exceptions.BusinessException;
import ch.vd.ifosdi.metier.registre.ContribuableSDI;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.PartialDateException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.fiscal.model.Contribuable;
import ch.vd.registre.fiscal.model.ContribuableFoyer;
import ch.vd.registre.fiscal.model.DeclarationQuittance;
import ch.vd.registre.fiscal.model.EnumCritereRechercheContribuable;
import ch.vd.registre.fiscal.model.EnumTypeImposition;
import ch.vd.registre.fiscal.model.impl.ContribuableRetourInfoDiImpl;
import ch.vd.registre.fiscal.model.impl.DeclarationQuittanceImpl;
import ch.vd.registre.fiscal.service.ServiceFiscal;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AbstractBusinessTest;
import ch.vd.uniregctb.common.ClientConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleDocumentDAO;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.fiscal.service.impl.ServiceFiscalBean;
import ch.vd.uniregctb.fiscal.service.impl.ServiceFiscalImpl;
import ch.vd.uniregctb.interfaces.migreg.Migrator;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.utils.ContribuableComparator;
import ch.vd.uniregctb.situationfamille.SituationFamilleService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import com.thoughtworks.xstream.XStream;

/**
 * Test case du service fiscal.
 *
 * @author Baba NGOM
 *
 */
@ContextConfiguration(locations = { "classpath:ut/uintf-ejbut-interfaces.xml",
		"classpath:ut/uintf-ejbut-database.xml",
		"classpath:ut/uintf-ejbut-beans.xml",
		"classpath:ut/uintf-ejbut-datasource.xml",
		"classpath:unireg-business-services.xml",
		"classpath:unireg-core-dao.xml", "classpath:unireg-core-sf.xml",
		"classpath:ut/unireg-businessut-editique.xml",
		"classpath:unireg-business-apireg.xml",
		"classpath:unireg-business-interfaces.xml",
		"classpath:ut/uintf-ejbut-services.xml",
		ClientConstants.UNIREG_BUSINESS_MIGREG })
public class ServiceFiscalTestCase extends AbstractBusinessTest {

	//private static final int CONTRIBUABLE_MIGRE =10000001;
	// private static final int CONTRIBUABLE_MIGRE = 75107103;
	//private static final int CONTRIBUABLE_MIGRE = 10006616;
	private static final int CONTRIBUABLE_MIGRE = 10035633;

	private static final long CONTRIBUABLE_SIMPLE = 10288531;
	private static final int ANNEE = 2006;
	private static final long CONTRIBUABLE_INTER_57_47528204 = 47528204;
	private static final long CONTRIBUABLE_INTER_55_24108604 = 24108604;

	// private static final long CONTRIBUABLE_INTER_57 = 10054424 /* INTER_54 */;
	// private static final long CONTRIBUABLE_INTER_57 = 10092638 /* INTER_54 */;
	private static final Logger LOGGER = Logger.getLogger(ServiceFiscalTestCase.class);
	private static final long CONTRIBUABLE_INTER_57 = CONTRIBUABLE_INTER_55_24108604;

	private static boolean doTruncate = true;

	private TiersDAO tiersDAO;

	private HibernateTemplate hibernateTemplate;

	private ServiceFiscalImpl serviceFiscalImpl;

	private ServiceInfrastructure serviceInfrastructure;
	private ServiceFiscal serviceFiscalHost;

	private ServiceCivil serviceCivil;
	private AdresseService adresseService;
	private TiersService tiersService;
	private SituationFamilleService situationFamilleService;
	private ServiceFiscalBean serviceFiscalBean;
	private ModeleDocumentDAO modeleDAO;
	private PeriodeFiscaleDAO periodeDAO;
	private DeclarationImpotService diService;

	private XStream xstream;
	private Migrator migrator;
	private List<Long> numerosContribuable;

	@SuppressWarnings("unchecked")
	@Override
	public void onSetUp() throws Exception {



		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		modeleDAO = getBean(ModeleDocumentDAO.class, "modeleDocumentDAO");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		serviceInfrastructure = getBean(ServiceInfrastructure.class, "_serviceInfrastructure");
		serviceCivil = getBean(ServiceCivil.class, "_serviceCivil");
		serviceFiscalHost = getBean(ServiceFiscal.class, "serviceFiscalHost");
		tiersService = getBean(TiersService.class, "tiersService");
		situationFamilleService = getBean(SituationFamilleService.class, "situationFamilleService");
		adresseService = getBean(AdresseService.class, "adresseService");
		diService = getBean(DeclarationImpotService.class, "diService");
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		serviceFiscalImpl = new ServiceFiscalImpl();
		serviceFiscalImpl.setTiersDAO(tiersDAO);
		serviceFiscalImpl.setServiceInfrastructure(serviceInfrastructure);
		serviceFiscalImpl.setServiceCivil(serviceCivil);
		serviceFiscalImpl.setAdresseService(adresseService);
		serviceFiscalImpl.setTiersService(tiersService);
		serviceFiscalImpl.setSituationFamilleService(situationFamilleService); // migrator.migreDonneesAnnie()
		serviceFiscalImpl.setHibernateTemplate(hibernateTemplate);
		serviceFiscalImpl.setPeriodeFiscaleDAO(periodeDAO);
		serviceFiscalImpl.setModeleDocumentDAO(modeleDAO);
		serviceFiscalImpl.setDiService(diService);
		xstream = new XStream();

			super.onSetUp();

		numerosContribuable = hibernateTemplate.find("SELECT tiers.numero FROM Tiers AS tiers");

	}

	/**
	 * Test de la m�thode getContribuableGenerale du service fiscal.
	 *
	 * @throws Exception
	 *             si un probl�me survient durant l'appel au service.
	 */
	@Test
	public void testGetContribuableInfoGenerale() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				ModeleDocument declarationVaudTax2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2008);


				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);

				return null;
			}
		});

		ContribuableFoyer contribuableUnireg = (ContribuableFoyer) serviceFiscalImpl.getContribuable(ids.ericId, ANNEE, false, false,true);
		assertNotNull(contribuableUnireg);


	}

	@Ignore
	public void testComparaisonGetContribuable() throws Exception {
		ContribuableFoyer contribuableHost = null;
		ContribuableFoyer contribuableUnireg = null;
		for (long numHabitant : numerosContribuable) {

			contribuableHost = (ContribuableFoyer) serviceFiscalHost.getContribuableInfoGenerale(numHabitant, ANNEE, true);

			assertNotNull(contribuableHost);
			String ctbHostXml = xstream.toXML(contribuableHost);
			File fileCtbHostXml = new File("comparaison/CtbHost" + numHabitant + ".xml");
			FileUtils.writeStringToFile(fileCtbHostXml, ctbHostXml);

			contribuableUnireg = (ContribuableFoyer) serviceFiscalImpl.getContribuable(numHabitant, ANNEE, true, false,true);
			assertNotNull(contribuableUnireg);
			String ctbUniregXml = xstream.toXML(contribuableUnireg);
			File fileCtbUniregXml = new File("comparaison/CtbUnireg" + numHabitant + ".xml");
			FileUtils.writeStringToFile(fileCtbUniregXml, ctbUniregXml);
			ContribuableComparator comparator = new ContribuableComparator();
			comparator.compare(contribuableHost, contribuableUnireg);
		}

		// Assert.assertEquals(ctbHostXml, ctbUniregXml);
	}






	@Test
	public void testIsContribuableI107() throws Exception {
		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				eric.setNumeroCompteBancaire("CFE2145000321457");
				eric.setTitulaireCompteBancaire("Eric Edouard Bolomey");
				eric.setDebiteurInactif(false);
				ids.ericId = eric.getNumero();
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			Assert.isFalse(serviceFiscalImpl.isContribuableI107(ids.ericId));


		return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
			PersonnePhysique eric = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			eric.setDebiteurInactif(true);
			return null;
			}
		});


		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			Assert.isTrue(serviceFiscalImpl.isContribuableI107(ids.ericId));


		return null;
			}
		});

	}



	/**
	 * Test de la méthode getContribuable du service fiscal.
	 *
	 * @throws Exception
	 *             si un probléme survient durant l'appel au service.
	 */
	@Test
	public void testGetNoContribuableFoyer() throws Exception {

		class Ids {
			Long ericId;
			Long susanneId;
			Long couplesId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();

				PersonnePhysique susanne = addNonHabitant("suzanne", "cuendet", date(1972, 4, 13), Sexe.FEMININ);
				ids.susanneId = susanne.getNumero();

				EnsembleTiersCouple couple = createEnsembleTiersCouple(eric, susanne,date(1985,8,8));
				ids.couplesId =couple.getMenage().getNumero();

				return null;
			}
		});


		Collection collection = serviceFiscalImpl.getNoContribuableFoyer(ids.ericId, 2000, 2006);
		Assert.isTrue(collection.size() > 0);
		for (Iterator it = collection.iterator(); it.hasNext();) {
			Long value = (Long) it.next();
			assertEquals(value, ids.couplesId);
		}

		collection = serviceFiscalImpl.getNoContribuableFoyer(ids.susanneId, 1995, 2003);
		Assert.isTrue(collection.size() > 0);
		for (Iterator it = collection.iterator(); it.hasNext();) {
			Long value = (Long) it.next();
			assertEquals(value, ids.couplesId);
		}


	}
	@Test
	public void testgetCtrlContribuable(){
		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		try {
			doInNewTransaction(new TxCallback() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {

					PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
					ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
					ModeleDocument declarationVaudTax2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
					addModeleFeuilleDocument("Déclaration", "210", declarationComplete2008);
					addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2008);
					addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2008);
					addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2008);


					// Un tiers tout ce quil y a de plus ordinaire
					PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
					ids.ericId = eric.getNumero();
					addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
					addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
							declarationComplete2008);

					DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) eric.getDeclarationForPeriode(2008).get(0);
					EtatDeclaration expedie = new EtatDeclaration();
					expedie.setEtat(TypeEtatDeclaration.EMISE);
					expedie.setDateObtention(date(2008,1,12));
					declaration.addEtat(expedie);

					return null;
				}
			});
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		try {
			ContribuableSDI contribuable = serviceFiscalImpl.getCtrlContribuable(RegDate.get(2008,3,3).asJavaDate(), ids.ericId.intValue());
			assertNotNull(contribuable);
			Assert.notEmpty(contribuable.getListeDI());
		}
		catch (PartialDateException e) {

			e.printStackTrace();
		}
		catch (BusinessException e) {

			e.printStackTrace();
		}

	}

	/**
	 * Test de comparaison de la méthode getNoContribuableFoyer du service fiscal unireg avec le service Host.
	 *
	 * @throws Exception
	 *             si un probléme survient durant l'appel au service.
	 */
	@Ignore
	public void testGetNoContribuableFoyer2() throws Exception {
		List listUnireg = (List) serviceFiscalImpl.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, 2000, ANNEE);
		List listHost = (List) serviceFiscalHost.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, 2000, ANNEE);
		Assert.isTrue(listUnireg.size() > 0);
		Assert.isTrue(listHost.size() > 0);
		assertEquals(listUnireg.get(0), listHost.get(0));

		listUnireg = (List) serviceFiscalImpl.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, 0, ANNEE);
		listHost = (List) serviceFiscalHost.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, 0, ANNEE);
		Assert.isTrue(listUnireg.size() > 0);
		Assert.isTrue(listHost.size() > 0);
		assertEquals(listUnireg.get(0), listHost.get(0));

		listUnireg = (List) serviceFiscalImpl.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, ANNEE, 0);
		listHost = (List) serviceFiscalHost.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, ANNEE, 0);
		Assert.isTrue(listUnireg.size() > 0);
		Assert.isTrue(listHost.size() > 0);
		assertEquals(listUnireg.get(0), listHost.get(0));

		listUnireg = (List) serviceFiscalImpl.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, 0, 0);
		listHost = (List) serviceFiscalHost.getNoContribuableFoyer(CONTRIBUABLE_MIGRE, 0, 0);
		Assert.isTrue(listUnireg.size() > 0);
		Assert.isTrue(listHost.size() > 0);
		assertEquals(listUnireg.get(0), listHost.get(0));

	}

	@SuppressWarnings("unchecked")
	@Ignore
	public final void testGetContribuableSansDI() throws Exception {
		List<Integer> listId = (List<Integer>) serviceFiscalHost.getListeCtbSansDIPeriode(ANNEE, 10009466);
		File fileContribuableSansDI = new File("contribuableSansDIHost" + ANNEE + ".xml");
		FileUtils.writeStringToFile(fileContribuableSansDI, xstream.toXML(listId));
		listId = (List<Integer>) serviceFiscalImpl.getListeCtbSansDIPeriode(ANNEE, 0);
		fileContribuableSansDI = new File("contribuableSansDIUniregs" + ANNEE + ".xml");
		FileUtils.writeStringToFile(fileContribuableSansDI, xstream.toXML(listId));

	}

	@SuppressWarnings("unchecked")
	@Ignore
	public final void testGetContribuableModifie() throws Exception {

		RegDate debut = RegDate.get(2006, RegDate.JANVIER, 1);
		RegDate fin = RegDate.get(2006, RegDate.DECEMBRE, 31);
		List<Integer> listId = (List<Integer>) serviceFiscalHost.getListeCtbModifies(debut.asJavaDate(), fin.asJavaDate(), 10009466);
		File fileContribuableModifie = new File("contribuableModifieHost" + ANNEE + ".xml");
		FileUtils.writeStringToFile(fileContribuableModifie, xstream.toXML(listId));
		/*
		 * listId = (List<Integer>) serviceFiscalImpl.getListeCtbSansDIPeriode(ANNEE, 0); fileContribuableModifie = new
		 * File("contribuableModifiesUniregs"+ANNEE+".xml"); FileUtils.writeStringToFile(fileContribuableModifie, xstream.toXML(listId));
		 */

	}

	/**
	 * Test de la methode rechercherContribuables du service fiscal.
	 *
	 * @throws Exception
	 *             si un probléme survient durant l'appel au service.
	 */
	@Ignore
	public void testRechercherContribuables() throws Exception {
		HashMap criteresRecherche = new HashMap();
		criteresRecherche.put(EnumCritereRechercheContribuable.NOM, "Villemin*");
		Collection result = serviceFiscalImpl.rechercherContribuables(criteresRecherche, 49);
		assertNotNull(result);
	}

	@Ignore
	public void testRechercherContribuables2() throws Exception {
		HashMap criteresRecherche = new HashMap();
		criteresRecherche.put(EnumCritereRechercheContribuable.NO_CONTRIBUABLE, new Integer((int) CONTRIBUABLE_INTER_57));
		Collection result = serviceFiscalImpl.rechercherContribuables(criteresRecherche, 49);
		assertNotNull(result);
	}

	@Test
	public void testModifierCodeBlocageRmbtAuto() throws Exception {

		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				eric.setNumeroCompteBancaire("CFE2145000321457");
				eric.setTitulaireCompteBancaire("Eric Edouard Bolomey");
				eric.setBlocageRemboursementAutomatique(false);
				ids.ericId = eric.getNumero();
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			serviceFiscalImpl.modifierCodeBlocageRmbtAuto(ids.ericId, true, "sipf", false);

			return null;
		}
	});


		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
		Contribuable contribuableUnireg = serviceFiscalImpl.getContribuable(ids.ericId, 2008, true, false,true);
		Assert.isTrue(contribuableUnireg.getCodeBlocageRmbtAuto());

		return null;
			}
		});

	}


	@Test
	public void testQuittanceDeclaration() throws Exception {
		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				ModeleDocument declarationComplete2008 = addModeleDocument(
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				ModeleDocument declarationVaudTax2008 = addModeleDocument(
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument("Déclaration", "210",
						declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220",
						declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230",
						declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240",
						declarationComplete2008);

				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(
						1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE,
						MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(
						2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);
				DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) eric.getDeclarationForPeriode(2008).get(0);
				EtatDeclaration expedie = new EtatDeclaration();
				expedie.setEtat(TypeEtatDeclaration.EMISE);
				expedie.setDateObtention(date(2009,1,12));
				declaration.addEtat(expedie);

				EtatDeclaration sommee = new EtatDeclaration();
				sommee.setEtat(TypeEtatDeclaration.SOMMEE);
				sommee.setDateObtention(date(2009,8,17));
				declaration.addEtat(sommee);


				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				List quittancesList = new ArrayList();
				String codeDeclaration = ids.ericId.toString() + "20080101";
				long noDeclaration = Long.parseLong(codeDeclaration);

				DeclarationQuittance noDec = new DeclarationQuittanceImpl(
						noDeclaration);
				quittancesList.add(noDec);
				serviceFiscalImpl.quittanceDeclarations(quittancesList);
				return null;
			}
		});


				PersonnePhysique eric = (PersonnePhysique) hibernateTemplate
						.get(PersonnePhysique.class, ids.ericId);

				Assert.notEmpty(eric.getDeclarationForPeriode(2008));
				DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) eric
						.getDeclarationForPeriode(2008).get(0);
				Assert.isTrue(TypeEtatDeclaration.RETOURNEE.equals(declaration.getDernierEtat().getEtat()));


	}

	@Test
	public void testQuittanceDeclarationSommee() throws Exception {
		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				ModeleDocument declarationComplete2008 = addModeleDocument(
						TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				ModeleDocument declarationVaudTax2008 = addModeleDocument(
						TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument("Déclaration", "210",
						declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220",
						declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230",
						declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240",
						declarationComplete2008);

				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(
						1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE,
						MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(
						2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);
				DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) eric.getDeclarationForPeriode(2008).get(0);
				EtatDeclaration expedie = new EtatDeclaration();
				expedie.setEtat(TypeEtatDeclaration.EMISE);
				expedie.setDateObtention(date(2009,1,12));
				declaration.addEtat(expedie);

				EtatDeclaration sommee = new EtatDeclaration();
				sommee.setEtat(TypeEtatDeclaration.SOMMEE);
				sommee.setDateObtention(RegDate.get().addDays(3));
				declaration.addEtat(sommee);


				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				List quittancesList = new ArrayList();
				String codeDeclaration = ids.ericId.toString() + "20080101";
				long noDeclaration = Long.parseLong(codeDeclaration);

				DeclarationQuittance noDec = new DeclarationQuittanceImpl(
						noDeclaration);
				quittancesList.add(noDec);
				serviceFiscalImpl.quittanceDeclarations(quittancesList);
				return null;
			}
		});


				PersonnePhysique eric = (PersonnePhysique) hibernateTemplate
						.get(PersonnePhysique.class, ids.ericId);

				Assert.notEmpty(eric.getDeclarationForPeriode(2008));
				DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) eric
						.getDeclarationForPeriode(2008).get(0);
				Assert.isTrue(TypeEtatDeclaration.RETOURNEE.equals(declaration.getDernierEtat().getEtat()));
				Assert.isTrue(haveEtatSommeeAnnulee(declaration));


	}

	@Test
	public void testModifierInformationsPersonnelles() throws Exception {
		class Ids {
			Long ericId;
			Long johnId;
		}

		final Ids ids = new Ids();

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				PeriodeFiscale periode2008 = addPeriodeFiscale(2008);
				ModeleDocument declarationComplete2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2008);
				ModeleDocument declarationVaudTax2008 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2008);
				addModeleFeuilleDocument("Déclaration", "210", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 1", "220", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 2-3", "230", declarationComplete2008);
				addModeleFeuilleDocument("Annexe 4-5", "240", declarationComplete2008);


				// Un tiers tout ce quil y a de plus ordinaire
				PersonnePhysique eric = addNonHabitant("Eric", "Bolomey", date(1965, 4, 13), Sexe.MASCULIN);
				ids.ericId = eric.getNumero();
				addForPrincipal(eric, date(1983, 4, 13), MotifFor.MAJORITE, MockCommune.Lausanne);
				addDeclarationImpot(eric, periode2008, date(2008, 1, 1), date(2008, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE,
						declarationComplete2008);

				return null;
			}
		});

		// Création d'un contribuable ordinaire et de sa DI
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			PersonnePhysique eric = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			Assert.isNull(eric.getAdresseCourrierElectronique());
			Assert.isNull(eric.getNumeroCompteBancaire());
			Assert.isNull(eric.getTitulaireCompteBancaire());
			Assert.isNull(eric.getNumeroTelephonePrive());
			Assert.isNull(eric.getNumeroTelephonePortable());
			Assert.notEmpty(eric.getDeclarationForPeriode(2008));
			DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) eric.getDeclarationForPeriode(2008).get(0);
			Assert.isTrue(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH.equals( declaration.getModeleDocument().getTypeDocument()));


		return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

			ContribuableRetourInfoDiImpl contribuableRetour = new ContribuableRetourInfoDiImpl();
			contribuableRetour.setNoContribuable(ids.ericId.intValue());
			contribuableRetour.setEmail("zuzu@gmail.com");
			contribuableRetour.setIban("CFE2145000321457");
			contribuableRetour.setNoMobile("0789651243");
			contribuableRetour.setNoTelephone("0215478936");
			contribuableRetour.setTitulaireCompte("Famille devel");
			contribuableRetour.setTypeImposition(EnumTypeImposition.ELECTRONIQUE);
			contribuableRetour.setAnneeFiscale((short) 2008);
			contribuableRetour.setNoImpotAnnee((short) 1);
			serviceFiscalImpl.modifierInformationsPersonnelles(contribuableRetour, false);

			return null;
			}
		});


		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
		PersonnePhysique eric = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.ericId);
			Assert.isTrue(eric.getAdresseCourrierElectronique().equals("zuzu@gmail.com"));
			Assert.isTrue(eric.getNumeroCompteBancaire().equals("CFE2145000321457"));
			Assert.isTrue(eric.getTitulaireCompteBancaire().equals("Famille devel"));
			Assert.isTrue(eric.getNumeroTelephonePrive().equals("0215478936"));
			Assert.isTrue(eric.getNumeroTelephonePortable().equals("0789651243"));
			Assert.notEmpty(eric.getDeclarationForPeriode(2008));
			 DeclarationImpotOrdinaire declaration = (DeclarationImpotOrdinaire) eric.getDeclarationForPeriode(2008).get(0);
			Assert.isTrue(TypeDocument.DECLARATION_IMPOT_VAUDTAX.equals( declaration.getModeleDocument().getTypeDocument()));
		return null;
			}
		});



	}






	private void chargeContribuableSansDi() throws Exception {
		File fileContribuableSansDI = new File("contribuableSansDIHost2006.xml");
		String xml = FileUtils.readFileToString(fileContribuableSansDI);
		List<Integer> listeContribuablesSansDI = (List<Integer>) xstream.fromXML(xml);
		for (Integer i : listeContribuablesSansDI) {
			migrator.migreHabitant(i.intValue(), i.intValue());
		}
	}

	@Override
	protected void indexData() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void removeIndexData() throws Exception {
		// TODO Auto-generated method stub

	}
	private boolean haveEtatSommeeAnnulee(Declaration declaration){
		for (EtatDeclaration etat : declaration.getEtats()) {
			if (TypeEtatDeclaration.SOMMEE.equals(etat.getEtat()) && etat.isAnnule()) {
				return true;
			}
		}
		return false;
	}
}
