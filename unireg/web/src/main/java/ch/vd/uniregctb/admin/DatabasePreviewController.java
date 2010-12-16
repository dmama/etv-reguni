package ch.vd.uniregctb.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Map<Class,List<InfoTiers>> infoTiers = buildInfoTiers();
		bean.setInfoTiers(infoTiers);
		return bean;
	}

	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors) throws Exception {
		if (!UniregModeHelper.isTestMode() || !(SecurityProvider.isGranted(Role.TESTER) || SecurityProvider.isGranted(Role.ADMIN))) {
			flashWarning("Vous ne possédez pas les droits suffisants pour accéder à la prévisualisation des tiers !");
			return new ModelAndView(new RedirectView("../tiers/list.do"));
		}
		return super.showForm(request, response, errors);
	}

	/**
	 * Construit la liste des numéros, type et noms courrier des 100 premiers tiers de la base de données.
	 *
	 * @return une liste contenant des informaitons de tiers
	 */
	private Map<Class, List<InfoTiers>> buildInfoTiers() {

		final Map<Class, List<InfoTiers>> infoMap = new HashMap<Class, List<InfoTiers>>();

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final Map<Class,List<Tiers>> map = tiersDao.getFirstGroupedByClass(100);
				for (Map.Entry<Class, List<Tiers>> entry : map.entrySet()) {
					for (Tiers t : entry.getValue()) {

						final Long numero = t.getNumero();
						final NatureTiers type = t.getNatureTiers();
						List<String> nomsPrenoms;
						try {
							nomsPrenoms = adresseService.getNomCourrier(t, null, false);
						}
						catch (Exception e) {
							nomsPrenoms = Arrays.asList("Exception: " + e.getMessage());
						}

						final InfoTiers info = new InfoTiers(numero, type, nomsPrenoms);

						List<InfoTiers> infoTiers = infoMap.get(entry.getKey());
						if (infoTiers == null) {
							infoTiers = new ArrayList<InfoTiers>();
							infoMap.put(entry.getKey(), infoTiers);
						}

						infoTiers.add(info);
					}
				}
				return null;
			}
		});

		return infoMap;
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
