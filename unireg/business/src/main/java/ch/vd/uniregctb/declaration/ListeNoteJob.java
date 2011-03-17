package ch.vd.uniregctb.declaration;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.document.ListeNoteRapport;
import ch.vd.uniregctb.rapport.RapportService;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.scheduler.JobParam;
import ch.vd.uniregctb.scheduler.JobParamInteger;

public class ListeNoteJob extends JobDefinition {

	public static final String NAME = "ListeNoteJob";
	private static final String CATEGORIE = "DI";

	public static final String I_NB_THREADS = "nbThreads";
	public static final String PERIODE_FISCALE = "PERIODE";

	private DeclarationImpotService service;

	private RapportService rapportService;

	private PlatformTransactionManager transactionManager;

	public ListeNoteJob(int order, String description) {
		super(NAME, CATEGORIE, order, description);

		final RegDate today = RegDate.get();
		final JobParam param0 = new JobParam();
		param0.setDescription("Période fiscale");
		param0.setName(PERIODE_FISCALE);
		param0.setMandatory(true);
		param0.setType(new JobParamInteger());
		addParameterDefinition(param0, today.year() - 1);

		final JobParam param1 = new JobParam();
		param1.setDescription("Nombre de threads");
		param1.setName(I_NB_THREADS);
		param1.setMandatory(true);
		param1.setType(new JobParamInteger());
		addParameterDefinition(param1, 4);
	}

	public void setRapportService(RapportService rapportService) {
		this.rapportService = rapportService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setService(DeclarationImpotService service) {
		this.service = service;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final RegDate dateTraitement = RegDate.get();
		final StatusManager statusManager = getStatusManager();

		// récupère le nombre de threads paramétrés
		final int nbThreads = getStrictlyPositiveIntegerValue(params, I_NB_THREADS);
		final int annee = getIntegerValue(params, PERIODE_FISCALE);

		// Extrait les résultats dans une transaction read-only (en fait, plusieurs, pour ne pas avoir de timeout de transaction)
		final ListeNoteResults results = service.produireListeNote(dateTraitement, nbThreads, annee, statusManager);

	final ListeNoteRapport rapport = rapportService.generateRapport(results, statusManager);

		setLastRunReport(rapport);
		Audit.success("La production de la liste des contribuables avec une note du "
				+ RegDateHelper.dateToDisplayString(dateTraitement) + " est terminée.", rapport);
	}

}
