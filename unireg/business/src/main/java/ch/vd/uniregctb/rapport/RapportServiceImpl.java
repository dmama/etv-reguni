package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.document.*;
import ch.vd.uniregctb.document.ListeDIsNonEmisesRapport;
import ch.vd.uniregctb.listes.listesnominatives.TypeAdresse;
import ch.vd.uniregctb.listes.suisseoupermiscresident.ListeContribuablesResidentsSansForVaudoisResults;
import ch.vd.uniregctb.metier.FusionDeCommunesResults;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.acomptes.AcomptesResults;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.*;
import ch.vd.uniregctb.common.JobResults.Info;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.declaration.ordinaire.*;
import ch.vd.uniregctb.declaration.ordinaire.DemandeDelaiCollectiveResults.Traite;
import ch.vd.uniregctb.declaration.ordinaire.EchoirDIsResults.Echue;
import ch.vd.uniregctb.declaration.ordinaire.ListeDIsNonEmises.LigneRapport;
import ch.vd.uniregctb.declaration.source.EnvoiLRsResults;
import ch.vd.uniregctb.declaration.source.EnvoiSommationLRsResults;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.listes.listesnominatives.ListesNominativesResults;
import ch.vd.uniregctb.metier.OuvertureForsResults;
import ch.vd.uniregctb.mouvement.DeterminerMouvementsDossiersEnMasseResults;
import ch.vd.uniregctb.registrefoncier.ProprietaireRapproche;
import ch.vd.uniregctb.registrefoncier.RapprocherCtbResults;
import ch.vd.uniregctb.role.ProduireRolesResults;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoCommune;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable;
import ch.vd.uniregctb.role.ProduireRolesResults.InfoContribuable.TypeContribuable;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults;
import ch.vd.uniregctb.situationfamille.ReinitialiserBaremeDoubleGainResults.Situation;
import ch.vd.uniregctb.tache.ListeTachesEnIsntanceParOID;
import ch.vd.uniregctb.tache.ListeTachesEnIsntanceParOID.LigneTacheInstance;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantAbstractResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurMenagesResults;
import ch.vd.uniregctb.tiers.rattrapage.flaghabitant.CorrectionFlagHabitantSurPersonnesPhysiquesResults;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.validation.ValidationJobResults;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

/**
 * {@inheritDoc}
 */
public class RapportServiceImpl implements RapportService {

    private static final String TYPE_IMPOT_ICC = "ICC";
    private static final String TYPE_IMPOT_IFD = "IFD";

    private static final int AVG_LINE_LEN = 384; // longueur moyenne d'une ligne d'un fichier CVS (d'après relevé sur le batch d'ouverture
    // des fors)

    private static final Logger LOGGER = Logger.getLogger(RapportServiceImpl.class);

    private static final char COMMA = ';';
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd.MM.yyyy kk:mm:ss");

    private AdresseService adresseService;
    private DocumentService docService;
    private HibernateTemplate hibernateTemplate;
    private ServiceInfrastructureService infraService;
    private TiersService tiersService;

    public void setAdresseService(AdresseService adresseService) {
        this.adresseService = adresseService;
    }

    public void setDocService(DocumentService docService) {
        this.docService = docService;
    }

    public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
        this.hibernateTemplate = hibernateTemplate;
    }

    public void setInfraService(ServiceInfrastructureService infraService) {
        this.infraService = infraService;
    }

    public void setTiersService(TiersService tiersService) {
        this.tiersService = tiersService;
    }

    /**
     * {@inheritDoc}
     */
    public DeterminationDIsRapport generateRapport(final DeterminationDIsResults results, StatusManager s) throws DeclarationException {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final String nom = "RapportDetermDIs" + results.annee;
        final String description = "Rapport du job de détermination des DIs à émettre pour l'année " + results.annee
                + ". Date de traitement = " + results.dateTraitement;
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(DeterminationDIsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<DeterminationDIsRapport>() {
                        public void writeDoc(DeterminationDIsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new DeclarationException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    private void writePDF(final DeterminationDIsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfDIsRapport document = new PdfDIsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job de détermination des DIs à émettre pour l'année " + results.annee);

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Période fiscale considérée:", String.valueOf(results.annee));
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre de contribuables traités:", String.valueOf(results.traites.size()));
                    table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ignores.size()));
                    table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.erreurs.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // CTBs traités
        {
            String filename = "contribuables_traites.csv";
            String contenu = traitesAsCsvFile(results.traites, filename, status);
            String titre = "Liste des contribuables traités";
            String listVide = "(aucun contribuable traité)";
            document.addListeDetaillee(writer, results.traites.size(), titre, listVide, filename, contenu);
        }

        // CTBs ignorés
        {
            String filename = "contribuables_ignores.csv";
            String contenu = asCsvFile(results.ignores, filename, status);
            String titre = "Liste des contribuables ignorés";
            String listVide = "(aucun contribuable ignoré)";
            document.addListeDetaillee(writer, results.ignores.size(), titre, listVide, filename, contenu);
        }

        // CTBs en erreurs
        {
            String filename = "contribuables_en_erreur.csv";
            String contenu = asCsvFile(results.erreurs, filename, status);
            String titre = "Liste des contribuables en erreur";
            String listVide = "(aucun contribuable en erreur)";
            document.addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
        }

        // HeaderFooter header = new HeaderFooter(new Phrase("This is a header."), false);
        // document.setHeader(header);
        // document.setFooter(new HeaderFooter(new Phrase("Rapport du job de détermination des DIs à émettre - Page"), new Phrase(
        // " - généré le " + new SimpleDateFormat("dd.MM.yyy k:m:s").format(new Date()))));

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

	private String traitesAsCsvFile(List<DeterminationDIsResults.Traite> list, String filename, StatusManager status) {
		String contenu = null;
		int size = list.size();
		if (size > 0) {

		    StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
			b.append("Numéro de l'office d'impôt");
			b.append(COMMA);
			b.append("Numéro de contribuable");
			b.append(COMMA);
			b.append("Début de la période");
			b.append(COMMA);
			b.append("Fin de la période");
			b.append(COMMA);
			b.append("Raison\n");

			final GentilIterator<DeterminationDIsResults.Traite> iter = new GentilIterator<DeterminationDIsResults.Traite>(list);
		    while (iter.hasNext()) {
		        if (iter.isAtNewPercent()) {
		            status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
		        }

		        DeterminationDIsResults.Traite info = iter.next();
		        StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
		        bb.append(info.officeImpotID).append(COMMA);
		        bb.append(info.noCtb).append(COMMA);
		        bb.append(info.dateDebut).append(COMMA);
			    bb.append(info.dateFin).append(COMMA);
		        bb.append(info.raison.description());
		        if (!iter.isLast()) {
		            bb.append("\n");
		        }

		        b.append(bb);
		    }
		    contenu = b.toString();
		}
		return contenu;
	}

	/**
     * {@inheritDoc}
     */
    public EnvoiDIsRapport generateRapport(final EnvoiDIsResults results, StatusManager s) throws DeclarationException {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final String nom = "RapportEnvoiDIs" + results.annee;
        final String description = "Rapport d'exécution du job d'envoi des DIs en masse pour l'année " + results.annee
                + ". Date de traitement = " + results.dateTraitement + "Type de contribuable = " + results.type.name();
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(EnvoiDIsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<EnvoiDIsRapport>() {
                        public void writeDoc(EnvoiDIsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new DeclarationException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    private void writePDF(final EnvoiDIsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfDIsRapport document = new PdfDIsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job d'envoi des DIs en masse pour l'année " + results.annee);

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Période fiscale considérée :", String.valueOf(results.annee));
                    table.addLigne("Catégorie de contribuables :", results.type.getDescription());
                    table.addLigne("Nombre maximum d'envois :", String.valueOf(results.nbMax));
	                table.addLigne("Numéro de contribuable minimal :", results.noCtbMin == null ? "-" : FormatNumeroHelper.numeroCTBToDisplay(results.noCtbMin));
	                table.addLigne("Numéro de contribuable maximal :", results.noCtbMax == null ? "-" : FormatNumeroHelper.numeroCTBToDisplay(results.noCtbMax));
                    table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre de contribuables traités:", String.valueOf(results.ctbsTraites.size()));
                    table.addLigne("Nombre d'indigents traités:", String.valueOf(results.ctbsIndigents.size()));
                    table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
                    table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
                    table.addLigne("Nombre de contribuables rollback", String.valueOf(results.ctbsRollback.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // CTBs traités
        {
            String filename = "contribuables_traites.csv";
            String contenu = ctbIdsAsCsvFile(results.ctbsTraites, filename, status);
            String titre = "Liste des contribuables traités";
            String listVide = "(aucun contribuable traité)";
            document.addListeDetaillee(writer, results.ctbsTraites.size(), titre, listVide, filename, contenu);
        }

        // CTBs indigents
        {
            String filename = "contribuables_indigents.csv";
            String contenu = ctbIdsAsCsvFile(results.ctbsIndigents, filename, status);
            String titre = "Liste des contribuables indigents";
            String listVide = "(aucun contribuable indigent)";
            document.addListeDetaillee(writer, results.ctbsIndigents.size(), titre, listVide, filename, contenu);
        }

        // CTBs ignorés
        {
            String filename = "contribuables_ignores.csv";
            String contenu = asCsvFile(results.ctbsIgnores, filename, status);
            String titre = "Liste des contribuables ignorés";
            String listVide = "(aucun contribuable ignoré)";
            document.addListeDetaillee(writer, results.ctbsIgnores.size(), titre, listVide, filename, contenu);
        }

        // CTBs en erreurs
        {
            String filename = "contribuables_en_erreur.csv";
            String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
            String titre = "Liste des contribuables en erreur";
            String listVide = "(aucun contribuable en erreur)";
            document.addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
        }

        // CTBs rollback
        {
            String filename = "contribuables_rollback.csv";
            String contenu = asCsvFile(results.ctbsRollback, filename, status);
            String titre = "Liste des contribuables rollback";
            String listVide = "(aucun contribuable rollback)";
            document.addListeDetaillee(writer, results.ctbsRollback.size(), titre, listVide, filename, contenu);
        }

        // HeaderFooter header = new HeaderFooter(new Phrase("This is a header."), false);
        // document.setHeader(header);
        // document.setFooter(new HeaderFooter(new Phrase("Rapport du job de détermination des DIs à émettre - Page"), new Phrase(
        // " - généré le " + new SimpleDateFormat("dd.MM.yyy k:m:s").format(new Date()))));

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    public ListeDIsNonEmisesRapport generateRapport(final ListeDIsNonEmises results, final StatusManager status) {
        final String nom = "RapportListeDIsNonEmises" + results.dateTraitement.index();
        final String description = "Rapport de la liste des DIs non émises." + ". Date de traitement = " + results.dateTraitement;
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(ListeDIsNonEmisesRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<ListeDIsNonEmisesRapport>() {
                        public void writeDoc(ListeDIsNonEmisesRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final ListeDIsNonEmises results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfListeDIsNonEmisesRapport document = new PdfListeDIsNonEmisesRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Liste des DIs non émises pour l'année " + results.annee);

        // Paramètres
        document.addEntete1("Paramètres");
        document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
            public void fillTable(PdfTableSimple table) throws DocumentException {
                table.addLigne("Période fiscale considérée: ", String.valueOf(results.annee));
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
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables sans DI: ", String.valueOf(results.getNombreDeDIsNonEmises()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport: ", formatTimestamp(dateGeneration));
                }
            });
        }
        {
            String filename = "contribuables_sans_DI.csv";
            String contenu = asCsvFile(results, filename, status);
            String titre = "Liste des contribuables traités";
            String listVide = "(aucun contribuable traité)";
            document.addListeDetaillee(writer, results.getLignes().size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private void writePDF(final ListeTachesEnIsntanceParOID results, final String nom, final String description, final Date dateGeneration,
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

    private String asCsvFile(ListeDIsNonEmises results, String filename, StatusManager status) {
        String contenu = null;
        List<LigneRapport> list = results.getLignes();
        int size = list.size();
        if (size > 0) {
            StringBuilder b = new StringBuilder("Numéro de contribuale" + COMMA + "Date de début" + COMMA + " Date de fin" + COMMA
                    + "Raison" + COMMA + "Détails\n");

            final GentilIterator<LigneRapport> iter = new GentilIterator<LigneRapport>(list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }
                final LigneRapport ligne = iter.next();
                b.append(ligne.getNbCtb()).append(COMMA);
                b.append(ligne.getDateDebut()).append(COMMA);
                b.append(ligne.getDateFin()).append(COMMA);
                b.append(ligne.getRaison()).append(COMMA);
                b.append(ligne.getDetails());
                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;

    }

    private String asCsvFile(ListeTachesEnIsntanceParOID results, String filename, StatusManager status) {
        String contenu = null;
        List<LigneTacheInstance> list = results.getLignes();
        int size = list.size();
        if (size > 0) {
            StringBuilder b = new StringBuilder("Numéro de l'OID" + COMMA + "Type de tâche " + COMMA + " Nombre de tâches\n");

            final GentilIterator<LigneTacheInstance> iter = new GentilIterator<LigneTacheInstance>(list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }
                final LigneTacheInstance ligne = iter.next();
                b.append(ligne.getNumeroOID()).append(COMMA);
                b.append(ligne.getTypeTache()).append(COMMA);
                b.append(ligne.getNombreTache()).append(COMMA);
                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;

    }

    /**
     * {@inheritDoc}
     */
    public MajoriteRapport generateRapport(final OuvertureForsResults results, StatusManager s) {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final String nom = "RapportMajorite" + results.dateTraitement.index();
        final String description = "Rapport d'exécution du job d'ouverture des fors des contribuables majeurs." + ". Date de traitement = "
                + results.dateTraitement;
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(MajoriteRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<MajoriteRapport>() {
                        public void writeDoc(MajoriteRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    private void writePDF(final OuvertureForsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfMajoriteRapport document = new PdfMajoriteRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job d'ouverture des fors des habitants majeurs");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total d'habitants:", String.valueOf(results.nbHabitantsTotal));
                    table.addLigne("Nombre d'habitants traités:", String.valueOf(results.habitantTraites.size()));
                    table.addLigne("Nombre d'habitants en erreur:", String.valueOf(results.habitantEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // Habitants traités
        {
            String filename = "habitants_traites.csv";
            String contenu = asCsvFile(results.habitantTraites, filename, status);
            String titre = "Liste des habitants traités";
            String listVide = "(aucun habitant traité)";
            document.addListeDetaillee(writer, results.habitantTraites.size(), titre, listVide, filename, contenu);
        }

        // Habitants en erreurs
        {
            String filename = "habitants_en_erreur.csv";
            String contenu = asCsvFile(results.habitantEnErrors, filename, status);
            String titre = "Liste des habitants en erreur";
            String listVide = "(aucun habitant en erreur)";
            document.addListeDetaillee(writer, results.habitantEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

	/**
	 * {@inheritDoc}
	 */
	public FusionDeCommunesRapport generateRapport(final FusionDeCommunesResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "FusionDeCommunes" + results.dateTraitement.index();
		final String description = "Rapport d'exécution du job de fusion de communes." + ". Date de traitement = " + results.dateTraitement;
		final Date dateGeneration = new Date();

		try {
			return docService.newDoc(FusionDeCommunesRapport.class, nom, description, "pdf",
					new DocumentService.WriteDocCallback<FusionDeCommunesRapport>() {
						public void writeDoc(FusionDeCommunesRapport doc, OutputStream os) throws Exception {
							writePDF(results, nom, description, dateGeneration, os, status);
						}
					});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void writePDF(final FusionDeCommunesResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws DocumentException {

        Assert.notNull(status);

        // Création du document PDF
        PdfMajoriteRapport document = new PdfMajoriteRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job de fusion de communes");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
	                table.addLigne("Date de fusion:", RegDateHelper.dateToDisplayString(results.dateFusion));
	                table.addLigne("Anciennes Communes:", displayCommunes(results.anciensNoOfs));
	                table.addLigne("Commune résultante:", displayCommune(results.nouveauNoOfs));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de tiers:", String.valueOf(results.nbTiersTotal));
                    table.addLigne("Nombre de tiers traités:", String.valueOf(results.tiersTraites.size()));
	                table.addLigne("Nombre de tiers ignorés:", String.valueOf(results.tiersIgnores.size()));
                    table.addLigne("Nombre de tiers en erreur:", String.valueOf(results.tiersEnErrors.size()));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // Habitants traités
        {
            String filename = "tiers_traites.csv";
            String contenu = ctbIdsAsCsvFile(results.tiersTraites, filename, status);
            String titre = "Liste des tiers traités";
            String listVide = "(aucun tiers traité)";
            document.addListeDetaillee(writer, results.tiersTraites.size(), titre, listVide, filename, contenu);
        }

		// Habitants en erreurs
		{
		    String filename = "tiers_ignores.csv";
		    String contenu = asCsvFile(results.tiersIgnores, filename, status);
		    String titre = "Liste des tiers ignorés";
		    String listVide = "(aucun tiers ignoré)";
		    document.addListeDetaillee(writer, results.tiersIgnores.size(), titre, listVide, filename, contenu);
		}

        // Habitants en erreurs
        {
            String filename = "tiers_en_erreur.csv";
            String contenu = asCsvFile(results.tiersEnErrors, filename, status);
            String titre = "Liste des tiers en erreur";
            String listVide = "(aucun tiers en erreur)";
            document.addListeDetaillee(writer, results.tiersEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
	}

	private String displayCommune(int noOfs) {

		StringBuilder s = new StringBuilder();

		Commune commune;
		try {
			commune = infraService.getCommuneByNumeroOfsEtendu(noOfs);
		}
		catch (InfrastructureException e) {
			commune = null;
		}

		if (commune == null) {
			s.append("<unknown>");
		}
		else {
			s.append(commune.getNomMinuscule());
		}

		s.append(" (").append(noOfs).append(")");
		return s.toString();
	}

	private String displayCommunes(Set<Integer> noOfs) {

		StringBuilder s = new StringBuilder();
		for (Integer no : noOfs) {
			s.append(displayCommune(no)).append(", ");
		}
		
		final String string = s.toString();
		return string.substring(0, string.length() - 2); // supprime le dernier ", "
	}

	/**
     * {@inheritDoc}
     */
    public RolesCommunesRapport generateRapport(final ProduireRolesResults results, final StatusManager s) {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final String nom = "RolesCommunes" + results.dateTraitement.index();
        final String description = "Rapport des rôles pour les communes." + ". Date de traitement = " + results.dateTraitement;
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(RolesCommunesRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<RolesCommunesRapport>() {
                        public void writeDoc(RolesCommunesRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    private void writePDF(final ProduireRolesResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        status.setMessage("Génération du rapport...");

        // Création du document PDF
        PdfRolesCommunesRapport document = new PdfRolesCommunesRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        if (results.noOfsCommune != null) {
            final Commune commune = getCommune(results.noOfsCommune);
            document.addTitrePrincipal("Rapport des rôles pour la commune de " + commune.getNomMinuscule());
        } else if (results.noColOID != null) {
            final OfficeImpot office = getOfficeImpot(results.noColOID);
            document.addTitrePrincipal("Rapport des rôles pour l'office d'impôt de " + office.getNomCourt());
        } else {
            document.addTitrePrincipal("Rapport des rôles pour toutes les communes vaudoises");
        }

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Année fiscale:", String.valueOf(results.annee));
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résumé général");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    final int nbCommunesTraitees;
                    if (results.noOfsCommune != null) {
                        nbCommunesTraitees = 1; // par définition
                    } else if (results.noColOID != null) {
                        // calcule le compte exact des communes gérées par l'OID (le résultats du job
                        // peut en contenir plus en fonction des déménagements, ...)
                        final List<Commune> list = getListeCommunesByOID(results.noColOID);
                        nbCommunesTraitees = Math.min(list.size(), results.infosCommunes.size());
                    } else {
                        nbCommunesTraitees = results.infosCommunes.size();
                    }
                    table.addLigne("Nombre de communes traitées:", String.valueOf(nbCommunesTraitees));
                    table.addLigne("Nombre de contribuables traités:", String.valueOf(results.ctbsTraites));
                    table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
                    table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // Détails des contribuables en erreur ou ignorés
        if (!results.ctbsEnErrors.isEmpty()) {
            String filename = "contribuables_en_erreur.csv";
            String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
            String titre = "Liste des contribuables en erreur";
            String listVide = "(aucun contribuable en erreur)";
            document.addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
        }

        if (!results.ctbsIgnores.isEmpty()) {
            String filename = "contribuables_ignores.csv";
            String contenu = asCsvFile(results.ctbsIgnores, filename, status);
            String titre = "Liste des contribuables ignorés";
            String listVide = "(aucun contribuable ignoré)";
            document.addListeDetaillee(writer, results.ctbsIgnores.size(), titre, listVide, filename, contenu);
        }

        final List<Commune> communes = getListCommunes(results);

        // Détail commune par commune
        for (final Commune commune : communes) {

            if (results.noOfsCommune != null) {
                if (commune.getNoOFSEtendu() != results.noOfsCommune) {
                    /*
					 * On ignore toutes les autres communes lorsqu'un rapport a été demandé spécifiquement pour une commune (il est possible
				     * et normal d'avoir des informations pour d'autres communes en raison des contribuables qui ont déménagé durant
					 * l'année, et donc qui produisent des informations sur deux communes).
					 */
                    continue;
                }
            } else if (results.noColOID != null) {
                final OfficeImpot office = infraService.getOfficeImpotDeCommune(commune.getNoOFSEtendu());
                if (office == null || office.getNoColAdm() != results.noColOID) {
                    /*
                     * On ignore toutes les communes non gérées par l'office d'impôt lorsqu'un rapport a été demandé spécifiquement pour ce
                     * dernier.
                     */
                    continue;
                }
            }

            final InfoCommune infoCommune = results.infosCommunes.get(commune.getNoOFSEtendu());
            if (infoCommune == null) {
                Audit.error("Rôle des communes: Impossible de trouver les informations pour la commune " + commune.getNomMinuscule()
                        + "(n°ofs " + commune.getNoOFSEtendu() + ")");
                continue;
            }

	        document.newPage();

            // Entête de la commune
            final String nomCommune = commune.getNomMinuscule();
            final int totalContribuables = infoCommune.getInfosContribuables().size();
            document.addTitrePrincipal("Liste des rôles " + results.annee + " pour la commune de\n" + nomCommune);

            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            // Résumé de la commune
            document.addEntete1("Résumé");
            {
                final Map<TypeContribuable, Integer> nombreParType = extractNombreParType(infoCommune);
                document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                    public void fillTable(PdfTableSimple table) throws DocumentException {
                        table.setWidths(new float[]{
                                2.0f, 1.0f
                        });
                        table.addLigne("Nombre total de contribuables traités:", String.valueOf(totalContribuables));
                        table.addLigne("Contribuables ordinaires:", nombreAsString(nombreParType.get(TypeContribuable.ORDINAIRE), nombreParType.get(TypeContribuable.MIXTE)));
                        table.addLigne("Contribuables hors canton:", nombreAsString(nombreParType.get(TypeContribuable.HORS_CANTON)));
                        table.addLigne("Contribuables hors Suisse:", nombreAsString(nombreParType.get(TypeContribuable.HORS_SUISSE)));
                        table.addLigne("Contribuables à la source:", nombreAsString(nombreParType.get(TypeContribuable.SOURCE)));
                        table.addLigne("Contribuables à la dépense:", nombreAsString(nombreParType.get(TypeContribuable.DEPENSE)));
                        table.addLigne("Contribuables plus assujettis:", nombreAsString(nombreParType.get(TypeContribuable.NON_ASSUJETTI)));
	                    table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                        table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                    }

                });
            }

            // Fichier CVS détaillé
            {
                String filename = "" + results.annee + "_roles_" + human2file(nomCommune) + ".csv";
                String contenu = asCsvFile(nomCommune, infoCommune, results.annee, status);
                String titre = "Liste détaillée";
                String listVide = "(aucun rôle trouvé)";
                document.addListeDetaillee(writer, totalContribuables, titre, listVide, filename, contenu);
            }
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    /**
     * {@inheritDoc}
     */
    public StatistiquesDIsRapport generateRapport(final StatistiquesDIs results, final StatusManager status) {
        final String nom = "RapportStatsDIs" + results.dateTraitement.index();
        final String description = "Rapport des statistiques des déclarations d'impôt ordinaires." + ". Date de traitement = "
                + results.dateTraitement;
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(StatistiquesDIsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<StatistiquesDIsRapport>() {
                        public void writeDoc(StatistiquesDIsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    protected void writePDF(final StatistiquesDIs results, String nom, String description, final Date dateGeneration, OutputStream os,
                            StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfStatsDIsRapport document = new PdfStatsDIsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Statistiques des déclaration d'impôt ordinaires pour " + results.annee);

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Année fiscale:", String.valueOf(results.annee));
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
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
            document.addListeDetaillee(writer, results.stats.size(), titre, listVide, filename, contenu);
        }

        // Déclarations en erreurs
        {
            String filename = "dis_en_erreur.csv";
            String contenu = asCsvFile(results.disEnErrors, filename, status);
            String titre = "Liste des déclarations en erreur";
            String listVide = "(aucune déclaration en erreur)";
            document.addListeDetaillee(writer, results.disEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();
    }

    /**
     * Génère un fichier CSV contenant les statistiques pour les déclarations d'impôt ordinaires
     */
    private String asCsvFile(StatistiquesDIs results, String filename, StatusManager status) {

        String contenu = null;

        // trie par ordre croissant selon l'ordre naturel de la clé
        ArrayList<Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>> list = new ArrayList<Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>>(
                results.stats.entrySet());
        Collections.sort(list, new Comparator<Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>>() {
            public int compare(Entry<StatistiquesDIs.Key, StatistiquesDIs.Value> o1, Entry<StatistiquesDIs.Key, StatistiquesDIs.Value> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        int size = list.size();
        if (size > 0) {
            StringBuilder b = new StringBuilder("Numéro de l'office d'impôt" + COMMA + "Type de contribuable" + COMMA
                    + "Etat de la déclaration" + COMMA + "Nombre\n");

            final GentilIterator<Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>> iter = new GentilIterator<Entry<StatistiquesDIs.Key, StatistiquesDIs.Value>>(
                    list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }
                final Entry<StatistiquesDIs.Key, StatistiquesDIs.Value> entry = iter.next();
                final StatistiquesDIs.Key key = entry.getKey();
                b.append(key.oid).append(COMMA);
                b.append(description(key.typeCtb)).append(COMMA);
                b.append(key.etat.description()).append(COMMA);
                b.append(entry.getValue().nombre);
                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

    /**
     * {@inheritDoc}
     */
    public StatistiquesCtbsRapport generateRapport(final StatistiquesCtbs results, final StatusManager status) {
        final String nom = "RapportStatsCtbs" + results.dateTraitement.index();
        final String description = "Rapport des statistiques des contribuables assujettis." + ". Date de traitement = "
                + results.dateTraitement;
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(StatistiquesCtbsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<StatistiquesCtbsRapport>() {
                        public void writeDoc(StatistiquesCtbsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final StatistiquesCtbs results, String nom, String description, final Date dateGeneration, OutputStream os,
                          StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfStatsCtbsRapport document = new PdfStatsCtbsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Statistiques des contribuables assujettis pour " + results.annee);

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Année fiscale:", String.valueOf(results.annee));
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
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
            document.addListeDetaillee(writer, results.stats.size(), titre, listVide, filename, contenu);
        }

        // Contribuables en erreurs
        {
            String filename = "ctbs_en_erreur.csv";
            String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
            String titre = "Liste des contribuables en erreur";
            String listVide = "(aucune contribuable en erreur)";
            document.addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private String asCsvFile(StatistiquesCtbs results, String filename, StatusManager status) {

        String contenu = null;

        // trie par ordre croissant selon l'ordre naturel de la clé
        Set<Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>> entrySet = results.stats.entrySet();
        ArrayList<Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>> list = new ArrayList<Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>>(
                entrySet);
        Collections.sort(list, new Comparator<Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>>() {
            public int compare(Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> o1,
                               Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        int size = list.size();
        if (size > 0) {
            StringBuilder b = new StringBuilder("Numéro de l'office d'impôt" + COMMA + "Commune" + COMMA + "Type de contribuable" + COMMA
                    + "Nombre\n");

            final GentilIterator<Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>> iter = new GentilIterator<Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value>>(
                    list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }
                final Entry<StatistiquesCtbs.Key, StatistiquesCtbs.Value> entry = iter.next();
                final StatistiquesCtbs.Key key = entry.getKey();
                b.append(key.oid).append(COMMA);
                b.append(description(key.commune)).append(COMMA);
                b.append(description(key.typeCtb)).append(COMMA);
                b.append(entry.getValue().nombre);
                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

    private Object description(Commune commune) {
        if (commune == null) {
            return "<inconnu>";
        } else {
            return commune.getNomMinuscule();
        }
    }

    private String description(StatistiquesCtbs.TypeContribuable typeCtb) {
        if (typeCtb == null) {
            return "<inconnu>";
        } else {
            return typeCtb.description();
        }
    }

    private String description(ch.vd.uniregctb.type.TypeContribuable typeCtb) {
        if (typeCtb == null) {
            return "<inconnu>";
        } else {
            return typeCtb.description();
        }
    }

    private OfficeImpot getOfficeImpot(Integer noColOID) {
        try {
            return infraService.getOfficeImpot(noColOID);
        }
        catch (InfrastructureException e) {
            return null;
        }
    }

    private Commune getCommune(int noOfsCommune) {
        try {
            return infraService.getCommuneByNumeroOfsEtendu(noOfsCommune);
        }
        catch (InfrastructureException e) {
            return null;
        }
    }

    private List<Commune> getListeCommunesByOID(int oid) {
        try {
            return infraService.getListeCommunesByOID(oid);
        }
        catch (InfrastructureException e) {
            return null;
        }
    }

    /**
     * @return la liste des communes triées par ordre alphabétique
     */
    private List<Commune> getListCommunes(final ProduireRolesResults results) {

        final List<Commune> listCommunes = new ArrayList<Commune>(results.infosCommunes.size());
        for (InfoCommune infoCommune : results.infosCommunes.values()) {
            final int noOfs = infoCommune.getNoOfs();
            final Commune commune = getCommune(noOfs);

            if (commune == null) {
                Audit.error("Rôles des communes: impossible de déterminer la commune avec le numéro Ofs = " + noOfs);
                continue;
            }
            Assert.isEqual(noOfs, commune.getNoOFSEtendu());
            listCommunes.add(commune);
        }

        Collections.sort(listCommunes, new Comparator<Commune>() {
            public int compare(Commune o1, Commune o2) {
                return o1.getNomMinuscule().compareTo(o2.getNomMinuscule());
            }
        });

        return listCommunes;
    }

    private String asCsvFile(final String nomCommune, InfoCommune infoCommune, int annee, StatusManager status) {

        final RegDate finAnnee = RegDate.get(annee, 12, 31);
        final List<InfoContribuable> infos = new ArrayList<InfoContribuable>(infoCommune.getInfosContribuables().values());

        final int size = infos.size();
        if (size == 0) {
            return null;
        }

        Collections.sort(infos, new Comparator<InfoContribuable>() {
            public int compare(InfoContribuable o1, InfoContribuable o2) {
                return (int) (o1.noCtb - o2.noCtb);
            }
        });

        final int noOfsCommune = infoCommune.getNoOfs();

        final StringBuilder b = new StringBuilder("Numéro OFS de la commune" + COMMA + // --------------------------
                "Nom de la commune" + COMMA + // -------------------------------------------------------------
                "Numéro de contribuable" + COMMA + // --------------------------------------------------------
                "Nom du contribuable" + COMMA + // -----------------------------------------------------------
                "Nom du contribuable secondaire" + COMMA + // ------------------------------------------------
                "Adresse courrier" + COMMA + // --------------------------------------------------------------
                "Type de contribuable" + COMMA + // ----------------------------------------------------------
                "Complément type contribuable" + COMMA + // --------------------------------------------------
                "Date d'ouverture" + COMMA + // --------------------------------------------------------
		        "Motif d'ouverture" + COMMA + // ------------------------------------------------------------
		        "Date de fermeture" + COMMA + // ----------------------------------------------------------
                "Motif de fermeture" + COMMA + // --------------------------------------------------------------
                "Assujetti" + COMMA + // ----------------------------------------------------
                "Numéro AVS contribuable" + COMMA + // -------------------------------------------------------
                "Numéro AVS contribuable secondaire\n");

		status.setMessage("Génération du rapport pour la commune de " + nomCommune + "...");
        b.append(traiteCommune(infos, noOfsCommune, nomCommune, finAnnee));
        return b.toString();
    }

    private String traiteCommune(final List<InfoContribuable> infos, final int noOfsCommune, String nomCommune, final RegDate finAnnee) {

        final StringBuilder b = new StringBuilder();

	    for (InfoContribuable info : infos) {

            final long noCtb = info.noCtb;
		    final List<String> noms = info.getNomsPrenoms();
		    final List<String> nosAvs = info.getNosAvs();
		    final String[] adresse = info.getAdresseEnvoi();

            final int sizeNoms = noms.size();
            Assert.isEqual(sizeNoms, nosAvs.size());

            // ajout des infos au fichier
            final String nom1 = sizeNoms > 0 ? noms.get(0) : "";                // au cas où on n'arrive pas à trouver les noms...
            final String nom2 = sizeNoms > 1 ? noms.get(1) : "";
            final String adresseCourrier = asCsvField(adresse);
            final String typeCtb = asCvsField(info.getTypeCtb());
            final String complTypeCtb = (TypeContribuable.MIXTE.equals(info.getTypeCtb()) ? "(sourcier mixte)" : "");

		    final Pair<RegDate, MotifFor> infosOuverture = info.getInfosOuverture();
		    final String debut;
		    final String motifOuverture;
		    if (infosOuverture != null) {
			    debut = infosOuverture.getFirst().toString();
			    motifOuverture = infosOuverture.getSecond() != null ? infosOuverture.getSecond().getDescription() : "";
		    }
		    else {
			    debut = "";
			    motifOuverture = "";
		    }

		    final Pair<RegDate, MotifFor> infosFermeture = info.getInfosFermeture();
		    final String fin;
		    final String motifFermeture;
		    if (infosFermeture != null) {
			    fin = infosFermeture.getFirst().toString();
			    motifFermeture = infosFermeture.getSecond() != null ? infosFermeture.getSecond().getDescription() : "";
		    }
		    else {
			    fin = "";
			    motifFermeture = "";
		    }

		    final String assujetti = info.isAssujettiDansCommmune() ? "Oui" : "Non";
            final String numeroAvs1 = sizeNoms > 0 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(0)) : "";
            final String numeroAvs2 = sizeNoms > 1 ? FormatNumeroHelper.formatNumAVS(nosAvs.get(1)) : "";

            b.append(noOfsCommune).append(COMMA);
            b.append(nomCommune).append(COMMA);
            b.append(noCtb).append(COMMA);
            b.append(nom1).append(COMMA);
            b.append(nom2).append(COMMA);
            b.append(adresseCourrier).append(COMMA);
            b.append(typeCtb).append(COMMA);
            b.append(complTypeCtb).append(COMMA);
            b.append(debut).append(COMMA);
		    b.append(motifOuverture).append(COMMA);
		    b.append(fin).append(COMMA);
            b.append(motifFermeture).append(COMMA);
            b.append(assujetti).append(COMMA);
            b.append(numeroAvs1).append(COMMA);
            b.append(numeroAvs2);

            b.append("\n");
        }

        return b.toString();
    }

    private String asCvsField(TypeContribuable typeCtb) {
        if (TypeContribuable.MIXTE.equals(typeCtb)) {
            // selon la spécification
            return TypeContribuable.ORDINAIRE.description();
        } else {
            return typeCtb.description();
        }
    }

    /**
     * Transforme les lignes spécifiées en une chaîne de caractère capable de tenir dans un champ d'un fichier CSV. Les retours de lignes
     * sont préservés, mais les éventuels caractères interdits (" et ;) sont supprimés.
     */
    private static String asCsvField(String[] lignes) {
        final StringBuilder b = new StringBuilder();
        b.append("\"");
        final int length = lignes.length;

		// compte les lignes non-vides
	    int nbLignesNonVides = 0;
	    for (int i = 0 ; i < length ; ++ i) {
		    if (!StringUtils.isBlank(lignes[i])) {
			    ++ nbLignesNonVides;
		    }
	    }

	    // construit la chaîne de caractères
        for (int i = 0; i < length; ++i) {
            final String ligne = lignes[i];
            if (!StringUtils.isBlank(ligne)) {
				b.append(ligne.replaceAll("[;\"]", "")); // on supprime les éventuels " et ;
	            -- nbLignesNonVides;
	            if (nbLignesNonVides > 0) {
					b.append("\n");
				}
            }
        }
        b.append("\"");
        return b.toString();
    }

    /**
     * Transforme la ligne spécifiée (qui peut contenir des retours de lignes embeddés) en une chaîne de caractère capable de tenir dans un
     * champ d'un fichier CSV. Les retours de lignes sont préservés, mais les éventuels caractères interdits (" et ;) sont supprimés.
     */
    private static String asCsvField(String lignes) {
        return asCsvField(lignes.split("\n"));
    }

    private String human2file(String nom) {
        // enlève les accents
        nom = nom.replaceAll("[àäâ]", "a");
        nom = nom.replaceAll("[éèëê]", "e");
        nom = nom.replaceAll("[îï]", "i");
        nom = nom.replaceAll("[öô]", "o");
        nom = nom.replaceAll("[üû]", "u");

        // remplace tous les caractères non-standard restant par un '_'
        nom = nom.replaceAll("[^-+0-9a-zA-Z._]", "_");
        return nom;
    }

    private Map<TypeContribuable, Integer> extractNombreParType(final InfoCommune infoCommune) {
        Map<TypeContribuable, Integer> nombreParType = new HashMap<TypeContribuable, Integer>();
        for (InfoContribuable info : infoCommune.getInfosContribuables().values()) {
            Integer nombre = nombreParType.get(info.getTypeCtb());
            if (nombre == null) {
                nombre = Integer.valueOf(1);
                nombreParType.put(info.getTypeCtb(), nombre);
            } else {
                nombre = Integer.valueOf(nombre + 1);
                nombreParType.put(info.getTypeCtb(), nombre);
            }
        }
        return nombreParType;
    }

    private static String nombreAsString(Integer nombre) {
        return nombre == null ? "0" : String.valueOf(nombre);
    }

    private static String nombreAsString(Integer integer1, Integer integer2) {
        int n = (integer1 == null ? 0 : integer1.intValue()) + (integer2 == null ? 0 : integer2.intValue());
        return String.valueOf(n);
    }

    /**
     * Construit le contenu du fichier détaillé des contribuables traités
     */
    private String ctbIdsAsCsvFile(List<Long> ctbsTraites, String filename, StatusManager status) {
        String contenu = null;
        int size = ctbsTraites.size();
        if (size > 0) {
            StringBuilder b = new StringBuilder("Numéro de contribuable\n");

            final GentilIterator<Long> iter = new GentilIterator<Long>(ctbsTraites);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }
                b.append(iter.next());
                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

    /**
     * Traduit la liste d'infos en un fichier CSV
     */
    private <T extends Info> String asCsvFile(List<T> list, String filename, StatusManager status) {
        String contenu = null;
        int size = list.size();
        if (size > 0) {

            StringBuilder b = new StringBuilder(AVG_LINE_LEN * list.size());
            b.append("Numéro de l'office d'impôt").append(COMMA).append("Numéro de contribuable").append(COMMA + "Nom du contribuable")
                    .append(COMMA).append("Raison").append(COMMA).append("Commentaire\n");

            final GentilIterator<T> iter = new GentilIterator<T>(list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }

                T info = iter.next();
                StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
                bb.append(info.officeImpotID).append(COMMA);
                bb.append(info.noCtb).append(COMMA);
                bb.append(info.nomCtb).append(COMMA);
                bb.append(info.getDescriptionRaison());
                if (info.details != null) {
                    bb.append(COMMA).append(asCsvField(info.details));
                }
                if (!iter.isLast()) {
                    bb.append("\n");
                }

                b.append(bb);
            }
            contenu = b.toString();
        }
        return contenu;
    }

    private String formatTimestamp(final Date dateGeneration) {
        return TIMESTAMP_FORMAT.format(dateGeneration);
    }

	private static String formatDureeExecution(JobResults results) {
		return PdfRapport.formatDureeExecution(results);
	}

    public EnvoiSommationsDIsRapport generateRapport(final EnvoiSommationsDIsResults results, final StatusManager statusManager) {
        final String nom = "RapportSommationDI" + results.getDateTraitement().index();
        final String description = "Rapport de l'envoi de sommation des DIs." + " Date de traitement = " + results.getDateTraitement();
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(EnvoiSommationsDIsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<EnvoiSommationsDIsRapport>() {
                        public void writeDoc(EnvoiSommationsDIsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, statusManager);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final EnvoiSommationsDIsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfEnvoiSommationsDIsRapport document = new PdfEnvoiSommationsDIsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Sommation des DIs au " + results.getDateTraitement());

        // Paramètres
        document.addEntete1("Paramètres");
        document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
            public void fillTable(PdfTableSimple table) throws DocumentException {
                table.addLigne("Date de traitement: ", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
                table.addLigne("Mise sous pli impossible: ", Boolean.toString(results.isMiseSousPliImpossible()));
            }
        });
        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.isInterrompu()) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de DI sommées:", String.valueOf(results.getTotalDisSommees()));
                    for (Integer annee : results.getListeAnnees()) {
                        table.addLigne(String.format("Période %s:", annee), String.valueOf(results.getTotalSommations(annee)));
                    }
                    table.addLigne("Nombre de DI non sommées pour cause de non assujettisement:", String.valueOf(results
                            .getTotalNonAssujettissement()));
                    table.addLigne("Nombre de sommations en erreur:", String.valueOf(results.getTotalSommationsEnErreur()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // Sommations DI en erreurs
        {
            String filename = "sommations_DI_en_erreur.csv";
            String contenu = asCsvFileSommationDI(results.getListeSommationsEnErreur(), filename, status);
            String titre = "Liste des déclarations impossibles à sommer";
            String listVide = "(aucune déclaration à sommer en erreur)";
            document.addListeDetaillee(writer, results.getTotalSommationsEnErreur(), titre, listVide, filename, contenu);
        }

        // DI avec contribuables non assujettis.
        {
            String filename = "non_assujettissement.csv";
            String contenu = asCsvFileSommationDI(results.getListeNonAssujettissement(), filename, status);
            String titre = "Liste des déclarations dont les contribuables ne sont pas assujettis";
            String listVide = "(aucune déclaration n'est liée à un contribuable non assujetti)";
            document.addListeDetaillee(writer, results.getTotalNonAssujettissement(), titre, listVide, filename, contenu);
        }

        // DI avec contribuables indigents.
        {
            String filename = "indigent.csv";
            String contenu = asCsvFileSommationDI(results.getListeIndigent(), filename, status);
            String titre = "Liste des déclarations dont les contribuables sont indigents";
            String listVide = "(aucune déclaration n'est liée à un contribuable indigent)";
            document.addListeDetaillee(writer, results.getTotalIndigent(), titre, listVide, filename, contenu);
        }

        // DI sommées.
        {
            String filename = "sommations.csv";
            String contenu = asCsvFileSommationDI(results.getSommations(), filename, status);
            String titre = "Liste des déclarations sommées";
            String listVide = "(aucune déclaration sommée)";
            document.addListeDetaillee(writer, results.getTotalNonAssujettissement(), titre, listVide, filename, contenu);
        }

        document.close();
        status.setMessage("Génération du rapport terminée.");
    }

    private String asCsvFileSommationDI(List<? extends EnvoiSommationsDIsResults.Info> list, String filename, StatusManager status) {
        String contenu = null;

        if (list.size() > 0) {
            StringBuilder b = new StringBuilder(list.get(0).getCVSEntete());
            b.append("\n");

            Iterator<? extends EnvoiSommationsDIsResults.Info> iter = list.iterator();
            while (iter.hasNext()) {
                final EnvoiSommationsDIsResults.Info ligne = iter.next();
                b.append(ligne.getCVS());
                if (iter.hasNext()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;

    }

    public ValidationJobRapport generateRapport(final ValidationJobResults results, final StatusManager statusManager) {
        final String nom = "RapportValidationTiers" + results.dateTraitement.index();
        final String description = "Rapport de la validation de tous les tiers." + " Date de traitement = " + results.dateTraitement;
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(ValidationJobRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<ValidationJobRapport>() {
                        public void writeDoc(ValidationJobRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, statusManager);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final ValidationJobResults results, String nom, String description, Date dateGeneration, OutputStream os,
                          StatusManager statusManager) throws Exception {

        Assert.notNull(statusManager);

        // Création du document PDF
        PdfRapport document = new PdfRapport() {
        };
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport de la validation de tous les contribuables");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                    table.addLigne("Calcul des assujettissements:", Boolean.toString(results.calculateAssujettissements));
                    table.addLigne("Cohérence des dates des DIs", Boolean.toString(results.coherenceAssujetDi));
                    table.addLigne("Calcul des adresses:", Boolean.toString(results.calculateAdresses));
                    table.addLigne("Cohérence des autorités des fors fiscaux:", Boolean.toString(results.coherenceAutoritesForsFiscaux));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre de contribuables ne validant pas:", String.valueOf(results.erreursValidation.size()));
                    table.addLigne("Nombre de périodes d'assujettissement qui ne sont pas calculables:", String
                            .valueOf(results.erreursAssujettissement.size()));
                    table.addLigne("Nombre de DIs émises dont les dates ne correspondent pas aux dates d'assujettissement:", String
                            .valueOf(results.erreursCoherenceDI.size()));
                    table.addLigne("Nombre de contribuables dont les adresses ne sont pas calculables:", String
                            .valueOf(results.erreursAdresses.size()));
                    table.addLigne("Nombre de fors fiscaux dont les types d'autorités fiscales ne sont pas cohérents:", String
                            .valueOf(results.erreursAutoritesForsFiscaux.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(new Date()));
                }
            });
        }

        // CTBs en erreurs
        {
            String filename = "contribuables_invalides.csv";
            String contenu = asCsvFile(results.erreursValidation, filename, statusManager);
            String titre = "Liste des contribuables invalides";
            String listVide = "(aucun contribuable invalide)";
            document.addListeDetaillee(writer, results.erreursValidation.size(), titre, listVide, filename, contenu);
        }

        // Assujettissements
        if (results.calculateAssujettissements) {
            String filename = "periodes_assujettissements_incalculables.csv";
            String contenu = asCsvFile(results.erreursAssujettissement, filename, statusManager);
            String titre = "Liste des périodes d'assujettissement qui ne sont pas calculables";
            String listVide = "(aucune période d'assujettissement incalculable)";
            document.addListeDetaillee(writer, results.erreursAssujettissement.size(), titre, listVide, filename, contenu);
        }

        // Cohérence DI
        if (results.calculateAssujettissements && results.coherenceAssujetDi) {
            String filename = "periodes_dis_incoherentes.csv";
            String contenu = asCsvFile(results.erreursCoherenceDI, filename, statusManager);
            String titre = "Liste des DIs émises dont les dates ne correspondent pas aux dates d'assujettissement";
            String listVide = "(aucune DI émise dont les dates ne correspondent pas aux dates d'assujettissement)";
            document.addListeDetaillee(writer, results.erreursCoherenceDI.size(), titre, listVide, filename, contenu);
        }

        // Adresses
        if (results.calculateAdresses) {
            String filename = "contribuables_adresses_incalculables.csv";
            String contenu = asCsvFile(results.erreursAdresses, filename, statusManager);
            String titre = "Liste des contribuables dont les adresses ne sont pas calculables";
            String listVide = "(aucun contribuable dont les adresses ne sont pas calculables)";
            document.addListeDetaillee(writer, results.erreursAdresses.size(), titre, listVide, filename, contenu);
        }

        // Autorités fiscales
        if (results.coherenceAutoritesForsFiscaux) {
            String filename = "autorites_fors_fiscaux_incoherentes.csv";
            String contenu = asCsvFile(results.erreursAutoritesForsFiscaux, filename, statusManager);
            String titre = "Liste des fors fiscaux dont les autorités fiscales ne sont pas cohérentes";
            String listVide = "(aucun for fiscal dont l'autorité fiscale n'est pas cohérente)";
            document.addListeDetaillee(writer, results.erreursAutoritesForsFiscaux.size(), titre, listVide, filename, contenu);
        }

        document.close();

        statusManager.setMessage("Génération du rapport terminée.");
    }

    public EnvoiLRsRapport generateRapport(final EnvoiLRsResults results, final StatusManager statusManager) {
        final String nom = "RapportEnvoiLR" + results.dateTraitement.index();
        final String description = "Rapport de l'envoi de LR pour le mois de " + results.dateFinPeriode + "." + " Date de traitement = "
                + results.dateTraitement;
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(EnvoiLRsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<EnvoiLRsRapport>() {
                        public void writeDoc(EnvoiLRsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, statusManager);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final EnvoiLRsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfRapport document = new PdfRapport() {
        };
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport de l'envoi des listes récapitulatives");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                    table.addLigne("Date de fin de période:", results.getMoisFinPeriode());
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de débiteurs :", String.valueOf(results.nbDPIsTotal));
                    table.addLigne("Nombre LR générées :", String.valueOf(results.LRTraitees.size()));
                    table.addLigne("dont mensuelles :", String.valueOf(results.nbLrMensuellesTraitees));
                    table.addLigne("     trimestrielles :", String.valueOf(results.nbLrTrimestriellesTraitees));
                    table.addLigne("     semestrielles :", String.valueOf(results.nbLrSemestriellesTraitees));
                    table.addLigne("     annuelles :", String.valueOf(results.nbLrAnnuellesTraitees));
                    table.addLigne("Nombre de LR en erreur :", String.valueOf(results.LREnErreur.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
                }
            });
        }

        // Débiteurs traités
        {
            String filename = "lr_generees.csv";
            String contenu = asCsvFile(results.LRTraitees, filename, status);
            String titre = "Liste des listes récapitulatives générées";
            String listVide = "(aucune liste récapitulative générée)";
            document.addListeDetaillee(writer, results.LRTraitees.size(), titre, listVide, filename, contenu);
        }

        // Débiteurs en erreurs
        {
            String filename = "lr_en_erreur.csv";
            String contenu = asCsvFile(results.LREnErreur, filename, status);
            String titre = "Liste des erreurs";
            String listVide = "(aucune erreur)";
            document.addListeDetaillee(writer, results.LREnErreur.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    public EnvoiSommationLRsRapport generateRapport(final EnvoiSommationLRsResults results, final StatusManager statusManager) {
        final String nom = "RapportSommationLR" + results.dateTraitement.index();
        final String description = "Rapport de l'envoi de sommation de LR." + " Date de traitement = " + results.dateTraitement;
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(EnvoiSommationLRsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<EnvoiSommationLRsRapport>() {
                        public void writeDoc(EnvoiSommationLRsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, statusManager);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final EnvoiSommationLRsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfRapport document = new PdfRapport() {
        };
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport de l'envoi des sommations des listes récapitulatives");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de listes récapitulatives:", String.valueOf(results.nbLRsTotal));
                    table.addLigne("Nombre de listes récapitulatives sommées:", String.valueOf(results.LRSommees.size()));
                    table.addLigne("Nombre de listes récapitulatives en erreur:", String.valueOf(results.SommationLREnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // LR sommées
        {
            String filename = "listes_recapitulatives_sommees.csv";
            String contenu = asCsvFile(results.LRSommees, filename, status);
            String titre = "Liste des débiteurs traités";
            String listVide = "(aucun débiteur traité)";
            document.addListeDetaillee(writer, results.LRSommees.size(), titre, listVide, filename, contenu);
        }

        // Sommations LR en erreurs
        {
            String filename = "sommation_en_erreur.csv";
            String contenu = asCsvFile(results.SommationLREnErrors, filename, status);
            String titre = "Liste des débiteurs en erreur";
            String listVide = "(aucun débiteur en erreur)";
            document.addListeDetaillee(writer, results.SommationLREnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    /**
     * Genère le rapport (PDF) pour l'envoi des listes nominatives
     *
     * @param results le résultat de l'exécution du job
     * @return le rapport
     */
    public ListesNominativesRapport generateRapport(final ListesNominativesResults results, final StatusManager statusManager) {
        final String nom = "RapportListesNominatives" + results.getDateTraitement().index();
        final String description = "Rapport de la génération des listes nominatives au " + results.getDateTraitement() + ".";
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(ListesNominativesRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<ListesNominativesRapport>() {
                        public void writeDoc(ListesNominativesRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, statusManager);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final ListesNominativesResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfRapport document = new PdfRapport() {
        };
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport de génération des listes nominatives");

        // Paramètres
        document.addEntete1("Paramètre");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Type d'adresses :", String.valueOf(results.getTypeAdressesIncluses().getDescription()));
	                table.addLigne("Inclure les personnes physiques / ménages :", String.valueOf(results.isAvecContribuables()));
	                table.addLigne("Inclure les débiteurs de prestations imposables :", String.valueOf(results.isAvecDebiteurs()));
	                table.addLigne("Nombre de threads :", String.valueOf(results.getNombreThreads()));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.isInterrompu()) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de tiers listés :", String.valueOf(results.getNombreTiersTraites()));
                    table.addLigne("Dont tiers en erreur :", String.valueOf(results.getListeErreurs().size()));
	                table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
                    table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
                }
            });
        }

        // Contribuables ok
        {
            final String filename = "tiers.csv";
            final String contenu = genererListesNominatives(results, filename, status);
            final String titre = "Liste des tiers";
            final String listVide = "(aucun)";
            document.addListeDetaillee(writer, results.getListeTiers().size(), titre, listVide, filename, contenu);
        }

        // Contribuables en erreurs
        {
            final String filename = "tiers_en_erreur.csv";
            final String contenu = genererErreursListesNominatives(results, filename, status);
            final String titre = "Liste des tiers en erreur";
            final String listVide = "(aucun)";
            document.addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private String genererErreursListesNominatives(ListesNominativesResults results, String filename, StatusManager status) {

        String contenu = null;
        final List<ListesNominativesResults.Erreur> list = results.getListeErreurs();
        final int size = list.size();
        if (size > 0) {

            final StringBuilder b = new StringBuilder(100 * list.size());
            b.append("Numéro de contribuable" + COMMA + "Erreur" + COMMA + "Complément\n");

            final String message = String.format("Génération du fichier %s", filename);
            status.setMessage(message, 0);

            final GentilIterator<ListesNominativesResults.Erreur> iter = new GentilIterator<ListesNominativesResults.Erreur>(list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(message, iter.getPercent());
                }

                final ListesNominativesResults.Erreur ligne = iter.next();
                b.append(ligne.noCtb).append(COMMA);
                b.append(ligne.getDescriptionRaison().replaceAll("[;\"]", "")).append(COMMA);
                if (ligne.details != null) {
                    b.append(ligne.details.replaceAll("[;\"]", ""));
                }
                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

	private static StringBuilder addNullableString(StringBuilder b, String string) {
		if (string != null) {
			b.append(string.trim().replaceAll("[;\"]", ""));
		}
		return b;
	}

    private String genererListesNominatives(ListesNominativesResults results, String filename, StatusManager status) {

        String contenu = null;
        final List<ListesNominativesResults.InfoTiers> list = results.getListeTiers();
        final int size = list.size();
        if (size > 0) {

            final StringBuilder b = new StringBuilder(100 * list.size());
            b.append("NUMERO_CTB").append(COMMA).append("NOM_1").append(COMMA).append("NOM_2");
            if (results.getTypeAdressesIncluses() == TypeAdresse.FORMATTEE) {
                b.append(COMMA).append("ADRESSE_1").append(COMMA).append("ADRESSE_2").append(COMMA).append("ADRESSE_3");
				b.append(COMMA).append("ADRESSE_4").append(COMMA).append("ADRESSE_5").append(COMMA).append("ADRESSE_6");
            }
	        else if (results.getTypeAdressesIncluses() == TypeAdresse.STRUCTUREE_RF) {
	            b.append(COMMA).append("RUE").append(COMMA).append("NPA");
	            b.append(COMMA).append("LOCALITE").append(COMMA).append("PAYS");
            }
            b.append("\n");

            final String message = String.format("Génération du fichier %s", filename);
            status.setMessage(message, 0);

            final GentilIterator<ListesNominativesResults.InfoTiers> iter = new GentilIterator<ListesNominativesResults.InfoTiers>(
                    list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(message, iter.getPercent());
                }

                final ListesNominativesResults.InfoTiers ligne = iter.next();
                b.append(ligne.numeroTiers).append(COMMA);
	            addNullableString(b, ligne.nomPrenom1).append(COMMA);
	            addNullableString(b, ligne.nomPrenom2);

                if (results.getTypeAdressesIncluses() == TypeAdresse.FORMATTEE) {
                    Assert.isTrue(ligne instanceof ListesNominativesResults.InfoTiersAvecAdresseFormattee);
                    final ListesNominativesResults.InfoTiersAvecAdresseFormattee ligneAvecAdresse = (ListesNominativesResults.InfoTiersAvecAdresseFormattee) ligne;
                    final String[] adresse = ligneAvecAdresse.adresse;
                    for (int indexLigne = 0; indexLigne < adresse.length; ++indexLigne) {
                        b.append(COMMA);
	                    addNullableString(b, adresse[indexLigne]);
                    }
                }
	            else if (results.getTypeAdressesIncluses() == TypeAdresse.STRUCTUREE_RF) {
	                Assert.isTrue(ligne instanceof ListesNominativesResults.InfoTiersAvecAdresseStructureeRF);
	                final ListesNominativesResults.InfoTiersAvecAdresseStructureeRF ligneAvecAdresse = (ListesNominativesResults.InfoTiersAvecAdresseStructureeRF) ligne;
	                b.append(COMMA);
	                addNullableString(b, ligneAvecAdresse.rue).append(COMMA);
	                addNullableString(b, ligneAvecAdresse.npa).append(COMMA);
	                addNullableString(b, ligneAvecAdresse.localite).append(COMMA);
	                addNullableString(b, ligneAvecAdresse.pays);
                }

                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

    public AcomptesRapport generateRapport(final AcomptesResults results, final StatusManager statusManager) {
        final String nom = "RapportAcomptes" + results.getDateTraitement().index();
        final String description = "Rapport de la génération des populations pour les bases acomptes au " + results.getDateTraitement()
                + ".";
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(AcomptesRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<AcomptesRapport>() {
                        public void writeDoc(AcomptesRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, statusManager);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final AcomptesResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfRapport document = new PdfRapport() {
        };
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport de génération des populations pour les bases acomptes");

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.isInterrompu()) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total traité :", String.valueOf(results.getNombreContribuablesAssujettisTraites()));
                    table.addLigne("Nombre total en erreur :", String.valueOf(results.getListeErreurs().size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
                }
            });
        }

        // Contribuables ok
        {
            final String filename = "contribuables_acomptes.csv";
            final String contenu = genererAcomptes(results, filename, status);
            final String titre = "Liste des populations pour les bases acomptes";
            final String listVide = "(aucun)";
            document.addListeDetaillee(writer, results.getListeContribuablesAssujettis().size(), titre, listVide, filename, contenu);
        }

        // Contribuables en erreurs
        {
            final String filename = "contribuables_acomptes_en_erreur.csv";
            final String contenu = genererErreursAcomptes(results, filename, status);
            final String titre = "Liste des populations pour les bases acomptes en erreur";
            final String listVide = "(aucun contribuable en erreur)";
            document.addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
        }

	    // contribuables ignorés (for intersectant avec la periode fiscale mais pas d'assujettissement, ou assujettissement ne donnant pas droit aux acomptes)
	    {
		    final String filename = "contribuables_acomptes_ingorés.csv";
		    final String contenu = genererListeIgnoresAcomptes(results, filename, status);
		    final String titre =" Liste des populations ignorées ayant un for sur une période fiscale concernée";
		    final String listeVide = "(aucun)";
		    document.addListeDetaillee(writer, results.getContribuablesIgnores().size(), titre, listeVide, filename, contenu);
	    }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

	private String genererListeIgnoresAcomptes(AcomptesResults results, String filename, StatusManager status) {

		String contenu = null;
		final List<AcomptesResults.InfoContribuableIgnore> list = results.getContribuablesIgnores();
		final int size = list.size();
		if (size > 0) {

		    final StringBuilder b = new StringBuilder(100 * list.size());
		    b.append("Numéro de contribuable" + COMMA + "Année fiscale" + COMMA + "Complément\n");

		    final String message = String.format("Génération du fichier %s", filename);
		    status.setMessage(message, 0);

		    final GentilIterator<AcomptesResults.InfoContribuableIgnore> iter = new GentilIterator<AcomptesResults.InfoContribuableIgnore>(list);
		    while (iter.hasNext()) {
		        if (iter.isAtNewPercent()) {
		            status.setMessage(message, iter.getPercent());
		        }

		        final AcomptesResults.InfoContribuableIgnore ligne = iter.next();
		        b.append(ligne.getNumeroCtb()).append(COMMA);
			    b.append(ligne.getAnneeFiscale()).append(COMMA);
			    b.append(ligne.toString().replaceAll("[;\"]", ""));
		        if (!iter.isLast()) {
		            b.append("\n");
		        }
		    }
		    contenu = b.toString();
		}
		return contenu;
	}

	private String genererErreursAcomptes(AcomptesResults results, String filename, StatusManager status) {

        String contenu = null;
        final List<AcomptesResults.Erreur> list = results.getListeErreurs();
        final int size = list.size();
        if (size > 0) {

            final StringBuilder b = new StringBuilder(100 * list.size());
            b.append("Numéro de contribuable" + COMMA + "Erreur" + COMMA + "Complément\n");

            final String message = String.format("Génération du fichier %s", filename);
            status.setMessage(message, 0);

            final GentilIterator<AcomptesResults.Erreur> iter = new GentilIterator<AcomptesResults.Erreur>(list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(message, iter.getPercent());
                }

                final AcomptesResults.Erreur ligne = iter.next();
                b.append(ligne.noCtb).append(COMMA);
                b.append(ligne.getDescriptionRaison().replaceAll("[;\"]", "")).append(COMMA);
                if (ligne.details != null) {
                    b.append(ligne.details.replaceAll("[;\"]", ""));
                }
                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

	private void fillLigneBuffer(StringBuilder b, long numeroCtb, String nom, String prenom, AcomptesResults.InfoAssujettissementContribuable assujettissement, String typeImpot) {
		b.append(numeroCtb).append(COMMA);

		if (nom != null) {
		    b.append(nom.replaceAll("[;\"]", ""));
		}
		b.append(COMMA);
		if (prenom != null) {
		    b.append(prenom.replaceAll("[;\"]", ""));
		}
		b.append(COMMA);

		if (assujettissement.noOfsForPrincipal != null) {
		    b.append(assujettissement.noOfsForPrincipal);
		}
		b.append(COMMA);

		if (assujettissement.noOfsForGestion != null) {
		    b.append(assujettissement.noOfsForGestion);
		}
		b.append(COMMA);

		if (assujettissement.typeContribuable != null) {
		    b.append(assujettissement.typeContribuable.descriptionAcomptes());
		}
		b.append(COMMA);

		b.append(typeImpot).append(COMMA);
		b.append(assujettissement.anneeFiscale);
	}

    private String genererAcomptes(AcomptesResults results, String filename, StatusManager status) {

        String contenu = null;
        final List<AcomptesResults.InfoContribuableAssujetti> list = results.getListeContribuablesAssujettis();
        final int size = list.size();
        if (size > 0) {

            final StringBuilder b = new StringBuilder(300 * list.size());
            b.append("Numéro de contribuable" + COMMA + "Nom du contribuable principal" + COMMA + "Prénom du contribuable principal" + COMMA
                    + "For principal" + COMMA + "For de gestion" + COMMA + "Type de population" + COMMA + "Type d'impôt" + COMMA
                    + "Période fiscale" + "\n");

            final String message = String.format("Génération du fichier %s", filename);
            status.setMessage(message, 0);

            final GentilIterator<AcomptesResults.InfoContribuableAssujetti> iter = new GentilIterator<AcomptesResults.InfoContribuableAssujetti>(list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(message, iter.getPercent());
                }

                final AcomptesResults.InfoContribuableAssujetti ligne = iter.next();

	            final String nom = ligne.getNom() != null ? ligne.getNom().trim() : null;
	            final String prenom = ligne.getPrenom() != null ? ligne.getPrenom().trim() : null;

	            final AcomptesResults.InfoAssujettissementContribuable assujettissementIcc = ligne.getAssujettissementIcc();
	            final AcomptesResults.InfoAssujettissementContribuable assujettissementIfd = ligne.getAssujettissementIfd();

	            // ICC
	            if (assujettissementIcc != null) {
		            fillLigneBuffer(b, ligne.getNumeroCtb(), nom, prenom, assujettissementIcc, TYPE_IMPOT_ICC);
	            }

	            if (!iter.isLast() || assujettissementIfd != null) {
		            b.append("\n");
	            }

	            // IFD
	            if (assujettissementIfd != null) {
		            fillLigneBuffer(b, ligne.getNumeroCtb(), nom, prenom, assujettissementIfd, TYPE_IMPOT_IFD);

		            if (!iter.isLast()) {
			            b.append("\n");
		            }
	            }
            }
            contenu = b.toString();
        }
        return contenu;
    }

    /**
     * Genère le rapport (PDF) pour les impressions en masse des chemises de taxation d'office
     *
     * @param results le résultat de l'exécution du job
     * @return le rapport
     */
    public ImpressionChemisesTORapport generateRapport(final ImpressionChemisesTOResults results, final StatusManager statusManager) {
        final String nom = "RapportChemisesTO" + results.getDateTraitement().index();
        final String description = "Rapport de l'impression des chemises de taxation d'office au " + results.getDateTraitement() + ".";
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(ImpressionChemisesTORapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<ImpressionChemisesTORapport>() {
                        public void writeDoc(ImpressionChemisesTORapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, statusManager);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writePDF(final ImpressionChemisesTOResults results, String nom, String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        final PdfRapport document = new PdfRapport() {
        };
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'impression des chemises de taxation d'office");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.getDateTraitement()));
                    table.addLigne("Nombre maximal de chemises à imprimer :", Integer.toString(results.getNbMax()));
                    table.addLigne("Office d'impôt : ", results.getNomOid() == null ? "tous" : results.getNomOid());
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.isInterrompu()) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total d'impressions :", Integer.toString(results.getNbChemisesImprimees()));
                    table.addLigne("Nombre total d'erreurs :", Integer.toString(results.getErreurs().size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
                }
            });
        }

        // Impressions OK
        {
            final String filename = "chemises_to_imprimees.csv";
            final String contenu = genererListeChemisesTO(results, filename, status);
            final String titre = "Liste des chemises TO imprimées";
            final String listVide = "(aucun)";
            document.addListeDetaillee(writer, results.getNbChemisesImprimees(), titre, listVide, filename, contenu);
        }

        // Impressions en erreur
        {
            final String filename = "chemises_to_erreurs.csv";
            final String contenu = genererErreursChemisesTO(results, filename, status);
            final String titre = "Liste des déclarations d'impôt en erreur";
            final String listVide = "(aucune)";
            document.addListeDetaillee(writer, results.getErreurs().size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private String genererErreursChemisesTO(ImpressionChemisesTOResults results, String filename, StatusManager status) {
        String contenu = null;
        final List<ImpressionChemisesTOResults.Erreur> list = results.getErreurs();
        final int size = list.size();
        if (size > 0) {

            final StringBuilder b = new StringBuilder(300 * list.size());
            b.append("ID Déclaration").append(COMMA).append("Message\n");

            final String message = String.format("Génération du fichier %s", filename);
            status.setMessage(message, 0);

            final GentilIterator<ImpressionChemisesTOResults.Erreur> iter = new GentilIterator<ImpressionChemisesTOResults.Erreur>(list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(message, iter.getPercent());
                }

                final ImpressionChemisesTOResults.Erreur ligne = iter.next();

                b.append(ligne.getIdDeclaration()).append(COMMA);
                if (ligne.getDetails() != null) {
                    b.append(ligne.getDetails().replaceAll("[;\"]", ""));
                }

                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

    private String genererListeChemisesTO(ImpressionChemisesTOResults results, String filename, StatusManager status) {
        String contenu = null;
        final List<ImpressionChemisesTOResults.ChemiseTO> list = results.getChemisesImprimees();
        final int size = list.size();
        if (size > 0) {

            final StringBuilder b = new StringBuilder(300 * list.size());
            b.append("Numéro de l'office d'impôt").append(COMMA).append("Numéro de contribuable").append(COMMA + "Nom du contribuable")
                    .append(COMMA).append("Date début période").append(COMMA).append("Date fin période").append(COMMA).append(
                    "Date sommation\n");

            final String message = String.format("Génération du fichier %s", filename);
            status.setMessage(message, 0);

            final GentilIterator<ImpressionChemisesTOResults.ChemiseTO> iter = new GentilIterator<ImpressionChemisesTOResults.ChemiseTO>(
                    list);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(message, iter.getPercent());
                }

                final ImpressionChemisesTOResults.ChemiseTO ligne = iter.next();

                b.append(ligne.officeImpotID).append(COMMA);
                b.append(ligne.noCtb).append(COMMA);
                if (ligne.nomCtb != null) {
                    b.append(ligne.nomCtb.replaceAll("[;\"]", ""));
                }
                b.append(COMMA);
                b.append(ligne.getDateDebutDi()).append(COMMA);
                b.append(ligne.getDateFinDi()).append(COMMA);
                b.append(ligne.getDateSommationDi()).append(COMMA);

                if (!iter.isLast()) {
                    b.append("\n");
                }
            }
            contenu = b.toString();
        }
        return contenu;
    }

    /**
     * {@inheritDoc}
     */
    public EchoirDIsRapport generateRapport(final EchoirDIsResults results, StatusManager s) {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final String nom = "RapportEchoirDIs" + results.dateTraitement.index();
        final String description = "Rapport d'exécution du job de passage des DIs sommées à l'état échu. Date de traitement = "
                + results.dateTraitement + ".";
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(EchoirDIsRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<EchoirDIsRapport>() {
                        public void writeDoc(EchoirDIsRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    private void writePDF(final EchoirDIsResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfDIsRapport document = new PdfDIsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job de passage des DIs sommées à l'état échu");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de déclarations inspectées:", String.valueOf(results.nbDIsTotal));
                    table.addLigne("Nombre de déclarations passées dans l'état échu:", String.valueOf(results.disEchues.size()));
                    table.addLigne("Nombre de déclarations ignorées:", String.valueOf(results.disIgnorees.size()));
                    table.addLigne("Nombre de déclarations en erreur:", String.valueOf(results.disEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // DIs échues
        {
            String filename = "dis_echues.csv";
            String contenu = disEchuesAsCsvFile(results.disEchues, filename, status);
            String titre = "Liste des déclarations nouvellement échues";
            String listVide = "(aucun déclaration échue)";
            document.addListeDetaillee(writer, results.disEchues.size(), titre, listVide, filename, contenu);
        }

        // DIs ignorées
        {
            String filename = "dis_ignorees.csv";
            String contenu = asCsvFile(results.disIgnorees, filename, status);
            String titre = "Liste des déclarations ignorées";
            String listVide = "(aucun déclaration ignorée)";
            document.addListeDetaillee(writer, results.disIgnorees.size(), titre, listVide, filename, contenu);
        }

        // DIs en erreur
        {
            String filename = "dis_en_erreur.csv";
            String contenu = asCsvFile(results.disEnErrors, filename, status);
            String titre = "Liste des déclarations en erreur";
            String listVide = "(aucun déclaration en erreur)";
            document.addListeDetaillee(writer, results.disEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private String disEchuesAsCsvFile(List<Echue> disEchues, String filename, StatusManager status) {
        String contenu = null;
        int size = disEchues.size();
        if (size > 0) {

            StringBuilder b = new StringBuilder(AVG_LINE_LEN * disEchues.size());
            b.append("Numéro de l'office d'impôt").append(COMMA).append("Numéro de contribuable").append(COMMA).append(
                    "Numéro de la déclaration\n");

            final GentilIterator<Echue> iter = new GentilIterator<Echue>(disEchues);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }

                Echue info = iter.next();
                StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
                bb.append(info.officeImpotID).append(COMMA);
                bb.append(info.ctbId).append(COMMA);
                bb.append(info.diId);
                if (!iter.isLast()) {
                    bb.append("\n");
                }

                b.append(bb);
            }
            contenu = b.toString();
        }
        return contenu;
    }

    /**
     * {@inheritDoc}
     */
    public ReinitialiserBaremeDoubleGainRapport generateRapport(final ReinitialiserBaremeDoubleGainResults results, StatusManager s) {

        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final String nom = "ReinitDoubleGain" + results.dateTraitement.index();
        final String description = "Rapport d'exécution du job de réinitialisation des barêmes double-gain. Date de traitement = "
                + results.dateTraitement + ".";
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(ReinitialiserBaremeDoubleGainRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<ReinitialiserBaremeDoubleGainRapport>() {
                        public void writeDoc(ReinitialiserBaremeDoubleGainRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void writePDF(final ReinitialiserBaremeDoubleGainResults results, String nom, String description, final Date dateGeneration,
                            OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfReinitDoubleGainRapport document = new PdfReinitDoubleGainRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job de réinitialisation des barêmes double-gain");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de situations de familles inspectées:", String.valueOf(results.nbSituationsTotal));
                    table.addLigne("Nombre de situations réinitialisées:", String.valueOf(results.situationsTraitees.size()));
                    table.addLigne("Nombre de situations en erreur:", String.valueOf(results.situationsEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // Situations réinitialisées
        {
            String filename = "situations_reinitialisees.csv";
            String contenu = situationsTraiteesAsCsvFile(results.situationsTraitees, filename, status);
            String titre = "Liste des situations réinitialisées";
            String listVide = "(aucun situation réinitialisée)";
            document.addListeDetaillee(writer, results.situationsTraitees.size(), titre, listVide, filename, contenu);
        }

        // Situations en erreur
        {
            String filename = "situations_en_erreur.csv";
            String contenu = asCsvFile(results.situationsEnErrors, filename, status);
            String titre = "Liste des situations en erreur";
            String listVide = "(aucun situation en erreur)";
            document.addListeDetaillee(writer, results.situationsEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private String situationsTraiteesAsCsvFile(List<Situation> situations, String filename, StatusManager status) {
        String contenu = null;
        int size = situations.size();
        if (size > 0) {

            StringBuilder b = new StringBuilder(AVG_LINE_LEN * situations.size());
            b.append("Numéro de l'office d'impôt").append(COMMA).append("Numéro de contribuable").append(COMMA).append(
                    "Numéro de l'ancienne situation de famille").append(COMMA).append("Numéro de la nouvelle situation de famille\n");

            final GentilIterator<Situation> iter = new GentilIterator<Situation>(situations);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }

                Situation situation = iter.next();
                StringBuilder bb = new StringBuilder(AVG_LINE_LEN);
                bb.append(situation.officeImpotID).append(COMMA);
                bb.append(situation.ctbId).append(COMMA);
                bb.append(situation.ancienneId).append(COMMA);
                bb.append(situation.nouvelleId);
                if (!iter.isLast()) {
                    bb.append("\n");
                }

                b.append(bb);
            }
            contenu = b.toString();
        }
        return contenu;
    }

    public ListeTachesEnIsntanceParOIDRapport generateRapport(final ListeTachesEnIsntanceParOID results, final StatusManager status) {
        final String nom = "RapportListeTacheEnInstanceParOID" + results.dateTraitement.index();
        final String description = "Rapport de la liste des Taches en instance par OID." + ". Date de traitement = " + results.dateTraitement;
        final Date dateGeneration = new Date();
        try {
            return docService.newDoc(ListeTachesEnIsntanceParOIDRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<ListeTachesEnIsntanceParOIDRapport>() {
                        public void writeDoc(ListeTachesEnIsntanceParOIDRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExclureContribuablesEnvoiRapport generateRapport(final ExclureContribuablesEnvoiResults results, StatusManager s) {
        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final RegDate dateTraitement = RegDate.get();
        final String nom = "RapportExclCtbsEnvoi" + dateTraitement.index();
        final String description = "Rapport d'exécution du job d'exclusion de contribuables de l'envoi automatique de DIs. Date de traitement = "
                + dateTraitement + ".";
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(ExclureContribuablesEnvoiRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<ExclureContribuablesEnvoiRapport>() {
                        public void writeDoc(ExclureContribuablesEnvoiRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    private void writePDF(final ExclureContribuablesEnvoiResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfDIsRapport document = new PdfDIsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du job d'exclusion de contribuables de l'envoi automatique de DIs");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de limite d'exclusion:", RegDateHelper.dateToDisplayString(results.dateLimiteExclusion));
                    table.addLigne("Contribuables à exclure:", "(voir le fichier contribuables_a_exclure.csv)");
                }
            });
            // ids en entrées
            String filename = "contribuables_a_exclure.csv";
            String contenu = idsCtbsAsCsvFile(results.ctbsIds, filename, status);
            document.attacheFichier(writer, filename, description, contenu, 500);
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables traités:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre de contribuables ignorés:", String.valueOf(results.ctbsIgnores.size()));
                    table.addLigne("Nombre de contribuables en erreur:", String.valueOf(results.ctbsEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // DIs ignorées
        {
            String filename = "ctbs_ignorees.csv";
            String contenu = asCsvFile(results.ctbsIgnores, filename, status);
            String titre = "Liste des contribuables ignorés";
            String listVide = "(aucun contribuable ignoré)";
            document.addListeDetaillee(writer, results.ctbsIgnores.size(), titre, listVide, filename, contenu);
        }

        // DIs en erreur
        {
            String filename = "ctbs_en_erreur.csv";
            String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
            String titre = "Liste des contribuables en erreur";
            String listVide = "(aucun contribuable en erreur)";
            document.addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private String idsCtbsAsCsvFile(List<Long> ctbsIds, String filename, StatusManager status) {
        String contenu = null;
        int size = ctbsIds.size();
        if (size > 0) {

            StringBuilder b = new StringBuilder(AVG_LINE_LEN * ctbsIds.size());
            b.append("Numéro de contribuable\n");

            final GentilIterator<Long> iter = new GentilIterator<Long>(ctbsIds);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }

                Long id = iter.next();
                b.append(id).append('\n');
            }
            contenu = b.toString();
        }
        return contenu;
    }

    /**
     * {@inheritDoc}
     */
    public DemandeDelaiCollectiveRapport generateRapport(final DemandeDelaiCollectiveResults results, StatusManager s) {
        final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

        final RegDate dateTraitement = RegDate.get();
        final String nom = "RapportDemDelaiColl" + dateTraitement.index();
        final String description = "Rapport d'exécution du traitement d'une demande de délais collective. Date de traitement = "
                + dateTraitement + ".";
        final Date dateGeneration = new Date();

        try {
            return docService.newDoc(DemandeDelaiCollectiveRapport.class, nom, description, "pdf",
                    new DocumentService.WriteDocCallback<DemandeDelaiCollectiveRapport>() {
                        public void writeDoc(DemandeDelaiCollectiveRapport doc, OutputStream os) throws Exception {
                            writePDF(results, nom, description, dateGeneration, os, status);
                        }
                    });
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    protected void writePDF(final DemandeDelaiCollectiveResults results, String nom, String description, final Date dateGeneration,
                            OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfDIsRapport document = new PdfDIsRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du traitement d'une demande de délais collective");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Période fiscale:", String.valueOf(results.annee));
                    table.addLigne("Date de délai:", RegDateHelper.dateToDisplayString(results.dateDelai));
                    table.addLigne("Contribuables à traiter:", "(voir le fichier contribuables_a_traiter.csv)");
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
            // ids en entrées
            String filename = "contribuables_a_traiter.csv";
            String contenu = idsCtbsAsCsvFile(results.ctbsIds, filename, status);
            document.attacheFichier(writer, filename, "Contribuables à traiter", contenu, 500);
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables traités:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre de déclarations traitées:", String.valueOf(results.traites.size()));
                    table.addLigne("Nombre de déclarations ignorés:", String.valueOf(results.ignores.size()));
                    table.addLigne("Nombre d'erreurs:", String.valueOf(results.errors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // DIs traitées
        {
            String filename = "dis_traitees.csv";
            String contenu = delaisTraitesAsCsvFile(results.traites, filename, status);
            String titre = "Liste des déclarations traitées";
            String listVide = "(aucun déclaration traitée)";
            document.addListeDetaillee(writer, results.traites.size(), titre, listVide, filename, contenu);
        }

        // DIs ignorées
        {
            String filename = "dis_ignorees.csv";
            String contenu = asCsvFile(results.ignores, filename, status);
            String titre = "Liste des déclarations ignorées";
            String listVide = "(aucun déclaration ignorée)";
            document.addListeDetaillee(writer, results.ignores.size(), titre, listVide, filename, contenu);
        }

        // les erreur
        {
            String filename = "erreurs.csv";
            String contenu = asCsvFile(results.errors, filename, status);
            String titre = "Liste des erreurs";
            String listVide = "(aucune erreur)";
            document.addListeDetaillee(writer, results.errors.size(), titre, listVide, filename, contenu);
        }

        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    private String delaisTraitesAsCsvFile(List<Traite> traites, String filename, StatusManager status) {
        String contenu = null;
        int size = traites.size();
        if (size > 0) {

            StringBuilder b = new StringBuilder(AVG_LINE_LEN * traites.size());
            b.append("Numéro de contribuable").append(COMMA).append("Numéro de déclaration\n");

            final GentilIterator<Traite> iter = new GentilIterator<Traite>(traites);
            while (iter.hasNext()) {
                if (iter.isAtNewPercent()) {
                    status.setMessage(String.format("Génération du fichier %s", filename), iter.getPercent());
                }

                Traite t = iter.next();
                b.append(t.ctbId).append(COMMA).append(t.diId).append('\n');
            }
            contenu = b.toString();
        }
        return contenu;
    }

	public RapprocherCtbRapport generateRapport(final RapprocherCtbResults  results, StatusManager s) {
		 final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

	        final String nom = "RapportRapprochementCtbs";
	        final String description = "Rapport d'exécution du job qui fait le rappochement entre les contribuables et les propriétaires fonciers"
	                + ". Date de traitement = " + results.dateTraitement;
	        final Date dateGeneration = new Date();

	        try {
	            return docService.newDoc(RapprocherCtbRapport.class, nom, description, "pdf",
	                    new DocumentService.WriteDocCallback<RapprocherCtbRapport>() {
	                        public void writeDoc(RapprocherCtbRapport doc, OutputStream os) throws Exception {
	                            writePDF(results, nom, description, dateGeneration, os, status);
	                        }
	                    });
	        }
	        catch (Exception e) {
	            throw new RuntimeException(e);
	        }
	}

	/**
     * Génère un rapport au format PDF à partir des résultats de job.
     */
    private void writePDF(final RapprocherCtbResults results, final String nom, final String description, final Date dateGeneration,
                          OutputStream os, StatusManager status) throws Exception {

        Assert.notNull(status);

        // Création du document PDF
        PdfRapprochementCtbRapport document = new PdfRapprochementCtbRapport();
        PdfWriter writer = PdfWriter.getInstance(document, os);
        document.open();
        document.addMetaInfo(nom, description);
        document.addEnteteUnireg();

        // Titre
        document.addTitrePrincipal("Rapport d'exécution du rapprochement des contribuables et des propriétaires fonciers");

        // Paramètres
        document.addEntete1("Paramètres");
        {
            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Date de traitement:", RegDateHelper.dateToDisplayString(results.dateTraitement));
                }
            });
        }

        // Résultats
        document.addEntete1("Résultats");
        {
            if (results.interrompu) {
                document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
                        + "les valeurs ci-dessous sont donc incomplètes.");
            }

            document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
                public void fillTable(PdfTableSimple table) throws DocumentException {
                    table.addLigne("Nombre total de contribuables:", String.valueOf(results.nbCtbsTotal));
                    table.addLigne("Nombre d'Individu trouvés avec correspondance exacte:", String.valueOf(results.nbIndividuTrouvesExact));
                    table.addLigne("Nombre d'Individu trouvés avec correspondance sauf date de naissance:", String.valueOf(results.nbIndividuTrouvesSaufDateNaissance));
                    table.addLigne("Nombre d'Individu trouvé sans correspondance exacte:", String.valueOf(results.nbIndividuTrouvesSansCorrespondance));
                    table.addLigne("Pas de contribuable trouvé:", String.valueOf(results.nbCtbInconnu));
                    table.addLigne("Pas d'individu trouvé:", String.valueOf(results.nbIndviduInconnu));
                    table.addLigne("Plus de deux individus trouvés:", String.valueOf(results.nbPlusDeDeuxIndividu));
                    table.addLigne("Nombre d'erreurs:", String.valueOf(results.ctbsEnErrors.size()));
	                table.addLigne("Durée d'exécution du job:", formatDureeExecution(results));
                    table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
                }
            });
        }

        // CTBs rapprochés
        {
            String filename = "contribuables_rapproches.csv";
            String contenu = ctbRapprocheAsCsvFile(results.listeRapproche, filename, status);
            String titre = "Liste des contribuables rapprochés";
            String listVide = "(aucun contribuable rapprocher)";
            document.addListeDetaillee(writer, results.listeRapproche.size(), titre, listVide, filename, contenu);
        }

     // les erreur
        {
            String filename = "erreurs.csv";
            String contenu = asCsvFile(results.ctbsEnErrors, filename, status);
            String titre = "Liste des erreurs";
            String listVide = "(aucune erreur)";
            document.addListeDetaillee(writer, results.ctbsEnErrors.size(), titre, listVide, filename, contenu);
        }


        document.close();

        status.setMessage("Génération du rapport terminée.");
    }

    /**
     * Construit le contenu du fichier détaillé des contribuables rapprochés
     */
	private String ctbRapprocheAsCsvFile(List<ProprietaireRapproche> listeRapprochee, String filename, StatusManager status) {
		String contenu = null;
		int size = listeRapprochee.size();
		if (size > 0) {
			StringBuilder b = new StringBuilder("NumeroFoncier;");
			b.append("Nom;");
			b.append("Prénom;");
			b.append("DateNaissance;");
			b.append("NoCTB;");
			b.append("NoCTB1;");
			b.append("Nom1;");
			b.append("Prénom1;");
			b.append("DateNaissance1;");
			b.append("NoCTB2;");
			b.append("Nom2;");
			b.append("Prénom2;");
			b.append("DateNaissance2;");
			b.append("FormulePolitesse;");
			b.append("NomCourrier1;");
			b.append("NomCourrier2;");
			b.append("Résultat\n");
			for (ProprietaireRapproche proprietaireRapproche : listeRapprochee) {
				b.append(proprietaireRapproche.getNumeroRegistreFoncier() + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNom()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getPrenom()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getDateNaissance()) + ";");
				b.append(proprietaireRapproche.getNumeroContribuable() + ";");
				b.append(convertNullToEmpty(String.valueOf(proprietaireRapproche.getNumeroContribuable1())) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNom1()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getPrenom1()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getDateNaissance1()) + ";");
				b.append(convertNullToEmpty(String.valueOf(proprietaireRapproche.getNumeroContribuable2())) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNom2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getPrenom2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getDateNaissance2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getFormulePolitesse()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNomCourrier1()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getNomCourrier2()) + ";");
				b.append(convertNullToEmpty(proprietaireRapproche.getResultat()) + ";");
				b.append("\n");
			}
			contenu = b.toString();

		}
		return contenu;
	}

	/**
	 * Convertit la valeur null en chaine vide poour une string passée en paramètre
	 *
	 * @param data
	 * @return chaine vide
	 */
	public static String convertNullToEmpty(final String data) {
		String maChaine;
		maChaine = (data == null) ? "" : data;
		return maChaine;

	}
	public static String convertNullToEmpty(final RegDate data) {
		String maChaine;
		maChaine = (data == null) ? "" : data.toString();
		return maChaine;

	}

	public ListeContribuablesResidentsSansForVaudoisRapport generateRapport(final ListeContribuablesResidentsSansForVaudoisResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

	       final String nom = "RapportResSansForVD";
	       final String description = "Rapport d'exécution du job qui liste les contribuables résidents suisses ou titulaires d'un permis C sans for vaudois"
	               + ". Date de traitement = " + results.getDateTraitement();
	       final Date dateGeneration = new Date();

	       try {
	           return docService.newDoc(ListeContribuablesResidentsSansForVaudoisRapport.class, nom, description, "pdf",
	                   new DocumentService.WriteDocCallback<ListeContribuablesResidentsSansForVaudoisRapport>() {
	                       public void writeDoc(ListeContribuablesResidentsSansForVaudoisRapport doc, OutputStream os) throws Exception {
	                           writePDF(results, nom, description, dateGeneration, os, status);
	                       }
	                   });
	       }
	       catch (Exception e) {
	           throw new RuntimeException(e);
	       }
	}

	private void writePDF(final ListeContribuablesResidentsSansForVaudoisResults results, String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfRapport document = new PdfListeContribuablesResidentsSansForVaudoisRapport();
		PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal("Rapport d'exécution du job qui listes les contribuables résidents suisses ou titulaires d'un permis C sans for vaudois");

		// Résultats
		document.addEntete1("Résultats");
		{
		    if (results.isInterrompu()) {
		        document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
		                + "les valeurs ci-dessous sont donc incomplètes.");
		    }

		    document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
		        public void fillTable(PdfTableSimple table) throws DocumentException {
		            table.addLigne("Nombre total de contribuables inspectés :", String.valueOf(results.getNombreContribuablesInspectes()));
		            table.addLigne("Nombre de contribuables identifiés :", String.valueOf(results.getContribuablesIdentifies().size()));
		            table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.getContribuablesIgnores().size()));
		            table.addLigne("Nombre d'erreurs :", String.valueOf(results.getListeErreurs().size()));
			        table.addLigne("Durée d'exécution du job :", formatDureeExecution(results));
		            table.addLigne("Date de génération du rapport :", formatTimestamp(dateGeneration));
		        }
		    });
		}

		// contribuables cibles de ce job
		{
		    String filename = "ctbs_identifies.csv";
		    String contenu = buildListeContribuablesIdentifies(results.getContribuablesIdentifies(), filename, status);
		    String titre = "Liste des contribuables identifiés";
		    String listVide = "(aucun)";
		    document.addListeDetaillee(writer, results.getContribuablesIdentifies().size(), titre, listVide, filename, contenu);
		}

		// contribuables ignorés
		{
		    String filename = "ctbs_ignores.csv";
		    String contenu = buildContribuablesIgnores(results.getContribuablesIgnores(), filename, status);
		    String titre = "Liste des contribuables ignorés";
		    String listVide = "(aucun)";
		    document.addListeDetaillee(writer, results.getContribuablesIdentifies().size(), titre, listVide, filename, contenu);
		}

		// erreurs
		{
		    String filename = "ctbs_en_erreur.csv";
		    String contenu = buildErreurs(results.getListeErreurs(), filename, status);
		    String titre = "Liste des erreurs";
		    String listVide = "(aucune)";
		    document.addListeDetaillee(writer, results.getListeErreurs().size(), titre, listVide, filename, contenu);
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String buildListeContribuablesIdentifies(List<Long> ctbIds, String filename, StatusManager status) {

		String contenu = null;
		if (ctbIds.size() > 0) {
			final StringBuilder b = new StringBuilder((ctbIds.size() + 1 ) * 10);
			b.append("Numéro\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);
			final GentilIterator<Long> iterator = new GentilIterator<Long>(ctbIds);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}
				b.append(iterator.next());
				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}

	private String buildContribuablesIgnores(List<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore> liste, String filename, StatusManager status) {

		String contenu = null;
		if (liste.size() > 0) {
			final StringBuilder b = new StringBuilder((liste.size() + 1 ) * 50);
			b.append("Numéro" + COMMA + "Raison\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);
			final GentilIterator<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore> iterator = new GentilIterator<ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore>(liste);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final ListeContribuablesResidentsSansForVaudoisResults.InfoContribuableIgnore info = iterator.next();
				b.append(info.ctbId).append(COMMA);
				b.append(info.cause.getDescription().replaceAll("[;\"]", ""));
				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}

	private String buildErreurs(List<ListesResults.Erreur> liste, String filename, StatusManager status) {

		String contenu = null;
		if (liste.size() > 0) {
			final StringBuilder b = new StringBuilder((liste.size() + 1 ) * 100);
			b.append("Numéro" + COMMA + "Raison" + COMMA + "Complément\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);
			final GentilIterator<ListesResults.Erreur> iterator = new GentilIterator<ListesResults.Erreur>(liste);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final ListesResults.Erreur ligne = iterator.next();
				b.append(ligne.noCtb).append(COMMA);
				b.append(ligne.getDescriptionRaison().replaceAll("[;\"]", "")).append(COMMA);
				if (ligne.details != null) {
					b.append(ligne.details.replaceAll("[;\"]", ""));
				}
				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}

	public CorrectionFlagHabitantRapport generateRapport(final CorrectionFlagHabitantSurPersonnesPhysiquesResults resultsPP, final CorrectionFlagHabitantSurMenagesResults resultsMC, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportCorrectionFlagHabitant";
		final String description = "Rapport d'exécution du job qui corrige les flags 'habitant' sur les personnes physiques en fonction de leur for principal actif"
				+ ". Date de traitement = " + RegDate.get();
		final Date dateGeneration = new Date();

		try {
			return docService.newDoc(CorrectionFlagHabitantRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<CorrectionFlagHabitantRapport>() {
				public void writeDoc(CorrectionFlagHabitantRapport doc, OutputStream os) throws Exception {
					writePDF(resultsPP, resultsMC, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void writePDF(final CorrectionFlagHabitantSurPersonnesPhysiquesResults pp, final CorrectionFlagHabitantSurMenagesResults mc,
	                  String nom, String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfCorrectionFlagHabitantRapport document = new PdfCorrectionFlagHabitantRapport();
		final PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal(String.format("Rapport de correction des flags 'habitant' sur les personnes physiques\n%s", formatTimestamp(dateGeneration)));

		// Résultats
		document.addEntete1("Résultats pour les personnes physiques seules");
		{
			if (pp.isInterrupted()) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(pp.getNombreElementsInspectes()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(pp.getErreurs().size()));
					table.addLigne("Nombre de nouveaux habitants :", String.valueOf(pp.getNouveauxHabitants().size()));
					table.addLigne("Nombre de nouveaux non-habitants :", String.valueOf(pp.getNouveauxNonHabitants().size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(pp));
				}
			});
		}

		// Résultats
		document.addEntete1("Résultats pour les ménages communs");
		{
			if (mc.isInterrupted()) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(mc.getNombreElementsInspectes()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(mc.getErreurs().size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(mc));
				}
			});
		}

		// Nouveaux habitants
		{
			final String filename = "nouveaux_habitants.csv";
			final String contenu = genererListeModifications(pp.getNouveauxHabitants(), filename, status);
			final String titre = "Liste des nouveaux habitants";
			final String listVide = "(aucun)";
			document.addListeDetaillee(writer, pp.getNouveauxHabitants().size(), titre, listVide, filename, contenu);
		}

		// Nouveaux non-habitants
		{
			final String filename = "nouveaux_non_habitants.csv";
			final String contenu = genererListeModifications(pp.getNouveauxNonHabitants(), filename, status);
			final String titre = "Liste des nouveaux non-habitants";
			final String listVide = "(aucun)";
			document.addListeDetaillee(writer, pp.getNouveauxNonHabitants().size(), titre, listVide, filename, contenu);
		}

		// Erreurs sur les personnes physiques
		{
			final String filename = "erreurs_pp.csv";
			final String contenu = genererListeErreurs(pp.getErreurs(), filename, status);
			final String titre = "Liste des personnes physiques en erreur";
			final String listVide = "(aucune)";
			document.addListeDetaillee(writer, pp.getErreurs().size(), titre, listVide, filename, contenu);
		}

		// Erreurs sur les ménages communs
		{
			final String filename = "erreurs_mc.csv";
			final String contenu = genererListeErreurs(mc.getErreurs(), filename, status);
			final String titre = "Liste des ménages communs en erreur";
			final String listVide = "(aucun)";
			document.addListeDetaillee(writer, mc.getErreurs().size(), titre, listVide, filename, contenu);
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String genererListeErreurs(List<CorrectionFlagHabitantAbstractResults.ContribuableErreur> erreurs, String filename, StatusManager status) {

		String contenu = null;
		if (erreurs != null && erreurs.size() > 0) {

			final StringBuilder builder = new StringBuilder((erreurs.size() + 1) * 100);
			builder.append("NO_CTB" + COMMA + "DESCRIPTION" + COMMA + "COMPLEMENT" + "\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableErreur> iterator = new GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableErreur>(erreurs);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final CorrectionFlagHabitantAbstractResults.ContribuableErreur erreur = iterator.next();
				builder.append(erreur.getNoCtb()).append(COMMA);
				builder.append(erreur.getMessage().getLibelle()).append(COMMA);
				if (!StringUtils.isEmpty(erreur.getComplementInfo())) {
					builder.append("\"");
					builder.append(erreur.getComplementInfo().replaceAll("[;\"]", ""));
					builder.append("\"");
				}
				if (!iterator.isLast()) {
					builder.append("\n");
				}
			}
			contenu = builder.toString();
		}
		return contenu;
	}

	private String genererListeModifications(List<CorrectionFlagHabitantAbstractResults.ContribuableInfo> modifications, String filename, StatusManager status) {

		String contenu = null;
		if (modifications != null && modifications.size() > 0) {

			final StringBuilder builder = new StringBuilder((modifications.size() + 1) * 10);
			builder.append("NO_CTB\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableInfo> iterator = new GentilIterator<CorrectionFlagHabitantAbstractResults.ContribuableInfo>(modifications);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final CorrectionFlagHabitantAbstractResults.ContribuableInfo modification = iterator.next();
				builder.append(modification.getNoCtb());
				if (!iterator.isLast()) {
					 builder.append("\n");
				}
			}

			contenu = builder.toString();
		}
		return contenu;
	}

	public DeterminerMouvementsDossiersEnMasseRapport generateRapport(final DeterminerMouvementsDossiersEnMasseResults results, StatusManager s) {

		final StatusManager status = (s == null ? new LoggingStatusManager(LOGGER) : s);

		final String nom = "RapportMouvementsDossiersMasse";
		final String description = "Rapport d'exécution du job de détermination des mouvements de dossiers en masse. Date de traitement = " + results.dateTraitement;
		final Date dateGeneration = new Date();

		try {
			return docService.newDoc(DeterminerMouvementsDossiersEnMasseRapport.class, nom, description, "pdf", new DocumentService.WriteDocCallback<DeterminerMouvementsDossiersEnMasseRapport>() {
				public void writeDoc(DeterminerMouvementsDossiersEnMasseRapport doc, OutputStream os) throws Exception {
					writePDF(results, nom, description, dateGeneration, os, status);
				}
			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void writePDF(final DeterminerMouvementsDossiersEnMasseResults results, String nom, String description, Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfDeterminerMouvementsDossiersEnMasseRapport document = new PdfDeterminerMouvementsDossiersEnMasseRapport();
		final PdfWriter writer = PdfWriter.getInstance(document, os);
		document.open();
		document.addMetaInfo(nom, description);
		document.addEnteteUnireg();

		// Titre
		document.addTitrePrincipal(String.format("Rapport de détermination des mouvements de dossiers en masse\n%s", formatTimestamp(dateGeneration)));

		// Paramètres
		document.addEntete1("Paramètres");
		{
		    document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
		        public void fillTable(PdfTableSimple table) throws DocumentException {
		            table.addLigne("Date de traitement :", RegDateHelper.dateToDisplayString(results.dateTraitement));
		        }
		    });
		}

		// Résultats
		document.addEntete1("Résultats");
		{
			if (results.isInterrompu()) {
				document.addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						+ "les valeurs ci-dessous sont donc incomplètes.");
			}

			document.addTableSimple(2, new PdfRapport.TableSimpleCallback() {
				public void fillTable(PdfTableSimple table) throws DocumentException {
					table.addLigne("Nombre de contribuables inspectés :", String.valueOf(results.getNbContribuablesInspectes()));
					table.addLigne("Nombre de contribuables ignorés :", String.valueOf(results.ignores.size()));
					table.addLigne("Nombre de mouvements créés :", String.valueOf(results.mouvements.size()));
					table.addLigne("Nombre d'erreurs :", String.valueOf(results.erreurs.size()));
					table.addLigne("Durée d'exécution :", formatDureeExecution(results));
				}
			});
		}

		// Mouvements
		{
			final String filename = "mouvements.csv";
			final String contenu = genererListeMouvements(results.mouvements, filename, status);
			final String titre = "Liste des mouvements générés";
			final String listVide = "(aucun)";
			document.addListeDetaillee(writer, results.mouvements.size(), titre, listVide, filename, contenu);
		}

		// Contribuables ignorés
		{
			final String filename = "ignores.csv";
			final String contenu = genererListeDossiersNonTraites(results.ignores, filename, status);
			final String titre = "Liste des dossiers ignorés";
			final String listVide = "(aucun)";
			document.addListeDetaillee(writer, results.ignores.size(), titre, listVide, filename, contenu);
		}

		// Erreurs
		{
			final String filename = "erreurs.csv";
			final String contenu = genererListeDossiersNonTraites(results.erreurs, filename, status);
			final String titre = "Liste des erreurs rencontrées";
			final String listVide = "(aucune)";
			document.addListeDetaillee(writer, results.erreurs.size(), titre, listVide, filename, contenu);
		}

		document.close();

		status.setMessage("Génération du rapport terminée.");
	}

	private String genererListeDossiersNonTraites(List<DeterminerMouvementsDossiersEnMasseResults.NonTraite> nonTraites, String filename, StatusManager status) {

		String contenu = null;
		if (nonTraites != null && nonTraites.size() > 0) {
			final StringBuilder b = new StringBuilder((nonTraites.size() + 1) * 50);

			b.append("NO_CTB").append(COMMA);
			b.append("RAISON").append(COMMA);
			b.append("COMPLEMENT\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final GentilIterator<DeterminerMouvementsDossiersEnMasseResults.NonTraite> iterator = new GentilIterator<DeterminerMouvementsDossiersEnMasseResults.NonTraite>(nonTraites);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final DeterminerMouvementsDossiersEnMasseResults.NonTraite nonTraite = iterator.next();
				b.append(nonTraite.noCtb).append(COMMA);
				b.append(nonTraite.getTypeInformation().replaceAll("[;\"]", "")).append(COMMA);
				if (nonTraite.complement != null) {
					b.append(nonTraite.complement.replaceAll("[;\"]", ""));
				}

				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}

	private String genererListeMouvements(List<DeterminerMouvementsDossiersEnMasseResults.Mouvement> mouvements, String filename, StatusManager status) {

		String contenu = null;
		if (mouvements != null && mouvements.size() > 0) {
			final StringBuilder b = new StringBuilder((mouvements.size() + 1) * 40);

			b.append("NO_CTB").append(COMMA);
			b.append("TYPE_MVT").append(COMMA);
			b.append("OID").append(COMMA);
			b.append("OID_DEST\n");

			final String message = String.format("Génération du fichier %s", filename);
			status.setMessage(message, 0);

			final GentilIterator<DeterminerMouvementsDossiersEnMasseResults.Mouvement> iterator = new GentilIterator<DeterminerMouvementsDossiersEnMasseResults.Mouvement>(mouvements);
			while (iterator.hasNext()) {
				if (iterator.isAtNewPercent()) {
					status.setMessage(message, iterator.getPercent());
				}

				final DeterminerMouvementsDossiersEnMasseResults.Mouvement mvt = iterator.next();
				b.append(mvt.noCtb).append(COMMA);
				b.append(mvt.getTypeInformation().replaceAll("[;\"]", "")).append(COMMA);
				b.append(mvt.oidActuel).append(COMMA);
				if (mvt instanceof DeterminerMouvementsDossiersEnMasseResults.MouvementOid) {
					b.append(((DeterminerMouvementsDossiersEnMasseResults.MouvementOid) mvt).oidDestination);
				}

				if (!iterator.isLast()) {
					b.append("\n");
				}
			}
			contenu = b.toString();
		}
		return contenu;
	}
}
