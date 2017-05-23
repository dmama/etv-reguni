package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.foncier.migration.mandataire.DonneesMandat;
import ch.vd.uniregctb.foncier.migration.mandataire.MigrationMandatImporterResults;

/**
 * Générateur du rapport PDF d'exécution du batch de migration des mandataires spéciaux
 */
public class PdfMigrationMandatairesSpeciauxRapport extends PdfRapport {

	public void write(final MigrationMandatImporterResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {
		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport de migration des mandataires spéciaux");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de début des mandats :", RegDateHelper.dateToDisplayString(results.dateDebutMandats));
				table.addLigne("Type de mandat spécial :", results.genreImpot.getLibelle());
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (status.interrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[] {.6f, .4f}, table -> {
				table.addLigne("Nombre de mandats migrés :", String.valueOf(results.getMandatsCrees().size()));
				table.addLigne("Nombre de mandats en erreur :", String.valueOf(results.getErreurs().size() + results.getLignesIgnorees().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Mandats migrés
		{
			final String filename = "mandats.csv";
			final String titre = "Liste des mandats migrés";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererMandatsMigres(results.getMandatsCrees(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String titre = "Liste des erreurs";
			final String listVide = "(aucune)";
			try (TemporaryFile contenu = genererListeErreurs(results.getErreurs(), results.getLignesIgnorees(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile genererListeErreurs(List<MigrationMandatImporterResults.Erreur> erreurs,
	                                          List<String> lignesIgnorees,
	                                          String filename, StatusManager status) {
		final String msgLigneIgnoree = "Ligne ignorée";
		final Stream<Pair<String, String>> strLignesIgnorees = lignesIgnorees.stream()
				.map(ligne -> Pair.of(ligne, msgLigneIgnoree));
		final Stream<Pair<String, String>> strErreurs = erreurs.stream()
				.map(erreur -> Pair.of(erreur.mandat.getLigneSource(), erreur.erreur));
		final List<Pair<String, String>> toDump = Stream.concat(strLignesIgnorees, strErreurs).collect(Collectors.toList());

		return CsvHelper.asCsvTemporaryFile(toDump, filename, status, new CsvHelper.FileFiller<Pair<String, String>>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("INPUT_LINE").append(COMMA);
				b.append("ERREUR");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, Pair<String, String> erreur) {
				b.append(CsvHelper.DOUBLE_QUOTE).append(erreur.getLeft()).append(CsvHelper.DOUBLE_QUOTE).append(COMMA);
				b.append(escapeChars(erreur.getRight()));
				return true;
			}
		});
	}

	private TemporaryFile genererMandatsMigres(List<DonneesMandat> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<DonneesMandat>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("INPUT_LINE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DonneesMandat elt) {
				b.append(CsvHelper.DOUBLE_QUOTE).append(elt.getLigneSource()).append(CsvHelper.DOUBLE_QUOTE);
				return true;
			}
		});
	}
}
