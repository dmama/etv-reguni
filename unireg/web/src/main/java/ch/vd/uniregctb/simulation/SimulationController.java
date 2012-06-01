package ch.vd.uniregctb.simulation;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.registre.base.validation.ValidationMessage;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tache.TacheService;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalDAO;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.manager.ForFiscalManager;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.utils.RegDateEditor;
import ch.vd.uniregctb.validation.ValidationService;

@SuppressWarnings("UnusedDeclaration")
@Controller
@RequestMapping(value = "/simulate")
public class SimulationController {

	private ForFiscalDAO forFiscalDAO;
	private TiersService tiersService;
	private TacheService tacheService;
	private ValidationService validationService;
	private ForFiscalManager forFiscalManager;
	private PlatformTransactionManager transactionManager;

	public void setForFiscalDAO(ForFiscalDAO forFiscalDAO) {
		this.forFiscalDAO = forFiscalDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public void setValidationService(ValidationService validationService) {
		this.validationService = validationService;
	}

	public void setForFiscalManager(ForFiscalManager forFiscalManager) {
		this.forFiscalManager = forFiscalManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(RegDate.class, new RegDateEditor(true, false, true)); // [SIFISC-5265] Les dates malformées sont considérées comme nulles
	}

	@RequestMapping(value = "/modeImpositionUpdate.do", method = RequestMethod.GET)
	@ResponseBody
	public SimulationResults modeImpositionUpdate(@RequestParam(value = "idFor", required = true) final long idFor,
	                                              @RequestParam(value = "changeOn", required = true) final RegDate dateChangement,
	                                              @RequestParam(value = "newMode", required = true) final ModeImposition modeImposition,
	                                              @RequestParam(value = "reason", required = true) final MotifFor motifChangement) {

		if (dateChangement == null) {
			return null;
		}
		
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<SimulationResults>() {
			@Override
			public SimulationResults doInTransaction(TransactionStatus status) {

				// on veut simuler les changements, mais surtout pas les committer ni envoyer d'événement
				status.setRollbackOnly();

				final ForFiscal ff = forFiscalDAO.get(idFor);
				if (!(ff instanceof ForFiscalPrincipal)) {
					return null;
				}
				final Tiers tiers = ff.getTiers();
				if (!(tiers instanceof Contribuable)) {
					return null;
				}
				final Contribuable ctb = (Contribuable) tiers;

				SimulationResults table;
				try {
					// applique le changement du mode d'imposition (transaction rollback-only)
					tiersService.changeModeImposition(ctb, dateChangement, modeImposition, motifChangement);

					table = buildSynchronizeActionsTable(ctb);
				}
				catch (ValidationException e) {
					table = new SimulationResults();
					for (ValidationMessage message : e.getErrors()) {
						table.addError(message.getMessage());
					}
				}

				return table;
			}
		});
	}

	@RequestMapping(value = "/forFiscalUpdate.do", method = RequestMethod.GET)
	@ResponseBody
	public SimulationResults forFiscalUpdate(@RequestParam(value = "idFor", required = true) final long idFor,
	                                         @RequestParam(value = "startDate", required = true) final RegDate dateOuverture,
	                                         @RequestParam(value = "startReason", required = true) final MotifFor motifOuverture,
	                                         @RequestParam(value = "endDate", required = true) final RegDate dateFermeture,
	                                         @RequestParam(value = "endReason", required = true)  final MotifFor motifFermeture,
	                                         @RequestParam(value = "noOfs", required = true) final int noOfsAutoriteFiscale) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(new TransactionCallback<SimulationResults>() {
			@Override
			public SimulationResults doInTransaction(TransactionStatus status) {

				// on veut simuler les changements, mais surtout pas les committer ni envoyer d'événement
				status.setRollbackOnly();

				final ForFiscal ff = forFiscalDAO.get(idFor);
				final Tiers tiers = ff.getTiers();
				if (!(tiers instanceof Contribuable)) {
					return null;
				}
				final Contribuable ctb = (Contribuable) tiers;

				SimulationResults table;
				try {
					// simule la mise-à-jour du for fiscal
					if (ff instanceof ForFiscalPrincipal) {
						forFiscalManager.updateForPrincipal((ForFiscalPrincipal) ff, dateFermeture, motifFermeture, noOfsAutoriteFiscale);
					}
					else if (ff instanceof ForFiscalSecondaire) {
						forFiscalManager.updateForSecondaire((ForFiscalSecondaire) ff, dateOuverture, motifOuverture, dateFermeture, motifFermeture, noOfsAutoriteFiscale);
					}
					else if (ff instanceof ForFiscalAutreElementImposable) {
						forFiscalManager.updateForAutreElementImposable((ForFiscalAutreElementImposable) ff, dateFermeture, motifFermeture);
					}
					else {
						// les autres types de fors ne sont pas pris en compte pour l'instant
						return null;
					}

					table = buildSynchronizeActionsTable(ctb);
				}
				catch (ValidationException e) {
					table = new SimulationResults();
					for (ValidationMessage message : e.getErrors()) {
						table.addError(message.getMessage());
					}
				}

				return table;
			}
		});
	}

	private SimulationResults buildSynchronizeActionsTable(Contribuable ctb) {

		final SimulationResults table;

		final ValidationResults vr = validationService.validate(ctb);
		if (vr.hasErrors()) {
			table = new SimulationResults();
			table.addErrors(vr.getErrors());
		}
		else {
			Exception exception = null;
			List<SynchronizeAction> actions = null;

			try {
				actions = tacheService.determineSynchronizeActionsForDIs(ctb);
			}
			catch (AssujettissementException e) {
				exception = e;
			}

			table = new SimulationResults();
			if (exception != null) {
				table.setException(exception);
			}
			else if (actions == null || actions.isEmpty()) {
				// rien d'intéressant à montrer
				return null;
			}
			else {
				table.addActions(actions);
			}
		}

		return table;
	}
}

