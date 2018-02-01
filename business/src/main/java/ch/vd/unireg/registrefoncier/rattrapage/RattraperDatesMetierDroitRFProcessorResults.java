package ch.vd.unireg.registrefoncier.rattrapage;

import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.SituationRF;
import ch.vd.unireg.registrefoncier.dao.ImmeubleRFDAO;

public class RattraperDatesMetierDroitRFProcessorResults extends JobResults<Long, RattraperDatesMetierDroitRFProcessorResults> {

	private final RattrapageDataSelection dataSelection;
	private final int nbThreads;
	private final ImmeubleRFDAO immeubleRFDAO;
	private final RegistreFoncierService registreFoncierService;

	private boolean interrompu = false;
	private final List<Processed> processed = new LinkedList<>();
	private final List<DebutUpdated> debutUpdated = new LinkedList<>();
	private final List<FinUpdated> finUpdated = new LinkedList<>();
	private final List<Untouched> untouched = new LinkedList<>();
	private final List<Erreur> erreurs = new LinkedList<>();

	public static final class Erreur {
		private final long immeubleId;
		private final String egrid;
		private final String message;

		public Erreur(long immeubleId, String egrid, String message) {
			this.immeubleId = immeubleId;
			this.egrid = egrid;
			this.message = message;
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public String getEgrid() {
			return egrid;
		}

		public String getMessage() {
			return message;
		}
	}

	public static final class Processed {

		private final long immeubleId;
		private final String egrid;
		private final String idRF;
		private final String commune;
		private final Integer noOfsCommune;
		private final int noParcelle;
		private final Integer index1;
		private final Integer index2;
		private final Integer index3;

		public Processed(@NotNull ImmeubleRF immeuble, @NotNull RegistreFoncierService registreFoncierService) {
			this.immeubleId = immeuble.getId();
			this.egrid = immeuble.getEgrid();
			this.idRF = immeuble.getIdRF();
			final Commune commune = registreFoncierService.getCommune(immeuble, null);
			this.commune = (commune == null ? null : commune.getNomOfficiel());
			this.noOfsCommune = (commune == null ? null : commune.getNoOFS());
			final SituationRF situation = immeuble.getSituations().stream()
					.max(new DateRangeComparator<>())
					.orElseThrow(() -> new IllegalArgumentException("L'immeuble n°" + this.immeubleId + " ne possède pas de situation valable"));
			this.noParcelle = situation.getNoParcelle();
			this.index1 = situation.getIndex1();
			this.index2 = situation.getIndex2();
			this.index3 = situation.getIndex3();
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public String getEgrid() {
			return egrid;
		}

		public String getIdRF() {
			return idRF;
		}

		public String getCommune() {
			return commune;
		}

		public Integer getNoOfsCommune() {
			return noOfsCommune;
		}

		public int getNoParcelle() {
			return noParcelle;
		}

		public Integer getIndex1() {
			return index1;
		}

		public Integer getIndex2() {
			return index2;
		}

		public Integer getIndex3() {
			return index3;
		}
	}

	public static final class DebutUpdated {

		private final long droitId;
		private final long immeubleId;
		private final String egrid;
		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final RegDate dateDebutMetierInitiale;
		private final String motifDebutInitial;
		private final RegDate dateDebutMetierCorrigee;
		private final String motifDebutCorrige;
		private final RegDate dateFinMetier;
		private final String motifFin;

		public DebutUpdated(@NotNull DroitProprieteRF droit, RegDate dateDebutMetierInitiale, String motifDebutInitial) {
			this.droitId = droit.getId();
			this.immeubleId = droit.getImmeuble().getId();
			this.egrid = droit.getImmeuble().getEgrid();
			this.dateDebut = droit.getDateDebut();
			this.dateFin = droit.getDateFin();
			this.dateDebutMetierInitiale = dateDebutMetierInitiale;
			this.motifDebutInitial = motifDebutInitial;
			this.dateDebutMetierCorrigee = droit.getDateDebutMetier();
			this.motifDebutCorrige = droit.getMotifDebut();
			this.dateFinMetier = droit.getDateFinMetier();
			this.motifFin = droit.getMotifFin();
		}

		public long getDroitId() {
			return droitId;
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public String getEgrid() {
			return egrid;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public RegDate getDateFin() {
			return dateFin;
		}

		public RegDate getDateDebutMetierInitiale() {
			return dateDebutMetierInitiale;
		}

		public String getMotifDebutInitial() {
			return motifDebutInitial;
		}

		public RegDate getDateDebutMetierCorrigee() {
			return dateDebutMetierCorrigee;
		}

		public String getMotifDebutCorrige() {
			return motifDebutCorrige;
		}

		public RegDate getDateFinMetier() {
			return dateFinMetier;
		}

		public String getMotifFin() {
			return motifFin;
		}
	}

	public static final class FinUpdated {

		private final long droitId;
		private final long immeubleId;
		private final String egrid;
		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final RegDate dateDebutMetier;
		private final String motifDebut;
		private final RegDate dateFinMetierInitiale;
		private final String motifFinInitial;
		private final RegDate dateFinMetierCorrigee;
		private final String motifFinCorrige;

		public FinUpdated(@NotNull DroitProprieteRF droit, RegDate dateFinMetierInitiale, String motifFinInitial) {
			this.droitId = droit.getId();
			this.immeubleId = droit.getImmeuble().getId();
			this.egrid = droit.getImmeuble().getEgrid();
			this.dateDebut = droit.getDateDebut();
			this.dateFin = droit.getDateFin();
			this.dateDebutMetier= droit.getDateDebutMetier();
			this.motifDebut= droit.getMotifDebut();
			this.dateFinMetierInitiale = dateFinMetierInitiale;
			this.motifFinInitial = motifFinInitial;
			this.dateFinMetierCorrigee = droit.getDateFinMetier();
			this.motifFinCorrige = droit.getMotifFin();
		}

		public long getDroitId() {
			return droitId;
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public String getEgrid() {
			return egrid;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public RegDate getDateFin() {
			return dateFin;
		}

		public RegDate getDateDebutMetier() {
			return dateDebutMetier;
		}

		public String getMotifDebut() {
			return motifDebut;
		}

		public RegDate getDateFinMetierInitiale() {
			return dateFinMetierInitiale;
		}

		public String getMotifFinInitial() {
			return motifFinInitial;
		}

		public RegDate getDateFinMetierCorrigee() {
			return dateFinMetierCorrigee;
		}

		public String getMotifFinCorrige() {
			return motifFinCorrige;
		}
	}

	public static final class Untouched {

		private final long droitId;
		private final long immeubleId;
		private final String egrid;
		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final RegDate dateDebutMetier;
		private final String motifDebut;
		private final RegDate dateFinMetier;
		private final String motifFin;

		public Untouched(@NotNull DroitProprieteRF droit) {
			this.droitId = droit.getId();
			this.immeubleId = droit.getImmeuble().getId();
			this.egrid = droit.getImmeuble().getEgrid();
			this.dateDebut = droit.getDateDebut();
			this.dateFin = droit.getDateFin();
			this.dateDebutMetier = droit.getDateDebutMetier();
			this.motifDebut = droit.getMotifDebut();
			this.dateFinMetier = droit.getDateFinMetier();
			this.motifFin = droit.getMotifFin();
		}

		public long getDroitId() {
			return droitId;
		}

		public long getImmeubleId() {
			return immeubleId;
		}

		public String getEgrid() {
			return egrid;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public RegDate getDateFin() {
			return dateFin;
		}

		public RegDate getDateDebutMetier() {
			return dateDebutMetier;
		}

		public String getMotifDebut() {
			return motifDebut;
		}

		public RegDate getDateFinMetier() {
			return dateFinMetier;
		}

		public String getMotifFin() {
			return motifFin;
		}
	}

	public RattraperDatesMetierDroitRFProcessorResults(@NotNull RattrapageDataSelection dataSelection,
	                                                   int nbThreads,
	                                                   @NotNull ImmeubleRFDAO immeubleRFDAO,
	                                                   @NotNull RegistreFoncierService registreFoncierService) {
		super(null, null);
		this.dataSelection = dataSelection;
		this.nbThreads = nbThreads;
		this.immeubleRFDAO = immeubleRFDAO;
		this.registreFoncierService = registreFoncierService;
	}

	public RattrapageDataSelection getDataSelection() {
		return dataSelection;
	}

	public int getNbThreads() {
		return nbThreads;
	}

	public boolean isInterrompu() {
		return interrompu;
	}

	public void end() {
		this.endTime = System.currentTimeMillis();
	}

	public List<Processed> getProcessed() {
		return processed;
	}

	public List<DebutUpdated> getDebutUpdated() {
		return debutUpdated;
	}

	public List<FinUpdated> getFinUpdated() {
		return finUpdated;
	}

	public List<Untouched> getUntouched() {
		return untouched;
	}

	public List<Erreur> getErreurs() {
		return erreurs;
	}

	public int getNbErreurs() {
		return erreurs.size();
	}

	public void setInterrompu(boolean interrompu) {
		this.interrompu = interrompu;
	}

	@Override
	public void addErrorException(Long id, Exception e) {
		final ImmeubleRF immeuble = immeubleRFDAO.get(id);
		if (immeuble != null) {
			erreurs.add(new Erreur(id, immeuble.getEgrid(), e.getMessage()));
		}
		else {
			erreurs.add(new Erreur(id, null, e.getMessage()));
		}
	}

	public void addProcessed(@NotNull ImmeubleRF immeuble) {
		processed.add(new Processed(immeuble, registreFoncierService));
	}

	public void addDebutUpdated(@NotNull DroitProprieteRF droit, @Nullable RegDate dateDebutMetierInitiale, @Nullable String motifDebutInitial) {
		debutUpdated.add(new DebutUpdated(droit, dateDebutMetierInitiale, motifDebutInitial));
	}

	public void addFinUpdated(@NotNull DroitProprieteRF droit, @Nullable RegDate dateFinMetierInitiale, @Nullable String motifFinInitial) {
		finUpdated.add(new FinUpdated(droit, dateFinMetierInitiale, motifFinInitial));
	}

	public void addUntouched(@NotNull DroitProprieteRF droit) {
		untouched.add(new Untouched(droit));
	}

	@Override
	public void addAll(RattraperDatesMetierDroitRFProcessorResults right) {
		processed.addAll(right.processed);
		debutUpdated.addAll(right.debutUpdated);
		finUpdated.addAll(right.finUpdated);
		untouched.addAll(right.untouched);
		erreurs.addAll(right.erreurs);
	}
}
