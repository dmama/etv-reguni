package ch.vd.uniregctb.admin;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.admin.AuditLogBean.AuditView;
import ch.vd.uniregctb.audit.AuditLine;
import ch.vd.uniregctb.audit.AuditLineDAO;
import ch.vd.uniregctb.common.AbstractSimpleFormController;
import ch.vd.uniregctb.common.WebParamPagination;

/**
 * Controller spring permettant la visualisation des logs d'audit
 */
public class AuditLogController extends AbstractSimpleFormController {

	//private final Logger LOGGER = Logger.getLogger(AuditLogController.class);

	private static final String TABLE_NAME = "logs";
	private static final int PAGE_SIZE = 50;

	private AuditLineDAO auditLineDAO;
	private PlatformTransactionManager transactionManager;

	private List<AuditView> coreToWeb(List<AuditLine> list) {
		final HibernateTemplate hibernateTemplate = auditLineDAO.getHibernateTemplate();
		List<AuditView> result = new ArrayList<AuditView>(list.size());
		for (AuditLine line : list) {
			result.add(new AuditView(line, hibernateTemplate));
		}
		return result;
	}

	/**
	 * <b>Note</b>: le form de la page d'Audit utilise la méthode HTTP 'get' de soumission du formulaire. Cela est nécessaire pour faire
	 * cohabiter la pagination (qui utilise forcément la méthode 'get') et le formulaire de filtrage des données.
	 * <p>
	 * La soumission de formulaire par la méthode 'get' ne semble pas avoir un support très élevé dans Spring, c'est pour ça que :
	 * <ul>
	 * <li>il est nécessaire de récupérer le bean à partir des erreurs de binding</li>
	 * <li>il est nécessaire de binder à la main le bean malgré tout</li>
	 * </ul>
	 *
	 * Peut-être y a-t-il une manière plus orthodoxe de faire, mais je ne l'ai pas trouvée. Libre à vous d'améliorer le code si vous savez
	 * ce que vous faites :-)
	 */
	@Override
	protected ModelAndView showForm(HttpServletRequest request, HttpServletResponse response, BindException errors) throws Exception {

		// Récupération de la pagination
		WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, "id", false);

		final Object target = errors.getTarget();
		AuditLogBean bean = (AuditLogBean) target;
		bindAndValidate(request, bean); // voir commentaire de la méthode

		// Chargement des lignes d'audit correspondant aux nouveaux critères
		fillBean(bean, pagination);

		return super.showForm(request, response, errors);
	}

	private void fillBean(final AuditLogBean bean, final WebParamPagination pagination) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		template.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {
				List<AuditLine> l = auditLineDAO.find(bean.getCriteria(), pagination);
				List<AuditView> list = coreToWeb(l);
				bean.setList(list);
				bean.setTotalSize(auditLineDAO.count(bean.getCriteria()));
				return null;
			}
		});
	}

	public void setAuditLineDAO(AuditLineDAO auditLineDAO) {
		this.auditLineDAO = auditLineDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
}
