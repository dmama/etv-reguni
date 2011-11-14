package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
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
			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{70f, 30f}, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de messages identifiés :", String.valueOf(results.identifies.size()));
					table.addLigne("Nombre de messages non identifiés :", String.valueOf(results.nonIdentifies.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Messages identifiés
		{
			final String filename = "messages_identifies.csv";
			final String contenu = getCsvMessagesIdentifies(results.identifies, filename, status);
			final String titre = "Liste des messages identifiés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.identifies.size(), titre, listVide, filename, contenu);
		}

		// Messages non-identifiés
		{
			final String filename = "messages_non_identifies.csv";
			final String contenu = getCsvMessagesNonIdentifies(results.nonIdentifies, filename, status);
			final String titre = "Liste des messages non-identifiés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, results.nonIdentifies.size(), titre, listVide, filename, contenu);
		}

		// erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = asCsvFile(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private <T extends IdentifierContribuableResults.Identifie> String getCsvMessagesIdentifies(List<T> liste, String filename, StatusManager status) {
		String contenu = null;
		if (liste != null && liste.size() > 0) {
			contenu = CsvHelper.asCsvFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
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

	private <T extends IdentifierContribuableResults.NonIdentifie> String getCsvMessagesNonIdentifies(List<T> liste, String filename, StatusManager status) {
		String contenu = null;
		if (liste != null && liste.size() > 0) {
			contenu = CsvHelper.asCsvFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
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
	protected static <T extends IdentifierContribuableResults.Erreur> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<T>() {
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