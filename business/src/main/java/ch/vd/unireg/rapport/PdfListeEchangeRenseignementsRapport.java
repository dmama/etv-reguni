package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.ListesResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.listes.ear.ListeEchangeRenseignementsResults;

/**
 * Rapport PDF d'exécution du batch d'extraction de la liste EAR d'une période fiscale
 */
public class PdfListeEchangeRenseignementsRapport extends PdfRapport {

	public void write(final ListeEchangeRenseignementsResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job pour l'echange automatique de renseignements d'une période fiscale");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Période fiscale :", String.valueOf(results.getAnneeFiscale()));
				table.addLigne("Nombre de threads :", String.valueOf(results.getNombreThreads()));
				table.addLigne("Inclure les personnes physiques / ménages :", String.valueOf(results.isAvecContribuablesPP()));
				table.addLigne("Inclure les personnes morales :", String.valueOf(results.isAvecContribuablesPM()));
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
				if (results.isAvecContribuablesPP()) {
					table.addLigne("Nombre de contribuables PP identifiés :", String.valueOf(results.getNbCtbPpIdentifies()));
					table.addLigne("Nombre de contribuables PP ignorés :", String.valueOf(results.getNbCtbPpIgnores()));
				}
				if (results.isAvecContribuablesPM()) {
					table.addLigne("Nombre de contribuables PM identifiés :", String.valueOf(results.getNbCtbPmIdentifies()));
					table.addLigne("Nombre de contribuables PM ignorés :", String.valueOf(results.getNbCtbPmIgnores()));
				}


				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getListeErreurs().size()));
				table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				table.addLigne("Date de génération :", formatTimestamp(dateGeneration));
			});
		}

		if (results.isAvecContribuablesPP()) {
			// Assujettis pp trouvés
			{
				final String filename = String.format("%d_%S_AVH.csv", results.getAnneeFiscale(), ServiceInfrastructureService.SIGLE_CANTON_VD);
				final String titre = String.format("Liste des numéros AVS des contribuables assujettis sur %d", results.getAnneeFiscale());
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = buildContenuIdentifiantPP(results, status, filename)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// Contribuables ignorés PP
			{
				final String filename = String.format("%d_%S_AVH_IGNORES.csv", results.getAnneeFiscale(), ServiceInfrastructureService.SIGLE_CANTON_VD);
				final String titre = "Liste des contribuables PP ignorés";
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = buildContenuIgnores(results.getPpIgnores(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}
		}

		if (results.isAvecContribuablesPM()) {
			// Assujettis pm trouvés
			{
				final String filename = String.format("%d_%S_UID.csv", results.getAnneeFiscale(), ServiceInfrastructureService.SIGLE_CANTON_VD);
				final String titre = String.format("Liste des numéros IDE des contribuables assujettis sur %d", results.getAnneeFiscale());
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = buildContenuIdentifiantPM(results, status, filename)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}


			// Contribuables ignorés PM
			{
				final String filename = String.format("%d_%S_UID_IGNORES.csv", results.getAnneeFiscale(), ServiceInfrastructureService.SIGLE_CANTON_VD);
				final String titre = "Liste des contribuables PM ignorés";
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = buildContenuIgnores(results.getPmIgnores(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
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

	private static TemporaryFile buildContenuIdentifiantPP(final ListeEchangeRenseignementsResults results, StatusManager status, String filename) {
		final List<ListeEchangeRenseignementsResults.InfoIdentifiantCtb> liste = results.getPpIdentifies();
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListeEchangeRenseignementsResults.InfoIdentifiantCtb>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("Year").append(COMMA).append("Canton").append(COMMA)
						.append("AHV").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ListeEchangeRenseignementsResults.InfoIdentifiantCtb elt) {
				b.append(results.getAnneeFiscale()).append(COMMA);
				b.append(ServiceInfrastructureService.SIGLE_CANTON_VD).append(COMMA);
				b.append(elt.identifiant);
				return true;
			}
		});
	}

	private static TemporaryFile buildContenuIdentifiantPM(final ListeEchangeRenseignementsResults results, StatusManager status, String filename) {
		final List<ListeEchangeRenseignementsResults.InfoIdentifiantCtb> liste = results.getPmIdentifies();
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListeEchangeRenseignementsResults.InfoIdentifiantCtb>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("Year").append(COMMA).append("Canton").append(COMMA)
						.append("UID").append(COMMA);
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ListeEchangeRenseignementsResults.InfoIdentifiantCtb elt) {
				b.append(results.getAnneeFiscale()).append(COMMA);
				b.append(ServiceInfrastructureService.SIGLE_CANTON_VD).append(COMMA);
				b.append(elt.identifiant);
				return true;
			}
		});
	}


	private static TemporaryFile buildContenuIgnores(List<ListeEchangeRenseignementsResults.InfoCtbIgnore> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<ListeEchangeRenseignementsResults.InfoCtbIgnore>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("NO_CTB").append(COMMA).append("RAISON");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, ListeEchangeRenseignementsResults.InfoCtbIgnore elt) {
				b.append(elt.noCtb).append(COMMA);
				b.append(escapeChars(elt.cause.description));
				return true;
			}
		});
	}

	private static TemporaryFile buildContenuErreurs(List<ListeEchangeRenseignementsResults.Erreur> results, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(results, filename, status, new CsvHelper.FileFiller<ListeEchangeRenseignementsResults.Erreur>() {
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
