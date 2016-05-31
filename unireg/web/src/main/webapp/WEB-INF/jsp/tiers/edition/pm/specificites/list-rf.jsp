<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.regimes.fiscaux">
			<fmt:param value="${portee}"/>
		</fmt:message>
	</tiles:put>
	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${pmId}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable"/>

		<fieldset>
			<legend><span><fmt:message key="label.regimes.fiscaux.${portee}"/></span></legend>

			<table border="0">
				<tr><td>
					<unireg:linkTo name="Ajouter" action="/regimefiscal/add.do" method="get" params="{pmId:${pmId},portee:'${portee}'}" title="Ajouter un régime fiscal ${portee}" link_class="add noprint"/>
				</td></tr>
			</table>

			<c:if test="${not empty regimes}">
				<display:table name="regimes" id="rf" class="display" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator" requestURI="/regimefiscal/edit-list.do" sort="list">
					<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut" style="width: 20%;">
						<unireg:regdate regdate="${rf.dateDebut}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin" style="width: 20%;">
						<unireg:regdate regdate="${rf.dateFin}"/>
					</display:column>
					<display:column sortable="true" titleKey="label.type">
						${rf.type.libelle}
					</display:column>
					<display:column class="action" style="width: 10%;">
						<c:if test="${!rf.annule}">
							<c:if test="${rf.dateFin == null}">
								<unireg:linkTo name="" action="/regimefiscal/edit.do" method="GET" params="{rfId:${rf.id}}" link_class="edit" title="Fermeture de régime fiscal" />
							</c:if>
							<c:if test="${rf.last}">
								<unireg:linkTo name="" action="/regimefiscal/cancel.do" method="POST" params="{rfId:${rf.id}}" link_class="delete"
								               title="Annulation de régime fiscal" confirm="Voulez-vous vraiment annuler ce régime fiscal ${portee} ?"/>
							</c:if>
						</c:if>
					</display:column>
				</display:table>
			</c:if>

		</fieldset>

		<!-- Scripts -->

		<!-- Debut Bouton -->
		<table>
			<tr>
				<td>
					<unireg:buttonTo name="Retour" action="/tiers/visu.do" method="get" params="{id:${pmId}}"/>
				</td>
			</tr>
		</table>
		<!-- Fin Bouton -->

	</tiles:put>

</tiles:insert>