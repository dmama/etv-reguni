package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.ListesResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.listes.assujettis.ListeAssujettisResults;

/**
 * Rapport PDF d'exécution du batch d'extraction de la liste des assujettis d'une période fiscale
 */
public class PdfListeAssujettisRapport extends PdfRapport {

	public void write(final ListeAssujettisResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'extraction de la liste des assujettis d'une période fiscale");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Période fiscale :", String.valueOf(results.getAnneeFiscale()));
				table.addLigne("Nombre de threads :", String.valueOf(results.getNombreThreads()));
				table.addLigne("Avec sourciers purs :", String.valueOf(results.isAvecSourciersPurs()));
				table.addLigne("Seulement assujettis fin année :", String.valueOf(results.isSeulementAssujettisFinAnnee()));
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre de contribuables inspectés :", String.valueOf(results.getNbContribuablesInspectes()));
				table.addLigne("Nombre de contribuables assujettis :", String.valueOf(results.getNbCtbAssujettis()));
				table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.getNbCtbIgnores()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getListeErreurs().size()));
				table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		// Assujettis trouvés
		{
			final String filename = "assujettis.csv";
			final String titre = "Liste des contribuables assujettis et de leurs assujettissements";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContenuAssujettis(results.getAssujettis(), status, filename)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Contribuables ignorés
		{
			final String filename = "ignores.csv";
			final String titre = "Liste des contribuables ignorés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = buildContenuIgnores(results.getIgnores(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = buildContenuErreurs(results.getListeErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile buildContenuAssujettis(List<ListeAssujettisResults.InfoCtbAssujetti> liste, StatusManager status, String filename) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListeAssujettisResults.InfoCtbAssujetti>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("TYPE_ASSUJETTISSEMENT").append(COMMA)
						.append("DATE_DEBUT").append(COMMA).append("DATE_FIN").append(COMMA)
						.append("MOTIF_FRAC_DEBUT").append(COMMA).append("MOTIF_FRAC_FIN").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ListeAssujettisResults.InfoCtbAssujetti elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.typeAssujettissement != null ? elt.typeAssujettissement.getDescription() : StringUtils.EMPTY).append(COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.debutAssujettissement)).append(COMMA);
				b.append(RegDateHelper.dateToDisplayString(elt.finAssujettissement)).append(COMMA);
				b.append(elt.motifDebut != null ? elt.motifDebut.getDescription(true) : StringUtils.EMPTY).append(COMMA);
				b.append(elt.motifFin != null ? elt.motifFin.getDescription(false) : StringUtils.EMPTY);
				return true;
			}
		});
	}

	private static TemporaryFile buildContenuIgnores(List<ListeAssujettisResults.InfoCtbIgnore> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListeAssujettisResults.InfoCtbIgnore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ListeAssujettisResults.InfoCtbIgnore elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(escapeChars(elt.cause.description));
				return true;
			}
		});
	}

	private static TemporaryFile buildContenuErreurs(List<ListeAssujettisResults.Erreur> results, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(results, filename, status, new CsvHelper.FileFiller<ListeAssujettisResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("RAISON").append(COMMA).append("DETAILS");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ListesResults.Erreur elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(elt.getDescriptionRaison()).append(COMMA);
				b.append(asCsvField(elt.details));
				return true;
			}
		});
	}
}
