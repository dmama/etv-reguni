package ch.vd.unireg.registrefoncier.dataimport;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.JobResults;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutation;
import ch.vd.unireg.evenement.registrefoncier.EvenementRFMutationDAO;
import ch.vd.unireg.evenement.registrefoncier.TypeEntiteRF;
import ch.vd.unireg.evenement.registrefoncier.TypeMutationRF;

public class MutationsRFProcessorResults extends JobResults<Long, MutationsRFProcessorResults> {

	private final long importId;
	private final boolean importInitial;
	private final RegDate dateValeur;
	private final int nbThreads;
	private final EvenementRFMutationDAO evenementRFMutationDAO;

	private boolean interrompu = false;
	private final Map<ProcessedKey, MutableLong> processed = new HashMap<>();
	private final List<Erreur> erreurs = new LinkedList<>();

	public static final class Erreur {
		private final Long mutationId;
		private final TypeEntiteRF typeEntite;
		private final TypeMutationRF typeMutation;
		private final String message;

		public Erreur(Long mutationId, TypeEntiteRF typeEntite, TypeMutationRF typeMutation, String message) {
			this.mutationId = mutationId;
			this.typeEntite = typeEntite;
			this.typeMutation = typeMutation;
			this.message = message;
		}

		public Long getMutationId() {
			return mutationId;
		}

		public TypeEntiteRF getTypeEntite() {
			return typeEntite;
		}

		public TypeMutationRF getTypeMutation() {
			return typeMutation;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "Erreur{" +
					"mutationId=" + mutationId +
					", typeEntite=" + typeEntite +
					", message='" + message + '\'' +
					'}';
		}
	}

	public static final class ProcessedKey {

		private final TypeEntiteRF typeEntite;
		private final TypeMutationRF typeMutation;

		public ProcessedKey(TypeEntiteRF typeEntite, TypeMutationRF typeMutation) {
			this.typeEntite = typeEntite;
			this.typeMutation = typeMutation;
		}

		public TypeEntiteRF getTypeEntite() {
			return typeEntite;
		}

		public TypeMutationRF getTypeMutation() {
			return typeMutation;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			final ProcessedKey that = (ProcessedKey) o;
			return typeEntite == that.typeEntite &&
					typeMutation == that.typeMutation;
		}

		@Override
		public int hashCode() {
			return Objects.hash(typeEntite, typeMutation);
		}
	}

	public MutationsRFProcessorResults(long importId, boolean importInitial, RegDate dateValeur, int nbThreads, EvenementRFMutationDAO evenementRFMutationDAO) {
		super(null, null);
		this.importId = importId;
		this.importInitial = importInitial;
		this.dateValeur = dateValeur;
		this.nbThreads = nbThreads;
		this.evenementRFMutationDAO = evenementRFMutationDAO;
	}

	public long getImportId() {
		return importId;
	}

	public boolean isImportInitial() {
		return importInitial;
	}

	public RegDate getDateValeur() {
		return dateValeur;
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

	public Map<ProcessedKey, MutableLong> getProcessed() {
		return processed;
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

	public void addErrorException(Long mutId, TypeEntiteRF typeEntite, TypeMutationRF typeMutation, Exception e) {
		erreurs.add(new Erreur(mutId, typeEntite, typeMutation, String.format("Exception levée : %s", e.getMessage())));
	}

	@Override
	public void addErrorException(Long id, Exception e) {
		final EvenementRFMutation mutation = evenementRFMutationDAO.get(id);
		if (mutation != null) {
			addErrorException(id, mutation.getTypeEntite(), mutation.getTypeMutation(), e);
		}
		else {
			addErrorException(id, null, null, e);
		}
	}

	public void addProcessed(long mutationId, @NotNull TypeEntiteRF typeEntite, @NotNull TypeMutationRF typeMutation) {
		final ProcessedKey key = new ProcessedKey(typeEntite, typeMutation);
		final MutableLong count = processed.computeIfAbsent(key, k -> new MutableLong(0));
		count.increment();
	}

	@Override
	public void addAll(MutationsRFProcessorResults right) {
		right.processed.forEach((key, value) -> {
			final MutableLong count = processed.computeIfAbsent(key, k -> new MutableLong(0));
			count.add(value.getValue());
		});
		erreurs.addAll(right.erreurs);
	}
}
