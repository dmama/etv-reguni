package ch.vd.unireg.registrefoncier.communaute;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.unireg.common.NomPrenomDates;
import ch.vd.unireg.registrefoncier.CommunauteRFPrincipalInfo;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.tiers.TiersService;

import static ch.vd.unireg.common.WithoutSpringTest.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModeleCommunauteViewTest {

	/**
	 * [SIFISC-30135] Vérifie que l'algorithme qui détermine le principal courant ne tient pas compte des principaux annulés.
	 */
	@Test
	public void testPrincipalCourantAvecPrincipauxAnnules() {

		final TiersService tiersService = Mockito.mock(TiersService.class);
		final RegistreFoncierService registreFoncierService = Mockito.mock(RegistreFoncierService.class);

		final PersonnePhysiqueRF elisabeth = new PersonnePhysiqueRF();
		elisabeth.setId(1L);
		elisabeth.setPrenom("Elisabeth");
		final PersonnePhysiqueRF michel = new PersonnePhysiqueRF();
		michel.setId(2L);
		michel.setPrenom("Michel");
		final PersonnePhysiqueRF marie = new PersonnePhysiqueRF();
		marie.setId(3L);
		marie.setPrenom("Marie");
		final PersonnePhysiqueRF catherine = new PersonnePhysiqueRF();
		catherine.setId(4L);
		catherine.setPrenom("Catherine");

		final ModeleCommunauteRF modele = new ModeleCommunauteRF();
		modele.setId(111L);
		modele.addMembre(elisabeth);
		modele.addMembre(michel);
		modele.addMembre(marie);
		modele.addMembre(catherine);
		modele.setMembresHashCode(ModeleCommunauteRF.hashCode(modele.getMembres()));
		modele.setRegroupements(Collections.emptySet());

		// retourne une liste de principaux dont les deux derniers sont annulés
		Mockito.when(registreFoncierService.buildPrincipalHisto(modele, true, false)).thenReturn(Arrays.asList(
				new CommunauteRFPrincipalInfo(1L, elisabeth.getId(), null, null, null, 12345678L, true),
				new CommunauteRFPrincipalInfo(1L, michel.getId(), date(2017, 1, 1), null, new Date(), 12345679L, false),
				new CommunauteRFPrincipalInfo(1L, catherine.getId(), date(2018, 1, 1), null, new Date(), 12345680L, false)
		));

		Mockito.when(registreFoncierService.getDecompositionNomPrenomDateNaissanceRF(Mockito.any(TiersRF.class))).thenReturn(new NomPrenomDates("test", "test", null, null));
		Mockito.when(registreFoncierService.getAyantDroit(elisabeth.getId())).thenReturn(elisabeth);
		Mockito.when(registreFoncierService.getAyantDroit(michel.getId())).thenReturn(michel);
		Mockito.when(registreFoncierService.getAyantDroit(marie.getId())).thenReturn(marie);
		Mockito.when(registreFoncierService.getAyantDroit(catherine.getId())).thenReturn(catherine);

		// on créé la vue
		final ModeleCommunauteView view = new ModeleCommunauteView(modele, tiersService, registreFoncierService);

		// [SIFISC-30135] le principal courant devrait être Catherine (par défaut) car tous les autres sont annulés
		final PrincipalCommunauteRFView principalCourant = view.getPrincipalCourant();
		assertNotNull(principalCourant);
		assertEquals(elisabeth.getId(), principalCourant.getPrincipal().getId());
		assertTrue(principalCourant.isParDefaut());
	}
}