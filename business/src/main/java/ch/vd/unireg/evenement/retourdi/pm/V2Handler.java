package ch.vd.unireg.evenement.retourdi.pm;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.xml.event.taxation.ibc.v2.AddressInformation;
import ch.vd.unireg.xml.event.taxation.ibc.v2.DeclarationIBC;
import ch.vd.unireg.xml.event.taxation.ibc.v2.DeclarationImpots;
import ch.vd.unireg.xml.event.taxation.ibc.v2.InformationMandataire;
import ch.vd.unireg.xml.event.taxation.ibc.v2.InformationPersonneMoraleModifiee;
import ch.vd.unireg.xml.event.taxation.ibc.v2.MailAddress;
import ch.vd.unireg.xml.event.taxation.ibc.v2.OrganisationMailAddressInfo;
import ch.vd.unireg.xml.event.taxation.ibc.v2.PersonMailAddressInfo;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypAdresse;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypBooleanAttr;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypDateAttr;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypNumeroIbanAttr;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypSiegeEtAdministrationEffective;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypTelephoneAttr;
import ch.vd.unireg.xml.event.taxation.ibc.v2.TypTxtMax40Attr;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.retourdi.RetourDiHandler;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.type.FormulePolitesse;
import ch.vd.unireg.type.TexteCasePostale;

@SuppressWarnings("Duplicates")
public class V2Handler extends AbstractRetourDIHandler implements RetourDiHandler<DeclarationIBC> {

	private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
	private static final BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);

	private static final Map<String, String> CIVILITE_MAPPING = buildCiviliteMapping();

	private static Map<String, String> buildCiviliteMapping() {
		final Map<String, String> map = new HashMap<>();
		map.put("Mme", FormulePolitesse.MADAME.salutations());
		map.put("M.", FormulePolitesse.MONSIEUR.salutations());
		return Collections.unmodifiableMap(map);
	}

	private ServiceInfrastructureService infraService;

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/taxation/DeclarationIBC-2.xsd");
	}

	@Override
	public Class<DeclarationIBC> getHandledClass() {
		return DeclarationIBC.class;
	}

	@Override
	public void doHandle(DeclarationIBC document, Map<String, String> incomingHeaders) throws EsbBusinessException {
		final RetourDI retourDI = buildRetourDI(document);
		traiterRetour(retourDI, incomingHeaders);
	}

	private RetourDI buildRetourDI(DeclarationIBC ibc) {
		final DeclarationImpots di = ibc.getDeclarationImpots();
		final InformationsEntreprise entreprise;
		final InformationsMandataire mandataire;
		if (di != null) {
			entreprise = extractInformationsEntreprise(di.getInformationPersonneMoraleModifiee());
			mandataire = extractInformationsMandataire(di.getInformationMandataire());
		}
		else {
			entreprise = null;
			mandataire = null;
		}
		return new RetourDI(ibc.getNoContribuable().longValue(), ibc.getPeriode().intValue(), ibc.getNoSequenceDi().intValue(), entreprise, mandataire);
	}

	@Nullable
	protected InformationsEntreprise extractInformationsEntreprise(InformationPersonneMoraleModifiee info) {
		if (info == null) {
			return null;
		}
		final RegDate dateFinExCommercial = isTrue(extractBoolean(info.getExerciceCommercialModifie())) ? extractDate(info.getExerciceCommercialFin()) : null;
		final String indicatifTel = extractTelephone(info.getContactTelephoneIndicatif());
		final String numeroTel = extractTelephone(info.getContactTelephoneNumero());
		return new InformationsEntreprise(dateFinExCommercial,
		                                  extractAdresse(info.getAdresseCourrier(), info.getAdresseCourrierStructuree(), true),
		                                  extractLocalisation(extractStringMax40(info.getSiege()), info.getSiegeStructure()),
		                                  extractLocalisation(extractStringMax40(info.getAdministrationEffective()), info.getAdministrationEffectiveStructuree()),
		                                  extractStringIban(info.getCompteNumeroIban()),
		                                  extractStringMax40(info.getCompteTitulaire()),
		                                  extractNoTel(indicatifTel, numeroTel));
	}

	@Nullable
	private static Localisation extractLocalisation(String plain, TypSiegeEtAdministrationEffective structure) {
		// pas de structure...
		if (structure == null) {
			if (StringUtils.isBlank(plain)) {
				return null;
			}
			return new Localisation.SaisieLibre(plain);
		}

		// nous avons une structure
		final BigInteger numeroOfsCommuneSuisse = structure.getNumeroOfsCommuneSuisse();
		if (numeroOfsCommuneSuisse != null) {
			// commune suisse
			final Integer ofs = toInteger(numeroOfsCommuneSuisse);
			return ofs != null ? new Localisation.CommuneSuisse(ofs) : null;
		}

		// c'est donc un pays étranger
		final BigInteger numeroOfsPays = structure.getNumeroOfsPaysEtranger();
		final Integer ofsPays = toInteger(numeroOfsPays);
		return ofsPays != null ? new Localisation.Etranger(ofsPays, structure.getLocalitePaysEtranger()) : null;
	}

	@Nullable
	private AdresseRaisonSociale extractAdresse(TypAdresse libre, MailAddress structuree, boolean seulementAdresseModifiee) {
		if (structuree == null) {
			// il s'agit d'une adresse libre...
			if (libre == null) {
				// vraiment rien à voir...
				return null;
			}
			final AdresseRaisonSociale.Brutte brutte = new AdresseRaisonSociale.Brutte(extractStringMax40(libre.getAdresseLigne1()),
			                                                                           extractStringMax40(libre.getAdresseLigne2()),
			                                                                           extractStringMax40(libre.getAdresseLigne3()),
			                                                                           extractStringMax40(libre.getAdresseLigne4()),
			                                                                      null,
																					   extractStringMax40(libre.getPersonneContact()),
			                                                                           extractStringMax40(libre.getAdresseNpa()),
			                                                                           extractStringMax40(libre.getAdresseLocalite()));
			return brutte.isEmpty() ? null : brutte;
		}

		// [SIFISC-21192] données du destinataire d'abord... (même si le flag "seulementAdresseModifiée" n'est pas levé)
		final DestinataireAdresse destinataire = extractDestinataire(structuree);

		// structurée non-indiquée comme modifiée, et on nous demande de faire attention...
		final AddressInformation ai = structuree.getAddressInformation();
		if ((seulementAdresseModifiee && !isTrue(structuree.isAdresseModifee())) || ai == null) {
			if (destinataire == null) {
				return null;
			}
			return new AdresseRaisonSociale.DestinataireSeulement(destinataire);
		}

		// Suisse ou pas Suisse ?
		if (ServiceInfrastructureService.SIGLE_SUISSE.equalsIgnoreCase(ai.getCountry()) || (ai.getCountry() == null && ai.getForeignZipCode() == null)) {
			// Suisse...
			return new AdresseRaisonSociale.StructureeSuisse(destinataire,
			                                                 ai.getAddressLine1(),
			                                                 ai.getAddressLine2(),
			                                                 toInteger(ai.getEstrid()),
			                                                 ai.getStreet(),
			                                                 ai.getHouseNumber(),
			                                                 extractCasePostale(ai.getPostOfficeBoxNumber(), ai.getPostOfficeBoxText()),
			                                                 ai.getTown(),
			                                                 toInteger(ai.getSwissZipCode()),
			                                                 ai.getSwissZipCodeAddOn(),
			                                                 ai.getSwissZipCodeId());
		}
		else {
			// Etranger...
			final Pays pays = ai.getCountry() == null ? null : infraService.getPays(ai.getCountry(), null);
			return new AdresseRaisonSociale.StructureeEtranger(destinataire,
			                                                   ai.getAddressLine1(),
			                                                   ai.getAddressLine2(),
			                                                   ai.getStreet(),
			                                                   ai.getHouseNumber(),
			                                                   extractCasePostale(ai.getPostOfficeBoxNumber(), ai.getPostOfficeBoxText()),
			                                                   ai.getTown(),
			                                                   ai.getForeignZipCode(),
			                                                   pays);
		}
	}

	@Nullable
	private static CasePostale extractCasePostale(@Nullable Long poBoxNumber, @Nullable String poBoxText) {
		if (poBoxNumber == null && StringUtils.isBlank(poBoxText)) {
			return null;
		}
		return new CasePostale(TexteCasePostale.parse(poBoxText), toInteger(poBoxNumber));
	}

	@Nullable
	private static DestinataireAdresse extractDestinataire(MailAddress adresse) {
		if (adresse == null) {
			return null;
		}
		final OrganisationMailAddressInfo organisation = adresse.getOrganisation();
		if (organisation != null) {
			final String contactTitle = organisation.getTitle();
			final String civilite = CIVILITE_MAPPING.getOrDefault(contactTitle, contactTitle);
			final String contact = buildContact(civilite, organisation.getFirstName(), organisation.getLastName());
			final DestinataireAdresse.Organisation org = new DestinataireAdresse.Organisation(extractFromElement(organisation.getNumeroIde()),
			                                                                                  organisation.getOrganisationName(),
			                                                                                  organisation.getOrganisationNameAddOn1(),
			                                                                                  organisation.getOrganisationNameAddOn2(),
			                                                                                  contact);
			return org.isEmpty() ? null : org;
		}
		final PersonMailAddressInfo person = adresse.getPerson();
		if (person != null) {
			final String title = person.getTitle();
			final String civilite = CIVILITE_MAPPING.getOrDefault(title, title);
			final DestinataireAdresse.Personne prsn = new DestinataireAdresse.Personne(person.getNumeroAvs(),
			                                                                           person.getFirstName(),
			                                                                           person.getLastName(),
			                                                                           civilite);
			return prsn.isEmpty() ? null : prsn;
		}
		return null;
	}

	@Nullable
	private static String buildContact(String title, String firstname, String lastname) {
		// en présence d'un nom, on concatène le titre, le prénom et le nom
		if (StringUtils.isBlank(lastname)) {
			return null;
		}
		final List<String> elements = new ArrayList<>(3);
		final String[] array = { title, firstname, lastname };
		for (String element : array) {
			if (StringUtils.isNotBlank(element)) {
				elements.add(element.trim());
			}
		}
		return StringUtils.trimToNull(CollectionsUtils.toString(elements, StringRenderer.DEFAULT, " ", null));
	}

	@Nullable
	protected InformationsMandataire extractInformationsMandataire(InformationMandataire info) {
		if (info == null) {
			return null;
		}
		final String indicatifTel = extractTelephone(info.getContactMandataireTelephoneIndicatif());
		final String noTel = extractTelephone(info.getContactMandataireTelephoneNumero());
		final InformationMandataire.AdresseCourrier adresseMandataire = info.getAdresseCourrier();
		final String ideMandataire = null;      // Consigne PMD 07.11.2016: one n'utilise pas ce numéro IDE (attention si un jour on doit l'utiliser, il peut être à deux endroits : dans l'adresse courrier et dans l'organisation de l'adresse structurée)
		final TypAdresse adresse = adresseMandataire != null ? adresseMandataire.getAdresseMandataire() : null;
		return new InformationsMandataire(ideMandataire,
		                                  extractAdresse(adresse, info.getAdresseCourrierStructuree(), false),
		                                  extractBoolean(info.getSansCopieMandataire()),
		                                  extractNoTel(indicatifTel, noTel));
	}

	private static boolean isTrue(@Nullable Boolean bool) {
		return bool != null && bool;
	}

	@Nullable
	private static Boolean extractBoolean(TypBooleanAttr element) {
		if (element == null || !element.isValide()) {
			return null;
		}
		return element.isValue() ? Boolean.TRUE : Boolean.FALSE;
	}

	@Nullable
	private static RegDate extractDate(JAXBElement<TypDateAttr> element) {
		final TypDateAttr date = extractFromElement(element);
		if (date == null || !date.isValide()) {
			return null;
		}
		return XmlUtils.xmlcal2regdate(date.getValue());
	}

	@Nullable
	private static String extractStringMax40(JAXBElement<TypTxtMax40Attr> element) {
		final TypTxtMax40Attr str = extractFromElement(element);
		if (str == null || !str.isValide()) {
			return null;
		}
		return StringUtils.trimToNull(str.getValue());
	}

	@Nullable
	private static String extractStringIban(JAXBElement<TypNumeroIbanAttr> element) {
		final TypNumeroIbanAttr iban = extractFromElement(element);
		if (iban == null || !iban.isValide()) {
			return null;
		}
		return StringUtils.trimToNull(iban.getValue());
	}

	@Nullable
	private static String extractTelephone(JAXBElement<TypTelephoneAttr> element) {
		final TypTelephoneAttr i = extractFromElement(element);
		if (i == null || !i.isValide() || i.getValue() == null) {
			return null;
		}
		return StringUtils.trimToNull(i.getValue());
	}

	@Nullable
	private static Integer toInteger(@Nullable BigInteger bi) {
		if (bi == null || bi.compareTo(MIN_INT) < 0 || bi.compareTo(MAX_INT) > 0) {
			// intraduisible en int... -> ignoré
			return null;
		}
		return bi.intValue();
	}

	@Nullable
	private static Integer toInteger(Long i) {
		return i == null ? null : toInteger(BigInteger.valueOf(i));
	}

	@Nullable
	private static String extractNoTel(@Nullable String indicatif, @Nullable String numero) {
		final String strIndicatif = StringUtils.trimToEmpty(indicatif);
		final String strNumero = StringUtils.trimToEmpty(numero);
		return StringUtils.trimToNull(strIndicatif + strNumero);
	}

	@Nullable
	private static <T> T extractFromElement(JAXBElement<T> element) {
		return element != null ? element.getValue() : null;
	}
}
