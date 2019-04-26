package ch.vd.unireg.declaration;


import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesEtSNCException;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCService;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCServiceImpl;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static ch.vd.unireg.common.WithoutSpringTest.date;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class LienAssociesSNCServiceTest {

	@InjectMocks
	private LienAssociesSNCService service = new LienAssociesSNCServiceImpl();

	@Mock
	private TiersService tiersService;

	@Mock
	private MessageHelper messageHelper;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		Mockito.when(messageHelper.getMessage(Mockito.any(String.class), Mockito.any(String.class))).thenReturn("Test message");
	}


	/**
	 * Vérifie qu'un établissement ne peut pas servir de sujet (associé), pour les rapports entre tiers Associé - SNC .
	 */
	@Test
	public void testIsAllowedAssociateTypeNonAcceptableDebiteurIS() throws Exception {
		final DebiteurPrestationImposable objet = getDebiteurIS();
		final DebiteurPrestationImposable sujet = getDebiteurIS();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_ASSOCIE);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_associe." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}

	@Test
	public void testIsAllowedAssociateTypeNonAcceptableCollectiviteAdministrative() throws Exception {
		final Etablissement objet = getEtablissement();
		final CollectiviteAdministrative sujet = getCollectiviteAdministrative();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_ASSOCIE);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_associe." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}

	@Test
	public void testIsAllowedAssociateTypeNonAcceptableAutreCommunaute() throws Exception {
		final Etablissement objet = getEtablissement();
		final AutreCommunaute sujet = getAutreCommunaute();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_ASSOCIE);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_associe." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(sujet.getNumero())));
		}
	}

	/**
	 * Vérifie qu'un établissement ne peut pas servir d'objet (SNC), pour les rapports entre tiers Associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetTypeNonAcceptable() {
		final Etablissement objet = getEtablissement();
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}


	/**
	 * Vérifie qu'une PM non SNC, n'est pas autorisée, pour les rapports associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetNonSNC() throws Exception {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.SUCCESSION);
		objet.addForFiscal(dernierForSnc);

		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.TIERS_PAS_SNC);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}

	/**
	 * Vérifie qu'un PP <--> PP  ne sont  pas autorisée, pour les rapports associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetNonSNC_PP() throws Exception {
		final PersonnePhysique objet = getPersonnePhysique();
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}

	/**
	 * Vérifie qu'un PP <--> MenageComun  ne sont  pas autorisée, pour les rapports associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetNonSNC_MenageCommun() throws Exception {
		final MenageCommun objet = getMenageCommun();
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}


	/**
	 * Vérifie qu'un PP <--> AutreCommunaute  ne sont  pas autorisée, pour les rapports associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetNonSNC_AutreCommunaute() throws Exception {
		final AutreCommunaute objet = getAutreCommunaute();
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}

	/**
	 * Vérifie qu'un PP <--> CollectiviteAdministrative  ne sont  pas autorisée, pour les rapports associé - SNC .
	 */
	@Test
	public void testIsAllowedObjetNonSNC_CollectiviteAdministrative() throws Exception {
		final CollectiviteAdministrative objet = getCollectiviteAdministrative();
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.MAUVAIS_TYPE_SNC);
			Assert.assertEquals(ex.getMessage(), messageHelper.getMessage("error.mauvais_type_snc." + sujet.getClass().getSimpleName(), FormatNumeroHelper.numeroCTBToDisplay(objet.getNumero())));
		}
	}


	/**
	 * Vérifie qu'on ne peut pas insérer de doublon de rapports entre tiers.
	 */
	@Test
	public void testIsAllowedExistRapport() throws Exception {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.REVENU_FORTUNE);
		objet.addForFiscal(dernierForSnc);
		final PersonnePhysique sujet = getPersonnePhysique();
		try {
			Mockito.when(tiersService.existRapportEntreTiers(Mockito.any(TypeRapportEntreTiers.class), Mockito.any(Contribuable.class), Mockito.any(Contribuable.class), Mockito.any())).thenReturn(Boolean.TRUE);
			service.isAllowed(sujet, objet, null);
			fail();
		}
		catch (LienAssociesEtSNCException ex) {
			Assert.assertEquals(ex.getErreur(), LienAssociesEtSNCException.EnumErreurLienAssocieSNC.CHEVAUCHEMENT_LIEN);
		}
	}

	/**
	 * Vérifie la création d'un rapport entre une Entreprise  et une PP.
	 */
	@Test
	public void testIsAllowedPP() throws Exception {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.REVENU_FORTUNE);
		objet.addForFiscal(dernierForSnc);
		final PersonnePhysique sujet = getPersonnePhysique();
		Mockito.when(tiersService.existRapportEntreTiers(Mockito.any(TypeRapportEntreTiers.class), Mockito.any(Contribuable.class), Mockito.any(Contribuable.class), Mockito.any(RegDate.class))).thenReturn(Boolean.FALSE);
		Assert.assertTrue(service.isAllowed(sujet, objet, date(2018, 12, 31)));

	}

	/**
	 * Vérifie la création d'un rapport entre une Entreprise  et une PM.
	 */
	@Test
	public void testIsAllowedPM() throws Exception {
		final Entreprise objet = new Entreprise();
		objet.setNumero(1234L);
		final ForFiscalPrincipal dernierForSnc = getForFiscalPrincipal(GenreImpot.REVENU_FORTUNE);
		objet.addForFiscal(dernierForSnc);
		final Entreprise sujet = new Entreprise();
		Mockito.when(tiersService.existRapportEntreTiers(Mockito.any(TypeRapportEntreTiers.class), Mockito.any(Contribuable.class), Mockito.any(Contribuable.class), Mockito.any(RegDate.class))).thenReturn(Boolean.FALSE);
		Assert.assertTrue(service.isAllowed(sujet, objet, date(2018, 12, 31)));
	}

	@NotNull
	private ForFiscalPrincipal getForFiscalPrincipal(GenreImpot genreImpot) {
		final ForFiscalPrincipal ffp = new ForFiscalPrincipalPM();
		ffp.setGenreImpot(genreImpot);
		ffp.setDateDebut(date(2000, 1, 1));
		ffp.setDateFin(date(2018, 12, 31));
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
		return ffp;
	}


	@NotNull
	private PersonnePhysique getPersonnePhysique() {
		final PersonnePhysique sujet = new PersonnePhysique();
		sujet.setNumero(5678L);
		return sujet;
	}

	@NotNull
	private MenageCommun getMenageCommun() {
		final MenageCommun sujet = new MenageCommun();
		sujet.setNumero(5678L);
		return sujet;
	}

	@NotNull
	private AutreCommunaute getAutreCommunaute() {
		final AutreCommunaute sujet = new AutreCommunaute();
		sujet.setNumero(5678L);
		return sujet;
	}

	@NotNull
	private CollectiviteAdministrative getCollectiviteAdministrative() {
		final CollectiviteAdministrative sujet = new CollectiviteAdministrative();
		sujet.setNumero(5678L);
		return sujet;
	}

	@NotNull
	private Etablissement getEtablissement() {
		final Etablissement objet = new Etablissement();
		objet.setNumero(1234L);
		return objet;
	}

	private DebiteurPrestationImposable getDebiteurIS() {
		final DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(7L);
		return dpi;

	}

}