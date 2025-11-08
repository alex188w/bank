{{/*
Имя чарта (короткое)
*/}}
{{- define "postgres.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Полное имя (используется для всех ресурсов)
*/}}
{{- define "postgres.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "postgres.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}