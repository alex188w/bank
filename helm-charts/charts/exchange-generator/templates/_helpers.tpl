{{/*
Return the name of the chart
*/}}
{{- define "exchange-generator.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Return the chart name and version
*/}}
{{- define "exchange-generator.chart" -}}
{{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Return the fullname (used for resource names)
*/}}
{{- define "exchange-generator.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "exchange-generator.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}