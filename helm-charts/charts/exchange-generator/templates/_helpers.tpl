{{- define "exchange-generator.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "exchange-generator.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}