package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.metier.ComparerForFiscalEtCommuneResults;

/**
 * Rapport PDF d'exécution du batch de comparaison du dernier for fiscal et de la commune de résidence
 */
public class PdfComparerForFiscalEtCommuneRapport extends PdfRapport {

	public void write(final ComparerForFiscalEtCommuneResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la comparaison du dernier for fiscal et de la commune de résidence ");

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
				table.addLigne("Nombre de contribuables analysés :", String.valueOf(results.nbCtbTotal));
				table.addLigne("Nombre de communes différentes  :", String.valueOf(results.listeCommunesDifferentes.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}



		// adresses resolues
			{
				final String filename = "for_et_commune_differente.csv";
				final String titre = "Liste des communes différentes";
				final String listVide = "(aucune)";
				try ( TemporaryFile contenu = getCsvForFiscalEtCommuneDifferente(results.listeCommunesDifferentes, filename, status)) {
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

	

	private <T extends ComparerForFiscalEtCommuneResults.CommunesDifferentes> TemporaryFile getCsvForFiscalEtCommuneDifferente(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("FOR FISCAL ID ").append(COMMA);
					b.append("NUMERO CTB").append(COMMA);
					b.append("COMMUNE FOR").append(COMMA);
					b.append("DATE DEBUT VALIDITE").append(COMMA);
					b.append("COMMUNE ADRESSE RESIDENCE").append(COMMA);
					b.append("DATE DEBUT ADRESSE RESIDENCE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.id).append(COMMA);
					b.append(info.numeroContribuable).append(COMMA);
					b.append(info.nomCommuneFor).append(COMMA);
					b.append(info.dateDebutFor).append(COMMA);
					b.append(info.nomCommuneAdresse).append(COMMA);
					b.append(info.dateDebutAdresse).append(COMMA);
					return true;
				}
			});
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ComparerForFiscalEtCommuneResults.Erreur> TemporaryFile asCsvErrorFile(List<T> list, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numero Contribuable").append(COMMA).append("Message d'erreur");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.id).append(COMMA);
					b.append(escapeChars(info.message));
					return true;
				}
			});
		}
		return contenu;
	}


}