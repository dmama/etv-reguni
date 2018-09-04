package ch.vd.unireg.evenement.party;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.avs.AvsHelper;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.indexer.tiers.TopList;
import ch.vd.unireg.interfaces.upi.ServiceUpiException;
import ch.vd.unireg.interfaces.upi.ServiceUpiRaw;
import ch.vd.unireg.interfaces.upi.data.UpiPersonInfo;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v2.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.vn.v1.CreateNonresidentByVNRequest;
import ch.vd.unireg.xml.event.party.nonresident.vn.v1.CreateNonresidentByVNResponse;
import ch.vd.unireg.xml.event.party.v2.ExceptionResponse;
import ch.vd.unireg.xml.event.party.v2.Response;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.xml.exception.v1.TechnicalExceptionInfo;

public class CreateNonresidentByVNRequestHandlerV1 implements RequestHandlerV2<CreateNonresidentByVNRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateNonresidentByVNRequestHandlerV1.class);

	private HibernateTemplate hibernateTemplate;
	private SecurityProviderInterface securityProvider;
	private ServiceUpiRaw serviceUpi;
	private GlobalTiersSearcher tiersSearcher;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	public void setServiceUpi(ServiceUpiRaw serviceUpi) {
		this.serviceUpi = serviceUpi;
	}

	public void setTiersSearcher(GlobalTiersSearcher tiersSearcher) {
		this.tiersSearcher = tiersSearcher;
	}

	@Override
	public RequestHandlerResult<Response> handle(CreateNonresidentByVNRequest request) throws ServiceException {

		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		final Pair<String, Integer> loginInfo = UserLoginHelper.parse(login);
		final String userId = loginInfo.getLeft();
		final int oid = loginInfo.getRight();
		if (!securityProvider.isGranted(Role.CREATE_NONHAB, userId, oid)) {
			throw new ServiceException(new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + userId + '/' + oid + ") n'a pas le droit de création de non-habitant sur l'application.", null));
		}

		final String avsDemande = Long.toString(request.getSocialNumber());
		final String avsError = AvsHelper.validateNouveauNumAVS(avsDemande);
		if (avsError != null) {
			throw new ServiceException(new BusinessExceptionInfo(avsError, BusinessExceptionCode.INVALID_REQUEST.name(), null));
		}

		// récupération des informations de l'UPI par rapport au numéro AVS fourni
		final UpiPersonInfo upiData;
		try {
			upiData = serviceUpi.getPersonInfo(avsDemande);
			if (upiData == null) {
				return new RequestHandlerResult<>(new ExceptionResponse(new BusinessExceptionInfo("Numéro AVS inconnu à l'UPI.", BusinessExceptionCode.UNKNOWN_PARTY.name(), null)));
			}
		}
		catch (ServiceUpiException e) {
			LOGGER.error("Erreur à l'interrogation du service UPI avec le numéro AVS " + avsDemande, e);
			throw new ServiceException(new TechnicalExceptionInfo(e.getMessage(), null));
		}

		// peut-être que l'UPI fourni un nouveau numéro AVS...
		final String avsRetenu = upiData.getNoAvs13();
		boolean contribuableConnu = false;
		if (!Objects.equals(avsDemande, avsRetenu)) {
			contribuableConnu = isContribuableConnu(avsDemande);
		}
		contribuableConnu = contribuableConnu || isContribuableConnu(avsRetenu);
		if (contribuableConnu) {
			return new RequestHandlerResult<>(new ExceptionResponse(new BusinessExceptionInfo("Un contribuable existe déjà avec ce numéro AVS.", BusinessExceptionCode.ALREADY_EXISTING_PARTY.name(), null)));
		}

		final PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom(upiData.getNom());
		nh.setTousPrenoms(upiData.getPrenoms());
		nh.setPrenomUsuel(NomPrenom.extractPrenomUsuel(upiData.getPrenoms()));
		nh.setDateNaissance(upiData.getDateNaissance());
		nh.setSexe(upiData.getSexe());
		nh.setNumeroAssureSocial(upiData.getNoAvs13());
		nh.setDateDeces(upiData.getDateDeces());
		if (upiData.getNomPrenomMere() != null) {
			nh.setNomMere(upiData.getNomPrenomMere().getNom());
			nh.setPrenomsMere(upiData.getNomPrenomMere().getPrenom());
		}
		if (upiData.getNomPrenomPere() != null) {
			nh.setNomPere(upiData.getNomPrenomPere().getNom());
			nh.setPrenomsPere(upiData.getNomPrenomPere().getPrenom());
		}
		if (upiData.getNationalite() != null && upiData.getNationalite().getPays() != null) {
			nh.setNumeroOfsNationalite(upiData.getNationalite().getPays().getNoOFS());
		}

		final int idNouveauNonHabitant = hibernateTemplate.merge(nh).getId().intValue();

		// On force le flush la session car sinon on risque un problème au moment du commit de la transaction, s'il reste
		// quelque chose à flusher, alors que le "principal" n'est plus connu...
		hibernateTemplate.flush();

		LOGGER.info("Création du non-habitant n°" + idNouveauNonHabitant);
		return new RequestHandlerResult<>(new CreateNonresidentByVNResponse(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()), idNouveauNonHabitant, null));
	}

	private boolean isContribuableConnu(String avs) {
		if (StringUtils.isBlank(avs)) {
			return false;
		}

		final TiersCriteria criteria = new TiersCriteria();
		criteria.setTypeTiersImperatif(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE);
		criteria.setInclureI107(false);
		criteria.setInclureTiersAnnules(false);
		criteria.setNavs13(avs);
		final TopList<TiersIndexedData> trouves = tiersSearcher.searchTop(criteria, 5);
		return trouves.getTotalHits() > 0;
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/create-nonresident-byvn-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Collections.singletonList(new ClassPathResource("event/party/create-nonresident-byvn-response-1.xsd"));
	}
}
