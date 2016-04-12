package ch.vd.unireg.interfaces.organisation.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.KindOfUidEntity;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;

public class UidRegisterTypeOfOrganisationConverter extends BaseEnumConverter<KindOfUidEntity, TypeOrganisationRegistreIDE> {

	@Override
	@NotNull
	protected TypeOrganisationRegistreIDE convert(@NotNull KindOfUidEntity value) {
		switch (value) {
		case AUTRE:
			return TypeOrganisationRegistreIDE.AUTRE;
		case PERSONNE_JURIDIQUE:
			return TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE;
		case PROFESSION_MEDICALE_UNIVERSITAIRE:
			return TypeOrganisationRegistreIDE.PROFESSION_MEDICALE_UNIVERSITAIRE;
		case AVOCAT:
			return TypeOrganisationRegistreIDE.AVOCAT;
		case NOTAIRE:
			return TypeOrganisationRegistreIDE.NOTAIRE;
		case PERSONNE_PHYSIQUE_ET_SOCIETE_SIMPLE_AGRICULTURE:
			return TypeOrganisationRegistreIDE.PERSONNE_PHYSIQUE_ET_SOCIETE_SIMPLE_AGRICULTURE;
		case SOCIETE_SIMPLE:
			return TypeOrganisationRegistreIDE.SOCIETE_SIMPLE;
		case ENTREPRISE_INDIVIDUELLE:
			return TypeOrganisationRegistreIDE.ENTREPRISE_INDIVIDUELLE;
		case ENTREPRISE_DE_DROIT_PUBLIC:
			return TypeOrganisationRegistreIDE.ENTREPRISE_DE_DROIT_PUBLIC;
		case PURE_UNITE_ADMINISTRATIVE:
			return TypeOrganisationRegistreIDE.PURE_UNITE_ADMINISTRATIVE;
		case ASSOCIATION:
			return TypeOrganisationRegistreIDE.ASSOCIATION;
		case FONDATION:
			return TypeOrganisationRegistreIDE.FONDATION;
		case SUCCURSALE_ETRANGERE_NON_AU_RC:
			return TypeOrganisationRegistreIDE.SUCCURSALE_ETRANGERE_NON_AU_RC;
		case DOMAINE_DOUANIER:
			return TypeOrganisationRegistreIDE.DOMAINE_DOUANIER;
		case ENTREPRISE_ETRANGERE:
			return TypeOrganisationRegistreIDE.ENTREPRISE_ETRANGERE;
		case AVOCAT_ET_NOTAIRE:
			return TypeOrganisationRegistreIDE.AVOCAT_ET_NOTAIRE;
		case SITE:
			return TypeOrganisationRegistreIDE.SITE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
