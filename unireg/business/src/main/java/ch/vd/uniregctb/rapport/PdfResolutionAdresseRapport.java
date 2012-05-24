package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.ResolutionAdresseResults;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;

/**
 * Rapport PDF d'exécution du batch d'échéance des LRs
 */
public class PdfResolutionAdresseRapport extends PdfRapport {

	public void write(final ResolutionAdresseResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la résolution des adresses ");

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
					table.addLigne("Nombre d'adresses traitées :", String.valueOf(results.nbAdresseTotal));
					table.addLigne("Nombre d'adresses résolues :", String.valueOf(results.listeAdresseResolues.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}



		// adresses resolues
			{
				final String filename = "adresses_resolues.csv";
				final String contenu = getCsvAdresseResolue(results.listeAdresseResolues, filename, status);
				final String titre = "Liste des adresses résolues";
				final String listVide = "(aucune)";
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}



		// erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = asCsvFile(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	

	private <T extends ResolutionAdresseResults.InfoAdresse> String getCsvAdresseResolue(List<T> liste, String filename, StatusManager status) {
		String contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NUMERO TIERS ").append(COMMA);
					b.append("NUMERO ADRESSE").append(COMMA);
					b.append("NUMERO RUE").append(COMMA);
					b.append("NOM RUE").append(COMMA);
					b.append("NOM LOCALITE").append(COMMA);
					b.append("NUMERO ORDRE POSTAL");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.tiersId).append(COMMA);
					b.append(info.adresseId).append(COMMA);
					b.append(info.rueId).append(COMMA);
					b.append(info.nomRue).append(COMMA);
					b.append(info.localite).append(COMMA);
					b.append(info.numeroOrdrePostal);
					return true;
				}
			});
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ResolutionAdresseResults.Erreur> String asCsvFile(List<T> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ADRESSE_ID").append(COMMA).append("ERREUR");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.id).append(COMMA);
					b.append(escapeChars(info.raison));
					return true;
				}
			});
		}
		return contenu;
	}


}