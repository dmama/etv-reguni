<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ taglib uri="http://www.unireg.com/uniregTagLib" prefix="unireg" %>

<c:if test="${command.forPrint}">
	<c:set var="templateUrl" value="/WEB-INF/jsp/templates/templateDialog.jsp" />
</c:if>
<c:if test="${!command.forPrint}">
	<c:set var="templateUrl" value="/WEB-INF/jsp/templates/template.jsp" />
</c:if>

<tiles:insert template="${templateUrl}">

	<tiles:put name="head">
		<%-- Code spécifique IE pour faire tourner l'année de 90° à gauche --%>
		<!--[if IE]>
		<style type="text/css">
			.timeline td.annee div {
				position: relative;
				width: 1.5em;
			}

			.timeline td.annee div span {
				position: absolute;
				top: -1.2em;
				left: 2px;
				filter: progid:DXImageTransform.Microsoft.BasicImage(rotation = 3);
			}
		</style>
		<![endif]-->
		<style type="text/css">
			.timeline td {
				border-color: white;
			}
		</style>
	</tiles:put>

	<tiles:put name="title">Vue chronologique des fors fiscaux et des assujettissements</tiles:put>

	<tiles:put name="body">

	<%--@elvariable id="command" type="ch.vd.uniregctb.tiers.timeline.ForsTimelineView"--%>
		<c:if test="${command.forPrint}">
				<h1><c:out value="${command.title}"/></h1>
				<h3><c:out value="${command.description}"/></h3>
		</c:if>
		<c:if test="${!command.forPrint}">
			<a href="<c:url value="/tiers/visu.do?id=" /><c:out value="${command.tiersId}" />" >&lt;&lt; Retour à la visualisation</a>

			<unireg:bandeauTiers numero="${command.tiersId}" showValidation="false" showEvenementsCivils="false" showLinks="false"/>
		</c:if>


		<c:if test="${fn:length(command.exceptions) > 0}">
			<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
				<tr><td class="heading iepngfix">Les assujettissements n'ont pas tous pu être calculés pour les raisons suivantes:</td></tr>
				<tr><td class="details"><ul>
				<c:forEach var="err" items="${command.exceptions}">
					<li class="err"><fmt:message key="label.validation.erreur"/>: <c:out value="${err.message}"/></li>
				</c:forEach>
				</ul></td></tr>
			</table>
		</c:if>

		<div id="legend">
			<table>
				<tr><td class="principal_vd" style="width: 20%">&nbsp;</td><td>For fiscal principal dans le canton de Vaud</td></tr>
				<tr><td class="principal_hc" style="width: 20%">&nbsp;</td><td>For fiscal principal hors canton</td></tr>
				<tr><td class="principal_hs" style="width: 20%">&nbsp;</td><td>For fiscal principal hors Suisse</td></tr>
			</table>
		</div>

		<form action="timeline<c:if test="${debugAssujettissement}">-debug</c:if>.do" method="get">
			<span id="timeline_header">Affichage des :
				<input type="hidden" name="id" value="${command.tiersId}">

				<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun'}">
					<input type="checkbox" id="checkForsGestion" onclick="$('#showForsGestion').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showForsGestion}"> checked</c:if>/>
					<label for="checkForsGestion">Fors de gestion</label>
					<input type="hidden" id="showForsGestion" name="showForsGestion" value="${command.showForsGestion}"/>
				</c:if>

				<c:if test="${debugAssujettissement && (command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun')}">
					<input type="checkbox" id="checkAssujettissementsSource" onclick="$('#showAssujettissementsSource').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showAssujettissementsSource}"> checked</c:if>/>
					<label for="checkAssujettissementsSource">Assujettissements source</label>
					<input type="hidden" id="showAssujettissementsSource" name="showAssujettissementsSource" value="${command.showAssujettissementsSource}"/>

					<input type="checkbox" id="checkAssujettissementsRole" onclick="$('#showAssujettissementsRole').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showAssujettissementsRole}"> checked</c:if>/>
					<label for="checkAssujettissementsRole">Assujettissements rôle</label>
					<input type="hidden" id="showAssujettissementsRole" name="showAssujettissementsRole" value="${command.showAssujettissementsRole}"/>
				</c:if>

				<input type="checkbox" id="checkAssujettissements" onclick="$('#showAssujettissements').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showAssujettissements}"> checked</c:if>/>
				<label for="checkAssujettissements">Assujettissements<c:if test="${debugAssujettissement && (command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun')}"> (combinés)</c:if></label>
				<input type="hidden" id="showAssujettissements" name="showAssujettissements" value="${command.showAssujettissements}"/>

				<input type="checkbox" id="checkPeriodesImposition" onclick="$('#showPeriodesImposition').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showPeriodesImposition}"> checked</c:if>/>
				<label for="checkPeriodesImposition">Périodes d'imposition</label>
				<input type="hidden" id="showPeriodesImposition" name="showPeriodesImposition" value="${command.showPeriodesImposition}"/>

				<c:if test="${command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant'}">
					<input type="checkbox" id="checkPeriodesImpositionIS" onclick="$('#showPeriodesImpositionIS').val($(this).is(':checked')); $(this).closest('form').submit();" <c:if test="${command.showPeriodesImpositionIS}"> checked</c:if>/>
					<label for="checkPeriodesImpositionIS">Périodes d'imposition IS</label>
					<input type="hidden" id="showPeriodesImpositionIS" name="showPeriodesImpositionIS" value="${command.showPeriodesImpositionIS}"/>
				</c:if>

			</span>
		</form>

		<table class="timeline">
			<tr>
				<th colspan="2">Période</th>
				<th colspan="2">Fors Principaux</th>
				<th id="th-fs" colspan="<c:out value="${command.table.forsSecondairesSize}"/>">Fors Secondaires</th>
				<c:if test="${command.showForsGestion && (command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun')}"><th>Fors de Gestion</th></c:if>
				<c:if test="${debugAssujettissement && (command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun')}">
					<c:if test="${command.showAssujettissementsSource}"><th>Assujettissements source</th></c:if>
					<c:if test="${command.showAssujettissementsRole}"><th>Assujettissements rôle</th></c:if>
				</c:if>
				<c:if test="${command.showAssujettissements}"><th>Assujettissements<c:if test="${debugAssujettissement && (command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun')}"> (combinés)</c:if></th></c:if>
				<c:if test="${command.showPeriodesImposition}"><th>Périodes d'imposition</th></c:if>
				<c:if test="${command.showPeriodesImpositionIS}"><th>Périodes d'imposition IS</th></c:if>
			</tr>

			<tr class="invisibleBorder" />

			<c:forEach var="ligne" varStatus="status" items="${command.table.rows}" >
				<tr>

					<%-- les périodes --%>
					<c:if test="${ligne.periode.multiYears}">
						<td class="periodeFull" colspan="2">
							<c:if test="${command.invertedTime}">
								<unireg:regdate regdate="${ligne.periode.dateFin}" format="dd.MM.yyyy" defaultValue="..."/><br/>
								<unireg:regdate regdate="${ligne.periode.dateDebut}" format="dd.MM.yyyy"/>
							</c:if>
							<c:if test="${!command.invertedTime}">
								<unireg:regdate regdate="${ligne.periode.dateDebut}" format="dd.MM.yyyy"/><br/>
								<unireg:regdate regdate="${ligne.periode.dateFin}" format="dd.MM.yyyy" defaultValue="..."/>
							</c:if>
						</td>
					</c:if>
					<c:if test="${!ligne.periode.multiYears}">
						<td class="periode">
							<c:if test="${command.invertedTime}">
								<unireg:regdate regdate="${ligne.periode.dateFin}" format="dd.MM"/><br/>
								<unireg:regdate regdate="${ligne.periode.dateDebut}" format="dd.MM"/>
							</c:if>
							<c:if test="${!command.invertedTime}">
								<unireg:regdate regdate="${ligne.periode.dateDebut}" format="dd.MM"/><br/>
								<unireg:regdate regdate="${ligne.periode.dateFin}" format="dd.MM"/>
							</c:if>
						</td>
						<c:choose>
							<c:when test="${ligne.periode.yearSpan == 1}">
								<td class="annee">
									<div><span><c:out value="${ligne.periode.anneeLabel}"/></span></div>
								</td>
							</c:when>
							<c:when test="${ligne.periode.yearSpan > 0}">
								<td class="annee" rowspan="<c:out value="${ligne.periode.yearSpan}" />">
									<div><span><c:out value="${ligne.periode.anneeLabel}"/></span></div>
								</td>
							</c:when>
							<c:when test="${ligne.periode.yearSpan == 0}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>

					<td class="invisibleBorder" />

					<%-- fors principaux --%>
					<c:choose>
						<c:when test="${ligne.forPrincipal.filler}">			
							<td class="filler" />
						</c:when>
						<c:when test="${!ligne.forPrincipal.span && !ligne.forPrincipal.filler}">
							<c:set var="fp" value="${ligne.forPrincipal.range}" />
							<c:choose>
								<c:when test="${fp.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">			
									<td class="principal_vd tooltip_cell" id="ffp-${fp.id}" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
									    <unireg:commune ofs="${fp.numeroOfsAutoriteFiscale}" displayProperty="nomOfficiel" date="${fp.dateDebut}"/>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
										    Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b><c:if test="${fp.motifOuverture != null}"> - <b><fmt:message key="option.motif.ouverture.${fp.motifOuverture}"/></b></c:if><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b><c:if test="${fp.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fp.motifFermeture}"/></b></c:if><br/>
										    Motif de rattachement : <b><fmt:message key="option.rattachement.${fp.motifRattachement}"/></b><br/>
											Genre d'impôt : <b><fmt:message key="option.genre.impot.${fp.genreImpot}"/></b><br/>
											<c:if test="${fp['class'].name == 'ch.vd.uniregctb.tiers.ForFiscalPrincipalPP'}">
												Mode d'imposition : <b><fmt:message key="option.mode.imposition.${fp.modeImposition}"/></b>
											</c:if>
										</div>
									</td>
								</c:when>
								<c:when test="${fp.typeAutoriteFiscale == 'COMMUNE_HC'}">
									<td class="principal_hc tooltip_cell" id="ffp-${fp.id}" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
                                        <unireg:commune ofs="${fp.numeroOfsAutoriteFiscale}" displayProperty="nomOfficielAvecCanton" date="${fp.dateDebut}"/>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
											Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b><c:if test="${fp.motifOuverture != null}"> - <b><fmt:message key="option.motif.ouverture.${fp.motifOuverture}"/></b></c:if><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b><c:if test="${fp.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fp.motifFermeture}"/></b></c:if><br/>
										    Motif de rattachement : <b><fmt:message key="option.rattachement.${fp.motifRattachement}"/></b><br/>
											Genre d'impôt : <b><fmt:message key="option.genre.impot.${fp.genreImpot}"/></b><br/>
											<c:if test="${fp['class'].name == 'ch.vd.uniregctb.tiers.ForFiscalPrincipalPP'}">
											    Mode d'imposition : <b><fmt:message key="option.mode.imposition.${fp.modeImposition}"/></b>
											</c:if>
										</div>
									</td>
								</c:when>
								<c:when test="${fp.typeAutoriteFiscale == 'PAYS_HS'}">
									<td class="principal_hs tooltip_cell" id="ffp-${fp.id}" rowspan="<c:out value="${ligne.forPrincipal.longueurAffichage}" />">
                                        <unireg:pays ofs="${fp.numeroOfsAutoriteFiscale}" displayProperty="nomCourt" date="${fp.dateDebut}"/>
										<div id="ffp-${fp.id}-tooltip" style="display:none;">
										    For fiscal principal <b>#${fp.id}</b><br/>
											Ouverture : <b><unireg:date date="${fp.dateDebut}"/></b><c:if test="${fp.motifOuverture != null}"> - <b><fmt:message key="option.motif.ouverture.${fp.motifOuverture}"/></b></c:if><br/>
										    Fermeture : <b><unireg:date date="${fp.dateFin}"/></b><c:if test="${fp.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fp.motifFermeture}"/></b></c:if><br/>
										    Motif de rattachement : <b><fmt:message key="option.rattachement.${fp.motifRattachement}"/></b><br/>
											Genre d'impôt : <b><fmt:message key="option.genre.impot.${fp.genreImpot}"/></b><br/>
											<c:if test="${fp['class'].name == 'ch.vd.uniregctb.tiers.ForFiscalPrincipalPP'}">
											    Mode d'imposition : <b><fmt:message key="option.mode.imposition.${fp.modeImposition}"/></b>
											</c:if>
										</div>
									</td>
								</c:when>
							</c:choose>
						</c:when>
						<c:when test="${ligne.forPrincipal.span}">
							<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
						</c:when>
					</c:choose>
					
					<%-- fors secondaires --%>
					<c:forEach var="fs" items="${ligne.forsSecondaires}" varStatus="fsLoop">
						<c:choose>
							<c:when test="${fs.filler}">			
								<td class="filler <c:if test="${fsLoop.index > 14}">fs-hideable</c:if>"/>
							</c:when>
							<c:when test="${!fs.span && !fs.filler}">
								<td class="secondaire tooltip_cell <c:if test="${fsLoop.index > 14}">fs-hideable</c:if>" id="ffs-${fs.range.id}" rowspan="<c:out value="${fs.longueurAffichage}" />">
                                    <unireg:commune ofs="${fs.range.numeroOfsAutoriteFiscale}" displayProperty="nomOfficiel" date="${fs.range.dateDebut}"/>
                                    <div id="ffs-${fs.range.id}-tooltip" style="display:none;">
                                        For fiscal secondaire <b>#${fs.range.id}</b><br/>
                                        Ouverture : <b><unireg:date date="${fs.range.dateDebut}"/></b><c:if test="${fs.range.motifOuverture != null}"> - <b><fmt:message key="option.motif.ouverture.${fs.range.motifOuverture}"/></b></c:if><br/>
                                        Fermeture : <b><unireg:date date="${fs.range.dateFin}"/></b><c:if test="${fs.range.motifFermeture != null}"> - <b><fmt:message key="option.motif.fermeture.${fs.range.motifFermeture}"/></b></c:if><br/>
	                                    Genre d'impôt : <b><fmt:message key="option.genre.impot.${fs.range.genreImpot}"/></b><br/>
	                                    Motif de rattachement : <b><fmt:message key="option.rattachement.${fs.range.motifRattachement}"/></b><br/>
                                    </div>
								</td>
							</c:when>
							<c:when test="${fs.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%> 
							</c:when>
						</c:choose>
					</c:forEach>
					<td id="fs-more" class="tooltip_cell" style="display: none;" rowspan="${command.table.rows.size()}" onclick="TimelineForsSecondaires.show();">
						<b>...</b>
						<div id="fs-more-tooltip" style="display: none;">
							<span style="font-style: italic;">Cliquer pour voir<br/>tous les fors secondaires...</span>
						</div>
					</td>

					<%-- fors de gestion --%>
					<c:if test="${command.showForsGestion && (command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun')}">
						<c:choose>
							<c:when test="${ligne.forGestion.filler}">
								<td class="filler" />
							</c:when>
							<c:when test="${!ligne.forGestion.span && !ligne.forGestion.filler}">
								<c:set var="fg" value="${ligne.forGestion.range}" />
								<td class="gestion tooltip_cell" id="fg-<unireg:regdate regdate="${fg.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.forGestion.longueurAffichage}" />">
                                    <unireg:commune ofs="${fg.noOfsCommune}" displayProperty="nomOfficiel" date="${fg.dateDebut}"/>
	                                <div id="fg-<unireg:regdate regdate="${fg.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
	                                    Ouverture : <b><unireg:date date="${fg.dateDebut}"/></b><br/>
	                                    Fermeture : <b><unireg:date date="${fg.dateFin}"/></b><br/>
	                                    Sous-jacent : <b>for fiscal #${fg.sousjacent.id}</b><br/>
	                                </div>
								</td>
							</c:when>
							<c:when test="${ligne.forGestion.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>

					<c:if test="${debugAssujettissement && (command.natureTiers == 'Habitant' || command.natureTiers == 'NonHabitant' || command.natureTiers == 'MenageCommun')}">

						<%-- assujettissements source --%>
						<c:if test="${command.showAssujettissementsSource}">
							<c:choose>
								<c:when test="${ligne.assujettissementSource.filler}">
									<td class="filler" />
								</c:when>
								<c:when test="${!ligne.assujettissementSource.span && !ligne.assujettissementSource.filler}">
									<c:set var="a" value="${ligne.assujettissementSource.range}" />
									<td class="assujettissement tooltip_cell" id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.assujettissementSource.longueurAffichage}" />">
	                                    <c:out value="${a.type.description}" />
		                                <div id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
		                                    Début : <b><unireg:date date="${a.dateDebut}"/></b>
		                                    <c:if test="${a.motifFractDebut != null}">
		                                        - <b><fmt:message key="option.motif.ouverture.${a.motifFractDebut}"/></b>
		                                    </c:if>
		                                    <br/>
		                                    Fin : <b><unireg:date date="${a.dateFin}"/></b>
		                                    <c:if test="${a.motifFractFin != null}">
		                                         - <b><fmt:message key="option.motif.fermeture.${a.motifFractFin}"/></b>
		                                    </c:if>
		                                    <br/>
		                                    <c:if test="${a['class'].name == 'ch.vd.uniregctb.metier.assujettissement.SourcierPur' || a['class'].name == 'ch.vd.uniregctb.metier.assujettissement.SourcierMixte'}">
		                                        Type autorité : <b><fmt:message key="option.type.autorite.fiscale.${a.typeAutoriteFiscalePrincipale}"/></b>
		                                    </c:if>
		                                </div>
									</td>
								</c:when>
								<c:when test="${ligne.assujettissementSource.span}">
									<%-- rien à mettre, le rowspan est automatiquement rempli --%>
								</c:when>
							</c:choose>
						</c:if>

						<%-- assujettissements rôle --%>
						<c:if test="${command.showAssujettissementsRole}">
							<c:choose>
								<c:when test="${ligne.assujettissementRole.filler}">
									<td class="filler" />
								</c:when>
								<c:when test="${!ligne.assujettissementRole.span && !ligne.assujettissementRole.filler}">
									<c:set var="a" value="${ligne.assujettissementRole.range}" />
									<td class="assujettissement tooltip_cell" id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.assujettissementRole.longueurAffichage}" />">
	                                    <c:out value="${a.type.description}" />
		                                <div id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
		                                    Début : <b><unireg:date date="${a.dateDebut}"/></b>
		                                    <c:if test="${a.motifFractDebut != null}">
		                                        - <b><fmt:message key="option.motif.ouverture.${a.motifFractDebut}"/></b>
		                                    </c:if>
		                                    <br/>
		                                    Fin : <b><unireg:date date="${a.dateFin}"/></b>
		                                    <c:if test="${a.motifFractFin != null}">
		                                         - <b><fmt:message key="option.motif.fermeture.${a.motifFractFin}"/></b>
		                                    </c:if>
		                                    <br/>
		                                    <c:if test="${a['class'].name == 'ch.vd.uniregctb.metier.assujettissement.SourcierPur' || a['class'].name == 'ch.vd.uniregctb.metier.assujettissement.SourcierMixte'}">
		                                        Type autorité : <b><fmt:message key="option.type.autorite.fiscale.${a.typeAutoriteFiscalePrincipale}"/></b>
		                                    </c:if>
		                                </div>
									</td>
								</c:when>
								<c:when test="${ligne.assujettissementRole.span}">
									<%-- rien à mettre, le rowspan est automatiquement rempli --%>
								</c:when>
							</c:choose>
						</c:if>
					</c:if>

					<%-- assujettissements --%>
					<c:if test="${command.showAssujettissements}">
						<c:choose>
							<c:when test="${ligne.assujettissement.filler}">
								<td class="filler" />
							</c:when>
							<c:when test="${!ligne.assujettissement.span && !ligne.assujettissement.filler}">
								<c:set var="a" value="${ligne.assujettissement.range}" />
								<td class="assujettissement tooltip_cell" id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.assujettissement.longueurAffichage}" />">
                                    <c:out value="${a.type.description}" />
	                                <div id="a-<unireg:regdate regdate="${a.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
	                                    Début : <b><unireg:date date="${a.dateDebut}"/></b>
	                                    <c:if test="${a.motifFractDebut != null}">
	                                        - <b><fmt:message key="option.motif.ouverture.${a.motifFractDebut}"/></b>
	                                    </c:if>
	                                    <br/>
	                                    Fin : <b><unireg:date date="${a.dateFin}"/></b>
	                                    <c:if test="${a.motifFractFin != null}">
	                                         - <b><fmt:message key="option.motif.fermeture.${a.motifFractFin}"/></b>
	                                    </c:if>
	                                    <br/>
	                                    <c:if test="${a['class'].name == 'ch.vd.uniregctb.metier.assujettissement.SourcierPur' || a['class'].name == 'ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al1' || a['class'].name == 'ch.vd.uniregctb.metier.assujettissement.SourcierMixteArt137Al2'}">
	                                        Type autorité : <b><fmt:message key="option.type.autorite.fiscale.${a.typeAutoriteFiscalePrincipale}"/></b>
	                                    </c:if>
	                                </div>
								</td>
							</c:when>
							<c:when test="${ligne.assujettissement.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>

					<%-- périodes d'imposition --%>
					<c:if test="${command.showPeriodesImposition}">
						<c:choose>
							<c:when test="${ligne.periodeImposition.filler}">
								<td class="filler" />
							</c:when>
							<c:when test="${!ligne.periodeImposition.span && !ligne.periodeImposition.filler}">
								<c:set var="pi" value="${ligne.periodeImposition.range}" />
								<td class="periodeImposition tooltip_cell" id="pi-<unireg:regdate regdate="${pi.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.periodeImposition.longueurAffichage}" />">
	                                <fmt:message key="option.type.contribuable.${pi.typeContribuable}"/>
									<c:if test="${pi.typeDocumentDeclaration != null}">
										/
	                                    <fmt:message key="option.type.document.${pi.typeDocumentDeclaration}"/>
									</c:if>
                                    <c:if test="${pi.declarationOptionnelle}">
                                        (optionnelle)
                                    </c:if>
                                    <c:if test="${pi.declarationRemplaceeParNote}">
                                        (remplacée par note)
                                    </c:if>
                                    <c:if test="${pi.diplomateSuisseSansImmeuble}">
                                        (diplomate suisse sans immeuble)
                                    </c:if>
	                                <div id="pi-<unireg:regdate regdate="${pi.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
	                                    Début : <b><unireg:date date="${pi.dateDebut}"/></b><br/>
	                                    Fin : <b><unireg:date date="${pi.dateFin}"/></b><br/>
		                                Période fiscale : <b>${pi.periodeFiscale}</b><br/>
	                                    Type de contribuable : <b><fmt:message key="option.type.contribuable.${pi.typeContribuable}"/></b><br/>
	                                    Type de document : <c:if test="${pi.typeDocumentDeclaration != null}"><b><fmt:message key="option.type.document.${pi.typeDocumentDeclaration}"/></b></c:if><br/>
	                                    Optionnelle : <b><fmt:message key="option.ouinon.${pi.declarationOptionnelle}"/></b><br/>
	                                    Remplacée par note : <b><fmt:message key="option.ouinon.${pi.declarationRemplaceeParNote}"/></b>
	                                </div>
								</td>
							</c:when>
							<c:when test="${ligne.periodeImposition.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
						</c:choose>
					</c:if>
					
					<%-- périodes d'imposition IS --%>
					<c:if test="${command.showPeriodesImpositionIS}">
						<c:choose>
							<c:when test="${ligne.periodeImpositionIS.filler}">
								<td class="filler" />
							</c:when>
							<c:when test="${ligne.periodeImpositionIS.span}">
								<%-- rien à mettre, le rowspan est automatiquement rempli --%>
							</c:when>
							<c:otherwise>
								<c:set var="pi" value="${ligne.periodeImpositionIS.range}" />
								<c:set var="td_class" value="periodeImpositionIS_${pi.type}"/>
								<td class="${td_class} tooltip_cell" id="piis-<unireg:regdate regdate="${pi.dateDebut}" format="yyyyMMdd"/>" rowspan="<c:out value="${ligne.periodeImpositionIS.longueurAffichage}" />">
									<fmt:message key="option.type.piis.${pi.type}"/>
									<c:if test="${pi.noOfs != null}">
										<br/>
										<c:choose>
											<c:when test="${pi.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD' || pi.typeAutoriteFiscale == 'COMMUNE_HC'}">
												<unireg:commune ofs="${pi.noOfs}" displayProperty="nomOfficielAvecCanton" date="${pi.dateDebut}"/>
											</c:when>
											<c:when test="${pi.typeAutoriteFiscale == 'PAYS_HS'}">
												<unireg:pays ofs="${pi.noOfs}" displayProperty="nomCourt"/>
											</c:when>
										</c:choose>
									</c:if>
	                                <div id="piis-<unireg:regdate regdate="${pi.dateDebut}" format="yyyyMMdd"/>-tooltip" style="display:none;">
	                                    Début : <b><unireg:date date="${pi.dateDebut}"/></b><br/>
	                                    Fin : <b><unireg:date date="${pi.dateFin}"/></b><br/>
	                                    Type : <b><fmt:message key="option.type.piis.${pi.type}"/></b><br/>
		                                <c:if test="${pi.noOfs != null}">
			                                <c:choose>
				                                <c:when test="${pi.typeAutoriteFiscale == 'COMMUNE_OU_FRACTION_VD'}">
					                                Commune VD : <b><unireg:commune ofs="${pi.noOfs}" displayProperty="nomOfficiel" date="${pi.dateDebut}"/></b>
				                                </c:when>
				                                <c:when test="${pi.typeAutoriteFiscale == 'COMMUNE_HC'}">
					                                Commune HC : <b><unireg:commune ofs="${pi.noOfs}" displayProperty="nomOfficiel" date="${pi.dateDebut}"/></b>
				                                </c:when>
				                                <c:when test="${pi.typeAutoriteFiscale == 'PAYS_HS'}">
					                                Pays : <b><unireg:pays ofs="${pi.noOfs}" displayProperty="nomCourt"/></b>
				                                </c:when>
			                                </c:choose>
		                                </c:if>
	                                </div>
								</td>
							</c:otherwise>
						</c:choose>
					</c:if>

				</tr>
			</c:forEach>
		</table>

		<c:if test="${!command.forPrint}">
			<a href="<c:url value="/tiers/visu.do?id=" /><c:out value="${command.tiersId}" />" >&lt;&lt; Retour à la visualisation</a>
		</c:if>

		<script type="text/javascript">

			const TimelineForsSecondaires = {
				hide : function() {
					const fsHidden = $("td.fs-hideable");
					if (fsHidden.length !== 0) {
						fsHidden.hide();
						$("#fs-more").show();
						$("#th-fs").attr("colspan", 16);
					}
				},

				show : function() {
					const fsHidden = $("td.fs-hideable");
					if (fsHidden.length !== 0) {
						fsHidden.show();
						$("#fs-more").hide();
						$("#th-fs").attr("colspan", ${command.table.forsSecondairesSize});
					}
				}
			};

			$(function() {
				$('.tooltip_cell').tooltip({
					items: "[id]",
					content: function(response) {
						var id = $(this).attr("id");
						return $('#' + id + '-tooltip').html();
					}
				});
				$("#legend").dialog({title: 'Légende', position: ['right','bottom']});

				TimelineForsSecondaires.hide();
			});
		</script>

		<![if !IE 6]>
		<script type="text/javascript">
			$(function () {
				$("table.timeline").show('slide', { direction:"left" });
			});
		</script>
		<![endif]-->

	</tiles:put>
	
</tiles:insert>
