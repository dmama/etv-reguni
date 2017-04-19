<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<%--@elvariable id="periodesDebut" type="java.util.List<ch.vd.uniregctb.registrefoncier.PeriodeFiscaleView>"--%>
<%--@elvariable id="degrevementNonIntegrable" type="java.lang.Boolean"--%>

<c:set var="commandName" value="${param.commandName}"/>
<c:set var="allowPeriodeDebutEdit" value="${param.allowPeriodeDebutEdit}"/>

<c:if test="${degrevementNonIntegrable}">
	<div class="flash-warning"><fmt:message key="label.degrevement.non.integrable.fulltext"/></div>
</c:if>

<table border="0">
	<tr class="even">
		<td style="width: 15%;"><fmt:message key="label.periode.fiscale.debut"/>&nbsp;:</td>
		<td style="width: 35%;">
			<c:choose>
				<c:when test="${allowPeriodeDebutEdit}">
					<form:select path="anneeDebut">
						<form:option value=""/>
						<c:forEach items="${periodesDebut}" var="periode">
							<form:option value="${periode.annee}" disabled="${periode.interdite}"/>
						</c:forEach>
					</form:select>
					<span style="color: red;">*</span>
					<form:errors path="anneeDebut" cssClass="error"/>
				</c:when>
				<c:otherwise>
					<form:hidden path="anneeDebut"/>
					<c:set var="pfName" value="${commandName}.anneeDebut"/>
					<spring:bind path="${pfName}">
						<span style="padding-left: 1em;"><c:out value="${status.value}"/></span>
					</spring:bind>
				</c:otherwise>
			</c:choose>
		</td>
		<td style="width: 15%;"><fmt:message key="label.periode.fiscale.fin"/>&nbsp;:</td>
		<td style="width: 35%;">
			<c:set var="pfFinName" value="${commandName}.anneeFin"/>
			<spring:bind path="${pfFinName}">
				<c:choose>
					<c:when test="${allowPeriodeDebutEdit}">
						<span style="font-style: italic; color: gray; padding-left: 1em;"><c:out value="${status.value}"/></span>
					</c:when>
					<c:otherwise>
						<span style="padding-left: 1em;"><c:out value="${status.value}"/></span>
					</c:otherwise>
				</c:choose>
			</spring:bind>
			<form:hidden path="anneeFin"/>
		</td>
	</tr>
	<tr class="odd">
		<td colspan="4" style="padding: 0 20% 0 20%;">
			<fieldset>
				<legend><span><fmt:message key="label.donnees.location"/></span></legend>
				<unireg:nextRowClass reset="0"/>
				<table class="degrevement">
					<thead>
					<tr>
						<th class="titre">&nbsp;</th>
						<th class="valeur"><fmt:message key="label.valeur.declaree"/></th>
						<th class="valeur"><fmt:message key="label.valeur.arretee"/></th>
					</tr>
					</thead>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.revenu.encaisse"/> (<fmt:message key="label.chf"/>)</td>
						<td class="valeur">
							<form:input path="location.revenu" cssClass="nombre" id="locationRevenu" onchange="EditDegrevementDynamic.calculatePourcentageRevenu();"/>
							<form:errors path="location.revenu" cssClass="error"/>
						</td>
						<td class="valeur">&nbsp;</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.volume"/> (m&sup3;)</td>
						<td class="valeur">
							<form:input path="location.volume" cssClass="nombre" id="locationVolume" onchange="EditDegrevementDynamic.calculatePourcentageVolume();"/>
							<form:errors path="location.volume" cssClass="error"/>
						</td>
						<td class="valeur">&nbsp;</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.surface"/> (m&sup2;)</td>
						<td class="valeur">
							<form:input path="location.surface" cssClass="nombre" id="locationSurface" onchange="EditDegrevementDynamic.calculatePourcentageSurface();"/>
							<form:errors path="location.surface" cssClass="error"/>
						</td>
						<td class="valeur">&nbsp;</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.pourcentage"/> (&percnt;)</td>
						<td class="valeur">
							<form:input path="location.pourcentage" cssClass="nombre" id="locationPourcentage"/>
							<form:errors path="location.pourcentage" cssClass="error"/>
						</td>
						<td class="valeur">
							<form:input path="location.pourcentageArrete" cssClass="nombre" id="locationPourcentageArrete" onchange="EditDegrevementDynamic.resetValeurArreteePropreUsage();"/>
							<span style="color: red;">*</span>
							<div style="display: inline-block; vertical-align: middle;">
								<form:errors path="location.pourcentageArrete" cssClass="error"/>
							</div>
						</td>
					</tr>
				</table>
			</fieldset>
			<fieldset>
				<legend><span><fmt:message key="label.donnees.propre.usage"/></span></legend>
				<unireg:nextRowClass reset="0"/>
				<table class="degrevement">
					<thead>
					<tr>
						<th class="titre">&nbsp;</th>
						<th class="valeur"><fmt:message key="label.valeur.declaree"/></th>
						<th class="valeur"><fmt:message key="label.valeur.arretee"/></th>
					</tr>
					</thead>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.revenu.estime"/> (<fmt:message key="label.chf"/>)</td>
						<td class="valeur">
							<form:input path="propreUsage.revenu" cssClass="nombre" id="propreUsageRevenu" onchange="EditDegrevementDynamic.calculatePourcentageRevenu();"/>
							<form:errors path="propreUsage.revenu" cssClass="error"/>
						</td>
						<td class="valeur">
							<div class="computed" id="percentRevenu">&nbsp;</div>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.volume"/> (m&sup3;)</td>
						<td class="valeur">
							<form:input path="propreUsage.volume" cssClass="nombre" id="propreUsageVolume" onchange="EditDegrevementDynamic.calculatePourcentageVolume();"/>
							<form:errors path="propreUsage.volume" cssClass="error"/>
						</td>
						<td class="valeur">
							<div class="computed" id="percentVolume">&nbsp;</div>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.surface"/> (m&sup2;)</td>
						<td class="valeur">
							<form:input path="propreUsage.surface" cssClass="nombre" id="propreUsageSurface" onchange="EditDegrevementDynamic.calculatePourcentageSurface();"/>
							<form:errors path="propreUsage.surface" cssClass="error"/>
						</td>
						<td class="valeur">
							<div class="computed" id="percentSurface">&nbsp;</div>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/>">
						<td class="titre"><fmt:message key="label.pourcentage"/> (&percnt;)</td>
						<td class="valeur">
							<form:input path="propreUsage.pourcentage" cssClass="nombre" id="propreUsagePourcentage"/>
							<form:errors path="propreUsage.pourcentage" cssClass="error"/>
						</td>
						<td class="valeur">
							<form:input path="propreUsage.pourcentageArrete" cssClass="nombre" id="propreUsagePourcentageArrete" onchange="EditDegrevementDynamic.resetValeurArreteeLocative();"/>
							<span style="color: red;">*</span>
							<div style="display: inline-block; vertical-align: middle;">
								<form:errors path="propreUsage.pourcentageArrete" cssClass="error"/>
							</div>
						</td>
					</tr>
				</table>
			</fieldset>
			<fieldset>
				<legend>
					<span><fmt:message key="label.donnees.loi.logement"/></span>
					<div style="padding-left: 2em; display: inline; font-weight: normal;">
						<form:checkbox path="loiLogement.controleOfficeLogement" onchange="EditDegrevementDynamic.toggleControleOfficeLogement();" id="avecControleOfficeLogement"/>
						<form:label path="loiLogement.controleOfficeLogement"><fmt:message key="option.ouinon.true"/></form:label>
					</div>
				</legend>
				<unireg:nextRowClass reset="0"/>
				<table class="degrevement">
					<tr class="<unireg:nextRowClass/> ctrl-office-logement">
						<td class="titre"><fmt:message key="label.date.octroi"/></td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="loiLogement.dateOctroi"/>
								<jsp:param name="id" value="dateOctroi"/>
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/> ctrl-office-logement">
						<td class="titre"><fmt:message key="label.date.echeance.octroi"/></td>
						<td>
							<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
								<jsp:param name="path" value="loiLogement.dateEcheance"/>
								<jsp:param name="id" value="dateEcheance"/>
							</jsp:include>
						</td>
					</tr>
					<tr class="<unireg:nextRowClass/> ctrl-office-logement">
						<td class="titre"><fmt:message key="label.pourcentage.caractere.social"/> (&percnt;)</td>
						<td>
							<form:input path="loiLogement.pourcentageCaractereSocial" cssClass="nombre"/>
							<form:errors path="loiLogement.pourcentageCaractereSocial" cssClass="error"/>
						</td>
					</tr>
				</table>
			</fieldset>
		</td>
	</tr>
</table>

<script type="application/javascript">

	const EditDegrevementDynamic = {

		calculatePourcentages: function() {
			this.calculatePourcentageRevenu();
			this.calculatePourcentageVolume();
			this.calculatePourcentageSurface();
		},

		calculatePourcentageRevenu: function() {
			this._calculatePourcentage('locationRevenu', 'propreUsageRevenu', 'percentRevenu');
		},

		calculatePourcentageVolume: function() {
			this._calculatePourcentage('locationVolume', 'propreUsageVolume', 'percentVolume');
		},

		calculatePourcentageSurface: function() {
			this._calculatePourcentage('locationSurface', 'propreUsageSurface', 'percentSurface');
		},

		resetValeurArreteeLocative: function() {
			this._resetValeurArretee('propreUsagePourcentageArrete', 'locationPourcentageArrete');
		},

		resetValeurArreteePropreUsage: function() {
			this._resetValeurArretee('locationPourcentageArrete', 'propreUsagePourcentageArrete');
		},

		toggleControleOfficeLogement: function() {
			const lines = $('tr.ctrl-office-logement');
			const avecCtrl = $('#avecControleOfficeLogement')[0].checked;
			if (avecCtrl) {
				lines.show();
			}
			else {
				lines.hide();
			}
		},

		initValeursArretees: function() {
			const location = this._extractDecimal(this._extractInputFieldValue('locationPourcentageArrete'));
			const propreUsage = this._extractDecimal(this._extractInputFieldValue('propreUsagePourcentageArrete'));
			if (location !== null && propreUsage === null) {
				this.resetValeurArreteePropreUsage();
			}
			else if (location === null && propreUsage !== null) {
				this.resetValeurArreteeLocative();
			}
		},

		_calculatePourcentage: function(idInputLocation, idInputPropreUsage, idSpanResult) {
			const location = this._extractInteger(this._extractInputFieldValue(idInputLocation));
			const propreUsage = this._extractInteger(this._extractInputFieldValue(idInputPropreUsage));
			const spanResult = $('#' + idSpanResult);
			if (location !== null && propreUsage !== null) {
				const denominateur = 1.0 * location + 1.0 * propreUsage;
				if (denominateur > 0) {
					const percent = (propreUsage * 100.0) / denominateur;
					spanResult.html(percent.toFixed(2) + "&nbsp;&percnt;");
					return;
				}
			}
			spanResult.html('&nbsp;');
		},

		_resetValeurArretee: function(idInputSource, idInputDestination) {
			const src = this._extractDecimal(this._extractInputFieldValue(idInputSource));
			const inputDest = $('#' + idInputDestination)[0];
			if (src !== null) {
				const srcPercent = 1.0 * src;
				if (srcPercent >= 0.0 && srcPercent <= 100.0) {
					const destPercent = 100.0 - srcPercent;
					inputDest.value = destPercent.toFixed(2);
					$('#' + idInputSource)[0].value = (100.0 - destPercent).toFixed(2);
				}
				else {
					inputDest.value = '';
				}
			}
		},

		_extractInputFieldValue: function(idInputField) {
			return $('#' + idInputField)[0].value;
		},

		_extractInteger: function(text) {
			const match = /^\s*([0-9]+)\s*$/g.exec(text);
			return match === null ? null : match[1];
		},

		_extractDecimal: function(text) {
			const match = /^\s*([0-9]+(?:\.[0-9]*)?)\s*/g.exec(text);
			return match === null ? null : match[1];
		}

	};

	$(function() {
		EditDegrevementDynamic.calculatePourcentages();
		EditDegrevementDynamic.initValeursArretees();
		EditDegrevementDynamic.toggleControleOfficeLogement();
	});

</script>
