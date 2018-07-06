package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.snc.liens.associes.DonneesLienAssocieEtSNC;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCEnMasseImporterResults;

/**
 * Générateur du rapport PDF d'exécution du batch  des liens entre associés et SNC importé du csv.
 */
public class PdfLienAssociesSNCEnMasseImporterRapport extends PdfRapport {

	public void write(final LienAssociesSNCEnMasseImporterResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {
		if (status == null) {
			throw new IllegalArgumentException();
		}

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'import des liens entre associés et SNC");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de traitements :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (status.isInterrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre de lien importé :", String.valueOf(results.getLiensCrees().size()));
				table.addLigne("Nombre de lien en erreur :", String.valueOf(results.getErreurs().size() + results.getLignesIgnorees().size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Mandats migrés
		{
			final String filename = "liensImportes.csv";
			final String titre = "Liste des liens importés";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = genererListeImportes(results.getLiensCrees(), filename, status)) {
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

	private TemporaryFile genererListeErreurs(List<LienAssociesSNCEnMasseImporterResults.Erreur> erreurs,
	                                          List<String> lignesIgnorees,
	                                          String filename, StatusManager status) {
		final String msgLigneIgnoree = "Ligne ignorée";
		final Stream<Pair<String, String>> strLignesIgnorees = lignesIgnorees.stream()
				.map(ligne -> Pair.of(ligne, msgLigneIgnoree));
		final Stream<Pair<String, String>> strErreurs = erreurs.stream()
				.map(erreur -> Pair.of(erreur.data.getLigneSource(), erreur.erreur));
		final List<Pair<String, String>> toDump = Stream.concat(strLignesIgnorees, strErreurs).collect(Collectors.toList());

		return getTemporaryFile(filename, status, toDump);
	}

	public static TemporaryFile getTemporaryFile(String filename, StatusManager status, List<Pair<String, String>> toDump) {
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

	private TemporaryFile genererListeImportes(List<DonneesLienAssocieEtSNC> liste, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<DonneesLienAssocieEtSNC>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				DonneesLienAssocieEtSNC.HEADERS.forEach(s -> b.append(s).append(CsvHelper.COMMA));
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, DonneesLienAssocieEtSNC elt) {
				b.append(elt.getLigneSource());
				return true;
			}
		});
	}
}
