package ch.vd.unireg.common;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.AdministrationEntreprise;
import ch.vd.unireg.tiers.AnnuleEtRemplace;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.AssujettissementParSubstitution;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.ConseilLegal;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.Curatelle;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.FusionEntreprises;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.LienAssociesEtSNC;
import ch.vd.unireg.tiers.Mandat;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.Parente;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.RepresentationConventionnelle;
import ch.vd.unireg.tiers.ScissionEntreprise;
import ch.vd.unireg.tiers.SocieteDirection;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.TiersWebHelper;
import ch.vd.unireg.tiers.TransfertPatrimoine;
import ch.vd.unireg.tiers.Tutelle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class I18nMessageHelperTest extends WebTest {
	private MessageHelper messageHelper;
	private final String nomSujet = "nomsujet";
	private final String nomObjet = "nomObjet";
	private final Long sujetId = 1L;
	private final Long objetId = 2L;

	@Before
	public void setUp() throws Exception {
		this.messageHelper = getBean(MessageHelper.class, "messageHelper");
	}

	@Test
	public void testPresenceCleMessageErreurRapportSNC() throws Exception {
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + DebiteurPrestationImposable.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + MenageCommun.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_associe." + AutreCommunaute.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_snc." + DebiteurPrestationImposable.class.getSimpleName(), "01"));
		assertNotNull(messageHelper.getMessage("error.mauvais_type_snc." + AutreCommunaute.class.getSimpleName(), "01"));
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeContactImpotSource() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(ContactImpotSource.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.contactimpotsource", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeRepresentationConventionnelle() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(RepresentationConventionnelle.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.representationconventionnelle", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeCuratelle() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(Curatelle.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.curatelle", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeConseilLegal() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(ConseilLegal.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.conseillegal", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeTutelle() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(Tutelle.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.tutelle", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeAnnuleEtRemplace() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(AnnuleEtRemplace.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.annuleetremplace", nomObjet, objetId, BooleanUtils.toInteger(fermeOuAnnule), nomSujet, sujetId);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeAppartenanceMenage() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(AppartenanceMenage.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.appartenancemenage", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeRapportPrestationImposable() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(RapportPrestationImposable.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.rapportprestationimposable", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeParente() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(Parente.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.parente", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeAssujettissementParSubstitution() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(AssujettissementParSubstitution.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.assujettissementparsubstitution", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeActiviteEconomique() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(ActiviteEconomique.class);
			Mockito.when(((ActiviteEconomique) rapportMock).isPrincipal()).thenReturn(Boolean.TRUE);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.activiteeconomique", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, BooleanUtils.toInteger(((ActiviteEconomique) rapportMock).isPrincipal()));
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeMandat() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(Mandat.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.mandat", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeFusionEntreprises() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(FusionEntreprises.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.fusionentreprises", nomSujet, sujetId, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, objetId);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeScissionEntreprise() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(ScissionEntreprise.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.scissionentreprise", nomSujet, sujetId, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, objetId);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeTransfertPatrimoine() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(TransfertPatrimoine.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.transfertpatrimoine", nomSujet, sujetId, BooleanUtils.toInteger(fermeOuAnnule), nomObjet, objetId);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeAdministrationEntreprise() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(AdministrationEntreprise.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.administrationentreprise", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeSocieteDirection() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(SocieteDirection.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.societedirection", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeHeritage() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(Heritage.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.heritage", nomSujet, BooleanUtils.toInteger(fermeOuAnnule), nomObjet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPresenceCleMessageTooltipRapportEntretiersDeTypeLienAssociesEtSNC() throws Exception {
		try {
			final RapportEntreTiers rapportMock = Mockito.mock(LienAssociesEtSNC.class);
			final String tooltip = getRapportEntreTiersTooltips(rapportMock);
			final boolean fermeOuAnnule = rapportMock.isAnnule() || rapportMock.getDateFin() != null;
			final String message = messageHelper.getMessage("tooltip.rapport.entretiers.lienassociesetsnc", nomObjet, BooleanUtils.toInteger(fermeOuAnnule), nomSujet);
			assertEquals(tooltip, message);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}

	private String getRapportEntreTiersTooltips(RapportEntreTiers rapport) throws AdresseException {

		final AdresseService adresseServiceMock = Mockito.mock(AdresseService.class);
		final TiersService tiersServiceMock = Mockito.mock(TiersService.class);
		final Tiers tiersSujetMock = Mockito.mock(Tiers.class);
		final Tiers tiersObjetMock = Mockito.mock(Tiers.class);


		//preparation du mocks rapport
		Mockito.when(rapport.isAnnule()).thenReturn(Boolean.TRUE);
		Mockito.when(rapport.getSujetId()).thenReturn(sujetId);
		Mockito.when(rapport.getObjetId()).thenReturn(objetId);

		//preparation du mocks tiersService
		Mockito.when(tiersServiceMock.getTiers(sujetId)).thenReturn(tiersSujetMock);
		Mockito.when(tiersServiceMock.getTiers(objetId)).thenReturn(tiersObjetMock);
		Mockito.when(tiersSujetMock.getNumero()).thenReturn(sujetId);
		Mockito.when(tiersObjetMock.getNumero()).thenReturn(objetId);

		//preparation du mocks addresseService
		Mockito.when(adresseServiceMock.getNomCourrier(Mockito.any(Tiers.class), Mockito.any(RegDate.class), Mockito.any(Boolean.class))).thenAnswer((Answer<List<String>>) invocation ->
		{
			final Tiers tiers = invocation.getArgumentAt(0, Tiers.class);
			return tiers.getNumero().equals(sujetId) ? Collections.singletonList(nomSujet) : Collections.singletonList(nomObjet);
		});

		return TiersWebHelper.getRapportEntreTiersTooltips(rapport, adresseServiceMock, tiersServiceMock, messageHelper);
	}

}
