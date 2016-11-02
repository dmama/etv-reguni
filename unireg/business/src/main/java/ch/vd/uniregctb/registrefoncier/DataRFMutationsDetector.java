package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.uniregctb.evenement.registrefoncier.EtatEvenementRF;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImport;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFImportDAO;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.uniregctb.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.uniregctb.registrefoncier.dao.ImmeubleRFDAO;
import ch.vd.uniregctb.registrefoncier.elements.XmlHelperRF;
import ch.vd.uniregctb.registrefoncier.helper.ImmeubleRFHelper;
import ch.vd.uniregctb.registrefoncier.key.ImmeubleRFKey;

/**
 * Cette classe reçoit les données extraites de l'import du registre foncier, les compare avec les données en base et génère des événements de mutation correspondants.
 */
public class DataRFMutationsDetector implements DataRFBatcher.Callback {

	private final long importId;
	private final XmlHelperRF xmlHelperRF;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	public DataRFMutationsDetector(long importId,
	                               XmlHelperRF xmlHelperRF,
	                               ImmeubleRFDAO immeubleRFDAO,
	                               EvenementRFImportDAO evenementRFImportDAO,
	                               EvenementRFMutationDAO evenementRFMutationDAO,
	                               PlatformTransactionManager transactionManager) {
		this.importId = importId;
		this.xmlHelperRF = xmlHelperRF;
		this.immeubleRFDAO = immeubleRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
	}

	@Override
	public void onImmeubles(@NotNull List<Grundstueck> immeubles) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {

				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (Grundstueck immeuble : immeubles) {

					if (immeuble.isIstKopie()) {
						// on ignore les bâtiments flaggés comme des copies
						continue;
					}

					final ImmeubleRFKey key = ImmeubleRFHelper.newImmeubleRFKey(immeuble);
					final ImmeubleRF immeubleRF = immeubleRFDAO.find(key);

					// on détermine ce qu'il faut faire
					final EvenementRFMutation.TypeMutation typeMutation;
					if (immeubleRF == null) {
						typeMutation = EvenementRFMutation.TypeMutation.CREATION;
					}
					else if (!ImmeubleRFHelper.currentDataEquals(immeubleRF, immeuble)) {
						typeMutation = EvenementRFMutation.TypeMutation.MODIFICATION;
					}
					else {
						// rien à faire
						continue;
					}

					// on ajoute l'événement à traiter
					final String immeubleAsXml = xmlHelperRF.toXMLString(immeuble);

					final EvenementRFMutation mutation = new EvenementRFMutation();
					mutation.setParentImport(parentImport);
					mutation.setEtat(EtatEvenementRF.A_TRAITER);
					mutation.setTypeEntite(EvenementRFMutation.TypeEntite.IMMEUBLE);
					mutation.setTypeMutation(typeMutation);
					mutation.setXmlContent(immeubleAsXml);

					evenementRFMutationDAO.save(mutation);
				}
			}
		});
	}

	@Override
	public void onDroits(@NotNull List<PersonEigentumAnteil> droits) {

	}

	@Override
	public void onProprietaires(@NotNull List<Personstamm> personnes) {

	}

	@Override
	public void onBatiments(@NotNull List<Gebaeude> batiments) {

	}

	@Override
	public void onSurfaces(@NotNull List<Bodenbedeckung> surfaces) {

	}

	@Override
	public void done() {

	}
}
