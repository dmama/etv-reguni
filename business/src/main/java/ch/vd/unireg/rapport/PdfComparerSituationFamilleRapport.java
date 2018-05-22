package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.situationfamille.ComparerSituationFamilleResults;

/**
 * Rapport PDF d'exécution du batch de comparaison des situations de famille
 */
public class PdfComparerSituationFamilleRapport extends PdfRapport {

	public void write(final ComparerSituationFamilleResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de la comparaison des situations des familles enregistrées dans UNIREG et celles du civil ");

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
				table.addLigne("Nombre de situations analysées :", String.valueOf(results.nbSituationTotal));
				table.addLigne("Nombre de situations différentes  :", String.valueOf(results.listeSituationsDifferentes.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}


		// adresses resolues
		{
			final String filename = "situations_differentes.csv";
			final String titre = "Liste des situations différentes";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = getCsvSituationsDifferentes(results.listeSituationsDifferentes, filename, status)) {
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

	

	private <T extends ComparerSituationFamilleResults.SituationsDifferentes> TemporaryFile getCsvSituationsDifferentes(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("SITUATION ID ").append(COMMA);
					b.append("NUMERO CTB").append(COMMA);
					b.append("ETAT UNIREG").append(COMMA);
					b.append("DATE DEBUT UNIREG").append(COMMA);
					b.append("ETAT HOST").append(COMMA);
					b.append("DATE DEBUT HOST");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.id).append(COMMA);
					b.append(info.numeroContribuable).append(COMMA);
					b.append(info.etatCivil).append(COMMA);
					b.append(info.dateDebutEtatCivil).append(COMMA);
					b.append(info.etatCivilHost).append(COMMA);
					b.append(info.dateDebutEtatCivilHost);
					return true;
				}
			});
		}
		return contenu;
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends ComparerSituationFamilleResults.Erreur> TemporaryFile asCsvErrorFile(List<T> list, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("id de la situation").append(COMMA).append("Message d'erreur");
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