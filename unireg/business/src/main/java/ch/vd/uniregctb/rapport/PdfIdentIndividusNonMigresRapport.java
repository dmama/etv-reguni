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
import ch.vd.uniregctb.identification.individus.IdentificationIndividuMigrationNH;
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

		// Liste des individus en vue de migration NH
		{
			final String filename = "migration_nh.csv";
			final String contenu = buildContenuMigrationNH(results.enVueDeMigrationNH, status, filename);
			final String titre = "Liste des individus à migrer en Non-Habitant";
			final String listVide = "(aucun)";
			addListeDetaillee(writer, titre, listVide, filename, contenu);
		}


		close();
		status.setMessage("Génération du rapport terminée.");
	}

	private String buildContenuMigrationNH(List<IdentificationIndividusNonMigresResults.EnVueDeMigrationNH> enVueDeMigrationNH, StatusManager status, String filename) {
		return CsvHelper.asCsvFile(enVueDeMigrationNH, filename, status, new CsvHelper.FileFiller<IdentificationIndividusNonMigresResults.EnVueDeMigrationNH>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				appendValueAndComma(b, "NO_TIERS");
				appendValueAndComma(b, "PRENOM");
				appendValueAndComma(b, "NOM");
				appendValueAndComma(b, "DATE_NAISSANCE");
				appendValueAndComma(b, "DATE_DECES");
				appendValueAndComma(b, "NO_AVS13");
				appendValueAndComma(b, "SEXE");
				appendValueAndComma(b, "CAT_ETRANGER");
				appendValueAndComma(b, "DATE_DEBUT_VALID_AUTORIS");
				appendValueAndComma(b, "NO_OFS_NATIONALITE");
				fillAdresseLineHeader(b);
				appendValueAndComma(b, "ETAT_CIVIL_TYPE");
				appendValueAndComma(b, "ETAT_CIVIL_DATE_DEBUT");
				appendValueAndComma(b, "REMARQUE");
			}
			@Override
			public boolean fillLine(CsvHelper.LineFiller b, IdentificationIndividusNonMigresResults.EnVueDeMigrationNH data) {
				appendValueAndComma(b, data.noCtb);
				appendValueAndComma(b, data.identiteRegPP.prenom);
				appendValueAndComma(b, data.identiteRegPP.nom);
				appendValueAndComma(b, data.identiteRegPP.dateNaissance != null ? data.identiteRegPP.dateNaissance.index() : null);
				appendValueAndComma(b, data.identiteRegPP.dateDeces != null ? data.identiteRegPP.dateDeces.index() : null);
				appendValueAndComma(b, data.identiteRegPP.noAVS13);
				appendValueAndComma(b, data.identiteRegPP.sexe);
				appendValueAndComma(b, data.identiteRegPP.categorieEtranger);
				appendValueAndComma(b, data.identiteRegPP.dateDebutValiditeAutorisation != null ? data.identiteRegPP.dateDebutValiditeAutorisation.index() : null);
				appendValueAndComma(b, data.identiteRegPP.numeroOfsNationalite);
				fillAdresseLine(b, data.identiteRegPP);
				appendValueAndComma(b, data.identiteRegPP.typeEtatCivil);
				appendValueAndComma(b, data.identiteRegPP.dateDebutEtatCivil != null ? data.identiteRegPP.dateDebutEtatCivil.index() : null);
				appendValueAndComma(b, data.remarque);
				return true;
			}

		});
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
			return b.append(COMMA).append(COMMA).append(COMMA).append(COMMA).append(COMMA);
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

	private static void fillAdresseLineHeader(CsvHelper.LineFiller b) {
		appendValueAndComma(b, "ADRESSE_TYPE");
		appendValueAndComma(b, "ADRESSE_DATE_DEBUT");
		appendValueAndComma(b, "ADRESSE_NO_OFS_PAYS");
		appendValueAndComma(b, "ADRESSE_NPA_CASE_POSTALE");
		appendValueAndComma(b, "ADRESSE_NO_APPARTEMENT");
		appendValueAndComma(b, "ADRESSE_NO_CASE_POSTALE");
		appendValueAndComma(b, "ADRESSE_NO_ORDRE_POSTAL");
		appendValueAndComma(b, "ADRESSE_NO_POSTAL");
		appendValueAndComma(b, "ADRESSE_NO_RUE");
		appendValueAndComma(b, "ADRESSE_RUE");
		appendValueAndComma(b, "ADRESSE_TEXTE_CASE_POSTALE");
	}


	private static CsvHelper.LineFiller fillAdresseLine(CsvHelper.LineFiller b, IdentificationIndividuMigrationNH ident) {
		if (ident == null || ident.adresse == null) {
			return b.append(COMMA).append(COMMA).append(COMMA).append(COMMA)
					.append(COMMA).append(COMMA).append(COMMA).append(COMMA)
					.append(COMMA).append(COMMA).append(COMMA);
		} else {
			appendValueAndComma(b, ident.adresse.type);
			appendValueAndComma(b, ident.adresse.dateDebut != null ? ident.adresse.dateDebut.index() : null);
			appendValueAndComma(b, ident.adresse.noOfsPays);
			appendValueAndComma(b, ident.adresse.npaCasePostale);
			appendValueAndComma(b, ident.adresse.numeroAppartement);
			appendValueAndComma(b, ident.adresse.numeroCasePostale);
			appendValueAndComma(b, ident.adresse.numeroOrdrePostal);
			appendValueAndComma(b, ident.adresse.numeroPostal);
			appendValueAndComma(b, ident.adresse.numeroRue);
			appendValueAndComma(b, ident.adresse.rue);
			appendValueAndComma(b, ident.adresse.texteCasePostale);
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
