package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.declaration.ordinaire.pp.EnvoiSommationsDIsPPResults;


/**
 * Rapport PDF concernant l'execution du batch des sommations DI PP
 */
public class PdfEnvoiSommationsDIsPPRapport extends PdfRapport {


    public void write(final EnvoiSommationsDIsPPResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfWriter writer = PdfWriter.getInstance(this, os);
        open();
        addMetaInfo(nom, description);
        addEnteteUnireg();

        // Titre
        addTitrePrincipal("Sommation des déclarations d'impôt PP");

        // Paramètres
        addEntete1("Paramètres");
        addTableSimple(new float[] {70, 30}, table -> {
            table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
            table.addLigne("Mise sous pli impossible: ", Boolean.toString(results.isMiseSousPliImpossible()));
	        if (results.getNombreMaxSommations() > 0) {
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
                table.addLigne("Nombre de DI non sommées pour cause de contribuable indigent :", String.valueOf(results.getTotalIndigent()));
	            table.addLigne("Nombre de DI non sommées pour cause de contribuable sourcier Pur :", String.valueOf(results.getTotalSourcierPur()));
                table.addLigne("Nombre de DI non sommées pour cause d'optionnalité :", String.valueOf(results.getTotalDisOptionnelles()));
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

        // DI avec contribuables indigents.
        {
            String filename = "indigents.csv";
            String titre = "Liste des déclarations dont les contribuables sont indigents";
	        String listVide = "(aucune déclaration n'est liée à un contribuable indigent)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getListeIndigent(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

	     // DI avec contribuables sourcier purs.
        {
            String filename = "sourciersPurs.csv";
            String titre = "Liste des déclarations dont les contribuables sont sourciers";
	        String listVide = "(aucune déclaration n'est liée à un contribuable sourcier)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getListeSourcierPur(), filename, status)) {
		        addListeDetaillee(writer, titre, listVide, filename, contenu);
	        }
        }

        // DI optionnelles
        {
            String filename = "optionnelles.csv";
            String titre = "Liste des déclarations non-sommées car optionnelles";
	        String listVide = "(aucune déclaration sommable n'est optionnelle)";
	        try (TemporaryFile contenu = asCsvFileSommationDI(results.getDisOptionnelles(), filename, status)) {
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
	private TemporaryFile asCsvFileSommationDI(final List<? extends EnvoiSommationsDIsPPResults.Info> list, String filename, StatusManager status) {
		final TemporaryFile content;
		if (!list.isEmpty()) {
			content = CsvHelper.asCsvTemporaryFile(list, filename, status, new CsvHelper.FileFiller<EnvoiSommationsDIsPPResults.Info>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append(list.get(0).getCsvEntete());
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, EnvoiSommationsDIsPPResults.Info elt) {
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

