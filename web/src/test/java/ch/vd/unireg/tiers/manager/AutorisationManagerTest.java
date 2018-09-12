package ch.vd.unireg.tiers.manager;

import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.unireg.common.WebTest;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.mandataire.ConfigurationMandataire;
import ch.vd.unireg.security.MockSecurityProvider;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.MockTiersService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AutorisationManagerTest extends WebTest {

	private AutorisationManager autorisationManager;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		autorisationManager = getBean(AutorisationManagerImpl.class, "autorisationManager");
	}

	// Tests sur RetourModeImpositionAllowed.INTERDIT

	@Test
	public void testIsModeImpositionAllowedInterditOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.TRUE);
		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.INTERDIT, returned);
	}

	@Test
	public void testIsModeImpositionAllowedInterditOrdinaireOK() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.TRUE);
		final Role[] roles = {Role.FOR_PRINC_ORDDEP_HAB};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);
	}

	@Test
	public void testIsModeImpositionAllowedInterditNotOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.TRUE);
		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.INTERDIT, returned);
	}

	@Test
	public void testIsModeImpositionAllowedInterditNotOrdinaireOK() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.TRUE);
		final Role[] roles = {Role.FOR_PRINC_SOURC_HAB};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);
	}

	// Tests sur RetourModeImpositionAllowed.DROITS_INCOHERENTS

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisVDOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.DROITS_INCOHERENTS, returned);
	}

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisVDOrdinaireOK() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.FOR_PRINC_ORDDEP_GRIS};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);

	}

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisVDNonOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.DROITS_INCOHERENTS, returned);

	}

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisVDNonOrdinaireOK() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.FOR_PRINC_SOURC_GRIS};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);

	}

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisNonVDOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_HC, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.DROITS_INCOHERENTS, returned);
	}

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisNonVDOrdinaireOK() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.FOR_PRINC_ORDDEP_HCHS};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_HC, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);
	}

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisNonVDNonOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_HC, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.DROITS_INCOHERENTS, returned);
	}

	@Test
	public void testIsModeImpositionAllowedDroitsIncoherentsAutFisNonVDNonOrdinaireOK() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(Boolean.FALSE);
		final Role[] roles = {Role.FOR_PRINC_SOURC_HCHS};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_HC, null, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);
	}

	// RetourModeImpositionAllowed.REGLES_INCOHERENTES

	@Test
	public void testIsModeImpositionAllowedSuisseOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(true);
		// Tiers service
		MockTiersService mockTiersService = new MockTiersService(albert);
		mockTiersService.setIsSuisse(true);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setTiersService(mockTiersService);

		// Security provider
		final Role[] roles = {Role.FOR_PRINC_ORDDEP_HAB};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		autorisationManager.setSecurityProvider(provider);

		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);
	}

	@Test
	public void testIsModeImpositionAllowedSuisseNonOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(true);
		// Tiers service
		MockTiersService mockTiersService = new MockTiersService(albert);
		mockTiersService.setIsSuisse(true);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setTiersService(mockTiersService);
		// Security provider
		final Role[] roles = {Role.FOR_PRINC_SOURC_HAB};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		autorisationManager.setSecurityProvider(provider);

		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.REGLES_INCOHERENTES, returned);
	}

	@Test
	public void testIsModeImpositionAllowedSansPermisCNonOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(true);
		// Tiers service
		MockTiersService mockTiersService = new MockTiersService(albert);
		mockTiersService.setIsSuisse(false);
		mockTiersService.setEtrangerSansPermisC(false);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setTiersService(mockTiersService);
		// Security provider
		final Role[] roles = {Role.FOR_PRINC_SOURC_HAB};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		autorisationManager.setSecurityProvider(provider);

		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.SOURCE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.REGLES_INCOHERENTES, returned);
	}

	@Test
	public void testIsModeImpositionAllowedSansPermisCOrdinaire() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(true);
		// Tiers service
		MockTiersService mockTiersService = new MockTiersService(albert);
		mockTiersService.setIsSuisse(false);
		mockTiersService.setEtrangerSansPermisC(false);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setTiersService(mockTiersService);
		// Security provider
		final Role[] roles = {Role.FOR_PRINC_ORDDEP_HAB};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		autorisationManager.setSecurityProvider(provider);

		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);
	}

	@Test
	public void testIsModeImpositionAllowedPermisC() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setHabitant(true);
		// Tiers service
		MockTiersService mockTiersService = new MockTiersService(albert);
		mockTiersService.setIsSuisse(false);
		mockTiersService.setEtrangerSansPermisC(false);
		AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setTiersService(mockTiersService);
		// Security provider
		final Role[] roles = {Role.FOR_PRINC_ORDDEP_HAB};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		autorisationManager.setSecurityProvider(provider);

		RetourModeImpositionAllowed returned = autorisationManager.isModeImpositionAllowed(albert, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, null, null, 123);

		assertEquals(RetourModeImpositionAllowed.OK, returned);
	}

	/**
	 * Ctb Hors conton non assujetti devrait pouvoir modifier les onglets fiscal et Civil Cf SIFISC-13335
	 */
	@Test
	public void testHabiliationEditionOngletFiscalEtCivilpourCTBHCNonAssuj() {
		final PersonnePhysique albert = new PersonnePhysique();
		albert.setNumero(1L);
		albert.setHabitant(Boolean.TRUE);

		final Role[] roles = {Role.MODIF_NONHAB_DEBPUR, Role.MODIF_HAB_DEBPUR, Role.VISU_ALL};
		final MockSecurityProvider provider = new MockSecurityProvider(roles);
		//AutorisationManagerImpl autorisationManager = new AutorisationManagerImpl();
		autorisationManager.setSecurityProvider(provider);
		//
		final ConfigurationMandataire configurationMandataireMock = Mockito.mock(ConfigurationMandataire.class);
		Mockito.when(configurationMandataireMock.getAffichageMandatGeneral(Mockito.any(Tiers.class))).thenReturn(ConfigurationMandataire.Acces.AUCUN);
		Mockito.when(configurationMandataireMock.getAffichageMandatTiers(Mockito.any(Tiers.class))).thenReturn(ConfigurationMandataire.Acces.AUCUN);

		final ServiceInfrastructureService infrastructureServiceMock = Mockito.mock(ServiceInfrastructureService.class);
		Mockito.when(infrastructureServiceMock.getGenresImpotMandataires()).thenReturn(Collections.emptyList());

		final Autorisations autorisations = autorisationManager.getAutorisations(albert, "iamtestuser", 22);
		assertTrue(autorisations.isDonneesFiscales());
		assertTrue(autorisations.isDonneesCiviles());
		assertTrue(autorisations.isSituationsFamille());
	}
}
