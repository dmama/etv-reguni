<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.ajout.mandataire"/>
	</tiles:put>

	<tiles:put name="body">

		<table style="border: 0;">
			<tr>
				<td style="width: 42%;">
					<unireg:nextRowClass reset="1"/>
					<unireg:bandeauTiers numero="${idMandant}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable mandant" />
				</td>
				<td>
					<table id="flecheRemplacement" cellpadding="0" cellspacing="0">
						<tr>
							<td style="width:1em;"/>
							<td id="flecheGauche" class="fleche_droite_bord_gauche iepngfix"/>
							<td id="flecheMilieu" class="fleche_milieu"><fmt:message key="label.confie.un.mandat.a"/></td>
							<td id="flecheDroite" class="fleche_droite_bord_droit iepngfix"/>
							<td style="width:1em;"/>
						</tr>
					</table>
				</td>
				<td style="width: 42%;">
					<unireg:nextRowClass reset="1"/>
					<c:set var="typeAdresse">
						<c:choose>
							<c:when test="${mode == 'courrier'}">REPRESENTATION</c:when>
							<c:otherwise>COURRIER</c:otherwise>
						</c:choose>
					</c:set>
					<unireg:bandeauTiers numero="${idMandataire}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" typeAdresse="${typeAdresse}" titre="Caractéristiques du contribuable mandataire" />
				</td>
			</tr>
		</table>

		<form:form method="post" id="formDonneesMandat" modelAttribute="donneesMandat" action="ajouter-tiers-mandataire.do">
			<!-- inclusion du tableau sur les données additionnelles d'un mandat -->
			<jsp:include page="add-donnees-mandat.jsp"/>

			<!-- Debut Boutons -->
			<table>
				<tr>
					<td style="text-align: center;">
						<input type="submit" value="<fmt:message key="label.param.add"/>"/>
					</td>
					<td>
						<unireg:buttonTo name="Retour" action="/mandataire/${mode}/ajouter-list.do" method="get" params="{idMandant:${idMandant}}" />
					</td>
				</tr>
			</table>

		</form:form>

	</tiles:put>
</tiles:insert>
