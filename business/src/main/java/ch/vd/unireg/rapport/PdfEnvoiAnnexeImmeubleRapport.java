package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.ordinaire.pp.EnvoiAnnexeImmeubleResults;

/**
 * Rapport PDF contenant les résultats de l'exécution d'un job de traitement des annexes immeubles.
 */
public class PdfEnvoiAnnexeImmeubleRapport extends PdfRapport {

	/**
	 * Génère un rapport au format PDF à partir des résultats de job.
	 */
	public void write(final EnvoiAnnexeImmeubleResults results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'envoi des annexes immeuble en masse pour l'année " + results.annee);

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Période fiscale considérée :", String.valueOf(results.annee));
				table.addLigne("Nombre maximum de contribuables :", String.valueOf(results.nbMaxAnnexes));
				table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, table -> {
				table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
				table.addLigne("Nombre de contribuables traités:", String.valueOf(results.ctbsTraites.size()));
				table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
				table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}


		// CTBs ignorés
		{
			String filename = "contribuables_traites.csv";
			String titre = "Liste des contribuables traités";
			String listVide = "(aucun contribuable traités)";
			try (TemporaryFile contenu = asCsvFileForInfoImmeuble(results.infoCtbTraites, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// CTBs ignorés
		{
			String filename = "contribuables_ignores.csv";
			String titre = "Liste des contribuables ignorés";
			String listVide = "(aucun contribuable ignoré)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsIgnores, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// CTBs en erreurs
		{
			String filename = "contribuables_en_erreur.csv";
			String titre = "Liste des contribuables en erreur";
			String listVide = "(aucun contribuable en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.ctbsEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}


		close();

		status.setMessage("Génération du rapport terminée.");
	}

	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	private static <T extends EnvoiAnnexeImmeubleResults.InfoCtbImmeuble> TemporaryFile asCsvFileForInfoImmeuble(List<T> list, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("OID").append(COMMA).append("NO_CTB").append(COMMA).append("NOM")
						.append(COMMA).append("NOMBRE ANNEXES IMMEUBLE IMPRIMMEES");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, T elt) {
				b.append(elt.officeImpotID != null ? elt.officeImpotID : EMPTY).append(COMMA);
				b.append(elt.noCtb).append(COMMA);
				b.append(escapeChars(elt.nomCtb)).append(COMMA);
				b.append(elt.nbAnnexeEnvoyee);
				return true;
			}
		});
	}

}