package ch.vd.uniregctb.registrefoncier;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.shared.batchtemplate.BatchCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.uniregctb.common.AuthenticationInterface;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
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
public class DataRFMutationsDetector {

	private static final int BATCH_SIZE = 20;
	private static final int NB_THREADS = 4;

	private final XmlHelperRF xmlHelperRF;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final EvenementRFImportDAO evenementRFImportDAO;
	private final EvenementRFMutationDAO evenementRFMutationDAO;
	private final PlatformTransactionManager transactionManager;

	public DataRFMutationsDetector(XmlHelperRF xmlHelperRF,
	                               ImmeubleRFDAO immeubleRFDAO,
	                               EvenementRFImportDAO evenementRFImportDAO,
	                               EvenementRFMutationDAO evenementRFMutationDAO,
	                               PlatformTransactionManager transactionManager) {
		this.xmlHelperRF = xmlHelperRF;
		this.immeubleRFDAO = immeubleRFDAO;
		this.evenementRFImportDAO = evenementRFImportDAO;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
		this.transactionManager = transactionManager;
	}

	public void processImmeubles(long importId, @NotNull Iterator<Grundstueck> iterator) {

		final ParallelBatchTransactionTemplate<Grundstueck> template = new ParallelBatchTransactionTemplate<Grundstueck>(iterator, BATCH_SIZE, NB_THREADS, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null, AuthenticationInterface.INSTANCE) {
			@Override
			protected int getBlockingQueueCapacity() {
				// on limite la queue interne du template à 10 lots de BATCH_SIZE, autrement
				// on sature rapidemment la mémoire de la JVM avec l'entier du fichier d'import.
				return 10;
			}
		};

		template.execute(new BatchCallback<Grundstueck>() {
			@Override
			public boolean doInTransaction(List<Grundstueck> batch) throws Exception {
				final EvenementRFImport parentImport = evenementRFImportDAO.get(importId);

				for (Grundstueck immeuble : batch) {

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

				return true;
			}
		}, null);
	}

	public void processDroits(long importId, Iterator<PersonEigentumAnteil> iterator) {
		// TODO (msi) implémenter la détection des mutations des droits
		while (iterator.hasNext()) {
			iterator.next();
		}
	}

	public void processProprietaires(long importId, Iterator<Personstamm> iterator) {
		// TODO (msi) implémenter la détection des mutations des propriétaires
		while (iterator.hasNext()) {
			iterator.next();
		}
	}

	public void processConstructions(long importId, Iterator<Gebaeude> iterator) {
		// TODO (msi) implémenter la détection des mutations des bâtiments
		while (iterator.hasNext()) {
			iterator.next();
		}
	}

	public void processSurfaces(long importId, Iterator<Bodenbedeckung> iterator) {
		// TODO (msi) implémenter la détection des mutations des surfaces
		while (iterator.hasNext()) {
			iterator.next();
		}
	}
}
