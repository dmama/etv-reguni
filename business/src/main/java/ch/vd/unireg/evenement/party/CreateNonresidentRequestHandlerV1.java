package ch.vd.unireg.evenement.party;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

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
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;

public class CreateNonresidentRequestHandlerV1 implements RequestHandlerV1<CreateNonresidentRequest> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateNonresidentRequestHandlerV1.class);

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
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/create-nonresident-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Collections.singletonList(new ClassPathResource("event/party/create-nonresident-response-1.xsd"));
	}
}
