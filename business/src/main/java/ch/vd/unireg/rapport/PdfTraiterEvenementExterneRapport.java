package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.evenement.externe.TraiterEvenementExterneResult;

/**
 * Rapport PDF d'exécution du batch d'échéance des LRs
 */
public class PdfTraiterEvenementExterneRapport extends PdfRapport {

	public void write(final TraiterEvenementExterneResult results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la relance du traitement des événements externes ");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{70f, 30f}, table -> {
				table.addLigne("Nombre d'événements en erreur ou non traité à examiner :", String.valueOf(results.nbEvenementTotal));
				table.addLigne("Nombre d'événements traites :", String.valueOf(results.traites.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Messages identifiés
		{
			final String filename = "EvenementExterneTraites.csv";
			final String titre = "Liste des événements externes traités";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = getCsvEvenementTraite(results.traites, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = asCsvErrorFile(results.erreurs, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private <T extends TraiterEvenementExterneResult.Traite> TemporaryFile getCsvEvenementTraite(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Evenement Id").append(COMMA);
					b.append("Numero Tiers").append(COMMA);
					b.append("Date Debut LR").append(COMMA);
					b.append("Date Fin LR").append(COMMA);
					b.append("Action");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.id).append(COMMA);
					b.append(info.numeroTiers).append(COMMA);
					b.append(info.debut).append(COMMA);
					b.append(info.fin).append(COMMA);
					b.append(info.action).append(COMMA);
					return true;
				}
			});
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends TraiterEvenementExterneResult.Erreur> TemporaryFile asCsvErrorFile(List<T> list, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Evenement Id").append(COMMA);
					b.append("Message d'erreur");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T erreur) {
					b.append(erreur.evenementId).append(COMMA);
					b.append(escapeChars(erreur.raison));
					return true;
				}
			});
		}
		return contenu;
	}
}