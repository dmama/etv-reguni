<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="idMandant" type="java.lang.Long"--%>
<%--@elvariable id="mandats" type="java.util.List<ch.vd.unireg.mandataire.MandatairePerceptionEditView>"--%>
<%--@elvariable id="accesMandataires" type="ch.vd.unireg.mandataire.AccesMandatairesView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.mandataires.perception"/>
	</tiles:put>

	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${idMandant}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du mandant" />

		<fieldset>
			<legend><span><fmt:message key="label.mandataires.perception"/></span></legend>

			<c:if test="${accesMandataires.hasTiersPerceptionInEdition()}">
				<unireg:raccourciAjouter tooltip="Ajouter un nouveau mandat" display="label.bouton.ajouter" link="ajouter-list.do?idMandant=${idMandant}"/>
			</c:if>

			<c:if test="${not empty mandats}">
				<display:table name="${mandats}" id="mandat" requestURI="visu.do" class="display" decorator="ch.vd.unireg.decorator.TableAnnulableDateRangeDecorator">
					<display:column titleKey="label.type" style="width: 20ex;">
						<fmt:message key="option.mandat.type.${mandat.typeMandat}"/>
					</display:column>
					<display:column titleKey="label.date.debut" style="width: 12ex;">
						<unireg:regdate regdate="${mandat.dateDebut}"/>
					</display:column>
					<display:column titleKey="label.date.fin" style="width: 12ex;">
						<unireg:regdate regdate="${mandat.dateFin}"/>
					</display:column>
					<display:column titleKey="label.numero.tiers.mandataire" style="width: 15ex;">
						<unireg:numCTB numero="${mandat.idMandataire}" link="true"/>
					</display:column>
					<display:column titleKey="label.nom.raison">
						<c:out value="${mandat.nomRaisonSociale}"/>
					</display:column>
					<display:column titleKey="label.complement.numeroIBAN">
						<c:out value="${mandat.iban}"/>
					</display:column>
					<display:column class="action" style="width: 3ex;">
						<c:if test="${accesMandataires.hasTiersPerceptionInEdition()}">
							<c:if test="${mandat.editable}">
								<unireg:raccourciModifier tooltip="Editer les données du mandat" link="../editer-mandat.do?idMandat=${mandat.id}"/>
							</c:if>
							<c:if test="${mandat.annulable}">
								<unireg:linkTo name="" title="Annuler le mandat" confirm="Voulez-vous réellement annuler ce mandat ?"
								               action="/mandataire/annuler-mandat.do" method="post" params="{idMandat:${mandat.id}}" link_class="delete"/>
							</c:if>
						</c:if>
					</display:column>
				</display:table>
			</c:if>
		</fieldset>

		<!-- Debut Bouton -->
		<table>
			<tr><td>
				<unireg:buttonTo name="Retour" action="/tiers/visu.do" method="get" params="{id:${idMandant}}" />
			</td></tr>
		</table>

	</tiles:put>
</tiles:insert>
