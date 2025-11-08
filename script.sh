#!/bin/bash
kubectl get clusterrole | Select-String "external-secrets" | ForEach-Object {
    $name = ($_ -split '\s+')[0]
    kubectl delete clusterrole $name
}

# Удаляем все ClusterRoleBinding с external-secrets в имени
kubectl get clusterrolebinding | Select-String "external-secrets" | ForEach-Object {
    $name = ($_ -split '\s+')[0]
    kubectl delete clusterrolebinding $name
}