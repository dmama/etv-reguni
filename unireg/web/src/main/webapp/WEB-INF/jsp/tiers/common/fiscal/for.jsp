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

<c:if test="${not empty command.forsFiscauxPrincipaux || (page == 'edit' && autorisations.forsPrincipaux)}">

	<fieldset>
		<legend><span><fmt:message key="label.fors.fiscaux.principaux"/></span></legend>

		<c:if test="${page == 'edit'}">
			<table border="0">
				<tr><td>
					<c:if test="${autorisations.forsPrincipaux}">
						<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/principal/add.do" params="{tiersId:${command.tiersGeneral.numero}}" link_class="add margin_right_10"/>
					</c:if>
					<c:if test="${command.forsPrincipalActif != null && autorisations.forsPrincipaux && command.natureTiers != 'Entreprise'}">
						<unireg:linkTo name="Changer le mode d'imposition" title="Changer le mode d'imposition" action="/fors/principal/editModeImposition.do" params="{forId:${command.forsPrincipalActif.id}}" link_class="add"/>
					</c:if>
				</td></tr>
			</table>
		</c:if>

		<c:if test="${not empty command.forsFiscauxPrincipaux}">
			<display:table name="command.forsFiscauxPrincipaux" id="forFiscal" pagesize="${command.nombreElementsTable}"
			               requestURI="${url}"
			               class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

				<display:column style="width:16px;">
					<div id="ffid-${forFiscal.id}" class="forPrincipalIconSmall" title="For fiscal principal"></div>
				</display:column>
				<display:column sortable ="true" titleKey="label.genre.impot">
					<fmt:message key="option.genre.impot.${forFiscal.genreImpot}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.rattachement" >
					<fmt:message key="option.rattachement.${forFiscal.motifRattachement}" />
				</display:column>
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.mode.imposition">
						<fmt:message key="option.mode.imposition.${forFiscal.modeImposition}" />
					</display:column>
				</c:if>
				<display:column sortable ="true" titleKey="label.for.abrege">
					<c:choose>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:commune ofs="${forFiscal.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:commune ofs="${forFiscal.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:pays ofs="${forFiscal.numeroForFiscalPays}" displayProperty="nomCourt" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
					</c:choose>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
					<fmt:formatDate value="${forFiscal.dateOuverture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.ouv">
					<c:if test="${forFiscal.motifOuverture != null}">
						<fmt:message key="option.motif.ouverture.${forFiscal.motifOuverture}" />
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
					<fmt:formatDate value="${forFiscal.dateFermeture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.fer">
					<c:if test="${forFiscal.motifFermeture != null}">
						<fmt:message key="option.motif.fermeture.${forFiscal.motifFermeture}" />
					</c:if>
				</display:column>
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.for.gestion">
						<c:if test="${!forFiscal.annule}">
							<input type="checkbox" <c:if test="${forFiscal.forGestion}">checked</c:if> disabled="disabled">
						</c:if>
					</display:column>
				</c:if>
				<display:column class="action">
					<c:if test="${page == 'visu' }">
						<unireg:consulterLog entityNature="ForFiscal" entityId="${forFiscal.id}"/>
					</c:if>
					<c:if test="${page == 'edit'}">
						<c:if test="${!forFiscal.annule}">
							<c:if test="${autorisations.forsPrincipaux}">
								<unireg:linkTo name="" action="/fors/principal/edit.do" method="GET" params="{forId:${forFiscal.id}}" link_class="edit" title="Edition de for" />
								<c:if test="${forFiscal.dernierForPrincipalOuDebiteur}">
									<unireg:linkTo name="" action="/fors/principal/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
									               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
								</c:if>
							</c:if>
						</c:if>
					</c:if>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>
		</c:if>
	</fieldset>

</c:if>

<c:if test="${not empty command.forsFiscauxSecondaires || (page == 'edit' && autorisations.forsSecondaires)}">

	<fieldset>
		<legend><span><fmt:message key="label.fors.fiscaux.secondaires"/></span></legend>

		<c:if test="${page == 'edit' && autorisations.forsSecondaires}">
			<table border="0">
				<tr><td>
					<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/secondaire/add.do" params="{tiersId:${command.tiersGeneral.numero}}" link_class="add margin_right_10"/>
				</td></tr>
			</table>
		</c:if>

		<c:if test="${not empty command.forsFiscauxSecondaires}">
			<display:table name="command.forsFiscauxSecondaires" id="forFiscal" pagesize="${command.nombreElementsTable}"
			               requestURI="${url}"
			               class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

				<display:column style="width:16px;">
					<div id="ffid-${forFiscal.id}" class="forSecondaireIconSmall" title="For fiscal secondaire"></div>
				</display:column>
				<display:column sortable ="true" titleKey="label.genre.impot">
					<fmt:message key="option.genre.impot.${forFiscal.genreImpot}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.rattachement" >
					<fmt:message key="option.rattachement.${forFiscal.motifRattachement}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.for.abrege">
					<c:choose>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:commune ofs="${forFiscal.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:commune ofs="${forFiscal.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:pays ofs="${forFiscal.numeroForFiscalPays}" displayProperty="nomCourt" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
					</c:choose>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
					<fmt:formatDate value="${forFiscal.dateOuverture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.ouv">
					<c:if test="${forFiscal.motifOuverture != null}">
						<fmt:message key="option.motif.ouverture.${forFiscal.motifOuverture}" />
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
					<fmt:formatDate value="${forFiscal.dateFermeture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.fer">
					<c:if test="${forFiscal.motifFermeture != null}">
						<fmt:message key="option.motif.fermeture.${forFiscal.motifFermeture}" />
					</c:if>
				</display:column>
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.for.gestion">
						<c:if test="${!forFiscal.annule}">
							<input type="checkbox" <c:if test="${forFiscal.forGestion}">checked</c:if> disabled="disabled">
						</c:if>
					</display:column>
				</c:if>
				<display:column class="action">
					<c:if test="${page == 'visu' }">
						<unireg:consulterLog entityNature="ForFiscal" entityId="${forFiscal.id}"/>
					</c:if>
					<c:if test="${page == 'edit' }">
						<c:if test="${!forFiscal.annule}">
							<c:if test="${autorisations.forsSecondaires}">
								<unireg:linkTo name="" action="/fors/secondaire/edit.do" method="GET" params="{forId:${forFiscal.id}}" link_class="edit" title="Edition de for" />
								<unireg:linkTo name="" action="/fors/secondaire/cancel.do" method="POST" params="{forId:${forFiscal.id}}" link_class="delete"
								               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
							</c:if>
						</c:if>
					</c:if>
				</display:column>
				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>
		</c:if>

	</fieldset>

</c:if>

<c:if test="${not empty command.autresForsFiscaux || (page == 'edit' && autorisations.forsAutresElementsImposables)}">

	<fieldset>
		<legend><span><fmt:message key="label.fors.fiscaux.autres"/></span></legend>

		<c:if test="${page == 'edit' && autorisations.forsAutresElementsImposables}">
			<table border="0">
				<tr><td>
					<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/autreelementimposable/add.do" params="{tiersId:${command.tiersGeneral.numero}}" link_class="add margin_right_10"/>
				</td></tr>
			</table>
		</c:if>

		<c:if test="${not empty command.autresForsFiscaux}">
			<display:table name="command.autresForsFiscaux" id="forFiscal" pagesize="${command.nombreElementsTable}"
			               requestURI="${url}"
			               class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

				<display:column style="width:16px;">
					<c:if test="${forFiscal.natureForFiscal == 'ForFiscalAutreElementImposable'}">
						<div id="ffid-${forFiscal.id}" class="forAutreElementImposableIconSmall"  title="For fiscal autre élément imposable"></div>
					</c:if>
					<c:if test="${forFiscal.natureForFiscal == 'ForFiscalAutreImpot'}">
						<div id="ffid-${forFiscal.id}" class="forAutreImpotIconSmall"  title="For fiscal autre impot"></div>
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
				<display:column sortable ="true" titleKey="label.for.abrege">
					<c:choose>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:commune ofs="${forFiscal.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:commune ofs="${forFiscal.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
						</c:when>
						<c:when test="${forFiscal.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:pays ofs="${forFiscal.numeroForFiscalPays}" displayProperty="nomCourt" titleProperty="noOFS" date="${forFiscal.regDateOuverture}"/>
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
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.for.gestion">
						<c:if test="${!forFiscal.annule}">
							<input type="checkbox" <c:if test="${forFiscal.forGestion}">checked</c:if> disabled="disabled">
						</c:if>
					</display:column>
				</c:if>
				<display:column class="action">
					<c:if test="${page == 'visu' }">
						<unireg:consulterLog entityNature="ForFiscal" entityId="${forFiscal.id}"/>
					</c:if>
					<c:if test="${page == 'edit' }">
						<c:if test="${!forFiscal.annule}">
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
		</c:if>

	</fieldset>

</c:if>

<c:if test="${not empty command.forsFiscaux}">
	<script type="text/javascript">

		// mise-en-évidence d'un for qui vient d'être ajouté ou édité
		var params = App.get_url_params();
		if (params && params.highlightFor) {
			$('#ffid-' + params.highlightFor).closest('tr').effect('highlight', 4000);
		}

	</script>
</c:if>
