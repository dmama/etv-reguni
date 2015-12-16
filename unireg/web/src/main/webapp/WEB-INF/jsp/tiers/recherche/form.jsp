<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="typeRecherche" value="${param.typeRecherche}" />
<c:set var="prefixeEffacer" value="${param.prefixeEffacer}" />
<c:set var="paramsEffacer">
	<c:choose>
		<c:when test="${param.paramsEffacer != null && param.paramsEffacer != ''}">
			{url_memorize:false,${param.paramsEffacer}}
		</c:when>
		<c:otherwise>
			{url_memorize:false}
		</c:otherwise>
	</c:choose>
</c:set>
<table>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.numero.tiers" />&nbsp;:</td>
		<td>
			<form:input  path="numeroFormatte" id="numeroFormatte" cssClass="number"/>
			<form:errors path="numeroFormatte" cssClass="error"/>
		</td>
		<c:if test="${typeRecherche == 'principale' }">
			<td width="12px"></td>
			<td style="vertical-align:middle; text-align:right;" width="2%"><a href="#" onclick="return Search.toogleMode();" style="font-size:11px">recherche simple</a></td>
		</c:if>
		<c:if test="${typeRecherche != 'principale' }">
			<td colspan="2">&nbsp;</td>
		</c:if>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td>
			<c:if test="${typeRecherche == 'principale' || typeRecherche == 'rt-debiteur' || typeRecherche == 'activation'}">
				<fmt:message key="label.nom.raison" />&nbsp;:
			</c:if>
			<c:if test="${		typeRecherche == 'couple' || typeRecherche == 'deces' 
							|| 	typeRecherche == 'separation' || typeRecherche == 'rt-sourcier'
							|| 	typeRecherche == 'annulationCouple' || 	typeRecherche == 'annulationDeces'
							|| 	typeRecherche == 'annulationSeparation' || typeRecherche == 'fusion-non-habitant'
							|| 	typeRecherche == 'fusion-habitant' || typeRecherche == 'rapport' 
							|| typeRecherche == 'acces' || typeRecherche == 'identification'}">
				<fmt:message key="label.nom.prenom" />&nbsp;:
			</c:if>
		</td>
		<td>
			<form:select path="typeRechercheDuNom" items="${typesRechercheNom}" />
		</td>
		<td colspan="2">
			<form:input  path="nomRaison" id="nomRaison" cssErrorClass="input-with-errors" size ="65" />
			<form:errors path="nomRaison" cssClass="error"/>
		</td>
	</tr>
	<tr class="<unireg:nextRowClass/>" >
		<td><fmt:message key="label.localite.postale.suisse" /> / <fmt:message key="label.pays.etranger" />&nbsp;:</td>
		<td>
			<form:input path="localiteOuPays" id="localiteOuPays" />
			<script>
				$(function() {
					Autocomplete.infra('localiteOuPays', '#localiteOuPays', true);
				});
			</script>
		</td>
		<td>&nbsp;</td>
		<td>&nbsp;</td>
	</tr>
	<c:if test="${	(typeRecherche == 'principale') || (typeRecherche == 'rt-sourcier') || (typeRecherche == 'rt-debiteur') 
				|| 	(typeRecherche == 'rapport') || (typeRecherche == 'activation') || (typeRecherche == 'identification')}">
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.for"/></td>
			<td>
				<form:input path="forAll" id="forAll" />
				<br>
				<form:errors path="forAll" cssClass="error"/>
				<form:hidden path="noOfsFor" id="noOfsFor" />
				<script>
					$(function() {
						Autocomplete.infra('communeOuPays', '#forAll', true, function(item) {
							$('#noOfsFor').val(item ? item.id1 : null);
						});
					});
				</script>
			</td>
			<td><fmt:message key="label.for.principal.actif" />&nbsp;:</td>
			<td><form:checkbox path="forPrincipalActif" />
			<span class="jTip formInfo" title="<c:url value="/htm/forPrincipalActif.htm?width=375"/>" id="forPrincipalActif2">?</span></td>
		</tr>
	</c:if>
	<c:if test="${typeRecherche != 'couple'}">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%"><fmt:message key="label.date.naissance" />&nbsp;:</td>
		<td width="25%">
			<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
				<jsp:param name="path" value="dateNaissance" />
				<jsp:param name="id" value="dateNaissance" />
			</jsp:include>
		</td>
		<td width="25%"><fmt:message key="label.numero.avs" />&nbsp;:</td>
		<td width="25%"><form:input path="numeroAVS" id="numeroAVS" />
		<span class="jTip formInfo" title="<c:url value="/htm/critereNAVS.htm?width=375"/>" id="numeroAVS2">?</span></td>
	</tr>
	</c:if>
	<c:if test="${typeRecherche == 'principale' }">
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.type.tiers" />&nbsp;:</td>
			<td width="25%">
				<form:select id="selectTypeTiers" path="typeTiers" onchange="typeTiers_onChange(this);">
					<form:option value=""><fmt:message key="option.TOUS" /></form:option>
					<form:option value="DEBITEUR_PRESTATION_IMPOSABLE" ><fmt:message key="option.type.tiers.DEBITEUR_PRESTATION_IMPOSABLE" /></form:option>
					<form:option value="CONTRIBUABLE_PP" ><fmt:message key="option.type.tiers.CONTRIBUABLE_PP" /></form:option>
					<form:option value="ENTREPRISE" ><fmt:message key="option.type.tiers.ENTREPRISE" /></form:option>
				</form:select>
			</td>
			<td width="25%">
				<span id="categorieDebiteurLabel"><fmt:message key="label.categorie.impot.source"/>&nbsp;:</span>
				&nbsp;
			</td>
			<td width="25%">
				<form:select id="categorieDebiteurValue" path="categorieDebiteurIs">
					<form:option value=""><fmt:message key="option.TOUTES"/></form:option>
					<form:options items="${categoriesImpotSource}"/>
				</form:select>
				&nbsp;
			</td>

			<script type="text/javascript">
					function typeTiers_onChange(selectElt) {
						var eltLabel = document.getElementById("categorieDebiteurLabel");
						var eltValue = document.getElementById("categorieDebiteurValue");
						var type = selectElt.options[selectElt.selectedIndex].value;
						if (type == 'DEBITEUR_PRESTATION_IMPOSABLE') {
							eltLabel.style.display = '';
							eltValue.style.display = '';
						}
						else {
							eltLabel.style.display = 'none';
							eltValue.style.display = 'none';
							eltValue.selectedIndex = 0;
						}
					}
					typeTiers_onChange(document.getElementById("selectTypeTiers"));
			</script>

		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.numero.ide" />&nbsp;:</td>
			<td width="25%">
				<form:input path="numeroIDE" id="numeroIDE" />
			</td>
			<td width="25%"><fmt:message key="label.mode.imposition" />&nbsp;:</td>
			<td width="25%">
				<form:select path="modeImpositionAsString">
					<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
					<form:options items="${modesImposition}"/>
				</form:select>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.forme.juridique" />&nbsp;:</td>
			<td width="25%">
				<form:select path="formeJuridiqueAsString">
					<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
					<form:options items="${formesJuridiques}"/>
				</form:select>
			</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.categorie.entreprise" />&nbsp;:</td>
			<td width="25%">
				<form:select path="categorieEntrepriseAsString">
					<form:option value="TOUS" ><fmt:message key="option.TOUS" /></form:option>
					<form:options items="${categoriesEntreprises}"/>
				</form:select>
			</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td width="25%"><fmt:message key="label.numero.symic" />&nbsp;:</td>
			<td width="25%">
				<form:input  path="noSymic" id="noSymic" />
			</td>
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
			<td width="25%"><fmt:message key="label.origine.i107" />&nbsp;:</td>
			<td width="25%"><form:checkbox path="inclureI107" /><span class="jTip formInfo" title="<c:url value="/htm/debiteurInactif.htm?width=375"/>" id="debiteurInactif">?</span></td>
			</authz:authorize>
			<td width="25%"><fmt:message key="label.inclure.tiers.annules" />&nbsp;:</td>
			<td width="25%"><form:checkbox path="inclureTiersAnnules" /></td>
			<authz:authorize ifNotGranted="ROLE_VISU_ALL">
			<td width="25%">&nbsp;</td>
			<td width="25%">&nbsp;</td>
			</authz:authorize>
		</tr>
	</c:if>
</table>
<!-- Debut Boutons -->
<table border="0">
	<tr class="<unireg:nextRowClass/>" >
		<td width="25%">&nbsp;</td>
		<td width="25%">
			<div class="navigation-action"><input type="submit" value="<fmt:message key="label.bouton.rechercher"/>" name="rechercher"/></div>
		</td>
		<td width="25%">
			<c:set var="nomBoutonEffacer"><fmt:message key="label.bouton.effacer"/></c:set>
			<div class="navigation-action"><unireg:buttonTo name="${nomBoutonEffacer}" action="${prefixeEffacer}/reset-search.do" params="${paramsEffacer}" method="get"/></div>
		</td>
		<td width="25%">&nbsp;</td>
	</tr>
</table>

<script>
	$(function() {
		Tooltips.activate_ajax_tooltips();
	});
</script>

<!-- Fin Boutons -->
