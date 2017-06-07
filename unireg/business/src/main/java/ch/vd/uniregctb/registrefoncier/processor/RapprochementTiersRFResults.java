package ch.vd.uniregctb.registrefoncier.processor;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.JobResults;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class RapprochementTiersRFResults extends JobResults<Long, RapprochementTiersRFResults> {

	private static final String ERREUR_TIERS_IDENTIFIE_NON_CONTRIBUABLE = "Le tiers identifié n'est pas un contribuable.";
	private static final String ERREUR_TIERS_RF_DEJA_RAPPROCHE_A_DATE_DE_TRAITEMENT = "Le tiers RF possède déjà un rapprochement valide à la date de traitement.";

	private boolean interrompu = false;
	private final int nbThreads;
	private final List<NouveauRapprochement> nouveauxRapprochements = new LinkedList<>();
	private final List<ErreurRapprochement> erreurs = new LinkedList<>();
	private final List<NonIdentification> nonIdentifications = new LinkedList<>();

	public static final class NouveauRapprochement {
		public final long idTiersRF;
		public final long noRF;
		public final long idContribuable;
		public final TypeRapprochementRF type;

		public NouveauRapprochement(RapprochementRF rapprochement) {
			this.idTiersRF = rapprochement.getTiersRF().getId();
			this.noRF = rapprochement.getTiersRF().getNoRF();
			this.idContribuable = rapprochement.getContribuable().getNumero();
			this.type = rapprochement.getTypeRapprochement();
		}

		@Override
		public String toString() {
			return "NouveauRapprochement{" +
					"idTiersRF=" + idTiersRF +
					", noRF=" + noRF +
					", idContribuable=" + idContribuable +
					", type=" + type +
					'}';
		}
	}

	public static abstract class NonRapprochement {
		public final long idTiersRF;
		public final Long idContribuable;

		// données du RF
		public final Long noRF;
		public final String nomRaisonSocialeRF;
		public final String prenomRF;
		public final RegDate dateNaissanceRF;
		public final Long noContribuableRF;

		public NonRapprochement(TiersRF tiersRF, Long idContribuable) {
			this.idTiersRF = tiersRF.getId();
			this.idContribuable = idContribuable;

			this.noContribuableRF = tiersRF.getNoContribuable();
			this.noRF = tiersRF.getNoRF();

			if (tiersRF instanceof PersonnePhysiqueRF) {
				final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) tiersRF;
				this.nomRaisonSocialeRF = pp.getNom();
				this.prenomRF = pp.getPrenom();
				this.dateNaissanceRF = pp.getDateNaissance();
			}
			else if (tiersRF instanceof PersonneMoraleRF) {
				final PersonneMoraleRF pm = (PersonneMoraleRF) tiersRF;
				this.nomRaisonSocialeRF = pm.getRaisonSociale();
				this.prenomRF = null;
				this.dateNaissanceRF = null;
			}
			else if (tiersRF instanceof CollectivitePubliqueRF) {
				final CollectivitePubliqueRF coll = (CollectivitePubliqueRF) tiersRF;
				this.nomRaisonSocialeRF = coll.getRaisonSociale();
				this.prenomRF = null;
				this.dateNaissanceRF = null;
			}
			else {
				this.nomRaisonSocialeRF = null;
				this.prenomRF = null;
				this.dateNaissanceRF = null;
			}
		}

		public NonRapprochement(long idTiersRF) {
			this.idTiersRF = idTiersRF;
			this.idContribuable = null;
			this.noContribuableRF = null;
			this.noRF = null;
			this.nomRaisonSocialeRF = null;
			this.prenomRF = null;
			this.dateNaissanceRF = null;
		}

		@Override
		public String toString() {
			return String.format("%s{%s}", getClass().getSimpleName(), getDataString());
		}

		protected String getDataString() {
			return String.format("idTiersRF=%d, noRF=%s, noContribuableRF=%s, nomRaisonSocialeRF=%s, prenomRF=%s, dateNaissanceRF=%s",
			                     idTiersRF,
			                     toString(noRF, StringRenderer.DEFAULT),
			                     toString(noContribuableRF, StringRenderer.DEFAULT),
			                     toString(nomRaisonSocialeRF, NonRapprochement::quote),
			                     toString(prenomRF, NonRapprochement::quote),
			                     toString(dateNaissanceRF, RegDateHelper::dateToDisplayString));
		}

		protected static <T> String toString(T obj, StringRenderer<? super T> renderer) {
			return Optional.ofNullable(obj)
					.map(renderer::toString)
					.orElse(StringUtils.EMPTY);
		}

		protected static String quote(String str) {
			return Optional.ofNullable(str)
					.map(s -> "'" + s + "'")
					.orElse(StringUtils.EMPTY);
		}
	}

	public static final class ErreurRapprochement extends NonRapprochement {
		public final String message;

		public ErreurRapprochement(TiersRF tiersRF, Long idContribuable, String message) {
			super(tiersRF, idContribuable);
			this.message = message;
		}

		public ErreurRapprochement(long idTiersRF, String message) {
			super(idTiersRF);
			this.message = message;
		}

		@Override
		protected String getDataString() {
			return String.format("%s, message=%s",
			                     super.getDataString(),
			                     quote(message));
		}
	}

	public static final class NonIdentification extends NonRapprochement {
		public final List<Long> candidats;

		public NonIdentification(TiersRF tiersRF, List<Long> candidats) {
			super(tiersRF, null);
			this.candidats = candidats;
		}

		@Override
		protected String getDataString() {
			final StringRenderer<Long> renderer = l -> Optional.ofNullable(l).map(String::valueOf).orElse("...");
			return String.format("%s, candidats=%s",
			                     super.getDataString(),
		                         toString(candidats, lst -> CollectionsUtils.toString(lst, renderer, "/")));
		}
	}

	public RapprochementTiersRFResults(int nbThreads, TiersService tiersService, AdresseService adresseService) {
		super(tiersService, adresseService);
		this.nbThreads = nbThreads;
	}

	public List<NouveauRapprochement> getNouveauxRapprochements() {
		return nouveauxRapprochements;
	}

	public List<ErreurRapprochement> getErreurs() {
		return erreurs;
	}

	public List<NonIdentification> getNonIdentifications() {
		return nonIdentifications;
	}

	public int getNbDossiersInspectes() {
		return getNbIdentifications() + getNbNonIdentifications() + getNbErreurs();
	}

	public int getNbIdentifications() {
		return nouveauxRapprochements.size();
	}

	public int getNbNonIdentifications() {
		return nonIdentifications.size();
	}

	public int getNbErreurs() {
		return erreurs.size();
	}

	@Override
	public void addAll(RapprochementTiersRFResults right) {
		nouveauxRapprochements.addAll(right.nouveauxRapprochements);
		erreurs.addAll(right.erreurs);
		nonIdentifications.addAll(right.nonIdentifications);
	}

	@Override
	public void end() {
		Collections.sort(nouveauxRapprochements, Comparator.comparingLong(r -> r.noRF));
		Collections.sort(nonIdentifications, Comparator.comparingLong(r -> r.noRF));
		Collections.sort(erreurs, Comparator.comparingLong(e -> e.idTiersRF));
		super.end();
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	@Override
	public void addErrorException(Long tiersRF, Exception e) {
		erreurs.add(new ErreurRapprochement(tiersRF, String.format("Exception levée : %s\n%s", e.getMessage(), ExceptionUtils.extractCallStack(e))));
	}

	public void addTiersNonIdentifie(TiersRF tiersRF, List<Long> candidats) {
		nonIdentifications.add(new NonIdentification(tiersRF, candidats));
	}

	public void addErrorTiersIdentifiePasContribuable(TiersRF tiersRF, long idTiersUnireg) {
		erreurs.add(new ErreurRapprochement(tiersRF, idTiersUnireg, ERREUR_TIERS_IDENTIFIE_NON_CONTRIBUABLE));
	}

	public void addErrorRapprochementDejaPresentADateTraitement(TiersRF tiersRF) {
		erreurs.add(new ErreurRapprochement(tiersRF, null, ERREUR_TIERS_RF_DEJA_RAPPROCHE_A_DATE_DE_TRAITEMENT));
	}

	public void addNouveauRapprochement(RapprochementRF rapprochement) {
		nouveauxRapprochements.add(new NouveauRapprochement(rapprochement));
	}
}
