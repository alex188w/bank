{{- define "kaffka-service.name" -}}
kaffka-service
{{- end }}

{{- define "kaffka-service.fullname" -}}
{{ include "kaffka-service.name" . }}
{{- end }}

{{- define "kaffka-service.labels" -}}
app.kubernetes.io/name: {{ include "kaffka-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}