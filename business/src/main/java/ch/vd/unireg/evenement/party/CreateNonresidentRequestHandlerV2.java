package ch.vd.unireg.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.evenement.RequestHandlerResult;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.IdentificationPersonne;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieIdentifiant;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.EnumHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.v2.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.nonresident.v2.CreateNonresidentResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;

public class CreateNonresidentRequestHandlerV2 implements RequestHandlerV1<CreateNonresidentRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateNonresidentRequestHandlerV2.class);

	private HibernateTemplate hibernateTemplate;

	private SecurityProviderInterface securityProvider;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult<CreateNonresidentResponse> handle(CreateNonresidentRequest request) throws ServiceException {
		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!securityProvider.isGranted(Role.CREATE_NONHAB, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas le droit de création de non-habitant sur l'application.", null));
		}
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom(request.getLastName());
		nh.setPrenomUsuel(request.getFirstName());
		nh.setDateNaissance(DataHelper.xmlToCore(request.getDateOfBirth()));
		nh.setSexe(EnumHelper.xmlToCore(request.getGender()));
		if (request.getSocialNumber() != null) {
			nh.setNumeroAssureSocial(request.getSocialNumber().toString());
		}
		else if (request.getOldSocialNumber() != null) {
			IdentificationPersonne ip = new IdentificationPersonne();
			ip.setCategorieIdentifiant(CategorieIdentifiant.CH_AHV_AVS);
			ip.setIdentifiant(request.getOldSocialNumber().toString());
			ip.setPersonnePhysique(nh);
			nh.addIdentificationPersonne(ip);
		}
		nh.setCategorieEtranger(EnumHelper.xmlToCore(request.getCategory()));
		final int idNouveauNonHabitant = hibernateTemplate.merge(nh).getId().intValue();

		// On force le flush la session car sinon problème:
		// l'authentification n'est plus valide au moment ou hibernate veut sauver l'eventuelle IdentificationPersonne
		hibernateTemplate.flush();

		LOGGER.info("Création du non-habitant n°" + idNouveauNonHabitant);
		return new RequestHandlerResult<>(new CreateNonresidentResponse(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()), idNouveauNonHabitant));
	}

	@Override
	@NotNull
	public List<String> getRequestXSDs() {
		return Arrays.asList("eCH-0010-4-0.xsd",
		                     "eCH-0044-2-0.xsd",
		                     "unireg-common-1.xsd",
		                     "party/unireg-party-address-1.xsd",
		                     "party/unireg-party-immovableproperty-1.xsd",
		                     "party/unireg-party-debtor-type-2.xsd",
		                     "party/unireg-party-taxdeclaration-2.xsd",
		                     "party/unireg-party-taxresidence-1.xsd",
		                     "party/unireg-party-relation-1.xsd",
		                     "party/unireg-party-2.xsd",
		                     "party/unireg-party-taxpayer-2.xsd",
		                     "party/unireg-party-debtor-2.xsd",
		                     "party/unireg-party-person-2.xsd",
		                     "event/party/request-1.xsd",
		                     "event/party/create-nonresident-request-2.xsd");
	}

	@Override
	@NotNull
	public List<String> getResponseXSDs() {
		return Arrays.asList("unireg-common-1.xsd",
		                     "unireg-exception-1.xsd",
		                     "event/party/response-1.xsd",
		                     "event/party/create-nonresident-response-2.xsd");
	}
}
