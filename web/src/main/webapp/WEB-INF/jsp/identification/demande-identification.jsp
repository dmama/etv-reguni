<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<!-- Les données de la demande d'identification doivent être présentes sous le nom "messageData" -->
<%--@elvariable id="messageData" type="ch.vd.unireg.identification.contribuable.view.DemandeIdentificationView"--%>

<!-- Debut Caracteristiques identification -->
<fieldset class="information" id="info-demande">
	<legend><span>
		<fmt:message key="caracteristiques.message.identification" />
	</span></legend>
	<table cellspacing="0" cellpadding="0" border="0">
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.type.message" />&nbsp;:</td>
			<td>
				<c:if test="${messageData.typeMessage != null }">
					<fmt:message key="option.type.message.${messageData.typeMessage}" />
				</c:if>
			</td>
			<td><fmt:message key="label.navs11" />&nbsp;:</td>
			<td><c:out value="${messageData.navs11}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.periode.fiscale" />&nbsp;:</td>
			<td><c:out value="${messageData.periodeFiscale}"/></td>
			<td><fmt:message key="label.navs13" />&nbsp;:</td>
			<td>
				<c:out value="${messageData.navs13}"/>
				<c:if test="${messageData.navs13Upi != null}">
					<span id="avs13upi-${messageData.id}" class="staticTip upiAutreNavs13">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</span>
					<div id="avs13upi-${messageData.id}-tooltip" style="display:none;">
						<fmt:message key="warning.identification.navs13.upi">
							<fmt:param value="${messageData.navs13Upi}"/>
						</fmt:message>
					</div>
				</c:if>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.emetteur" />&nbsp;:</td>
			<td><c:out value="${messageData.emetteurId}"/></td>
			<td><fmt:message key="label.nom" />&nbsp;:</td>
			<td><c:out value="${messageData.nom}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td><fmt:message key="label.date.message" />&nbsp;:</td>
			<td>
				<c:if test="${messageData.dateMessage != null }">
					<fmt:formatDate value="${messageData.dateMessage}" pattern="dd.MM.yyyy"/>
				</c:if>
			</td>
			<td><fmt:message key="label.prenoms" />&nbsp;:</td>
			<td>${messageData.prenoms}</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<c:if test="${messageData.typeMessage == 'CS_EMPLOYEUR'}">
					<fmt:message key="label.transmetteur.message"/>
				</c:if>
			</td>
			<td>
				<c:if test="${messageData.typeMessage == 'CS_EMPLOYEUR'}">
					<c:out value="${messageData.transmetteur}"/>
				</c:if>
			</td>
			<td><fmt:message key="label.date.naissance" />&nbsp;:</td>
			<td>
				<c:if test="${messageData.dateNaissance != null }">
					<unireg:date date="${messageData.dateNaissance}" />
				</c:if>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td>
				<c:if test="${messageData.typeMessage == 'CS_EMPLOYEUR'}">
					<fmt:message key="label.montant.message"/>
				</c:if>
			</td>
			<td>
				<c:if test="${messageData.typeMessage == 'CS_EMPLOYEUR'}">
					<c:out value="${messageData.montant}"/>
				</c:if>
			</td>
			<td><fmt:message key="label.sexe" />&nbsp;:</td>
			<td>
				<c:if test="${messageData.sexe != null }">
					<fmt:message key="option.sexe.${messageData.sexe}" />
				</c:if>
			</td>
		</tr>
		<tr class="<unireg:nextRowClass/>" >
			<td colspan="2">
				<c:if test="${messageData.documentUrl != null}">
                    <unireg:buttonTo name="label.bouton.identification.visualiser" action="/identification/gestion-messages/voirmessage.do" method="get" params="{id:${messageData.id}}"/>
				</c:if>
				&nbsp;
			</td>
			<td><fmt:message key="label.adresse" />&nbsp;:</td>
			<td>
				<c:if test="${messageData.rue != null }">
					<c:out value="${messageData.rue}"/>
				</c:if>
			</td>
		</tr>
		<c:if test="${messageData.npa != null }">
			<tr class="<unireg:nextRowClass/>" >
				<td colspan="2"></td>
				<td></td>
				<td><c:out value="${messageData.npa}"/></td>
			</tr>
		</c:if>
		<c:if test="${messageData.npaEtranger != null }">
			<tr class="<unireg:nextRowClass/>" >
				<td colspan="2"></td>
				<td></td>
				<td><c:out value="${messageData.npaEtranger}"/></td>
			</tr>
		</c:if>
		<c:if test="${messageData.lieu != null }">
			<tr class="<unireg:nextRowClass/>" >
				<td colspan="2"></td>
				<td></td>
				<td><c:out value="${messageData.lieu}"/></td>
			</tr>
		</c:if>
		<c:if test="${messageData.pays != null }">
			<tr class="<unireg:nextRowClass/>" >
				<td colspan="2"></td>
				<td></td>
				<td><c:out value="${messageData.pays}"/></td>
			</tr>
		</c:if>
	</table>

	<fieldset>
		<legend><fmt:message key="label.etat.courant"/></legend>
		<table>
			<tr class="<unireg:nextRowClass/>">
				<td width="25%"><fmt:message key="label.etat.message" />&nbsp;:</td>
				<td width="25%">
					<c:if test="${messageData.etatMessage != null }">
						<fmt:message key="option.etat.message.${messageData.etatMessage}" />
					</c:if>
				</td>
				<td colspan="2">&nbsp;</td>
			</tr>
			<tr class="<unireg:nextRowClass/>">
				<td><fmt:message key="label.commentaire.traitement"/>&nbsp;:</td>
				<td colspan="3"><em><c:out value="${messageData.commentaireTraitement}"/></em></td>
			</tr>
		</table>
	</fieldset>

</fieldset>

<script type="text/javascript" language="javascript">
	$(function() {
		Tooltips.activate_static_tooltips($('#info-demande'));
	});
</script>

<!-- Fin Caracteristiques identification -->