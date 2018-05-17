<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="type" type="java.lang.String"--%>
<c:if test="${type == 'CommunauteRF'}">
<fieldset class="information"><legend><span>Caractéristique de la communauté sélectionnée</span></legend>
	<%--@elvariable id="communauteId" type="java.lang.Long"--%>
	<%--@elvariable id="typeCommunaute" type="ch.vd.unireg.registrefoncier.TypeCommunaute"--%>
	<table>
		<unireg:nextRowClass reset="1"/>
		<tr class="<unireg:nextRowClass/>"><td width="15%">N°</td><td><unireg:linkTo name="${communauteId}" action="/registrefoncier/immeuble/graph.do" params="{commId:'${communauteId}'}"/></td></tr>
		<tr class="<unireg:nextRowClass/>"><td>Type</td><td>${typeCommunaute}</td></tr>
	</table>
</fieldset>
</c:if>
<c:if test="${type == 'TiersRF'}">
<fieldset class="information"><legend><span>Caractéristique du tiers RF sélectionné</span></legend>
	<%--@elvariable id="typeTiersRF" type="java.lang.String"--%>
	<%--@elvariable id="raisonSociale" type="java.lang.String"--%>
	<%--@elvariable id="numeroRC" type="java.lang.String"--%>
	<%--@elvariable id="prenom" type="java.lang.String"--%>
	<%--@elvariable id="nom" type="java.lang.String"--%>
	<%--@elvariable id="dateNaissance" type="ch.vd.registre.base.date.RegDate"--%>
	<table>
		<unireg:nextRowClass reset="1"/>
		<tr class="<unireg:nextRowClass/>"><td width="15%">Type</td><td>${typeTiersRF} (non-rapproché)</td></tr>
		<c:if test="${raisonSociale != null}"><tr class="<unireg:nextRowClass/>"><td>Raison sociale</td><td><c:out value="${raisonSociale}"/></td></tr></c:if>
		<c:if test="${numeroRC != null}"><tr class="<unireg:nextRowClass/>"><td>Raison sociale</td><td><c:out value="${numeroRC}"/></td></tr></c:if>
		<c:if test="${prenom != null}"><tr class="<unireg:nextRowClass/>"><td>Prénom</td><td><c:out value="${prenom}"/></td></tr></c:if>
		<c:if test="${nom != null}"><tr class="<unireg:nextRowClass/>"><td>Nom</td><td><c:out value="${nom}"/></td></tr></c:if>
		<c:if test="${dateNaissance != null}"><tr class="<unireg:nextRowClass/>"><td>Date naissance</td><td><unireg:date date="${dateNaissance}"/></td></tr></c:if>
	</table>
</fieldset>
</c:if>
<c:if test="${type == 'Contribuable'}">
<fieldset class="information"><legend><span>Caractéristique du tiers sélectionné</span></legend>
	<%--@elvariable id="ctbId" type="java.lang.Long"--%>
	<%--@elvariable id="role" type="java.lang.String"--%>
	<%--@elvariable id="adresse" type="java.lang.String"--%>
	<table>
		<unireg:nextRowClass reset="1"/>
		<c:set var="noctb">
			<unireg:numCTB numero="${ctbId}"/>
		</c:set>
		<tr class="<unireg:nextRowClass/>"><td width="15%" style="white-space: nowrap;">N° de tiers</td><td><unireg:linkTo name="${noctb}" action="/registrefoncier/immeuble/graph.do" params="{ctbId:'${ctbId}'}"/></td></tr>
		<tr class="<unireg:nextRowClass/>"><td>Rôle</td><td>${role}</td></tr>
		<tr class="<unireg:nextRowClass/>"><td>Adresse</td><td>${adresse}</td></tr>
	</table>
</fieldset>
</c:if>
<c:if test="${type == 'ImmeubleRF'}">
<fieldset class="information"><legend><span>Caractéristique de l'immeuble sélectionné</span></legend>
	<%--@elvariable id="immeuble" type="ch.vd.unireg.registrefoncier.immeuble.graph.Immeuble"--%>
	<table>
		<unireg:nextRowClass reset="1"/>
		<tr class="<unireg:nextRowClass/>"><td width="15%">N°</td><td><unireg:linkTo name="${immeuble.id}" action="/registrefoncier/immeuble/graph.do" params="{id:'${immeuble.id}'}"/></td></tr>
		<tr class="<unireg:nextRowClass/>"><td width="15%">EGRID</td><td><unireg:linkTo name="${immeuble.egrid}" action="/registrefoncier/immeuble/graph.do" params="{egrid:'${immeuble.egrid}'}"/></td></tr>
		<tr class="<unireg:nextRowClass/>"><td>ID RF</td><td>${immeuble.idRF}</td></tr>
		<tr class="<unireg:nextRowClass/>"><td>Type</td><td>${immeuble.typeLong}</td></tr>
		<tr class="<unireg:nextRowClass/>"><td>Situation</td><td>${immeuble.commune} / ${immeuble.parcelle}</td></tr>
	</table>
</fieldset>
</c:if>
<c:if test="${type == 'DroitRF'}">
<fieldset class="information"><legend><span>Caractéristique du droit sélectionné</span></legend>
	<%--@elvariable id="typeDroit" type="java.lang.String"--%>
	<%--@elvariable id="regime" type="ch.vd.unireg.registrefoncier.GenrePropriete"--%>
	<%--@elvariable id="dateDebutMetier" type="ch.vd.registre.base.date.RegDate"--%>
	<%--@elvariable id="dateFinMetier" type="ch.vd.registre.base.date.RegDate"--%>
	<%--@elvariable id="motifDebut" type="java.lang.String"--%>
	<%--@elvariable id="motifFin" type="java.lang.String"--%>
	<%--@elvariable id="part" type="java.lang.String"--%>
	<%--@elvariable id="numeroAffaire" type="java.lang.String"--%>
	<table>
		<unireg:nextRowClass reset="1"/>
		<tr class="<unireg:nextRowClass/>"><td width="15%">Type</td><td>${typeDroit}<c:if test="${regime != null}"> - ${regime}</c:if></td></tr>
		<tr class="<unireg:nextRowClass/>"><td style="white-space: nowrap;">Date de début</td><td><unireg:date date="${dateDebutMetier}"/> (${motifDebut})</td></tr>
		<tr class="<unireg:nextRowClass/>"><td style="white-space: nowrap;">Date de fin</td><td><c:if test="${dateFinMetier != null}"><unireg:date date="${dateFinMetier}"/> (${motifFin})</c:if></td></tr>
		<c:if test="${part != null}"><tr class="<unireg:nextRowClass/>"><td>Parts</td><td>${part}</td></tr></c:if>

		<c:if test="${numeroAffaire != null}"><tr class="<unireg:nextRowClass/>"><td style="white-space: nowrap;">Numéro d'affaire</td><td>${numeroAffaire}</td></tr></c:if>
	</table>
</fieldset>
</c:if>
