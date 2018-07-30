package ch.vd.unireg.evenement.identification.contribuable;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.NumeroCtbStringRenderer;
import ch.vd.unireg.identification.contribuable.IdentificationContribuableService;
import ch.vd.unireg.identification.contribuable.TooManyIdentificationPossibilitiesException;
import ch.vd.unireg.jms.EsbBusinessCode;
import ch.vd.unireg.jms.EsbBusinessException;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.TypeTiers;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.identification.request.v4.CorporationIdentificationData;
import ch.vd.unireg.xml.event.identification.request.v4.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v4.IdentificationData;
import ch.vd.unireg.xml.event.identification.request.v4.NPA;
import ch.vd.unireg.xml.event.identification.request.v4.NaturalPersonIdentificationData;
import ch.vd.unireg.xml.event.identification.response.v4.Erreur;
import ch.vd.unireg.xml.event.identification.response.v4.IdentificationContribuableResponse;
import ch.vd.unireg.xml.event.identification.response.v4.IdentificationResult;
import ch.vd.unireg.xml.event.identification.response.v4.IdentifiedCorporation;
import ch.vd.unireg.xml.event.identification.response.v4.IdentifiedNaturalPerson;
import ch.vd.unireg.xml.event.identification.response.v4.IdentifiedTaxpayer;
import ch.vd.unireg.xml.event.identification.response.v4.ObjectFactory;

public class IdentificationContribuableRequestHandlerV4 implements IdentificationContribuableRequestHandler<IdentificationContribuableRequest, IdentificationContribuableResponse> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentificationContribuableRequestHandlerV4.class);

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
		final List<IdentificationResult> results = new ArrayList<>(request.getNaturalPersonDataOrCorporationData().size());
		int index = 0;
		for (IdentificationData inData : request.getNaturalPersonDataOrCorporationData()) {
			final String identifier = String.format("%s/part-%02d", businessId, index++);
			results.add(doIdentification(inData, identifier));
		}
		final IdentificationContribuableResponse response = new IdentificationContribuableResponse(results);
		return objectFactory.createIdentificationContribuableResponse(response);
	}

	private IdentificationResult doIdentification(IdentificationData data, String businessIdPart) throws EsbBusinessException {
		if (data instanceof NaturalPersonIdentificationData) {
			return doIdentificationPersonnePhysique((NaturalPersonIdentificationData) data, businessIdPart);
		}
		else if (data instanceof CorporationIdentificationData) {
			return doIdentificationPersonneMorale((CorporationIdentificationData) data, businessIdPart);
		}
		else {
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, "Seules les demandes d'identification de personnes physique ou morale sont acceptées.", null);
		}
	}

	private interface Identificator<D extends IdentificationData, C, I extends IdentifiedTaxpayer> {
		C getCriteres(D data);

		List<Long> identifie(C criteres) throws TooManyIdentificationPossibilitiesException;

		I buildIdentifiedInformation(long id);
	}

	private <D extends IdentificationData, C, I extends IdentifiedTaxpayer> IdentificationResult doIdentification(D data, String businessIdPart, Identificator<D, C, I> identificator) throws EsbBusinessException {
		final C criteres;
		try {
			criteres = identificator.getCriteres(data);
		}
		catch (IllegalArgumentException e) {
			// techniquement, la XSD est bien respectée (on aurait vu un vrai souci à ce niveau avant d'arriver ici),
			// mais on va dire que c'est équivalent si les données sont pourries...
			throw new EsbBusinessException(EsbBusinessCode.XML_INVALIDE, e);
		}

		IdentificationResultKind status;
		List<Long> found;
		try {
			found = identificator.identifie(criteres);
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

		final IdentificationResult result;
		if (status == IdentificationResultKind.FOUND_NONE) {
			final String message = String.format("Aucun contribuable trouvé pour le message '%s'.", businessIdPart);
			result = new IdentificationResult(null, new Erreur(message, null), data.getId());
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
			result = new IdentificationResult(null, new Erreur(null, message), data.getId());
			LOGGER.info(message);
		}
		else {
			// on a trouvé un et un seul contribuable:
			final long idCtb = found.get(0);
			final I itp = identificator.buildIdentifiedInformation(idCtb);
			result = new IdentificationResult(itp, null, data.getId());

			LOGGER.info(String.format("Un contribuable trouvé pour le message '%s' : %d.", businessIdPart, idCtb));
		}

		return result;
	}

	private IdentificationResult doIdentificationPersonneMorale(CorporationIdentificationData data, String businessIdPart) throws EsbBusinessException {
		return doIdentification(data, businessIdPart, new Identificator<CorporationIdentificationData, CriteresEntreprise, IdentifiedCorporation>() {
			@Override
			public CriteresEntreprise getCriteres(CorporationIdentificationData data) {
				return createCriteresEntreprise(data);
			}

			@Override
			public List<Long> identifie(CriteresEntreprise criteres) throws TooManyIdentificationPossibilitiesException {
				return identCtbService.identifieEntreprise(criteres);
			}

			@Override
			public IdentifiedCorporation buildIdentifiedInformation(long id) {
				final IdentifiedCorporation ic = new IdentifiedCorporation();
				ic.setNumeroContribuable((int) id);

				final Contribuable ctb = (Contribuable) tiersService.getTiers(id);
				if (ctb instanceof Entreprise) {
					final String raisonSociale = tiersService.getDerniereRaisonSociale((Entreprise) ctb);
					ic.setRaisonSociale(tokenize(raisonSociale, MAX_NAME_LENGTH));
				}
				return ic;
			}
		});
	}

	private IdentificationResult doIdentificationPersonnePhysique(NaturalPersonIdentificationData data, String businessIdPart) throws EsbBusinessException {
		return doIdentification(data, businessIdPart, new Identificator<NaturalPersonIdentificationData, CriteresPersonne, IdentifiedNaturalPerson>() {
			@Override
			public CriteresPersonne getCriteres(NaturalPersonIdentificationData data) {
				return createCriteresPersonne(data);
			}

			@Override
			public List<Long> identifie(CriteresPersonne criteres) throws TooManyIdentificationPossibilitiesException {
				return identCtbService.identifiePersonnePhysique(criteres, null);
			}

			@Override
			public IdentifiedNaturalPerson buildIdentifiedInformation(long id) {
				final IdentifiedNaturalPerson inp = new IdentifiedNaturalPerson();
				inp.setNumeroContribuable((int) id);

				final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
				final NomPrenom nomPrenom = tiersService.getDecompositionNomPrenom(pp, false);
				inp.setNom(tokenize(nomPrenom.getNom(), MAX_NAME_LENGTH));
				inp.setPrenom(tokenize(nomPrenom.getPrenom(), MAX_NAME_LENGTH));
				inp.setDateNaissance(DataHelper.coreToPartialDateXmlv2(tiersService.getDateNaissance(pp)));
				return inp;
			}
		});
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
	private static CriteresPersonne createCriteresPersonne(NaturalPersonIdentificationData data) throws IllegalArgumentException {
		final CriteresPersonne criteresPersonne = new CriteresPersonne();
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

	/**
	 * @param data les données d'identification entrants
	 * @return une structure de données représentant les critères de recherche
	 * @throws IllegalArgumentException en cas de souci de conversion des données de la requête vers la donnée structurée
	 */
	private static CriteresEntreprise createCriteresEntreprise(CorporationIdentificationData data) throws IllegalArgumentException {
		final CriteresEntreprise criteres = new CriteresEntreprise();
		criteres.setIde(data.getUid());
		criteres.setRaisonSociale(data.getRaisonSociale());

		//On ne considère que les entrerprises plus les autres communautés. Cf [SIFISC-28899]
		criteres.setTypesTiers(EnumSet.of(TypeTiers.ENTREPRISE));

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
			criteres.setAdresse(criteresAdresse);
		}
		return criteres;
	}

	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/identification/identification-contribuable-request-4.xsd");
	}

	public List<ClassPathResource> getResponseXSD() {
		return Collections.singletonList(new ClassPathResource("event/identification/identification-contribuable-response-4.xsd"));
	}
}
