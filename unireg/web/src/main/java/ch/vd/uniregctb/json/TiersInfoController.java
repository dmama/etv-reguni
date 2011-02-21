package ch.vd.uniregctb.json;

import javax.servlet.http.HttpServletRequest;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.ControllerUtils;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class TiersInfoController extends JsonController {

	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected String buildJsonResponse(HttpServletRequest request) throws Exception {

		final String numeroAsString = request.getParameter("numero");
		final Long numero = Long.parseLong(numeroAsString);

		ControllerUtils.checkAccesDossierEnLecture(numero);
		
		final StringBuilder s = new StringBuilder();
		s.append("{");

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		template.execute(new TransactionCallback() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				transactionStatus.setRollbackOnly();

				final Tiers tiers = tiersDAO.get(numero);
				if (tiers != null) {
					// à compléter selon les besoins
					s.append("\"numero\":").append(numero).append(",");
					s.append("\"nature\":\"").append(tiers.getNatureTiers()).append("\",");
					s.append("\"dateDebutActivite\":").append(toJSON(tiers.getDateDebutActivite()));
				}

				return null;
			}
		});

		s.append("}");
		return s.toString();
	}

	private static String toJSON(RegDate date) {
		if (date == null) {
			return "null";
		}
		else {
			final StringBuilder s = new StringBuilder();
			s.append("{");
			s.append("\"year\":").append(date.year()).append(",");
			s.append("\"month\":").append(date.month()).append(",");
			s.append("\"day\":").append(date.day());
			s.append("}");
			return s.toString();
		}
	}
}
