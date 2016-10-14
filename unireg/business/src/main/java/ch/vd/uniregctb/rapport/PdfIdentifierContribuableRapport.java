package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableResults;

/**
 * Rapport PDF d'exécution du batch d'échéance des LRs
 */
public class PdfIdentifierContribuableRapport extends PdfRapport {

	public void write(final IdentifierContribuableResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la relance de l'identification automatique ");

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
				table.addLigne("Nombre de messages identifiés :", String.valueOf(results.identifies.size()));
				table.addLigne("Nombre de messages non identifiés :", String.valueOf(results.nonIdentifies.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// Messages identifiés
		{
			final String filename = "messages_identifies.csv";
			final String titre = "Liste des messages identifiés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = getCsvMessagesIdentifies(results.identifies, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Messages non-identifiés
		{
			final String filename = "messages_non_identifies.csv";
			final String titre = "Liste des messages non-identifiés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = getCsvMessagesNonIdentifies(results.nonIdentifies, filename, status)) {
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

	private <T extends IdentifierContribuableResults.Identifie> TemporaryFile getCsvMessagesIdentifies(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Business-ID Message").append(COMMA);
					b.append("PRENOMS").append(COMMA);
					b.append("NOM").append(COMMA);
					b.append("NUMERO CTB").append(COMMA);
					b.append("NUMERO MENAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(escapeChars(info.businessID)).append(COMMA);
					b.append(escapeChars(info.prenom)).append(COMMA);
					b.append(escapeChars(info.nom)).append(COMMA);
					b.append(info.noCtb).append(COMMA);
					if (info.noCtbMenage != null) {
						b.append(info.noCtbMenage);
					}
					return true;
				}
			});
		}
		return contenu;
	}

	private <T extends IdentifierContribuableResults.NonIdentifie> TemporaryFile getCsvMessagesNonIdentifies(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Business-ID Message").append(COMMA);
					b.append("PRENOMS").append(COMMA);
					b.append("NOM");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(escapeChars(info.businessID)).append(COMMA);
					b.append(escapeChars(info.prenom)).append(COMMA);
					b.append(escapeChars(info.nom));
					return true;
				}
			});
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends IdentifierContribuableResults.Erreur> TemporaryFile asCsvErrorFile(List<T> list, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("id du message").append(COMMA);
					b.append("Message d'erreur");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.messageId).append(COMMA);
					b.append(escapeChars(info.raison));
					return true;
				}
			});
		}
		return contenu;
	}


}