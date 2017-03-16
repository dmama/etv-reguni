package ch.vd.uniregctb.rapport;

import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.foncier.InitialisationIFoncResults;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;

/**
 * Rapport PDF contenant le rapport de l'extraction des données nécessaires à l'initialisation de la taxation IFONC
 */
public class PdfInitialisationIFoncRapport extends PdfRapport {

	public void write(final InitialisationIFoncResults results, final String nom, final String description, final Date dateGeneration, OutputStream os, StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		final PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Rapport d'exécution du batch d'extraction des données nécessaires à l'initialisation de la taxation IFONC.");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Date de référence :", RegDateHelper.dateToDisplayString(results.dateReference));
				table.addLigne("Nombre de threads :", String.valueOf(results.nbThreads));
			});
		}

		// Résultats
		addEntete1("Résultats");
		{
			if (status.interrupted()) {
				addWarning("Attention ! Le job a été interrompu par l'utilisateur,\n"
						           + "les valeurs ci-dessous sont donc incomplètes.");
			}

			addTableSimple(new float[]{.6f, .4f}, table -> {
				table.addLigne("Nombre d'immeubles inspectés :", String.valueOf(results.getNbImmeublesInspectes()));
				table.addLigne("Nombre d'erreurs :", String.valueOf(results.getErreurs().size()));
				table.addLigne("Nombre de droits extraits :", String.valueOf(results.getDroits().size()));
				table.addLigne("Durée d'exécution du job :", formatDureeExecution(results.endTime - results.startTime));
				table.addLigne("Date de génération : ", formatTimestamp(dateGeneration));
			});
		}

		// Droits extraits
		{
			String filename = "droits.csv";
			String titre = "Liste des droits extraits";
			String listVide = "(aucun)";
			try (TemporaryFile contenu = processedAsCsvFile(results.getDroits(), filename, status)) {
				addListeDetaillee(writer, titre, listVide, filename, contenu);
			}
		}

		// Cas en erreur
		{
			final String filename = "erreurs.csv";
			final String titre = " Liste des erreurs";
			final String listeVide = "(aucune)";
			try (TemporaryFile contenu = errorsAsCsvFile(results.getErreurs(), filename, status)) {
				addListeDetaillee(writer, titre, listeVide, filename, contenu);
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static TemporaryFile processedAsCsvFile(List<InitialisationIFoncResults.InfoDroit> droits, String filename, StatusManager status) {
		return CsvHelper.asCsvTemporaryFile(droits, filename, status, new CsvHelper.FileFiller<InitialisationIFoncResults.InfoDroit>() {
			@Override
			public void fillHeader(CsvHelper.LineFiller b) {
				b.append("CTB_ID").append(COMMA);
				b.append("NOM").append(COMMA);
				b.append("PRENOM").append(COMMA);
				b.append("RAISON_SOCIALE").append(COMMA);
				b.append("TYPE_AYANT_DROIT").append(COMMA);
				b.append("COMMUNAUTE_ID").append(COMMA);
				b.append("TYPE_DROIT").append(COMMA);
				b.append("REGIME_PROPRIETE").append(COMMA);
				b.append("MOTIF_DEBUT").append(COMMA);
				b.append("DATE_DEBUT").append(COMMA);
				b.append("MOTIF_FIN").append(COMMA);
				b.append("DATE_FIN").append(COMMA);
				b.append("PART_PROPRIETE_NUMERATEUR").append(COMMA);
				b.append("PART_PROPRIETE_DENOMINATEUR").append(COMMA);
				b.append("EGRID").append(COMMA);
				b.append("NO_PARCELLE").append(COMMA);
				b.append("INDEX1").append(COMMA);
				b.append("INDEX2").append(COMMA);
				b.append("INDEX3").append(COMMA);
				b.append("OFS_COMMUNE").append(COMMA);
				b.append("NOM_COMMUNE");
			}

			@Override
			public boolean fillLine(CsvHelper.LineFiller b, InitialisationIFoncResults.InfoDroit droit) {
				b.append(Optional.ofNullable(droit.idContribuable).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.identificationRF).map(identif -> identif.nom).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.identificationRF).map(identif -> identif.prenom).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.identificationRF).map(identif -> identif.raisonSociale).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.classAyantDroit).map(AYANT_DROIT_DISPLAY_STRINGS::get).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.idCommunaute).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.classDroit).map(DROIT_DISPLAY_STRING::get).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.regime).map(Enum::name).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.motifDebut).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(droit.dateDebut).append(COMMA);
				b.append(Optional.ofNullable(droit.motifFin).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(droit.dateFin).append(COMMA);
				b.append(Optional.ofNullable(droit.part).map(Fraction::getNumerateur).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.part).map(Fraction::getDenominateur).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.egrid).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.noParcelle).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.index1).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.index2).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.index3).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.noOfsCommune).map(String::valueOf).orElse(StringUtils.EMPTY)).append(COMMA);
				b.append(Optional.ofNullable(droit.nomCommune).map(CsvHelper::escapeChars).orElse(StringUtils.EMPTY));
				return true;
			}
		});
	}

	private static final Map<Class<? extends AyantDroitRF>, String> AYANT_DROIT_DISPLAY_STRINGS = buildTypeAyantDroitDisplayStrings();

	private static Map<Class<? extends AyantDroitRF>, String> buildTypeAyantDroitDisplayStrings() {
		final Map<Class<? extends AyantDroitRF>, String> map = new HashMap<>();
		map.put(CommunauteRF.class, "Communauté");
		map.put(PersonnePhysiqueRF.class, "Personne physique");
		map.put(PersonneMoraleRF.class, "Personne morale");
		map.put(CollectivitePubliqueRF.class, "Collectivité publique");
		return map;
	}

	private static final Map<Class<? extends DroitRF>, String> DROIT_DISPLAY_STRING = buildTypeDroitDisplayStrings();

	private static Map<Class<? extends DroitRF>, String> buildTypeDroitDisplayStrings() {
		final Map<Class<? extends DroitRF>, String> map = new HashMap<>();
		map.put(DroitHabitationRF.class, "Droit d'habitation");
		map.put(DroitProprieteCommunauteRF.class, "Propriété");
		map.put(DroitProprietePersonneMoraleRF.class, "Propriété");
		map.put(DroitProprietePersonnePhysiqueRF.class, "Propriété");
		map.put(UsufruitRF.class, "Usufruit");
		return map;
	}

	private TemporaryFile errorsAsCsvFile(List<InitialisationIFoncResults.ErreurDroit> liste, String filename, StatusManager status) {
		TemporaryFile contenu = null;
		if (!liste.isEmpty()) {
			contenu = CsvHelper.asCsvTemporaryFile(liste, filename, status, new CsvHelper.FileFiller<InitialisationIFoncResults.ErreurDroit>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("IMMEUBLE_ID").append(COMMA);
					b.append("MESSAGE").append(COMMA);
					b.append("STACK_TRACE");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, InitialisationIFoncResults.ErreurDroit elt) {
					b.append(elt.idImmeuble).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.message)).append(COMMA);
					b.append(CsvHelper.asCsvField(elt.stackTrace));
					return true;
				}
			});
		}
		return contenu;
	}
}