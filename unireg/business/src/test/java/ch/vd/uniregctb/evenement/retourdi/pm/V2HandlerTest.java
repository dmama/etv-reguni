package ch.vd.uniregctb.evenement.retourdi.pm;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.xml.event.taxation.ibc.v2.AddressInformation;
import ch.vd.unireg.xml.event.taxation.ibc.v2.InformationMandataire;
import ch.vd.unireg.xml.event.taxation.ibc.v2.InformationPersonneMoraleModifiee;
import ch.vd.unireg.xml.event.taxation.ibc.v2.MailAddress;
import ch.vd.unireg.xml.event.taxation.ibc.v2.OrganisationMailAddressInfo;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypAdresse;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypNumeroIdeAttr;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypTelephoneAttr;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypTxtMax40Attr;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.NumeroIDEHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.jms.EsbBusinessException;

@SuppressWarnings("Duplicates")
public class V2HandlerTest extends BusinessTest {

	private CollectingRetourService retourService;
	private V2Handler handler;

	private static class CollectingRetourService implements RetourDIPMService {

		private final List<RetourDI> collected = new ArrayList<>();

		@Override
		public void traiterRetour(RetourDI retour, Map<String, String> incomingHeaders) throws EsbBusinessException {
			collected.add(retour);
		}

		public List<RetourDI> getCollected() {
			return collected;
		}
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		retourService = new CollectingRetourService();
		handler = new V2Handler();
		handler.setInfraService(getBean(ServiceInfrastructureService.class, "serviceInfrastructureService"));
		handler.setRetourService(retourService);
	}

	@Test
	public void testExtractDonneesEntreprisesAvecContactEntrepriseEtFlagAdresseModifieeFalse() throws Exception {

		final InformationPersonneMoraleModifiee info = new InformationPersonneMoraleModifiee();
		final OrganisationMailAddressInfo organisation = new OrganisationMailAddressInfo(null, "Toubidou", null, null, "Mme", "Albertine", "Martin");
		final AddressInformation addressInformation = new AddressInformation(null, null, null, "Chemin du pont", "43", null, null, "Lausanne", null, MockLocalite.Lausanne.getNPA().longValue(), null, MockLocalite.Lausanne.getNoOrdre(), null, null);
		info.setAdresseCourrierStructuree(new MailAddress(organisation, null, addressInformation, Boolean.FALSE));

		final InformationsEntreprise infoEntreprise = handler.extractInformationsEntreprise(info);
		Assert.assertNotNull(infoEntreprise);
		Assert.assertEquals("Mme Albertine Martin", infoEntreprise.getAdresseCourrier().getContact());
		Assert.assertNotNull(infoEntreprise.getAdresseCourrier().getDestinataire());
		Assert.assertEquals("Mme Albertine Martin", infoEntreprise.getAdresseCourrier().getDestinataire().getContact());
	}

	/**
	 * [SIFISC-21693] Vérifie que le numéro de téléphone professionnel est bien extrait du retour des DIs PM.
	 */
	@Test
	public void testExtractDonneesEntreprisesAvecNumeroTelephoneProfessionnel() throws Exception {

		final InformationPersonneMoraleModifiee info = new InformationPersonneMoraleModifiee();
		info.setContactTelephoneIndicatif(new JAXBElement<>(new QName("IDE_BIDON"), TypTelephoneAttr.class, new TypTelephoneAttr("066", true, Boolean.FALSE)));
		info.setContactTelephoneNumero(new JAXBElement<>(new QName("IDE_BIDON"), TypTelephoneAttr.class, new TypTelephoneAttr("123456789", true, Boolean.FALSE)));

		final InformationsEntreprise infoEntreprise = handler.extractInformationsEntreprise(info);
		Assert.assertNotNull(infoEntreprise);
		Assert.assertEquals("066123456789", infoEntreprise.getNoTelContact());
	}

	@Test
	public void testPriseEnCompteContactMandataire() throws Exception {
		final InformationMandataire info = new InformationMandataire();
		final OrganisationMailAddressInfo organisation = new OrganisationMailAddressInfo(null, "Chez Bernard", null, null, "Monsieur", "François", "Rollin");
		final AddressInformation addressInformation = new AddressInformation(null, null, null, "Avenue de la Gare", "12", null, null, "Lausanne", null, 1003L, null, null, null, null);
		final MailAddress address = new MailAddress(organisation, null, addressInformation, Boolean.TRUE);
		info.setAdresseCourrierStructuree(address);
		final InformationsMandataire infoExtraite = handler.extractInformationsMandataire(info);
		Assert.assertNotNull(infoExtraite);
		Assert.assertNull(infoExtraite.getIdeMandataire());
		Assert.assertNotNull(infoExtraite.getAdresse());
		Assert.assertEquals("Monsieur François Rollin", infoExtraite.getContact());

		final Pair<String, Adresse> split = infoExtraite.getAdresse().split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(split);
		Assert.assertEquals("Chez Bernard", split.getLeft());
		Assert.assertEquals("Avenue de la Gare", split.getRight().getRue());
	}

	@Test
	public void testNonPriseEnCompteNumeroIDEMandataireAdresseStructuree() throws Exception {
		final String ide = "CHE-116.311.185";
		Assert.assertTrue(NumeroIDEHelper.isValid(ide));

		final InformationMandataire info = new InformationMandataire();
		final OrganisationMailAddressInfo organisation = new OrganisationMailAddressInfo(new JAXBElement<>(new QName("IDE-BIDON"), String.class, ide), "Chez Bernard", null, null, null, null, null);
		final AddressInformation addressInformation = new AddressInformation(null, null, null, "Avenue de la Gare", "12", null, null, "Lausanne", null, 1003L, null, null, null, null);
		final MailAddress address = new MailAddress(organisation, null, addressInformation, Boolean.TRUE);
		info.setAdresseCourrierStructuree(address);
		final InformationsMandataire infoExtraite = handler.extractInformationsMandataire(info);
		Assert.assertNotNull(infoExtraite);
		Assert.assertNull(infoExtraite.getIdeMandataire());             // l'IDE n'est pas extrait, même valide
		Assert.assertNotNull(infoExtraite.getAdresse());

		// mais l'adresse est bien prise en compte
		final Pair<String, Adresse> split = infoExtraite.getAdresse().split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(split);
		Assert.assertEquals("Chez Bernard", split.getLeft());
		Assert.assertEquals("Avenue de la Gare", split.getRight().getRue());
	}

	@Test
	public void testNonPriseEnCompteNumeroIDEMandataireAdresseNonStructuree() throws Exception {
		final String ide = "CHE-116.311.185";
		Assert.assertTrue(NumeroIDEHelper.isValid(ide));

    	final InformationMandataire info = new InformationMandataire();
		final JAXBElement<TypNumeroIdeAttr> ideMandataire = new JAXBElement<>(new QName("IDE_BIDON"), TypNumeroIdeAttr.class, new TypNumeroIdeAttr(ide, true, Boolean.FALSE));
		final InformationMandataire.AdresseCourrier adresseCourrier = new InformationMandataire.AdresseCourrier(ideMandataire, new TypAdresse(toTxtMax40("Chez Bernard"), toTxtMax40("Avenue de la Gare 12"), null, null, null, toTxtMax40("1003"), toTxtMax40("Lausanne")));
		info.setAdresseCourrier(adresseCourrier);

		final InformationsMandataire infoExtraite = handler.extractInformationsMandataire(info);
		Assert.assertNotNull(infoExtraite);
		Assert.assertNull(infoExtraite.getIdeMandataire());             // l'IDE n'est pas extrait, même valide
		Assert.assertNotNull(infoExtraite.getAdresse());

		// mais l'adresse est bien prise en compte
		final Pair<String, Adresse> split = infoExtraite.getAdresse().split(serviceInfra, tiersService, RegDate.get());
		Assert.assertNotNull(split);
		Assert.assertEquals("Chez Bernard", split.getLeft());
		Assert.assertEquals(MockRue.Lausanne.AvenueDeLaGare.getNoRue(), split.getRight().getNumeroRue());
	}

	private static JAXBElement<TypTxtMax40Attr> toTxtMax40(String string) {
		return new JAXBElement<>(new QName("STR_BIDON"), TypTxtMax40Attr.class, null, new TypTxtMax40Attr(string, true, Boolean.FALSE));
	}
}
