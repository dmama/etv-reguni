<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="page" value="${param.page}" />
<c:if test="${page == 'visu' }">
	<c:set var="url" value="visu.do" />
</c:if>
<c:if test="${page == 'edit' }">
	<c:set var="url" value="edit.do" />
	<unireg:setAuth var="autorisations" tiersId="${command.tiersGeneral.numero}"/>
</c:if>
<c:if test="${not empty command.forsFiscaux}">
<display:table
		name="command.forsFiscaux" id="forFiscal" pagesize="10" 
		requestURI="${url}"
		class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

	<display:column style="width:16px;">
		<c:if test="${forFiscal.natureForFiscal == 'ForFiscalPrincipal'}">
			<div id="ffid-${forFiscal.id}" class="forPrincipalIconSmall" title="For fiscal principal"/>
		</c:if>
		<c:if test="${forFiscal.natureForFiscal == 'ForFiscalSecondaire'}">
			<div id="ffid-${forFiscal.id}" class="forSecondaireIconSmall" title="For fiscal secondaire"/>
		</c:if>
		<c:if test="${forFiscal.natureForFiscal == 'ForFiscalAutreElementImposable'}">
			<div id="ffid-${forFiscal.id}" class="forAutreElementImposableIconSmall"  title="For fiscal autre élément imposable"/>
		</c:if>
		<c:if test="${forFiscal.natureForFiscal == 'ForFiscalAutreImpot'}">
			<div id="ffid-${forFiscal.id}" class="forAutreImpotIconSmall"  title="For fiscal autre impot"/>
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.genre.impot">
			<fmt:message key="option.genre.impot.${forFiscal.genreImpot}"  />
	</display:column>
	<display:column sortable ="true" titleKey="label.rattachement" >
		<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
				<fmt:message key="option.rattachement.${forFiscal.motifRattachement}" />
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.mode.imposition">
		<c:if test="${forFiscal.natureForFiscal == 'ForFiscalPrincipal'}">
				<fmt:message key="option.mode.imposition.${forFiscal.modeImposition}" />
		</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.for.abrege">
			<c:choose>
				<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">			
					<unireg:commune ofs="${forFiscal.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
				</c:when>
				<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_HC' }">
					<unireg:commune ofs="${forFiscal.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
					(<unireg:commune ofs="${forFiscal.numeroForFiscalCommuneHorsCanton}" displayProperty="sigleCanton" date="${forFiscal.regDateOuverture}"/>)
				</c:when>
				<c:when test="${forFiscal.typeAutoriteFiscale == 'PAYS_HS' }">
					<unireg:infra entityId="${forFiscal.numeroForFiscalPays}" entityType="pays" entityPropertyName="nomCourt" entityPropertyTitle="noOFS"></unireg:infra>
				</c:when>
			</c:choose>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
			<fmt:formatDate value="${forFiscal.dateOuverture}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.motif.ouv">
			<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
				<c:if test="${forFiscal.motifOuverture != null}">
					<fmt:message key="option.motif.ouverture.${forFiscal.motifOuverture}" />
				</c:if>
			</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
			<fmt:formatDate value="${forFiscal.dateFermeture}" pattern="dd.MM.yyyy"/>
	</display:column>
	<display:column sortable ="true" titleKey="label.motif.fer">
			<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
				<c:if test="${forFiscal.motifFermeture != null}">
					<fmt:message key="option.motif.fermeture.${forFiscal.motifFermeture}" />
				</c:if>
			</c:if>
	</display:column>
	<display:column sortable ="true" titleKey="label.for.gestion">
		<c:if test="${!forFiscal.annule}">
			<c:if test="${forFiscal.natureForFiscal != 'ForFiscalAutreImpot'}">
				<input type="checkbox" <c:if test="${forFiscal.forGestion}">checked</c:if> disabled="disabled">
			</c:if>
		</c:if>
	</display:column>
	<display:column class="action">
		<c:if test="${page == 'visu' }">
			<unireg:consulterLog entityNature="ForFiscal" entityId="${forFiscal.id}"/>
		</c:if>
		<c:if test="${page == 'edit' }">
			<c:if test="${!forFiscal.annule}">
				<c:if test="${forFiscal.natureForFiscal == 'ForFiscalPrincipal' && autorisations.forsPrincipaux}">
					<unireg:linkTo name="" action="/fors/principal/edit.do" method="GET" params="{forId:${forFiscal.id}}" link_class="edit" title="Edition de for" />
					<c:if test="${forFiscal.dernierForPrincipalOuDebiteur}">
						<unireg:linkTo name="" action="/fors/principal/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
						               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
					</c:if>
				</c:if>
				<c:if test="${forFiscal.natureForFiscal == 'ForFiscalSecondaire' && autorisations.forsSecondaires}">
					<unireg:linkTo name="" action="/fors/secondaire/edit.do" method="GET" params="{forId:${forFiscal.id}}" link_class="edit" title="Edition de for" />
					<unireg:linkTo name="" action="/fors/secondaire/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
					               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
				</c:if>
				<c:if test="${forFiscal.natureForFiscal == 'ForFiscalAutreElementImposable' && autorisations.forsAutresElementsImposables}">
					<unireg:linkTo name="" action="/fors/autreelementimposable/edit.do" method="GET" params="{forId:${forFiscal.id}}" link_class="edit" title="Edition de for" />
					<unireg:linkTo name="" action="/fors/autreelementimposable/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
					               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
				</c:if>
				<c:if test="${forFiscal.natureForFiscal == 'ForFiscalAutreImpot' && autorisations.forsAutresImpots}">
					<span class="button_placeholder">&nbsp;</span>
					<unireg:linkTo name="" action="/fors/autreimpot/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
					               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
				</c:if>
			</c:if>
		</c:if>
	</display:column>
	<display:setProperty name="paging.banner.all_items_found" value=""/>
	<display:setProperty name="paging.banner.one_item_found" value=""/>
	
</display:table>
<script type="text/javascript">

	// mise-en-évidence d'un for qui vient d'être ajouté ou édité
	var params = App.get_url_params();
	if (params && params.highlightFor) {
		$('#ffid-' + params.highlightFor).closest('tr').effect('highlight', 4000);
	}

</script>
</c:if>