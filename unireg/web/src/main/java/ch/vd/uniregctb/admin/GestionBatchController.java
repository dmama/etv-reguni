package ch.vd.uniregctb.admin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.scheduler.BatchScheduler;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;

/**
 * Controller spring permettant l'affichage des batches
 */
public class GestionBatchController extends AbstractEnhancedSimpleFormController {

	private BatchScheduler batchScheduler;

	@SuppressWarnings("UnusedDeclaration")
	public void setBatchScheduler(BatchScheduler batchScheduler) {
		this.batchScheduler = batchScheduler;
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		final List<JobDefinition> jobs = batchScheduler.getSortedJobs();
		return new BatchList(jobs, this.getMessageSourceAccessor());
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {

		if (!SecurityProvider.isGranted(Role.ADMIN) && !SecurityProvider.isGranted(Role.TESTER)) {
			throw new AccessDeniedException("vous ne possédez aucun droit IfoSec d'administration pour l'application Unireg");
		}

		return showForm(request, response, errors);
	}
}
