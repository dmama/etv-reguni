package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.ordinaire.pp.StatistiquesDIs;

/**
 * Rapport PDF contenant les statistiques des déclarations d'impôt ordinaires
 */
public class PdfStatsDIsRapport extends PdfRapport {

	public void write(final StatistiquesDIs results, String nom, String description, final Date dateGeneration, OutputStream os,
	                  StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Statistiques des déclarations d'impôt ordinaires pour " + results.annee);

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Année fiscale:", String.valueOf(results.annee));
				table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
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
				table.addLigne("Nombre total de déclarations:", String.valueOf(results.nbDIsTotal));
				table.addLigne("Nombre de déclarations en erreur:", String.valueOf(results.disEnErrors.size()));
				table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		// Déclarations traités
		{
			String filename = "stats_dis_" + results.annee + ".csv";
			String titre = "Statistiques des déclarations d'impôt ordinaires";
			String listVide = "(aucune déclaration)";
			try (TemporaryFile contenu = asCsvFile(results, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Déclarations en erreurs
		{
			String filename = "dis_en_erreur.csv";
			String titre = "Liste des déclarations en erreur";
			String listVide = "(aucune déclaration en erreur)";
			try (TemporaryFile contenu = asCsvFile(results.disEnErrors, filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		close();
	}

	/**
	 * Génère un fichier CSV contenant les statistiques pour les déclarations d'impôt ordinaires
	 */
	private TemporaryFile asCsvFile(StatistiquesDIs results, String filename, StatusManager status) {

		TemporaryFile contenu = null;

		// trie par ordre croissant selon l'ordre naturel de la clé
		final List<Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>> list = new ArrayList<>(results.stats.entrySet());
		Collections.sort(list, Comparator.comparing(Map.Entry::getKey));

		int size = list.size();
		if (size > 0) {
			contenu = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("Numéro de l'office d'impôt").append(COMMA);
					b.append("Type de contribuable").append(COMMA);
					b.append("Etat de la déclaration").append(COMMA);
					b.append("Nombre");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value> entry) {
					final StatistiquesDIs.Key key = entry.getKey();
					b.append(key.oid).append(COMMA);
					b.append(escapeChars(description(key.typeCtb))).append(COMMA);
					b.append(escapeChars(key.etat.description())).append(COMMA);
					b.append(entry.getValue().nombre);
					return true;
				}
			});
		}
		return contenu;
	}

	private String description(ch.vd.uniregctb.type.TypeContribuable typeCtb) {
		if (typeCtb == null) {
			return "<inconnu>";
		}
		else {
			return typeCtb.description();
		}
	}
}
