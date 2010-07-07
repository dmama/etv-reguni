package ch.vd.uniregctb.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.type.Niveau;

/**
 * Implémentation du provider de sécurité qui regroupe la sécurité d'IFOSec (authentification) et celle d'Unireg (accès aux dossiers).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SecurityProviderImpl implements SecurityProviderInterface, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(SecurityProviderImpl.class);

	private static final String PREFIX_TEST_UNITAIRE = "[UT] ";

	private SecuriteDossierService securiteDossierService;
	private IfoSecService ifoSecService;
	private boolean bypassUnitTest;

	/**
	 * {@inheritDoc}
	 */
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite) {

		// bypass pour les tests unitaires
		if (wantBypassUnitTest(visaOperateur)) {
			return role != Role.VISU_LIMITE;
		}

		return ifoSecService.isGranted(role, visaOperateur, codeCollectivite);
	}

	/**
	 * {@inheritDoc}
	 */
	public Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException {

		// bypass pour les tests unitaires
		if (wantBypassUnitTest(visaOperateur)) {
			return Niveau.ECRITURE;
		}

		return securiteDossierService.getAcces(visaOperateur, tiersId);
	}


	/**
	 * {@inheritDoc}
	 */
	public List<Niveau> getDroitAcces(String visa, List<Long> ids) {

		// bypass pour les tests unitaires
		if (wantBypassUnitTest(visa)) {
			final ArrayList<Niveau> list = new ArrayList<Niveau>(ids.size());
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


	public void setIfoSecService(IfoSecService ifosSecService) {
		this.ifoSecService = ifosSecService;
	}

	public void afterPropertiesSet() throws Exception {
		bypassUnitTest = SecurityDebugConfig.isIfoSecBypassUnitTest();

		if (bypassUnitTest) {

			LOGGER.warn("+---------------------------------------------------------------------------------------+");
			LOGGER.warn("| Attention ! IfoSec est en mode 'test unitaire' : certaines procédures sont bypassées. |");
			LOGGER.warn("+---------------------------------------------------------------------------------------+");

			try {
				AuthenticationHelper.pushPrincipal(AuthenticationHelper.SYSTEM_USER);
				Audit.warn("IfoSec est en mode 'test unitaire'.");
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	private boolean wantBypassUnitTest(String visa) {
		return bypassUnitTest && visa.startsWith(PREFIX_TEST_UNITAIRE);
	}
}
