package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.registrefoncier.processor.RapprochementTiersRFResults;

/**
 * Rapport PDF contenant les résultats du job d'appariement des établissements secondaires
 */
public class PdfRapprochementTiersRFRapport extends PdfRapport {

	public void write(final RapprochementTiersRFResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du job de rapprochement des tiers RF");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Nombre de threads :", String.valueOf(results.getNbThreads()));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[] {.6f, .4f}, table -> {
				table.addLigne("Nombre de tiers RF inspectés :", String.valueOf(results.getNbDossiersInspectes()));
				table.addLigne("Nombre de rapprochements opérés :", String.valueOf(results.getNbIdentifications()));
				table.addLigne("Nombre de d'identifications échouées :", String.valueOf(results.getNbNonIdentifications()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getNbErreurs()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Rapprochements générés
		{
			final String filename = "rapprochements.csv";
			final String titre = "Liste des rapprochements opérés";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = rapprochesAsCsvFile(results.getNouveauxRapprochements(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Identification échouées
		{
			final String filename = "non-identifications.csv";
			final String titre = "Liste des cas de non-identification";
			final String listVide = "(aucun)";
			try (TemporaryFile contenu = nonIdentfiesAsCsvFile(results.getNonIdentifications(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des cas en erreur";
			final String listeVide = "(aucun)";
			try (TemporaryFile contenu = erreursAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private TemporaryFile rapprochesAsCsvFile(List<RapprochementTiersRFResults.NouveauRapprochement> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RapprochementTiersRFResults.NouveauRapprochement>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_RF_TIERS").append(COMMA);
					b.append("NO_RF").append(COMMA);
					b.append("ID_CTB").append(COMMA);
					b.append("TYPE_RAPPROCHEMENT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RapprochementTiersRFResults.NouveauRapprochement elt) {
					b.append(elt.idTiersRF).append(COMMA);
					b.append(elt.noRF).append(COMMA);
					b.append(elt.idContribuable).append(COMMA);
					b.append(elt.type);
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile nonIdentfiesAsCsvFile(List<RapprochementTiersRFResults.NonIdentification> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RapprochementTiersRFResults.NonIdentification>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_RF_TIERS").append(COMMA);
					b.append("NO_RF").append(COMMA);
					b.append("ID_CONTRIBUABLE_RF").append(COMMA);
					b.append("NOM_RAISON_SOCIALE_RF").append(COMMA);
					b.append("PRENOM_RF").append(COMMA);
					b.append("DATE_NAISSANCE_RF").append(COMMA);
					b.append("CANDIDATS");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RapprochementTiersRFResults.NonIdentification elt) {
					b.append(elt.idTiersRF).append(COMMA);
					b.append(Optional.ofNullable(elt.noRF).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.noContribuableRF).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomRaisonSocialeRF)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.prenomRF)).append(COMMA);
					b.append(elt.dateNaissanceRF).append(COMMA);
					b.append(elt.candidats.stream().sorted().map(String::valueOf).collect(Collectors.joining(" / ")));
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile erreursAsCsvFile(List<RapprochementTiersRFResults.ErreurRapprochement> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<RapprochementTiersRFResults.ErreurRapprochement>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("ID_RF_TIERS").append(COMMA);
					b.append("NO_RF").append(COMMA);
					b.append("ID_CONTRIBUABLE_RF").append(COMMA);
					b.append("NOM_RAISON_SOCIALE_RF").append(COMMA);
					b.append("PRENOM_RF").append(COMMA);
					b.append("DATE_NAISSANCE_RF").append(COMMA);
					b.append("ID_CONTRIBUABLE_UNIREG").append(COMMA);
					b.append("MESSAGE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, RapprochementTiersRFResults.ErreurRapprochement elt) {
					b.append(elt.idTiersRF).append(COMMA);
					b.append(Optional.ofNullable(elt.noRF).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(Optional.ofNullable(elt.noContribuableRF).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.nomRaisonSocialeRF)).append(COMMA);
					b.append(CsvHelper.escapeChars(elt.prenomRF)).append(COMMA);
					b.append(elt.dateNaissanceRF).append(COMMA);
					b.append(Optional.ofNullable(elt.idContribuable).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.message));
					return true;
				}
			});
		}
		return contenu;
	}
}