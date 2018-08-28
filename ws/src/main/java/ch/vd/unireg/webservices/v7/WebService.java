package ch.vd.unireg.webservices.v7;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Set;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import ch.vd.unireg.webservices.common.WebServiceHelper;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.groupdeadline.v7.GroupDeadlineValidationRequest;
import ch.vd.unireg.ws.groupdeadline.v7.GroupDeadlineValidationResponse;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;

@Api
public interface WebService {

	String USER_PARAM_DESCRIPTION = "visa et OID de l’opérateur qui fait la demande de consultation";
	String ERROR_400_MESSAGE = "si le numéro de tiers indiqué n’est pas constitué que de chiffres, si le paramètre « user » est absent ou mal-formé, ou le corps de la requête ne correspond pas au contenu attendu";
	String ERROR_403_READ_MESSAGE_COMPLET = "si l’opérateur indiqué ne possède pas les droits de visualisation complète sur l’application Unireg ou les droits de suffisants sur la ressource en question";
	String ERROR_403_READ_MESSAGE_LIMITEE = "si l’opérateur indiqué ne possède pas les droits de visualisation limitée sur l’application Unireg ou les droits de suffisants sur la ressource en question";
	String ERROR_403_WRITE_MESSAGE = "si l’opérateur indiqué ne possède pas les droits de visualisation sur l’application Unireg ou les droits de modification sur la ressource en question";
	String ERROR_404_MESSAGE = "si le numéro de tiers indiqué ne correspond à rien de connu";
	String ERROR_415_MESSAGE = "si aucun des types de réponse possibles n’est accepté par l’appelant";

	String PART_NAMES = "ADDRESSES, AGENTS, BANK_ACCOUNTS, BUSINESS_YEARS, CAPITALS, CHILDREN, CORPORATION_FLAGS, CORPORATION_STATUSES, DEBTOR_PERIODICITIES, EBILLING_STATUSES, FAMILY_STATUSES, HOUSEHOLD_MEMBERS, IMMOVABLE_PROPERTIES, " +
			"INHERITANCE_RELATIONSHIPS, LABELS, LAND_RIGHTS, LAND_TAX_LIGHTENINGS, LEGAL_FORMS, LEGAL_SEATS, MANAGING_TAX_RESIDENCES, PARENTS, RELATIONS_BETWEEN_PARTIES, RESIDENCY_PERIODS, SIMPLIFIED_TAX_LIABILITIES, TAXATION_PERIODS, " +
			"TAX_DECLARATIONS, TAX_DECLARATIONS_DEADLINES, TAX_DECLARATIONS_STATUSES, TAX_LIABILITIES, TAX_LIGHTENINGS, TAX_RESIDENCES, TAX_SYSTEMS, VIRTUAL_INHERITANCE_LAND_RIGHTS, VIRTUAL_LAND_RIGHTS, VIRTUAL_TAX_RESIDENCES, " +
			"WITHHOLDING_TAXATION_PERIODS, OPERATING_PERIODS, VIRTUAL_LAND_TAX_LIGHTENINGS";

	@GET
	@Produces(WebServiceHelper.TEXT_PLAIN_WITH_UTF8_CHARSET)
	@Path("/status/ping")
	@ApiOperation(value = "Méthode qui permet de vérifier la disponibilité du service")
	@ApiResponses(value = @ApiResponse(code = 200, message = "pong"))
	Response ping();

	@GET
	@Path("/swagger.json")
	@Produces(WebServiceHelper.TEXT_PLAIN_WITH_UTF8_CHARSET)
	@ApiOperation(value = "Description JSON du WS pour Swagger")
	Response getSwaggerJson();

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/repayment/{partyNo}/blocked")
	@ApiOperation(value = "Modification de l’état courant du flag de blocage des remboursements automatiques sur un tiers")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "si tout s’est bien passé"),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = ERROR_403_WRITE_MESSAGE),
			@ApiResponse(code = 404, message = ERROR_404_MESSAGE),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response setAutomaticRepaymentBlockingFlag(@ApiParam("numéro du tiers dont on veut modifier le statut du remboursement automatique") @PathParam("partyNo") int partyNo,
	                                           @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                                           @ApiParam(value = "« true » ou « false » (ou toute autre variante de ces valeurs insensibles à la casse) en fonction de la nouvelle valeur à assiger au flag", required = true) String value);

	@GET
	@Produces(WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET)
	@Path("/repayment/{partyNo}/blocked")
	@ApiOperation(value = "Récupération de l'état courant du flag de blocage des remboursements automatiques sur un tiers")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "avec le corps de la réponse à « true » ou « false » selon l’état du flag"),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = ERROR_403_READ_MESSAGE_COMPLET),
			@ApiResponse(code = 404, message = ERROR_404_MESSAGE),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getAutomaticRepaymentBlockingFlag(@ApiParam("numéro du tiers dont on veut vérifier le statut du remboursement automatique") @PathParam("partyNo") int partyNo,
	                                           @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/security/{user}/{partyNo}")
	@ApiOperation(value = "Récupération du type d’accès autorisé pour un opérateur sur un dossier donné")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "les données demandées", response = SecurityResponse.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 404, message = ERROR_404_MESSAGE),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getSecurityOnParty(@ApiParam("visa de l’opérateur pour lequel on cherche les droits d’accès") @PathParam("user") String user,
	                            @ApiParam("numéro du tiers dont le dossier est la cible des droits d’accès considérés") @PathParam("partyNo") int partyNo);

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/securityOnParties")
	@ApiOperation(value = "Récupération du type d’accès autorisé pour un opérateur sur plusieurs dossiers donnés")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "les données demandées", response = SecurityListResponse.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 404, message = ERROR_404_MESSAGE),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getSecurityOnParties(@ApiParam("visa de l’opérateur pour lequel on cherche les droits d’accès")  @QueryParam("user") String user,
	                              @ApiParam("numéros des tiers dont les dossiers sont les cibles des droits d’accès considérés") @QueryParam("partyNo") List<Integer> partyNos);

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/party/{partyNo}")
	@ApiOperation(value = "Accès aux données d’un tiers connu par son numéro")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "le tiers demandé", response = Party.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = ERROR_403_READ_MESSAGE_COMPLET),
			@ApiResponse(code = 404, message = ERROR_404_MESSAGE),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getParty(@ApiParam("numéro du contribuable dont on veut obtenir les données") @PathParam("partyNo") int partyNo,
	                  @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                  @ApiParam(value = "parts optionnelles spécifiquement demandées", allowMultiple = true, allowableValues = PART_NAMES) @QueryParam("part") Set<PartyPart> parts);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/parties")
	@ApiOperation(value = "Accès aux données de plusieurs tiers connus par leurs numéros, en un seul appel (optimisé pour les batchs)",
			notes = "Pour des raisons de performance, l’utilisation des parts « TAX_DECLARATION_STATUSES », « TAX_DECLARATION_DEADLINES » et « EBILLING_STATUSES » n’est pas recommandée lors d’appels en masse, " +
					"les deux premières car elles causent une forte augmentation du nombre de requêtes en base de données " +
					"et la dernière car elle fait appel à des informations directement tirées de l’application CyberFact sans cache intermédiaire")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "le tiers demandé", response = Parties.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = ERROR_403_READ_MESSAGE_COMPLET),
			@ApiResponse(code = 404, message = ERROR_404_MESSAGE),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getParties(@ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                    @ApiParam(value = "numéros des contribuables dont on veut obtenir les données ; le nombre maximal de numéros acceptés a été arbitrairement fixé à 100", required = true, allowMultiple = true) @QueryParam("partyNo") List<Integer> partyNos,
	                    @ApiParam(value = "parts optionnelles spécifiquement demandées", allowMultiple = true, allowableValues = PART_NAMES) @QueryParam("part") Set<PartyPart> parts);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/communityOfHeirs/{deceasedId}")
	@ApiOperation(value = "Retourne la composition de la communauté d’héritier d’un tiers décédé",
			notes = "Les informations retournées sont : l’historique des membres de la communauté et l’historique des principaux de la communauté")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la communauté d'héritiers demandée", response = CommunityOfHeirs.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = ERROR_403_READ_MESSAGE_COMPLET),
			@ApiResponse(code = 404, message = "si la communité d’héritiers n’existe pas"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getCommunityOfHeirs(@ApiParam(value = "le numéro de tiers du défunt") @PathParam("deceasedId") int deceasedId,
	                             @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	String SEARCH_PARTYNO_DESCRIPTION = "numéro du tiers (si renseigné, les autres critères sont ignorés)";
	String SEARCH_NAME_DESCRIPTION = "nom (ou désignation sociale) des tiers recherchés (URI-encodé en UTF-8)";
	String SEARCH_MODE_DESCRIPTION = "mode de recherche sur le nom (IS_EXACTLY : ne recherche que sur des mots complets, CONTAINS : recherche également sur des portions de mots, " +
			"PHONETIC : recherche également sur des faibles variations des mots indiqués";
	String SEARCH_TOWNCOUNTRY_DESCRIPTION = "nom d’une localité ou d’un pays (lié par une des adresses actuelles des tiers recherchés), URI-encodé en UTF-8";
	String SEARCH_DATEBIRTH_DESCRIPTION = "date de naissance (peut être partielle) des tiers recherchés (JJ.MM.AAAA : date complète ; MM.AAAA ou AAAA : date partielle";
	String SEARCH_VN_DESCRIPTION = "numéro d’assuré social (AVS)";
	String SEARCH_IDE_DESCRIPTION = "numéro d’IDE";
	String SEARCH_TAXRESID_DESCRIPTION = "numéro OFS de la commune d’un for fiscal courant";
	String SEARCH_ACTIVEMAINTAXRES_DESCRIPTION = "booléen qui détermine si le for indiqué par le paramètre « taxResidenceFSOId » doit être un for principal ou s’il peut être un for d’un autre type";
	String SEARCH_PARTYTYPE_DESCRIPTION = "si renseigné, indique quels types de tiers sont éligibles";
	String SEARCH_DEBTORCAT_DESCRIPTION = "le type de débiteur de prestation imposable recherché";
	String SEARCH_ACTIVEPARTY_DESCRIPTION = "booléen qui permet de limiter (par défaut, aucune limitation de ce genre n’est imposée) la recherche aux tiers qui ont un for fiscal actuellement actif";
	String SEARCH_OLDNUMBER_DESCRIPTION = "ancien numéro de sourcier des tiers recherchés";
	String SEARCH_PARTYTYPE_LIST = "NATURAL_PERSON, RESIDENT_NATURAL_PERSON, NON_RESIDENT_NATURAL_PERSON, HOUSEHOLD, DEBTOR, CORPORATION, ESTABLISHMENT, ADMINISTRATIVE_AUTHORITY, OTHER_COMMUNITY";
	String SEARCH_DEBTORCAT_LIST = "ADMINISTRATORS, SPEAKERS_ARTISTS_SPORTSMEN, MORTGAGE_CREDITORS, PENSION_FUND, REGULAR, LAW_ON_UNDECLARED_WORK, PROFIT_SHARING_FOREIGN_COUNTRY_TAXPAYERS, WINE_FARM_SEASONAL_WORKERS";
	String SEARCH_MODE_LIST = "IS_EXACTLY, CONTAINS, PHONETIC";

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/searchParty")
	@ApiOperation(value = "Recherche de tiers selon des critères avancés",
			notes = "Seul « user » est obligatoire ; dans la mesure cependant où seules les recherches ayant moins de 100 résultats aboutissent (les autres se terminent " +
					"avec un message « trop de résultats, veuillez affiner vos critères), un ou plusieurs autres critères sont dans les faits toujours nécessaires.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "le résultat de la recherche", response = CommunityOfHeirs.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = ERROR_403_READ_MESSAGE_LIMITEE),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response searchParty(@ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                     @ApiParam(value = SEARCH_PARTYNO_DESCRIPTION) @QueryParam("partyNo") String partyNo,
	                     @ApiParam(value = SEARCH_NAME_DESCRIPTION) @QueryParam("name") String name,
	                     @ApiParam(value = SEARCH_MODE_DESCRIPTION, allowableValues = SEARCH_MODE_LIST) @QueryParam("nameSearchMode") @DefaultValue("IS_EXACTLY") SearchMode nameSearchMode,
	                     @ApiParam(value = SEARCH_TOWNCOUNTRY_DESCRIPTION) @QueryParam("townOrCountry") String townOrCountry,
	                     @ApiParam(value = SEARCH_DATEBIRTH_DESCRIPTION) @QueryParam("dateOfBirth") String dateOfBirth,
	                     @ApiParam(value = SEARCH_VN_DESCRIPTION) @QueryParam("vn") String socialInsuranceNumber,
	                     @ApiParam(value = SEARCH_IDE_DESCRIPTION) @QueryParam("uid") String uidNumber,
	                     @ApiParam(value = SEARCH_TAXRESID_DESCRIPTION) @QueryParam("taxResidenceFSOId") Integer taxResidenceFSOId,
	                     @ApiParam(value = SEARCH_ACTIVEMAINTAXRES_DESCRIPTION) @QueryParam("onlyActiveMainTaxResidence") @DefaultValue("false") boolean onlyActiveMainTaxResidence,
	                     @ApiParam(value = SEARCH_PARTYTYPE_DESCRIPTION, allowableValues = SEARCH_PARTYTYPE_LIST) @QueryParam("partyType") Set<PartySearchType> partyTypes,
	                     @ApiParam(value = SEARCH_DEBTORCAT_DESCRIPTION, allowableValues = SEARCH_DEBTORCAT_LIST) @QueryParam("debtorCategory") DebtorCategory debtorCategory,
	                     @ApiParam(value = SEARCH_ACTIVEPARTY_DESCRIPTION) @QueryParam("activeParty") Boolean activeParty,
	                     @ApiParam(value = SEARCH_OLDNUMBER_DESCRIPTION) @QueryParam("oldWithholdingNumber") Long oldWithholdingNumber);

	@GET
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@Path("/taxOffices/{municipalityId}")
	@ApiOperation(value = "Fournit les indications sur le district fiscal et la région fiscale d’une commune vaudoise donnée par son numéro OFS")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "les informations demandées", response = TaxOffices.class),
			@ApiResponse(code = 400, message = "si le paramètre « date » est présent et mal-formé"),
			@ApiResponse(code = 404, message = "si la commune indiquée n’existe pas dans le canton de Vaud, ou si aucun district ou aucune région n’est associée"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getTaxOffices(@ApiParam(value = "numéro OFS de la commune considérée") @PathParam("municipalityId") int municipalityId,
	                       @ApiParam(value = "date de validité du numéro OFS fourni (JJ.MM.AAAA)") @QueryParam("date") String date);

	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@Path("/ackOrdinaryTaxDeclarations")
	@ApiOperation(value = "Quittancement de masse de déclarations d’impôt ordinaires")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la réponse du quittancement", response = OrdinaryTaxDeclarationAckResponse.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = "si l’opérateur indiqué ne possède pas les droits de quittance de déclarations d’impôt PP sur l’application Unireg"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response ackOrdinaryTaxDeclarations(@ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                                    @ApiParam(value = "XML au format de l’élément « ordinaryTaxDeclarationAckRequest » du namespace http://www.vd.ch/fiscalite/unireg/ws/ack/7.", required = true) OrdinaryTaxDeclarationAckRequest request);

	@POST
	@Path("/newOrdinaryTaxDeclarationDeadline/{partyNo}/{taxPeriod}/{sequenceNo}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@ApiOperation(value = "Nouvelle demande de délai")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la réponse de la demande de délai", response = DeadlineResponse.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = "si l’opérateur indiqué ne possède pas les droits de demande de délai sur l’application Unireg"),
			@ApiResponse(code = 404, message = "si le contribuable n’existe pas ou s’il n’a aucune déclaration correspondant, pour la période fiscale donnée, au numéro de séquence donné"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response newOrdinaryTaxDeclarationDeadline(@ApiParam(value = "numéro du contribuable pour lequel la demande de délai est faite") @PathParam("partyNo") int partyNo,
	                                           @ApiParam(value = "période fiscale de la déclaration à laquelle on veut rajouter un délai") @PathParam("taxPeriod") int pf,
	                                           @ApiParam(value = "numéro de séquence de la déclaration à laquelle on veut rajouter un délai") @PathParam("sequenceNo") int seqNo,
	                                           @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                                           @ApiParam(value = "XML au format de l’élément « deadlineRequest » du namespace http://www.vd.ch/fiscalite/unireg/ws/deadline/7") DeadlineRequest request);

	@POST
	@Path("/validateGroupDeadlineRequest")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@ApiOperation(value = "Validation d'une demande de groupée de délais sur plusieurs déclarations d'impôt")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la réponse de la demande de validation", response = GroupDeadlineValidationResponse.class),
			@ApiResponse(code = 400, message = ERROR_400_MESSAGE),
			@ApiResponse(code = 403, message = "si l’opérateur indiqué ne possède pas les droits de demande de délai sur l’application Unireg"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response validateGroupDeadlineRequest(@ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                                      @ApiParam(value = "XML au format de l’élément « groupDeadlineValidationRequest » du namespace http://www.vd.ch/fiscalite/unireg/ws/groupdeadline/7.", required = true) GroupDeadlineValidationRequest request);

	@GET
	@Path("/modifiedTaxPayers")
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@ApiOperation(value = "Liste des identifiants des contribuables qui ont été modifiés dans la période de temps spécifiée")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la liste des identifiants demandés", response = PartyNumberList.class),
			@ApiResponse(code = 400, message = "si l’un des paramètres « user », « since » ou « until » est absent ou mal-formé"),
			@ApiResponse(code = 403, message = ERROR_403_READ_MESSAGE_COMPLET),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getModifiedTaxPayers(@ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                              @ApiParam(value = "la date de début de la période concernée, exprimée en millisecondes depuis le 1er janvier 1970 à 0:00 GMT") @QueryParam("since") Long since,
	                              @ApiParam(value = "la date de fin de la période concernée, exprimée en millisecondes depuis le 1er janvier 1970 à 0:00 GMT") @QueryParam("until") Long until);

	@GET
	@Path("/debtor/{debtorNo}/{taxPeriod}")
	@Produces({MediaType.APPLICATION_XML, WebServiceHelper.APPLICATION_JSON_WITH_UTF8_CHARSET})
	@ApiOperation(value = "Information sur les listes récapitulatives restant à émettre sur une période fiscale pour un débiteur de prestation imposable")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "les informations demandées", response = DebtorInfo.class),
			@ApiResponse(code = 400, message = "si l’un des paramètres « user », « since » ou « until » est absent ou mal-formé"),
			@ApiResponse(code = 403, message = ERROR_403_READ_MESSAGE_COMPLET),
			@ApiResponse(code = 404, message = "si aucun débiteur de prestation imposable n’existe avec le numéro indiqué"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getDebtorInfo(@ApiParam(value = "numéro de tiers du débiteur IS") @PathParam("debtorNo") int debtorNo,
	                       @ApiParam(value = "période fiscale considérée") @PathParam("taxPeriod") int pf,
	                       @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	@GET
	@Path("/avatar/{partyNo}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@ApiOperation(value = "Image (pour le moment PNG 128x128) représentant le tiers, selon son type (homme, femme, couple, entreprise…)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "l’image elle-même", response = byte[].class),
			@ApiResponse(code = 404, message = "si le tiers n’existe pas"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getAvatar(@ApiParam(value = "numéro de tiers") @PathParam("partyNo") int partyNo);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/fiscalEvents/{partyNo}")
	@ApiOperation(value = "Fournit la liste des événements fiscaux émis pour le tiers indiqué")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "les événements demandés", response = FiscalEvents.class),
			@ApiResponse(code = 404, message = "si le tiers est inconnu"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getFiscalEvents(@ApiParam(value = "numéro de tiers") @PathParam("partyNo") int partyNo,
	                         @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/immovableProperty/{immoId}")
	@ApiOperation(value = "Fournit un immeuble du registre foncier sur la base du numéro technique Unireg de l’immeuble")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "l'immeuble demandé", response = ImmovableProperty.class),
			@ApiResponse(code = 404, message = "si l’immeuble spécifié est inconnu"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getImmovableProperty(@ApiParam(value = "numéro technique Unireg de l’immeuble souhaité") @PathParam("immoId") long immoId,
	                              @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	/**
	 * Retourne un immeuble à partir de sa situation précise.
	 *
	 * @param municipalityFsoId le numéro OFS de la commune de l'immeuble (obligatoire)
	 * @param parcelNumber      le numéro de parcelle de l'immeuble (obligatoire)
	 * @param index1            l'index n°1 (optionnel, si pas renseigné retourne l'immeuble avec un index1 nul)
	 * @param index2            l'index n°2 (optionnel, si pas renseigné retourne l'immeuble avec un index2 nul)
	 * @param index3            l'index n°3 (optionnel, si pas renseigné retourne l'immeuble avec un index3 nul)
	 * @param user              l'utilisateur physique ayant fait la demande.
	 * @return l'immeuble correspondant ou null si aucun immeuble ne correspond.
	 */
	@SuppressWarnings("JavadocReference")
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/immovablePropertyByLocation/{municipalityFsoId}/{parcelNumber}")
	@ApiOperation(value = "Fournit un immeuble du registre foncier sur la base des informations de situation de l’immeuble")
	@ApiImplicitParams(value = {
			@ApiImplicitParam(name = "index1", value = "index1 de la parcelle (optionnel, si pas renseigné retourne l'immeuble avec un index1 nul)", paramType = "query", dataType = "int32"),
			@ApiImplicitParam(name = "index2", value = "index2 de la parcelle (optionnel, si pas renseigné retourne l'immeuble avec un index2 nul)", paramType = "query", dataType = "int32"),
			@ApiImplicitParam(name = "index3", value = "index3 de la parcelle (optionnel, si pas renseigné retourne l'immeuble avec un index3 nul)", paramType = "query", dataType = "int32")
	})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "l'immeuble demandé", response = ImmovableProperty.class),
			@ApiResponse(code = 404, message = "si l’immeuble spécifié est inconnu"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getImmovablePropertyByLocation(@ApiParam(value = "numéro OFS de la commune de l’immeuble") @PathParam("municipalityFsoId") int municipalityFsoId,
	                                        @ApiParam(value = "numéro de parcelle") @PathParam("parcelNumber") int parcelNumber,
	                                        @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                                        @Context UriInfo uriInfo);

	/**
	 * Recherche un ou plusieurs immeubles en fonction de plusieurs critères. Cette méthode diffère de la méthode {@link #getImmovablePropertyByLocation(int, int, String, UriInfo)}
	 * dans le sens où elle accepte des critères partiels et peut retourne plusieurs immeubles correspondants.
	 *
	 * @param municipalityFsoId le numéro OFS de la commune de l'immeuble (obligatoire)
	 * @param parcelNumber      le numéro de parcelle de l'immeuble (obligatoire)
	 * @param index1            l'index n°1 (optionnel)
	 * @param index2            l'index n°2 (optionnel)
	 * @param index3            l'index n°3 (optionnel)
	 * @param user              l'utilisateur physique ayant fait la demande.
	 * @return une liste d'immeubles correspondant à la demande
	 */
	@SuppressWarnings("JavadocReference")
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/findImmovablePropertyByLocation")
	@ApiOperation(value = "Recherche un ou plusieurs immeubles sur la base des informations de situation")
	@ApiImplicitParams(value = {
			@ApiImplicitParam(name = "index1", value = "index1 de la parcelle (optionnel)", paramType = "query", dataType = "int32"),
			@ApiImplicitParam(name = "index2", value = "index2 de la parcelle (optionnel)", paramType = "query", dataType = "int32"),
			@ApiImplicitParam(name = "index3", value = "index3 de la parcelle (optionnel)", paramType = "query", dataType = "int32")
	})
	Response findImmovablePropertyByLocation(@ApiParam(value = "numéro OFS de la commune de l’immeuble") @QueryParam("municipalityFsoId") int municipalityFsoId,
	                                         @ApiParam(value = "numéro de parcelle") @QueryParam("parcelNumber") int parcelNumber,
	                                         @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user,
	                                         @Context UriInfo uriInfo);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/immovableProperties")
	@ApiOperation(value = "Fournit plusieurs immeubles du registre foncier sur la base des numéros techniques Unireg des immeubles",
			notes = "Au maximum 100 immeubles par requête.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la liste des immeubles demandés", response = ImmovablePropertyList.class),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getImmovableProperties(@ApiParam(value = "numéros techniques Unireg des immeubles souhaités", allowMultiple = true) @QueryParam("immoId") List<Long> immoIds,
	                                @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/building/{buildingId}")
	@ApiOperation(value = "Fournit un bâtiment du registre foncier sur la base du numéro technique Unireg du bâtiment")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "le bâtiment demandé", response = Building.class),
			@ApiResponse(code = 404, message = "si le bâtiment demandé est inconnu"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getBuilding(@ApiParam(value = "numéro technique Unireg du bâtiment souhaité") @PathParam("buildingId") long buildingNo,
	                     @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/buildings")
	@ApiOperation(value = "Fournit plusieurs bâtiments du registre foncier sur la base des numéros techniques Unireg de bâtiment",
			notes = "Au maximum 100 bâtiments par requête.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la liste des bâtiments demandés", response = BuildingList.class),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getBuildings(@ApiParam(value = "numéros techniques Unireg des bâtiments souhaités", allowMultiple = true) @QueryParam("buildingId") List<Long> buildingId,
	                      @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/communityOfOwners/{communityId}")
	@ApiOperation(value = "Fournit une communauté avec la liste complète des propriétaires en faisant partie",
			notes = "Les membres d’une communauté de propriétaires proviennent : 1) des membres de la communauté RF; " +
					"2) des héritiers fiscaux (= rapport-entre-tiers de type « Héritage ») d’un membre de la communauté. " +
					"Les collections suivantes contiennent : « members » : membres RF et héritiers fiscaux mais pas les défunts (pour des raisons historiques) ;" +
					"« leaders » : membres RF, héritiers fiscaux et défunts (périodes de validité calculées au plus juste);" +
					"« memberships » : membres RF, héritiers fiscaux et défunts (périodes de validité calculées au plus juste).")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la communauté de propriétaires demandée", response = CommunityOfOwners.class),
			@ApiResponse(code = 404, message = "si la communauté n'existe pas"),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getCommunityOfOwners(@ApiParam(value = "numéro technique Unireg de la communauté souhaitée") @PathParam("communityId") long communityId,
	                              @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("/landRegistry/communitiesOfOwners")
	@ApiOperation(value = "Fournit des communautés avec la liste complète des propriétaires en faisant partie",
			notes = "Au maximum 100 communautés par requête.")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "la liste des communautés demandées", response = CommunityOfOwnersList.class),
			@ApiResponse(code = 415, message = ERROR_415_MESSAGE)
	})
	Response getCommunitiesOfOwners(@ApiParam(value = "numéros  techniques Unireg des communautés souhaitées", allowMultiple = true) @QueryParam("communityId") List<Long> communityId,
	                                @ApiParam(value = USER_PARAM_DESCRIPTION, required = true) @QueryParam("user") String user);
}
