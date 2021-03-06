package ch.vd.unireg.security;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.type.Niveau;

/**
 * Implémentation du provider de sécurité qui regroupe la sécurité fiscale (procédures) et celle d'Unireg (accès aux dossiers).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SecurityProviderImpl implements SecurityProviderInterface, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityProviderImpl.class);

	private static final String PREFIX_TEST_UNITAIRE = "[UT] ";

	private SecuriteDossierService securiteDossierService;
	private ServiceSecuriteService securiteService;
	private AuditManager audit;
	private boolean bypassUnitTest;

	@Override
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite) {

		// bypass pour les tests unitaires
		if (wantBypassUnitTest(visaOperateur)) {
			return role != Role.VISU_LIMITE;
		}

		return securiteService.isGranted(role, visaOperateur, codeCollectivite);
	}

	@Override
	public Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException {

		// bypass pour les tests unitaires
		if (wantBypassUnitTest(visaOperateur)) {
			return Niveau.ECRITURE;
		}

		return securiteDossierService.getAcces(visaOperateur, tiersId);
	}

	@Override
	public List<Niveau> getDroitsAcces(String visa, List<Long> ids) {

		// bypass pour les tests unitaires
		if (wantBypassUnitTest(visa)) {
			final ArrayList<Niveau> list = new ArrayList<>(ids.size());
			for (Long id : ids) {
				if (id == null) {
					list.add(null);
				}
				else {
					list.add(Niveau.ECRITURE);
				}
			}
			return list;
		}

		return securiteDossierService.getAcces(visa, ids);
	}

	public void setSecuriteDossierService(SecuriteDossierService service) {
		securiteDossierService = service;
	}

	public void setSecuriteService(ServiceSecuriteService securiteService) {
		this.securiteService = securiteService;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		bypassUnitTest = SecurityDebugConfig.isSecurityBypassUnitTest();

		if (bypassUnitTest) {

			LOGGER.warn("+--------------------------------------------------------------------------------------------+");
			LOGGER.warn("| Attention ! La sécurité est en mode 'test unitaire' : certaines procédures sont bypassées. |");
			LOGGER.warn("+--------------------------------------------------------------------------------------------+");

			AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
			try {
				audit.warn("La sécurité est en mode 'test unitaire'.");
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	private boolean wantBypassUnitTest(String visa) {
		if (bypassUnitTest) {
			if (visa.startsWith(PREFIX_TEST_UNITAIRE)) {
				return true;
			}
			else {
				LOGGER.warn("**** Attention ! Le bypass de sécurité est activé mais l'utilisateur [" + visa + "] ne commence pas par " + PREFIX_TEST_UNITAIRE + ": aucune procédure bypassée ****");
			}
		}
		return false;
	}
}
