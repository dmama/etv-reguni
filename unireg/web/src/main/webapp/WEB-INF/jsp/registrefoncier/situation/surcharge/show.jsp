<%@ taglib prefix="util" uri="http://www.unireg.com/uniregTagLib" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
	</tiles:put>

	<%--@elvariable id="currentSituation" type="ch.vd.uniregctb.registrefoncier.situation.surcharge.SituationFullView"--%>
	<tiles:put name="title">
		<fmt:message key="title.selection.fraction.commune">
			<fmt:param>${currentSituation.noParcelle}</fmt:param>
			<fmt:param>${currentSituation.communeRF.nom}</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

		<unireg:linkTo name="<< Retour à la liste" action="${currentSituation.listAction}"/>

		<fieldset class="information">
			<legend><span><fmt:message key="title.details.situation" /></span></legend>

			<%--@elvariable id="urlGeoVD" type="java.lang.Class"--%>
			<table border="0">
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.id.situation.rf" />&nbsp;:</td>
					<td width="25%">${currentSituation.id}</td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.commune.faitiere.rf" />&nbsp;:</td>
					<td width="25%"><c:out value="${currentSituation.communeRF.nom}"/></td>
					<td width="25%"><fmt:message key="label.date.debut" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${currentSituation.dateDebut}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.parcelle"/>&nbsp;:</td>
					<td width="25%">${currentSituation.noParcelle}</td>
					<td width="25%"><fmt:message key="label.date.fin" />&nbsp;:</td>
					<td width="25%"><unireg:regdate regdate="${currentSituation.dateFin}"/></td>
				</tr>
				<tr class="<unireg:nextRowClass/>" >
					<td width="25%"><fmt:message key="label.indexes" />&nbsp;:</td>
					<td width="25%"><c:out value="${currentSituation.indexes}"/></td>
					<td width="25%"><fmt:message key="label.liens.externes" />&nbsp;:</td>
					<td width="25%">
						<a href="${currentSituation.immeuble.urlIntercapi}" target="_blank"><fmt:message key="label.lien.capitastra"/></a>
						<c:if test="${urlGeoVD != null}"> / <a href="${urlGeoVD}" target="_blank"><fmt:message key="label.lien.geovd"/></a></c:if>
					</td>
				</tr>
			</table>
		</fieldset>

		<fieldset class="information">
			<legend><span><fmt:message key="title.surcharge.fraction" /></span></legend>

			<%--@elvariable id="surcharge" type="ch.vd.uniregctb.registrefoncier.situation.surcharge.SituationSurchargeView"--%>
			<%--@elvariable id="mapFaitieresFractions" type="java.util.Map"--%>
			<%--@elvariable id="fraction" type="ch.vd.uniregctb.tiers.view.CommuneView"--%>
			<form:form method="post" commandName="surcharge" action="${currentSituation.showAction}" htmlEscape="false">
				<form:hidden path="situationId"/>
				<table border="0">
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.fraction.commune" />&nbsp;:</td>
						<td width="75%">
							<form:select path="noOfsSurcharge">
								<form:option value="">-- Choisir --</form:option>
								<form:options items="${mapFaitieresFractions[currentSituation.communeRF.noOfs]}" itemLabel="nomEtValidite" itemValue="noOfs"/>
							</form:select>
							<span class="mandatory">*</span>
							<form:errors path="noOfsSurcharge" cssClass="error"/>

						</td>
					</tr>
					<tr>
						<td width="25%"></td>
						<td width="75%" style="padding-top: 10px">
							<input class="submit" type="submit" value="Appliquer" style="margin-right: 10px"/>
							<unireg:buttonTo name="Annuler" method="get" action="${currentSituation.listAction}"/>
						</td>
					</tr>
				</table>

			</form:form>
		</fieldset>

		<%--@elvariable id="otherSituations" type="java.util.List"--%>
		<c:if test="${fn:length(otherSituations) > 0}">
			<fieldset class="information">
				<legend><span><fmt:message key="title.autres.situations" /></span></legend>

				<display:table name="otherSituations" id="situation" class="display_table" requestURI="/registrefoncier/situation/surcharge/list.do" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">
					<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucune.situation.trouve"/></span></display:setProperty>
					<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.une.situation.trouve"/></span></display:setProperty>
					<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.situations.trouvees"/></span></display:setProperty>
					<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.situations.trouvees"/></span></display:setProperty>

					<display:column titleKey="label.id.situation.rf" sortable="true" sortName="id" property="id"/>
					<display:column titleKey="label.date.debut" sortable="true" sortName="dateDebut">
						<unireg:regdate regdate="${situation.dateDebut}"/>
					</display:column>
					<display:column titleKey="label.date.fin" sortable="true" sortName="dateFin">
						<unireg:regdate regdate="${situation.dateFin}"/>
					</display:column>
					<display:column titleKey="label.parcelle" sortable="true" sortName="noParcelle" property="noParcelle"/>
					<display:column titleKey="label.indexes" property="indexes"/>
					<display:column titleKey="label.commune" sortable="true" sortName="communeRF.nom">
						<c:out value="${situation.communeRF.nom}"/>
					</display:column>
					<display:column titleKey="label.fraction.surchargee" sortable="true" sortName="communeSurchargee.nom">
						<c:out value="${situation.communeSurchargee.nom}"/>
					</display:column>
				</display:table>

			</fieldset>
		</c:if>


	</tiles:put>
</tiles:insert>
