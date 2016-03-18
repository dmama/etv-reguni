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

		<c:choose>
			<c:when test="${page == 'edit'}">
				<table border="0">
					<tr><td>
						<c:if test="${fn:length(command.forsFiscauxPrincipaux) > command.nombreElementsTable && command.nombreElementsTable > 0}">
							<c:choose>
								<c:when test="${command.forsPrincipauxPagines}">
									<unireg:linkTo name="Liste complète" action="/fiscal/edit.do" title="Liste complète des fors principaux" params="{id:${command.tiers.numero},forsFiscauxPrincipauxPagines:false}" link_class="deplier margin_right_10"/>
								</c:when>
								<c:otherwise>
									<unireg:linkTo name="Liste paginée" action="/fiscal/edit.do" title="Liste paginée des fors principaux" params="{id:${command.tiers.numero},forsFiscauxPrincipauxPagines:true}" link_class="plier margin_right_10"/>
								</c:otherwise>
							</c:choose>
						</c:if>
						<c:if test="${autorisations.forsPrincipaux}">
							<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/principal/add.do" params="{tiersId:${command.tiersGeneral.numero}}" link_class="add margin_right_10"/>
						</c:if>
						<c:if test="${command.forsPrincipalActif != null && autorisations.forsPrincipaux && command.natureTiers != 'Entreprise'}">
							<unireg:linkTo name="Changer le mode d'imposition" title="Changer le mode d'imposition" action="/fors/principal/editModeImposition.do" params="{forId:${command.forsPrincipalActif.id}}" link_class="add"/>
						</c:if>
					</td></tr>
				</table>
			</c:when>
			<c:when test="${fn:length(command.forsFiscauxPrincipaux) > command.nombreElementsTable && command.nombreElementsTable > 0}">
				<table border="0">
					<tr><td>
						<c:choose>
							<c:when test="${command.forsPrincipauxPagines}">
								<unireg:linkTo name="Liste complète" action="/tiers/visu.do" title="Liste complète des fors principaux" params="{id:${command.tiers.numero},forsFiscauxPrincipauxPagines:false}" link_class="deplier margin_right_10"/>
							</c:when>
							<c:otherwise>
								<unireg:linkTo name="Liste paginée" action="/tiers/visu.do" title="Liste paginée des fors principaux" params="{id:${command.tiers.numero},forsFiscauxPrincipauxPagines:true}" link_class="plier margin_right_10"/>
							</c:otherwise>
						</c:choose>
					</td></tr>
				</table>
			</c:when>
		</c:choose>

		<c:if test="${not empty command.forsFiscauxPrincipaux}">
			<display:table name="command.forsFiscauxPrincipaux" id="ffp" pagesize="${(command.nombreElementsTable == 0 || !command.forsPrincipauxPagines) ? 0 : command.nombreElementsTable}"
			               requestURI="${url}" sort="list"
			               class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

				<display:column style="width:16px;">
					<div id="ffid-${ffp.id}" class="forPrincipalIconSmall" title="For fiscal principal"></div>
				</display:column>
				<display:column sortable ="true" titleKey="label.genre.impot">
					<fmt:message key="option.genre.impot.${ffp.genreImpot}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.rattachement" >
					<fmt:message key="option.rattachement.${ffp.motifRattachement}" />
				</display:column>
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.mode.imposition">
						<fmt:message key="option.mode.imposition.${ffp.modeImposition}" />
					</display:column>
				</c:if>
				<display:column sortable ="true" titleKey="label.for.abrege">
					<c:choose>
						<c:when test="${ffp.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:commune ofs="${ffp.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${ffp.regDateOuverture}"/>
						</c:when>
						<c:when test="${ffp.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:commune ofs="${ffp.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${ffp.regDateOuverture}"/>
						</c:when>
						<c:when test="${ffp.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:pays ofs="${ffp.numeroForFiscalPays}" displayProperty="nomCourt" titleProperty="noOFS" date="${ffp.regDateOuverture}"/>
						</c:when>
					</c:choose>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
					<fmt:formatDate value="${ffp.dateOuverture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.ouv">
					<c:if test="${ffp.motifOuverture != null}">
						<fmt:message key="option.motif.ouverture.${ffp.motifOuverture}" />
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
					<fmt:formatDate value="${ffp.dateFermeture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.fer">
					<c:if test="${ffp.motifFermeture != null}">
						<fmt:message key="option.motif.fermeture.${ffp.motifFermeture}" />
					</c:if>
				</display:column>
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.for.gestion">
						<c:if test="${!ffp.annule}">
							<input type="checkbox" <c:if test="${ffp.forGestion}">checked</c:if> disabled="disabled">
						</c:if>
					</display:column>
				</c:if>
				<display:column class="action">
					<c:if test="${page == 'visu' }">
						<unireg:consulterLog entityNature="ForFiscal" entityId="${ffp.id}"/>
					</c:if>
					<c:if test="${page == 'edit'}">
						<c:if test="${!ffp.annule}">
							<c:if test="${autorisations.forsPrincipaux}">
								<unireg:linkTo name="" action="/fors/principal/edit.do" method="GET" params="{forId:${ffp.id}}" link_class="edit" title="Edition de for" />
								<c:if test="${ffp.dernierForPrincipalOuDebiteur}">
									<unireg:linkTo name="" action="/fors/principal/cancel.do" method="POST" params="{forId:${ffp.id}}" link_class="delete"
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

		<c:choose>
			<c:when test="${page == 'edit'}">
				<table border="0">
					<tr><td>
						<c:if test="${fn:length(command.forsFiscauxSecondaires) > command.nombreElementsTable && command.nombreElementsTable > 0}">
							<c:choose>
								<c:when test="${command.forsSecondairesPagines}">
									<unireg:linkTo name="Liste complète" action="/fiscal/edit.do" title="Liste complète des fors secondaires" params="{id:${command.tiers.numero},forsFiscauxSecondairesPagines:false}" link_class="deplier margin_right_10"/>
								</c:when>
								<c:otherwise>
									<unireg:linkTo name="Liste paginée" action="/fiscal/edit.do" title="Liste paginée des fors secondaires" params="{id:${command.tiers.numero},forsFiscauxSecondairesPagines:true}" link_class="plier margin_right_10"/>
								</c:otherwise>
							</c:choose>
						</c:if>
						<c:if test="${autorisations.forsSecondaires}">
							<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/secondaire/add.do" params="{tiersId:${command.tiersGeneral.numero}}" link_class="add margin_right_10"/>
						</c:if>
					</td></tr>
				</table>
			</c:when>
			<c:when test="${fn:length(command.forsFiscauxSecondaires) > command.nombreElementsTable && command.nombreElementsTable > 0}">
				<table border="0">
					<tr><td>
						<c:choose>
							<c:when test="${command.forsSecondairesPagines}">
								<unireg:linkTo name="Liste complète" action="/tiers/visu.do" title="Liste complète des fors secondaires" params="{id:${command.tiers.numero},forsFiscauxSecondairesPagines:false}" link_class="deplier margin_right_10"/>
							</c:when>
							<c:otherwise>
								<unireg:linkTo name="Liste paginée" action="/tiers/visu.do" title="Liste paginée des fors secondaires" params="{id:${command.tiers.numero},forsFiscauxSecondairesPagines:true}" link_class="plier margin_right_10"/>
							</c:otherwise>
						</c:choose>
					</td></tr>
				</table>
			</c:when>
		</c:choose>

		<c:if test="${not empty command.forsFiscauxSecondaires}">
			<display:table name="command.forsFiscauxSecondaires" id="ffs" pagesize="${(command.nombreElementsTable == 0 || !command.forsSecondairesPagines) ? 0 : command.nombreElementsTable}"
			               requestURI="${url}" sort="list"
			               class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

				<display:column style="width:16px;">
					<div id="ffid-${ffs.id}" class="forSecondaireIconSmall" title="For fiscal secondaire"></div>
				</display:column>
				<display:column sortable ="true" titleKey="label.genre.impot">
					<fmt:message key="option.genre.impot.${ffs.genreImpot}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.rattachement" >
					<fmt:message key="option.rattachement.${ffs.motifRattachement}" />
				</display:column>
				<display:column sortable ="true" titleKey="label.for.abrege">
					<c:choose>
						<c:when test="${ffs.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:commune ofs="${ffs.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${ffs.regDateOuverture}"/>
						</c:when>
						<c:when test="${ffs.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:commune ofs="${ffs.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${ffs.regDateOuverture}"/>
						</c:when>
						<c:when test="${ffs.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:pays ofs="${ffs.numeroForFiscalPays}" displayProperty="nomCourt" titleProperty="noOFS" date="${ffs.regDateOuverture}"/>
						</c:when>
					</c:choose>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
					<fmt:formatDate value="${ffs.dateOuverture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.ouv">
					<c:if test="${ffs.motifOuverture != null}">
						<fmt:message key="option.motif.ouverture.${ffs.motifOuverture}" />
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
					<fmt:formatDate value="${ffs.dateFermeture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.fer">
					<c:if test="${ffs.motifFermeture != null}">
						<fmt:message key="option.motif.fermeture.${ffs.motifFermeture}" />
					</c:if>
				</display:column>
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.for.gestion">
						<c:if test="${!ffs.annule}">
							<input type="checkbox" <c:if test="${ffs.forGestion}">checked</c:if> disabled="disabled">
						</c:if>
					</display:column>
				</c:if>
				<display:column class="action">
					<c:if test="${page == 'visu' }">
						<unireg:consulterLog entityNature="ForFiscal" entityId="${ffs.id}"/>
					</c:if>
					<c:if test="${page == 'edit' }">
						<c:if test="${!ffs.annule}">
							<c:if test="${autorisations.forsSecondaires}">
								<unireg:linkTo name="" action="/fors/secondaire/edit.do" method="GET" params="{forId:${ffs.id}}" link_class="edit" title="Edition de for" />
								<unireg:linkTo name="" action="/fors/secondaire/cancel.do" method="POST" params="{forId:${ffs.id}}" link_class="delete"
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

		<c:choose>
			<c:when test="${page == 'edit'}">
				<table border="0">
					<tr><td>
						<c:if test="${fn:length(command.autresForsFiscaux) > command.nombreElementsTable && command.nombreElementsTable > 0}">
							<c:choose>
								<c:when test="${command.autresForsPagines}">
									<unireg:linkTo name="Liste complète" action="/fiscal/edit.do" title="Liste complète des autres fors fiscaux" params="{id:${command.tiers.numero},autresForsFiscauxPagines:false}" link_class="deplier margin_right_10"/>
								</c:when>
								<c:otherwise>
									<unireg:linkTo name="Liste paginée" action="/fiscal/edit.do" title="Liste paginée des autres fors fiscaux" params="{id:${command.tiers.numero},autresForsFiscauxPagines:true}" link_class="plier margin_right_10"/>
								</c:otherwise>
							</c:choose>
						</c:if>
						<c:if test="${autorisations.forsAutresElementsImposables}">
							<unireg:linkTo name="Ajouter" title="Ajouter un for" action="/fors/autreelementimposable/add.do" params="{tiersId:${command.tiersGeneral.numero}}" link_class="add margin_right_10"/>
						</c:if>
					</td></tr>
				</table>
			</c:when>
			<c:when test="${fn:length(command.autresForsFiscaux) > command.nombreElementsTable && command.nombreElementsTable > 0}">
				<table border="0">
					<tr><td>
						<c:choose>
							<c:when test="${command.autresForsPagines}">
								<unireg:linkTo name="Liste complète" action="/tiers/visu.do" title="Liste complète des autres fors fiscaux" params="{id:${command.tiers.numero},autresForsFiscauxPagines:false}" link_class="deplier margin_right_10"/>
							</c:when>
							<c:otherwise>
								<unireg:linkTo name="Liste paginée" action="/tiers/visu.do" title="Liste paginée des autres fors fiscaux" params="{id:${command.tiers.numero},autresForsFiscauxPagines:true}" link_class="plier margin_right_10"/>
							</c:otherwise>
						</c:choose>
					</td></tr>
				</table>
			</c:when>
		</c:choose>

		<c:if test="${not empty command.autresForsFiscaux}">
			<display:table name="command.autresForsFiscaux" id="ffa" pagesize="${(command.nombreElementsTable == 0 || !command.autresForsPagines) ? 0 : command.nombreElementsTable}"
			               requestURI="${url}" sort="list"
			               class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

				<display:column style="width:16px;">
					<c:if test="${ffa.natureForFiscal == 'ForFiscalAutreElementImposable'}">
						<div id="ffid-${ffa.id}" class="forAutreElementImposableIconSmall"  title="For fiscal autre élément imposable"></div>
					</c:if>
					<c:if test="${ffa.natureForFiscal == 'ForFiscalAutreImpot'}">
						<div id="ffid-${ffa.id}" class="forAutreImpotIconSmall"  title="For fiscal autre impot"></div>
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.genre.impot">
					<fmt:message key="option.genre.impot.${ffa.genreImpot}"  />
				</display:column>
				<display:column sortable ="true" titleKey="label.rattachement" >
					<c:if test="${ffa.natureForFiscal != 'ForFiscalAutreImpot'}">
						<fmt:message key="option.rattachement.${ffa.motifRattachement}" />
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.for.abrege">
					<c:choose>
						<c:when test="${ffa.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' }">
							<unireg:commune ofs="${ffa.numeroForFiscalCommune}" displayProperty="nomOfficiel" titleProperty="noOFS" date="${ffa.regDateOuverture}"/>
						</c:when>
						<c:when test="${ffa.typeAutoriteFiscale == 'COMMUNE_HC' }">
							<unireg:commune ofs="${ffa.numeroForFiscalCommuneHorsCanton}" displayProperty="nomOfficielAvecCanton" titleProperty="noOFS" date="${ffa.regDateOuverture}"/>
						</c:when>
						<c:when test="${ffa.typeAutoriteFiscale == 'PAYS_HS' }">
							<unireg:pays ofs="${ffa.numeroForFiscalPays}" displayProperty="nomCourt" titleProperty="noOFS" date="${ffa.regDateOuverture}"/>
						</c:when>
					</c:choose>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouv" sortProperty="dateOuverture">
					<fmt:formatDate value="${ffa.dateOuverture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.ouv">
					<c:if test="${ffa.natureForFiscal != 'ForFiscalAutreImpot'}">
						<c:if test="${ffa.motifOuverture != null}">
							<fmt:message key="option.motif.ouverture.${ffa.motifOuverture}" />
						</c:if>
					</c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fer" sortProperty="dateFermeture">
					<fmt:formatDate value="${ffa.dateFermeture}" pattern="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable ="true" titleKey="label.motif.fer">
					<c:if test="${ffa.natureForFiscal != 'ForFiscalAutreImpot'}">
						<c:if test="${ffa.motifFermeture != null}">
							<fmt:message key="option.motif.fermeture.${ffa.motifFermeture}" />
						</c:if>
					</c:if>
				</display:column>
				<c:if test="${command.natureTiers != 'Entreprise'}">
					<display:column sortable ="true" titleKey="label.for.gestion">
						<c:if test="${!ffa.annule}">
							<input type="checkbox" <c:if test="${ffa.forGestion}">checked</c:if> disabled="disabled">
						</c:if>
					</display:column>
				</c:if>
				<display:column class="action">
					<c:if test="${page == 'visu' }">
						<unireg:consulterLog entityNature="ForFiscal" entityId="${ffa.id}"/>
					</c:if>
					<c:if test="${page == 'edit' }">
						<c:if test="${!ffa.annule}">
							<c:if test="${ffa.natureForFiscal == 'ForFiscalAutreElementImposable' && autorisations.forsAutresElementsImposables}">
								<unireg:linkTo name="" action="/fors/autreelementimposable/edit.do" method="GET" params="{forId:${ffa.id}}" link_class="edit" title="Edition de for" />
								<unireg:linkTo name="" action="/fors/autreelementimposable/cancel.do" method="POST" params="{forId:${ffa.id}}" link_class="delete"
								               title="Annulation de for" confirm="Voulez-vous vraiment annuler ce for fiscal ?"/>
							</c:if>
							<c:if test="${ffa.natureForFiscal == 'ForFiscalAutreImpot' && autorisations.forsAutresImpots}">
								<span class="button_placeholder">&nbsp;</span>
								<unireg:linkTo name="" action="/fors/autreimpot/cancel.do" method="POST" params="{forId:${ffa.id}}" link_class="delete"
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
