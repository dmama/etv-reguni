package ch.vd.unireg.admin;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.vd.unireg.admin.AuditLogBean.AuditView;
import ch.vd.unireg.audit.AuditLineDAO;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.hibernate.HibernateTemplate;

/**
 * Controller spring permettant la visualisation des logs d'audit
 */
@Controller
public class AuditLogController {

	//private final Logger LOGGER = LoggerFactory.getLogger(AuditLogController.class);

	private static final String TABLE_NAME = "logs";
	private static final int PAGE_SIZE = 50;

	private AuditLineDAO auditLineDAO;
	private HibernateTemplate hibernateTemplate;

	/**
	 * <b>Note</b>: le form de la page d'Audit utilise la méthode HTTP 'get' de soumission du formulaire. Cela est nécessaire pour faire
	 * cohabiter la pagination (qui utilise forcément la méthode 'get') et le formulaire de filtrage des données.
	 */
	@RequestMapping(value = "/admin/audit.do", method = RequestMethod.GET)
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public String dump(HttpServletRequest request, @ModelAttribute AuditLogBean bean, Model model) throws Exception {

		// Récupération de la pagination
		WebParamPagination pagination = new WebParamPagination(request, TABLE_NAME, PAGE_SIZE, "id", false);

		// Chargement des lignes d'audit correspondant aux nouveaux critères
		bean.setList(auditLineDAO.find(bean.getCriteria(), pagination).stream()
				.map(l -> new AuditView(l, hibernateTemplate))
				.collect(Collectors.toList()));
		bean.setTotalSize(auditLineDAO.count(bean.getCriteria()));
		model.addAttribute("command", bean);

		return "admin/audit";
	}

	public void setAuditLineDAO(AuditLineDAO auditLineDAO) {
		this.auditLineDAO = auditLineDAO;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}
}
