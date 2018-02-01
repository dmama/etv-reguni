package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.uniregctb.evenement.registrefoncier.TypeMutationRF;
import ch.vd.uniregctb.registrefoncier.dataimport.MutationsRFProcessorResults;

/**
 * Rapport PDF contenant les résultats du job de traitement des mutations sur les immeubles du RF.
 */
public class PdfMutationsRFProcessorRapport extends PdfRapport {

	public void write(final MutationsRFProcessorResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de traitement des mutations du RF.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Id de l'import :", String.valueOf(results.getImportId()));
				table.addLigne("Import initial :", String.valueOf(results.isImportInitial()));
				table.addLigne("Date de valeur :", RegDateHelper.dateToDisplayString(results.getDateValeur()));
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				for (TypeEntiteRF typeEntity : TypeEntiteRF.values()) {
					final String entityName = getName(typeEntity);
					table.addLigne("Mutations traitées sur les " + entityName + " :", "");
					for (TypeMutationRF typeMutation : TypeMutationRF.values()) {
						final String typeMutationName = getName(typeMutation);
						final MutableLong count = results.getProcessed().get(new MutationsRFProcessorResults.ProcessedKey(typeEntity, typeMutation));
						table.addLigne("  - " + typeMutationName + " :", count == null ? "0" : String.valueOf(count.getValue()));
					}
				}
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getNbErreurs()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des mutations en erreur";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	@NotNull
	private static String getName(TypeMutationRF typeMutation) {
		final String typeMutationName;
		switch (typeMutation) {
		case CREATION:
			typeMutationName = "création";
			break;
		case MODIFICATION:
			typeMutationName = "modification";
			break;
		case SUPPRESSION:
			typeMutationName = "suppression";
			break;
		default:
			throw new IllegalArgumentException("Type de mutation inconnu = [" + typeMutation + "]");
		}
		return typeMutationName;
	}

	@NotNull
	private static String getName(TypeEntiteRF typeEntity) {
		switch (typeEntity) {
		case AYANT_DROIT:
			return "ayant-droits";
		case BATIMENT:
			return "bâtiments";
		case COMMUNE:
			return "communes";
		case DROIT:
			return "droits";
		case IMMEUBLE:
			return "immeubles";
		case SURFACE_AU_SOL:
			return "surfaces au sol";
		case SERVITUDE:
			return "servitudes";
		default:
			throw new IllegalArgumentException("Type d'entité inconnue = [" + typeEntity + "]");
		}
	}

	private TemporaryFile erreursAsCsvFile(List<MutationsRFProcessorResults.Erreur> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<MutationsRFProcessorResults.Erreur>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MUTATION_ID").append(COMMA);
					b.append("TYPE_ENTITE").append(COMMA);
					b.append("TYPE_MUTATION").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, MutationsRFProcessorResults.Erreur elt) {
					b.append(elt.getMutationId()).append(COMMA);
					b.append(elt.getTypeEntite()).append(COMMA);
					b.append(elt.getTypeMutation()).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.getMessage()));
					return true;
				}
			});
		}
		return contenu;
	}
}