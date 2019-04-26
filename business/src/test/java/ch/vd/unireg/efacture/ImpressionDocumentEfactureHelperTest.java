package ch.vd.unireg.efacture;

import java.math.BigInteger;
import java.util.Date;

import noNamespace.InfoEnteteDocumentDocument1;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoiDetaillee;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseGenerique;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.TypeAdresseFiscale;
import ch.vd.unireg.common.NpaEtLocalite;
import ch.vd.unireg.common.RueEtNumero;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.impl.LegacyEditiqueHelperImpl;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.data.OfficeImpotImpl;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ImpressionDocumentEfactureHelperTest {

	private ImpressionDocumentEfactureHelperImpl helper;
	private LegacyEditiqueHelperImpl legacyHelper;
	private AdresseService adresseService;
	private ServiceInfrastructureService infraService;

	@Before
	public void setUp() throws Exception {

		adresseService = Mockito.mock(AdresseService.class);
		infraService = Mockito.mock(ServiceInfrastructureService.class);

		legacyHelper = new LegacyEditiqueHelperImpl();
		legacyHelper.setAdresseService(adresseService);
		legacyHelper.setInfraService(infraService);

		helper = new ImpressionDocumentEfactureHelperImpl();
		helper.setLegacyEditiqueHelper(legacyHelper);
	}

	/**
	 * [SIFISC-30787] Ce test vérifie que le texte "Centre d'appels téléphoniques" apparaît dans le bloc "Affaire traitée par"
	 */
	@Test
	public void testRemplitEnteteDocumentEFacture() throws EditiqueException, AdresseException {

		final AdresseEnvoiDetaillee adresseEnvoi = new AdresseEnvoiDetaillee(null, null, null, null, null, new RueEtNumero("Route de Berne", "46"), new NpaEtLocalite("1014", "Lausanne Adm cant"), null, null, null, AdresseGenerique.SourceType.INFRA, null);
		Mockito.when(adresseService.getAdresseEnvoi(Mockito.any(Tiers.class), Mockito.any(), Mockito.any(TypeAdresseFiscale.class), Mockito.anyBoolean())).thenReturn(adresseEnvoi);

		final Adresse adresse = new MockAdresse("Route de Berne", "46", "1014", "Lausanne Adm cant");
		final OfficeImpotImpl cat = Mockito.mock(OfficeImpotImpl.class);
		Mockito.when(cat.getAdresse()).thenReturn(adresse);
		Mockito.when(cat.getNoTelephone()).thenReturn("021'316'00'00");
		Mockito.when(infraService.getCAT()).thenReturn(cat);

		final PersonnePhysique pp = new PersonnePhysique(10000001L);
		final Date dateTraitement = RegDate.get(2019, 3, 1).asJavaDate();
		final RegDate dateDemande = RegDate.get(2019, 1, 1);
		final ImpressionDocumentEfactureParams params = new ImpressionDocumentEfactureParams(pp,
		                                                                                     TypeDocument.E_FACTURE_ATTENTE_SIGNATURE,
		                                                                                     dateTraitement,
		                                                                                     dateDemande,
		                                                                                     BigInteger.valueOf(41100000145216997L));

		final InfoEnteteDocumentDocument1.InfoEnteteDocument entete = helper.remplitEnteteDocument(params);
		assertNotNull(entete);
		final InfoEnteteDocumentDocument1.InfoEnteteDocument.Expediteur expediteur = entete.getExpediteur();
		assertNotNull(expediteur);
		assertEquals("021'316'00'00", expediteur.getNumTelephone());
		assertEquals("Centre d'appels téléphoniques", expediteur.getTraitePar());
	}
}