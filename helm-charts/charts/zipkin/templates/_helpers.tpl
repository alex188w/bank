{{- define "zipkin.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "zipkin.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}