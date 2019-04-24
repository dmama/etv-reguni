package ch.vd.unireg.evenement.identification.contribuable;

import javax.xml.bind.JAXBElement;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.NumeroCtbStringRenderer;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.identification.contribuable.TooManyIdentificationPossibilitiesException;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.identification.request.v2.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v2.NPA;
import ch.vd.unireg.xml.event.identification.response.v2.Erreur;
import ch.vd.unireg.xml.event.identification.response.v2.IdentificationContribuableResponse;
import ch.vd.unireg.xml.event.identification.response.v2.ObjectFactory;

public class IdentificationContribuableRequestHandlerV2 implements IdentificationContribuableRequestHandler<IdentificationContribuableRequest, IdentificationContribuableResponse> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationContribuableRequestHandlerV2.class);

	private static final int MAX_NAME_LENGTH = 100;

	private final ObjectFactory objectFactory = new ObjectFactory();

	private IdentificationContribuableService identCtbService;
	private TiersService tiersService;

	public void setIdentCtbService(IdentificationContribuableService identCtbService) {
		this.identCtbService = identCtbService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	private enum IdentificationResult {
		FOUND_ONE,
		FOUND_NONE,
		FOUND_SEVERAL,
		FOUND_MANY
	}

	public JAXBElement<IdentificationContribuableResponse> handle(IdentificationContribuableRequest request, String businessId) throws EsbBusinessException {

		final IdentificationContribuableResponse response = new IdentificationContribuableResponse();
		final CriteresPersonne criteresPersonne;
		try {
			criteresPersonne = createCriteresPersonne(request);
		}
		catch (IllegalArgumentException e) {
			// techniquement, la XSD est bien respectée (on aurait vu un vrai souci à ce niveau avant d'arriver ici),
			// mais on va dire que c'est équivalent si les données sont pourries...
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e);
		}

		IdentificationResult status;
		List<Long> found;
		try {
			found = identCtbService.identifiePersonnePhysique(criteresPersonne, null);
			switch (found.size()) {
			case 0:
				status = IdentificationResult.FOUND_NONE;
				break;
			case 1:
				status = IdentificationResult.FOUND_ONE;
				break;
			default:
				status = IdentificationResult.FOUND_SEVERAL;
				break;
			}
		}
		catch (TooManyIdentificationPossibilitiesException e) {
			found = e.getExamplesFound();
			status = IdentificationResult.FOUND_MANY;
		}

		if (status == IdentificationResult.FOUND_NONE) {
			final String message = String.format("Aucun contribuable trouvé pour le message '%s'.", businessId);
			final Erreur aucun = new Erreur(message, null);
			response.setErreur(aucun);
			LOGGER.info(message);
		}
		else if (status == IdentificationResult.FOUND_SEVERAL || status == IdentificationResult.FOUND_MANY) {
			final String detail;
			if (found.size() > 0) {
				detail = String.format(" (%s%s)", CollectionsUtils.toString(found, NumeroCtbStringRenderer.INSTANCE, ", "), status == IdentificationResult.FOUND_MANY ? ", ..." : StringUtils.EMPTY);
			}
			else {
				detail = StringUtils.EMPTY;
			}
			final String message = String.format("Plusieurs contribuables trouvés pour le message '%s'%s.", businessId, detail);
			final Erreur plusieurs = new Erreur(null, message);
			response.setErreur(plusieurs);
			LOGGER.info(message);
		}
		else {
			// on a trouvé un et un seul contribuable:
			final Long idCtb = found.get(0);
			final IdentificationContribuableResponse.Contribuable ctb = new IdentificationContribuableResponse.Contribuable();

			ctb.setNumeroContribuableIndividuel(idCtb.intValue());

			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(idCtb);
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
			ctb.setNom(tokenize(nomPrenom.getNom(), MAX_NAME_LENGTH));
			ctb.setPrenom(tokenize(nomPrenom.getPrenom(), MAX_NAME_LENGTH));
			ctb.setDateNaissance(DataHelper.coreToPartialDateXmlv1(tiersService.getDateNaissance(pp)));

			response.setContribuable(ctb);
			LOGGER.info(String.format("Un contribuable trouvé pour le message '%s' : %d.", businessId, idCtb));
		}

		return objectFactory.createIdentificationContribuableResponse(response);
	}

	private static String tokenize(String src, int maxLength) {
		if (src == null) {
			return null;
		}
		return StringUtils.abbreviate(src.replaceAll("\\s+", " "), maxLength);
	}

	/**
	 * @param request la requête d'identification entrante
	 * @return une structure de données représentant les critères de recherche
	 * @throws IllegalArgumentException en cas de souci de conversion des données de la requête vers la donnée structurée (notamment sur la date de naissance)
	 */
	private CriteresPersonne createCriteresPersonne(IdentificationContribuableRequest request) throws IllegalArgumentException {
		CriteresPersonne criteresPersonne = new CriteresPersonne();
		final Long navs13 = request.getNAVS13();
		if (navs13 != null) {
			criteresPersonne.setNAVS13(navs13.toString());
		}

		final Long navs11 = request.getNAVS11();
		if (navs11 != null) {
			criteresPersonne.setNAVS11(navs11.toString());
		}

		criteresPersonne.setNom(request.getNom());
		criteresPersonne.setPrenoms(request.getPrenoms());
		criteresPersonne.setDateNaissance(DataHelper.xmlToCore(request.getDateNaissance()));
		final NPA requestNPA = request.getNPA();
		if (requestNPA != null) {
			final CriteresAdresse criteresAdresse = new CriteresAdresse();
			final Long npaSuisse = requestNPA.getNPASuisse();
			final String npaEtranger = requestNPA.getNPAEtranger();
			if (npaSuisse != null) {
				criteresAdresse.setNpaSuisse(npaSuisse.intValue());
			}
			if (npaEtranger != null) {
				criteresAdresse.setNpaEtranger(npaEtranger);
			}
			criteresAdresse.setChiffreComplementaire(requestNPA.getChiffreComplementaire());
			criteresAdresse.setCodePays(requestNPA.getPays());
			criteresAdresse.setNoOrdrePosteSuisse(requestNPA.getNPASuisseId());
			criteresPersonne.setAdresse(criteresAdresse);
		}
		return criteresPersonne;
	}

	public List<String> getRequestXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "event/identification/identification-contribuable-request-2.xsd");
	}

	public List<String> getResponseXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "event/identification/identification-contribuable-response-2.xsd");
	}
}
