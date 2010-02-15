package ch.vd.uniregctb.evenement.fiscal;

import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.test.annotation.ExpectedException;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.evenement.EvenementFiscal;
import ch.vd.uniregctb.evenement.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.EvenementFiscalFor;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;


public class EvenementFiscalServiceTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/evenement/fiscal/EvenementFiscalServiceTest.xml";
	private final static Long NUMERO_CONTRIBUABLE = 12300002L;
	private final static Long NUMERO_DEBITEUR = 12500001L;


	private EvenementFiscalServiceImpl evenementFiscalService;
	private EvenementFiscalFacade evenementFiscalFacade;
	private EvenementFiscalDAO evenementFiscalDAO;


	private TiersDAO tiersDAO;

	public EvenementFiscalServiceTest() {
		super();
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementFiscalService = new EvenementFiscalServiceImpl();
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		evenementFiscalService.setEvenementFiscalDAO(evenementFiscalDAO);
		evenementFiscalFacade = EasyMock.createMock(EvenementFiscalFacade.class);
		evenementFiscalService.setEvenementFiscalFacade(evenementFiscalFacade);

		final ParametreAppService parametres = getBean(ParametreAppService.class, "parametreAppService");
		evenementFiscalService.setParametres(parametres);

		loadDatabase(DB_UNIT_DATA_FILE);
		tiersDAO = getBean( TiersDAO.class, "tiersDAO");
	}

	/**
	 *
	 * @throws Exception
	 */
	//@Test
	@ExpectedException(IllegalArgumentException.class)
	public void publierEvenementTiersArgumentNull() throws Exception {
		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.OUVERTURE_FOR;
		evenementFiscalDAO.creerEvenementFor(null, typeEvenement, RegDate.get(), MotifFor.ARRIVEE_HS, null,new Long(1));
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	@ExpectedException(IllegalArgumentException.class)
	public void publierEvenementTypeEvenementArgumentNull() throws Exception {
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		assertNotNull(tiers);
		TypeEvenementFiscal typeEvenement = null;
		evenementFiscalDAO.creerEvenementFor(tiers, typeEvenement, RegDate.get(), MotifFor.ARRIVEE_HS, null, new Long(1));
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	@ExpectedException(IllegalArgumentException.class)
	public void publierEvenementDateArgumentNull() throws Exception {
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		assertNotNull(tiers);
		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.OUVERTURE_FOR;
		evenementFiscalDAO.creerEvenementFor(tiers, typeEvenement, null, MotifFor.ARRIVEE_HS, null, new Long(1));
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void publierEvenement() throws Exception {
		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.OUVERTURE_FOR;
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, typeEvenement, RegDate.get(), MotifFor.ARRIVEE_HS, null, new Long(1));

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscalFor) EasyMock.anyObject());
		replay(evenementFiscalFacade);

		evenementFiscalService.publierEvenementFiscal(evenementFiscal);
		Assert.assertEquals(1, evenementFiscalDAO.getAll().size());

	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	@ExpectedException(RuntimeException.class)
	public void publierEvenementNullArgument() throws Exception {
		evenementFiscalService.publierEvenementFiscal(null);
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	@ExpectedException(RuntimeException.class)
	public void publierEvenementEvenementFiscalException() throws Exception {
		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.OUVERTURE_FOR;
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		assertNotNull(tiers);
		EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, typeEvenement, RegDate.get(), MotifFor.ARRIVEE_HS, null, new Long(1));

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscal) EasyMock.anyObject());
		EasyMock.expectLastCall().andThrow(new EvenementFiscalException( "unexpected exception"));
		replay(evenementFiscalFacade);

		evenementFiscalService.publierEvenementFiscal(evenementFiscal);
	}

	@Test
	@ExpectedException(RuntimeException.class)
	public void publierEvenementEvenementFiscalException2() throws Exception {
		TypeEvenementFiscal typeEvenement = TypeEvenementFiscal.OUVERTURE_FOR;
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);
		assertNotNull(tiers);
		EvenementFiscal evenementFiscal = evenementFiscalDAO.creerEvenementFor(tiers, typeEvenement, RegDate.get(), MotifFor.ARRIVEE_HS, null, new Long(1));

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscal) EasyMock.anyObject());
		EasyMock.expectLastCall().andThrow(new EvenementFiscalException());
		replay(evenementFiscalFacade);

		evenementFiscalService.publierEvenementFiscal(evenementFiscal);
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void publierEvenementFiscalRetourLR() throws Exception {
		DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(NUMERO_DEBITEUR);

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscal) EasyMock.anyObject());
		replay(evenementFiscalFacade);

		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(RegDate.get());
		lr.setDateFin(RegDate.get());
		evenementFiscalService.publierEvenementFiscalRetourLR(debiteur, lr, RegDate.get());

		Assert.assertEquals(1, evenementFiscalDAO.getAll().size());
		EvenementFiscal evenementFiscal = evenementFiscalDAO.getAll().get(0);
		Assert.assertEquals(TypeEvenementFiscal.RETOUR_LR, evenementFiscal.getType());
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void publierEvenementFiscalOuverturePeriodeDecompteLR() throws Exception {
		DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersDAO.get(NUMERO_DEBITEUR);

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscal) EasyMock.anyObject());
		replay(evenementFiscalFacade);

		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(RegDate.get());
		lr.setDateFin(RegDate.get());
		evenementFiscalService.publierEvenementFiscalOuverturePeriodeDecompteLR(debiteur, lr, RegDate.get());

		Assert.assertEquals(1, evenementFiscalDAO.getAll().size());
		EvenementFiscal evenementFiscal = evenementFiscalDAO.getAll().get(0);
		Assert.assertEquals(TypeEvenementFiscal.OUVERTURE_PERIODE_DECOMPTE_LR, evenementFiscal.getType());
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void publierEvenementFiscalOuvertureFor() throws Exception {
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscal) EasyMock.anyObject());
		replay(evenementFiscalFacade);

		evenementFiscalService.publierEvenementFiscalOuvertureFor(tiers, RegDate.get(), MotifFor.ARRIVEE_HS, new Long(1));
		Assert.assertEquals(1, evenementFiscalDAO.getAll().size());
		EvenementFiscal evenementFiscal = evenementFiscalDAO.getAll().get(0);
		Assert.assertEquals(TypeEvenementFiscal.OUVERTURE_FOR, evenementFiscal.getType());
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void publierEvenementFiscalFermetureFor() throws Exception {
		Tiers tiers = tiersDAO.get(NUMERO_CONTRIBUABLE);

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscal) EasyMock.anyObject());
		replay(evenementFiscalFacade);

		evenementFiscalService.publierEvenementFiscalFermetureFor(tiers, RegDate.get(), MotifFor.DEPART_HS, new Long(1));
		Assert.assertEquals(1, evenementFiscalDAO.getAll().size());
		EvenementFiscal evenementFiscal = evenementFiscalDAO.getAll().get(0);
		Assert.assertEquals(TypeEvenementFiscal.FERMETURE_FOR, evenementFiscal.getType());
	}

	/**
	 *
	 * @throws Exception
	 */
	@Test
	public void publierEvenementFiscalChangementSituation() throws Exception {
		Contribuable contribuable = (Contribuable) tiersDAO.get(NUMERO_CONTRIBUABLE);

		//add behavior to mock
		evenementFiscalFacade.publierEvenement((EvenementFiscal) EasyMock.anyObject());
		replay(evenementFiscalFacade);

		evenementFiscalService.publierEvenementFiscalChangementSituation(contribuable, RegDate.get(), new Long(1));
		Assert.assertEquals(1, evenementFiscalDAO.getAll().size());
		EvenementFiscal evenementFiscal = evenementFiscalDAO.getAll().get(0);
		Assert.assertEquals(TypeEvenementFiscal.CHANGEMENT_SITUATION_FAMILLE, evenementFiscal.getType());
	}

}
