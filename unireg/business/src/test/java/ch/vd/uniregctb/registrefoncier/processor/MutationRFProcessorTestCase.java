package ch.vd.uniregctb.registrefoncier.processor;

import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;

public abstract class MutationRFProcessorTestCase extends BusinessTest {

	private EvenementRFImportDAO evenementRFImportDAO;
	private EvenementRFMutationDAO evenementRFMutationDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.evenementRFImportDAO = getBean(EvenementRFImportDAO.class, "evenementRFImportDAO");
		this.evenementRFMutationDAO = getBean(EvenementRFMutationDAO.class, "evenementRFMutationDAO");
	}

	protected Long insertMutation(final String xml, final RegDate dateEvenement, final EvenementRFMutation.TypeEntite typeEntite, final EvenementRFMutation.TypeMutation typeMutation, @Nullable String idRF) throws Exception {
		return doInNewTransaction(new ch.vd.registre.base.tx.TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				EvenementRFImport parentImport = new EvenementRFImport();
				parentImport.setEtat(EtatEvenementRF.A_TRAITER);
				parentImport.setDateEvenement(dateEvenement);
				parentImport = evenementRFImportDAO.save(parentImport);

				final EvenementRFMutation mutation = new EvenementRFMutation();
				mutation.setTypeEntite(typeEntite);
				mutation.setTypeMutation(typeMutation);
				mutation.setIdImmeubleRF(idRF);
				mutation.setEtat(EtatEvenementRF.A_TRAITER);
				mutation.setParentImport(parentImport);
				mutation.setXmlContent(xml);

				return evenementRFMutationDAO.save(mutation).getId();
			}
		});
	}
}
