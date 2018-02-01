<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="idMandant" type="java.lang.Long"--%>
<%--@elvariable id="mandats" type="java.util.List<ch.vd.unireg.mandataire.MandataireCourrierEditView>"--%>
<%--@elvariable id="accesMandataires" type="ch.vd.unireg.mandataire.AccesMandatairesView"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<c:choose>
			<c:when test="${accesMandataires.hasGeneralInEdition() && accesMandataires.hasSpecialInEdition()}">
				<fmt:message key="title.edition.mandataires.courrier"/>
			</c:when>
			<c:when test="${accesMandataires.hasGeneralInEdition()}">
				<fmt:message key="title.edition.mandataires.courrier.general"/>
			</c:when>
			<c:when test="${accesMandataires.hasSpecialInEdition()}">
				<fmt:message key="title.edition.mandataires.courrier.special"/>
			</c:when>
			<c:otherwise>
				<fmt:message key="title.edition.mandataires.courrier"/>
			</c:otherwise>
		</c:choose>
	</tiles:put>

	<tiles:put name="body">

		<unireg:nextRowClass reset="1"/>
		<unireg:bandeauTiers numero="${idMandant}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du mandant" />

		<fieldset>
			<c:choose>
				<c:when test="${accesMandataires.hasGeneralInEdition() && accesMandataires.hasSpecialInEdition()}">
					<legend><span><fmt:message key="label.mandataires.courrier"/></span></legend>
				</c:when>
				<c:when test="${accesMandataires.hasGeneralInEdition()}">
					<legend><span><fmt:message key="label.mandataires.courrier.general"/></span></legend>
				</c:when>
				<c:when test="${accesMandataires.hasSpecialInEdition()}">
					<legend><span><fmt:message key="label.mandataires.courrier.special"/></span></legend>
				</c:when>
				<c:otherwise>
					<legend><span><fmt:message key="label.mandataires.courrier"/></span></legend>
				</c:otherwise>
			</c:choose>

			<c:if test="${accesMandataires.hasGeneralInEdition() || accesMandataires.hasSpecialInEdition()}">
				<unireg:raccourciAjouter tooltip="Ajouter un nouveau mandat" display="label.bouton.ajouter" link="ajouter-list.do?idMandant=${idMandant}"/>
			</c:if>

			<c:if test="${not empty mandats}">
				<display:table name="${mandats}" id="courrier" requestURI="visu.do" class="display" decorator="ch.vd.unireg.decorator.TableAnnulableDateRangeDecorator">
					<display:column titleKey="label.type" style="width: 30ex;">
						<fmt:message key="option.mandat.type.${courrier.typeMandat}"/>
						<c:if test="${courrier.libelleGenreImpot != null}">
							(<c:out value="${courrier.libelleGenreImpot}"/>)
						</c:if>
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
					<display:column class="action" style="width: 3ex;">
						<c:if test="${(courrier.typeMandat == 'GENERAL' && accesMandataires.hasGeneralInEdition()) || (courrier.typeMandat == 'SPECIAL' && accesMandataires.hasSpecialInEdition(courrier.codeGenreImpot))}">
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
