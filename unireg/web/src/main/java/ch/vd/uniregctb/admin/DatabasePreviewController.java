package ch.vd.uniregctb.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.utils.UniregModeHelper;

/**
 * Ce contrôleur permet d'afficher les 100 premiers tiers de la base de données.
 */
public class DatabasePreviewController extends AbstractSimpleFormController {

	private TiersDAO tiersDao;
	private AdresseService adresseService;
	private PlatformTransactionManager transactionManager;

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		final DatabasePreview bean = (DatabasePreview) super.formBackingObject(request);
		final List<InfoTiers> infoTiers = buildInfoTiers();
		bean.setInfoTiers(infoTiers);
		return bean;
	}

	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors) throws Exception {
		if (!UniregModeHelper.isTestMode() || !(SecurityProvider.isGranted(Role.TESTER) || SecurityProvider.isGranted(Role.ADMIN))) {
			return new ModelAndView(new RedirectView("../tiers/list.do"));
		}
		return super.showForm(request, response, errors);
	}

	/**
	 * Construit la liste des numéros, type et noms courrier des 100 premiers tiers de la base de données.
	 *
	 * @return une liste contenant des informaitons de tiers
	 */
	private List<InfoTiers> buildInfoTiers() {

		final List<InfoTiers> infoTiers = new ArrayList<InfoTiers>();

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<Tiers> list = tiersDao.getFirst(100);
				for (Tiers t : list) {

					final Long numero = t.getNumero();
					final NatureTiers type = t.getNatureTiers();
					List<String> nomsPrenoms;
					try {
						nomsPrenoms = adresseService.getNomCourrier(t, null, false);
					}
					catch (Exception e) {
						nomsPrenoms = Arrays.asList("Exception: " + e.getMessage());
					}

					InfoTiers info = new InfoTiers(numero, type, nomsPrenoms);
					infoTiers.add(info);
				}
				return null;
			}
		});

		return infoTiers;
	}

	public void setTiersDao(TiersDAO tiersDao) {
		this.tiersDao = tiersDao;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
