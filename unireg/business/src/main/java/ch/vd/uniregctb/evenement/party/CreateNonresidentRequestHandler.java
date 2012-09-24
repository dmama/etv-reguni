package ch.vd.uniregctb.evenement.party;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.event.party.v1.CreateNonresidentRequest;
import ch.vd.unireg.xml.event.party.v1.CreateNonresidentResponse;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.party.person.v1.Sex;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.xml.EnumHelper;
import ch.vd.uniregctb.xml.ServiceException;

public class CreateNonresidentRequestHandler implements RequestHandler<CreateNonresidentRequest> {

	private static final Logger LOGGER = Logger.getLogger(CreateNonresidentRequestHandler.class);

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public RequestHandlerResult handle(CreateNonresidentRequest request) throws ServiceException {
		// Vérification des droits d'accès
		final UserLogin login = request.getLogin();
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw new ServiceException(
					new AccessDeniedExceptionInfo("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid() + ") n'a pas les droits d'accès en lecture complète sur l'application.", null));
		}
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setNom(request.getLastName());
		nh.setPrenom(request.getFirstName());
		final ch.vd.unireg.xml.common.v1.Date dob = request.getDateOfBirth();
		nh.setDateNaissance(RegDate.get(dob.getYear(), dob.getMonth(), dob.getDay()));
		nh.setSexe(request.getGender() == Sex.MALE ? Sexe.MASCULIN : request.getGender() == Sex.FEMALE ? Sexe.FEMININ : null);
		nh.setNumeroAssureSocial(request.getSocialNumber().toString());
		nh.setCategorieEtranger(EnumHelper.xmlToCore(request.getCategory()));
		CreateNonresidentResponse response = new CreateNonresidentResponse(XmlUtils.date2xmlcal(new Date()), false, null);
		nh = (PersonnePhysique) tiersDAO.save(nh);
		response.setCreated(true);
		response.setNumber(nh.getNumero().intValue());
		return new RequestHandlerResult(response);
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
