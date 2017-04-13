package ch.vd.uniregctb.rapport;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.CsvHelper;
import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.common.TemporaryFile;
import ch.vd.uniregctb.evenement.externe.EtatEvenementExterne;
import ch.vd.uniregctb.evenement.identification.contribuable.IdentificationContribuable;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.stats.evenements.StatistiqueEvenementInfo;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsOrganisationsResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsCivilsPersonnesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsExternesResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsIdentificationContribuableResults;
import ch.vd.uniregctb.stats.evenements.StatsEvenementsNotairesResults;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Rapport des statistiques des événements reçus par Unireg
 */
public class PdfStatistiquesEvenementsRapport extends PdfRapport {

	/**
	 * Renvoie la représentation décimale d'un entier, ou de la valeur indiquée comme valeur par défaut si cet entier est <code>null</code>
	 * @param value valeur à représenter
	 * @param defaut valeur à utiliser si <code>value<code> est <code>null</code>
	 * @return représentation décimale de l'entier
	 */
	private static String toStringInt(Integer value, int defaut) {
		return Integer.toString(value != null ? value : defaut);
	}

	public void write(final StatsEvenementsCivilsPersonnesResults civilsPersonnes,
	                  final StatsEvenementsCivilsOrganisationsResults civilsOrganisations,
	                  final StatsEvenementsExternesResults externes,
	                  final StatsEvenementsIdentificationContribuableResults identCtb,
	                  final StatsEvenementsNotairesResults notaires,
	                  final RegDate dateReference, String nom, String description, final Date dateGeneration, OutputStream os,
	                  StatusManager status) throws Exception {

		Assert.notNull(status);

		// Création du document PDF
		PdfWriter writer = PdfWriter.getInstance(this, os);
		open();
		addMetaInfo(nom, description);
		addEnteteUnireg();

		// Titre
		addTitrePrincipal("Statistiques des événements");

		// Paramètres
		addEntete1("Paramètres");
		{
			addTableSimple(2, table -> {
				table.addLigne("Evénements civils (personnes):", String.valueOf(civilsPersonnes != null));
				table.addLigne("Evénements civils (organisations):", String.valueOf(civilsOrganisations != null));
				table.addLigne("Evénements externes:", String.valueOf(externes != null));
				table.addLigne("Evénements identification:", String.valueOf(identCtb != null));
				table.addLigne("Evénements notaires:", String.valueOf(notaires != null));
				table.addLigne("Date de référence:", RegDateHelper.dateToDisplayString(dateReference));
				table.addLigne("Date de génération du rapport:", formatTimestamp(dateGeneration));
			});
		}

		if (civilsPersonnes != null) {

			newPage();
			addTitrePrincipal("Evénements civils (personnes)");

			// Evénements civils : états
			addEntete1("Répartition par état");
			{
				addTableSimple(3, table -> {

					table.addLigne("Etat", "Total", "Reçus depuis " + RegDateHelper.dateToDisplayString(dateReference));
					table.setHeaderRows(1);

					final Map<EtatEvenementCivil, Integer> etats = civilsPersonnes.getEtats();
					final Map<EtatEvenementCivil, Integer> etatsNouveaux = civilsPersonnes.getEtatsNouveaux();
					for (EtatEvenementCivil etat : EtatEvenementCivil.values()) {
						final Integer nombre = etats.get(etat);
						final Integer nombreNouveaux = etatsNouveaux.get(etat);

						final String total = toStringInt(nombre, 0);
						final String totalNouveaux = toStringInt(nombreNouveaux, 0);
						table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
					}
				});
			}

			// événements civils : types en erreur
			{
				final String filename = "erreurs_evts_civils_personnes_par_type.csv";
				final String titre = "Erreurs des événements civils (personnes) par type d'événement";
				final String listVide = "(aucune)";
				try (TemporaryFile contenu = asCsvFile(civilsPersonnes.getErreursParType(), civilsPersonnes.getErreursParTypeNouveaux(), "RECUS", dateReference, filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// messages d'erreur regroupés par type
			{
				final String filename = "messages_erreurs_evts_civils_personnes_par_type.csv";
				final String titre = "Messages d'erreurs des événements civils (personnes) par type d'événement";
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = buildStatsMessagesErreursEchParType(civilsPersonnes.getToutesErreurs(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// toutes les erreurs
			{
				final String filename = "erreurs_evts_civils_personnes.csv";
				final String titre = "Erreurs des événements civils (personnes)";
				final String listVide = "(aucune)";
				try (TemporaryFile contenu = asCsvStatFile(civilsPersonnes.getToutesErreurs(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// manipulations manuelles
			{
				final String filename = "manipulations_evts_civils_personnes.csv";
				final String titre = String.format("Manipulations manuelles des événements civils (personnes) depuis le %s", RegDateHelper.dateToDisplayString(dateReference));
				final String listVide = "(aucune)";
				try (TemporaryFile contenu = asCsvStatFile(civilsPersonnes.getManipulationsManuelles(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// forçages par type
			{
				final String filename = "evts_civils_forces_personnes.csv";
				final String titre = "Evénements civils (personnes) forcés par type d'événement";
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = asCsvFile(civilsPersonnes.getForcesParType(), civilsPersonnes.getForcesRecemmentParType(), "FORCES", dateReference, filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// taille des queues d'attente
			{
				final String filename = "queues_attente_personnes.csv";
				final String titre = "Queues d'événements (personnes) en attente";
				final String listeVide = "(aucune)";
				try (TemporaryFile contenu = asCsvStatFile(civilsPersonnes.getQueuesAttente(), filename, status)) {
					addListeDetaillee(writer, titre, listeVide, filename, contenu);
				}
			}
		}

		if (civilsOrganisations != null) {

			newPage();
			addTitrePrincipal("Evénements civils (organisations)");

			// Evénements civils : états
			addEntete1("Répartition par état");
			{
				addTableSimple(3, table -> {

					table.addLigne("Etat", "Total", "Reçus depuis " + RegDateHelper.dateToDisplayString(dateReference));
					table.setHeaderRows(1);

					final Map<EtatEvenementOrganisation, Integer> etats = civilsOrganisations.getEtats();
					final Map<EtatEvenementOrganisation, Integer> etatsNouveaux = civilsOrganisations.getEtatsNouveaux();
					for (EtatEvenementOrganisation etat : EtatEvenementOrganisation.values()) {
						final Integer nombre = etats.get(etat);
						final Integer nombreNouveaux = etatsNouveaux.get(etat);

						final String total = toStringInt(nombre, 0);
						final String totalNouveaux = toStringInt(nombreNouveaux, 0);
						table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
					}
				});
			}

			// erreurs
			{
				final String filename = "erreurs_organisations.csv";
				final String titre = "Erreurs des événements civils (organisations)";
				final String listeVide = "(aucune)";
				try (TemporaryFile contenu = asCsvStatFile(civilsOrganisations.getErreurs(), filename, status)) {
					addListeDetaillee(writer, titre, listeVide, filename, contenu);
				}
			}

			// événements en souffrance depuis plus de 15 jours
			{
				final String filename = "en_souffrance_organisations.csv";
				final String titre = "Evénements civils (organisations) reçus il y a plus de 15 jours et encore en souffrance";
				final String listeVide = "(aucun)";
				try (TemporaryFile contenu = asCsvStatFile(civilsOrganisations.getEnSouffrance(), filename, status)) {
					addListeDetaillee(writer, titre, listeVide, filename, contenu);
				}
			}

			// mutations traitées
			{
				final String filename = "mutations_traitees_organisations.csv";
				final String titre = "Statistique des mutations traitées (organisations)";
				final String listeVide = "(aucune)";
				try (TemporaryFile contenu = asCsvFile(civilsOrganisations.getMutationsTraitees(),
				                                       civilsOrganisations.getMutationsRecentesTraitees(),
				                                       filename,
				                                       dateReference,
				                                       status)) {
					addListeDetaillee(writer, titre, listeVide, filename, contenu);
				}
			}

			// détail des mutations traitées
			{
				final String filename = "details_mutations_traitees_organisations.csv";
				final String titre = "Détails des mutations traitées (organisations) depuis le " + RegDateHelper.dateToDisplayString(dateReference);
				final String listeVide = "(aucune)";
				try (TemporaryFile contenu = asCsvStatFile(civilsOrganisations.getDetailsMutationsTraiteesRecentes(), filename, status)) {
					addListeDetaillee(writer, titre, listeVide, filename, contenu);
				}
			}
		}

		// événements externes
		if (externes != null) {
			
			newPage();
			addTitrePrincipal("Evénements externes");

			// événements externes : états
			addEntete1("Répartition par état");
			{
				addTableSimple(2, table -> {

					table.addLigne("Etat", "Total");
					table.setHeaderRows(1);

					final Map<EtatEvenementExterne, Integer> etats = externes.getEtats();
					for (EtatEvenementExterne etat : EtatEvenementExterne.values()) {
						final Integer nombre = etats.get(etat);
						table.addLigne(etat.name(), toStringInt(nombre, 0));
					}
				});
			}

			// événements externes : erreurs
			{
				final String filename = "erreurs_evts_externes.csv";
				final String titre = "Erreurs des événements externes";
				final String listVide = "(aucune)";
				try (TemporaryFile contenu = asCsvStatFile(externes.getErreurs(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}
		}

		// événements d'identification de contribuable
		if (identCtb != null) {

			newPage();
			addTitrePrincipal("Demandes d'identification de contribuable");

			// événements d'identification de contribuable : états
			addEntete1("Répartition par état");
			{
				addTableSimple(new float[] {60f, 20f, 20f}, table -> {

					table.addLigne("Etat", "Total", "Reçus depuis " + RegDateHelper.dateToDisplayString(dateReference));
					table.setHeaderRows(1);

					final Map<IdentificationContribuable.Etat, Integer> etats = identCtb.getEtats();
					final Map<IdentificationContribuable.Etat, Integer> etatsNouveaux = identCtb.getEtatsNouveaux();
					for (IdentificationContribuable.Etat etat : IdentificationContribuable.Etat.values()) {
						final Integer nombre = etats.get(etat);
						final Integer nombreNouveaux = etatsNouveaux.get(etat);

						final String total = toStringInt(nombre, 0);
						final String totalNouveaux = toStringInt(nombreNouveaux, 0);
						table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
					}
				});
			}

			// événements d'identification de contribuable : restant à traiter
			{
				final String filename = "identification_ctb_a_traiter.csv";
				final String titre = "Evénements d'identification à traiter";
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = asCsvStatFile(identCtb.getATraiter(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}
		}

		// événements notaires
		if (notaires != null) {

			newPage();
			addTitrePrincipal("Evénements \"Notaires\" (ReqDes)");

			// événements ReqDes : états
			addEntete1("Répartition par état");
			{
				addTableSimple(new float[] {60f, 20f, 20f}, table -> {

					table.addLigne("Etat", "Total", "Reçus depuis " + RegDateHelper.dateToDisplayString(dateReference));
					table.setHeaderRows(1);

					final Map<EtatTraitement, Integer> etats = notaires.getEtats();
					final Map<EtatTraitement, Integer> etatsNouveaux = notaires.getEtatsNouveaux();
					for (EtatTraitement etat : EtatTraitement.values()) {
						final Integer nombre = etats.get(etat);
						final Integer nombreNouveaux = etatsNouveaux.get(etat);

						final String total = toStringInt(nombre, 0);
						final String totalNouveaux = toStringInt(nombreNouveaux, 0);
						table.addLigne(String.format("%s", etat.toString()), total, totalNouveaux);
					}
				});
			}

			// événements ReqDes : erreurs
			{
				final String filename = "reqdes_erreurs.csv";
				final String titre = "Evénements ReqDes en erreur";
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = asCsvStatFile(notaires.getToutesErreurs(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}

			// événements ReqDes : manipulations manuelles
			{
				final String filename = "reqdes_forces.csv";
				final String titre = "Evénements ReqDes forcés";
				final String listVide = "(aucun)";
				try (TemporaryFile contenu = asCsvStatFile(notaires.getManipulationsManuelles(), filename, status)) {
					addListeDetaillee(writer, titre, listVide, filename, contenu);
				}
			}
		}

		close();

		status.setMessage("Génération du rapport terminée.");
	}

	private static String asCsvLine(String[] elements, boolean withNewLine) {
		final StringBuilder b = new StringBuilder();
		for (int i = 0 ; i < elements.length ; ++ i) {
			if (i > 0) {
				b.append(COMMA);
			}
			final String string = escapeChars(elements[i]);
			final boolean needsQuote = string.contains("\n");
			if (needsQuote) {
				b.append('\"');
			}
			b.append(string);
			if (needsQuote) {
				b.append('\"');
			}
		}
		if (withNewLine) {
			b.append('\n');
		}
		return b.toString();
	}

	private static <T extends StatistiqueEvenementInfo> TemporaryFile asCsvStatFile(List<T> elements, String fileName, StatusManager statusManager) {
		TemporaryFile contenu = null;
		if (elements != null && !elements.isEmpty()) {
			final T first = elements.get(0);
			contenu = CsvHelper.asCsvTemporaryFile(elements, fileName, statusManager, new CsvHelper.FileFiller<T>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append(asCsvLine(first.getNomsColonnes(), false));
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, T elt) {
					final String[] valeurs = elt.getValeursColonnes();
					b.append(asCsvLine(valeurs, false));
					return true;
				}
			});
		}
		return contenu;
	}

	private static TemporaryFile asCsvFile(Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> tous, @Nullable final Map<Pair<TypeEvenementCivilEch, ActionEvenementCivilEch>, Integer> nouveaux, @Nullable final String prefixeNouveaux, final RegDate dateReference, String fileName, StatusManager statusManager) throws IOException {
		TemporaryFile contenu = null;
		if (tous != null && !tous.isEmpty()) {
			final class Data {
				final TypeEvenementCivilEch type;
				final ActionEvenementCivilEch action;
				final int total;
				final int marginal;

				Data(TypeEvenementCivilEch type, ActionEvenementCivilEch action, int total, int marginal) {
					this.type = type;
					this.action = action;
					this.total = total;
					this.marginal = marginal;
				}
			}
			final List<Data> list = new LinkedList<>();
			for (TypeEvenementCivilEch type : TypeEvenementCivilEch.values()) {
				for (ActionEvenementCivilEch action : ActionEvenementCivilEch.values()) {
					final Pair<TypeEvenementCivilEch, ActionEvenementCivilEch> key = Pair.of(type, action);
					final Integer total = tous.get(key);
					final Integer marginal = nouveaux != null ? nouveaux.get(key) : null;
					if ((total != null && total > 0) || (marginal != null && marginal > 0)) {
						list.add(new Data(type, action, total != null ? total : 0, marginal != null ? marginal : 0));
					}
				}
			}

			contenu = CsvHelper.asCsvTemporaryFile(list, fileName, statusManager, new CsvHelper.FileFiller<Data>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("TYPE").append(COMMA).append("ACTION").append(COMMA).append("TOTAL");
					if (nouveaux != null) {
						b.append(COMMA).append(prefixeNouveaux).append("_DEPUIS_").append(RegDateHelper.dateToDisplayString(dateReference));
					}
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Data elt) {
					b.append(elt.type).append(COMMA).append(elt.action).append(COMMA).append(elt.total);
					if (nouveaux != null) {
						b.append(COMMA).append(elt.marginal);
					}
					return true;
				}
			});
		}
		return contenu;
	}

	protected static final class EvtCivilEchMsgTypeKey {
		public final String msg;
		public final TypeEvenementCivilEch type;
		public final ActionEvenementCivilEch action;

		protected EvtCivilEchMsgTypeKey(String msg, TypeEvenementCivilEch type, ActionEvenementCivilEch action) {
			this.msg = msg.replaceAll("[0-9]+(\\.[0-9]+)*", "?").replaceAll("\\?(, \\?)+", "?");        // pour enlever toutes les différences sur des dates ou des numéros d'individu, de tiers...
			this.type = type;
			this.action = action;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final EvtCivilEchMsgTypeKey that = (EvtCivilEchMsgTypeKey) o;

			if (action != that.action) return false;
			if (msg != null ? !msg.equals(that.msg) : that.msg != null) return false;
			if (type != that.type) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = msg != null ? msg.hashCode() : 0;
			result = 31 * result + (type != null ? type.hashCode() : 0);
			result = 31 * result + (action != null ? action.hashCode() : 0);
			return result;
		}
	}

	private TemporaryFile buildStatsMessagesErreursEchParType(List<StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo> toutesErreurs, String fileName, StatusManager statusManager) {
		TemporaryFile contenu = null;
		if (toutesErreurs != null && !toutesErreurs.isEmpty()) {

			final String messageCalcul = String.format("Calcul des statistiques pour le fichier %s", fileName);
			statusManager.setMessage(messageCalcul, 0);

			// première partie : calcul des statistiques
			final Map<EvtCivilEchMsgTypeKey, MutableInt> map = new HashMap<>();
			final GentilIterator<StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo> iter = new GentilIterator<>(toutesErreurs);
			while (iter.hasNext()) {
				if (iter.isAtNewPercent()) {
					statusManager.setMessage(messageCalcul, iter.getPercent());
				}

				final StatsEvenementsCivilsPersonnesResults.EvenementCivilEnErreurInfo erreur = iter.next();
				if (erreur != null && erreur.etat == EtatEvenementCivil.EN_ERREUR) {
					final EvtCivilEchMsgTypeKey key = new EvtCivilEchMsgTypeKey(erreur.message, erreur.type, erreur.action);
					final MutableInt nb = map.get(key);
					if (nb == null) {
						map.put(key, new MutableInt(1));
					}
					else {
						nb.increment();
					}
				}
			}

			// tri des lignes dans l'ordre décroissant des nombres d'occurrence
			final List<Map.Entry<EvtCivilEchMsgTypeKey, MutableInt>> stats = new ArrayList<>(map.entrySet());
			Collections.sort(stats, Comparator.comparingInt(entry -> entry.getValue().intValue()));

			// touche finale : remplissage de la chaîne de caractères qui finira dans le fichier CSV
			contenu = CsvHelper.asCsvTemporaryFile(stats, fileName, statusManager, new CsvHelper.FileFiller<Map.Entry<EvtCivilEchMsgTypeKey, MutableInt>>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("MESSAGE").append(COMMA);
					b.append("TYPE_EVT").append(COMMA);
					b.append("ACTION").append(COMMA);
					b.append("COUNT");
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, Map.Entry<EvtCivilEchMsgTypeKey, MutableInt> stat) {
					final EvtCivilEchMsgTypeKey key = stat.getKey();
					b.append(asCsvField(key.msg)).append(COMMA);
					b.append(key.type).append(COMMA);
					b.append(key.action).append(COMMA);
					b.append(stat.getValue().intValue());
					return true;
				}
			});
		}
		return contenu;
	}

	private TemporaryFile asCsvFile(final Map<StatsEvenementsCivilsOrganisationsResults.MutationsTraiteesStatsKey, Integer> statsGlobales,
	                                final Map<StatsEvenementsCivilsOrganisationsResults.MutationsTraiteesStatsKey, Integer> statsRecentes,
	                                String fileName,
	                                final RegDate dateDebutActivite,
	                                StatusManager statusManager) {

		TemporaryFile contenu = null;
		if (!statsGlobales.isEmpty() || !statsRecentes.isEmpty()) {

			statusManager.setMessage("Génération du fichier " + fileName);

			// récupération des clés disponibles
			final Set<StatsEvenementsCivilsOrganisationsResults.MutationsTraiteesStatsKey> keys = new LinkedHashSet<>();
			keys.addAll(statsGlobales.keySet());
			keys.addAll(statsRecentes.keySet());

			// remplissage des fichiers
			contenu = CsvHelper.asCsvTemporaryFile(keys, fileName, statusManager, new CsvHelper.FileFiller<StatsEvenementsCivilsOrganisationsResults.MutationsTraiteesStatsKey>() {
				@Override
				public void fillHeader(CsvHelper.LineFiller b) {
					b.append("TYPE_MUTATION").append(COMMA);
					b.append("ETAT_FINAL_EVT").append(COMMA);
					b.append("TOTAL").append(COMMA);
					b.append("TOTAL_DEPUIS_").append(RegDateHelper.dateToDisplayString(dateDebutActivite));
				}

				@Override
				public boolean fillLine(CsvHelper.LineFiller b, StatsEvenementsCivilsOrganisationsResults.MutationsTraiteesStatsKey key) {
					final Integer global = statsGlobales.get(key);
					final Integer recent = statsRecentes.get(key);
					b.append(asCsvField(key.getDescription())).append(COMMA);
					b.append(key.getEtat()).append(COMMA);
					b.append(global != null ? global : 0).append(COMMA);
					b.append(recent != null ? recent : 0);
					return true;
				}
			});
		}
		return contenu;
	}
}
