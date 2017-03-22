<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="idContribuable" type="java.lang.Long"--%>
<%--@elvariable id="demandesDegrevement" type="java.util.List<ch.vd.uniregctb.registrefoncier.DemandeDegrevementICIView>"--%>
<%--@elvariable id="degrevements" type="java.util.List<ch.vd.uniregctb.registrefoncier.DegrevementICIView>"--%>
<%--@elvariable id="exonerations" type="java.util.List<ch.vd.uniregctb.registrefoncier.ExonerationIFONCView>"--%>
<%--@elvariable id="immeuble" type="ch.vd.uniregctb.registrefoncier.ResumeImmeubleView"--%>

<unireg:setAuth var="autorisations" tiersId="${idContribuable}"/>

<span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
<fieldset>
	<legend><span><fmt:message key="label.resume.immeuble"/></span></legend>
	<table border="0">
		<unireg:nextRowClass reset="0"/>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.commune"/>&nbsp;:</td>
			<td style="width: 30%;"><span title="${immeuble.ofsCommune}"><c:out value="${immeuble.nomCommune}"/></span></td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.parcelle"/>&nbsp;:</td>
			<td style="width: 30%;"><c:out value="${immeuble.noParcelleComplet}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.nature"/>&nbsp;:</td>
			<td style="width: 30%;"><c:out value="${immeuble.nature}"/></td>
		</tr>
		<tr class="<unireg:nextRowClass/>">
			<td style="width: 15%;"><fmt:message key="label.estimation.fiscale"/>&nbsp;:</td>
			<td style="width: 30%;"><c:out value="${immeuble.estimationFiscale}"/></td>
		</tr>
	</table>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.demandes.degrevement.ici"/></span></legend>
	<c:choose>
		<c:when test="${not empty demandesDegrevement}">
			<display:table name="${demandesDegrevement}" id="demande" class="display" decorator="ch.vd.uniregctb.decorator.TableAnnuableDateRangeDecorator">
				<display:column titleKey="label.periode.fiscale">
					<c:out value="${demande.periodeFiscale}"/>
				</display:column>
				<display:column titleKey="label.date.envoi">
					<unireg:regdate regdate="${demande.dateEnvoi}"/>
					<c:choose>
						<c:when test="${demande.urlVisualisationExterneDocument != null}">
							&nbsp;<a href="#" class="pdf" title="Visualisation du courrier" onclick="VisuExterneDoc.openWindow('${demande.urlVisualisationExterneDocument}');">&nbsp;</a>
						</c:when>
						<c:when test="${demande.avecCopieConformeEnvoi}">
							&nbsp;<a href="../autresdocs/copie-conforme-envoi.do?idDoc=${demande.id}&url_memorize=false" class="pdf" id="print-envoi-${demande.id}" title="Courrier envoyé" onclick="Link.tempSwap(this, '#disabled-print-envoi-${demande.id}');">&nbsp;</a>
							<span class="pdf-grayed" id="disabled-print-envoi-${demande.id}" style="display: none;">&nbsp;</span>
						</c:when>
					</c:choose>
				</display:column>
				<display:column titleKey="label.date.delai.accorde">
					<unireg:regdate regdate="${demande.delaiRetour}"/>
				</display:column>
				<display:column titleKey="label.date.retour">
					<unireg:regdate regdate="${demande.dateRetour}"/>
				</display:column>
				<display:column titleKey="label.etat.avancement" >
					<fmt:message key="option.etat.avancement.${demande.etat}" />
					<c:choose>
						<c:when test="${demande.urlVisualisationExterneRappel != null}">
							&nbsp;<a href="#" class="pdf" title="Visualisation du courrier de rappel" onclick="VisuExterneDoc.openWindow('${demande.urlVisualisationExterneRappel}');">&nbsp;</a>
						</c:when>
						<c:when test="${demande.avecCopieConformeRappel}">
							&nbsp;<a href="../autresdocs/copie-conforme-rappel.do?idDoc=${demande.id}&url_memorize=false" class="pdf" id="print-rappel-${demande.id}" title="Rappel envoyé" onclick="Link.tempSwap(this, '#disabled-print-rappel-${docFiscal.id}');">&nbsp;</a>
							<span class="pdf-grayed" id="disabled-print-rappel-${demande.id}" style="display: none;">&nbsp;</span>
						</c:when>
					</c:choose>
				</display:column>
				<display:column class="action">
					<unireg:consulterLog entityNature="AutreDocumentFiscal" entityId="${demande.id}"/>
				</display:column>
			</display:table>
		</c:when>
		<c:otherwise>
			<span style="font-style: italic"><fmt:message key="label.aucun.formulaire.demande.degrevement"/></span>
		</c:otherwise>
	</c:choose>
</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.degrevements.ici"/></span></legend>
	<c:choose>
		<c:when test="${not empty degrevements}">
			<unireg:nextRowClass reset="1"/>
			<table class="display display_table">
				<thead>
				<tr>
					<th rowspan="2"><fmt:message key="label.date.debut"/></th>
					<th rowspan="2"><fmt:message key="label.date.fin"/></th>
					<th colspan="5" style="text-align: center;"><fmt:message key="label.donnees.location"/></th>
					<th colspan="5" style="text-align: center;"><fmt:message key="label.donnees.propre.usage"/></th>
					<th colspan="3" style="text-align: center;"><fmt:message key="label.donnees.loi.logement"/></th>
					<th rowspan="2">&nbsp;</th>
				</tr>
				<tr>
					<th><fmt:message key="label.surface"/></th>
					<th><fmt:message key="label.volume"/></th>
					<th><fmt:message key="label.revenu"/></th>
					<th><fmt:message key="label.portion.declaree"/></th>
					<th><fmt:message key="label.portion.arretee"/></th>
					<th><fmt:message key="label.surface"/></th>
					<th><fmt:message key="label.volume"/></th>
					<th><fmt:message key="label.revenu.estime"/></th>
					<th><fmt:message key="label.portion.declaree"/></th>
					<th><fmt:message key="label.portion.arretee"/></th>
					<th><fmt:message key="label.concerne.loi.logement"/></th>
					<th><fmt:message key="label.date.octroi"/></th>
					<th><fmt:message key="label.date.echeance.octroi"/></th>
				</tr>
				</thead>
				<tbody>
				<c:forEach items="${degrevements}" var="degrevement">
					<tr class="<unireg:nextRowClass/>">
						<td><unireg:regdate regdate="${degrevement.dateDebut}"/></td>
						<td><unireg:regdate regdate="${degrevement.dateFin}"/></td>
						<c:choose>
							<c:when test="${degrevement.location != null}">
								<td style="text-align: right;">
									<c:out value="${degrevement.location.surface}"/><c:if test="${degrevement.location.surface != null}">&nbsp;m&sup2;</c:if>
								</td>
								<td style="text-align: right;">
									<c:out value="${degrevement.location.volume}"/><c:if test="${degrevement.location.surface != null}">&nbsp;m&sup3;</c:if>
								</td>
								<td style="text-align: right;">
									<c:out value="${degrevement.location.revenu}"/><c:if test="${degrevement.location.surface != null}">&nbsp;<fmt:message key="label.chf"/></c:if>
								</td>
								<td style="text-align: right;">
									<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.location.pourcentage}"/><c:if test="${degrevement.location.pourcentage != null}">&nbsp;&percnt;</c:if>
								</td>
								<td style="text-align: right; font-weight: bold;">
									<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.location.pourcentageArrete}"/><c:if test="${degrevement.location.pourcentageArrete != null}">&nbsp;&percnt;</c:if>
								</td>
							</c:when>
							<c:otherwise>
								<td colspan="5">&nbsp;</td>
							</c:otherwise>
						</c:choose>
						<c:choose>
							<c:when test="${degrevement.propreUsage != null}">
								<td style="text-align: right;">
									<c:out value="${degrevement.propreUsage.surface}"/><c:if test="${degrevement.propreUsage.surface != null}">&nbsp;m&sup2;</c:if>
								</td>
								<td style="text-align: right;">
									<c:out value="${degrevement.propreUsage.volume}"/><c:if test="${degrevement.propreUsage.surface != null}">&nbsp;m&sup3;</c:if>
								</td>
								<td style="text-align: right;">
									<c:out value="${degrevement.propreUsage.revenu}"/><c:if test="${degrevement.propreUsage.surface != null}">&nbsp;<fmt:message key="label.chf"/></c:if>
								</td>
								<td style="text-align: right;">
									<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.propreUsage.pourcentage}"/><c:if test="${degrevement.propreUsage.pourcentage != null}">&nbsp;&percnt;</c:if>
								</td>
								<td style="text-align: right; font-weight: bold;">
									<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${degrevement.propreUsage.pourcentageArrete}"/><c:if test="${degrevement.propreUsage.pourcentageArrete != null}">&nbsp;&percnt;</c:if>
								</td>
							</c:when>
							<c:otherwise>
								<td colspan="5">&nbsp;</td>
							</c:otherwise>
						</c:choose>
						<c:choose>
							<c:when test="${degrevement.loiLogement != null}">
								<td style="text-align: center;"><input type="checkbox" disabled checked/></td>
								<td><unireg:regdate regdate="${degrevement.loiLogement.dateOctroi}"/></td>
								<td><unireg:regdate regdate="${degrevement.loiLogement.dateEcheance}"/></td>
							</c:when>
							<c:otherwise>
								<td style="text-align: center;"><input type="checkbox" disabled/></td>
								<td>&nbsp;</td>
								<td>&nbsp;</td>
							</c:otherwise>
						</c:choose>
						<td>
							<unireg:consulterLog entityNature="AllegementFoncier" entityId="${degrevement.idDegrevement}"/>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</c:when>
		<c:otherwise>
			<span style="font-style: italic"><fmt:message key="label.aucune.donnee.degrevement"/></span>
		</c:otherwise>
	</c:choose>

</fieldset>

<fieldset>
	<legend><span><fmt:message key="label.exonerations.ifonc"/></span></legend>
	<c:choose>
		<c:when test="${not empty exonerations}">
			<unireg:nextRowClass reset="1"/>
			<table class="display display_table">
				<thead>
				<tr>
					<th><fmt:message key="label.date.debut"/></th>
					<th><fmt:message key="label.date.fin"/></th>
					<th><fmt:message key="label.pourcentage.exoneration"/></th>
					<th>&nbsp;</th>
				</tr>
				</thead>
				<tbody>
				<c:forEach items="${exonerations}" var="exo">
					<tr class="<unireg:nextRowClass/>">
						<td><unireg:regdate regdate="${exo.dateDebut}"/></td>
						<td><unireg:regdate regdate="${exo.dateFin}"/></td>
						<td>
							<fmt:formatNumber maxIntegerDigits="4" minFractionDigits="2" value="${exo.pourcentageExoneration}"/><c:if test="${exo.pourcentageExoneration != null}">&nbsp;&percnt;</c:if>
						</td>
						<td>
							<unireg:consulterLog entityNature="AllegementFoncier" entityId="${exo.idExoneration}"/>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</c:when>
		<c:otherwise>
			<span style="font-style: italic"><fmt:message key="label.aucune.donnee.exoneration"/></span>
		</c:otherwise>
	</c:choose>
</fieldset>
