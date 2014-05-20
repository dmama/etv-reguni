package ch.vd.uniregctb.evenement.party;

import java.util.Arrays;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.nonresident.v1.CreateNonresidentResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class CreateNonresidentRequestHandlerV1 implements RequestHandler<CreateNonresidentRequest> {

	private HibernateTemplate hibernateTemplate;

	private SecurityProviderInterface securityProvider;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	@Override
	public RequestHandlerResult handle(CreateNonresidentRequest request) throws ServiceException {
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

		return new RequestHandlerResult(new CreateNonresidentResponse(XmlUtils.date2xmlcal(DateHelper.getCurrentDate()), idNouveauNonHabitant));
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/party/create-nonresident-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/party/create-nonresident-response-1.xsd"));
	}
}
