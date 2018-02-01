package ch.vd.unireg.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.common.CsvHelper;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.declaration.ordinaire.pm.EnvoiSommationsDIsPMResults;


/**
 * Rapport PDF concernant l'execution du batch des sommations DI PM
 */
public class PdfEnvoiSommationsDIsPMRapport extends PdfRapport {


    public void write(final EnvoiSommationsDIsPMResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfWriter writer = PdfWriter.getInstance(this, os);
        open();
        addMetaInfo(nom, description);
        addEnteteUnireg();

        // Titre
        addTitrePrincipal("Sommation des déclarations d'impôt PM");

        // Paramètres
        addEntete1("Paramètres");
        addTableSimple(new float[] {70, 30}, table -> {
            table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
	        if (results.getNombreMaxSommations() != null && results.getNombreMaxSommations() > 0) {
		        table.addLigne("Nombre maximal de sommations à émettre: ", Integer.toString(results.getNombreMaxSommations()));
	        }
        });
        // Résultats
        addEntete1("Résultats");
        {
            if (results.isInterrompu()) {
                addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            addTableSimple(2, table -> {
                table.addLigne("Nombre total de DI sommées:", String.valueOf(results.getTotalDisSommees()));
                for (Integer annee : results.getListeAnnees()) {
                    table.addLigne(String.format("Période %s :", annee), String.valueOf(results.getTotalSommations(annee)));
                }
                table.addLigne("Nombre de DI non sommées pour cause de délai effectif non-échu :", String.valueOf(results.getTotalDelaisEffectifsNonEchus()));
                table.addLigne("Nombre de DI non sommées pour cause de non assujettisement :", String.valueOf(results.getTotalNonAssujettissement()));
                table.addLigne("Nombre de DI non sommées pour cause de suspension :", String.valueOf(results.getTotalDisSuspendues()));
                table.addLigne("Nombre de sommations en erreur :", String.valueOf(results.getTotalSommationsEnErreur()));
	            table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
                table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
            });
        }

        // Sommations DI en erreurs
        {
            String filename = "sommations_DI_en_erreur.csv";
            String titre = "Liste des déclarations impossibles à sommer";
	        String listVide = "(aucune déclaration à sommer en erreur)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getListeSommationsEnErreur(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        // DI avec délai effectif non échu
        {
            String filename = "delai_effectif_non_echu.csv";
            String titre = "Liste des déclarations dont le délai effectif n'est pas échu";
	        String listVide = "(aucune)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getListeDisDelaiEffectifNonEchu(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        // DI avec contribuables non assujettis.
        {
            String filename = "non_assujettissement.csv";
            String titre = "Liste des déclarations dont les contribuables ne sont pas assujettis";
	        String listVide = "(aucune déclaration n'est liée à un contribuable non assujetti)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getListeNonAssujettissement(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        // DI suspendues
        {
            String filename = "suspendues.csv";
            String titre = "Liste des déclarations non-sommées car suspendues";
	        String listVide = "(aucune déclaration sommable n'est suspendue)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getDisSuspendues(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        // DI sommées.
        {
            String filename = "sommations.csv";
	        String titre = "Liste des déclarations sommées";
	        String listVide = "(aucune déclaration sommée)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getSommations(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        close();
        status.setMessage("Génération du rapport terminée.");
    }

	@SuppressWarnings({"unchecked"})
	private TemporaryFile asCsvFileSommationDI(final List<? extends EnvoiSommationsDIsPMResults.Info> list, String filename, StatusManager status) {
		final TemporaryFile content;
		if (!list.isEmpty()) {
			content = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<EnvoiSommationsDIsPMResults.Info>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append(list.get(0).getCsvEntete());
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiSommationsDIsPMResults.Info elt) {
					final String csv = elt.getCsv();
					if (StringUtils.isNotBlank(csv)) {
						b.append(elt.getCsv());
						return true;
					}
					return false;
				}
			});
		}
		else {
			content = null;
		}
		return content;
	}
}

