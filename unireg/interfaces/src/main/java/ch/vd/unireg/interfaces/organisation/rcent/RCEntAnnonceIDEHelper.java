package ch.vd.unireg.interfaces.organisation.rcent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.ech.ech0010.v6.AddressInformation;
import ch.ech.ech0097.v2.NamedOrganisationId;
import ch.ech.ech0097.v2.UidOrganisationIdCategorie;
import ch.ech.ech0097.v2.UidStructure;
import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.Address;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.NoticeRequest;
import ch.vd.evd0022.v3.NoticeRequestAddresses;
import ch.vd.evd0022.v3.NoticeRequestBody;
import ch.vd.evd0022.v3.NoticeRequestHeader;
import ch.vd.evd0022.v3.NoticeRequestIdentification;
import ch.vd.evd0022.v3.NoticeRequestReport;
import ch.vd.evd0022.v3.NoticeRequestStatus;
import ch.vd.evd0022.v3.RequestApplication;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.Pair;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AdresseAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEData;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.ProtoAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.converters.FormeLegaleConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.LegalFormConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.NoticeRequestAddressConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.NoticeRequestStatusCodeConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.RaisonDeRadiationRegistreIDEConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.StatutAnnonceConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeAnnonceConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeDeSiteConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeOfLocationConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeOfNoticeRequestConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterDeregistrationReasonConverter;

/**
 * @author Raphaël Marmier, 2016-08-23, <raphael.marmier@vd.ch>
 */
public class RCEntAnnonceIDEHelper {

	/*
		Identification du service IDE selon Art. 3c LIDE: numéro IDE de l'administration cantonale des impôts (ACI VD)
	 */
	public static final NumeroIDE NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS = new NumeroIDE("CHE322886489");
	/*
		Identification d'Unireg en tant qu'application de l'ACI
	 */
	public static final String NO_APPLICATION_UNIREG = "2";
	public static final String NOM_APPLICATION_UNIREG = "UNIREG";

	/*
		Utilisateur "unireg" pour satisfaire RCEnt qui ne tolère par de champ userId vide.
	 */
	public static final String UNIREG_USER = "unireg";

	public static final AnnonceIDEData.InfoServiceIDEObligEtenduesImpl SERVICE_IDE_UNIREG =
			new AnnonceIDEData.InfoServiceIDEObligEtenduesImpl(RCEntAnnonceIDEHelper.NO_IDE_ADMINISTRATION_CANTONALE_DES_IMPOTS,
			                                                   RCEntAnnonceIDEHelper.NO_APPLICATION_UNIREG,
			                                                   RCEntAnnonceIDEHelper.NOM_APPLICATION_UNIREG);

	public final static TypeOfNoticeRequestConverter TYPE_OF_NOTICE_CONVERTER = new TypeOfNoticeRequestConverter();
	public static final NoticeRequestStatusCodeConverter NOTICE_REQUEST_STATUS_CONVERTER = new NoticeRequestStatusCodeConverter();
	public static final TypeOfLocationConverter TYPE_OF_LOCATION_CONVERTER = new TypeOfLocationConverter();
	public static final LegalFormConverter LEGAL_FORM_CONVERTER = new LegalFormConverter();
	public static final NoticeRequestAddressConverter ADDRESS_CONVERTER = new NoticeRequestAddressConverter();
	public static final UidRegisterDeregistrationReasonConverter UID_REGISTER_DEREGISTRATION_REASON_CONVERTER = new UidRegisterDeregistrationReasonConverter();

	public static final StatutAnnonceConverter STATUS_ANNONCE_CONVERTER = new StatutAnnonceConverter();
	public static final TypeAnnonceConverter TYPE_ANNONCE_CONVERTER = new TypeAnnonceConverter();
	public static final TypeDeSiteConverter TYPE_DE_SITE_CONVERTER = new TypeDeSiteConverter();
	public static final RaisonDeRadiationRegistreIDEConverter RAISON_DE_RADIATION_REGISTRE_IDE_CONVERTER = new RaisonDeRadiationRegistreIDEConverter();
	public static final FormeLegaleConverter FORME_LEGALE_CONVERTER = new FormeLegaleConverter();

	private static final String DUMMY_TEMPLATE_ID = "dummy_template_id";

	/**
	 * Converti une demande d'annonce à l'IDE de RCEnt en annonce à l'IDE ou en modèle d'annonce à l'IDE (sans numéro), en fonction de la présence ou non
	 * d'un numéro.
	 *
	 * @param noticeReport la demande d'annonce RCEnt en entrée.
	 * @return une annonce ou un modèle d'annonce à l'IDE.
	 */
	private static BaseAnnonceIDE buildBaseAnnonceIDE(@NotNull NoticeRequestReport noticeReport) {

		final NoticeRequest noticeRequest = noticeReport.getNoticeRequest();

		final NoticeRequestHeader noticeHeader = noticeRequest.getNoticeRequestHeader();
		final NoticeRequestIdentification noticeIdent = noticeHeader.getNoticeRequestIdentification();
		final NoticeRequestBody noticeBody = noticeRequest.getNoticeRequestBody();

		// Utilisateur
		final String userId = noticeHeader.getUserId();
		final String telephone = noticeHeader.getUserPhoneNumber();

		// Création et données de base
		final String noticeRequestId = noticeIdent.getNoticeRequestId();
		final Long numero = (noticeRequestId == null  || noticeRequestId.startsWith(DUMMY_TEMPLATE_ID)) ? null : Long.valueOf(noticeRequestId);
		final TypeAnnonce typeAnnonce = TYPE_OF_NOTICE_CONVERTER.apply(noticeIdent.getTypeOfNoticeRequest());
		final Date dateAnnonce = noticeIdent.getNoticeRequestDateTime();
		final TypeDeSite typeDeSite = TYPE_OF_LOCATION_CONVERTER.apply(noticeBody.getTypeOfLocation());

		final RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE = UID_REGISTER_DEREGISTRATION_REASON_CONVERTER.apply(noticeBody.getDeregistrationReason());
		final String commentaire = noticeHeader.getComment();

		// Numéros IDE
		final UidStructure uid = noticeBody.getUid();
		final NumeroIDE noIde = uid == null ? null : NumeroIDE.valueOf(uid.getUidOrganisationId());
		final UidStructure uidReplacement = noticeBody.getUidReplacement();
		final NumeroIDE noIdeRemplacant = uidReplacement == null ? null : NumeroIDE.valueOf(uidReplacement.getUidOrganisationId());
		final NamedOrganisationId headquarterUID = noticeBody.getHeadquarterUID();
		final NumeroIDE noIdeEtablissementPrincipal = headquarterUID == null ? null : new NumeroIDE(headquarterUID.getOrganisationId());

		// RCEnt meta data
		final BigInteger cantonalId = noticeBody.getCantonalId();
		final Long numeroSite = cantonalId == null ? null : cantonalId.longValue();
		final BigInteger headquarterCantonalId = noticeBody.getHeadquarterCantonalId();
		final Long numeroOrganisation = headquarterCantonalId == null ? null : headquarterCantonalId.longValue();
		final BigInteger uidReplacedByCantonalId = noticeBody.getUIDReplacedByCantonalId();
		final Long numeroSiteRemplacant = uidReplacedByCantonalId == null ? null : uidReplacedByCantonalId.longValue();

		// Contenu
		final String nom = noticeBody.getName();
		final String nomAdditionnel = noticeBody.getAdditionalName();
		final LegalForm legalForm = noticeBody.getLegalForm();
		final FormeLegale formeLegale = legalForm == null ? null : LEGAL_FORM_CONVERTER.convert(legalForm);
		final String secteurActivite = noticeBody.getBranchText();

		final NoticeRequestAddresses addresses = noticeBody.getAddresses();
		final Address address = addresses == null ? null : addresses.getAddress();
		final AddressInformation postOfficeBoxAddress = addresses == null ? null : addresses.getPostOfficeBoxAddress();
		final AdresseAnnonceIDE adresse;

		final RequestApplication reportingApplication = noticeHeader.getNoticeRequestIdentification().getReportingApplication();
		final String appId = reportingApplication.getId();
		final String appName = reportingApplication.getApplicationName();
		final AnnonceIDEData.InfoServiceIDEObligEtenduesImpl application = new AnnonceIDEData.InfoServiceIDEObligEtenduesImpl(null, appId, appName);

		if (address != null) {
			adresse = ADDRESS_CONVERTER.convert(address);
		}
		else if (postOfficeBoxAddress != null) {
			final Address addressWrapper = new Address();
			addressWrapper.setAddressInformation(postOfficeBoxAddress);
			adresse = ADDRESS_CONVERTER.convert(addressWrapper);
		}
		else {
			adresse = null;
		}

		final NoticeRequestStatus noticeStatus = noticeReport.getNoticeRequestStatus();
		final AnnonceIDEData.StatutImpl statut = new AnnonceIDEData.StatutImpl(NOTICE_REQUEST_STATUS_CONVERTER.apply(noticeStatus.getNoticeRequestStatusCode()),
		                                                                       noticeStatus.getNoticeRequestStatusDate(),
		                                                                       convertErrors(noticeReport));

		if (numero == null) {
			return createProtoAnnonceIDE(typeAnnonce, dateAnnonce, userId, telephone, typeDeSite, raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant,
			                             noIdeEtablissementPrincipal, numeroSite, numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel, formeLegale, secteurActivite, adresse,
			                             statut, application);
		}
		else {
			return createAnnonceIDE(numero, typeAnnonce, dateAnnonce, userId, telephone, typeDeSite, raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant,
			                        noIdeEtablissementPrincipal, numeroSite, numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel, formeLegale, secteurActivite, adresse,
			                        statut, application);
		}
	}

	@NotNull
	public static NoticeRequest buildNoticeRequest(BaseAnnonceIDE proto) {
		return doBuildNoticeRequest(proto);
	}

	@NotNull
	public static NoticeRequest buildNoticeRequest(AnnonceIDEEnvoyee annonce) {
		return doBuildNoticeRequest(annonce);
	}

	@NotNull
	private static NoticeRequest doBuildNoticeRequest(BaseAnnonceIDE proto) {

		final NoticeRequestHeader header = new NoticeRequestHeader();
		final NoticeRequestIdentification identification = new NoticeRequestIdentification();
		header.setNoticeRequestIdentification(identification);
		final NoticeRequestBody body = new NoticeRequestBody();

		final TypeAnnonce type = proto.getType();
		identification.setTypeOfNoticeRequest(type == null ? null : TYPE_ANNONCE_CONVERTER.convert(type));

		identification.setNoticeRequestId(DUMMY_TEMPLATE_ID);
		if (proto instanceof AnnonceIDEEnvoyee) {
			final Long numero = ((AnnonceIDEEnvoyee) proto).getNumero();
			identification.setNoticeRequestId(numero == null ? DUMMY_TEMPLATE_ID : numero.toString());
		}
		identification.setNoticeRequestDateTime(proto.getDateAnnonce());

		final BaseAnnonceIDE.InfoServiceIDEObligEtendues infoServiceIDEObligEtendues = proto.getInfoServiceIDEObligEtendues();
		if (infoServiceIDEObligEtendues != null) {
			identification.setReportingApplication(new RequestApplication(infoServiceIDEObligEtendues.getApplicationId(), infoServiceIDEObligEtendues.getApplicationName()));
			final NumeroIDE noIdeServiceIDEObligEtendues = infoServiceIDEObligEtendues.getNoIdeServiceIDEObligEtendues();
			identification.setIDESource(noIdeServiceIDEObligEtendues == null ? null : new NamedOrganisationId("CH.IDE", noIdeServiceIDEObligEtendues.getValeur()));
		}
		final BaseAnnonceIDE.Utilisateur utilisateur = proto.getUtilisateur();
		header.setUserId(utilisateur == null || utilisateur.getUserId() == null ? UNIREG_USER : utilisateur.getUserId());
		header.setUserPhoneNumber(utilisateur == null ? null :utilisateur.getTelephone());
		header.setComment(proto.getCommentaire());

		final TypeDeSite typeDeSite = proto.getTypeDeSite();
		body.setTypeOfLocation(typeDeSite == null ? null :TYPE_DE_SITE_CONVERTER.convert(typeDeSite));

		final NumeroIDE noIde = proto.getNoIde();
		body.setUid(noIde == null ? null : new UidStructure(UidOrganisationIdCategorie.CHE, noIde.getValeurBrute()));;
		final NumeroIDE noIdeRemplacant = proto.getNoIdeRemplacant();
		body.setUidReplacement(noIdeRemplacant == null ? null : new UidStructure(UidOrganisationIdCategorie.CHE, noIdeRemplacant.getValeurBrute()));

		final RaisonDeRadiationRegistreIDE raisonDeRadiation = proto.getRaisonDeRadiation();
		body.setDeregistrationReason(raisonDeRadiation == null ? null : RAISON_DE_RADIATION_REGISTRE_IDE_CONVERTER.convert(raisonDeRadiation));

		final AnnonceIDEEnvoyee.InformationOrganisation informationOrganisation = proto.getInformationOrganisation();
		if (informationOrganisation != null) {
			final Long numeroSite = informationOrganisation.getNumeroSite();
			body.setCantonalId(numeroSite == null ? null : BigInteger.valueOf(numeroSite));
			final Long numeroSiteRemplacant = informationOrganisation.getNumeroSiteRemplacant();
			body.setUIDReplacedByCantonalId(numeroSiteRemplacant == null ? null : BigInteger.valueOf(numeroSiteRemplacant));
			final Long numeroOrganisation = informationOrganisation.getNumeroOrganisation();
			body.setHeadquarterCantonalId(numeroOrganisation == null ? null : BigInteger.valueOf(numeroOrganisation));
		}

		final AnnonceIDEEnvoyee.Contenu contenu = proto.getContenu();
		if (contenu != null) {

			body.setName(contenu.getNom());
			body.setAdditionalName(contenu.getNomAdditionnel());

			final AdresseAnnonceIDE adresse = contenu.getAdresse();
			if (adresse != null) {
				final NoticeRequestAddresses noticeRequestAddresses = new NoticeRequestAddresses();
				if (adresse.getNumeroCasePostale() != null || adresse.getTexteCasePostale() != null) {
					noticeRequestAddresses.setPostOfficeBoxAddress(ADDRESS_CONVERTER.convert(adresse).getAddressInformation());
				}
				else {
					noticeRequestAddresses.setAddress(ADDRESS_CONVERTER.convert(adresse));
				}
				body.setAddresses(noticeRequestAddresses);
			}

			final FormeLegale formeLegale = contenu.getFormeLegale();
			body.setLegalForm(formeLegale == null ? null : FORME_LEGALE_CONVERTER.convert(formeLegale));
			body.setBranchText(contenu.getSecteurActivite());
		}

		return new NoticeRequest(header, body);
	}

	@NotNull
	public static AnnonceIDE createAnnonceIDE(Long numero, TypeAnnonce typeAnnonce, Date dateAnnonce, String userId, String telephone, TypeDeSite typeDeSite,
	                                          RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE, String commentaire, NumeroIDE noIde, NumeroIDE noIdeRemplacant,
	                                          NumeroIDE noIdeEtablissementPrincipal, Long numeroSite, Long numeroOrganisation, Long numeroSiteRemplacant, String nom, String nomAdditionnel,
	                                          FormeLegale formeLegale, String secteurActivite, AdresseAnnonceIDE adresse, AnnonceIDEData.StatutImpl statut, AnnonceIDEData.InfoServiceIDEObligEtenduesImpl application) {
		Assert.notNull(numero, "Une annonce à l'IDE ne peut pas avoir un numéro vide.");

		final AnnonceIDEData.UtilisateurImpl utilisateur = new AnnonceIDEData.UtilisateurImpl(userId, telephone);

		final AnnonceIDE annonceIDE = new AnnonceIDE(numero,
		                                             typeAnnonce,
		                                             dateAnnonce,
		                                             utilisateur,
		                                             typeDeSite,
		                                             statut,
		                                             application
		);
		fillDetailsAnnonceIDE(raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant, noIdeEtablissementPrincipal, numeroSite, numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel,
		                      formeLegale, secteurActivite, adresse, annonceIDE);
		return annonceIDE;
	}

	@NotNull
	public static ProtoAnnonceIDE createProtoAnnonceIDE(TypeAnnonce typeAnnonce, Date dateAnnonce, String userId, String telephone, TypeDeSite typeDeSite,
	                                                    RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE, String commentaire, NumeroIDE noIde, NumeroIDE noIdeRemplacant,
	                                                    NumeroIDE noIdeEtablissementPrincipal, Long numeroSite, Long numeroOrganisation, Long numeroSiteRemplacant, String nom, String nomAdditionnel,
	                                                    FormeLegale formeLegale, String secteurActivite, AdresseAnnonceIDE adresse, AnnonceIDEData.StatutImpl statut, AnnonceIDEData.InfoServiceIDEObligEtenduesImpl application) {
		final AnnonceIDEData.UtilisateurImpl utilisateur = new AnnonceIDEData.UtilisateurImpl(userId, telephone);

		final ProtoAnnonceIDE protoAnnonceIDE = new ProtoAnnonceIDE(typeAnnonce,
		                                                            dateAnnonce,
		                                                            utilisateur,
		                                                            typeDeSite,
		                                                            statut,
		                                                            application
		);
		fillDetailsAnnonceIDE(raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant, noIdeEtablissementPrincipal, numeroSite, numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel,
		                      formeLegale, secteurActivite, adresse, protoAnnonceIDE);
		return protoAnnonceIDE;
	}

	private static void fillDetailsAnnonceIDE(RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE, String commentaire, NumeroIDE noIde, NumeroIDE noIdeRemplacant,
	                                          NumeroIDE noIdeEtablissementPrincipal, Long numeroSite, Long numeroOrganisation, Long numeroSiteRemplacant, String nom, String nomAdditionnel,
	                                          FormeLegale formeLegale, String secteurActivite, AdresseAnnonceIDE adresse, AnnonceIDEData annonceIDEModeleRCEnt) {
		annonceIDEModeleRCEnt.setRaisonDeRadiation(raisonDeRadiationRegistreIDE);
		annonceIDEModeleRCEnt.setCommentaire(commentaire);

		// Numéros IDE
		annonceIDEModeleRCEnt.setNoIde(noIde);
		annonceIDEModeleRCEnt.setNoIdeRemplacant(noIdeRemplacant);
		annonceIDEModeleRCEnt.setNoIdeEtablissementPrincipal(noIdeEtablissementPrincipal);

		// RCEnt meta data
		final AnnonceIDEData.InformationOrganisationImpl informationOrganisationImpl = new AnnonceIDEData.InformationOrganisationImpl(
				numeroSite,
				numeroOrganisation,
				numeroSiteRemplacant
		);
		annonceIDEModeleRCEnt.setInformationOrganisation(informationOrganisationImpl);

		// Contenu
		final AnnonceIDEData.ContenuImpl contenu = new AnnonceIDEData.ContenuImpl();
		contenu.setNom(nom);
		contenu.setNomAdditionnel(nomAdditionnel);
		contenu.setFormeLegale(formeLegale);
		contenu.setSecteurActivite(secteurActivite);

		contenu.setAdresse(adresse);
		annonceIDEModeleRCEnt.setContenu(contenu);
	}

	@NotNull
	public static AdresseAnnonceIDERCEnt createAdresseAnnonceIDERCEnt(String rue, String numero, String numeroAppartement, Integer npa, String ville, Integer noOfsPays, String iso2Pays, String nomCourtPays,
	                                                                  Integer numeroCasePostale, String texteCasePostale, Integer egid) {
		final AdresseAnnonceIDERCEnt adresse = new AdresseAnnonceIDERCEnt();
		adresse.setRue(rue);
		adresse.setNumero(numero);
		adresse.setNumeroAppartement(numeroAppartement);
		adresse.setNpa(npa);
		adresse.setVille(ville);
		adresse.setPays(new AdresseAnnonceIDERCEnt.PaysRCEnt(noOfsPays, iso2Pays, nomCourtPays));

		adresse.setNumeroCasePostale(numeroCasePostale);
		adresse.setTexteCasePostale(texteCasePostale);

		adresse.setEgid(egid);
		return adresse;
	}

	public static ProtoAnnonceIDE buildProtoAnnonceIDE(NoticeRequestReport noticeReport) {

		final BaseAnnonceIDE annonce = buildBaseAnnonceIDE(noticeReport);
		if (!(annonce instanceof ProtoAnnonceIDE)) {
			throw new IllegalArgumentException("Le rapport spécifié contient un numéro d'annonce à l'IDE : il ne s'agit pas d'une proto-annonce !");
		}
		return (ProtoAnnonceIDE) annonce;
	}

	public static AnnonceIDE buildAnnonceIDE(NoticeRequestReport noticeReport) {

		final BaseAnnonceIDE annonce = buildBaseAnnonceIDE(noticeReport);
		if (!(annonce instanceof AnnonceIDE)) {
			throw new IllegalArgumentException("Le rapport spécifié ne contient pas de numéro d'annonce à l'IDE : il ne s'agit pas d'une annonce émise par Unireg !");
		}
		return (AnnonceIDE) annonce;
	}

	private static List<Pair<String, String>> convertErrors(NoticeRequestReport noticeReport) {
		if (noticeReport.getListOfErrors() == null || noticeReport.getListOfErrors().isEmpty()) {
			return null;
		}
		final ArrayList<Pair<String, String>> erreurs = new ArrayList<>();
		for (ch.vd.evd0022.v3.Error error : noticeReport.getListOfErrors()) {
			erreurs.add(new Pair<>(error.getErrorId(), error.getErrorDescription()));
		}
		return erreurs;
	}
}
