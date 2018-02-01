<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

	<%--@elvariable id="principalView" type="ch.vd.unireg.rapport.view.SetPrincipalView"--%>
	<tiles:put name="title">
		<fmt:message key="title.set.principal">
			<fmt:param><unireg:numCTB numero="${principalView.heritierId}"/></fmt:param>
			<fmt:param><unireg:numCTB numero="${principalView.defuntId}"/></fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">

		<%--Bandeau--%>
		<table>
		<tr>
			<td id="td_tiers_gauche">
				<unireg:bandeauTiers numero="${principalView.heritierId}" titre="Héritier" showValidation="false" showEvenementsCivils="false" showLinks="false"/>
			</td>
			<td>
				<%-- Flèche du sens du rapport  --%>
				<table id="flecheSensRapport" cellpadding="0" cellspacing="0">
					<tr>
						<td style="width:1em;"></td>
						<td id="flecheGauche" class="fleche_droite_bord_gauche iepngfix"></td>
						<td id="flecheMilieu" class="fleche_milieu">Héritier principal</td>
						<td id="flecheDroite" class="fleche_droite_bord_droit iepngfix"></td>
						<td style="width:1em;"></td>
					</tr>
				</table>
			</td>
			<td id="td_tiers_droite">
				<unireg:bandeauTiers numero="${principalView.defuntId}" titre="Défunt" showValidation="false" showEvenementsCivils="false" showLinks="false"/>
			</td>
		</tr>
		</table>

		<fieldset><legend><span><fmt:message key="label.histo.heritiers.principaux" /></span></legend>
			<display:table name="principaux" id="principal" pagesize="25" class="display" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
				<display:column titleKey="label.date.debut">
					<unireg:regdate regdate="${principal.dateDebut}" />
				</display:column>
				<display:column titleKey="label.date.fin">
					<unireg:regdate regdate="${principal.dateFin}" />
				</display:column>
				<display:column titleKey="label.numero.contribuable">
					<c:set var="noctb">
						<unireg:numCTB numero="${principal.numero}"/>
					</c:set>
					<unireg:linkTo name="${noctb}" action="/tiers/visu.do" params="{id:${principal.numero}}"/>
				</display:column>
				<display:column titleKey="label.nom">
					<unireg:multiline lines="${principal.nomCourrier}"/>
				</display:column>
			</display:table>
		</fieldset>

		<form:form name="formSetPrincipal" commandName="principalView" id="formSetPrincipal">
			<fieldset><legend><span><fmt:message key="label.proprietes.election.principal" /></span></legend>
				<form:hidden path="defuntId"/>
				<form:hidden path="heritierId"/>
				<!-- Debut Rapport -->
				<table border="0">
					<unireg:nextRowClass reset="0"/>
					<tr class="<unireg:nextRowClass/>" >
						<td width="25%"><fmt:message key="label.date.debut"/>&nbsp;:</td>
						<td width="75%" colspan="3">
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="dateDebut" />
								<jsp:param name="id" value="dateDebut" />
							</jsp:include>
						</td>
					</tr>
				</table>
			</fieldset>
			<table border="0">
				<tr>
					<td width="25%">&nbsp;</td>
					<td width="25%"><input type="submit" id="maj" value="<fmt:message key="label.bouton.sauver" />"></td>
					<td width="25%">&nbsp;</td>
					<td width="25%"><unireg:RetourButton link="list.do?id=${principalView.defuntId}"/></td>
					<td width="25%">&nbsp;</td>
				</tr>
			</table>

		</form:form>
	</tiles:put>
</tiles:insert>

		
