package ch.vd.unireg.registrefoncier.communaute;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.NomPrenomDates;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

public class MembreCommunauteView {

	/**
	 * Id technique de l'ayant-droit
	 */
	@Nullable
	private final Long id;
	@Nullable
	private final Long ctbId;
	private final String role;
	private final String nom;
	private final String prenom;
	private final RegDate dateNaissance;
	private final RegDate dateDeces;
	private final String forPrincipal;

	public MembreCommunauteView(@Nullable AyantDroitRF ayantDroit, @Nullable Contribuable ctb, @NotNull TiersService tiersService, @NotNull RegistreFoncierService registreFoncierService) {
		this.id = ayantDroit == null ? null : ayantDroit.getId();
		if (ctb == null) {
			this.ctbId = null;
			this.role = null;
			if (ayantDroit instanceof TiersRF) {
				final NomPrenomDates decompo = registreFoncierService.getDecompositionNomPrenomDateNaissanceRF((TiersRF) ayantDroit);
				this.nom = decompo.getNom();
				this.prenom = decompo.getPrenom();
				this.dateNaissance = decompo.getDateNaissance();
				this.dateDeces = decompo.getDateDeces();
			}
			else {
				this.nom = null;
				this.prenom = null;
				this.dateNaissance = null;
				this.dateDeces = null;
			}
			this.forPrincipal = null;
		}
		else {
			this.ctbId = ctb.getNumero();
			this.role = ctb.getRoleLigne1();
			if (ctb instanceof PersonnePhysique) {
				final NomPrenom decompo = tiersService.getDecompositionNomPrenom((PersonnePhysique) ctb, false);
				this.nom = decompo.getNom();
				this.prenom = decompo.getPrenom();
				this.dateNaissance = tiersService.getDateNaissance((PersonnePhysique) ctb);
				this.dateDeces = tiersService.getDateDeces((PersonnePhysique) ctb);
			}
			else {
				this.nom = tiersService.getNomRaisonSociale(ctb);
				this.prenom = null;
				this.dateNaissance = null;
				this.dateDeces = null;
			}
			this.forPrincipal = Stream.concat(Stream.of(ctb.getForFiscalPrincipalAt(null)), tiersService.getForsFiscauxVirtuels(ctb, true).stream())
					.filter(Objects::nonNull)
					.filter(f -> f.isValidAt(null))
					.findFirst()
					.map(tiersService::getLocalisationAsString)
					.orElse(null);
		}
	}

	public MembreCommunauteView(@NotNull AyantDroitRF ayantDroit, @NotNull TiersService tiersService, RegistreFoncierService registreFoncierService) {
		// on va chercher le contribuable rapproché au tiers RF
		this(ayantDroit, Optional.of(ayantDroit)
				.filter(TiersRF.class::isInstance)
				.map(TiersRF.class::cast)
				.map(TiersRF::getCtbRapproche)
				.orElse(null), tiersService, registreFoncierService);

	}

	@Nullable
	public Long getId() {
		return id;
	}

	@Nullable
	public Long getCtbId() {
		return ctbId;
	}

	public String getRole() {
		return role;
	}

	/**
	 * @return le nom du membre (source = Registre Civil si ctbId != null, Registre RF si ctbId == null).
	 */
	public String getNom() {
		return nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public RegDate getDateNaissance() {
		return dateNaissance;
	}

	public RegDate getDateDeces() {
		return dateDeces;
	}

	public String getForPrincipal() {
		return forPrincipal;
	}
}
