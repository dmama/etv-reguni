package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.identification.individus.IdentificationIndividu;
import ch.vd.uniregctb.identification.individus.IdentificationIndividusNonMigresResults;

public class PdfIdentIndividusNonMigresRapport extends PdfRapport {

	public void write(final IdentificationIndividusNonMigresResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job d'indentification des individus non-migrés par RcPers");

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\nles valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre d'individus considérés :", String.valueOf(results.nbCtbsTotal));
					table.addLigne("Nombre d'individus identifiés :", String.valueOf(results.identifies.size()));
					table.addLigne("Nombre d'individus non-identifiés :", String.valueOf(results.nonIdentifies.size()));
					table.addLigne("Nombre d'individus ignorés :", String.valueOf(results.ignores.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(results.endTime - results.startTime));
					table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
				}
			});
		}

		// Cas traités
		{
			final String filename = "individus_identifies.csv";
			final String contenu = buildContenuIdentifies(results.identifies, status, filename);
			final String titre = "Liste des individus identifiées";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Cas non-identifiés
		{
			final String filename = "individus_non_identifies.csv";
			final String contenu = buildContenuNonIdentifies(results.nonIdentifies, status, filename);
			final String titre = "Liste des individus non-identifiées";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Cas ignorés
		{
			final String filename = "individus_ignores.csv";
			final String contenu = buildContenuIgnores(results.ignores, status, filename);
			final String titre = "Liste des individus ignorés";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = buildContenuErreurs(results.erreurs, status, filename);
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}

		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private String buildContenuIdentifies(List<IdentificationIndividusNonMigresResults.Identifie> traites, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(traites, filename, status, new CsvHelper.FileFiller<IdentificationIndividusNonMigresResults.Identifie>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_TIERS").append(COMMA).append("STRATEGIE_IDENTIFICATION").append(COMMA);
				b.append("REGPP_NO_IND").append(COMMA).append("REGPP_PRENOM").append(COMMA).append("REGPP_NOM").append(COMMA).append("REGPP_DATE_NAISSANCE").append(COMMA).append("REGPP_NO_AVS13").append(COMMA);
				b.append("RCPERS_NO_IND").append(COMMA).append("RCPERS_PRENOM").append(COMMA).append("RCPERS_NOM").append(COMMA).append("RCPERS_DATE_NAISSANCE").append(COMMA).append("RCPERS_NO_AVS13").append(COMMA);
				b.append("REMARQUE").append(COMMA).append("NO_TIERS_DEJA_LIES").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, IdentificationIndividusNonMigresResults.Identifie data) {
				b.append(data.noCtb).append(COMMA).append(data.strategie).append(COMMA);
				fillIdentificationLine(b, data.identiteRegPP);
				fillIdentificationLine(b, data.identiteRcPers);
				if (data.remarque != null) {
					b.append(data.remarque);
				}
				b.append(COMMA);
				if (data.tiersDejaLies != null) {
					b.append(idsToString(data.tiersDejaLies));
				}
				b.append(COMMA);
				return true;
			}

		});
	}

	private String buildContenuNonIdentifies(List<IdentificationIndividusNonMigresResults.NonIdentifie> traites, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(traites, filename, status, new CsvHelper.FileFiller<IdentificationIndividusNonMigresResults.NonIdentifie>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_TIERS").append(COMMA);
				b.append("REGPP_NO_IND").append(COMMA).append("REGPP_PRENOM").append(COMMA).append("REGPP_NOM").append(COMMA).append("REGPP_DATE_NAISSANCE").append(COMMA).append("REGPP_NO_AVS13").append(COMMA);
				b.append("RAISON").append(COMMA).append("DETAILS").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, IdentificationIndividusNonMigresResults.NonIdentifie data) {
				b.append(data.noCtb).append(COMMA);
				fillIdentificationLine(b, data.identiteRegPP);
				if (data.raison != null) {
					b.append(data.raison);
				}
				b.append(COMMA);
				if (data.details != null) {
					b.append(data.details);
				}
				b.append(COMMA);
				return true;
			}

		});
	}

	private static String idsToString(List<Long> ids) {
		final StringBuilder sb = new StringBuilder();
		for (Long id : ids) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(id);
		}
		return sb.toString();
	}

	private static CsvHelper.LineFiller fillIdentificationLine(CsvHelper.LineFiller b, IdentificationIndividu ident) {
		if (ident == null) {
			return b.append(COMMA).append(COMMA).append(COMMA).append(COMMA);
		}
		else {
			appendValueAndComma(b, ident.noInd);
			appendValueAndComma(b, ident.prenom);
			appendValueAndComma(b, ident.nom);
			appendValueAndComma(b, ident.dateNaissance);
			appendValueAndComma(b, ident.noAVS13);
			return b;
		}
	}

	private static void appendValueAndComma(CsvHelper.LineFiller b, Object value) {
		if (value != null) {
			b.append(value);
		}
		b.append(COMMA);
	}

	private String buildContenuIgnores(List<IdentificationIndividusNonMigresResults.Ignore> ignores, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(ignores, filename, status, new CsvHelper.FileFiller<IdentificationIndividusNonMigresResults.Ignore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_TIERS").append(COMMA).append("NO_IND").append(COMMA).append("RAISON").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, IdentificationIndividusNonMigresResults.Ignore elt) {
				b.append(elt.noCtb).append(COMMA).append(elt.identiteRegPP.noInd).append(COMMA).append(CsvHelper.escapeChars(elt.raison.description())).append(COMMA);
				return true;
			}
		});
	}

	private String buildContenuErreurs(List<IdentificationIndividusNonMigresResults.Erreur> erreurs, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(erreurs, filename, status, new CsvHelper.FileFiller<IdentificationIndividusNonMigresResults.Erreur>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_TIERS").append(COMMA);
				b.append("REGPP_NO_IND").append(COMMA).append("REGPP_PRENOM").append(COMMA).append("REGPP_NOM").append(COMMA).append("REGPP_DATE_NAISSANCE").append(COMMA).append("REGPP_NO_AVS13").append(COMMA);
				b.append("RCPERS_NO_IND").append(COMMA);
				b.append("RAISON").append(COMMA).append("DETAILS").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, IdentificationIndividusNonMigresResults.Erreur elt) {
				b.append(elt.noCtb).append(COMMA);
				fillIdentificationLine(b, elt.identiteRegPP);
				if (elt.noIndRcpers != null) {
					b.append(elt.noIndRcpers);
				}
				b.append(COMMA);
				b.append(CsvHelper.escapeChars(elt.raison.description())).append(COMMA).append(CsvHelper.escapeChars(elt.details)).append(COMMA);
				return true;
			}
		});
	}
}
