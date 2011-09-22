package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesDIs;

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
			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Année fiscale:", String.valueOf(results.annee));
					table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
				}
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (results.interrompu) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre total de déclarations:", String.valueOf(results.nbDIsTotal));
					table.addLigne("Nombre de déclarations en erreur:", String.valueOf(results.disEnErrors.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// Déclarations traités
		{
			String filename = "stats_dis_" + results.annee + ".csv";
			String contenu = asCsvFile(results, filename, status);
			String titre = "Statistiques des déclarations d'impôt ordinaires";
			String listVide = "(aucune déclaration)";
			addListeDetaillee(writer, results.stats.size(), titre, listVide, filename, contenu);
		}

		// Déclarations en erreurs
		{
			String filename = "dis_en_erreur.csv";
			String contenu = asCsvFile(results.disEnErrors, filename, status);
			String titre = "Liste des déclarations en erreur";
			String listVide = "(aucune déclaration en erreur)";
			addListeDetaillee(writer, results.disEnErrors.size(), titre, listVide, filename, contenu);
		}

		close();
	}

	/**
	 * Génère un fichier CSV contenant les statistiques pour les déclarations d'impôt ordinaires
	 */
	private String asCsvFile(StatistiquesDIs results, String filename, StatusManager status) {

		String contenu = null;

		// trie par ordre croissant selon l'ordre naturel de la clé
		ArrayList<Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>> list = new ArrayList<Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>>(
				results.stats.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>>() {
			@Override
			public int compare(Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value> o1, Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});

		int size = list.size();
		if (size > 0) {
			StringBuilder b = new StringBuilder("Numéro de l'office d'impôt" + COMMA + "Type de contribuable" + COMMA
					+ "Etat de la déclaration" + COMMA + "Nombre\n");

			final GentilIterator<Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>> iter = new GentilIterator<Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>>(
					list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}
				final Map.Entry<StatistiquesDIs.Key, StatistiquesDIs.Value> entry = iter.next();
				final StatistiquesDIs.Key key = entry.getKey();
				b.append(key.oid).append(COMMA);
				b.append(description(key.typeCtb)).append(COMMA);
				b.append(key.etat.description()).append(COMMA);
				b.append(entry.getValue().nombre);
				b.append('\n');
			}
			contenu = b.toString();
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
