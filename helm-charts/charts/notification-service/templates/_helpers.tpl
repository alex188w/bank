{{- define "notification-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "notification-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}