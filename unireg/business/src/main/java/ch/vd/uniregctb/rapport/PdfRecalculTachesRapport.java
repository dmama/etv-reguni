package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.tache.TacheSyncResults;

/**
 * Rapport PDF contenant les résultats du rapprochement des ctb et des propriétaires fonciers.
 */
public class PdfRecalculTachesRapport extends PdfRapport {

	public void write(final TacheSyncResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de re-calcul des tâches d'envoi et d'annulation de déclaration d'impôt");

		// Paramètres
		addEntete1("Paramètre");
		{
			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nettoyage seul : ", String.valueOf(results.isCleanupOnly()));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total d'actions menées :", String.valueOf(results.getActions().size()));
					table.addLigne("Nombre total d'erreurs :", String.valueOf(results.getExceptions().size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
				}
			});
		}

		// Action menées
		{
			final String filename = "actions_menees.csv";
			final byte[] contenu = actionsAsCsvFile(results.getActions(), filename, status);
			final String titre = "Liste des actions menées par le job";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final byte[] contenu = errorsAsCsvFile(results.getExceptions(), filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private byte[] actionsAsCsvFile(List<TacheSyncResults.ActionInfo> actions, String fileName, StatusManager status) {
		return CsvHelper.asCsvFile(actions, fileName, status, new CsvHelper.FileFiller<TacheSyncResults.ActionInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("ACTION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, TacheSyncResults.ActionInfo elt) {
				b.append(elt.ctbId).append(COMMA);
				b.append(asCsvField(escapeChars(elt.actionMsg)));
				return true;
			}
		});
	}

	private byte[] errorsAsCsvFile(List<TacheSyncResults.ExceptionInfo> exceptions, String fileName, StatusManager status) {
		return CsvHelper.asCsvFile(exceptions, fileName, status, new CsvHelper.FileFiller<TacheSyncResults.ExceptionInfo>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("EXCEPTION");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, TacheSyncResults.ExceptionInfo elt) {
				b.append(elt.ctbId).append(COMMA);
				b.append(asCsvField(escapeChars(elt.exceptionMsg)));
				return true;
			}
		});
	}
}

