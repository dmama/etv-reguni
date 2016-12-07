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
import ch.vd.uniregctb.identification.contribuable.IdentifierContribuableFromListeResults;

/**
 * Rapport PDF d'exécution du batch d'échéance des LRs
 */
public class PdfIdentifierContribuableFromListeRapport extends PdfRapport {

	public void write(final IdentifierContribuableFromListeResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution de l'identification automatique à partir d'une liste");

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
				table.addLigne("Nombre de personnes analysées :", String.valueOf(results.getNbPersonnesTotal()));
				table.addLigne("Nombre de personnes identifiés :", String.valueOf(results.identifies.size()));
				table.addLigne("Nombre de personnes avec plusieurs numéros trouvés :", String.valueOf(results.plusieursTrouves.size()));
				table.addLigne("Nombre de personnes non identifiés :", String.valueOf(results.nonIdentifies.size()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
			});
		}

		// personnes identifiés
		{
			final String filename = "personnes_identifiees.csv";
			final String titre = "Liste des personnes identifiées";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = getCsvPersonnesIdentifies(results.identifies, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// personnes non-identifiés
		{
			final String filename = "personnes_non_identifiees.csv";
			final String titre = "Liste des personnes non-identifiées";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = getCsvPersonnesNonIdentifies(results.nonIdentifies, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Plusieurs numéros trouvés
		{
			final String filename = "personnes_avec_plusieurs_numeros.csv";
			final String titre = "Liste des personnes avec plusieurs numéros";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = getCsvPlusieursTrouves(results.plusieursTrouves, filename, status)) {
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

	private <T extends IdentifierContribuableFromListeResults.Identifie> TemporaryFile getCsvPersonnesIdentifies(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NOM").append(COMMA);
					b.append("PRENOMS").append(COMMA);
					//b.append("SEXE").append(COMMA);
					b.append("DATE_NAISSANCE").append(COMMA);
					//b.append("NAVS13").append(COMMA);
					//b.append("NAVS11").append(COMMA);
					b.append("NUMERO CTB").append(COMMA);
					b.append("NUMERO MENAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(escapeChars(info.criteres.getNom())).append(COMMA);
					b.append(escapeChars(info.criteres.getPrenoms())).append(COMMA);
					//b.append(info.criteres.getSexe().toString()).append(COMMA);
					b.append(RegDateHelper.dateToDisplayString(info.criteres.getDateNaissance())).append(COMMA);
					//b.append(info.criteres.getNAVS13()).append(COMMA);
					//b.append(info.criteres.getNAVS11()).append(COMMA);
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

	private <T extends IdentifierContribuableFromListeResults.NonIdentifie> TemporaryFile getCsvPersonnesNonIdentifies(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NOM").append(COMMA);
					b.append("PRENOMS").append(COMMA);
					//b.append("SEXE").append(COMMA);
					b.append("DATE_NAISSANCE").append(COMMA);
					//b.append("NAVS13").append(COMMA);
					//b.append("NAVS11").append(COMMA);
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(escapeChars(info.criteres.getNom())).append(COMMA);
					b.append(escapeChars(info.criteres.getPrenoms())).append(COMMA);
					//b.append(info.criteres.getSexe().toString()).append(COMMA);
					b.append(RegDateHelper.dateToDisplayString(info.criteres.getDateNaissance())).append(COMMA);
					//b.append(info.criteres.getNAVS13()).append(COMMA);
					//b.append(info.criteres.getNAVS11()).append(COMMA);
					return true;
				}
			});
		}
		return contenu;
	}

	private <T extends IdentifierContribuableFromListeResults.PlusieursTrouves> TemporaryFile getCsvPlusieursTrouves(List<T> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (liste != null && !liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NOM").append(COMMA);
					b.append("PRENOMS").append(COMMA);
				//	b.append("SEXE").append(COMMA);
					b.append("DATE_NAISSANCE").append(COMMA);
				//	b.append("NAVS13").append(COMMA);
				//	b.append("NAVS11").append(COMMA);
					b.append("NUMEROS_TROUVES").append(COMMA);
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(escapeChars(info.criteres.getNom())).append(COMMA);
					b.append(escapeChars(info.criteres.getPrenoms())).append(COMMA);
					//b.append(info.criteres.getSexe().toString()).append(COMMA);
					b.append(RegDateHelper.dateToDisplayString(info.criteres.getDateNaissance())).append(COMMA);
					//b.append(info.criteres.getNAVS13()).append(COMMA);
					//b.append(info.criteres.getNAVS11()).append(COMMA);
					b.append(info.trouves.toString()).append(COMMA);
					return true;
				}
			});
		}
		return contenu;
	}


	/**
	 * Traduit la liste d'infos en un fichier CSV
	 */
	protected static <T extends IdentifierContribuableFromListeResults.Erreur> TemporaryFile asCsvErrorFile(List<T> list, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("NOM").append(COMMA);
					b.append("PRENOMS").append(COMMA);
					//b.append("SEXE").append(COMMA);
					b.append("DATE_NAISSANCE").append(COMMA);
					//b.append("NAVS13").append(COMMA);
					//b.append("NAVS11").append(COMMA);
					b.append("RAISON").append(COMMA);
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T info) {
					b.append(escapeChars(info.criteres.getNom())).append(COMMA);
					b.append(escapeChars(info.criteres.getPrenoms())).append(COMMA);
					//b.append(info.criteres.getSexe().toString()).append(COMMA);
					b.append(RegDateHelper.dateToDisplayString(info.criteres.getDateNaissance())).append(COMMA);
					//b.append(info.criteres.getNAVS13()).append(COMMA);
					//b.append(info.criteres.getNAVS11()).append(COMMA);
					b.append(info.raison).append(COMMA);
					return true;
				}
			});
		}
		return contenu;
	}


}