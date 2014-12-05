package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.declaration.source.DeterminerLRsEchuesResults;

/**
 * Rapport PDF d'exécution du batch d'échéance des LRs
 */
public class PdfDeterminerLRsEchuesRapport extends PdfRapport {

	public void write(final DeterminerLRsEchuesResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		final Integer periodeFiscale = results.getPeriodeFiscale();
		if (periodeFiscale == null) {
			addTitrePrincipal("Rapport d'exécution du job d'échéance des LR");
		}
		else {
			addTitrePrincipal("Rapport d'exécution du job d'échéance des LR pour la période fiscale " + periodeFiscale);
		}

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Période fiscale considérée :", periodeFiscale == null ? "Toutes" : String.valueOf(periodeFiscale));
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

			addTableSimple(new float[] {70f, 30f}, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de débiteurs analysés :", String.valueOf(results.getNbDebiteursAnalyses()));
					table.addLigne("Nombre de débiteurs ignorés :", String.valueOf(results.ignores.size()));
					table.addLigne("Nombre de listes échues :", String.valueOf(results.lrEchues.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
				}
			});
		}

		// débiteurs ignorés
		{
			final String filename = "debiteurs_ignores.csv";
			final byte[] contenu = getCsvDebiteursNonTraites(results.ignores, filename, status);
			final String titre = "Liste des débiteurs ignorés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// listes récapitulatives échues
		{
			final String filename = "lr_echues.csv";
			final byte[] contenu = getCsvLrEchues(results.lrEchues, filename, status);
			final String titre = "Liste des LR échues";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// erreurs
		{
			final String filename = "erreurs.csv";
			final byte[] contenu = getCsvDebiteursNonTraites(results.erreurs, filename, status);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private <T extends DeterminerLRsEchuesResults.ResultDebiteurNonTraite> byte[] getCsvDebiteursNonTraites(List<T> liste, String filename, StatusManager status) {
		byte[] contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_TIERS").append(COMMA).append("NOM").append(COMMA).append("RAISON").append(COMMA).append("COMMENTAIRE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(info.idDebiteur).append(COMMA);
					b.append(escapeChars(info.nomDebiteur)).append(COMMA);
					b.append(escapeChars(info.getDescriptionRaison())).append(COMMA);
					if (!StringUtils.isBlank(info.getCommentaire())) {
						b.append(CsvHelper.asCsvField(escapeChars(info.getCommentaire())));
					}
					return true;
				}
			});
		}
		return contenu;
	}

	private byte[] getCsvLrEchues(List<DeterminerLRsEchuesResults.ResultLrEchue> lrEchues, String filename, StatusManager status) {
		byte[] contenu = null;
		if (lrEchues != null && !lrEchues.isEmpty()) {
			contenu = CsvHelper.asCsvFile(lrEchues, filename, status, new CsvHelper.FileFiller<DeterminerLRsEchuesResults.ResultLrEchue>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NO_TIERS").append(COMMA).append("CATEGORIE_IS").append(COMMA).append("NOM").append(COMMA).append("DEBUT_PERIODE_LR").append(COMMA).append("FIN_PERIODE_LR");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, DeterminerLRsEchuesResults.ResultLrEchue info) {
					b.append(info.idDebiteur).append(COMMA);
					b.append(info.categorieImpotSource).append(COMMA);
					b.append(escapeChars(info.nomDebiteur)).append(COMMA);
					b.append(info.debutPeriode.index()).append(COMMA);
					b.append(info.finPeriode.index());
					return true;
				}
			});
		}
		return contenu;
	}
}
