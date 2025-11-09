{{- define "cash-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "cash-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}