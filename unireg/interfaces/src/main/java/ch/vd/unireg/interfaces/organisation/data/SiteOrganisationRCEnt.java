package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0021.v1.Address;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntHelper;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class SiteOrganisationRCEnt implements SiteOrganisation {
	private final long cantonalId;
	@NotNull
	private final List<DateRanged<String>> nom;

	public final DonneesRC rc;
	public final DonneesRegistreIDE ide;

	private final Map<String,List<DateRanged<String>>> identifiants;
	private final List<DateRanged<String>> nomsAdditionnels;
	private final List<DateRanged<KindOfLocation>> typeDeSite;
	/**
	 * municipalityId du {@link SwissMunicipality}
	 */
	private final List<DateRanged<Integer>> siege;
	//private final List<DateRanged<Function>> fonction;
	private final List<DateRanged<Long>> remplacePar;
	private final List<DateRanged<Long>> enRemplacementDe;


	public SiteOrganisationRCEnt(final OrganisationLocation rcEntLocation) {
		cantonalId = rcEntLocation.getCantonalId();
		nom = RCEntHelper.convert(rcEntLocation.getName());
		identifiants = RCEntHelper.convert(rcEntLocation.getIdentifiers());
		nomsAdditionnels = RCEntHelper.convert(rcEntLocation.getOtherNames());
		typeDeSite = RCEntHelper.convertAndMap(rcEntLocation.getKindOfLocation(), new Function<ch.vd.evd0022.v1.KindOfLocation, KindOfLocation>() {
			@Override
			public KindOfLocation apply(ch.vd.evd0022.v1.KindOfLocation kindOfLocation) {
				return KindOfLocation.valueOf(kindOfLocation.toString());
			}
		});
		siege = RCEntHelper.convert(rcEntLocation.getSeat());
		// fonction =
		remplacePar = RCEntHelper.convert(rcEntLocation.getReplacedBy());
		enRemplacementDe = RCEntHelper.convert(rcEntLocation.getInReplacementOf());

		final OrganisationLocation.RCEntRCData rc = rcEntLocation.getRc();
		this.rc = new DonneesRC(
				RCEntHelper.convertAndMap(rc.getLegalAddress(), new Function<Address, Adresse>() {
					@Override
					public Adresse apply(ch.vd.evd0021.v1.Address address) {
						return RCEntAddressHelper.fromRCEntAddress(address);
					}
				}),
				RCEntHelper.convertAndMap(rc.getStatus(), new Function<ch.vd.evd0022.v1.CommercialRegisterStatus, StatusRC>() {
					@Override
					public StatusRC apply(ch.vd.evd0022.v1.CommercialRegisterStatus commercialRegisterStatus) {
						return StatusRC.valueOf(commercialRegisterStatus.toString());
					}
				}),
				RCEntHelper.convert(rc.getName()),
				RCEntHelper.convertAndMap(rc.getEntryStatus(), new Function<ch.vd.evd0022.v1.CommercialRegisterEntryStatus, StatusInscriptionRC>() {
					@Override
					public StatusInscriptionRC apply(ch.vd.evd0022.v1.CommercialRegisterEntryStatus commercialRegisterEntryStatus) {
						return StatusInscriptionRC.valueOf(commercialRegisterEntryStatus.toString());
					}
				}),
				RCEntHelper.convertAndMap(rc.getCapital(), new Function<ch.vd.evd0022.v1.Capital, Capital>() {
					@Override
					public Capital apply(ch.vd.evd0022.v1.Capital capital) {
						return new Capital(TypeOfCapital.valueOf(capital.getTypeOfCapital().toString()), capital.getCurrency(), capital.getCapitalAmount(), capital.getCashedInAmount(), capital.getDivision());
					}
				})
		);
		this.ide = null;
	}

	public static class DonneesRC {
		private final List<DateRanged<StatusRC>> status;
		private final List<DateRanged<String>> nom;
		private final List<DateRanged<StatusInscriptionRC>> statusInscription;
		private final List<DateRanged<Capital>> capital;
		private final List<DateRanged<Adresse>> adresseLegale;

		public DonneesRC(List<DateRanged<Adresse>> adresseLegale, List<DateRanged<StatusRC>> status, List<DateRanged<String>> nom,
		                 List<DateRanged<StatusInscriptionRC>> statusInscription, List<DateRanged<Capital>> capital) {
			this.adresseLegale = adresseLegale;
			this.status = status;
			this.nom = nom;
			this.statusInscription = statusInscription;
			this.capital = capital;
		}

		public List<DateRanged<Adresse>> getAdresseLegale() {
			return adresseLegale;
		}

		public List<DateRanged<Capital>> getCapital() {
			return capital;
		}

		public List<DateRanged<String>> getNom() {
			return nom;
		}

		public List<DateRanged<StatusRC>> getStatus() {
			return status;
		}

		public List<DateRanged<StatusInscriptionRC>> getStatusInscription() {
			return statusInscription;
		}
	}

	public static class DonneesRegistreIDE {
		private final List<DateRanged<StatusRegistreIDE>> status;
		private final List<DateRanged<TypeOrganisationRegistreIDE>> typeOfOrganisation;
		private final List<DateRanged<Adresse>> adresseEffective;
		private final List<DateRanged<Adresse>> adresseBoitePostale;
		private final List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation;

		public DonneesRegistreIDE(List<DateRanged<Adresse>> adresseBoitePostale,
		                          List<DateRanged<StatusRegistreIDE>> status,
		                          List<DateRanged<TypeOrganisationRegistreIDE>> typeOfOrganisation,
		                          List<DateRanged<Adresse>> adresseEffective,
		                          List<DateRanged<RaisonLiquidationRegistreIDE>> raisonDeLiquidation) {
			this.adresseBoitePostale = adresseBoitePostale;
			this.status = status;
			this.typeOfOrganisation = typeOfOrganisation;
			this.adresseEffective = adresseEffective;
			this.raisonDeLiquidation = raisonDeLiquidation;
		}

		public List<DateRanged<Adresse>> getAdresseBoitePostale() {
			return adresseBoitePostale;
		}

		public List<DateRanged<Adresse>> getAdresseEffective() {
			return adresseEffective;
		}

		public List<DateRanged<RaisonLiquidationRegistreIDE>> getRaisonDeLiquidation() {
			return raisonDeLiquidation;
		}

		public List<DateRanged<StatusRegistreIDE>> getStatus() {
			return status;
		}

		public List<DateRanged<TypeOrganisationRegistreIDE>> getTypeOfOrganisation() {
			return typeOfOrganisation;
		}
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public List<DateRanged<Long>> getEnRemplacementDe() {
		return enRemplacementDe;
	}

	public DonneesRegistreIDE getIde() {
		return ide;
	}

	public Map<String, List<DateRanged<String>>> getIdentifiants() {
		return identifiants;
	}

	@NotNull
	public List<DateRanged<String>> getNom() {
		return nom;
	}

	public List<DateRanged<String>> getNomsAdditionnels() {
		return nomsAdditionnels;
	}

	public DonneesRC getRc() {
		return rc;
	}

	public List<DateRanged<Long>> getRemplacePar() {
		return remplacePar;
	}

	public List<DateRanged<Integer>> getSiege() {
		return siege;
	}

	public List<DateRanged<KindOfLocation>> getTypeDeSite() {
		return typeDeSite;
	}
}
