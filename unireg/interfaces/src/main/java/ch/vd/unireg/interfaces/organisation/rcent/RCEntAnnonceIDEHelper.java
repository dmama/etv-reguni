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
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.ModeleAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.ModeleAnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.NumeroIDE;
import ch.vd.unireg.interfaces.organisation.data.RaisonDeRadiationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeAnnonce;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.rcent.converters.FormeLegaleConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.LegalFormConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.NoticeRequestAddressConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.NoticeRequestStatusCodeConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.RaisonDeRadiationRegistreIDEConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeAnnonceConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeDeSiteConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeOfLocationConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.TypeOfNoticeRequestConverter;
import ch.vd.unireg.interfaces.organisation.rcent.converters.UidRegisterDeregistrationReasonConverter;

/**
 * @author Raphaël Marmier, 2016-08-23, <raphael.marmier@vd.ch>
 */
public class RCEntAnnonceIDEHelper {

	private final static TypeOfNoticeRequestConverter TYPE_OF_NOTICE_CONVERTER = new TypeOfNoticeRequestConverter();
	public static final NoticeRequestStatusCodeConverter NOTICE_REQUEST_STATUS_CONVERTER = new NoticeRequestStatusCodeConverter();
	public static final TypeOfLocationConverter TYPE_OF_LOCATION_CONVERTER = new TypeOfLocationConverter();
	public static final LegalFormConverter LEGAL_FORM_CONVERTER = new LegalFormConverter();
	public static final NoticeRequestAddressConverter ADDRESS_CONVERTER = new NoticeRequestAddressConverter();
	public static final UidRegisterDeregistrationReasonConverter UID_REGISTER_DEREGISTRATION_REASON_CONVERTER = new UidRegisterDeregistrationReasonConverter();

	private static final TypeAnnonceConverter TYPE_ANNONCE_CONVERTER = new TypeAnnonceConverter();
	private static final TypeDeSiteConverter TYPE_DE_SITE_CONVERTER = new TypeDeSiteConverter();
	private static final RaisonDeRadiationRegistreIDEConverter RAISON_DE_RADIATION_REGISTRE_IDE_CONVERTER = new RaisonDeRadiationRegistreIDEConverter();
	private static final FormeLegaleConverter FORME_LEGALE_CONVERTER = new FormeLegaleConverter();

	private static final String DUMMY_TEMPLATE_ID = "dummy_template_id";

	/**
	 * Converti une demande d'annonce IDE de RCEnt en annonce IDE ou en modèle d'annonce IDE (sans numéro), en fonction de la présence ou non
	 * d'un numéro.
	 *
	 * @param noticeRequest la demande d'annonce RCEnt en entrée.
	 * @return une annonce ou un modèle d'annonce IDE.
	 */
	public static ModeleAnnonceIDE get(NoticeRequest noticeRequest) {
		final NoticeRequestHeader noticeHeader = noticeRequest.getNoticeRequestHeader();
		final NoticeRequestIdentification noticeIdent = noticeHeader.getNoticeRequestIdentification();
		final NoticeRequestBody noticeBody = noticeRequest.getNoticeRequestBody();

		// Utilisateur
		final String userId = noticeHeader.getUserId();
		final String telephone = noticeHeader.getUserPhoneNumber();

		// Création et données de base
		final String noticeRequestId = noticeIdent.getNoticeRequestId();
		final Long numero = noticeRequestId.startsWith(DUMMY_TEMPLATE_ID) ? null : Long.valueOf(noticeRequestId);
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

		if (address != null) {
			adresse = ADDRESS_CONVERTER.convert(address);
		} else if (postOfficeBoxAddress != null) {
			final Address addressWrapper = new Address();
			addressWrapper.setAddressInformation(postOfficeBoxAddress);
			adresse = ADDRESS_CONVERTER.convert(addressWrapper);
		} else {
			adresse = null;
		}

		if (numero == null) {
			return createModeleAnnonceIDERCEnt(typeAnnonce, dateAnnonce, userId, telephone, typeDeSite, raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant,
			                                   noIdeEtablissementPrincipal,
			                                   numeroSite,
			                                   numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel, formeLegale, secteurActivite, adresse);
		} else {
			return createAnnonceIDERCEnt(numero, typeAnnonce, dateAnnonce, userId, telephone, typeDeSite, raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant,
			                             noIdeEtablissementPrincipal,
			                             numeroSite,
			                             numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel, formeLegale, secteurActivite, adresse);
		}
	}

	@NotNull
	public static NoticeRequest buildNoticeRequest(ModeleAnnonceIDE modele) {
		return doBuildNoticeRequest(modele);
	}

	@NotNull
	public static NoticeRequest buildNoticeRequest(AnnonceIDE annonce) {
		return doBuildNoticeRequest(annonce);
	}

	@NotNull
	private static NoticeRequest doBuildNoticeRequest(ModeleAnnonceIDE modele) {

		final NoticeRequestHeader header = new NoticeRequestHeader();
		final NoticeRequestIdentification identification = new NoticeRequestIdentification();
		header.setNoticeRequestIdentification(identification);
		final NoticeRequestBody body = new NoticeRequestBody();

		final TypeAnnonce type = modele.getType();
		identification.setTypeOfNoticeRequest(type == null ? null : TYPE_ANNONCE_CONVERTER.convert(type));

		identification.setNoticeRequestId(DUMMY_TEMPLATE_ID);
		if (modele instanceof AnnonceIDE) {
			final Long numero = ((AnnonceIDE) modele).getNumero();
			identification.setNoticeRequestId(numero == null ? DUMMY_TEMPLATE_ID : numero.toString());
		}
		identification.setNoticeRequestDateTime(modele.getDateAnnonce());

		final ModeleAnnonceIDE.ServiceIDE serviceIDE = modele.getServiceIDE();
		identification.setReportingApplication( serviceIDE == null ? null : new RequestApplication(serviceIDE.getApplicationId(), serviceIDE.getApplicationName()));
		identification.setIDESource( serviceIDE == null ? null : new NamedOrganisationId("CH.IDE", serviceIDE.getNoIdeServiceIDE().getValeur()));

		final ModeleAnnonceIDE.Utilisateur utilisateur = modele.getUtilisateur();
		header.setUserId(utilisateur == null ? null :utilisateur.getUserId());
		header.setUserPhoneNumber(utilisateur == null ? null :utilisateur.getTelephone());
		header.setComment(modele.getCommentaire());

		final TypeDeSite typeDeSite = modele.getTypeDeSite();
		body.setTypeOfLocation(typeDeSite == null ? null :TYPE_DE_SITE_CONVERTER.convert(typeDeSite));

		final NumeroIDE noIde = modele.getNoIde();
		body.setUid(noIde == null ? null : new UidStructure(UidOrganisationIdCategorie.CHE, noIde.getValeurBrute()));;
		final NumeroIDE noIdeRemplacant = modele.getNoIdeRemplacant();
		body.setUidReplacement(noIdeRemplacant == null ? null : new UidStructure(UidOrganisationIdCategorie.CHE, noIdeRemplacant.getValeurBrute()));

		final RaisonDeRadiationRegistreIDE raisonDeRadiation = modele.getRaisonDeRadiation();
		body.setDeregistrationReason(raisonDeRadiation == null ? null : RAISON_DE_RADIATION_REGISTRE_IDE_CONVERTER.convert(raisonDeRadiation));

		final AnnonceIDE.InformationOrganisation informationOrganisation = modele.getInformationOrganisation();
		if (informationOrganisation != null) {
			final Long numeroSite = informationOrganisation.getNumeroSite();
			body.setCantonalId(numeroSite == null ? null : BigInteger.valueOf(numeroSite));
			final Long numeroSiteRemplacant = informationOrganisation.getNumeroSiteRemplacant();
			body.setUIDReplacedByCantonalId(numeroSiteRemplacant == null ? null : BigInteger.valueOf(numeroSiteRemplacant));
			final Long numeroOrganisation = informationOrganisation.getNumeroOrganisation();
			body.setHeadquarterCantonalId(numeroOrganisation == null ? null : BigInteger.valueOf(numeroOrganisation));
		}

		final AnnonceIDE.Contenu contenu = modele.getContenu();
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
	public static AnnonceIDERCEnt createAnnonceIDERCEnt(Long numero, TypeAnnonce typeAnnonce, Date dateAnnonce, String userId, String telephone, TypeDeSite typeDeSite,
	                                                    RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE, String commentaire, NumeroIDE noIde, NumeroIDE noIdeRemplacant,
	                                                    NumeroIDE noIdeEtablissementPrincipal, Long numeroSite, Long numeroOrganisation, Long numeroSiteRemplacant, String nom, String nomAdditionnel,
	                                                    FormeLegale formeLegale, String secteurActivite, AdresseAnnonceIDE adresse) {
		Assert.notNull(numero, "Une annonce IDE ne peut pas avoir un numéro vide.");

		final AnnonceIDERCEnt.UtilisateurRCEnt utilisateur = new AnnonceIDERCEnt.UtilisateurRCEnt(userId, telephone);

		final AnnonceIDERCEnt annonceIDERCEnt = new AnnonceIDERCEnt(numero,
		                                                            typeAnnonce,
		                                                            dateAnnonce,
		                                                            utilisateur,
		                                                            typeDeSite,
		                                                            null
		);
		fillDetailsAnnonceIDE(raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant, noIdeEtablissementPrincipal, numeroSite, numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel,
		                      formeLegale, secteurActivite, adresse, annonceIDERCEnt);
		return annonceIDERCEnt;
	}

	@NotNull
	public static ModeleAnnonceIDERCEnt createModeleAnnonceIDERCEnt(TypeAnnonce typeAnnonce, Date dateAnnonce, String userId, String telephone, TypeDeSite typeDeSite,
	                                                                RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE, String commentaire, NumeroIDE noIde, NumeroIDE noIdeRemplacant,
	                                                                NumeroIDE noIdeEtablissementPrincipal, Long numeroSite, Long numeroOrganisation, Long numeroSiteRemplacant, String nom, String nomAdditionnel,
	                                                                FormeLegale formeLegale, String secteurActivite, AdresseAnnonceIDE adresse) {
		final AnnonceIDERCEnt.UtilisateurRCEnt utilisateur = new AnnonceIDERCEnt.UtilisateurRCEnt(userId, telephone);

		final ModeleAnnonceIDERCEnt annonceIDEModeleRCEnt = new ModeleAnnonceIDERCEnt(typeAnnonce,
		                                                                              dateAnnonce,
		                                                                              utilisateur,
		                                                                              typeDeSite,
		                                                                              null
		);
		fillDetailsAnnonceIDE(raisonDeRadiationRegistreIDE, commentaire, noIde, noIdeRemplacant, noIdeEtablissementPrincipal, numeroSite, numeroOrganisation, numeroSiteRemplacant, nom, nomAdditionnel,
		                      formeLegale, secteurActivite, adresse, annonceIDEModeleRCEnt);
		return annonceIDEModeleRCEnt;
	}

	private static void fillDetailsAnnonceIDE(RaisonDeRadiationRegistreIDE raisonDeRadiationRegistreIDE, String commentaire, NumeroIDE noIde, NumeroIDE noIdeRemplacant,
	                                          NumeroIDE noIdeEtablissementPrincipal, Long numeroSite, Long numeroOrganisation, Long numeroSiteRemplacant, String nom, String nomAdditionnel,
	                                          FormeLegale formeLegale, String secteurActivite, AdresseAnnonceIDE adresse, ModeleAnnonceIDERCEnt annonceIDEModeleRCEnt) {
		annonceIDEModeleRCEnt.setRaisonDeRadiation(raisonDeRadiationRegistreIDE);
		annonceIDEModeleRCEnt.setCommentaire(commentaire);

		// Numéros IDE
		annonceIDEModeleRCEnt.setNoIde(noIde);
		annonceIDEModeleRCEnt.setNoIdeRemplacant(noIdeRemplacant);
		annonceIDEModeleRCEnt.setNoIdeEtablissementPrincipal(noIdeEtablissementPrincipal);

		// RCEnt meta data
		final AnnonceIDERCEnt.InformationOrganisationRCEnt informationOrganisationRCEnt = new AnnonceIDERCEnt.InformationOrganisationRCEnt(
				numeroSite,
				numeroOrganisation,
				numeroSiteRemplacant
		);
		annonceIDEModeleRCEnt.setInformationOrganisation(informationOrganisationRCEnt);

		// Contenu
		final AnnonceIDERCEnt.ContenuRCEnt contenu = new AnnonceIDERCEnt.ContenuRCEnt();
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

	public static ModeleAnnonceIDERCEnt get(NoticeRequestReport noticeReport) {
		final ModeleAnnonceIDERCEnt annonceIDERCEnt = (ModeleAnnonceIDERCEnt) get(noticeReport.getNoticeRequest());

		final NoticeRequestStatus noticeStatus = noticeReport.getNoticeRequestStatus();
		final ModeleAnnonceIDERCEnt.StatutRCEnt statut = new ModeleAnnonceIDERCEnt.StatutRCEnt(NOTICE_REQUEST_STATUS_CONVERTER.apply(noticeStatus.getNoticeRequestStatusCode()),
		                                                                                       noticeStatus.getNoticeRequestStatusDate(),
		                                                                                       convertErrors(noticeReport)
		);

		return new ModeleAnnonceIDERCEnt(annonceIDERCEnt, statut);
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
