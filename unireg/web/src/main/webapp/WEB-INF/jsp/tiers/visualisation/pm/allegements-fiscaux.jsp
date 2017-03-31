<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.allegements.fiscaux"/></span></legend>

	<unireg:setAuth var="auth" tiersId="${command.tiers.numero}"/>
	<c:if test="${!command.tiers.annule && auth.allegementsFiscaux}">
		<table border="0">
			<tr><td>
				<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../allegement/edit-list.do?pmId=${command.tiers.numero}" tooltip="Modifier les allègements fiscaux" display="label.bouton.modifier"/>
				</c:if>
			</td></tr>
		</table>
	</c:if>

	<c:if test="${not empty command.allegementsFiscaux}">

		<input class="noprint" name="allg_histo" type="checkbox" onClick="Histo.toggleRowsIsHistoFromClass('allegement','allg_histo', 'histo-only');" id="allg_histo" />
		<label class="noprint" for="allg_histo"><fmt:message key="label.historique" /></label>

		<display:table name="${command.allegementsFiscaux}" id="allegement" requestURI="visu.do" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnulableDateRangeDecorator">
			<display:column sortable="true" titleKey="label.date.debut" sortProperty="dateDebut">
				<unireg:regdate regdate="${allegement.dateDebut}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.date.fin" sortProperty="dateFin">
				<unireg:regdate regdate="${allegement.dateFin}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type.impot">
				<fmt:message key="option.allegement.fiscal.type.impot.${allegement.typeImpot}"/>
			</display:column>
			<display:column sortable="true" titleKey="label.type.collectivite">
				<fmt:message key="option.allegement.fiscal.type.collectivite.${allegement.typeCollectivite}"/>
				<c:if test="${allegement.typeCollectivite == 'COMMUNE' && allegement.noOfsCommune != null}">
					&nbsp;(<unireg:commune ofs="${allegement.noOfsCommune}" displayProperty="nomOfficiel" titleProperty="noOFS"/>)
				</c:if>
			</display:column>
			<display:column titleKey="label.type">
				<c:choose>
					<c:when test="${allegement.typeCollectivite == 'COMMUNE' || allegement.typeCollectivite == 'CANTON'}">
						<c:if test="${allegement.typeICC != null}">
							<fmt:message key="option.allegement.icc.type.${allegement.typeICC}"/>
						</c:if>
					</c:when>
					<c:when test="${allegement.typeCollectivite == 'CONFEDERATION'}">
						<c:if test="${allegement.typeIFD != null}">
							<fmt:message key="option.allegement.ifd.type.${allegement.typeIFD}"/>
						</c:if>
					</c:when>
					<c:otherwise>
						&nbsp;
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column sortable="true" titleKey="label.allegements.fiscaux.pourcentage" sortProperty="pourcentage">
				<c:choose>
					<c:when test="${allegement.pourcentage != null}">
						<fmt:formatNumber maxIntegerDigits="3" minIntegerDigits="1" maxFractionDigits="2" minFractionDigits="0" value="${allegement.pourcentage}"/>&nbsp;%
					</c:when>
					<c:otherwise>
						<span><fmt:message key="label.allegement.montant"/></span>
					</c:otherwise>
				</c:choose>
			</display:column>
			<display:column class="action">
				<unireg:consulterLog entityNature="AllegementFiscal" entityId="${allegement.id}"/>
			</display:column>
		</display:table>

	</c:if>

</fieldset>

<script type="text/javascript">

	$(function() {
		Histo.toggleRowsIsHistoFromClass('allegement','allg_histo', 'histo-only');
	});

</script>
