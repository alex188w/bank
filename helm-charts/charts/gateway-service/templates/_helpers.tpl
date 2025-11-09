{{- define "gateway-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "gateway-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}