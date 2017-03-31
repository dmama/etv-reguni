<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.mandataires.courrier"/>
	</tiles:put>

	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${idMandant}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du mandant" />

		<fieldset>
			<legend><span><fmt:message key="label.mandataires.courrier"/></span></legend>

			<unireg:raccourciAjouter tooltip="Ajouter un nouveau mandat" display="label.bouton.ajouter" link="ajouter-list.do?idMandant=${idMandant}"/>

			<c:if test="${not empty mandats}">
				<display:table name="${mandats}" id="courrier" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnulableDateRangeDecorator">
					<display:column titleKey="label.type" style="width: 20ex;">
						<fmt:message key="option.mandat.type.${courrier.typeMandat}"/>
					</display:column>
					<display:column titleKey="label.date.debut" style="width: 12ex;">
						<unireg:regdate regdate="${courrier.dateDebut}"/>
					</display:column>
					<display:column titleKey="label.date.fin" style="width: 12ex;">
						<unireg:regdate regdate="${courrier.dateFin}"/>
					</display:column>
					<display:column titleKey="label.numero.tiers.mandataire" style="width: 15ex;">
						<unireg:numCTB numero="${courrier.idMandataire}" link="true"/>
					</display:column>
					<display:column titleKey="label.nom.raison">
						<c:out value="${courrier.nomRaisonSociale}"/>
						<c:choose>
							<c:when test="${courrier.idMandataire != null}">
								<unireg:raccourciDetail onClick="Mandataires.showDetailsMandat(${courrier.id});"/>
							</c:when>
							<c:otherwise>
								<unireg:raccourciDetail onClick="Mandataires.showDetailsAdresse(${courrier.id});"/>
							</c:otherwise>
						</c:choose>
					</display:column>
					<display:column titleKey="label.avec.copie.courriers" style="text-align: center; width: 10%;">
						<c:if test="${courrier.withCopy != null}">
							<input type="checkbox" disabled="disabled" <c:if test="${courrier.withCopy}">checked="checked"</c:if>/>
						</c:if>
					</display:column>
					<display:column titleKey="label.genre.impot" style="width: 25ex;">
						<c:out value="${courrier.libelleGenreImpot}"/>
					</display:column>
					<display:column class="action" style="width: 3ex;">
						<c:choose>
							<c:when test="${courrier.idMandataire != null}">
								<c:if test="${courrier.editable}">
									<unireg:raccourciModifier tooltip="Editer les données du mandat" link="../editer-mandat.do?idMandat=${courrier.id}"/>
								</c:if>
								<c:if test="${courrier.annulable}">
									<unireg:linkTo name="" title="Annuler le mandat" confirm="Voulez-vous réellement annuler ce mandat ?"
									               action="/mandataire/annuler-mandat.do" method="post" params="{idMandat:${courrier.id}}" link_class="delete"/>
								</c:if>
							</c:when>
							<c:otherwise>
								<c:if test="${courrier.editable}">
									<unireg:raccourciModifier tooltip="Editer les données du mandat" link="../editer-adresse.do?idAdresse=${courrier.id}"/>
								</c:if>
								<c:if test="${courrier.annulable}">
									<unireg:linkTo name="" title="Annuler le mandat" confirm="Voulez-vous réellement annuler ce mandat ?"
													action="/mandataire/annuler-adresse.do" method="post" params="{idAdresse:${courrier.id}}" link_class="delete"/>
								</c:if>
							</c:otherwise>
						</c:choose>
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
