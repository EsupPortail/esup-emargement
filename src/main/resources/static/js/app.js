//insert before
function insertBefore(el, referenceNode) {
    referenceNode.parentNode.insertBefore(el, referenceNode);
}
//Random id
var ID = function() {
    return '_' + Math.random().toString(36).substr(2, 9);
};
var allSelect = selectAll;
var myChartBar = null;
var suneditor0 = null;
var selectCmp = null;
//==jQuery remove()
function remove(id) {
    var elem = document.getElementById(id);
    if (elem != null) {
        return elem.parentNode.removeChild(elem);
    }
}

function getCalendar(calendarEl, urlEvents, editable) {
    var calendar = new FullCalendar.Calendar(calendarEl, {
        plugins: ['bootstrap', 'dayGrid', 'timeGrid', 'list'],
        header: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay,listMonth'
        },
        navLinks: true, // can click day/week names to navigate views
        editable: editable,
        locale: 'fr',
        events: urlEvents,
        contentHeight: 600,
        themeSystem: 'bootstrap'
    });

    calendar.render();
}

function searchUsersAutocomplete(id, url, paramurl, maxItems) {
    var searchBox = document.getElementById(id);
    if (searchBox != null) {
        var awesomplete = new Awesomplete(searchBox, {
            minChars: 3,
            maxItems: maxItems
        });
        searchBox.addEventListener("keyup", function() {
            if (this.value.length > 2) {
                var request = new XMLHttpRequest();
                var input = this.value;
                request.open('GET', url + "?searchValue=" + this.value + paramurl, true);
                request.onload = function() {
                    if (request.status >= 200 && request.status < 400) {
                        var data = JSON.parse(this.response);
                        var list = [];
                        var labelNumEtu = "";
                        var valueNumEtu = "";
                        data.forEach(function(value, key) {
                            if (id == "searchTagCheck" || id == "searchUserApp" || id == "searchPresence" || id == "searchIndividuTagCheck" || id == "searchIndividuTagChecker") {
                                var labelValue = "<strong>Nom : </strong>" + value.nom + "<strong class='ml-2'>Prénom : </strong>" + value.prenom + "<strong class='ml-2'>Eppn : </strong>" + value.eppn + labelNumEtu;
                                list.push({
                                    label: labelValue,
                                    value: value.eppn + "//" + value.nom + "//" + value.prenom + valueNumEtu
                                });
                            } else if (id == "searchLocation") {
                                var labelValue = "<strong>Nom : </strong>" + value.nom + "<strong class='ml-2'>Site : </strong>" + value.campus.site + "<strong class='ml-2'>Adresse : </strong>" +
                                    value.adresse;
                                list.push({
                                    label: labelValue,
                                    value: value.nom + "//" + value.campus.site + "//" + value.adresse
                                });
                            } else if (id == "searchSessionEpreuve") {
                                var realDate = value.dateExamen.substring(0, 10);
                                var splitDate = realDate.split("-");
                                var frDate = splitDate[2] + "-" + splitDate[1] + "-" + splitDate[0];
                                var labelValue = "<strong>Nom : </strong>" + value.nomSessionEpreuve + "<strong class='ml-2'>Site : </strong>" + value.campus.site + "<strong class='ml-2'>Date : </strong>" +
                                    frDate;
                                list.push({
                                    label: labelValue,
                                    value: value.nomSessionEpreuve + "//" + value.campus.site + "//" + frDate
                                });
                            } else if (id == "searchGroup") {
                                list.push(value);
                            } else {
                                if (value.numEtudiant != null) {
                                    labelNumEtu = "<strong class='ml-2'>N° Identifiant : </strong>" + value.numEtudiant;
                                    valueNumEtu = "//" + value.numEtudiant
                                }
                                if (value.username != null) {
                                    var labelValue = "<strong>Nom : </strong>" + value.username + "<strong class='ml-2'>Prénom : </strong>" + value.prenom + "<strong class='ml-2'>Eppn : </strong>" +
                                        value.eppn + labelNumEtu;
                                    list.push({
                                        label: labelValue,
                                        value: value.eppn + "//" + value.username + "//" + value.prenom + valueNumEtu
                                    });
                                }
                                if (value.numIdentifiant != null) {
                                    labelNumEtu = "<strong class='ml-2'>N° Identifiant : </strong>" + value.numIdentifiant;
                                    valueNumEtu = "//" + value.numEtudiant
                                }
                            }
                        });
                        awesomplete.list = list;
                    } else {
                        console.log("erreur du serveur!");
                    }
                };
                request.send();
            }
        });
    }
}

function changeSelectSessionEpreuve2(id, id2, url) {
    var selectOrigin = document.getElementById(id);
    var selectToPopulate = document.getElementById(id2);
    var request = new XMLHttpRequest();
    var input = this.value;

    request.open('GET', url + "?sessionEpreuve=" + selectOrigin.value, true);
    request.onload = function() {
        if (request.status >= 200 && request.status < 400) {
            selectToPopulate.innerHTML = "";
            var data = JSON.parse(this.response);
            if (data.length == 0) {
                $(".alertmsg").remove();
                $("#" + id2).closest(".col").append("<p class='text-danger font-weight-bold alertmsg'>Aucun lieu associé à cette session</p>");
                option = document.createElement('option');
                option.value = "";
                option.textContent = "";
                selectToPopulate.appendChild(option);
                $("#" + id2).prop("disabled", true);
                $("#submitForm").prop("disabled", true);
            } else {
                data.forEach(function(value, key) {
                    $(".alertmsg").remove();
                    $("#" + id2).removeAttr("disabled");
                    $("#submitForm").removeAttr("disabled");
                    option = document.createElement('option');
                    if (currentLocation !== undefined) {
                        if (value.id == currentLocation) {
                            option.selected = true;
                        }
                    }
                    option.value = value.id;
                    option.textContent = value.location.nom;
                    selectToPopulate.appendChild(option);
                });
            }
        } else {
            console.log("erreur du serveur!");
        }
    };
    request.send();
}
//Configs
function displayFormconfig(val, valeur) {
    var checkTrue = "";
    var checkFalse = "";
    if (valeur == "true") {
        checkTrue = "checked='checked'";
    } else if (valeur == "false") {
        checkFalse = "checked='checked'";
    }
    if (checkTrue == "" && checkFalse == "" && val == "BOOLEAN") {
        checkTrue = "checked='checked'";
    }
    var bool = '<div class="col-lg-10"><div id="boolGroup"><label class="radio-inline"><input type="radio" name="value" id="boolTrue" value="true"' + checkTrue + '/> True' +
        '</label><label class="radio-inline ml-2"><input type="radio" name="value" id="boolFalse" value="false" ' + checkFalse + ' /> False</label></div></div>';

    var areaEditor = document.getElementById("suneditor");
    switch (val) {
        case 'HTML':
            remove("boolGroup");
            remove("valeur");
            remove("suneditor_valeur");
            $("#suneditor").closest("div").after("<div class='col-lg-10'><textarea class='form-control' id='valeur' name='value'></textarea></div>");
            suneditor0 = createSunEditor('valeur');
            suneditor0.setContents(valeur);
            break;
        case 'TEXT':
            if (suneditor0 != null) {
                suneditor0 = suneditor0.hide();
            }
            remove("suneditor_valeur");
            remove("boolGroup");
            remove("valeur");
            $("#suneditor").closest("div").after("<div class='col-lg-10'><textarea class='form-control' id='valeur' name='value'>" + valeur + "</textarea></div>");
            break;
        case 'BOOLEAN':
            if (suneditor0 != null) {
                suneditor0 = suneditor0.hide();
            }
            remove("boolGroup");
            remove("suneditor_valeur");
            remove("valeur");
            $("#suneditor").closest("div").after(bool);
            break;
    }
}

function deleteParam(urlLocation, name) {
    const [head, tail] = urlLocation.split('?');
    var url = head + '?' + tail.replace(new RegExp(`&${name}=[^&]*|${name}=[^&]*&`), '');
    return url;
}

function updatePresence(url, numEtu) {
    var tagDate = document.getElementById("tagDate");
    var request = new XMLHttpRequest();
    request.open('GET', url + "?presence=" + numEtu, true);
    request.onload = function() {
        if (request.status >= 200 && request.status < 400) {
            var data = JSON.parse(this.response);
            var redirect = deleteParam(window.location.href, "tc");
            if (numEtu.startsWith("true")) {
                const options = {
                    year: "2-digit",
                    month: "2-digit",
                    day: "2-digit",
                    hour12: false,
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit"
                }
                var date = new Date().toLocaleTimeString("fr-FR", options);
                var person = data[0].person;;
                var url = emargementContextUrl + "/supervisor/" + person.eppn + "/photo";
                var modal = $('#photoModal2');
                modal.find('.modal-title').text(date);
                modal.find('.modal-body #nomPresence').text(person.nom.toUpperCase());
                modal.find('.modal-body #prenomPresence').text(person.prenom);
                if (person.numIdentifiant != null) {
                    modal.find('.modal-body #numIdentifiantPresence').text('N° ' + person.numIdentifiant);
                }
                modal.find('.modal-body #photoPresent').prop("src", url);
                modal.find('.modal-body #photoPresent').prop("alt", person.prenom + " " + person.nom);
                modal.modal('show');
                setTimeout(function() {
                    window.location.href = redirect;
                }, 1750);
            } else {
                window.location.href = redirect;
            }

        } else {
            console.log("erreur du serveur!");
        }
    };
    request.send();
}

//Revup param GET
function $_GET(param) {
    var vars = {};
    window.location.href.replace(location.hash, '').replace(
        /[?&]+([^=&]+)=?([^&]*)?/gi, // regexp
        function(m, key, value) { // callback
            vars[key] = value !== undefined ? value : '';
        }
    );

    if (param) {
        return vars[param] ? vars[param] : null;
    }
    return vars;
}

//Configs
function createSunEditor(id) {
    const editor = SUNEDITOR.create(id, {

        // height/width of the editor
        width: '100%',
        height: '150px',

        // show/hide toolbar icons
        buttonList: [
            ['font', 'fontSize', 'formatBlock'],
            ['bold', 'underline', 'italic'],
            ['fontColor', 'hiliteColor'],
            ['align', 'horizontalRule', 'list', 'link', 'image'],
            ['codeView']
        ]
    });
    return editor;
}

//Ajax count nb Etu
function countItem(apogeeBean, countUrl, type) {
    $.ajax({
        url: countUrl,
        contentType: "application/json",
        data: apogeeBean,
        success: function(data) {
            if (type == "matiere") {
                $("#nbInscritsMatiere").text("[" + data + "]");
            }
            if (type == "groupe") {
                $("#nbInscritsGroupe").text("[" + data + "]");
            }
        }
    });
}

//Recherches...
function submitSearchForm(id, url, endUrl) {
    var search = document.getElementById(id);
    if (search != null) {
        search.onfocus = function() {
            this.value = "";
        };
        searchUsersAutocomplete(id, url, endUrl, 10);
        var formSearch = document.getElementById("formSearch");
        search.addEventListener("awesomplete-selectcomplete", function(event) {
            var splitResult = this.value.split("//");
            search.value = splitResult[0].toString().trim();
            formSearch.submit();
        });
    }

}

//Couleurs pour graphiques stats
var seed = 11;

function random() {
    var x = Math.sin(seed++) * 1000;
    return x - Math.floor(x);
}
var generateColors = [];
var generateBorderColors = [];
var generateStackColors = [];
for (var k = 0; k < 100; k++) {
    var color = Math.floor(random() * 256) + ',' + Math.floor(random() * 256) + ',' + Math.floor(random() * 256);
    generateColors.push('rgba(' + color + ', 0.6)');
    generateStackColors.push('rgba(' + color + ', 0.6)');
    generateBorderColors.push('rgba(' + color + ', 1)');
}
var monthsArray = ["Jan", "Fev", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sept", "Oct", "Nov", "Déc"];
//affiche stats
function getStats(param, url, id, chartType, option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, byMonth, datalabels) {
    var prefId = document.getElementById(id);
    var request = new XMLHttpRequest();
    var paramUrl = (param != null) ? '&' + param : '';
    request.open('GET', emargementContextUrl + "/" + url + "/stats/json?type=" + id + paramUrl, true);
    request.onload = function() {
        if (request.status >= 200 && request.status < 400) {
            var data = JSON.parse(this.response);
            if ((Object.keys(data[id]).length) > 0) {
                if (chartType == "multiBar") {
                    multiChartStackBar(data[id], id, 3, transTooltip, formatDate);
                } else if (chartType == "chartBar") {
                    chartBar(data[id], label1, id, transTooltip, formatDate, data[data2], label2);
                } else if (chartType == "pie") {
                    chartPieorDoughnut(data[id], id, chartType, option, datalabels);
                } else if (chartType == "doughnut") {
                    chartPieorDoughnut(data[id], id, chartType, option, datalabels);
                } else if (chartType == "lineChart") {
                    lineChart(data[id], id, fill, arrayDates, byMonth, formatDate);
                }
            } else {
                $("#" + id).next().show();
                $("#" + id).hide();
            }
        }
    }
    request.send();
}

function lineChart(data, id, fill, arrayDates, byMonth, formatDate) {
    if (document.getElementById(id) != null) {
        var inlineValeurs = [];
        var inlineDatasets = [];
        var a = 0;
        //dates
        var xValues = [];
        var dates = arrayDates;
        //Hack : on enlève le '_'
        for (var idx in data) {
            if (!byMonth) {
                xValues = Object.keys(data[idx]).filter(function(propertyName) {
                    return propertyName.substr(1, propertyName.length);
                });
                for (var ind in dates) {
                    if (xValues.indexOf(dates[ind]) > -1) {
                        inlineValeurs.push(data[idx][dates[ind]]);
                    } else {
                        inlineValeurs.push(null);
                    }
                };
            } else {
                var keyMois = Object.keys(data[idx]);
                for (i = 1; i <= 12; i++) {
                    if (keyMois.indexOf(i.toString()) > -1) {
                        inlineValeurs.push(data[idx][i]);
                    } else {
                        inlineValeurs.push(null);
                    }
                }
            }
            inlineDatasets.push({
                //Hack : on enlève le '_'
                label: idx.substr(1, idx.length),
                backgroundColor: generateColors[a],
                borderColor: generateColors[a],
                pointColor: generateBorderColors[a],
                pointBorderColor: "#fff",
                pointHoverBorderColor: "#fff",
                pointBackgroundColor: generateColors[a],
                data: inlineValeurs,
                spanGaps: true,
                fill: fill,
                datalabels: {
                    display: false
                }
            });
            a++;
            inlineValeurs =   [];
        };
        var dateLabels = dates;
        if (formatDate) {
            dateLabels = [];
            for (var ind in dates) {
                dateLabels.push(formatDateString(dates[ind]));
            };
        }
        var dataMois = {
            labels: dateLabels,
            datasets: inlineDatasets
        };
        var ctx3 = document.getElementById(id).getContext("2d");
        var myLineChart = new Chart(ctx3, {
            type: 'line',
            data: dataMois,
            options: {
                responsive: true,
                scales: {
                    xAxes: [{
                        ticks: {
                            autoSkip: true
                        }
                    }]
                },
                tooltips: {
                    mode: 'label',
                    titleFontSize: 14,
                    bodyFontSize: 25
                }
            }
        });
    }
}

function chartPieorDoughnut(data, id, type, option, datalabels) {
    if (document.getElementById(id) != null) {
        if (typeof datalabels == "undefined") {
            datalabels = false;
        }
        var legend = true;
        if (option == "legend") {
            legend = false;
        }
        var doughnutLabels = [];
        var values = [];
        var dataSets = [];
        var doughnutDataArray = [];
        for (var idx in data) {
            doughnutLabels.push(idx);
            values.push(data[idx]);
        };
        dataSets.push({
            data: values,
            backgroundColor: generateStackColors,
            hoverBackgroundColor: generateColors,
            datalabels: {
                anchor: 'end',
                display: datalabels
            }
        });
        var doughnutDataArray = {
            labels: doughnutLabels,
            datasets: dataSets
        };
        var ctx3 = document.getElementById(id).getContext("2d");
        var myDoughnutChart2 = new Chart(ctx3, {
            type: type,
            data: doughnutDataArray,
            options: {
                responsive: true,
                animateRotate: false,
                legend: {
                    display: legend
                },
                plugins: {
                    datalabels: {
                        backgroundColor: function(context) {
                            return context.dataset.backgroundColor;
                        },
                        borderColor: 'white',
                        borderRadius: 25,
                        borderWidth: 2,
                        color: 'white',
                        display: function(context) {
                            var dataset = context.dataset;
                            var count = dataset.data.length;
                            var value = dataset.data[context.dataIndex];
                            return value > count * 1.5;
                        },
                        font: {
                            weight: 'bold'
                        },
                        formatter: Math.round
                    }
                },
                tooltips: {
                    enabled: true,

                    callbacks: {
                        label: function(tooltipItem, data) {
                            var dataset = data.datasets[tooltipItem.datasetIndex];
                            var total = dataset.data.reduce(function(previousValue, currentValue, currentIndex, array) {
                                return previousValue + currentValue;
                            });
                            var currentValue = dataset.data[tooltipItem.index];
                            var percentage = Math.floor(((currentValue / total) * 100) + 0.5);
                            return data.labels[tooltipItem.index] + " : " + percentage + "% (" + currentValue + ")";
                        }
                    }

                }
            }
        });
    }
}

function multiChartStackBar(allData, id, start, transTooltip, formatDate) {
    if (document.getElementById(id) != null) {
        var barLabels = [];
        var length = Object.keys(allData).length;
        var dataSets = [];
        var j = 0;
        for (var idx in allData) {
            //Hack : on enlève le '_'
            lab = idx.substr(1, idx.length);
            if (formatDate) {
                lab = formatDateString(idx.substr(1, idx.length));
            }
            barLabels.push(lab);
            var k = start;
            var obj = allData[idx];
            for (var key in obj) {
                var values = [];
                for (var i = 0; i < length; i++) {
                    values.push(0);
                }
                values[j] = obj[key];
                dataSets.push({
                    label: key,
                    data: values,
                    backgroundColor: generateStackColors[k],
                    datalabels: {
                        display: false
                    }
                });
                k++;
            };
            j++;
        };
        var barChartData = {
            labels: barLabels,
            datasets: dataSets
        }
        var ctx = document.getElementById(id).getContext("2d");
        var myBar = new Chart(ctx, {
            type: 'bar',
            data: barChartData,
            options: {
                responsive: true,
                legend: {
                    display: false
                },
                scales: {
                    yAxes: [{
                        ticks: {
                            beginAtZero: true
                        },
                        stacked: true
                    }],
                    xAxes: [{
                        stacked: true
                    }]
                },
                tooltips: {
                    mode: 'label',
                    bodyFontSize: 15,
                    titleFontSize: 16,
                    footerFontSize: 15,
                    callbacks: {
                        afterTitle: function() {
                            window.total = 0;
                        },
                        label: function(t, e) {
                            if (t.yLabel != 0) {
                                var a = e.datasets[t.datasetIndex].label || '';
                                var valor = parseInt(e.datasets[t.datasetIndex].data[t.index]);
                                window.total += valor;
                                if (transTooltip != null) {
                                    b = a.toString().replace(/_/g, "");
                                    msg = transTooltip + b.charAt(0).toUpperCase() + b.slice(1).toLowerCase();
                                    a = messages[msg];
                                }
                                return a + ': ' + t.yLabel
                            }
                        },
                        footer: function() {
                            return "TOTAL: " + window.total.toString().replace(/\B(?=(\d{3})+(?!\d))/g, " ");
                        }
                    }
                }
            }
        });
    }
}

function chartBar(data1, label1, id, transTooltip, formatDate, data2, label2) {
    if (document.getElementById(id) != null) {
        var listLabels = [];
        var listValeurs = [];
        var listTooltipLabels = [];
        for (var idx in data1) {
            if (formatDate) {
                idx = formatDateString(idx);
            }
            listTooltipLabels.push(idx);
            if (idx.length > 12) {
                idx = idx.substring(0, 12) + ".";
            }
            if (transTooltip != null) {
                b = idx.replace(/_/g, "");
                msg = transTooltip + b.charAt(0).toUpperCase() + b.slice(1).toLowerCase();
                idx = messages[msg];
            }
            listLabels.push(idx);
            listValeurs.push(data1[idx]);
        };
        var datasets = [{
            label: label1,
            backgroundColor: generateColors[3],
            borderColor: generateBorderColors[3],
            borderWidth: 1,
            data: listValeurs,
            datalabels: {
                display: false
            }
        }];
        if (data2 != null) {
            var listValeurs2 = [];
            for (var idx2 in data2) {
                listValeurs2.push(data2[idx2]);
            };
            datasets.push({
                label: label2,
                backgroundColor: generateColors[4],
                borderColor: generateBorderColors[4],
                borderWidth: 1,
                data: listValeurs2,
                datalabels: {
                    display: false
                }
            })
        }
        var barChartData = {
            labels: listLabels,
            datasets: datasets
        }
        var ctx = document.getElementById(id).getContext("2d");
        if (myChartBar != null && myChartBar.ctx.canvas.id == id) {
            myChartBar.destroy();
        }
        myChartBar = new Chart(ctx, {
            type: 'bar',
            data: barChartData,
            options: {
                responsive: true,
                legend: {
                    display: false
                },
                tooltips: {
                    bodyFontSize: 22,
                    callbacks: {
                        title: function(t, e) {
                            tootipTitle = listTooltipLabels[t[0].index];
                            if (transTooltip != null) {
                                b = tootipTitle.replace(/_/g, "");
                                msg = transTooltip + b.charAt(0).toUpperCase() + b.slice(1).toLowerCase();
                                tootipTitle = messages[msg];
                            }
                            return tootipTitle;
                        }
                    }
                },
                scales: {
                    yAxes: [{
                        ticks: {
                            beginAtZero: true
                        }
                    }]
                }
            }
        });
    }
}
//Select event
function setSelectEvent(id, idEvent) {
    var url = emargementContextUrl + "/manager/event/selectEvent/";
    var request = new XMLHttpRequest();
    request.open('GET', url + id, true);
    request.onload = function() {
        if (request.status >= 200 && request.status < 400) {
            $(idEvent).html();
            $(idEvent).html(this.response);
            new SlimSelect({
                select: '#icsEvents'
            })
        } else {
            console.log("erreur du serveur!");
        }
    };
    request.send();
}
//==jQuery document.ready
document.addEventListener('DOMContentLoaded', function() {
    //Autocomplete
    var userAppEppn = document.getElementById("eppn");
    var userAppNom = document.getElementById("nom");
    var userAppPrenom = document.getElementById("prenom");
    var numIdentifiant = document.getElementById("numIdentifiant");
    var superAdmin = document.getElementById("searchSuperAdmin");

    if (userAppPrenom != null) {
        if (superAdmin != null) {
            searchUsersAutocomplete("eppn", emargementContextUrl + "/superadmin/admins/searchUsersLdap", "", 10);
        } else {
            searchUsersAutocomplete("eppn", emargementContextUrl + "/admin/userApp/searchUsersLdap", "", 10);
        }
        userAppEppn.addEventListener("awesomplete-selectcomplete", function(e) {
            var splitEppn = this.value.split("//");
            userAppEppn.value = splitEppn[0].toString().trim();
            userAppNom.value = splitEppn[1].toString().trim()
            userAppPrenom.value = splitEppn[2].toString().trim()
        });
    }
    //input num Etu
    if (numIdentifiant != null) {
        searchUsersAutocomplete("eppn", emargementContextUrl + "/admin/userApp/searchUsersLdap", "", 10);
        if (userAppEppn != null) {
            userAppEppn.addEventListener("awesomplete-selectcomplete", function(e) {
                var splitEppn = this.value.split("//");
                userAppEppn.value = splitEppn[0].toString().trim();
                numIdentifiant.value = (typeof splitEppn[3] == "undefined") ? "" : splitEppn[3].toString().trim();
            });
        }
    }
    
    //select presence
    var presenceForm = document.getElementById("presenceForm");
    if (presenceForm != null) {
        var selectSessionEpreuve = document.getElementById("sessionEpreuvePresence");
        changeSelectSessionEpreuve2("sessionEpreuvePresence", "location", emargementContextUrl + "/supervisor/sessionLocation/searchSessionLocations");
        selectSessionEpreuve.addEventListener("change", function() {
            changeSelectSessionEpreuve2("sessionEpreuvePresence", "location", emargementContextUrl + "/supervisor/sessionLocation/searchSessionLocations");
        });
    }

    //Messages modal bilan CSv
    var dialogMsg = document.querySelector('#modalBilanCsvBody');
    if (dialogMsg != null) {
        $('#modalBilanCsv').modal('show');
    }
    var modalMsg = document.querySelector('#modalMsg');
    if (modalMsg != null) {
        $('#modalMsg').modal('show');
    }
    var idArray = [];
    var checkboxes = document.querySelectorAll('.case');
    var selectAll = document.getElementById("selectall");
    var listeIds = document.getElementById("listeIds");
    if (selectAll != null) {
        selectAll.addEventListener("click", function() {
            idArray = [];
            for (var i = 0, n = checkboxes.length; i < n; i++) {
                if (!checkboxes[i].disabled) {
                    checkboxes[i].checked = this.checked;
                }
            }
            idArray = Array.from(document.querySelectorAll("input[name='case']")).filter(function(checkbox) {
                return checkbox.checked;
            }).map(function(checkbox) {
                return checkbox.value;
            });
            if (listeIds != null) {
                listeIds.value = idArray;
            }
        });

        Array.from(checkboxes).forEach(function(link) {
            return link.addEventListener('click', function(event) {
                if (idArray.indexOf(this.value) < 0) {
                    idArray.push(this.value);
                } else {
                    idArray.splice(idArray.indexOf(this.value), 1);
                }
                if (!this.checked) {
                    selectAll.checked = false;
                }
                if (listeIds != null) {
                    listeIds.value = idArray;
                }
            });
        });

    }

    var suneditor = document.getElementById("pdfArea");
    var editor1 = null;
    if (suneditor != null) {
        editor1 = createSunEditor('pdfArea');
        editor2 = createSunEditor('emailArea');
    }

    //Click events
    document.addEventListener('click', function(event) {
        if (event.target.matches('.presenceCheck')) {
            var isPresent = event.target.checked;
            var isPresentValue = isPresent + ',' + event.target.value;
            updatePresence(emargementContextUrl + "/supervisor/updatePresents", isPresentValue);
        }
        if (event.target.matches('#pdfPreview')) {
            var htmltemplate = document.getElementById("htmltemplate");
            htmltemplate.value = editor1.getContents();
            var formPdfConvoc = document.getElementById("formPdfConvoc");
            formPdfConvoc.submit();
        }
        if (event.target.matches('#submitEmailConvocation')) {
            event.preventDefault();
            var bodyMsg = document.getElementById("bodyMsg");
            bodyMsg.value = editor2.getContents();
            var htmltemplatePdf = document.getElementById("htmltemplatePdf");
            htmltemplatePdf.value = editor1.getContents();
            var sendConvocation = document.getElementById("sendConvocation");
            var subject = document.getElementById("subject");
            var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
            if (subject.value.length == 0) {
                alert("Vous devez rentrer un sujet");
            } else if (!isPreviewPdfOk.checked) {
                alert("Vous devez valider le PDF de convocation");
            } else {
                sendConvocation.submit();
            }
        }
        if (event.target.matches('#submitEmailConvsignes')) {
            event.preventDefault();
            var bodyMsg = document.getElementById("bodyMsg");
            bodyMsg.value = editor2.getContents();
            var htmltemplatePdf = document.getElementById("htmltemplatePdf");
            htmltemplatePdf.value = editor1.getContents();
            var sendConvocation = document.getElementById("sendConsignes");
            var subject = document.getElementById("subject");
            var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
            if (subject.value.length == 0) {
                alert("Vous devez rentrer un sujet");
            } else if (!isPreviewPdfOk.checked) {
                alert("Vous devez valider le PDF de consignes");
            } else {
                sendConvocation.submit();
            }
        }

    }, false);

    //Configs
    var boolGroup = document.getElementById("boolGroup");
    if (boolGroup != null) {
        boolGroup.style.display = "none";
    }
    var valeur = document.getElementById("hiddenValeur");
    if (valeur != null) {
        var typeConfig = document.querySelector("input[type=radio][name=type]:checked");
        if (typeConfig != null) {
            displayFormconfig(typeConfig.value, valeur.value);
        }

        var radioType = document.querySelectorAll('input[type=radio][name=type]');
        Array.from(radioType).forEach(function(link) {
            return link.addEventListener('click', function(event) {
                displayFormconfig(this.value, valeur.value);
            });
        });
    }

    //configs
    var appliConfig = document.getElementById('configForm');
    if (appliConfig != null) {
        appliConfig.addEventListener('submit', function(e) {
            e.preventDefault();
            var radioType = document.querySelectorAll('input[type=radio][name=type]:checked')[0].value;
            if (radioType == "HTML") {
                document.getElementById("valeur").value = suneditor0.getContents();
            }
            appliConfig.submit();
        });
    }
    //submit export inscrits
    var submitExport = document.getElementById('submitExport');
    if (submitExport != null) {
        var importBtn = document.getElementById('import');
        $(".statusExport").hide();
        importBtn.addEventListener('click', function(e) {
            var sessionEpreuve = document.getElementById('sessionEpreuve');
            if (sessionEpreuve.value == "") {
                e.preventDefault();
                alert("Vous devez choisr une Session!");
            } else {
                $(".statusExport").show();
                submitExport.submit();
            }
        });

        $('#formApogee').cascadingDropdown({
            selectBoxes: [{
                selector: '.step1'
            }, {
                selector: '.step2'
            }, {
                selector: '.step3',
            }, {
                selector: '.step4',
                requires: ['.step1', '.step2', '.step3'],
                source: function(request, response) {
                    $.getJSON(emargementContextUrl + "/manager/extraction/searchDiplomes", request, function(data) {
                        $("#nbCodEtp").text("[" + data.length + "]");
                        response($.map(data, function(item, index) {
                            return {
                                label: item.libEtp,
                                value: item.codEtp,
                                selected: index == 0 // Select first available option

                            };
                        }));
                    });
                },
            }, {
                selector: '.step5',
                requires: ['.step1', '.step2', '.step3', '.step4'],
                source: function(request, response) {
                    $.getJSON(emargementContextUrl + "/manager/extraction/searchMatieres", request, function(data) {
                        $("#nbCodElp").text("[" + data.length + "]");
                        response($.map(data, function(item, index) {
                            return {
                                label: item.libElp + " (" + item.codElp + ")",
                                value: item.codElp,
                                selected: index == 0 // Select first available option

                            };
                        }));
                    });
                },
            }, {
                selector: '.step6',
                requires: ['.step1', '.step2', '.step3', '.step4', '.step5'],
                source: function(request, response) {
                    $.getJSON(emargementContextUrl + "/manager/extraction/searchGroupes", request, function(data) {
                        $("#nbCodExtGpe").text("[" + data.length + "]");
                        response($.map(data, function(item, index) {
                            return {
                                label: item.libGpe + " (" + item.codExtGpe + ")",
                                value: item.codExtGpe,
                                selected: index == 0 // Select first available option

                            };
                        }));
                    });
                },
            }],
            onChange: function(event, value, requiredValues, requirementsMet) {
                var apogeeBean = {
                    "codCmp": codCmp.value,
                    "codAnu": codAnu.value,
                    "codEtp": codEtp.value,
                    "codElp": codElp.value,
                    "codSes": codSes.value,
                    "codExtGpe": codExtGpe.value
                };
                var countUrlGroupe = emargementContextUrl + "/manager/extraction/countAutorisesGroupe";
                var countUrl = emargementContextUrl + "/manager/extraction/countAutorises";
                countItem(apogeeBean, countUrl, "matiere");
                countItem(apogeeBean, countUrlGroupe, "groupe");
            }
        });
    }

    var importUsersLdapform = document.getElementById('importUsersLdapform');
    if (importUsersLdapform != null) {
        var importBtn = document.getElementById('importLdap');
        var nbSelected = $("#usersGroupLdap option:selected").length;
        $(".statusExportLdap").hide();
        importBtn.addEventListener('click', function(e) {
            var sessionEpreuveLdap = document.getElementById('sessionEpreuveLdap');
            if (sessionEpreuveLdap.value == "") {
                e.preventDefault();
                alert("Vous devez choisr une Session!");
            } else {
                if (nbSelected > 0) {
                    $(".statusExportLdap").show();
                    importUsersLdapform.submit();
                }
            }
        });
    }
    $('#selectAll').on("click", function() {
        var selectedItems = 0;
        var nbSelected = $("#usersGroupLdap option:selected").length;
        $('#usersGroupLdap option').each(function() {
            if (!this.disabled && this.value != '') {
                $(this).prop('selected', (nbSelected == 0) ? true : false);
                selectedItems = $(this).prop('selected') ? $("#ldapMembersSize").text() : 0;
            }
        });
        $("#selectedUsers").text(selectedItems);
    });

    $('#usersGroupLdap option').on("click", function() {
        nbSelected = $("#usersGroupLdap option:selected").length;
        $("#selectedUsers").text(nbSelected);
    });
    /* SEARCH LONG POLL */
    var searchLongPoll = {
        debug: false,
        run: false,
        timer: undefined,
        lastAuthDate: 0,
        list: undefined
    };
    searchLongPoll.start = function() {
        if (!this.run) {
            this.run = true;
            this.timer = this.poll();
        }
    }
    searchLongPoll.clear = function() {
        //
    }
    searchLongPoll.stop = function() {
        if (this.run && this.timer != null) {
            clearTimeout(this.timer);
        }
        run = false;
    }
    searchLongPoll.load = function() {
        if (typeof rootUrl != "undefined" && this.run) {
            var request = new XMLHttpRequest();
            request.open('GET', emargementContextUrl + "/supervisor/searchPoll", true);
            request.onload = function() {
                if (request.status >= 200 && request.status < 400) {
                    var message = this.response;
                    if (message && message.length) {
                        if (message != "stop") {
                            window.location.href = rootUrl + message;
                        }
                    } else {
                        setTimeout(function() {
                            searchLongPoll.timer = searchLongPoll.poll();
                        }, 2000);
                    }
                }
            };
            request.onerror = function() {
                // Plus de session (et redirection CAS) ou erreur autre ... on stoppe pour ne pas boucler
                console.log("searchLongPoll stoppé ");
            };
            request.send();
        }
    }
    searchLongPoll.poll = function() {
        if (this.timer != null) {
            clearTimeout(this.timer);
        }
        setTimeout(searchLongPoll.load(), 1000);
    }
    if (typeof rootUrl != "undefined") {
        searchLongPoll.start();
    }
    /* SEARCH LONG POLL - END*/

    // tableau inscrits
    var tempsAmenage = document.getElementById('tempsAmenage');
    var repartition = document.getElementById('repartition');
    if (tempsAmenage != null) {
        tempsAmenage.addEventListener('change', function(e) {
            document.getElementById('formSearch').submit();
        });
    }
    if (repartition != null) {
        repartition.addEventListener('change', function(e) {
            document.getElementById('formSearch').submit();
        });
    }

    //Recherches
    if (document.getElementById("searchTagCheck") != null) {
        var sessionId = document.getElementById("sessionId");
        submitSearchForm("searchTagCheck", emargementContextUrl + "/manager/tagCheck/searchTagCheck", "&sessionId=" + sessionId.value);
    } else if (document.getElementById("searchUserApp") != null) {
        submitSearchForm("searchUserApp", emargementContextUrl + "/admin/userApp/search", "");
    } else if (document.getElementById("searchLocation") != null) {
        submitSearchForm("searchLocation", emargementContextUrl + "/admin/location/search", "");
    } else if (document.getElementById("searchSessionEpreuve") != null) {
        submitSearchForm("searchSessionEpreuve", emargementContextUrl + "/manager/sessionEpreuve/search", "");
    } else if (document.getElementById("searchPresence") != null) {
        var seId = document.getElementById("sessionEpreuvePresence");
        var slId = document.getElementById("location");
        submitSearchForm("searchPresence", emargementContextUrl + "/supervisor/search", "&sessionEpreuve=" + seId.value + "&sessionLocation=" + slId.value);
    } else if (document.getElementById("searchIndividuTagCheck") != null && document.getElementById("searchIndividuTagChecker") != null) {
        submitSearchForm("searchIndividuTagCheck", emargementContextUrl + "/manager/individu/search", "&type=tagCheck");
        submitSearchForm("searchIndividuTagChecker", emargementContextUrl + "/manager/individu/search", "&type=tagChecker");
    }

    //Affiche modal present
    var photoModal = document.getElementById('photoModal');
    if (photoModal != null) {
        if (photoModal != "") {
            $('#photoModal').fadeIn(100, function() {
                $('#photoModal').modal('show');
            });
            setTimeout(function() {
                $('#photoModal').fadeOut(2000, function() {
                    $('#photoModal').modal('hide');
                });
            }, 2000);
        }
    }

    //Affiner repartition
    function affinerRepartition(realId, operation) {
        msgWarning.textContent = "";
        var candidat = document.getElementById('candidats_' + realId);
        var percentage = document.getElementById('txRemplissage_' + realId);
        var capacite = document.getElementById('capacite_' + realId);
        var txRemplissage = percentage.textContent.substring(0, percentage.textContent.length - 1);
        var diviseur = parseInt(capacite.textContent, 10);
        var tempsAmenage = document.getElementById('tempsAmenage_' + realId);
        var tempsAmenageBool = (tempsAmenage.title == "Non") ? false : true;
        var inputCandidat = document.getElementById('form_' + realId);
        var sum = 0;
        var sum2 = 0;
        var tempTotal = 0;
        if (!tempsAmenageBool) {
            total = parseInt(totalCandidatsNotTempsAmenage, 10);
        } else {
            total = parseInt(totalCandidatsTempsAmenage, 10);
        }
        candidats.forEach(function(item) {
            var splitTempId = item.id.split("_");
            var test = (document.getElementById('tempsAmenage_' + splitTempId[1]).title == "Non") ? false : true;
            if ((test && tempsAmenageBool) || (!test && !tempsAmenageBool)) {
                sum = sum + parseInt(item.text, 10);
            } else {
                sum2 = sum2 + parseInt(item.text, 10);
            }
        });;
        tempTotal = sum + sum2;
        var nbCandidat = parseInt(candidat.text, 10);
        if ((nbCandidat > 0 && sum > 0 && operation == "minus") ||  (nbCandidat < diviseur && sum < total && operation == "plus")) {
            var newCandidats = (operation == "minus") ? nbCandidat - 1 : nbCandidat + 1;
            candidat.text = newCandidats;
            inputCandidat.value = newCandidats;
            var newTxRemplissage = Math.round((newCandidats / diviseur) * 100);
            if (tempsAmenageBool) {
                newTotalTiers.textContent = (operation == "minus") ? sum - 1 : sum + 1;
            } else {
                newTotal.textContent = (operation == "minus") ? sum - 1 : sum + 1;
            }
            percentage.textContent = newTxRemplissage + '%';
            var realTotal = (operation == "minus") ? tempTotal - 1 : tempTotal + 1;
            if (realTotal < bigTotal) {
                submitAffinage.disabled = true;
            } else {
                submitAffinage.disabled = false;
            }
        } else {
            msgWarning.textContent = (operation == "minus") ? "Vous ne pouvez plus retirer de personnes" : "Vous ne pouvez plus ajouter de personnes";
        }
    }

    var submitAffinage = document.getElementById("affinerButton");
    if (submitAffinage != null) {
        var minus = document.querySelectorAll('[id^="minusRepartition_"]');

        var candidats = document.querySelectorAll('[id^="candidats_"]');
        var newTotal = document.getElementById('newTotal');
        var newTotalTiers = document.getElementById('newTotalTiers');
        var msgWarning = document.getElementById('msgWarning');
        var bigTotal = parseInt(totalCandidatsNotTempsAmenage, 10) + parseInt(totalCandidatsTempsAmenage, 10);

        submitAffinage.addEventListener('click', function(e) {
            var formAffinage = document.getElementById("formAffinage");
            formAffinage.submit();
        });
        minus.forEach(function(item) {
            item.addEventListener('click', function(e) {
                var splitId = this.id.split("_");
                var realId = splitId[1];
                affinerRepartition(realId, "minus");
            });
        });
        var plus = document.querySelectorAll('[id^="plusRepartition_"]');
        plus.forEach(function(item) {
            item.addEventListener('click', function(e) {
                msgWarning.textContent = "";
                var splitId = this.id.split("_");
                var realId = splitId[1];
                affinerRepartition(realId, "plus");
            });
        });
    }

    var helpForm = document.getElementById('helpForm');
    if (helpForm != null) {
        var areaEditor = document.getElementById("areaEditor");
        var suneditor = createSunEditor('value');
        helpForm.addEventListener('submit', function(e) {
            e.preventDefault();
            document.getElementById("value").value = suneditor.getContents();
            helpForm.submit();
        });
    }

    //stats
    if (document.getElementById('statsCharts') != null) {
        var url = "manager";
        $("body").css("background-color", "#f2f2f2");
        //getStats(param, null,id, chartType,  option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, byMonth)
        getStats(null, url, "sessionEpreuvesByCampus", "pie");
        getStats(null, url, "sessionLocationByLocation", "doughnut");
        getStats(null, url, "tagCheckersByContext", "doughnut");
        getStats(null, url, "presenceByContext", "pie");
        getStats(null, url, "sessionEpreuveByYearMonth", "lineChart", null, null, false, null, null, null, true, monthsArray, true);
        getStats(null, url, "countTagCheckByYearMonth", "lineChart", null, null, false, null, null, null, true, monthsArray, true);
        getStats(null, url, "countTagChecksByTypeBadgeage", "doughnut");
        getStats(null, url, "countTagCheckBySessionLocationBadgedAndPerson", "doughnut");
    }
    //stats superadmin
    if (document.getElementById('statsSuperAdminCharts') != null) {
        var url = "superadmin";
        $("body").css("background-color", "#f2f2f2");
        //getStats(param, null,id, chartType,  spinner, option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, byMonth)
        getStats(null, url, "sessionEpreuvesByContext", "multiBar");
        getStats(null, url, "countTagChecksByContext", "multiBar");
        getStats(null, url, "countNbTagCheckerByContext", "chartBar");
        getStats(null, url, "countLocationsByContext", "chartBar");
        getStats(null, url, "countUserAppsByContext", "multiBar");
        getStats(null, url, "countCampusesByContext", "chartBar");
    }

    //stats session epreuve
    $('[id^=modalChartSeBtn]').on('click', function(e) {
        var url = "manager";
        var splitId = this.id.split("-");
        getStats("&param=" + splitId[1], url, "countTagChecksByTimeBadgeage", "chartBar");
    })
    $('#modal-chart').on('show.bs.modal', function(event) {
        var button = $(event.relatedTarget) // Button that triggered the modal
        var title = button.data('whatever') // Extract info from data-* attributes
        var modal = $(this)
        modal.find('.modal-title').text(title)
    })
    var calendarEl = document.getElementById('calendar');

    if (calendarEl != null) {
        getCalendar(calendarEl, emargementContextUrl + "/manager/calendar/events", true);
    }

    var calendarElAll = document.getElementById('calendarAll');
    if (calendarElAll != null) {
        getCalendar(calendarElAll, emargementContextUrl + "/superadmin/calendar/events", false);
    }

    //Datepicker SessionEpreuve
    $('input[type=date]').on('click', function(event) {
        event.preventDefault();
    });
    $('#dateSessionEpreuve').datetimepicker({
        format: 'DD/MM/YYYY',
        locale: 'fr',
        daysOfWeekDisabled: [0, 6],
        useCurrent: false

    });
    $('#stringDate').datetimepicker({
        format: 'DD/MM/YYYY',
        locale: 'fr',
        daysOfWeekDisabled: [0, 6],
        useCurrent: false

    });

    $('#heureConvocation').datetimepicker({
        format: 'LT',
        locale: 'fr'
    });
    $('#heureEpreuve').datetimepicker({
        format: 'LT',
        locale: 'fr'
    });
    $('#finEpreuve').datetimepicker({
        format: 'LT',
        locale: 'fr'
    });

    //Pagination --->rajout tous
    if (document.getElementById('pagination') != null) {
        if (allSelect != null && allSelect > 10) {
            var location = window.location;
            var dataUrl = location.pathname;
            var allSelectUrl = "";
            if (location.search == "") {
                allSelectUrl = dataUrl + "?size=" + allSelect;
            } else {
                var url = new URL(location);
                var query_string = url.search;
                var search_params = new URLSearchParams(query_string);
                search_params.set('size', allSelect);
                url.search = search_params;
                allSelectUrl = url.pathname + url.search;
                search_params.set('size', "10");
                url.search = search_params;
                selectUrl10 = url.pathname + url.search;
            }
            $("#pagination select").prepend("<option value='" + allSelect + "' data-page-size-url='" + allSelectUrl + "'>Tous</option>");
        }
        $("#pagination option:selected").removeProp("selected");
        var sizeSelected = ($_GET("size") != null) ? $_GET("size") : allSelect;
        $("#pagination option[value='" + sizeSelected + "']").attr('selected', 'selected');
    }

    //Extraction
    if (document.getElementById('extractionPage') != null) {
        new SlimSelect({
            select: '#codSes',
            showSearch: false
        });
        new SlimSelect({
            select: '#codAnu',
            showSearch: false
        });
        new SlimSelect({
            select: '#sessionEpreuve'
        });
        selectCmp = new SlimSelect({
            select: '#codCmp',
            searchText: 'Sorry nothing to see here'
        });
        selectEtp = new SlimSelect({
            select: '#codEtp'
        });
        selectEtp.enable();
        selectElp = new SlimSelect({
            select: '#codElp',
            placeholder: true,
            text: 'placeholder text'
        });
        selectElp.enable();
        selectGroupes = new SlimSelect({
            select: '#codExtGpe',
            allowDeselect: true
        });
        selectGroupes.enable();
        new SlimSelect({
            select: '#sessionEpreuveLdap'
        });
    }
    if (document.getElementById('eventsSelect') != null) {
        new SlimSelect({
            select: '#icsSelect',
            showSearch: false
        });
        new SlimSelect({
            select: '#icsEvents',
            placeholder: 'Recherche épreuve'
        });
    }
    if (document.getElementById('suList') != null) {
        new SlimSelect({
            select: '#suList',
            placeholder: 'Rechercher Utilisateur'
        });
    }
    //Presence
    if (document.getElementById('presencePage') != null) {
        const uuid = ID();
        const eventSource = new EventSource(emargementContextUrl + `/supervisor/register/${uuid}`);
        eventSource.addEventListener('tc', response => {

            var tagCheck = JSON.parse(response.data);
            var isPresent = (tagCheck.tagDate == null) ? false : true;

            $("#" + tagCheck.id + " #tagDate").html(isPresent ? moment(tagCheck.tagDate).format('HH:mm:ss') : "");
            $("#" + tagCheck.id).prop("class", (isPresent) ? "table-success" : "table-danger");
            $("#" + tagCheck.id + " .presenceCheck").prop("checked", (isPresent) ? true : false);
            var iscCheckedByCard = (tagCheck.isCheckedByCard) ? "<i class='fa fa-check text-success'></i>" : "<i class='fa fa-times text-danger'></i>";
            $("#" + tagCheck.id + " .checkedByCard").html(isPresent ? iscCheckedByCard : "");
            var sessionLocationBadged = (tagCheck.sessionLocationBadged != null) ? tagCheck.sessionLocationBadged.location.nom : '';
            $("#" + tagCheck.id + " .sessionLocationBadged").html(isPresent ? sessionLocationBadged : "");
            var tagChecker = (tagCheck.tagChecker != null) ? tagCheck.tagChecker.userApp.prenom + ' ' + tagCheck.tagChecker.userApp.nom : '';
            $("#" + tagCheck.id + " .tagChecker").html(isPresent ? tagChecker : '');
        }, false);
        
        eventSource.addEventListener('sl', response => {
            var sl = JSON.parse(response.data); console.log(sl);
            $("#sl" + sl.id ).html(sl.nbPresentsSessionLocation);
        }, false);
        
        eventSource.addEventListener('message', this.onmessage = function(e) {
            var splitData = e.data.split("@@");
            var percent = parseFloat(splitData[1]).toFixed(2);
            $("#progressBar" + splitData[0]).css("width", percent + '%');
            $("#progressBar" + splitData[0]).text(percent + '%');
        });
        eventSource.addEventListener('total', response => {
            var total = response.data;
            var splitData1 = total.split("@@");
            $("#totalPresent" + splitData1[0]).text(splitData1[1]);
        }, false);
        eventSource.addEventListener('refresh', response => {
            var refresh = response.data;
            if (refresh > 0) {
                var redirect = deleteParam(window.location.href, "tc");
                setTimeout(function() {
                    window.location.href = redirect;
                }, 1750);
            }
        }, false);
    }

    //Extraction ldap
    var searchGroup = document.getElementById('searchLdapGroupForm');
    var searchLdapGroupForm = document.getElementById('searchLdapGroupForm');
    if (searchGroup != null) {
        searchUsersAutocomplete("searchGroup", emargementContextUrl + "/manager/extraction/ldap/searchGroup", "", 20);

        searchGroup.addEventListener("awesomplete-selectcomplete", function(event) {
            searchLdapGroupForm.submit();
        });
    }

    //Remplissage form session à partir d'ics
    var icsEvents = document.getElementById('icsEvents');
    if (icsEvents != null) {
        var icsSelect = document.getElementById('icsSelect');
        icsSelect.addEventListener("change", function() {
            setSelectEvent(this.value, "#divEvents");
        })

        $(document).on("change", "#icsEvents", function() {
            var url = emargementContextUrl + "/manager/event/searchEvent";
            var request = new XMLHttpRequest();
            request.open('GET', url + "?uid=" + this.value, true);
            request.onload = function() {
                if (request.status >= 200 && request.status < 400) {
                    var data = JSON.parse(this.response);
                    $("#nomSessionEpreuve").val(data.summary);
                    $("#dateSessionEpreuve").val(moment(data.startDate).format('DD/MM/YYYY'));
                    $("#heureConvocation").val(moment(data.startDate).subtract(30, 'minutes').format('HH:mm'));
                    $("#heureEpreuve").val(moment(data.startDate).format('HH:mm'));
                    $("#finEpreuve").val(moment(data.endDate).format('HH:mm'));
                } else {
                    console.log("erreur du serveur!");
                }
            };
            request.send();
        });
    }
    //modal events
    var modalEvent = document.getElementById('modal-event');
    if (modalEvent != null) {
        $('[id^=event]').on('click', function(e) {
            var splitId = this.id.split("-");
            setSelectEvent(splitId[1], "#modal-body-event");
        })
        $('#modal-event').on('show.bs.modal', function(event) {
            var button = $(event.relatedTarget) // Button that triggered the modal
            var title = button.data('whatever') // Extract info from data-* attributes
            var modal = $(this)
            modal.find('.modal-title').text(title)
        })
    }

    //Locations
    var icsEventLocations = document.getElementById('icsEventLocations');
    if (icsEventLocations != null) {
        new SlimSelect({
            select: '#icsEventLocations',
            placeholder: 'Recherche Lieu'
        })
        icsEventLocations.addEventListener("change", function() {
            $("#nom").val(this.value);
        });
    }

    //select context
    var selectContext = document.getElementById('selectContext');
    if (selectContext != null) {
        selectContext.addEventListener("change", function() {
            var text = $(this).find("option:selected").text();
            if (text == "Ordre") {
                window.location.href = this.value;
            } else {
                this.form.submit();
            }
        })
    }

    //Form update Session
    $("#isSessionEpreuveClosed1").click(function() {
        $("input[type=text]").prop("readonly", false);
        $("input[type=time]").prop("readonly", false);
        $("select").prop("disabled", false);
        $("#hiddenCampus").prop("disabled", true);
        $("#file").prop("disabled", false);

        if ($(this).is(":checked")) {
            $("input[type=text]").prop("readonly", true);
            $("input[type=time]").prop("readonly", true);
            $("select").prop("disabled", true);
            $("#hiddenCampus").prop("disabled", false);
            $("#file").prop("disabled", true);
        }
    });
    
    //affiche modal photo
    $('.userModal').on("click", function(event) {
    	var splitField = this.getAttribute("data-whatever").split("//");
    	$("#photoPresent").prop("src", emargementContextUrl + "/supervisor/" + splitField[0] + "/photo");
    	$("#photoPresent").prop("alt", "Photo" + splitField[0]);
    	$("#nomPresence").text(splitField[1]);
    	$("#prenomPresence").text(splitField[2]);
    	$('#photoModal2').modal('show');
    	if(splitField[3]!="null"){
    		$("#numIdentifiantPresence").text(splitField[3]);
    	}
    });
    $('#photoModal2').on('hidden.bs.modal', function (e) {
    	$("#photoPresent").prop("src", "");
    	$("#photoPresent").prop("alt", "");
    })
    
    //Changement année univ
    $("#anneeUnivSelect").on("change",  function (e) {
    	var annee = this.value
    	window.location.href = emargementContextUrl + "/manager/sessionEpreuve?anneeUniv=" + annee;
    });
});