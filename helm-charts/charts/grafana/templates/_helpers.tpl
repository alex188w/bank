{{- define "grafana.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "grafana.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}