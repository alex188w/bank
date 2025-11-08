{{- define "account-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "account-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}