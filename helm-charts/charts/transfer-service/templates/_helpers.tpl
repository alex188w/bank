{{- define "transfer-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "transfer-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}