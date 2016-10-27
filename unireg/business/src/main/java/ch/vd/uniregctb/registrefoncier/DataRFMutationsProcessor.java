package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.tx.TxCallback;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.processor.ImmeubleRFProcessor;
import ch.vd.uniregctb.registrefoncier.processor.MutationRFProcessor;

/**
 * Processeur responsable de traiter les mutations du registre foncier.
 */
public class DataRFMutationsProcessor {

	private final XmlHelperRF xmlHelperRF;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	private final ImmeubleRFProcessor immeubleProcessor;

	public DataRFMutationsProcessor(@NotNull XmlHelperRF xmlHelperRF,
	                                @NotNull ImmeubleRFDAO immeubleRFDAO,
	                                @NotNull EvenementRFImportDAO evenementRFImportDAO,
	                                @NotNull EvenementRFMutationDAO evenementRFMutationDAO,
	                                @NotNull PlatformTransactionManager transactionManager) {
		this.xmlHelperRF = xmlHelperRF;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;

		this.immeubleProcessor = new ImmeubleRFProcessor(immeubleRFDAO, xmlHelperRF);
	}

	/**
	 * Traite tous les mutations à l'état A_TRAITER de l'import spécifié
	 *
	 * @param importId l'id d'un import du registre foncier
	 */
	public void processImport(long importId) {

		final List<Long> ids = findIdsMutationsATraiter(importId);

		// TODO (msi) générer un rapport
		final BatchTransactionTemplate template = new BatchTransactionTemplate(ids, 100, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		template.execute(new BatchCallback<Long>() {
			@Override
			public boolean doInTransaction(List<Long> mutationsIds) throws Exception {
				mutationsIds.stream()
						.map(id -> getMutation(id))
						.forEach(mut -> processMutation(mut));
				return true;
			}
		}, null);
	}

	private void processMutation(@NotNull EvenementRFMutation mut) {
		final MutationRFProcessor proc = getProcessor(mut);
		proc.process(mut);
	}

	@NotNull
	private EvenementRFMutation getMutation(Long id) {
		final EvenementRFMutation mutation = evenementRFMutationDAO.get(id);
		if (mutation == null) {
			throw new IllegalArgumentException("La mutation RF avec l'id=[" + id + "] n'existe pas.");
		}
		return mutation;
	}

	@NotNull
	private MutationRFProcessor getProcessor(@NotNull EvenementRFMutation mut) {
		switch (mut.getTypeEntite()) {
		case AYANT_DROIT:
			// TODO (msi)
			throw new NotImplementedException();
		case BATIMENT:
			// TODO (msi)
			throw new NotImplementedException();
		case DROIT:
			// TODO (msi)
			throw new NotImplementedException();
		case IMMEUBLE:
			return immeubleProcessor;
		case SURFACE:
			// TODO (msi)
			throw new NotImplementedException();
		default:
			throw new IllegalArgumentException("Type d'entité RF inconnue = [" + mut.getTypeEntite() + "]");
		}
	}

	@NotNull
	private List<Long> findIdsMutationsATraiter(long importId) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TxCallback<List<Long>>() {
			@Override
			public List<Long> execute(TransactionStatus status) throws Exception {
				return evenementRFMutationDAO.findIds(importId, EtatEvenementRF.A_TRAITER);
			}
		});
	}
}
