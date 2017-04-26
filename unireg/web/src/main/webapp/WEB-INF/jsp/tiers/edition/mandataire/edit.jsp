<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@page import="ch.vd.uniregctb.common.LengthConstants"%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edition.mandat"/>
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
					<c:choose>
						<c:when test="${mandat.idTiersMandataire != null}">
							<unireg:nextRowClass reset="1"/>
							<c:set var="typeAdresse">
								<c:choose>
									<c:when test="${mode == 'courrier'}">REPRESENTATION</c:when>
									<c:otherwise>COURRIER</c:otherwise>
								</c:choose>
							</c:set>
							<unireg:bandeauTiers numero="${mandat.idTiersMandataire}" showAvatar="false" showValidation="false" showEvenementsCivils="false" showLinks="false" titre="Caractéristiques du contribuable mandataire" typeAdresse="${typeAdresse}" />
						</c:when>
						<c:otherwise>
							<fieldset class="information">
								<legend><span><fmt:message key="label.mandataire"/></span></legend>
								<unireg:adresseMandataire idAdresse="${mandat.idAdresse}" displayMode="TABLE"/>
							</fieldset>
						</c:otherwise>
					</c:choose>
				</td>
			</tr>
		</table>

		<form:form commandName="editMandat" id="editMandatForm">

			<c:set var="lengthnumcompte" value="<%=LengthConstants.TIERS_NUMCOMPTE%>" scope="request" />
			<c:set var="lengthpersonne" value="<%=LengthConstants.TIERS_PERSONNE%>" scope="request" />
			<c:set var="lengthtel" value="<%=LengthConstants.TIERS_NUMTEL%>" scope="request" />

			<input type="hidden" name="idMandant" value="${idMandant}"/>
			<form:hidden path="typeMandat"/>

			<fieldset>
				<legend><span><fmt:message key="label.donnees.mandat"/></span></legend>
				<unireg:nextRowClass reset="0"/>
				<table>
					<tr class="<unireg:nextRowClass/>" >
						<td style="width: 15%;"><fmt:message key="label.date.ouverture" />&nbsp;:</td>
						<td style="width: 35%;">
							<unireg:regdate regdate="${mandat.dateDebut}"/>
							<form:hidden path="dateDebut"/>
						</td>
						<td style="width: 15%;"><fmt:message key="label.date.fermeture" />&nbsp;:</td>
						<td style="width: 35%;">
							<c:choose>
								<c:when test="${mandat.dateFin == null}">
									<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
										<jsp:param name="path" value="dateFin" />
										<jsp:param name="id" value="dateFin" />
									</jsp:include>
								</c:when>
								<c:otherwise>
									<unireg:regdate regdate="${mandat.dateFin}"/>
									<form:hidden path="dateFin"/>
								</c:otherwise>
							</c:choose>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td><fmt:message key="label.type.mandat"/>&nbsp;:</td>
						<td><fmt:message key="option.mandat.type.${mandat.typeMandat}"/></td>
						<c:choose>
							<c:when test="${mandat.typeMandat == 'TIERS'}">
								<td><fmt:message key="label.complement.numeroIBAN"/>&nbsp;:</td>
								<td>
									<form:input path="iban" size="${lengthnumcompte}" maxlength="${lengthnumcompte}"/>
									<span class="mandatory">*</span>
									<span class="jTip formInfo" title="<c:url value="/htm/iban.htm?width=375"/>" id="tipIban">?</span>
									<form:errors path="iban" cssClass="error"/>
								</td>
							</c:when>
							<c:otherwise>
								<td><fmt:message key="label.avec.copie.courriers"/>&nbsp;:</td>
								<td><form:checkbox path="withCopy"/></td>
							</c:otherwise>
						</c:choose>
					</tr>
					<c:if test="${mandat.typeMandat == 'GENERAL' || mandat.typeMandat == 'SPECIAL'}">
						<tr class="<unireg:nextRowClass/> mdt-gen mdt-spec">
							<td><fmt:message key="label.prenom.contact"/>&nbsp;:</td>
							<td><form:input path="prenomPersonneContact" size="40" maxlength="${lengthpersonne}"/></td>
							<td><fmt:message key="label.nom.contact"/>&nbsp;:</td>
							<td><form:input path="nomPersonneContact" size="40" maxlength="${lengthpersonne}"/></td>
						</tr>
						<tr class="<unireg:nextRowClass/> mdt-gen mdt-spec">
							<td><fmt:message key="label.no.tel.contact"/>&nbsp;:</td>
							<td><form:input path="noTelContact" size="25" maxlength="${lengthtel}"/></td>
							<c:choose>
								<c:when test="${mandat.typeMandat == 'SPECIAL'}">
									<td><fmt:message key="label.genre.impot"/>&nbsp;:</td>
									<td><c:out value="${mandat.libelleGenreImpot}"/></td>
								</c:when>
								<c:otherwise>
									<td colspan="2">&nbsp;</td>
								</c:otherwise>
							</c:choose>
						</tr>
					</c:if>
				</table>
			</fieldset>

			<!-- Debut Boutons -->
			<table>
				<tr>
					<td style="text-align: center;">
						<input type="submit" value="<fmt:message key="label.param.edit"/>" onclick="return confirm('Voulez-vous réellement modifier ces données de mandat ?');"/>
					</td>
					<td>
						<unireg:buttonTo name="Retour" action="/mandataire/${mode}/edit-list.do" method="get" params="{ctbId:${idMandant}}" />
					</td>
				</tr>
			</table>

		</form:form>

	</tiles:put>
</tiles:insert>
