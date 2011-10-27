package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.ordinaire.StatistiquesCtbs;
import ch.vd.uniregctb.interfaces.model.Commune;

/**
 * Rapport PDF contenant les statistiques des contribuables assujettis.
 */
public class PdfStatsCtbsRapport extends PdfRapport {

	public void write(final StatistiquesCtbs results, String nom, String description, final Date dateGeneration, OutputStream os,
	                  StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Statistiques des contribuables assujettis pour " + results.annee);

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
					table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
					table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
				}
			});
		}

		// Contribuables traités
		{
			String filename = "stats_ctbs_" + results.annee + ".csv";
			String contenu = asCsvFile(results, filename, status);
			String titre = "Statistiques des contribuables assujettis";
			String listVide = "(aucune contribuable)";
			addListeDetaillee(writer, results.stats.size(), titre, listVide, filename, contenu);
		}

		// Contribuables en erreurs
		{
			String filename = "ctbs_en_erreur.csv";
			String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
			String titre = "Liste des contribuables en erreur";
			String listVide = "(aucune contribuable en erreur)";
			addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String asCsvFile(StatistiquesCtbs results, String filename, StatusManager status) {

		String contenu = null;

		// trie par ordre croissant selon l'ordre naturel de la clé
		Set<Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>> entrySet = results.stats.entrySet();
		ArrayList<Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>> list = new ArrayList<Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>>(
				entrySet);
		Collections.sort(list, new Comparator<Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>>() {
			@Override
			public int compare(Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> o1,
			                   Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> o2) {
				return o1.getKey().compareTo(o2.getKey());
			}
		});

		int size = list.size();
		if (size > 0) {
			StringBuilder b = new StringBuilder("Numéro de l'office d'impôt" + COMMA + "Commune" + COMMA + "numéro OFS de la Commune" + COMMA+ "Type de contribuable" + COMMA
					+ "Nombre\n");

			final GentilIterator<Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>> iter = new GentilIterator<Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>>(
					list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}
				final Map.Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> entry = iter.next();
				final StatistiquesCtbs.Key key = entry.getKey();
				b.append(key.oid).append(COMMA);
				b.append(description(key.commune)).append(COMMA);
				b.append(descriptionOFS(key.commune)).append(COMMA);
				b.append(description(key.typeCtb)).append(COMMA);
				b.append(entry.getValue().nombre);
				b.append('\n');
			}
			contenu = b.toString();
		}
		return contenu;
	}

	private Object description(Commune commune) {
		if (commune == null) {
			return "<inconnu>";
		}
		else {
			return commune.getNomMinuscule();
		}
	}

	private Object descriptionOFS(Commune commune) {
		if (commune == null) {
			return "<inconnu>";
		}
		else {
			return commune.getNoOFS();
		}
	}


	private String description(StatistiquesCtbs.TypeContribuable typeCtb) {
		if (typeCtb == null) {
			return "<inconnu>";
		}
		else {
			return typeCtb.description();
		}
	}
}
