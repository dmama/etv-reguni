package ch.vd.unireg.interfaces.entreprise.rcent.converters;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v3.KindOfUidEntity;
import ch.vd.unireg.interfaces.entreprise.data.TypeEntrepriseRegistreIDE;

public class UidRegisterTypeOfOrganisationConverter extends BaseEnumConverter<KindOfUidEntity, TypeEntrepriseRegistreIDE> {

	@Override
	@NotNull
	protected TypeEntrepriseRegistreIDE convert(@NotNull KindOfUidEntity value) {
		switch (value) {
		case AUTRE:
			return TypeEntrepriseRegistreIDE.AUTRE;
		case PERSONNE_JURIDIQUE:
			return TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE;
		case PROFESSION_MEDICALE_UNIVERSITAIRE:
			return TypeEntrepriseRegistreIDE.PROFESSION_MEDICALE_UNIVERSITAIRE;
		case AVOCAT:
			return TypeEntrepriseRegistreIDE.AVOCAT;
		case NOTAIRE:
			return TypeEntrepriseRegistreIDE.NOTAIRE;
		case PERSONNE_PHYSIQUE_ET_SOCIETE_SIMPLE_AGRICULTURE:
			return TypeEntrepriseRegistreIDE.PERSONNE_PHYSIQUE_ET_SOCIETE_SIMPLE_AGRICULTURE;
		case SOCIETE_SIMPLE:
			return TypeEntrepriseRegistreIDE.SOCIETE_SIMPLE;
		case ENTREPRISE_INDIVIDUELLE:
			return TypeEntrepriseRegistreIDE.ENTREPRISE_INDIVIDUELLE;
		case ENTREPRISE_DE_DROIT_PUBLIC:
			return TypeEntrepriseRegistreIDE.ENTREPRISE_DE_DROIT_PUBLIC;
		case PURE_UNITE_ADMINISTRATIVE:
			return TypeEntrepriseRegistreIDE.PURE_UNITE_ADMINISTRATIVE;
		case ASSOCIATION:
			return TypeEntrepriseRegistreIDE.ASSOCIATION;
		case FONDATION:
			return TypeEntrepriseRegistreIDE.FONDATION;
		case SUCCURSALE_ETRANGERE_NON_AU_RC:
			return TypeEntrepriseRegistreIDE.SUCCURSALE_ETRANGERE_NON_AU_RC;
		case DOMAINE_DOUANIER:
			return TypeEntrepriseRegistreIDE.DOMAINE_DOUANIER;
		case ENTREPRISE_ETRANGERE:
			return TypeEntrepriseRegistreIDE.ENTREPRISE_ETRANGERE;
		case AVOCAT_ET_NOTAIRE:
			return TypeEntrepriseRegistreIDE.AVOCAT_ET_NOTAIRE;
		case SITE:
			return TypeEntrepriseRegistreIDE.SITE;
		default:
			throw new IllegalArgumentException(genericUnsupportedValueMessage(value));
		}
	}
}
