package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.xml.event.identification.request.v3.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v3.IdentificationData;
import ch.vd.unireg.xml.event.identification.request.v3.NPA;
import ch.vd.unireg.xml.event.identification.response.v3.Erreur;
import ch.vd.unireg.xml.event.identification.response.v3.IdentificationContribuableResponse;
import ch.vd.unireg.xml.event.identification.response.v3.IdentificationResult;
import ch.vd.unireg.xml.event.identification.response.v3.ObjectFactory;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.NumeroCtbStringRenderer;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.identification.contribuable.TooManyIdentificationPossibilitiesException;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.xml.DataHelper;

public class IdentificationContribuableRequestHandlerV3 implements IdentificationContribuableRequestHandler<IdentificationContribuableRequest, IdentificationContribuableResponse> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationContribuableRequestHandlerV3.class);

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

	private enum IdentificationResultKind {
		FOUND_ONE,
		FOUND_NONE,
		FOUND_SEVERAL,
		FOUND_MANY
	}

	public JAXBElement<IdentificationContribuableResponse> handle(IdentificationContribuableRequest request, String businessId) throws EsbBusinessException {
		final List<IdentificationResult> results = new ArrayList<>(request.getIdentificationData().size());
		int index = 0;
		for (IdentificationData inData : request.getIdentificationData()) {
			final String identifier = String.format("%s/part-%02d", businessId, index ++);
			results.add(doIdentification(inData, identifier));
		}
		final IdentificationContribuableResponse response = new IdentificationContribuableResponse(results);
		return objectFactory.createIdentificationContribuableResponse(response);
	}

	private IdentificationResult doIdentification(IdentificationData data, String businessIdPart) throws EsbBusinessException {

		final IdentificationResult result = new IdentificationResult();
		final CriteresPersonne criteresPersonne;
		try {
			criteresPersonne = createCriteresPersonne(data);
		}
		catch (IllegalArgumentException e) {
			// techniquement, la XSD est bien respectée (on aurait vu un vrai souci à ce niveau avant d'arriver ici),
			// mais on va dire que c'est équivalent si les données sont pourries...
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e);
		}

		IdentificationResultKind status;
		List<Long> found;
		try {
			found = identCtbService.identifiePersonnePhysique(criteresPersonne, null);
			switch (found.size()) {
			case 0:
				status = IdentificationResultKind.FOUND_NONE;
				break;
			case 1:
				status = IdentificationResultKind.FOUND_ONE;
				break;
			default:
				status = IdentificationResultKind.FOUND_SEVERAL;
				break;
			}
		}
		catch (TooManyIdentificationPossibilitiesException e) {
			found = e.getExamplesFound();
			status = IdentificationResultKind.FOUND_MANY;
		}

		if (status == IdentificationResultKind.FOUND_NONE) {
			final String message = String.format("Aucun contribuable trouvé pour le message '%s'.", businessIdPart);
			final Erreur aucun = new Erreur(message, null);
			result.setErreur(aucun);
			LOGGER.info(message);
		}
		else if (status == IdentificationResultKind.FOUND_SEVERAL || status == IdentificationResultKind.FOUND_MANY) {
			final String detail;
			if (found.size() > 0) {
				detail = String.format(" (%s%s)", CollectionsUtils.toString(found, NumeroCtbStringRenderer.INSTANCE, ", "), status == IdentificationResultKind.FOUND_MANY ? ", ..." : StringUtils.EMPTY);
			}
			else {
				detail = StringUtils.EMPTY;
			}
			final String message = String.format("Plusieurs contribuables trouvés pour le message '%s'%s.", businessIdPart, detail);
			final Erreur plusieurs = new Erreur(null, message);
			result.setErreur(plusieurs);
			LOGGER.info(message);
		}
		else {
			// on a trouvé un et un seul contribuable:
			final Long idCtb = found.get(0);
			final IdentificationResult.Contribuable ctb = new IdentificationResult.Contribuable();

			ctb.setNumeroContribuableIndividuel(idCtb.intValue());

			final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(idCtb);
			final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
			ctb.setNom(tokenize(nomPrenom.getNom(), MAX_NAME_LENGTH));
			ctb.setPrenom(tokenize(nomPrenom.getPrenom(), MAX_NAME_LENGTH));
			ctb.setDateNaissance(DataHelper.coreToPartialDateXmlv2(tiersService.getDateNaissance(pp)));

			result.setContribuable(ctb);
			LOGGER.info(String.format("Un contribuable trouvé pour le message '%s' : %d.", businessIdPart, idCtb));
		}

		// si on veut se baser sur autre chose que l'ordre et passer une clé quelconque à chaque demande, c'est possible...
		result.setId(data.getId());

		return result;
	}

	private static String tokenize(String src, int maxLength) {
		if (src == null) {
			return null;
		}
		return StringUtils.abbreviate(src.replaceAll("\\s+", " "), maxLength);
	}

	/**
	 * @param data les données d'identification entrants
	 * @return une structure de données représentant les critères de recherche
	 * @throws IllegalArgumentException en cas de souci de conversion des données de la requête vers la donnée structurée (notamment sur la date de naissance)
	 */
	private CriteresPersonne createCriteresPersonne(IdentificationData data) throws IllegalArgumentException {
		CriteresPersonne criteresPersonne = new CriteresPersonne();
		final Long navs13 = data.getNAVS13();
		if (navs13 != null) {
			criteresPersonne.setNAVS13(navs13.toString());
		}

		final Long navs11 = data.getNAVS11();
		if (navs11 != null) {
			criteresPersonne.setNAVS11(navs11.toString());
		}

		criteresPersonne.setNom(data.getNom());
		criteresPersonne.setPrenoms(data.getPrenoms());
		criteresPersonne.setDateNaissance(DataHelper.xmlToCore(data.getDateNaissance()));
		final NPA requestNPA = data.getNPA();
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
			criteresPersonne.setAdresse(criteresAdresse);
		}
		return criteresPersonne;
	}

	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/identification/identification-contribuable-request-3.xsd");
	}

	public List<ClassPathResource> getResponseXSD() {
		return Collections.singletonList(new ClassPathResource("event/identification/identification-contribuable-response-3.xsd"));
	}
}
