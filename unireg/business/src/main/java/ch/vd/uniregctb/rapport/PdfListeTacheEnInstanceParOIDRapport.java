package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tache.ListeTachesEnInstanceParOID;


/**
 * Rapport PDF contenant la liste des taches en instance par OID.
 */
public class PdfListeTacheEnInstanceParOIDRapport extends PdfRapport {

	public void write(final ListeTachesEnInstanceParOID results, final String nom, final String description, final Date dateGeneration,
	                  OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfListeTacheEnInstanceParOIDRapport document = new PdfListeTacheEnInstanceParOIDRapport();
		PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Liste des tâches en instances par OID ");

		// Paramètres
		document.addEntete1("Paramètres");
		document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
			@Override
			public void fillTable(PdfTableSimple table) throws DocumentException {
				table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.dateTraitement));
			}
		});
		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.interrompu) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				@Override
				public void fillTable(PdfTableSimple table) throws DocumentException {
					DecimalFormat df = new DecimalFormat("###,###,###.#");
					table.addLigne("Nombre moyen de tâche par contribuable: ", df.format(results.getNombreTacheMoyen()) + " tâche(s)");
					table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
					table.addLigne("Date de génération du rapport: ", formatTimestamp(dateGeneration));
				}
			});
		}
		{
			String filename = "tachesenInstance_par_OID.csv";
			String contenu = asCsvFile(results, filename, status);
			String titre = "Liste des tâches en instance par OID";
			String listVide = "(aucune tâche traitée)";
			document.addListeDetaillee(writer, results.getLignes().size(), titre, listVide, filename, contenu);
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String asCsvFile(ListeTachesEnInstanceParOID results, String filename, StatusManager status) {
		String contenu = null;
		List<ListeTachesEnInstanceParOID.LigneTacheInstance> list = results.getLignes();
		int size = list.size();
		if (size > 0) {
			StringBuilder b = new StringBuilder("Numéro de l'OID" + COMMA + "Type de tâche " + COMMA + " Nombre de tâches\n");

			final GentilIterator<ListeTachesEnInstanceParOID.LigneTacheInstance> iter = new GentilIterator<ListeTachesEnInstanceParOID.LigneTacheInstance>(list);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
				}
				final ListeTachesEnInstanceParOID.LigneTacheInstance ligne = iter.next();
				b.append(ligne.getNumeroOID()).append(COMMA);
				b.append(ligne.getTypeTache()).append(COMMA);
				b.append(ligne.getNombreTache()).append(COMMA);
				b.append('\n');
			}
			contenu = b.toString();
		}
		return contenu;

	}
}
