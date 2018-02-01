<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<!-- Debut Etiquettes -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>
<c:if test="${autorisations.etiquettes}">
	<table border="0">
		<tr>
			<td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../etiquette/edit-list.do?tiersId=${command.tiers.numero}" tooltip="Modifier les étiquettes" display="label.bouton.modifier"/>
				</c:if>
			</td>
		</tr>
	</table>
</c:if>

<fieldset>
	<c:choose>
		<c:when test="${command.natureTiers == 'MenageCommun'}">
			<legend><span>
				<fmt:message key="label.etiquettes.tiers.nomme">
					<fmt:param value="${command.nomPrenomPrincipal}"/>
				</fmt:message>
			</span></legend>
		</c:when>
		<c:otherwise>
			<legend><span><fmt:message key="label.etiquettes" /></span></legend>
		</c:otherwise>
	</c:choose>

	<input class="noprint" name="etiq-histo-prn" type="checkbox" <c:if test="${command.labelsHisto}">checked</c:if> onClick="window.location.href = App.toggleBooleanParam(window.location, 'labelsHisto', true);" id="etiq-histo-prn" />
	<label class="noprint" for="etiq-histo-prn"><fmt:message key="label.historique" /></label>

	<c:if test="${not empty command.etiquettes}">

		<unireg:nextRowClass reset="1"/>
		<display:table name="command.etiquettes" id="etiquette" requestURI="/tiers/visu.do" htmlId="etiq-prn" sort="list" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			<display:column sortable="true" titleKey="label.libelle" sortProperty="libelle" style="width: 20%;">
				<c:out value="${etiquette.libelle}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 15%;">
				<unireg:regdate regdate="${etiquette.dateDebut}" format="dd.MM.yyyy"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 15%;">
				<unireg:regdate regdate="${etiquette.dateFin}" format="dd.MM.yyyy"/>
			</display:column>
			<display:column sortable="false" titleKey="label.commentaire">
				<c:out value="${etiquette.commentaire}"/>
			</display:column>
			<display:column class="action" style="width: 10%;">
				<unireg:consulterLog entityNature="Etiquette" entityId="${etiquette.id}"/>
			</display:column>

			<display:setProperty name="paging.banner.all_items_found" value=""/>
			<display:setProperty name="paging.banner.one_item_found" value=""/>
		</display:table>

	</c:if>

</fieldset>

<!-- Cas du conjoint pour les ménages communs -->
<c:if test="${command.natureTiers == 'MenageCommun' && command.nomPrenomConjoint != null}">

	<fieldset>
		<legend><span>
			<fmt:message key="label.etiquettes.tiers.nomme">
				<fmt:param value="${command.nomPrenomConjoint}"/>
			</fmt:message>
		</span></legend>

		<input class="noprint" name="etiq-histo-cjt" type="checkbox" <c:if test="${command.labelsConjointHisto}">checked</c:if> onClick="window.location.href = App.toggleBooleanParam(window.location, 'labelsConjointHisto', true);" id="etiq-histo-cjt" />
		<label class="noprint" for="etiq-histo-cjt"><fmt:message key="label.historique" /></label>

		<c:if test="${not empty command.etiquettesConjoint}">

			<unireg:nextRowClass reset="1"/>
			<display:table name="command.etiquettesConjoint" id="etiquette" requestURI="/tiers/visu.do" htmlId="etiq-cjt" sort="list" class="display" decorator="ch.vd.unireg.decorator.TableAnnulableDateRangeDecorator">
				<display:column sortable="true" titleKey="label.libelle" sortProperty="libelle" style="width: 20%;">
					<c:out value="${etiquette.libelle}"/>
				</display:column>
				<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 15%;">
					<unireg:regdate regdate="${etiquette.dateDebut}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 15%;">
					<unireg:regdate regdate="${etiquette.dateFin}" format="dd.MM.yyyy"/>
				</display:column>
				<display:column sortable="false" titleKey="label.commentaire">
					<c:out value="${etiquette.commentaire}"/>
				</display:column>
				<display:column class="action" style="width: 10%;">
					<unireg:consulterLog entityNature="Etiquette" entityId="${etiquette.id}"/>
				</display:column>

				<display:setProperty name="paging.banner.all_items_found" value=""/>
				<display:setProperty name="paging.banner.one_item_found" value=""/>
			</display:table>

		</c:if>

	</fieldset>

</c:if>

<!-- Fin Etiquettes -->
		

		

		